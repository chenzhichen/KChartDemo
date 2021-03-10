package com.kchart.data


data class KLineBean(
    var date: Long = 0,
    var open: Float = 0f,
    var close: Float = 0f,
    var high: Float = 0f,
    var low: Float = 0f,
    var vol: Float = 0f
)
