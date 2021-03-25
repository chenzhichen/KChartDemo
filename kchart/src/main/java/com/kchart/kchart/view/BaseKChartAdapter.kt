package com.kchart.kchart.view

import android.database.DataSetObservable
import android.database.DataSetObserver


abstract class BaseKChartAdapter<T> {
    private val mDataSetObservable = DataSetObservable()
    fun notifyDataSetChanged() {
        if (getCount() > 0) {
            mDataSetObservable.notifyChanged()
        } else {
            mDataSetObservable.notifyInvalidated()
        }
    }

    fun registerDataSetObserver(observer: DataSetObserver) {
        mDataSetObservable.registerObserver(observer)
    }

    fun unregisterDataSetObserver(observer: DataSetObserver) {
        mDataSetObservable.unregisterObserver(observer)
    }


    /**
     * 获取点的数目
     *
     * @return
     */
    abstract fun getCount(): Int

    /**
     * 通过序号获取item
     *
     * @param position 对应的序号
     * @return 数据实体
     */
    abstract fun getItem(position: Int): Any?

    /**
     * 通过序号获取时间
     *
     * @param position
     * @return
     */
    abstract fun getDate(position: Int): String?


    abstract fun getData(): List<T>
}