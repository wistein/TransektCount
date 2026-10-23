package com.wmstein.transektcount.widgets

/***************************************************************************************************
 * Copyright © 2016-2026, Wilhelm Stein, Bonn, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use any of its files except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************************************
 *
 * CountingExternWidget.kt is the interface for widget_counting_e.xml
 *
 * Created by wmstein on 2016-12-18,
 * last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2026-10-21.
 **************************************************************************************************/

import android.content.Context
import android.content.Context.WINDOW_SERVICE
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

class CountingExternWidget(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    var sHeight: Int = 0
    var ht: Int = 0

    private val namef1e: TextView
    private val namef2e: TextView
    private val namef3e: TextView
    private val namepe: TextView
    private val namele: TextView
    private val nameee: TextView

    private var idf1e: LinearLayout
    private var idf2e: LinearLayout
    private var idf3e: LinearLayout
    private var idpe: LinearLayout
    private var idle: LinearLayout
    private var idee: LinearLayout

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
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_counting_e, this, true)

        idf1e = findViewById(R.id.idf1e)
        idf2e = findViewById(R.id.idf2e)
        idf3e = findViewById(R.id.idf3e)
        idpe = findViewById(R.id.idpe)
        idle = findViewById(R.id.idle)
        idee = findViewById(R.id.idee)

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

    @Suppress("RemoveRedundantQualifierName")
    fun setCounte(newcount: Count?) {
        // Get screen size to adapt the counting view
        val wm = checkNotNull(context.getSystemService(WINDOW_SERVICE) as WindowManager)
        val metrics = wm.currentWindowMetrics
        sHeight = metrics.bounds.top + metrics.bounds.bottom

        // Height for counter line on counting page
        ht = sHeight / 22

        val lparamsf1e: ViewGroup.LayoutParams = idf1e.layoutParams
        lparamsf1e.height = ht
        val lparamsf2e: ViewGroup.LayoutParams = idf2e.layoutParams
        lparamsf2e.height = ht
        val lparamsf3e: ViewGroup.LayoutParams = idf3e.layoutParams
        lparamsf3e.height = ht
        val lparamspe: ViewGroup.LayoutParams = idpe.layoutParams
        lparamspe.height = ht
        val lparamsle: ViewGroup.LayoutParams = idle.layoutParams
        lparamsle.height = ht
        val lparamsee: ViewGroup.LayoutParams = idee.layoutParams
        lparamsee.height = ht

        idf1e.layoutParams = lparamsf1e
        idf2e.layoutParams = lparamsf2e
        idf3e.layoutParams = lparamsf3e
        idpe.layoutParams = lparamspe
        idle.layoutParams = lparamsle
        idee.layoutParams = lparamsee

        count = newcount
        namef1e.text = context.getString(R.string.countImagomfHint)
        namef2e.text = context.getString(R.string.countImagomHint)
        namef3e.text = context.getString(R.string.countImagofHint)
        namepe.text = context.getString(R.string.countPupaHint)
        namele.text = context.getString(R.string.countLarvaHint)
        nameee.text = context.getString(R.string.countOvoHint)

        if (count!!.count_f1e > 0)
            countCountf1e.text = count!!.count_f1e.toString()
        if (count!!.count_f2e > 0)
            countCountf2e.text = count!!.count_f2e.toString()
        if (count!!.count_f3e > 0)
            countCountf3e.text = count!!.count_f3e.toString()
        if (count!!.count_pe > 0)
            countCountpe.text = count!!.count_pe.toString()
        if (count!!.count_le > 0)
            countCountle.text = count!!.count_le.toString()
        if (count!!.count_ee > 0)
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
        countCountf1e.text = count!!.increaseF1e().toString()
    }

    fun countDownf1e() {
        countCountf1e.text = count!!.safeDecreaseF1e().toString()
    }

    fun countUpf2e() {
        countCountf2e.text = count!!.increaseF2e().toString()
    }

    fun countDownf2e() {
        countCountf2e.text = count!!.safeDecreaseF2e().toString()
    }

    fun countUpf3e() {
        countCountf3e.text = count!!.increaseF3e().toString()
    }

    fun countDownf3e() {
        countCountf3e.text = count!!.safeDecreaseF3e().toString()
    }

    fun countUppe() {
        countCountpe.text = count!!.increasePe().toString()
    }

    fun countDownpe() {
        countCountpe.text = count!!.safeDecreasePe().toString()
    }

    fun countUple() {
        countCountle.text = count!!.increaseLe().toString()
    }

    fun countDownle() {
        countCountle.text = count!!.safeDecreaseLe().toString()
    }

    fun countUpee() {
        countCountee.text = count!!.increaseEe().toString()
    }

    fun countDownee() {
        countCountee.text = count!!.safeDecreaseEe().toString()
    }
}
