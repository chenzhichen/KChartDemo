package com.redpoint

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout


class RedPointLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var redPointHelpers = mutableListOf<RedPointHelper>()
    private var onPreDrawListener: ViewTreeObserver.OnPreDrawListener =
        ViewTreeObserver.OnPreDrawListener {
            redPointHelpers.forEach { helper -> helper.onPreDraw(this) }
            true
        }

    init {
        setWillNotDraw(false)
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        if (child is RedPointHelper) {
            redPointHelpers.add(child)
        }
    }

    override fun onViewRemoved(child: View) {
        super.onViewRemoved(child)
        if (child is RedPointHelper) {
            redPointHelpers.remove(child)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnPreDrawListener(onPreDrawListener)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        redPointHelpers.forEach { helper -> helper.updateLayout(this) }
    }

    override fun onDrawForeground(canvas: Canvas?) {
        super.onDrawForeground(canvas)
        redPointHelpers.forEach { helper -> helper.drawRedPoint(this, canvas) }
    }
}