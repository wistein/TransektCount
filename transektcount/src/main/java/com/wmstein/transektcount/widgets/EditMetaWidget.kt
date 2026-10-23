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
 * EditMetaWidget.kt used by EditMetaActivity.java
 *
 * Created by wmstein for TransektCount on 2016-04-02,
 * last edited in java on 2024-12-06,
 * converted to Kotlin on 2025-11-15,
 * last edited on 2026-10-21.
 **************************************************************************************************/

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

import com.wmstein.transektcount.R

import java.util.Objects

class EditMetaWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // date
    private val widgetDateHead: TextView
    private val widgetDatum: TextView

    // start time
    private val widgetStartTimeHead: TextView
    private val widgetStartTime: TextView

    // end time
    private val widgetEndTimeHead: TextView
    private val widgetEndTime: TextView

    // temperature
    private val widgetTempHead: TextView
    private val widgetStartTemp: EditText
    private val widgetEndTemp: EditText

    // wind
    private val widgetWindHead: TextView
    private val widgetStartWind: EditText
    private val widgetEndWind: EditText

    // clouds
    private val widgetCloudsHead: TextView
    private val widgetStartClouds: EditText
    private val widgetEndClouds: EditText

    // notes
    private val widgetNotesHead: TextView
    private val widgetNotesText: TextView

    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_edit_meta, this, true)

        widgetDateHead = findViewById(R.id.widgetDateTitle)
        widgetDatum = findViewById(R.id.widgetDateValue)

        widgetStartTimeHead = findViewById(R.id.widgetStartTimeTitle)
        widgetStartTime = findViewById(R.id.widgetStartTimeValue)

        widgetEndTimeHead = findViewById(R.id.widgetEndTimeTitle)
        widgetEndTime = findViewById(R.id.widgetEndTimeValue)

        widgetTempHead = findViewById(R.id.widgetTempTitle)
        widgetStartTemp = findViewById(R.id.widgetStartTempValue)
        widgetEndTemp = findViewById(R.id.widgetEndTempValue)

        widgetWindHead = findViewById(R.id.widgetWindTitle)
        widgetStartWind = findViewById(R.id.widgetStartWindValue)
        widgetEndWind = findViewById(R.id.widgetEndWindValue)

        widgetCloudsHead = findViewById(R.id.widgetCloudsTitle)
        widgetStartClouds = findViewById(R.id.widgetStartCloudsValue)
        widgetEndClouds = findViewById(R.id.widgetEndCloudsValue)

        widgetNotesHead = findViewById(R.id.widgetNoteTitle)
        widgetNotesText = findViewById(R.id.widgetNoteValue)
    }

    // Following the SETS
    // date
    fun setWidgetDate1(title: String) {
        widgetDateHead.text = title
    }

    fun setWidgetDate2(name: String) {
        widgetDatum.text = name
    }

    // start_tm
    fun setWidgetSTime1(title: String) {
        widgetStartTimeHead.text = title
    }

    fun setWidgetSTime2(name: String) {
        widgetStartTime.text = name
    }

    // end_tm
    fun setWidgetETime1(title: String) {
        widgetEndTimeHead.text = title
    }

    fun setWidgetETime2(name: String) {
        widgetEndTime.text = name
    }

    // temperature
    fun setWidgetTemp1(title: String) {
        widgetTempHead.text = title
    }

    // wind
    fun setWidgetWind1(title: String) {
        widgetWindHead.text = title
    }

    // clouds
    fun setWidgetClouds1(title: String) {
        widgetCloudsHead.text = title
    }

    // note
    fun setWidgetNote1(title: String) {
        widgetNotesHead.text = title
    }

    fun setWidgetNote2(name: String) {
        widgetNotesText.text = name
    }

    // Plausi for numeric input
    val regEx: String = "^[0-9]*$"

    // Following the GETS
    // date
    val widgetDate: String
        get() = widgetDatum.text.toString()

    // start time
    val widgetSTime: String
        get() = widgetStartTime.text.toString()

    // stop time
    val widgetETime: String
        get() = widgetEndTime.text.toString()

    // note
    val widgetNote: String
        get() = widgetNotesText.text.toString()

    // Following the GETS and SETS
    // start temperature with plausi
    var widgetTempS: Int
        get() {
            val text = widgetStartTemp.text.toString()
            return if (text.isEmpty()) 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) 100
            else try {
                text.replace("\\D".toRegex(), "").toInt()
            } catch (_: NumberFormatException) {
                100
            }
        }
        set(value) {
            if (value > 0) {
                widgetStartTemp.setText(value.toString())
            }
        }

    // end temperature with plausi
    var widgetTempE: Int
        get() {
            val text = widgetEndTemp.text.toString()
            return if (text.isEmpty()) 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) 100
            else try {
                text.replace("\\D".toRegex(), "").toInt()
            } catch (_: NumberFormatException) {
                100
            }
        }
        set(value) {
            if (value > 0) {
                widgetEndTemp.setText(value.toString())
            }
        }

    // start wind with plausi
    var widgetWinds: Int
        get() {
            val text = widgetStartWind.text.toString()
            return if (text.isEmpty()) 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) 100
            else try {
                text.replace("\\D".toRegex(), "").toInt()
            } catch (_: NumberFormatException) {
                100
            }
        }
        set(value) {
            if (value > 0) {
                widgetStartWind.setText(value.toString())
            }
        }

    // end wind with plausi
    var widgetWinde: Int
        get() {
            val text = widgetEndWind.text.toString()
            return if (text.isEmpty()) 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) 100
            else try {
                text.replace("\\D".toRegex(), "").toInt()
            } catch (_: NumberFormatException) {
                100
            }
        }
        set(value) {
            if (value > 0) {
                widgetEndWind.setText(value.toString())
            }
        }

    // start clouds with plausi
    var widgetCloudsS: Int
        get() {
            val text = widgetStartClouds.text.toString()
            return if (text.isEmpty()) 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) 200
            else try {
                val number = text.replace("\\D".toRegex(), "").toInt()
                // Check plausi and round integer value to decade (number+5)/10*10
                if (number in 0 .. 100)
                    return (number + 5) / 10 * 10
                else 200
            } catch (_: NumberFormatException) {
                200
            }
        }
        set(value) {
            if (value in 0..100) {
                widgetStartClouds.setText(value.toString())
            }
        }

    // end clouds with plausi
    var widgetCloudsE: Int
        get() {
            val text = widgetEndClouds.text.toString()
            return if (text.isEmpty()) 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) 200
            else try {
                val number = text.replace("\\D".toRegex(), "").toInt()
                // Check plausi and round integer value to decade (number+5)/10*10
                if (number in 0 .. 100)
                    return (number + 5) / 10 * 10
                else 200
            } catch (_: NumberFormatException) {
                200
            }
        }
        set(value) {
            if (value in 0..100) {
                widgetEndClouds.setText(value.toString())
            }
        }

}
