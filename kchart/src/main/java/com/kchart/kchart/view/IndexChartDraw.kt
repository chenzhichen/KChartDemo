package com.kchart.kchart.view

import android.graphics.*
import android.text.TextUtils
import com.kchart.kchart.data.BarEntry
import com.kchart.kchart.data.LineEntry
import com.kchart.kchart.util.dpToPx
import com.kchart.kchart.util.spToPx
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


open class IndexChartDraw : ChartDraw() {
    var candleWidth = dpToPx(3f)
    var isMACD = false

    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val fallPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val risePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val indexTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val pressLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val indexBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    val indexBackgroundRect = RectF()

    private val bar = Path()


    var lineData: HashMap<String, LinkedHashMap<String, ArrayList<LineEntry>>> = LinkedHashMap()
    var barData: HashMap<String, List<BarEntry>> = LinkedHashMap()

    private var yValueFormatter: IValueFormatter = ValueFormatter()

    init {
        fallPaint.color = Color.parseColor("#DE4D42")
        risePaint.color = Color.parseColor("#35AB71")
        gridLinePaint.color = Color.parseColor("#a0a1ad")
        gridLinePaint.strokeWidth = 1f
        gridLinePaint.style = Paint.Style.STROKE
        yTextPaint.textSize = spToPx(9f)
        indexTextPaint.textSize = spToPx(9f)

        pressLinePaint.strokeWidth = 1f
        pressLinePaint.style = Paint.Style.STROKE
        pressLinePaint.color = Color.parseColor("#a0a1ad")

        indexBackgroundPaint.color = Color.parseColor("#cfd1e3")

    }

    override fun draw(
        canvas: Canvas,
        view: KChartView,
        rect: RectF,
        curPosition: Int,
        lastPosition: Int
    ) {
        val currentPointX = view.transformer.valueToPixels(curPosition.toFloat())
        val lastPositionX = view.transformer.valueToPixels(lastPosition.toFloat())
        canvas.save()
        canvas.clipRect(rect)
        drawBar(canvas, curPosition, currentPointX, view, rect)
        drawLine(
            canvas,
            linePaint,
            currentPointX,
            curPosition,
            lastPositionX,
            lastPosition,
            view,
            rect
        )
        canvas.restore()
    }

    override fun drawGrid(view: KChartView, canvas: Canvas, rect: RectF) {
        canvas.drawRect(rect, gridLinePaint)
        canvas.drawLine(
            0f,
            rect.centerY(),
            rect.right,
            rect.centerY(),
            gridLinePaint
        )
    }

    override fun drawYValue(view: KChartView, canvas: Canvas, rect: RectF) {
        drawTextYValue(
            view,
            canvas,
            rect,
            rect.top,
            rect.top + measureTextHeight(yTextPaint) / 2
        )
        drawTextYValue(
            view,
            canvas,
            rect,
            rect.centerY(),
            fixTextY(rect.centerY(), yTextPaint)
        )
        drawTextYValue(
            view,
            canvas,
            rect,
            rect.bottom,
            rect.bottom
        )

    }

    open fun drawTextYValue(
        view: KChartView,
        canvas: Canvas,
        rect: RectF,
        value: Float,
        y: Float
    ) {
        val text = getYValueFormatter().format(
            view.getValueY(
                value,
                valueScaleY,
                minValue,
                rect.bottom
            )
        )
        canvas.drawText(
            text,
            (view.viewWidth - rect.right - measureTextWidth(
                text,
                yTextPaint
            )) / 2f + rect.right,
            y,
            yTextPaint
        )
    }

    override fun drawIndexText(
        view: KChartView,
        canvas: Canvas,
        rect: RectF,
        longPressEvent: LongPressEvent?
    ) {
        indexTextPaint.color = Color.parseColor("#3b4169")
        var x = dpToPx(5f)
        x = drawVolIndexText(x, view, canvas, rect, longPressEvent)
        drawLineIndexText(
            x,
            fixTextY(rect.top - view.indexTopHeight / 2f, indexTextPaint),
            canvas,
            rect,
            longPressEvent
        )
    }

    override fun drawLongPressLine(
        view: KChartView,
        canvas: Canvas,
        rect: RectF,
        e: LongPressEvent?
    ) {
        if (e == null) {
            return
        }
        if ((e.y >= rect.top && e.y <= rect.bottom) || view.isLocking) {
            if (view.isLocking) {
                e.y = view.getViewY(
                    view.getAdapter()!!.getData()[e.position].close,
                    maxValue,
                    viewScaleY,
                    rect.top
                )
            }
            canvas.drawLine(0f, e.y, rect.right, e.y, pressLinePaint)

            indexBackgroundRect.left = rect.right
            indexBackgroundRect.right = view.viewWidth
            val value = getYValueFormatter().format(
                view.getValueY(
                    e.y,
                    valueScaleY,
                    minValue,
                    rect.bottom
                )
            )
            val height = measureTextHeight(yTextPaint)
            indexBackgroundRect.top = e.y - height / 2 - dpToPx(2f)
            indexBackgroundRect.bottom = e.y + height / 2 + dpToPx(2f)

            canvas.drawRect(indexBackgroundRect, indexBackgroundPaint)
            canvas.drawText(
                value,
                rect.right + (view.viewWidth - rect.right - measureTextWidth(
                    value,
                    yTextPaint
                )) / 2,
                fixTextY(indexBackgroundRect.centerY(), yTextPaint),
                yTextPaint
            )
        }
    }

    override fun drawXValue(view: KChartView, canvas: Canvas, rect: RectF) {
    }

    private fun drawVolIndexText(
        x: Float,
        view: KChartView,
        canvas: Canvas,
        rect: RectF,
        longPressEvent: LongPressEvent?
    ): Float {
        var x = x
        barData.entries.forEach {
            val indexName = if (isMACD) {
                it.key
            } else {
                it.key + ": " + getYValueFormatter().format(
                    if (longPressEvent != null)
                        it.value[longPressEvent!!.position].y
                    else
                        it.value.last().y
                )
            }
            canvas.drawText(
                indexName,
                x,
                fixTextY(rect.top - view.indexTopHeight / 2f, indexTextPaint),
                indexTextPaint
            )
            x += dpToPx(5f) + measureTextWidth(indexName, indexTextPaint)
        }
        return x
    }

    fun drawLineIndexText(
        x: Float,
        y: Float,
        canvas: Canvas,
        rect: RectF,
        longPressEvent: LongPressEvent?
    ) {
        var default = x
        var x: Float
        var y = y
        lineData.entries.forEach { it ->
            x = default
            if (!TextUtils.isEmpty(it.key)) {
                indexTextPaint.color = Color.parseColor("#3b4169")
                val indexName = it.key
                canvas.drawText(
                    indexName,
                    x,
                    y,
                    indexTextPaint
                )
                x += dpToPx(5f) + measureTextWidth(indexName, indexTextPaint)
            }
            it.value.entries.forEach {
                indexTextPaint.color = it.value.last().color
                val value =
                    if (longPressEvent == null)
                        getYValueFormatter().format(it.value.last().y)
                    else {
                        val entity =
                            it.value[longPressEvent!!.position]
                        if (entity.x >= 0 || isMACD) {
                            getYValueFormatter().format(entity.y)
                        } else {
                            "- -"
                        }
                    }
                val indexName = it.key + ": " + value
                if (x + measureTextWidth(indexName, indexTextPaint) > rect.right) {
                    x = dpToPx(5f)
                    y += measureTextHeight(indexTextPaint) + dpToPx(5f)
                }
                canvas.drawText(
                    indexName,
                    x,
                    y,
                    indexTextPaint
                )
                x += dpToPx(5f) + measureTextWidth(indexName, indexTextPaint)
            }
            y += measureTextHeight(indexTextPaint)
        }
    }


    fun drawLine(
        canvas: Canvas,
        paint: Paint,
        stopX: Float,
        curPosition: Int,
        startX: Float,
        lastPosition: Int,
        view: KChartView,
        rect: RectF
    ) {
        lineData.entries.forEach { it ->
            it.value.entries.forEach {
                val startEntry = it.value[lastPosition]
                val stopEntry = it.value[curPosition]
                paint.color = startEntry.color
                if (stopEntry.x >= 0 && startEntry.x >= 0) {
                    canvas.drawLine(
                        startX,
                        view.getViewY(startEntry.y, maxValue, viewScaleY, rect.top),
                        stopX,
                        view.getViewY(stopEntry.y, maxValue, viewScaleY, rect.top),
                        paint
                    )
                }
            }
        }
    }

    open fun drawBar(
        canvas: Canvas,
        position: Int,
        x: Float,
        view: KChartView,
        rect: RectF
    ) {
        val r: Float = candleWidth * view.currentScaleX / 2
        barData.entries.forEach {
            val entry = it.value[position]
            var top = view.getViewY(entry.y, maxValue, viewScaleY, rect.top)
            var bottom = view.getViewY(0f, maxValue, viewScaleY, rect.top)
            bar.reset()
            bar.moveTo(x - r, top)
            bar.lineTo(x + r, top)
            bar.lineTo(x + r, bottom)
            bar.lineTo(x - r, bottom)
            bar.close()
            risePaint.style = if (view.candleSolid) Paint.Style.FILL else Paint.Style.STROKE
            fallPaint.style = Paint.Style.FILL
            if (entry.up) {
                canvas.drawPath(bar, risePaint)
            } else {
                canvas.drawPath(bar, fallPaint)
            }
        }

    }

    override fun getMaxValue(position: Int, view: KChartView): Float {
        return max(getLineDataMaxValue(position), getBarDataMaxValue(position))
    }

    override fun getMinValue(position: Int, view: KChartView): Float {
        return min(getLineDataMinValue(position), getBarDataMinValue(position))
    }

    open fun getLineDataMaxValue(position: Int): Float {
        var max = Float.MIN_VALUE
        if (lineData.size == 0) {
            return max
        }
        lineData.entries.forEach { it ->
            it.value.entries.forEach {
                val entry = it.value[position]
                if (entry.x >= 0) {
                    max = max(max, entry.y)
                }
            }
        }
        return max
    }

    open fun getLineDataMinValue(position: Int): Float {
        var min = Float.MAX_VALUE
        if (lineData.size == 0) {
            return min
        }
        lineData.entries.forEach { it ->
            it.value.entries.forEach {
                val entry = it.value[position]
                if (entry.x >= 0) {
                    min = min(min, entry.y)
                }
            }
        }
        return min
    }

    open fun getBarDataMaxValue(position: Int): Float {
        var max = Float.MIN_VALUE
        if (barData.size == 0) {
            return max
        }
        barData.entries.forEach {
            val entry = it.value[position]
            if (entry.x >= 0) {
                max = max(max, entry.y)
            }
        }
        return max
    }

    open fun getBarDataMinValue(position: Int): Float {
        var min = Float.MAX_VALUE
        if (barData.size == 0) {
            return min
        }
        barData.entries.forEach {
            val entry = it.value[position]
            if (entry.x >= 0) {
                min = min(min, entry.y)
            }
        }
        return min
    }

    /**
     * 解决text居中的问题
     */
    open fun fixTextY(y: Float, paint: Paint): Float {
        val fontMetrics: Paint.FontMetrics = paint.fontMetrics
        return y + (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent
    }

    open fun measureTextHeight(paint: Paint): Float {
        val fontMetrics: Paint.FontMetrics = paint.fontMetrics
        return fontMetrics.descent - fontMetrics.ascent
    }

    override fun measureTextWidth(value: String, paint: Paint): Float {
        return paint.measureText(value)
    }

    override fun getYValueFormatter(): IValueFormatter {
        return yValueFormatter
    }

    override fun setYValueFormatter(formatter: IValueFormatter) {
        yValueFormatter = formatter
    }

    override fun clearData() {
        lineData.clear()
        barData.clear()
    }

}