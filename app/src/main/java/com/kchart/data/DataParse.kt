package com.kchart.main

import com.kchart.data.BarEntry
import com.kchart.data.KLineBean
import com.kchart.data.KMAEntity
import com.kchart.data.LineEntry
import org.json.JSONArray


fun parseData(data: String): ArrayList<KLineBean> {
    val kLineBeans = ArrayList<KLineBean>()
    try {
        val jsonArray = JSONArray(data)
        if (jsonArray == null || jsonArray.length() == 0) return kLineBeans
        for (i in 0 until jsonArray.length()) {
            val array = jsonArray.getJSONArray(i)
            var date = array.getLong(0)
            var open = array.getDouble(1)
            var hight = array.getDouble(2)
            var low = array.getDouble(3)
            var close = array.getDouble(4)
            var vol = array.getDouble(5)
            var kLineData = KLineBean(
                date,
                open.toFloat(), close.toFloat(),
                hight.toFloat(), low.toFloat(),
                vol.toFloat()
            )
            kLineBeans.add(kLineData)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return kLineBeans
}


fun initKLineMA(datas: List<KLineBean>, n: Int, color: Int): java.util.ArrayList<LineEntry> {
    val maDataL = ArrayList<LineEntry>()
    if (null == datas || datas.isEmpty()) {
        return maDataL
    }
    val kmaEntitys = KMAEntity(datas, n).compute(0f)
    for (i in 0 until kmaEntitys.size) {
        if (i >= n - 1) maDataL.add(
            LineEntry(
                i,
                kmaEntitys[i],
                color
            )
        ) else maDataL.add(LineEntry(-1, kmaEntitys[i], color))
    }
    return maDataL
}

fun initVolMA(datas: List<KLineBean>, n: Int, color: Int): ArrayList<LineEntry> {
    val MAs = java.util.ArrayList<LineEntry>()
    val index = n - 1
    if (datas != null && datas.isNotEmpty()) {
        for (i in datas.indices) {
            if (i >= index) {
                MAs.add(LineEntry(i, getVolSum(i - index, i, datas) / n, color))
            } else {
                MAs.add(LineEntry(-1, 0f, color))
            }

        }
    }
    return MAs
}


fun initVol(datas: List<KLineBean>): java.util.ArrayList<BarEntry> {
    val maDataL = ArrayList<BarEntry>()
    if (null == datas || datas.isEmpty()) {
        return maDataL
    }
    for (i in datas.indices) {
        val bean = datas[i]
        val entry = BarEntry(i, bean.vol, bean.close >= bean.open)
        maDataL.add(entry)
    }
    return maDataL
}

fun getCloseSum(a: Int, b: Int, datas: List<KLineBean>): Float {
    var sum = 0f
    for (i in a..b) {
        sum += datas[i].close
    }
    return sum
}

fun getVolSum(a: Int, b: Int, datas: List<KLineBean>): Float {
    var sum = 0f
    for (i in a..b) {
        sum += datas[i].vol
    }
    return sum
}