package com.kchart.kchart.view

import com.kchart.kchart.data.KLineBean
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class KChartAdapter : BaseKChartAdapter<KLineBean>() {
    private var datas = ArrayList<KLineBean>()
    private var simpleDateFormat: SimpleDateFormat = SimpleDateFormat("MM-dd HH:mm")
    var addToHeader = false
    var calculationListener: CalculationListener? = null


    override fun getDate(position: Int): String {
        return if (position < 0 || position >= datas.size) {
            ""
        } else {
            simpleDateFormat.format(Date(datas[position].date))
        }
    }

    override fun getItem(position: Int): KLineBean {
        return datas[position]
    }

    override fun getCount(): Int {
        return datas.size
    }

    override fun getData(): List<KLineBean> {
        return datas
    }

    /**
     * 向头部添加数据
     */
    fun addHeaderData(data: List<KLineBean>) {
        if (data != null && data.isNotEmpty()) {
            datas.addAll(0, data)
            calculationListener?.calculate()
            addToHeader = true
            notifyDataSetChanged()
            addToHeader = false
        }
    }

    /**
     * 向尾部添加数据
     */
    fun addFooterData(data: List<KLineBean>) {
        if (data != null && data.isNotEmpty()) {
            datas.addAll(data)
            calculationListener?.calculate()
            notifyDataSetChanged()
        }
    }

    /**
     * 改变某个点的值
     *
     * @param position 索引值
     */
    fun changeItem(position: Int, data: KLineBean) {
        if (data == null) {
            return
        }
        datas[position] = data
        notifyDataSetChanged()
    }

    fun getDatas(): ArrayList<KLineBean> {
        return datas
    }

    fun setDatas(datas: ArrayList<KLineBean>) {
        if (datas == null) {
            return
        }
        this.datas = datas
        notifyDataSetChanged()
    }

    fun setDateFormat(dateFormat: String) {
        simpleDateFormat = SimpleDateFormat(dateFormat)
    }




}