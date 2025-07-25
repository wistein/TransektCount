/*
 * Copyright © 2016-2024. Wilhelm Stein, Bonn, Germany.
 */
package com.wmstein.transektcount.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import com.wmstein.transektcount.R
import com.wmstein.transektcount.database.Count
import java.util.Objects

/***********************************************************
 * Interface for widget_counting_head2.xml
 * fills headline of Internal counting area with edit button
 * Created by wmstein 18.12.2016,
 * last edited in Java on 2023-05-09,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2024-10-23
 */
@SuppressLint("ViewConstructor")
class CountingWidgetHead2(context: Context, attrs: AttributeSet?) :
    RelativeLayout(context, attrs) {
    private val countHead2: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_head2, this, true)
        countHead2 = findViewById(R.id.countHead2)
    }

    fun setCountHead2(count: Count) {
        // set TextView countHead2
        countHead2.text = context.getString(R.string.countInternalHint)

        // set ImageButton Edit
        val editButton = findViewById<ImageButton>(R.id.buttonEdit)
        editButton.tag = count.id
    }

}
