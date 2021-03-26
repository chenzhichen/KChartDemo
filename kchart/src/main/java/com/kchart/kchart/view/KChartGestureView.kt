package com.kchart.kchart.view

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat
import com.kchart.kchart.util.Transformer
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt


abstract class KChartGestureView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val gestureListener = GestureListener()
    private var gestureDetector: GestureDetectorCompat =
        GestureDetectorCompat(getContext(), gestureListener)
    var overScroller: OverScroller = OverScroller(getContext())

    var currentScaleX = 1f

    var longPressEvent: LongPressEvent? = null

    private var isLongPress = false


    private var scaleXMax = 5f

    private var scaleXMin = 0.5f

    private var lastFlingX = 0f

    private var savedDist = 1f

    private var isMultipleTouch = false
    private var centerX = 0f
    private var factor = 0f


    val transformer = Transformer()

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
                    centerX = getCentreX(event)
                }
            }

            MotionEvent.ACTION_DOWN -> {
                lastFlingX = 0f
                overScroller.abortAnimation()
            }

            MotionEvent.ACTION_MOVE -> {
                if (isLongPress) {
                    longPressEvent?.position = getPointIndex(event.x)
                    longPressEvent?.y = event.y
                    longPressEvent?.isLongPress = isLongPress
                    invalidate()
                    return true
                }
                if (isMultipleTouch) {
                    val dist = spacing(event)
                    factor = dist / savedDist
                    currentScaleX *= factor
                    savedDist = dist
                    when {
                        currentScaleX < scaleXMin -> {
                            currentScaleX = scaleXMin
                        }
                        currentScaleX > scaleXMax -> {
                            currentScaleX = scaleXMax
                        }
                        else -> {
                            transformer.zoom(factor, centerX)
                            calculateValue()
                        }
                    }
                    return true
                }
            }

        }
        return gestureDetector.onTouchEvent(event)
    }


    override fun computeScroll() {
        if (overScroller.computeScrollOffset()) {
            val x = overScroller.currX.toFloat()
            val dx = x - lastFlingX
            lastFlingX = x
            scrollTo(dx)
            if (transformer.hasToHeader() || transformer.hasToFooter()) {
                overScroller.abortAnimation()
            }
        }
    }


    open fun scrollTo(x: Float) {
        transformer.scroll(x)
        calculateValue()
    }

    abstract fun calculateValue()

    abstract fun getChartWidth(): Float

    abstract fun getDataSize(): Int

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
                scrollTo(distanceX)
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
                getPointIndex(e.x),
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
            lastFlingX = 0f
            if (!transformer.hasToFooter() && !transformer.hasToHeader()) {
                overScroller.fling(
                    0,
                    0,
                    (-velocityX).toInt(),
                    0,
                    Int.MIN_VALUE,
                    Int.MAX_VALUE,
                    0,
                    0
                )
                invalidate()
                return true
            }
            return false
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


    private fun blockParentEvent() {
        parent?.requestDisallowInterceptTouchEvent(true)
    }

    private fun getPointIndex(x: Float): Int {
        var position = transformer.pixelsToValue(if (x > getChartWidth()) getChartWidth() else x)
            .roundToInt()
        if (position >= getDataSize()) {
            position = getDataSize() - 1;
        }
        if (position < 0) {
            position = 0
        }
        return position
    }

}