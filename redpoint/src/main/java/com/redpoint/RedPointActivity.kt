package com.redpoint

import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.kchart.rxjava.R
import com.kchartdemo.base.util.RouteList
import kotlinx.android.synthetic.main.activity_rx_java.*
import taylor.com.animation_dsl.animSet

@Route(path = RouteList.RedPointActivity)
class RedPointActivity : AppCompatActivity() {
    private val SCALE_UNSELECTED = 0.65f
    private val SCALE_SELECTED = 1f

    private val highlightAnim by lazy {
        animSet {
            anim {
                values = floatArrayOf(SCALE_SELECTED, SCALE_UNSELECTED)
                duration = 70
                interpolator = AccelerateDecelerateInterpolator()
                action = { value ->
                    tvRecommend.scaleX = value as Float
                    tvRecommend.scaleY = value
                    tvConcern.scaleX = (SCALE_SELECTED + SCALE_UNSELECTED) - value
                    tvConcern.scaleY = (SCALE_SELECTED + SCALE_UNSELECTED) - value
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rx_java)

        tvRecommend.setOnClickListener { highlightAnim.reverse() }
        tvConcern.setOnClickListener { highlightAnim.start() }
    }
}