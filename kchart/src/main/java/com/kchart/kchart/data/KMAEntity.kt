package com.kchart.kchart.data

import com.kchart.main.getCloseSum


class KMAEntity(private val kLineBeen: List<KLineBean>, private val n: Int) {
    fun compute(defult: Float): ArrayList<Float> {
        val MAs: ArrayList<Float> = ArrayList()
        var ma = 0.0f
        val index: Int = n - 1
        if (kLineBeen != null && kLineBeen.isNotEmpty()) {
            for (i in kLineBeen.indices) {
                ma = if (i >= index) {
                    getCloseSum(i - index, i, kLineBeen) / n
                } else {
                    defult
                }
                MAs.add(ma)
            }
        }
        return MAs
    }

}