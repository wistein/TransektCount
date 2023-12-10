/*
 * Copyright (c) 2016 - 2023. Wilhelm Stein, Bonn, Germany.
 */
package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import com.wmstein.transektcount.AutoFitText
import com.wmstein.transektcount.R
import com.wmstein.transektcount.database.Count
import java.util.Objects

/**********************************
 * Interface for widget_counting_lhi.xml
 * Created by wmstein on 06.09.2016
 * Last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2023-10-06
 */
class CountingWidgetLhInt(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private val namef1i: TextView
    private val namef2i: TextView
    private val namef3i: TextView
    private val namepi: TextView
    private val nameli: TextView
    private val nameei: TextView
    
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
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_lhi, this, true)
        namef1i = findViewById(R.id.f1iNameLH)
        namef2i = findViewById(R.id.f2iNameLH)
        namef3i = findViewById(R.id.f3iNameLH)
        namepi = findViewById(R.id.piNameLH)
        nameli = findViewById(R.id.liNameLH)
        nameei = findViewById(R.id.eiNameLH)
        countCountf1i = findViewById(R.id.countCountLHf1i)
        countCountf2i = findViewById(R.id.countCountLHf2i)
        countCountf3i = findViewById(R.id.countCountLHf3i)
        countCountpi = findViewById(R.id.countCountLHpi)
        countCountli = findViewById(R.id.countCountLHli)
        countCountei = findViewById(R.id.countCountLHei)
    }

    fun setCountLHi(newcount: Count?) {
        count = newcount
        namef1i.text = context.getString(R.string.countImagomfHint)
        namef2i.text = context.getString(R.string.countImagomHint)
        namef3i.text = context.getString(R.string.countImagofHint)
        namepi.text = context.getString(R.string.countPupaHint)
        nameli.text = context.getString(R.string.countLarvaHint)
        nameei.text = context.getString(R.string.countOvoHint)
        countCountf1i.text = count!!.count_f1i.toString()
        countCountf2i.text = count!!.count_f2i.toString()
        countCountf3i.text = count!!.count_f3i.toString()
        countCountpi.text = count!!.count_pi.toString()
        countCountli.text = count!!.count_li.toString()
        countCountei.text = count!!.count_ei.toString()
        val countUpf1eButton = findViewById<ImageButton>(R.id.buttonUpLHf1i)
        countUpf1eButton.tag = count!!.id
        val countUpf2eButton = findViewById<ImageButton>(R.id.buttonUpLHf2i)
        countUpf2eButton.tag = count!!.id
        val countUpf3eButton = findViewById<ImageButton>(R.id.buttonUpLHf3i)
        countUpf3eButton.tag = count!!.id
        val countUppeButton = findViewById<ImageButton>(R.id.buttonUpLHpi)
        countUppeButton.tag = count!!.id
        val countUpleButton = findViewById<ImageButton>(R.id.buttonUpLHli)
        countUpleButton.tag = count!!.id
        val countUpeeButton = findViewById<ImageButton>(R.id.buttonUpLHei)
        countUpeeButton.tag = count!!.id
        val countDownf1eButton = findViewById<ImageButton>(R.id.buttonDownLHf1i)
        countDownf1eButton.tag = count!!.id
        val countDownf2eButton = findViewById<ImageButton>(R.id.buttonDownLHf2i)
        countDownf2eButton.tag = count!!.id
        val countDownf3eButton = findViewById<ImageButton>(R.id.buttonDownLHf3i)
        countDownf3eButton.tag = count!!.id
        val countDownpeButton = findViewById<ImageButton>(R.id.buttonDownLHpi)
        countDownpeButton.tag = count!!.id
        val countDownleButton = findViewById<ImageButton>(R.id.buttonDownLHli)
        countDownleButton.tag = count!!.id
        val countDowneeButton = findViewById<ImageButton>(R.id.buttonDownLHei)
        countDowneeButton.tag = count!!.id
    }

    // Count up/down and set value on lefthanded screen
    fun countUpLHf1i() {
        // increase count_f1i
        countCountf1i.text = count!!.increase_f1i().toString()
    }

    fun countDownLHf1i() {
        countCountf1i.text = count!!.safe_decrease_f1i().toString()
    }

    fun countUpLHf2i() {
        countCountf2i.text = count!!.increase_f2i().toString()
    }

    fun countDownLHf2i() {
        countCountf2i.text = count!!.safe_decrease_f2i().toString()
    }

    fun countUpLHf3i() {
        countCountf3i.text = count!!.increase_f3i().toString()
    }

    fun countDownLHf3i() {
        countCountf3i.text = count!!.safe_decrease_f3i().toString()
    }

    fun countUpLHpi() {
        countCountpi.text = count!!.increase_pi().toString()
    }

    fun countDownLHpi() {
        countCountpi.text = count!!.safe_decrease_pi().toString()
    }

    fun countUpLHli() {
        countCountli.text = count!!.increase_li().toString()
    }

    fun countDownLHli() {
        countCountli.text = count!!.safe_decrease_li().toString()
    }

    fun countUpLHei() {
        countCountei.text = count!!.increase_ei().toString()
    }

    fun countDownLHei() {
        countCountei.text = count!!.safe_decrease_ei().toString()
    }
}