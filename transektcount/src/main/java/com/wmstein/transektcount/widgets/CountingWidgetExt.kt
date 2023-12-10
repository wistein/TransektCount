/*
 * Copyright (c) 2016 -2023. Wilhelm Stein, Bonn, Germany.
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

/****************************************************
 * Interface for widget_counting_e.xml
 * Created by wmstein on 18.12.2016
 * last edited in Java on 2021-01-26
 * converted to Kotlin on 2023-06-26
 * last edited on 2023-10-06
 */
class CountingWidgetExt(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
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
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_e, this, true)
        namef1e = findViewById(R.id.f1eName)
        namef2e = findViewById(R.id.f2eName)
        namef3e = findViewById(R.id.f3eName)
        namepe = findViewById(R.id.peName)
        namele = findViewById(R.id.leName)
        nameee = findViewById(R.id.eeName)
        countCountf1e = findViewById(R.id.countCountf1e)
        countCountf2e = findViewById(R.id.countCountf2e)
        countCountf3e = findViewById(R.id.countCountf3e)
        countCountpe = findViewById(R.id.countCountpe)
        countCountle = findViewById(R.id.countCountle)
        countCountee = findViewById(R.id.countCountee)
    }

    fun setCounte(newcount: Count?) {
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
        val countUpf1eButton = findViewById<ImageButton>(R.id.buttonUpf1e)
        countUpf1eButton.tag = count!!.id
        val countUpf2eButton = findViewById<ImageButton>(R.id.buttonUpf2e)
        countUpf2eButton.tag = count!!.id
        val countUpf3eButton = findViewById<ImageButton>(R.id.buttonUpf3e)
        countUpf3eButton.tag = count!!.id
        val countUppeButton = findViewById<ImageButton>(R.id.buttonUppe)
        countUppeButton.tag = count!!.id
        val countUpleButton = findViewById<ImageButton>(R.id.buttonUple)
        countUpleButton.tag = count!!.id
        val countUpeeButton = findViewById<ImageButton>(R.id.buttonUpee)
        countUpeeButton.tag = count!!.id
        val countDownf1eButton = findViewById<ImageButton>(R.id.buttonDownf1e)
        countDownf1eButton.tag = count!!.id
        val countDownf2eButton = findViewById<ImageButton>(R.id.buttonDownf2e)
        countDownf2eButton.tag = count!!.id
        val countDownf3eButton = findViewById<ImageButton>(R.id.buttonDownf3e)
        countDownf3eButton.tag = count!!.id
        val countDownpeButton = findViewById<ImageButton>(R.id.buttonDownpe)
        countDownpeButton.tag = count!!.id
        val countDownleButton = findViewById<ImageButton>(R.id.buttonDownle)
        countDownleButton.tag = count!!.id
        val countDowneeButton = findViewById<ImageButton>(R.id.buttonDownee)
        countDowneeButton.tag = count!!.id
    }

    // Count up/down and set value on screen
    fun countUpf1e() {
        countCountf1e.text = count!!.increase_f1e().toString()
    }

    fun countDownf1e() {
        countCountf1e.text = count!!.safe_decrease_f1e().toString()
    }

    fun countUpf2e() {
        countCountf2e.text = count!!.increase_f2e().toString()
    }

    fun countDownf2e() {
        countCountf2e.text = count!!.safe_decrease_f2e().toString()
    }

    fun countUpf3e() {
        countCountf3e.text = count!!.increase_f3e().toString()
    }

    fun countDownf3e() {
        countCountf3e.text = count!!.safe_decrease_f3e().toString()
    }

    fun countUppe() {
        countCountpe.text = count!!.increase_pe().toString()
    }

    fun countDownpe() {
        countCountpe.text = count!!.safe_decrease_pe().toString()
    }

    fun countUple() {
        countCountle.text = count!!.increase_le().toString()
    }

    fun countDownle() {
        countCountle.text = count!!.safe_decrease_le().toString()
    }

    fun countUpee() {
        countCountee.text = count!!.increase_ee().toString()
    }

    fun countDownee() {
        countCountee.text = count!!.safe_decrease_ee().toString()
    }
}