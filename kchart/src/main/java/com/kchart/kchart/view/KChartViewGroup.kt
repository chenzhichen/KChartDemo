package com.kchart.kchart.view

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.widget.OverScroller
import androidx.core.view.NestedScrollingParent
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.kchart.kchart.util.getScreenSize
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToLong


class KChartViewGroup(context: Context, attrs: AttributeSet) : ViewGroup(context, attrs),
    NestedScrollingParent {
    companion object {
        private const val TOP_CHILD_FLING_THRESHOLD = 3
    }

    private var topViewHeight = 0
    private var lastY = 0f
    private var x = 0
    private var y = 0
    private var scroller: OverScroller = OverScroller(context)
    private var velocityTracker: VelocityTracker? = null
    private var offsetAnimator: ValueAnimator? = null
    private var maximumVelocity = ViewConfiguration.get(context)
        .scaledMaximumFlingVelocity
    private var minimumVelocity = ViewConfiguration.get(context)
        .scaledMinimumFlingVelocity

    private var touchSlop = ViewConfiguration.get(context).scaledTouchSlop


    override fun onStartNestedScroll(child: View, target: View, axes: Int): Boolean {
        return true
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
    }

    override fun onStopNestedScroll(target: View) {
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        val hiddenTop = dy > 0 && scrollY < topViewHeight
        val showTop = dy < 0 && scrollY >= 0 && !target.canScrollVertically(-1)
        if (hiddenTop || showTop) {
            scrollBy(0, dy)
            consumed[1] = dy
        }
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        var consumed = consumed
        if (target is RecyclerView && velocityY < 0) {
            val firstChild = target.getChildAt(0)
            val childAdapterPosition = target.getChildAdapterPosition(firstChild)
            consumed = childAdapterPosition > TOP_CHILD_FLING_THRESHOLD
        }
        if (!consumed) {
            animateScroll(velocityY, computeDuration(0f))
        } else {
            animateScroll(velocityY, computeDuration(velocityY))
        }
        return true
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun getNestedScrollAxes(): Int {
        return 0
    }


    private fun animateScroll(velocityY: Float, duration: Long) {
        val currentOffset = scrollY
        if (offsetAnimator == null) {
            offsetAnimator = ValueAnimator()
            offsetAnimator!!.addUpdateListener { animation ->
                if (animation.animatedValue is Int) {
                    scrollTo(0, (animation.animatedValue as Int))
                }
            }
        } else {
            offsetAnimator?.cancel()
        }
        offsetAnimator?.duration = min(duration, 600)
        if (velocityY >= 0) {
            offsetAnimator?.setIntValues(currentOffset, topViewHeight)
            offsetAnimator?.start()
        }
    }

    private fun computeDuration(velocityY: Float): Long {
        var velocityY = velocityY
        val distance: Int
        distance = if (velocityY > 0) {
            abs(topViewHeight - scrollY)
        } else {
            abs(topViewHeight - (topViewHeight - scrollY))
        }
        val duration: Long
        velocityY = abs(velocityY)
        duration = if (velocityY > 0) {
            3 * (1000 * (distance / velocityY)).roundToLong()
        } else {
            val distanceRatio = distance.toFloat() / height
            ((distanceRatio + 1) * 150).toLong()
        }
        return duration
    }


    private fun initVelocityTrackerIfNotExists() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker!!.recycle()
            velocityTracker = null
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var height = 0
        var barHeight = 0
        topViewHeight = 0
        for (i in 0 until childCount) {
            val view = getChildAt(i) ?: continue
            if (getScreenSize(context as Activity).height() == 0) {
                continue
            }
            if (view is RecyclerView || view is ViewPager || view is NestedScrollView) {
                view.layoutParams.height = getScreenSize(context as Activity).height() - barHeight
                height += view.layoutParams.height
                measureChild(view, widthMeasureSpec, heightMeasureSpec)
            } else {
                measureChild(view, widthMeasureSpec, heightMeasureSpec)
                if (view.tag != null && view.tag is String && view.tag == "Scroll") {
                    topViewHeight += view.measuredHeight
                }
                if (view.tag != null && view.tag is String && view.tag == "Bar") {
                    barHeight += view.measuredHeight
                }
                height += view.measuredHeight
            }
        }
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var hadUsedVertical = 0
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            if (view.visibility == View.GONE) {
                continue
            }
            view.layout(
                0,
                hadUsedVertical,
                view.measuredWidth,
                hadUsedVertical + view.measuredHeight
            )
            hadUsedVertical += view.measuredHeight
        }
    }


    private fun fling(velocityY: Int) {
        scroller.fling(0, scrollY, 0, velocityY, 0, 0, 0, topViewHeight)
        invalidate()
    }

    override fun scrollTo(x: Int, y: Int) {
        var y = y
        if (y < 0) {
            y = 0
        }
        if (y > topViewHeight) {
            y = topViewHeight
        }
        if (y != scrollY) {
            super.scrollTo(x, y)
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(0, scroller.currY)
            invalidate()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        var intercepted = false
        if (ev.action == MotionEvent.ACTION_DOWN) {
            super.onInterceptTouchEvent(ev)
            if (!scroller.isFinished) scroller.abortAnimation()
            lastY = ev.y
            x = ev.x.toInt()
            y = ev.y.toInt()
            intercepted = false
        }
        if (scrollY >= topViewHeight) {
            return super.onInterceptTouchEvent(ev)
        }
        when (ev.action) {
            MotionEvent.ACTION_MOVE -> intercepted = if (ev.pointerCount >= 2) {
                false
            } else {
                //横向位移增量
                val deltaX = (x - ev.x).toInt()
                //竖向位移增量
                val deltaY = (y - ev.y).toInt()
                if (abs(deltaY) >= touchSlop) {
                    abs(deltaX) < abs(deltaY)
                } else {
                    false
                }
            }
            MotionEvent.ACTION_UP -> intercepted = false
        }
        return intercepted
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        initVelocityTrackerIfNotExists()
        velocityTracker!!.addMovement(event)
        val action = event.action
        val y = event.y
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) scroller.abortAnimation()
                lastY = y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dy: Float = y - lastY
                scrollBy(0, (-dy).toInt())
                lastY = y
            }
            MotionEvent.ACTION_CANCEL -> {
                lastY = 0f
                recycleVelocityTracker()
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
            }
            MotionEvent.ACTION_UP -> {
                lastY = 0f
                velocityTracker!!.computeCurrentVelocity(1000, maximumVelocity.toFloat())
                val velocityY = velocityTracker!!.yVelocity.toInt()
                if (abs(velocityY) > minimumVelocity) {
                    fling(-velocityY)
                }
                recycleVelocityTracker()
            }
        }
        return false
    }

}