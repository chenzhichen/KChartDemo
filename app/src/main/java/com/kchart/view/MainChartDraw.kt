package com.kchart.view

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.kchart.data.KLineBean
import com.kchart.util.dpToPx
import com.kchart.util.spToPx
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class MainChartDraw : IndexChartDraw() {


    private val timeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val mainIndex = arrayListOf("开:", "高:", "低:", "收:")
    private var simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm")

    init {
        timeTextPaint.textSize = spToPx(9f)
    }

    override fun draw(
        canvas: Canvas,
        view: KChartView,
        rect: Rect,
        curPosition: Int,
        lastPosition: Int
    ) {
        val currentPointX = view.translateXtoX(view.getX(curPosition))
        val lastPositionX = view.translateXtoX(view.getX(lastPosition))
        canvas.save()
        canvas.clipRect(rect)
        drawCandle(view, canvas, currentPointX, view.getAdapter()!!.getItem(curPosition), rect)
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


    override fun getMaxValue(position: Int, view: KChartView): Float {
        var kLineBean = view.getAdapter()!!.getItem(position)
        return max(kLineBean.high, getLineDataMaxValue(position))
    }

    override fun getMinValue(position: Int, view: KChartView): Float {
        var kLineBean = view.getAdapter()!!.getItem(position)
        return min(kLineBean.low, getLineDataMinValue(position))
    }


    /**
     * 画Candle
     *
     * @param canvas
     * @param x      x轴坐标
     */
    private fun drawCandle(
        view: KChartView,
        canvas: Canvas,
        x: Float,
        candleEntry: KLineBean,
        rect: Rect
    ) {
        val high = view.getViewY(candleEntry.high, maxValue, viewScaleY, rect.top.toFloat())
        val low = view.getViewY(candleEntry.low, maxValue, viewScaleY, rect.top.toFloat())
        val open = view.getViewY(candleEntry.open, maxValue, viewScaleY, rect.top.toFloat())
        val close = view.getViewY(candleEntry.close, maxValue, viewScaleY, rect.top.toFloat())
        val r = candleWidth * view.currentScaleX / 2f
        when {
            //下跌
            candleEntry.open > candleEntry.close -> {
                if (abs(open - close) < 1) {
                    canvas.drawLine(x - r, close, x + r, close, fallPaint)
                } else {
                    canvas.drawRect(x - r, open, x + r, close, fallPaint)
                }
                canvas.drawLine(x, high, x, low, fallPaint)
            }
            //上涨
            candleEntry.open < candleEntry.close -> {
                //实心
                if (view.candleSolid) {
                    if (abs(open - close) < 1) {
                        canvas.drawLine(x - r, close, x + r, close, risePaint)
                    } else {
                        canvas.drawRect(x - r, close, x + r, open, risePaint)
                    }
                    canvas.drawLine(x, high, x, low, risePaint)
                } else {
                    canvas.drawLine(x, high, x, close, risePaint)
                    canvas.drawLine(x, open, x, low, risePaint)
                    canvas.drawLine(x - r, open, x - r, close, risePaint)
                    canvas.drawLine(x + r, open, x + r, close, risePaint)
                    canvas.drawLine(x - r, open, x + r, open, risePaint)
                    canvas.drawLine(x - r, close, x + r, close, risePaint)
                }
            }
            else -> {
                canvas.drawRect(x - r, open, x + r, close + 1, risePaint)
                canvas.drawLine(x, high, x, low, risePaint)
            }

        }
    }

    override fun drawGrid(view: KChartView, canvas: Canvas, rect: Rect) {
        canvas.drawLine(0f, 0f, view.viewWidth.toFloat(), 0f, gridLinePaint)
        canvas.drawLine(
            rect.right.toFloat(),
            0f,
            rect.right.toFloat(),
            rect.bottom.toFloat(),
            gridLinePaint
        )

        //横向的grid
        val rowSpace: Float = (rect.height() / (view.gridRows - 1)).toFloat()
        for (i in 0 until view.gridRows) {
            canvas.drawLine(
                0f,
                rowSpace * i + rect.top,
                rect.right.toFloat(),
                rowSpace * i + rect.top,
                gridLinePaint
            )
        }
    }

    override fun drawYValue(view: KChartView, canvas: Canvas, rect: Rect) {
        val rowSpace: Float = (rect.height() / (view.gridRows - 1)).toFloat()
        for (i in 0 until view.gridRows) {
            val value = rowSpace * i + rect.top
            var y = when (i) {
                view.gridRows - 1 -> value
                else -> fixTextY(value, yTextPaint)
            }
            drawTextYValue(
                view,
                canvas,
                rect,
                value,
                y
            )
        }
    }

    override fun drawXValue(
        view: KChartView,
        canvas: Canvas,
        rect: Rect
    ) {
        var space = rect.right / (view.gridColumns - 1)
        for (i in 0 until view.gridColumns) {
            var x = (space * i).toFloat()
            var position = view.indexOfTranslateX(view.xToTranslateX(x))
            var time =
                view.getAdapter()!!.getDate(position)
            val width = measureTextWidth(time, timeTextPaint)
            when (i) {
                0 -> x += dpToPx(2f)
                view.gridColumns - 1 -> x -= width + dpToPx(2f)
                else -> {
                    x -= width / 2
                }
            }
            canvas.drawText(
                time,
                x,
                fixTextY(rect.bottom.toFloat() + view.timeRectHeight.toFloat() / 2f, timeTextPaint),
                timeTextPaint
            )
        }
    }

    override fun drawIndexText(
        view: KChartView,
        canvas: Canvas,
        rect: Rect,
        longPressEvent: LongPressEvent?
    ) {
        indexTextPaint.color = Color.parseColor("#3b4169")

        var x = dpToPx(5f).toFloat()
        var y = dpToPx(5f).toFloat() + measureTextHeight(indexTextPaint)

        if (longPressEvent != null) {
            var position = longPressEvent.position
            val kLineBean = view.getAdapter()!!.getItem(position)
            val time = simpleDateFormat.format(Date(kLineBean.date))
            canvas.drawText(time, x, y, indexTextPaint)
            x += measureTextWidth(time, indexTextPaint) + dpToPx(5f).toFloat()

            mainIndex[0] = "开:" + getYValueFormatter().format(kLineBean.open)
            mainIndex[1] = "高:" + getYValueFormatter().format(kLineBean.high)
            mainIndex[2] = "低:" + getYValueFormatter().format(kLineBean.low)
            mainIndex[3] = "收:" + getYValueFormatter().format(kLineBean.close)

            mainIndex.forEach {
                if (x + measureTextWidth(it, indexTextPaint) > rect.right) {
                    x = dpToPx(5f).toFloat()
                    y += dpToPx(2f).toFloat() + measureTextHeight(indexTextPaint)
                }
                canvas.drawText(it, x, y, indexTextPaint)
                x += measureTextWidth(it, indexTextPaint) + dpToPx(5f).toFloat()
            }

            x = dpToPx(5f).toFloat()
            y += dpToPx(2f).toFloat() + measureTextHeight(indexTextPaint)
        }

        drawLineIndexText(
            x,
            y,
            canvas,
            rect,
            longPressEvent
        )
    }

    override fun drawLongPressLine(
        view: KChartView,
        canvas: Canvas,
        rect: Rect,
        longPressEvent: LongPressEvent?
    ) {
        if (longPressEvent != null) {
            var x = view.translateXtoX(view.getX(longPressEvent.position))
            if (longPressEvent.isLongPress) {
                when {
                    x < rect.left -> {
                        x = rect.left.toFloat()
                    }
                    x > rect.right -> {
                        x = rect.right.toFloat()
                    }
                }
                var position = view.indexOfTranslateX(view.xToTranslateX(x))
                x = view.translateXtoX(view.getX(position))

                when {
                    x <= rect.left -> {
                        position++
                        x = view.translateXtoX(view.getX(position))
                    }
                    x >= rect.right -> {
                        position--
                        x = view.translateXtoX(view.getX(position))
                    }
                }
                longPressEvent.position = position
                drawVerticalLine(canvas, x, view, rect, position)
                super.drawLongPressLine(view, canvas, rect, longPressEvent)
            } else {
                if (x >= rect.left && x <= rect.right) {
                    drawVerticalLine(canvas, x, view, rect, longPressEvent.position)
                    super.drawLongPressLine(view, canvas, rect, longPressEvent)
                }
            }
        }
    }

    private fun drawVerticalLine(
        canvas: Canvas,
        x: Float,
        view: KChartView,
        rect: Rect,
        position: Int
    ) {
        canvas.drawLine(x, 0f, x, view.viewHeight.toFloat(), pressLinePaint)
        indexBackgroundRect.top = rect.bottom
        indexBackgroundRect.bottom = rect.bottom + view.timeRectHeight
        val time = simpleDateFormat.format(Date(view.getAdapter()!!.getData()[position].date))
        val with = measureTextWidth(time, yTextPaint)
        indexBackgroundRect.left = (x - with / 2).toInt() - dpToPx(5f)
        indexBackgroundRect.right = (x + with / 2).toInt() + dpToPx(5f)
        var x = x - with / 2
        if (indexBackgroundRect.left < 0) {
            indexBackgroundRect.left = 0
            indexBackgroundRect.right = (with + dpToPx(10f)).toInt()
            x = dpToPx(5f).toFloat()
        }

        if (indexBackgroundRect.right > view.viewWidth) {
            indexBackgroundRect.left = (view.viewWidth - with - dpToPx(10f)).toInt()
            indexBackgroundRect.right = view.viewWidth
            x = view.viewWidth - dpToPx(5f).toFloat() - with
        }
        canvas.drawRect(indexBackgroundRect, indexBackgroundPaint)
        canvas.drawText(
            time,
            x,
            fixTextY(indexBackgroundRect.exactCenterY(), yTextPaint),
            yTextPaint
        )
    }

    override fun clearData() {
        super.clearData()
    }


}