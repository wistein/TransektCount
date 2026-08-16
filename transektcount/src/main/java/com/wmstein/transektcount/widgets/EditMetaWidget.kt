package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

import com.wmstein.transektcount.R

import java.util.Objects

/*****************************************************
 * EditMetaWidget.kt used by EditMetaActivity.java
 * Created by wmstein for TransektCount on 2016-04-02,
 * last edited in java on 2024-12-06,
 * converted to Kotlin on 2025-11-15,
 * last edited on 2026-08-14
 */
class EditMetaWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    val widgetTempHead: TextView
    var widgetStartTemp: EditText // start temperature
    var widgetEndTemp: EditText // end temperature

    val widgetWindHead: TextView
    var widgetStartWind: EditText // start wind
    var widgetEndWind: EditText // end wind

    val widgetCloudsHead: TextView
    var widgetStartClouds: EditText // start clouds
    var widgetEndClouds: EditText // end clouds

    val widgetDateHead: TextView // date
    val widgetDatum: TextView
    val widgetStartTimeHead: TextView // start-time
    val widgetStartTime: TextView
    val widgetEndTimeHead: TextView // end-time
    val widgetEndTime: TextView
    val widgetNotesHead: TextView // notes
    val widgetNotesText: TextView

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_edit_meta, this, true)

        widgetTempHead = findViewById(R.id.widgetTemp1)
        widgetStartTemp = findViewById(R.id.widgetStartTemp2)
        widgetEndTemp = findViewById(R.id.widgetEndTemp2)
        widgetWindHead = findViewById(R.id.widgetWind1)
        widgetStartWind = findViewById(R.id.widgetStartWind2)
        widgetEndWind = findViewById(R.id.widgetEndWind2)
        widgetCloudsHead = findViewById(R.id.widgetClouds1)
        widgetStartClouds = findViewById(R.id.widgetStartClouds2)
        widgetEndClouds = findViewById(R.id.widgetEndClouds2)

        widgetDateHead = findViewById(R.id.widgetDate1)
        widgetDatum = findViewById(R.id.widgetDate2)
        widgetStartTimeHead = findViewById(R.id.widgetSTime1)
        widgetStartTime = findViewById(R.id.widgetSTime2)
        widgetEndTimeHead = findViewById(R.id.widgetETime1)
        widgetEndTime = findViewById(R.id.widgetETime2)
        widgetNotesHead = findViewById(R.id.widgetNote1)
        widgetNotesText = findViewById(R.id.widgetNote2)
    }

    // Following the SETS
    // temperature
    fun setWidgetTemp1(title: String) {
        widgetTempHead.text = title
    }

    fun setWidgetStartTemp2(name: Int) {
        if (name == 0) widgetStartTemp.setText("")
        else widgetStartTemp.setText(name.toString())
    }

    fun setWidgetEndTemp2(name: Int) {
        if (name == 0) widgetEndTemp.setText("")
        else widgetEndTemp.setText(name.toString())
    }

    // wind
    fun setWidgetWind1(title: String) {
        widgetWindHead.text = title
    }

    fun setWidgetStartWind2(name: Int) {
        if (name == 0) widgetStartWind.setText("")
        else widgetStartWind.setText(name.toString())
    }

    fun setWidgetEndWind2(name: Int) {
        if (name == 0) widgetEndWind.setText("")
        else widgetEndWind.setText(name.toString())
    }


    // clouds
    fun setWidgetClouds1(title: String) {
        widgetCloudsHead.text = title
    }

    fun setWidgetStartClouds2(name: Int) {
        if (name == 0) widgetStartClouds.setText("")
        else widgetStartClouds.setText(name.toString())
    }

    fun setWidgetEndClouds2(name: Int) {
        if (name == 0) widgetEndClouds.setText("")
        else widgetEndClouds.setText(name.toString())
    }

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

    // note
    fun setWidgetNote1(title: String) {
        widgetNotesHead.text = title
    }

    fun setWidgetNote2(name: String) {
        widgetNotesText.text = name
    }

    // plausi for numeric input
    val regEx: String = "^[0-9]*$"

    // following the GETS
    val widgetTemps: Int
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

    val widgetTempe: Int
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

    val widgetWinds: Int
        // get wind with plausi
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

    val widgetWinde: Int
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

    val widgetClouds: Int
        // get start clouds with plausi
        get() {
            val text = widgetStartClouds.text.toString()
            return if (text.isEmpty()) 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) 200
            else try {
                // Round integer value to decade (number+5)/10*10
                val number = (text.replace("\\D".toRegex(), "").toInt() + 5) / 10 * 10
                number
            } catch (_: NumberFormatException) {
                200
            }
        }

    val widgetCloude: Int
        // get end clouds with plausi
        get() {
            val text = widgetEndClouds.text.toString()
            return if (text.isEmpty()) 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) 200
            else try {
                val number = (text.replace("\\D".toRegex(), "").toInt() + 5) / 10 * 10
                number
            } catch (_: NumberFormatException) {
                200
            }
        }

    val widgetDate: String
        // get date
        get() = widgetDatum.text.toString()

    val widgetSTime: String
        // get start time
        get() = widgetStartTime.text.toString()

    val widgetETime: String
        // get stop time
        get() = widgetEndTime.text.toString()

    val widgetNote: String
        // get stop time
        get() = widgetNotesText.text.toString()

}
