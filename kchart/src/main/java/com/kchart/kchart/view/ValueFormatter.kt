package com.kchart.kchart.view


class ValueFormatter : IValueFormatter {
    override fun format(value: Float): String {
        return String.format("%.2f", value)
    }
}