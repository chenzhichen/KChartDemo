package com.kchartdemo.base

import android.app.Application
import com.alibaba.android.arouter.launcher.ARouter
import kotlin.properties.Delegates

class MyApplication : Application() {
    companion object {
        var instance: MyApplication by Delegates.notNull()

    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ARouter.openLog()
        ARouter.openDebug()
        ARouter.init(this)// 尽可能早，推荐在Application中初始化
    }
}