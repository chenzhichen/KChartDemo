package com.kchart.kchart.util

import android.app.Activity
import android.graphics.Rect
import com.kchartdemo.base.MyApplication
import kotlin.math.roundToInt


fun dpToPx(dp: Float): Float {
    val density: Float =
        MyApplication.instance.resources.displayMetrics.density
    return dp * density
}

fun spToPx(spValue: Float): Float {
    val fontScale = MyApplication.instance.resources.displayMetrics.scaledDensity
    return spValue * fontScale + 0.5f
}

fun getScreenSize(activity: Activity): Rect {
    val rect = Rect()
    activity.window.decorView.getWindowVisibleDisplayFrame(rect)
    return rect

}