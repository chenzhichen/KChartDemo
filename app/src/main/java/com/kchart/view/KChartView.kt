package com.kchart.view

import android.content.Context
import android.database.DataSetObserver
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import com.kchart.util.dpToPx
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


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
    var mainRect = Rect()

    //绘制指标副图
    private val childChartDraw = LinkedHashMap<String, ChartDraw>()
    private val childRect = LinkedHashMap<String, Rect>()

    private val childRectMinValue = LinkedHashMap<String, Float>()
    private val childRectMaxValue = LinkedHashMap<String, Float>()

    private var translateX = 0f
    var startIndex = 0
    var stopIndex = 0

    //蜡状图是否实心
    var candleSolid = false

    var gridRows = 5
    var gridColumns = 3

    var viewHeight = 0
    var viewWidth = 0

    private var itemCount = 0
    private var datasLen = 0f
    private var itemWidth = dpToPx(5f).toFloat()
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
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewHeight = h
        viewWidth = w
        setTranslateXFromScrollX(scrollXDistance)
        mainRect.left = 0
        mainRect.right = viewWidth - dpToPx(30f)
        mainRect.top = mainTopHeight
        mainRect.bottom = mainRectHeight + mainTopHeight
        checkAndFixScrollX()
        setTranslateXFromScrollX(scrollXDistance)
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

    private fun measureHeight(measureSpec: Int): Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec);
        return when (specMode) {
            MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED -> {
                mainRectHeight + timeRectHeight + mainTopHeight + childRect.size * (indexTopHeight + indexRectHeight) + dpToPx(
                    2f
                )
            }
            MeasureSpec.EXACTLY -> specSize
            else -> 500
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

    override fun onScaleChanged() {
        checkAndFixScrollX()
        setTranslateXFromScrollX(scrollXDistance)
    }

    override fun getMinScrollX(): Float {
        return 0f
    }

    override fun getMaxScrollX(): Float {
        return getMaxTranslateX() - getMinTranslateX()
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
        if (itemCount != 0) {
            datasLen = (itemCount - 1) * itemWidth
            checkAndFixScrollX()
            setTranslateXFromScrollX(scrollXDistance)
        } else {
            scrollX = 0
        }
        calculateValue()
    }


    /**
     * scrollX 转换为 TranslateX
     *
     * @param scrollX
     */
    private fun setTranslateXFromScrollX(scrollX: Float) {
        translateX =
            if (datasLen * currentScaleX <= mainRect.right) itemWidth / 2f else scrollX + getMinTranslateX()
    }

    /**
     * 计算往左最多还可滑动的距离
     *
     * @return
     */
    private fun getMinTranslateX(): Float {
        return -datasLen + mainRect.right / currentScaleX - itemWidth / 2f
    }

    /**
     * 计算往右最多还可滑动的距离
     *
     * @return
     */
    private fun getMaxTranslateX(): Float {
        return itemWidth / 2f
    }

    /**
     * view中的x转化为TranslateX
     *
     * @param x
     * @return
     */
    override fun xToTranslateX(x: Float): Float {
        return -translateX + x / currentScaleX
    }

    /**
     * translateX转化为view中的x
     *
     * @param translate
     * @return
     */
    override fun translateXtoX(translate: Float): Float {
        return (translate + translateX) * currentScaleX
    }

    /**
     * 根据索引索取x坐标
     *
     * @param position 索引值
     * @return
     */
    override fun getX(position: Int): Float {
        return position * itemWidth
    }

    /**
     * 二分查找当前值的index
     *
     * @return
     */
    private fun indexOfTranslateX(translateX: Float, start: Int, end: Int): Int {
        if (start < 0 || end < 0) {
            return 0
        }
        if (end == start) {
            return start
        }
        if (end - start == 1) {
            val startValue = getX(start)
            val endValue = getX(end)
            return if (abs(translateX - startValue) < abs(translateX - endValue)) start else end
        }
        val mid = start + (end - start) / 2
        val midValue = getX(mid)
        return when {
            translateX < midValue -> {
                indexOfTranslateX(translateX, start, mid)
            }
            translateX > midValue -> {
                indexOfTranslateX(translateX, mid, end)
            }
            else -> {
                mid
            }
        }
    }

    override fun indexOfTranslateX(translateX: Float): Int {
        return indexOfTranslateX(translateX, 0, itemCount - 1)
    }

    fun getViewY(value: Float, maxValue: Float, viewScaleY: Float, top: Float): Float {
        return (maxValue - value) * viewScaleY + top
    }

    fun getValueY(value: Float, valueScaleY: Float, minValue: Float, bottom: Float): Float {
        return (bottom - value) * valueScaleY + minValue
    }

    override fun calculateValue() {
        startIndex = indexOfTranslateX(xToTranslateX(0f))
        stopIndex = indexOfTranslateX(xToTranslateX(mainRect.right.toFloat()))
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
        mainRect.right =
            (viewWidth - max(
                mainChartDraw!!.measureTextWidth(
                    mainChartDraw!!.getYValueFormatter().format(mainChartDraw!!.maxValue),
                    mainChartDraw!!.yTextPaint
                ),
                mainChartDraw!!.measureTextWidth(
                    mainChartDraw!!.getYValueFormatter().format(mainChartDraw!!.minValue),
                    mainChartDraw!!.yTextPaint
                )
            )).roundToInt() - dpToPx(10f)
        childRect.entries.forEach {
            it.value.right = mainRect.right
        }
        checkAndFixScrollX()
        setTranslateXFromScrollX(scrollXDistance)
        invalidate()
    }

    private fun calculateYRange(
        rect: Rect,
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

    private fun calculateNewIndexRect(): Rect {
        val rect = Rect()
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

    fun removeChildChartDraw(key: String) {
        childChartDraw.remove(key)
        childRect.remove(key)
        fixIndexRect()
    }

}