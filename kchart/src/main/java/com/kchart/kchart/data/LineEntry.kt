package com.kchart.kchart.data


class LineEntry {
    var y: Float
    var x: Int//<0则表示改点不绘制
    var color = 0

    constructor(x: Int, y: Float) {
        this.x = x
        this.y = y
    }

    constructor(x: Int, y: Float, color: Int) {
        this.x = x
        this.y = y
        this.color = color
    }
}