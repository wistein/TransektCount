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

/***************************************
 * Interface for widget_counting_lhe.xml
 * Created by wmstein on 06.12.2016,
 * last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2023-10-06
 */
class CountingWidgetLhExt(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private val namef1e: TextView
    private val namef2e: TextView
    private val namef3e: TextView
    private val namepe: TextView
    private val namele: TextView
    private val nameee: TextView
    
    // external counters
    private val countCountf1e: AutoFitText
    private val countCountf2e: AutoFitText
    private val countCountf3e: AutoFitText
    private val countCountpe: AutoFitText
    private val countCountle: AutoFitText
    private val countCountee: AutoFitText
    @JvmField
    var count: Count? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_lhe, this, true)
        namef1e = findViewById(R.id.f1eNameLH)
        namef2e = findViewById(R.id.f2eNameLH)
        namef3e = findViewById(R.id.f3eNameLH)
        namepe = findViewById(R.id.peNameLH)
        namele = findViewById(R.id.leNameLH)
        nameee = findViewById(R.id.eeNameLH)
        countCountf1e = findViewById(R.id.countCountLHf1e)
        countCountf2e = findViewById(R.id.countCountLHf2e)
        countCountf3e = findViewById(R.id.countCountLHf3e)
        countCountpe = findViewById(R.id.countCountLHpe)
        countCountle = findViewById(R.id.countCountLHle)
        countCountee = findViewById(R.id.countCountLHee)
    }

    fun setCountLHe(newcount: Count?) {
        count = newcount
        namef1e.text = context.getString(R.string.countImagomfHint)
        namef2e.text = context.getString(R.string.countImagomHint)
        namef3e.text = context.getString(R.string.countImagofHint)
        namepe.text = context.getString(R.string.countPupaHint)
        namele.text = context.getString(R.string.countLarvaHint)
        nameee.text = context.getString(R.string.countOvoHint)
        countCountf1e.text = count!!.count_f1e.toString()
        countCountf2e.text = count!!.count_f2e.toString()
        countCountf3e.text = count!!.count_f3e.toString()
        countCountpe.text = count!!.count_pe.toString()
        countCountle.text = count!!.count_le.toString()
        countCountee.text = count!!.count_ee.toString()
        val countUpf1eButton = findViewById<ImageButton>(R.id.buttonUpLHf1e)
        countUpf1eButton.tag = count!!.id
        val countUpf2eButton = findViewById<ImageButton>(R.id.buttonUpLHf2e)
        countUpf2eButton.tag = count!!.id
        val countUpf3eButton = findViewById<ImageButton>(R.id.buttonUpLHf3e)
        countUpf3eButton.tag = count!!.id
        val countUppeButton = findViewById<ImageButton>(R.id.buttonUpLHpe)
        countUppeButton.tag = count!!.id
        val countUpleButton = findViewById<ImageButton>(R.id.buttonUpLHle)
        countUpleButton.tag = count!!.id
        val countUpeeButton = findViewById<ImageButton>(R.id.buttonUpLHee)
        countUpeeButton.tag = count!!.id
        val countDownf1eButton = findViewById<ImageButton>(R.id.buttonDownLHf1e)
        countDownf1eButton.tag = count!!.id
        val countDownf2eButton = findViewById<ImageButton>(R.id.buttonDownLHf2e)
        countDownf2eButton.tag = count!!.id
        val countDownf3eButton = findViewById<ImageButton>(R.id.buttonDownLHf3e)
        countDownf3eButton.tag = count!!.id
        val countDownpeButton = findViewById<ImageButton>(R.id.buttonDownLHpe)
        countDownpeButton.tag = count!!.id
        val countDownleButton = findViewById<ImageButton>(R.id.buttonDownLHle)
        countDownleButton.tag = count!!.id
        val countDowneeButton = findViewById<ImageButton>(R.id.buttonDownLHee)
        countDowneeButton.tag = count!!.id
    }

    // Count up/down and set value on lefthanded screen
    fun countUpLHf1e() {
        countCountf1e.text = count!!.increase_f1e().toString()
    }

    fun countDownLHf1e() {
        countCountf1e.text = count!!.safe_decrease_f1e().toString()
    }

    fun countUpLHf2e() {
        countCountf2e.text = count!!.increase_f2e().toString()
    }

    fun countDownLHf2e() {
        countCountf2e.text = count!!.safe_decrease_f2e().toString()
    }

    fun countUpLHf3e() {
        countCountf3e.text = count!!.increase_f3e().toString()
    }

    fun countDownLHf3e() {
        countCountf3e.text = count!!.safe_decrease_f3e().toString()
    }

    fun countUpLHpe() {
        countCountpe.text = count!!.increase_pe().toString()
    }

    fun countDownLHpe() {
        countCountpe.text = count!!.safe_decrease_pe().toString()
    }

    fun countUpLHle() {
        countCountle.text = count!!.increase_le().toString()
    }

    fun countDownLHle() {
        countCountle.text = count!!.safe_decrease_le().toString()
    }

    fun countUpLHee() {
        countCountee.text = count!!.increase_ee().toString()
    }

    fun countDownLHee() {
        countCountee.text = count!!.safe_decrease_ee().toString()
    }
}