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
                .navigation(this, object : NavigationCallback {
                    override fun onFound(postcard: Postcard?) {
                        Log.e("ARouter", "onFound----${postcard.toString()}")
                    }

                    override fun onLost(postcard: Postcard?) {
                        Log.e("ARouter", "onLost----${postcard.toString()}")
                    }

                    override fun onArrival(postcard: Postcard?) {
                        Log.e("ARouter", "onArrival----${postcard.toString()}")
                    }

                    override fun onInterrupt(postcard: Postcard?) {
                        Log.e("ARouter", "onInterrupt----${postcard.toString()}")
                    }

                })
        }
    }


}