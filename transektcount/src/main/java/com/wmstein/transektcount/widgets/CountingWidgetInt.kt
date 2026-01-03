/*
 * Copyright © 2016-2026, Wilhelm Stein, Bonn, Germany.
 */
package com.wmstein.transektcount.widgets

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.Point
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.wmstein.transektcount.AutoFitText
import com.wmstein.transektcount.R
import com.wmstein.transektcount.database.Count
import java.util.Objects

/*****************************************************************
 * CountingWidgetInt.kt is the interface for widget_counting_i.xml
 * Created by wmstein 18.12.2016
 * last edited in Java on 2021-01-26
 * converted to Kotlin on 2023-06-26
 * last edited on 2025-11-22
 */
class CountingWidgetInt(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    var sHeight: Int = 0
    var ht: Int = 0

    private val namef1i: TextView
    private val namef2i: TextView
    private val namef3i: TextView
    private val namepi: TextView
    private val nameli: TextView
    private val nameei: TextView

    private var idf1i: LinearLayout
    private var idf2i: LinearLayout
    private var idf3i: LinearLayout
    private var idpi: LinearLayout
    private var idli: LinearLayout
    private var idei: LinearLayout

    // section internal counters
    private val countCountf1i: AutoFitText
    private val countCountf2i: AutoFitText
    private val countCountf3i: AutoFitText
    private val countCountpi: AutoFitText
    private val countCountli: AutoFitText
    private val countCountei: AutoFitText

    @JvmField
    var count: Count? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_i, this, true)
        idf1i = findViewById(R.id.idf1i)
        idf2i = findViewById(R.id.idf2i)
        idf3i = findViewById(R.id.idf3i)
        idpi = findViewById(R.id.idpi)
        idli = findViewById(R.id.idli)
        idei = findViewById(R.id.idei)

        namef1i = findViewById(R.id.f1iName)
        namef2i = findViewById(R.id.f2iName)
        namef3i = findViewById(R.id.f3iName)
        namepi = findViewById(R.id.piName)
        nameli = findViewById(R.id.liName)
        nameei = findViewById(R.id.eiName)

        countCountf1i = findViewById(R.id.countCountf1i)
        countCountf2i = findViewById(R.id.countCountf2i)
        countCountf3i = findViewById(R.id.countCountf3i)
        countCountpi = findViewById(R.id.countCountpi)
        countCountli = findViewById(R.id.countCountli)
        countCountei = findViewById(R.id.countCountei)
    }

    @Suppress("RemoveRedundantQualifierName")
    fun setCounti(newcount: Count?) {
        // Get screen size to adapt the counting view
        val wm = checkNotNull(context.getSystemService(WINDOW_SERVICE) as WindowManager)
        if (Build.VERSION.SDK_INT >= 30) {
            val metrics = wm.currentWindowMetrics
            sHeight = metrics.bounds.top + metrics.bounds.bottom
        } else {
            @Suppress("DEPRECATION")
            val display = wm.defaultDisplay // deprecated in 30
            val size = Point()
            @Suppress("DEPRECATION")
            display.getSize(size) // deprecated in 30
            sHeight = size.y
        }

        // Height for counter line on counting page
        ht = sHeight / 22

        val lparamsf1i: ViewGroup.LayoutParams = idf1i.layoutParams
        lparamsf1i.height = ht
        val lparamsf2i: ViewGroup.LayoutParams = idf2i.layoutParams
        lparamsf2i.height = ht
        val lparamsf3i: ViewGroup.LayoutParams = idf3i.layoutParams
        lparamsf3i.height = ht
        val lparamspi: ViewGroup.LayoutParams = idpi.layoutParams
        lparamspi.height = ht
        val lparamsli: ViewGroup.LayoutParams = idli.layoutParams
        lparamsli.height = ht
        val lparamsei: ViewGroup.LayoutParams = idei.layoutParams
        lparamsei.height = ht

        idf1i.layoutParams = lparamsf1i
        idf2i.layoutParams = lparamsf2i
        idf3i.layoutParams = lparamsf3i
        idpi.layoutParams = lparamspi
        idli.layoutParams = lparamsli
        idei.layoutParams = lparamsei

        count = newcount
        namef1i.text = context.getString(R.string.countImagomfHint)
        namef2i.text = context.getString(R.string.countImagomHint)
        namef3i.text = context.getString(R.string.countImagofHint)
        namepi.text = context.getString(R.string.countPupaHint)
        nameli.text = context.getString(R.string.countLarvaHint)
        nameei.text = context.getString(R.string.countOvoHint)

        if (count!!.count_f1i > 0)
            countCountf1i.text = count!!.count_f1i.toString()
        if (count!!.count_f2i > 0)
            countCountf2i.text = count!!.count_f2i.toString()
        if (count!!.count_f3i > 0)
            countCountf3i.text = count!!.count_f3i.toString()
        if (count!!.count_pi > 0)
            countCountpi.text = count!!.count_pi.toString()
        if (count!!.count_li > 0)
            countCountli.text = count!!.count_li.toString()
        if (count!!.count_ei > 0)
            countCountei.text = count!!.count_ei.toString()

        val countUpf1iButton = findViewById<ImageButton>(R.id.buttonUpf1i)
        countUpf1iButton.tag = count!!.id
        val countUpf2iButton = findViewById<ImageButton>(R.id.buttonUpf2i)
        countUpf2iButton.tag = count!!.id
        val countUpf3iButton = findViewById<ImageButton>(R.id.buttonUpf3i)
        countUpf3iButton.tag = count!!.id
        val countUppiButton = findViewById<ImageButton>(R.id.buttonUppi)
        countUppiButton.tag = count!!.id
        val countUpliButton = findViewById<ImageButton>(R.id.buttonUpli)
        countUpliButton.tag = count!!.id
        val countUpeiButton = findViewById<ImageButton>(R.id.buttonUpei)
        countUpeiButton.tag = count!!.id
        val countDownf1iButton = findViewById<ImageButton>(R.id.buttonDownf1i)
        countDownf1iButton.tag = count!!.id
        val countDownf2iButton = findViewById<ImageButton>(R.id.buttonDownf2i)
        countDownf2iButton.tag = count!!.id
        val countDownf3iButton = findViewById<ImageButton>(R.id.buttonDownf3i)
        countDownf3iButton.tag = count!!.id
        val countDownpiButton = findViewById<ImageButton>(R.id.buttonDownpi)
        countDownpiButton.tag = count!!.id
        val countDownliButton = findViewById<ImageButton>(R.id.buttonDownli)
        countDownliButton.tag = count!!.id
        val countDowneiButton = findViewById<ImageButton>(R.id.buttonDownei)
        countDowneiButton.tag = count!!.id
    }

    // Count up/down and set value on screen
    fun countUpf1i() {
        // increase count_f1i
        countCountf1i.text = count!!.increase_f1i().toString()
    }

    fun countDownf1i() {
        countCountf1i.text = count!!.safe_decrease_f1i().toString()
    }

    fun countUpf2i() {
        countCountf2i.text = count!!.increase_f2i().toString()
    }

    fun countDownf2i() {
        countCountf2i.text = count!!.safe_decrease_f2i().toString()
    }

    fun countUpf3i() {
        countCountf3i.text = count!!.increase_f3i().toString()
    }

    fun countDownf3i() {
        countCountf3i.text = count!!.safe_decrease_f3i().toString()
    }

    fun countUppi() {
        countCountpi.text = count!!.increase_pi().toString()
    }

    fun countDownpi() {
        countCountpi.text = count!!.safe_decrease_pi().toString()
    }

    fun countUpli() {
        countCountli.text = count!!.increase_li().toString()
    }

    fun countDownli() {
        countCountli.text = count!!.safe_decrease_li().toString()
    }

    fun countUpei() {
        countCountei.text = count!!.increase_ei().toString()
    }

    fun countDownei() {
        countCountei.text = count!!.safe_decrease_ei().toString()
    }
}
