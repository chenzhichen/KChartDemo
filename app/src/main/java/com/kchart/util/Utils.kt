package com.kchart.util

import android.app.Activity
import android.graphics.Rect
import com.kchart.main.MyApplication
import kotlin.math.roundToInt


fun dpToPx(dp: Float): Int {
    val density: Float =
        MyApplication.instance.resources.displayMetrics.density
    return (dp * density).roundToInt()
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