package com.example.frank.mysticklayout;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.OverScroller;
import android.widget.ScrollView;

public class StickyNavLayout extends LinearLayout {

    private View      mTop;
    private View      mNav;
    private ViewPager mViewPager;

    private int       mTopViewHeight;
    private ViewGroup mInnerScrollView;
    private boolean isTopHidden = false;

    private OverScroller    mScroller;
    //通过这个类来获取加速度参数。
    private VelocityTracker mVelocityTracker;

    //用于区分用户是点击还是拖。
    private int mTouchSlop;
    private int mMaximumVelocity, mMinimumVelocity;

    private float   mLastY;
    //用于判断是不是移动。
    private boolean mDragging;

    private boolean isInControl = false;

    public StickyNavLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);

        mScroller = new OverScroller(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMaximumVelocity = ViewConfiguration.get(context)
                .getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context)
                .getScaledMinimumFlingVelocity();


    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTop = findViewById(R.id.id_stickynavlayout_topview);
        mNav = findViewById(R.id.id_stickynavlayout_indicator);
        View view = findViewById(R.id.id_stickynavlayout_viewpager);
        if (!(view instanceof ViewPager)) {
            throw new RuntimeException(
                    "id_stickynavlayout_viewpager show used by ViewPager !");
        }
        mViewPager = (ViewPager) view;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ViewGroup.LayoutParams params = mViewPager.getLayoutParams();
        //这里之所以这样减是因为头布局的不需要减。之后会滑动上去。
        params.height = getMeasuredHeight() - mNav.getMeasuredHeight();

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mTopViewHeight = mTop.getMeasuredHeight();
    }

    //这一大段代码不知道有什么软用！！有用，作用是不停的去循环当前的event事件，保证当第一次走到第一个位置往下拉的时候，可以拉出来。
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
		int action = ev.getAction();
		float y = ev.getY();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mLastY = y;
			break;
		case MotionEvent.ACTION_MOVE:
			float dy = y - mLastY;
			getCurrentScrollView();

            //这段代码的作用是第一次头布局隐藏起来的时候，并且是往下滑动，把事件取消再重新分发，然后走到拦截里面。
			if (mInnerScrollView instanceof ScrollView) {
				if (mInnerScrollView.getScrollY() == 0 && isTopHidden && dy > 0
						&& !isInControl) {
					isInControl = true;
					ev.setAction(MotionEvent.ACTION_CANCEL);
					MotionEvent ev2 = MotionEvent.obtain(ev);
					dispatchTouchEvent(ev);
					ev2.setAction(MotionEvent.ACTION_DOWN);
					return dispatchTouchEvent(ev2);
				}
			} else if (mInnerScrollView instanceof ListView) {

				ListView lv = (ListView) mInnerScrollView;
				View c = lv.getChildAt(lv.getFirstVisiblePosition());

				if (!isInControl && c != null && c.getTop() == 0 && isTopHidden
						&& dy > 0) {
					isInControl = true;
					ev.setAction(MotionEvent.ACTION_CANCEL);
					MotionEvent ev2 = MotionEvent.obtain(ev);
					dispatchTouchEvent(ev);
					ev2.setAction(MotionEvent.ACTION_DOWN);
					return dispatchTouchEvent(ev2);
				}
			}else if (mInnerScrollView instanceof RecyclerView) {

				RecyclerView rv = (RecyclerView) mInnerScrollView;

				if (!isInControl && android.support.v4.view.ViewCompat.canScrollVertically(rv, -1) && isTopHidden
						&& dy > 0) {
					isInControl = true;
                    //先把当前的点击事件取消，然后在copy一个相同的事件，分发下去，分发之后会走到自己的onintercepttouchevent()中，
                    //然后在判断是否拦截。
					ev.setAction(MotionEvent.ACTION_CANCEL);
					MotionEvent ev2 = MotionEvent.obtain(ev);
					dispatchTouchEvent(ev);
					ev2.setAction(MotionEvent.ACTION_DOWN);
					return dispatchTouchEvent(ev2);
				}
			}
			break;
		}
		return super.dispatchTouchEvent(ev);
	}
    /**
     *
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        float y = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mLastY;
                getCurrentScrollView();
                if (Math.abs(dy) > mTouchSlop) {
                    mDragging = true;
                    if (mInnerScrollView instanceof ScrollView) {
                        // 如果topView没有隐藏
                        // 或sc的scrollY = 0 && topView隐藏 && 下拉，则拦截
                        if (!isTopHidden
                                || (mInnerScrollView.getScrollY() == 0
                                && isTopHidden && dy > 0)) {

                            initVelocityTrackerIfNotExists();
                            mVelocityTracker.addMovement(ev);
                            mLastY = y;
                            return true;
                        }
                    } else if (mInnerScrollView instanceof ListView) {

                        ListView lv = (ListView) mInnerScrollView;
                        View c = lv.getChildAt(lv.getFirstVisiblePosition());
                        // 如果topView没有隐藏
                        // 或sc的listView在顶�?&& topView隐藏 && 下拉，则拦截

                        if (!isTopHidden || //
                                (c != null //
                                        && c.getTop() == 0//
                                        && isTopHidden && dy > 0)) {

                            initVelocityTrackerIfNotExists();
                            mVelocityTracker.addMovement(ev);
                            mLastY = y;
                            return true;
                        }
                    } else if (mInnerScrollView instanceof RecyclerView) {
                        RecyclerView rv = (RecyclerView) mInnerScrollView;

                        if (!isTopHidden || (!android.support.v4.view.ViewCompat.canScrollVertically(rv, -1) &&
                                isTopHidden && dy > 0)) {
                            initVelocityTrackerIfNotExists();
                            mVelocityTracker.addMovement(ev);
                            mLastY = y;
                            return true;
                        }
                    }

                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mDragging = false;
                recycleVelocityTracker();
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private void getCurrentScrollView() {

        int currentItem = mViewPager.getCurrentItem();
        PagerAdapter a = mViewPager.getAdapter();
        if (a instanceof FragmentPagerAdapter) {
            FragmentPagerAdapter fadapter = (FragmentPagerAdapter) a;
            Fragment item = (Fragment) fadapter.instantiateItem(mViewPager,
                    currentItem);
            mInnerScrollView = (ViewGroup) (item.getView()
                    .findViewById(R.id.id_stickynavlayout_innerscrollview));
        } else if (a instanceof FragmentStatePagerAdapter) {
            FragmentStatePagerAdapter fsAdapter = (FragmentStatePagerAdapter) a;
            Fragment item = (Fragment) fsAdapter.instantiateItem(mViewPager,
                    currentItem);
            mInnerScrollView = (ViewGroup) (item.getView()
                    .findViewById(R.id.id_stickynavlayout_innerscrollview));
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(event);
        int action = event.getAction();
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mLastY = y;
                return true;//这里为什么要return true呢？
            case MotionEvent.ACTION_MOVE:
                float dy = y - mLastY;

                Log.e("TAG", "dy = " + dy + " , y = " + y + " , mLastY = " + mLastY);

                if (!mDragging && Math.abs(dy) > mTouchSlop) {
                    mDragging = true;
                }
                if (mDragging) {
                    scrollBy(0, (int) -dy);

                    // 如果topView隐藏，且上滑动时，则改变当前事件为ACTION_DOWN
                    if (getScrollY() == mTopViewHeight && dy < 0) {
                        event.setAction(MotionEvent.ACTION_DOWN);
                        dispatchTouchEvent(event);
                        isInControl= false;

                    }
                }

                mLastY = y;
                break;
            case MotionEvent.ACTION_CANCEL:
                mDragging = false;
                recycleVelocityTracker();
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_UP:
                mDragging = false;
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocityY = (int) mVelocityTracker.getYVelocity();
                if (Math.abs(velocityY) > mMinimumVelocity) {
                    fling(-velocityY);
                }
                recycleVelocityTracker();
                break;
        }

        return super.onTouchEvent(event);
    }


    /**
     * fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY)
     * 这个方法也很重要，
     * 如果你想实现滑动之后，布局能够根据移动速度，慢慢减速的话，就需要用这个来实现，
     * 这里需要加速度的参数，我们可以通过VelocityTracker这个类来获取，然后使用，
     *
     * @param velocityY
     */
    public void fling(int velocityY) {
        mScroller.fling(0, getScrollY(), 0, velocityY, 0, 0, 0, mTopViewHeight);
        invalidate();
    }

    @Override
    public void scrollTo(int x, int y) {
        if (y < 0) {
            y = 0;
        }
        if (y > mTopViewHeight) {
            y = mTopViewHeight;
        }
        if (y != getScrollY()) {
            super.scrollTo(x, y);
        }

        isTopHidden = getScrollY() == mTopViewHeight;

    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(0, mScroller.getCurrY());
            invalidate();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

}
