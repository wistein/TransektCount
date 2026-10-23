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
 * CountingHead2Widget.kt is the interface for widget_counting_head2.xml
 * fills headline of Internal counting area with edit button.
 *
 * Created by wmstein 2016-12-18,
 * last edited in Java on 2023-05-09,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2026-10-21.
 **************************************************************************************************/

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

@SuppressLint("ViewConstructor")
class CountingHead2Widget(context: Context, attrs: AttributeSet?) :
    RelativeLayout(context, attrs) {
    private val countHead2: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_counting_head2, this, true)

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
