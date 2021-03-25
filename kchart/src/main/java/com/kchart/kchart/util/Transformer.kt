package com.kchart.kchart.util

import android.graphics.Matrix
import android.graphics.RectF


class Transformer {
    private val matrixValue = Matrix() // 把值映射到屏幕像素的矩阵

    private val matrixTouch = Matrix() // 缩放和平移矩阵

    private val matrixOffset = Matrix() // 偏移矩阵

    private val matrixInvert = Matrix() // 用于缓存反转矩阵

    private var touchValues = FloatArray(9) // 存储缩放和平移信息

    private var ptsBuffer = FloatArray(2)

    private var maxScrollOffset = 0f// 最大滚动量

    private var minScrollOffset = 0f// 最小滚动量

    private var scaleX = 1f
    private var chartWidth = 0f
    private var itemWidth = 0f
    private var dataSize = 0


    fun resetMatrix(viewRect: RectF, count: Int, itemWidth: Float) {
        if (count <= 0 || viewRect == null || viewRect.width() <= 0 || chartWidth != 0f) {
            return
        }
        chartWidth = viewRect.width()
        dataSize = count
        this.itemWidth = itemWidth
        val visibleCount = chartWidth / itemWidth.toInt()
        initMatrixValue(chartWidth, count.toFloat())
        initMatrixOffset(viewRect.left, viewRect.top)
        initMatrixTouch(count.toFloat(), visibleCount)
    }

    /**
     * 初始化值矩阵
     */
    private fun initMatrixValue(chartWidth: Float, count: Float) {
        matrixValue.reset()
        matrixValue.postScale(chartWidth / count, 1f)
    }

    /**
     * 偏移矩阵运算
     *
     * @param offsetY 偏移量 Y
     */
    private fun initMatrixOffset(offsetX: Float, offsetY: Float) {
        matrixOffset.reset()
        matrixOffset.postTranslate(offsetX, offsetY)
    }

    /**
     * 手势滑动缩放矩阵运算
     *
     */
    private fun initMatrixTouch(count: Float, visibleCount: Float) {
        touchValues = FloatArray(9)
        matrixTouch.reset()
        matrixTouch.postScale(count / visibleCount, 1f)

        computeScrollRange()
        scroll(maxScrollOffset)
    }

    /**
     * 利用矩阵将 entry 的值映射到屏幕像素上
     *
     */
    fun valueToPixels(x: Float): Float {
        ptsBuffer[0] = x
        matrixValue.mapPoints(ptsBuffer)
        matrixTouch.mapPoints(ptsBuffer)
        matrixOffset.mapPoints(ptsBuffer)
        return ptsBuffer[0]
    }

    /**
     * 将基于屏幕像素的坐标反转成 entry 的值
     *
     */
    fun pixelsToValue(x: Float): Float {
        ptsBuffer[0] = x
        matrixInvert.reset()
        matrixOffset.invert(matrixInvert)
        matrixInvert.mapPoints(ptsBuffer)
        matrixTouch.invert(matrixInvert)
        matrixInvert.mapPoints(ptsBuffer)
        matrixValue.invert(matrixInvert)
        matrixInvert.mapPoints(ptsBuffer)
        return ptsBuffer[0]
    }


    /**
     * 更新当前滚动量，当滚动到边界时将不能再滚动
     *
     * @param dx 变化量
     */
    fun scroll(dx: Float) {
        matrixTouch.getValues(touchValues)
        touchValues[Matrix.MTRANS_X] += -dx
        if (touchValues[Matrix.MTRANS_X] < -maxScrollOffset) {
            touchValues[Matrix.MTRANS_X] = -maxScrollOffset
        } else if (touchValues[Matrix.MTRANS_X] > minScrollOffset) {
            touchValues[Matrix.MTRANS_X] = minScrollOffset
        }
        matrixTouch.setValues(touchValues)
    }

    /**
     * 缩放
     *
     * @param factor  缩放因子
     */
    fun zoom(factor: Float, x: Float) {
        scaleX *= factor
        val position = pixelsToValue(x)
        matrixTouch.getValues(touchValues)
        touchValues[Matrix.MSCALE_X] *= factor
        matrixTouch.setValues(touchValues)
        val preX = valueToPixels(position)
        computeScrollRange()
        scroll(preX - x)
    }

    /**
     * 计算当前缩放下，X 轴方向的最小滚动值和最大滚动值
     */
    private fun computeScrollRange() {
        minScrollOffset = itemWidth / 2 * scaleX
        val visibleCount = chartWidth / itemWidth / scaleX
        maxScrollOffset = if (visibleCount >= dataSize) {
            0f
        } else {
            dataSize * itemWidth * scaleX - chartWidth - minScrollOffset
        }
    }
}