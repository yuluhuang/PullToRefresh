package com.pulltorefresh.ylh.pulltorefresh;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: 余芦煌(504367857@qq.com)
 * Date: 2014-12-17
 * Time: 18:52
 * FIXME
 */
public class PullToRefresh extends ListView implements AbsListView.OnScrollListener {

    View header;//顶部布局文件
    int headerHeight;//顶部布局文件的高度
    int firstVisibleItem;//当前第一个可见item的位置
    int scrollState;//listView 当前滚动状态

    boolean isRemark;//标记，当前是在listView最顶端按下的
    int startY;//按下时的Y值
    int state;//当前状态
    final int NONE = 0;//正常状态
    final int PULL = 1;//提示下拉状态
    final int RELESE = 2;//提示释放状态
    final int REFLASHING = 3;//刷新状态


    public void setiReflashListener(IReflashListener iReflashListener) {
        this.iReflashListener = iReflashListener;
    }

    IReflashListener iReflashListener;//刷新数据的接口

    public PullToRefresh(Context context) {
        super(context);
        initView(context);
    }

    public PullToRefresh(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public PullToRefresh(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }


    /**
     * 初始化界面，加载顶部布局文件
     *
     * @param context
     */
    private void initView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        header = inflater.inflate(R.layout.header_layout, null);
        measureView(header);
        headerHeight = header.getMeasuredHeight();
        Log.i("R", headerHeight + "");
        topPadding(-headerHeight);
        this.addHeaderView(header);
        this.setOnScrollListener(this);
    }

    /**
     * 通知父布局，占用的宽和高
     *
     * @param view
     */
    private void measureView(View view) {
        ViewGroup.LayoutParams p = view.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            int width = ViewGroup.getChildMeasureSpec(0, 0, p.width);
            int height;
            int tempHeight = p.height;
            if (tempHeight > 0) {
                height = MeasureSpec.makeMeasureSpec(tempHeight, MeasureSpec.EXACTLY);

            } else {
                height = MeasureSpec.makeMeasureSpec(tempHeight, MeasureSpec.UNSPECIFIED);
            }
            view.measure(width, height);
        }
    }

    /**
     * 设置header布局上边距
     *
     * @param topPadding
     */
    private void topPadding(int topPadding) {
        header.setPadding(header.getPaddingLeft(), topPadding, header.getPaddingRight(),
                header.getPaddingBottom());
        header.invalidate();

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        this.firstVisibleItem = firstVisibleItem;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        this.scrollState = scrollState;

    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (firstVisibleItem == 0) {
                    isRemark=true;
                    startY=(int)ev.getY();

                }
                break;
            case MotionEvent.ACTION_MOVE:
                onMove(ev);
                break;
            case MotionEvent.ACTION_UP:
                if(state==RELESE){
                    state=REFLASHING;
                    //加载最新数据；
                    reflashViewByState();
                    iReflashListener.onReflash();

                }else if(state==PULL){
                    state=NONE;
                    isRemark=false;
                    reflashViewByState();
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    /**
     * 根据当前状态，改变界面显示
     */
    private void reflashViewByState(){
        TextView tip=(TextView)header.findViewById(R.id.tip);
        ImageView arrow=(ImageView)header.findViewById(R.id.arrow);
        ProgressBar progress=(ProgressBar)header.findViewById(R.id.progress);
        RotateAnimation animation=new RotateAnimation(0,180,
                RotateAnimation.RELATIVE_TO_SELF,0.5f,
                RotateAnimation.RELATIVE_TO_SELF,0.5f);
        animation.setDuration(200);
        animation.setFillAfter(true);
        RotateAnimation animation1=new RotateAnimation(180,0,
                RotateAnimation.RELATIVE_TO_SELF,0.5f,
                RotateAnimation.RELATIVE_TO_SELF,0.5f);
        animation1.setDuration(0);
        animation1.setFillAfter(true);
        switch (state){
            case NONE:
                arrow.clearAnimation();
                topPadding(-headerHeight);
                break;
            case PULL:
                arrow.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);
                tip.setText("下拉刷新");
                arrow.clearAnimation();
                arrow.setAnimation(animation1);
                break;
            case RELESE:

                arrow.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);
                tip.setText("松开可以刷新");
                arrow.clearAnimation();
                arrow.setAnimation(animation);
                break;
            case REFLASHING:
                topPadding(50);
                arrow.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
                tip.setText("正在刷新...");
                arrow.clearAnimation();
                break;
        }

    }

    /**
     * 获取完数据
     */
    public void reflashComplete(){
        state=NONE;
        isRemark=false;
        reflashViewByState();
        TextView lastupdatetine=(TextView)header.findViewById(R.id.lastupdate_time);
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time=format.format(new Date(System.currentTimeMillis()));
        lastupdatetine.setText(time);
    }

    public  interface IReflashListener{
        public void onReflash();
    }
    /**
     * 判断移动过程中的操作
     *
     * @param ev
     */
    private void onMove(MotionEvent ev) {
        if (!isRemark) {
            return;
        }

        int tempY = (int) ev.getY();
        int space = tempY - startY;
        int topPadding=space-headerHeight;
        switch (state) {
            case NONE:
                if (space > 0) {
                    state = PULL;
                    reflashViewByState();
                }
                break;
            case PULL:
                topPadding(topPadding);
                if (space > headerHeight + 30 &&
                        scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    state = RELESE;
                    reflashViewByState();
                }
                break;
            case RELESE:
                topPadding(topPadding);
                if(space<headerHeight+30){
                    state=PULL;
                    reflashViewByState();
                }else if(space<=0){
                    state=NONE;
                    isRemark=false;
                    reflashViewByState();
                }

                break;
        }
    }
}
