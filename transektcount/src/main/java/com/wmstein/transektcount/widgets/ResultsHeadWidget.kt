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
 * ResultsHeadWidget.kt is used by EditMetaActivity.java
 *
 * Created by wmstein on 2016-04-03,
 * Last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2026-10-21.
 **************************************************************************************************/

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView

import com.wmstein.transektcount.R

import java.util.Objects

class ResultsHeadWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widgetLno // used for transect_no title
            : TextView
    private val widgetLno1 // used for transect_no
            : TextView
    private val widgetLname // used for inspector_name title
            : TextView
    private val widgetLname1 // used for inspector_name
            : TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_list_head, this, true)

        widgetLno = findViewById(R.id.widgetLNo)
        widgetLno1 = findViewById(R.id.widgetLNo1)
        widgetLname = findViewById(R.id.widgetLName)
        widgetLname1 = findViewById(R.id.widgetLName1)
    }

    fun setWidgetLNo(title: String) {
        widgetLno.text = title
    }

    fun setWidgetLNo1(name: String) {
        widgetLno1.text = name
    }

    fun setWidgetLName(title: String) {
        widgetLname.text = title
    }

    fun setWidgetLName1(name: String) {
        widgetLname1.text = name
    }
}