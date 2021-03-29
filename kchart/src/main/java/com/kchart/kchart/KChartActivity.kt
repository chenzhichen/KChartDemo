package com.kchart.kchart

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Route
import com.kchart.kchart.data.BarEntry
import com.kchart.kchart.data.KLineBean
import com.kchart.kchart.data.LineEntry
import com.kchart.kchart.view.*
import com.kchart.main.initKLineMA
import com.kchart.main.initVol
import com.kchart.main.initVolMA
import com.kchart.main.parseData
import com.kchartdemo.base.util.RouteList
import kotlinx.android.synthetic.main.activity_k_chart.*
import java.io.InputStream
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.sqrt

@Route(path = RouteList.KChartActivity)
class KChartActivity : AppCompatActivity() {
    private var format = DecimalFormat("##.#")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_k_chart)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SimpleAdapter()
        var data = ""
        var input: InputStream? = null
        try {
            input = assets.open("data.json")
            val scanner = Scanner(input, "UTF-8").useDelimiter("\\A")
            if (scanner.hasNext()) {
                data = scanner.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            input?.close()
        }
        kChartView.adapter.setDatas(parseData(data))

        kChartView.mainChartDraw.minValueFactor = 0.05f
        kChartView.mainChartDraw.setYValueFormatter(object : IValueFormatter {
            override fun format(value: Float): String {
                return String.format("%.2f", value)
            }

        })
        ma.isChecked = true
        addMaIndex(true)
        vol.isChecked = true
        addVolIndex(true)
        macd.isChecked = true
        addMACD(true)
        ma.setOnCheckedChangeListener { _, isChecked ->
            addMaIndex(isChecked)
        }
        vol.setOnCheckedChangeListener { _, isChecked ->
            addVolIndex(isChecked)
        }

        boll.setOnCheckedChangeListener { _, isChecked ->
            addBOLL(isChecked)
        }
        macd.setOnCheckedChangeListener { _, isChecked ->
            addMACD(isChecked)
        }

        addHeader.setOnClickListener {
            var data = ArrayList<KLineBean>()
            data.add(kChartView.adapter.getItem(0))
            kChartView.adapter.addHeaderData(data)
        }
        addFooter.setOnClickListener {
            var data = ArrayList<KLineBean>()
            data.add(kChartView.adapter.getItem(kChartView.adapter.getCount() - 1))
            kChartView.adapter.addFooterData(data)
        }

        kChartView.adapter.calculationListener = object : CalculationListener {
            override fun calculate() {
                computerIndex()
            }

        }

    }

    fun computerIndex() {
        if (vol.isChecked) {
            computerVolIndex(kChartView.getChildChartDraw("val") as IndexChartDraw)
        }
        if (ma.isChecked) {
            computerMaIndex()
        }
        if (boll.isChecked) {
            computerBOLL()
        }
        if (macd.isChecked) {
            computerMACDIndex(kChartView.getChildChartDraw("macd") as IndexChartDraw)
        }

    }


    class SimpleAdapter : RecyclerView.Adapter<SimpleAdapter.ViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = "$position"
        }

        override fun getItemCount(): Int {
            return 20
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var textView: TextView = itemView.findViewById(R.id.textView)

        }

    }

    private fun addVolIndex(add: Boolean) {
        if (!add) {
            kChartView.removeChildChartDraw("val")
            return
        }
        val volChartDraw = IndexChartDraw()
        volChartDraw.maxValueFactor = 0.03f
        volChartDraw.setYValueFormatter(object : IValueFormatter {
            override fun format(value: Float): String {
                return when {
                    abs(value) > 1000000000 -> {
                        format.format((value / 1000000000).toLong()) + "B"
                    }
                    abs(value) > 1000000 -> {
                        return format.format((value / 1000000).toLong()) + "M"
                    }
                    abs(value) > 1000 -> {
                        format.format((value / 1000).toLong()) + "K"
                    }
                    else -> {
                        String.format("%.2f", value)
                    }
                }
            }
        })
        computerVolIndex(volChartDraw)
        kChartView.addChildChartDraw("val", volChartDraw)
    }

    private fun computerVolIndex(volChartDraw: IndexChartDraw) {
        volChartDraw.barData["量(5,10)"] = initVol(kChartView.adapter.getDatas())
        val volLineMap = LinkedHashMap<String, ArrayList<LineEntry>>()
        volLineMap["MA5"] =
            initVolMA(
                kChartView.adapter.getDatas(),
                5,
                Color.parseColor("#DE4D42")
            )
        volLineMap["MA10"] =
            initVolMA(
                kChartView.adapter.getDatas(),
                10,
                Color.parseColor("#fca044")
            )
        volChartDraw.lineData[""] = volLineMap
    }

    private fun addMaIndex(add: Boolean) {
        if (!add) {
            kChartView.mainChartDraw.lineData.remove("")
            kChartView.calculateValue()
            return
        }
        computerMaIndex()
        kChartView.calculateValue()
    }

    private fun computerMaIndex() {
        val lineMap = LinkedHashMap<String, ArrayList<LineEntry>>()
        lineMap["MA(5)"] =
            initKLineMA(
                kChartView.adapter.getDatas(),
                5,
                Color.parseColor("#4b5fe1")
            )
        lineMap["MA(7)"] =
            initKLineMA(
                kChartView.adapter.getDatas(),
                7,
                Color.parseColor("#6482d9")
            )
        lineMap["MA(10)"] =
            initKLineMA(
                kChartView.adapter.getDatas(),
                10,
                Color.parseColor("#fca044")
            )

        lineMap["MA(25)"] =
            initKLineMA(
                kChartView.adapter.getDatas(),
                25,
                Color.parseColor("#DE4D42")
            )
        kChartView.mainChartDraw.lineData[""] = lineMap
    }

    private fun addBOLL(add: Boolean) {
        if (!add) {
            kChartView.mainChartDraw.lineData.remove("BOLL(20,2)")
            kChartView.calculateValue()
            return
        }
        computerBOLL()
        kChartView.calculateValue()
    }

    private fun computerBOLL() {
        val UPs = ArrayList<Float>()
        val MBs = ArrayList<Float>()
        val DNs = ArrayList<Float>()

        var mb = 0.0f
        var up = 0.0f
        var dn = 0.0f
        val n = 20
        val parameter = 2

        for (i in kChartView.adapter.getDatas().indices) {
            val point: KLineBean = kChartView.adapter.getDatas()[i]
            if (i >= n - 1) {
                var md = 0f
                for (j in i - n + 1..i) {
                    val c: Float = kChartView.adapter.getDatas()[j].close
                    val m: Float = getMA(i, kChartView.adapter.getDatas(), n)
                    val value = c - m
                    md += value * value
                }
                md /= n
                md = sqrt(md.toDouble()).toFloat()
                mb = getMA(i, kChartView.adapter.getDatas(), n)
                up = mb + parameter * md
                dn = mb - parameter * md
                UPs.add(up)
                MBs.add(mb)
                DNs.add(dn)
            } else {
                UPs.add(point.close)
                MBs.add(point.close)
                DNs.add(point.close)
            }
        }
        val bollDataUP = ArrayList<LineEntry>()
        val bollDataMB = ArrayList<LineEntry>()
        val bollDataDN = ArrayList<LineEntry>()

        val color1 = Color.RED
        val color2 = Color.GRAY
        val color3 = Color.DKGRAY

        for (i in kChartView.adapter.getDatas().indices) {
            if (i >= n - 1) {
                bollDataUP.add(LineEntry(i, UPs[i], color1))
                bollDataMB.add(LineEntry(i, MBs[i], color2))
                bollDataDN.add(LineEntry(i, DNs[i], color3))
            } else {
                bollDataUP.add(LineEntry(-1, UPs[i], color1))
                bollDataMB.add(LineEntry(-1, MBs[i], color2))
                bollDataDN.add(LineEntry(-1, DNs[i], color3))
            }
        }
        val lineMap = LinkedHashMap<String, ArrayList<LineEntry>>()
        lineMap["MID"] = bollDataMB
        lineMap["UP"] = bollDataUP
        lineMap["LOW"] = bollDataDN
        kChartView.mainChartDraw.lineData["BOLL(20,2)"] = lineMap
    }

    private fun getMA(i: Int, kLineBeens: List<KLineBean>, n: Int): Float {
        var close = 0.0f
        var kLineBean: KLineBean
        if (i < n - 1) {
            return 0f
        } else {
            for (j in i - n + 1..i) {
                kLineBean = kLineBeens[j]
                close += kLineBean.close
            }
        }
        return close / n
    }

    private fun addMACD(add: Boolean) {
        if (!add) {
            kChartView.removeChildChartDraw("macd")
            return
        }
        val macdChartDraw = IndexChartDraw()
        computerMACDIndex(macdChartDraw)
        kChartView.addChildChartDraw("macd", macdChartDraw)
    }

    private fun computerMACDIndex(macdChartDraw: IndexChartDraw) {
        val DEAs = java.util.ArrayList<Float>()
        val DIFs = java.util.ArrayList<Float>()
        val MACDs = java.util.ArrayList<Float>()

        val dEAs: MutableList<Float> = java.util.ArrayList()
        val dIFs: MutableList<Float> = java.util.ArrayList()
        val mACDs: MutableList<Float> = java.util.ArrayList()

        val shortPeriod = 9
        val longPeriod = 26
        val midPeriod = 12

        var eMA12 = 0.0f
        var eMA26 = 0.0f
        var close = 0f
        var dIF = 0.0f
        var dEA = 0.0f
        var mACD = 0.0f
        if (kChartView.adapter.getDatas() != null && kChartView.adapter.getDatas().size > 0) {
            for (i in kChartView.adapter.getDatas().indices) {
                close = kChartView.adapter.getDatas()[i].close
                if (i == 0) {
                    eMA12 = close
                    eMA26 = close
                } else {
                    eMA12 =
                        eMA12 * (shortPeriod - 1) / (shortPeriod + 1) + close * 2 / (shortPeriod + 1)
                    eMA26 =
                        eMA26 * (longPeriod - 1) / (longPeriod + 1) + close * 2 / (longPeriod + 1)
                }
                dIF = eMA12 - eMA26
                dEA = dEA * (midPeriod - 1) / (midPeriod + 1) + dIF * 2 / (midPeriod + 1)
                mACD = dIF - dEA
                dEAs.add(dEA)
                dIFs.add(dIF)
                mACDs.add(mACD * 2)
            }
            for (i in dEAs.indices) {
                DEAs.add(dEAs[i])
                DIFs.add(dIFs[i])
                MACDs.add(mACDs[i])
            }
        }

        val macdData = java.util.ArrayList<BarEntry>()
        val deaData = java.util.ArrayList<LineEntry>()
        val difData = java.util.ArrayList<LineEntry>()
        val macdlineData = java.util.ArrayList<LineEntry>()
        val color1 = Color.RED
        val color2 = Color.GRAY
        val color3 = Color.BLUE
        for (i in 0 until MACDs.size) {
            val macd: Float = MACDs[i]
            macdData.add(BarEntry(i, macd, macd > 0))
            deaData.add(LineEntry(i, DEAs[i], color1))
            difData.add(LineEntry(i, DIFs[i], color2))
            macdlineData.add(LineEntry(-1, macd, color3))
        }


        macdChartDraw.maxValueFactor = 0.03f
        macdChartDraw.minValueFactor = 0.03f
        macdChartDraw.setYValueFormatter(object : IValueFormatter {
            override fun format(value: Float): String {
                return String.format("%.2f", value)
            }
        })
        val macdLineMap = LinkedHashMap<String, ArrayList<LineEntry>>()
        macdLineMap["DIF"] = difData
        macdLineMap["DEA"] = deaData
        macdLineMap["MACD"] = macdlineData
        macdChartDraw.lineData[""] = macdLineMap
        macdChartDraw.barData["MACD(12,26,9)"] = macdData
        macdChartDraw.isMACD = true

        Handler().sendEmptyMessageDelayed(2, 2)
    }
}
