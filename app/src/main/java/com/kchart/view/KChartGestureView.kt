package com.kchart.view

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt


abstract class KChartGestureView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val gestureListener = GestureListener()
    private var gestureDetector: GestureDetectorCompat =
        GestureDetectorCompat(getContext(), gestureListener)
    var overScroller: OverScroller = OverScroller(getContext())

    var scrollXDistance = 0f

    var currentScaleX = 2f

    var longPressEvent: LongPressEvent? = null

    private var isLongPress = false

    var scaleXMax = 5f

    var scaleXMin = 0.5f

    private var savedDist = 1f

    private var isMultipleTouch = false

    private var preScalePosition = 0//准备放大的数据下标

    override fun onTouchEvent(event: MotionEvent): Boolean {
        isMultipleTouch = event.pointerCount >= 2
        when (event.action and event.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isLongPress = false
                longPressEvent?.isLongPress = isLongPress
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    blockParentEvent()
                    savedDist = spacing(event)
                    preScalePosition = indexOfTranslateX(xToTranslateX(getCentreX(event)))
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isLongPress) {
                    longPressEvent?.position = indexOfTranslateX(xToTranslateX(event.x))
                    longPressEvent?.y = event.y
                    longPressEvent?.isLongPress = isLongPress
                    invalidate()
                    return true
                }
                if (isMultipleTouch) {
                    val dist = spacing(event)
                    currentScaleX *= dist / savedDist
                    savedDist = dist
                    when {
                        currentScaleX < scaleXMin -> {
                            currentScaleX = scaleXMin
                        }
                        currentScaleX > scaleXMax -> {
                            currentScaleX = scaleXMax
                        }
                        else -> {
                            scrollBy(translateXtoX(getX(preScalePosition)) - getCentreX(event))
                        }
                    }
                    return true
                }
            }

        }
        gestureDetector.onTouchEvent(event)
        return true
    }


    override fun computeScroll() {
        if (overScroller.computeScrollOffset()) {
            scrollTo(overScroller.currX.toFloat())
        }
    }

    fun scrollBy(x: Float) {
        scrollTo(scrollXDistance - (x / currentScaleX))
    }

    open fun scrollTo(x: Float) {
        val oldX = scrollXDistance
        scrollXDistance = x
        when {
            scrollXDistance < getMinScrollX() -> {
                scrollXDistance = getMinScrollX()
                farRight()
                overScroller.forceFinished(true)
            }
            scrollXDistance > getMaxScrollX() -> {
                scrollXDistance = getMaxScrollX()
                overScroller.forceFinished(true)
            }
        }
        if (oldX < x) {
            farLeft()
        }
        onScaleChanged()
        calculateValue()
    }

    abstract fun calculateValue()

    abstract fun getMinScrollX(): Float

    abstract fun getMaxScrollX(): Float

    abstract fun onScaleChanged()

    abstract fun xToTranslateX(x: Float): Float

    abstract fun indexOfTranslateX(translateX: Float): Int

    abstract fun getX(position: Int): Float

    abstract fun translateXtoX(translate: Float): Float

    abstract fun farLeft()

    abstract fun farRight()

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return false
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (!isLongPress && !isMultipleTouch) {
                blockParentEvent()
                scrollBy(distanceX)
                return true
            }
            return false
        }

        override fun onLongPress(e: MotionEvent) {
            if (e.pointerCount >= 2) {
                return
            }
            blockParentEvent()
            isLongPress = true
            longPressEvent = LongPressEvent(
                indexOfTranslateX(xToTranslateX(e.x)),
                e.y,
                isLongPress
            )
            invalidate()
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (isLongPress || isMultipleTouch) {
                return false
            }
            blockParentEvent()
            overScroller.fling(
                scrollXDistance.toInt(),
                0,
                (velocityX / currentScaleX).roundToInt(),
                0,
                Int.MIN_VALUE,
                Int.MAX_VALUE,
                0,
                0
            )
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            longPressEvent = null
            invalidate()
            return false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            return true
        }

    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun getCentreX(event: MotionEvent): Float {
        return abs(event.getX(0) - event.getX(1)) / 2 + min(
            event.getX(0),
            event.getX(1)
        )
    }

    fun checkAndFixScrollX() {
        if (scrollXDistance < getMinScrollX()) {
            scrollXDistance = getMinScrollX()
            overScroller.forceFinished(true)
        } else if (scrollXDistance > getMaxScrollX()) {
            scrollXDistance = getMaxScrollX()
            overScroller.forceFinished(true)
        }
    }


    private fun blockParentEvent() {
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true)
        }
    }

}