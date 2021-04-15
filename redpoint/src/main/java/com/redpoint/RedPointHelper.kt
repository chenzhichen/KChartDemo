package com.redpoint

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.kchart.rxjava.R


class RedPointHelper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    companion object {
        @JvmStatic
        val TYPE_RADIUS = "radius"

        @JvmStatic
        val TYPE_OFFSET_X = "offset_x"

        @JvmStatic
        val TYPE_OFFSET_Y = "offset_y"
    }

    private val DEFAULT_RADIUS = 5F
    private var ids = mutableListOf<Int>()
    private var rects = mutableListOf<Rect>()
    private var lastRects = mutableListOf<Rect>()

    private lateinit var offsetXs: MutableList<Float>
    private lateinit var offsetYs: MutableList<Float>
    private lateinit var radiuses: MutableList<Float>
    private var bgPaint: Paint = Paint()

    init {
        readAttrs(attrs)
        initPaint()
    }

    private fun initPaint() {
        bgPaint.apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.parseColor("#ff0000")
        }
    }

    private fun readAttrs(attributeSet: AttributeSet?) {
        attributeSet?.let { attrs ->
            context.obtainStyledAttributes(attrs, R.styleable.RedPointHelper)?.let {
                divideIds(it.getString(R.styleable.RedPointHelper_reference_ids))
                divideRadius(it.getString(R.styleable.RedPointHelper_reference_radius))
                dividerOffsets(
                    it.getString(R.styleable.RedPointHelper_reference_offsetX),
                    it.getString(R.styleable.RedPointHelper_reference_offsetY)
                )
                it.recycle()
            }
        }
    }

    fun drawRedPoint(redPointLayout: RedPointLayout, canvas: Canvas?) {
        ids.forEachIndexed { index, id ->
            redPointLayout.findViewById<View>(id)?.let { _ ->
                val cx = rects[index].right + offsetXs.getOrElse(index) { 0F }.dp2px()
                val cy = rects[index].top + offsetYs.getOrElse(index) { 0F }.dp2px()
                val radius = radiuses.getOrElse(index) { DEFAULT_RADIUS }.dp2px()
                canvas?.drawCircle(cx, cy, radius, bgPaint)
            }
        }
    }

    private fun dividerOffsets(offsetXString: String?, offsetYString: String?) {
        offsetXs = mutableListOf()
        offsetYs = mutableListOf()
        offsetXString?.split(",")?.forEach { offset -> offsetXs.add(offset.trim().toFloat()) }
        offsetYString?.split(",")?.forEach { offset -> offsetYs.add(offset.trim().toFloat()) }
    }

    private fun divideRadius(radiusString: String?) {
        radiuses = mutableListOf()
        radiusString?.split(",")?.forEach { radius -> radiuses.add(radius.trim().toFloat()) }
    }

    private fun Float.dp2px(): Float {
        val scale = Resources.getSystem().displayMetrics.density
        return this * scale + 0.5f
    }

    fun setValue(id: Int, type: String, value: Float) {
        val dirtyIndex = ids.indexOf(id)
        if (dirtyIndex != -1) {
            when (type) {
                TYPE_OFFSET_X -> offsetXs[dirtyIndex] = value
                TYPE_OFFSET_Y -> offsetYs[dirtyIndex] = value
                TYPE_RADIUS -> radiuses[dirtyIndex] = value
            }
            (parent as? RedPointLayout)?.postInvalidate()
        }
    }

    fun updateLayout(redPointLayout: RedPointLayout) {

    }

    fun onPreDraw(redPointLayout: RedPointLayout) {
        ids.forEachIndexed { index, id ->
            redPointLayout.findViewById<View>(id)?.let { v ->
                v.getHitRect(rects[index])
                if (rects[index] != lastRects[index]) {
                    redPointLayout.postInvalidate()
                    lastRects[index].set(rects[index])
                }
            }
        }
    }


    private fun divideIds(idString: String?) {
        idString?.split(",")?.forEach { id ->
            ids.add(resources.getIdentifier(id.trim(), "id", context.packageName))
            rects.add(Rect())
            lastRects.add(Rect())
        }
    }
}