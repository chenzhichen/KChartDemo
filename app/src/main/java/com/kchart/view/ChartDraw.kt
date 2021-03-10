package com.kchart.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect


abstract class ChartDraw {
    //最大值增幅
    open var maxValueFactor = 0f

    //最小值增幅
    var minValueFactor = 0f

    var maxValue = 0f
    var minValue = 0f
    var viewScaleY = 0f
    var valueScaleY = 0f
    val yTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 需要滑动 物体draw方法
     *
     * @param canvas canvas
     * @param view   k线图View
     */
    abstract fun draw(
        canvas: Canvas,
        view: KChartView,
        rect: Rect,
        curPosition: Int,
        lastPosition: Int
    )

    /**
     * 绘制边框
     */
    abstract fun drawGrid(view: KChartView, canvas: Canvas, rect: Rect)

    /**
     * 绘制Y轴坐标
     */
    abstract fun drawYValue(view: KChartView, canvas: Canvas, rect: Rect)

    /**
     * 绘制指标数据
     */
    abstract fun drawIndexText(view: KChartView, canvas: Canvas, rect: Rect, e: LongPressEvent?)

    /**
     * 绘制长按十字线
     */
    abstract fun drawLongPressLine(view: KChartView, canvas: Canvas, rect: Rect, e: LongPressEvent?)

    /**
     * 绘制X轴坐标
     */
    abstract fun drawXValue(view: KChartView, canvas: Canvas, rect: Rect)

    /**
     * 获取当前实体中最大的值
     *
     * @return
     */
    abstract fun getMaxValue(position: Int, view: KChartView): Float

    /**
     * 获取当前实体中最小的值
     *
     * @return
     */
    abstract fun getMinValue(position: Int, view: KChartView): Float

    /**
     * 测量文字宽度
     */
    abstract fun measureTextWidth(value: String, paint: Paint): Float


    /**
     * 获取Y轴value格式化器
     */
    abstract fun getYValueFormatter(): IValueFormatter

    /**
     * 获取Y轴value格式化器
     */
    abstract fun setYValueFormatter(formatter: IValueFormatter)

    /**
     *清除数据
     */
    abstract fun clearData()

}