package com.kchart.kchart.view

import android.content.Context
import android.database.DataSetObserver
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import com.kchart.kchart.util.dpToPx
import kotlin.math.max
import kotlin.math.min


class KChartView(context: Context, attrs: AttributeSet) : KChartGestureView(context, attrs) {

    var mainTopHeight = dpToPx(40f)
    var mainRectHeight = dpToPx(250f)
    var timeRectHeight = dpToPx(15f)
    var indexTopHeight = dpToPx(20f)
    var indexRectHeight = dpToPx(65f)

    //长按的水平线是否跟随收盘价
    var isLocking = true

    //绘制K线主图
    private var mainChartDraw: ChartDraw? = null
    fun setMainChartDraw(chartDraw: ChartDraw) {
        mainChartDraw = chartDraw
        calculateValue()
    }

    //主图绘制的范围
    var mainRect = RectF()

    //绘制指标副图
    private val childChartDraw = LinkedHashMap<String, ChartDraw>()
    private val childRect = LinkedHashMap<String, RectF>()

    private val childRectMinValue = LinkedHashMap<String, Float>()
    private val childRectMaxValue = LinkedHashMap<String, Float>()

    var startIndex = 0
    var stopIndex = 0

    //蜡状图是否实心
    var candleSolid = false

    var gridRows = 5
    var gridColumns = 3

    var viewHeight = 0f
    var viewWidth = 0f

    private var itemCount = 0
    private var itemWidth = dpToPx(5f)
    private var adapter: KChartAdapter? = null
    fun setAdapter(newAdapter: KChartAdapter) {
        if (dataSetObserver != null) {
            adapter?.unregisterDataSetObserver(dataSetObserver)
        }
        adapter = newAdapter
        itemCount = if (adapter != null) {
            adapter!!.registerDataSetObserver(dataSetObserver)
            adapter!!.getCount()
        } else {
            0
        }
        notifyChanged()
    }

    fun getAdapter(): KChartAdapter? {
        return adapter
    }

    private val dataSetObserver: DataSetObserver = object : DataSetObserver() {
        override fun onChanged() {
            itemCount = if (adapter == null) 0 else adapter!!.getCount()
            notifyChanged()
        }

        override fun onInvalidated() {
            itemCount = if (adapter == null) 0 else adapter!!.getCount()
            notifyChanged()
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(
            measureWidth(widthMeasureSpec),
            measureHeight(heightMeasureSpec).toInt()
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewHeight = h.toFloat()
        viewWidth = w.toFloat()
        mainRect.left = 0f
        mainRect.right = viewWidth - dpToPx(50f)
        mainRect.top = mainTopHeight
        mainRect.bottom = mainRectHeight + mainTopHeight
        if (getAdapter() != null)
            transformer.resetMatrix(
                mainRect,
                itemCount,
                itemWidth
            )
//        if (!overScroller.isFinished) overScroller.abortAnimation()
        calculateValue()
    }

    private fun measureWidth(measureSpec: Int): Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        return when (specMode) {
            MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED -> width
            MeasureSpec.EXACTLY -> specSize
            else -> 500
        }
    }

    private fun measureHeight(measureSpec: Int): Float {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec);
        return when (specMode) {
            MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED -> {
                mainRectHeight + timeRectHeight + mainTopHeight + childRect.size * (indexTopHeight + indexRectHeight) + dpToPx(
                    2f
                )
            }
            MeasureSpec.EXACTLY -> specSize.toFloat()
            else -> 500f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mainChartDraw?.drawGrid(this, canvas, mainRect)
        mainChartDraw?.drawYValue(this, canvas, mainRect)
        mainChartDraw?.drawIndexText(this, canvas, mainRect, longPressEvent)
        mainChartDraw?.drawXValue(this, canvas, mainRect)
        childChartDraw.entries.forEach {
            it.value.drawGrid(this, canvas, childRect[it.key]!!)
            it.value.drawYValue(this, canvas, childRect[it.key]!!)
            it.value.drawIndexText(this, canvas, childRect[it.key]!!, longPressEvent)
        }
        for (i in startIndex..stopIndex) {
            val lastPosition = if (i == 0) 0 else i - 1
            mainChartDraw?.draw(canvas, this, mainRect, i, lastPosition)
            childChartDraw.entries.forEach {
                it.value.draw(canvas, this, childRect[it.key]!!, i, lastPosition)
            }
        }
        mainChartDraw?.drawLongPressLine(this, canvas, mainRect, longPressEvent)
        childChartDraw.entries.forEach {
            it.value.drawLongPressLine(this, canvas, childRect[it.key]!!, longPressEvent)
        }
    }

    /**
     * 滑动到最左边
     */
    override fun farLeft() {
    }

    /**
     * 滑动到最右边
     */
    override fun farRight() {
    }


    /**
     * 重新计算并刷新线条
     */
    private fun notifyChanged() {
        if (mainRect != null)
            transformer.resetMatrix(
                mainRect,
                itemCount,
                itemWidth,
                getAdapter()!!.addToHeader
            )
        calculateValue()
    }

    fun getViewY(value: Float, maxValue: Float, viewScaleY: Float, top: Float): Float {
        return (maxValue - value) * viewScaleY + top
    }

    fun getValueY(value: Float, valueScaleY: Float, minValue: Float, bottom: Float): Float {
        return (bottom - value) * valueScaleY + minValue
    }

    override fun calculateValue() {
        startIndex = transformer.pixelsToValue(0f).toInt()
        stopIndex = transformer.pixelsToValue(mainRect.right + itemWidth * currentScaleX).toInt()
        if (startIndex < 0) {
            startIndex = 0
        }
        if (stopIndex >= itemCount) {
            stopIndex = itemCount - 1
        }
        if (mainChartDraw == null || itemCount <= 0) {
            return
        }
        //计算最大值最小值
        var mainMaxValue = mainChartDraw?.getMinValue(startIndex, this)!!
        var mainMinValue = mainMaxValue
        childRectMaxValue.clear()
        childRectMinValue.clear()
        for (i in startIndex..stopIndex) {
            mainMaxValue = max(mainMaxValue, mainChartDraw?.getMaxValue(i, this)!!)
            mainMinValue = min(mainMinValue, mainChartDraw?.getMinValue(i, this)!!)
            childChartDraw.entries.forEach {
                when {
                    childRectMaxValue[it.key] == null -> {
                        childRectMaxValue[it.key] = it.value?.getMaxValue(i, this)!!
                        childRectMinValue[it.key] = it.value?.getMinValue(i, this)!!
                    }
                    else -> {
                        childRectMaxValue[it.key] =
                            max(childRectMaxValue[it.key]!!, it.value?.getMaxValue(i, this)!!)
                        childRectMinValue[it.key] =
                            min(childRectMinValue[it.key]!!, it.value?.getMinValue(i, this)!!)
                    }
                }

            }
        }
        childRect.entries.forEach {
            val chartDraw = childChartDraw[it.key]!!
            calculateYRange(
                it.value,
                childRectMaxValue[it.key]!!,
                childRectMinValue[it.key]!!,
                chartDraw.maxValueFactor,
                chartDraw.minValueFactor,
                chartDraw
            )
        }
        calculateYRange(
            mainRect,
            mainMaxValue,
            mainMinValue,
            mainChartDraw!!.maxValueFactor,
            mainChartDraw!!.minValueFactor,
            mainChartDraw
        )
        //修复右边距离
//        mainRect.right =
//            (viewWidth - max(
//                mainChartDraw!!.measureTextWidth(
//                    mainChartDraw!!.getYValueFormatter().format(mainChartDraw!!.maxValue),
//                    mainChartDraw!!.yTextPaint
//                ),
//                mainChartDraw!!.measureTextWidth(
//                    mainChartDraw!!.getYValueFormatter().format(mainChartDraw!!.minValue),
//                    mainChartDraw!!.yTextP
//                )
//            )).roundToInt() - dpToPx(10f)

        childRect.entries.forEach {
            it.value.right = mainRect.right
        }
        invalidate()
    }

    override fun getChartWidth(): Float {
        return mainRect?.right
    }

    override fun getDataSize(): Int {
        return itemCount
    }

    private fun calculateYRange(
        rect: RectF,
        maxValue: Float,
        minValue: Float,
        maxValueFactor: Float,
        minValueFactor: Float,
        chartDraw: ChartDraw?
    ) {
        if (chartDraw == null) {
            return
        }
        var padding = maxValue - minValue
        var min = minValue - padding * minValueFactor
        var max = maxValue + padding * maxValueFactor

        var scaleY = rect.height() / (max - min)
        var valueScaleY = (max - min) / rect.height()

        chartDraw!!.maxValue = max
        chartDraw!!.minValue = min
        chartDraw!!.viewScaleY = scaleY
        chartDraw!!.valueScaleY = valueScaleY
    }

    private fun calculateNewIndexRect(): RectF {
        val rect = RectF()
        rect.left = mainRect.left
        rect.right = mainRect.right
        rect.top =
            mainRectHeight + timeRectHeight + mainTopHeight + childRect.size * (indexTopHeight + indexRectHeight) + indexTopHeight
        rect.bottom =
            mainRectHeight + timeRectHeight + mainTopHeight + (childRect.size + 1) * (indexTopHeight + indexRectHeight)
        return rect
    }

    private fun fixIndexRect() {
        var position = 0
        childRect.entries.forEach {
            it.value.top =
                mainRectHeight + timeRectHeight + mainTopHeight + position * (indexTopHeight + indexRectHeight) + indexTopHeight
            it.value.bottom =
                mainRectHeight + timeRectHeight + mainTopHeight + (position + 1) * (indexTopHeight + indexRectHeight)
            position++
        }
        requestLayout()
    }

    fun addChildChartDraw(key: String, chartDraw: ChartDraw) {
        childChartDraw[key] = chartDraw
        childRect[key] = calculateNewIndexRect()
        requestLayout()
    }

    fun getChildChartDraw(key: String): ChartDraw? {
        return childChartDraw[key]
    }

    fun removeChildChartDraw(key: String) {
        childChartDraw.remove(key)
        childRect.remove(key)
        fixIndexRect()
    }

}