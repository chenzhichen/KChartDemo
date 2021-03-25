package com.rxjava

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.kchart.rxjava.R
import com.kchartdemo.base.util.RouteList

@Route(path = RouteList.RxJavaActivity)
class RxJavaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rx_java)
    }
}