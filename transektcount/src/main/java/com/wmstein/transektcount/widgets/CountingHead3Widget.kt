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
 * CountingHead3Widget.kt is the interface for widget_counting_head3.xml
 * shows the headline for external counts
 *
 * Created by wmstein 2016-12-18,
 * last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-10-06,
 * last edited on 2026-10-21.
 **************************************************************************************************/

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView

import com.wmstein.transektcount.R

import java.util.Objects

class CountingHead3Widget(
    context: Context,
    attrs: AttributeSet?
) : RelativeLayout(context, attrs) {
    private val countHead3: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_counting_head3, this, true)

        countHead3 = findViewById(R.id.countHead3)
    }

    fun setCountHead3() {
        countHead3.text = context.getString(R.string.countExternalHint)
    }
}
