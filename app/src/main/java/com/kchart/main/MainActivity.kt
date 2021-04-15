package com.kchart.main

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.Postcard
import com.alibaba.android.arouter.facade.callback.NavigationCallback
import com.alibaba.android.arouter.launcher.ARouter
import com.kchartdemo.base.util.RouteList
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        kchart.setOnClickListener {
            ARouter.getInstance().build(RouteList.KChartActivity)
                .navigation()
        }
        redPoint.setOnClickListener {
            ARouter.getInstance().build(RouteList.RedPointActivity)
                .navigation()
        }
    }


}