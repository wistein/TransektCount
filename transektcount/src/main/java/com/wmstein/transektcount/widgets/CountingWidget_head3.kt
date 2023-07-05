/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */
package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.wmstein.transektcount.R
import java.util.Objects

/****************************************************
 * Interface for widget_counting_head3.xml
 * Created by wmstein 18.12.2016
 * Last edited in Java on 2021-01-26
 * converted to Kotlin on 2023-06-26
 */
class CountingWidget_head3(context: Context, attrs: AttributeSet?) :
    RelativeLayout(context, attrs) {
    private val countHead3: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_head3, this, true)
        countHead3 = findViewById(R.id.countHead3)
    }

    fun setCountHead3() {
        countHead3.text = context.getString(R.string.countExternalHint)
    }
}