package com.kchart.main

import android.app.Application
import kotlin.properties.Delegates


class MyApplication : Application() {

    companion object {
        var instance: MyApplication by Delegates.notNull()

    }

    override fun onCreate() {
        super.onCreate()
        instance = this

    }
}