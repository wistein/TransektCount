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
 * last edited on 2025-11-15
 */
class EditMetaWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    val widget_temp1: TextView // start temperature
    var widget_starttemp2: EditText
    var widget_endtemp2: EditText // end temperature

    val widget_wind1: TextView // start wind
    var widget_startwind2: EditText
    var widget_endwind2: EditText // end wind

    val widget_clouds1: TextView // start clouds
    var widget_startclouds2: EditText
    var widget_endclouds2: EditText // end clouds

    val widget_date1: TextView // date
    val widget_date2: TextView
    val widget_stime1: TextView // start-time
    val widget_stime2: TextView
    val widget_etime1: TextView // end-time
    val widget_etime2: TextView
    val widget_note1: TextView // notes
    val widget_note2: TextView

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_edit_meta, this, true)
        widget_temp1 = findViewById<TextView>(R.id.widgetTemp1)
        widget_starttemp2 = findViewById<EditText>(R.id.widgetStartTemp2)
        widget_endtemp2 = findViewById<EditText>(R.id.widgetEndTemp2)
        widget_wind1 = findViewById<TextView>(R.id.widgetWind1)
        widget_startwind2 = findViewById<EditText>(R.id.widgetStartWind2)
        widget_endwind2 = findViewById<EditText>(R.id.widgetEndWind2)
        widget_clouds1 = findViewById<TextView>(R.id.widgetClouds1)
        widget_startclouds2 = findViewById<EditText>(R.id.widgetStartClouds2)
        widget_endclouds2 = findViewById<EditText>(R.id.widgetEndClouds2)

        widget_date1 = findViewById<TextView>(R.id.widgetDate1)
        widget_date2 = findViewById<TextView>(R.id.widgetDate2)
        widget_stime1 = findViewById<TextView>(R.id.widgetSTime1)
        widget_stime2 = findViewById<TextView>(R.id.widgetSTime2)
        widget_etime1 = findViewById<TextView>(R.id.widgetETime1)
        widget_etime2 = findViewById<TextView>(R.id.widgetETime2)
        widget_note1 = findViewById<TextView>(R.id.widgetNote1)
        widget_note2 = findViewById<TextView>(R.id.widgetNote2)
    }

    // Following the SETS
    // temperature
    fun setWidgetTemp1(title: String?) {
        widget_temp1.text = title
    }

    fun setWidgetStartTemp2(name: Int) {
        if (name == 0) widget_starttemp2.setText("")
        else widget_starttemp2.setText(name.toString())
    }

    fun setWidgetEndTemp2(name: Int) {
        if (name == 0) widget_endtemp2.setText("")
        else widget_endtemp2.setText(name.toString())
    }

    // wind
    fun setWidgetWind1(title: String?) {
        widget_wind1.text = title
    }

    fun setWidgetStartWind2(name: Int) {
        if (name == 0) widget_startwind2.setText("")
        else widget_startwind2.setText(name.toString())
    }

    fun setWidgetEndWind2(name: Int) {
        if (name == 0) widget_endwind2.setText("")
        else widget_endwind2.setText(name.toString())
    }


    // clouds
    fun setWidgetClouds1(title: String?) {
        widget_clouds1.text = title
    }

    fun setWidgetStartClouds2(name: Int) {
        if (name == 0) widget_startclouds2.setText("")
        else widget_startclouds2.setText(name.toString())
    }

    fun setWidgetEndClouds2(name: Int) {
        if (name == 0) widget_endclouds2.setText("")
        else widget_endclouds2.setText(name.toString())
    }

    // date
    fun setWidgetDate1(title: String?) {
        widget_date1.text = title
    }

    fun setWidgetDate2(name: String?) {
        widget_date2.text = name
    }

    // start_tm
    fun setWidgetSTime1(title: String?) {
        widget_stime1.text = title
    }

    fun setWidgetSTime2(name: String?) {
        widget_stime2.text = name
    }

    // end_tm
    fun setWidgetETime1(title: String?) {
        widget_etime1.text = title
    }

    fun setWidgetETime2(name: String?) {
        widget_etime2.text = name
    }

    // note
    fun setWidgetNote1(title: String?) {
        widget_note1.text = title
    }

    fun setWidgetNote2(name: String?) {
        widget_note2.text = name
    }

    // plausi for numeric input
    val regEx: String = "^[0-9]*$"

    // following the GETS
    val widgetTemps: Int
        get() {
            val text = widget_starttemp2.text.toString()
            if (isEmpty(text)) return 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) return 100
            else try {
                return text.replace("\\D".toRegex(), "").toInt()
            } catch (_: NumberFormatException) {
                return 100
            }
        }

    val widgetTempe: Int
        get() {
            val text = widget_endtemp2.text.toString()
            if (isEmpty(text)) return 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) return 100
            else try {
                return text.replace("\\D".toRegex(), "").toInt()
            } catch (_: NumberFormatException) {
                return 100
            }
        }

    val widgetWinds: Int
        // get wind with plausi
        get() {
            val text = widget_startwind2.text.toString()
            if (isEmpty(text)) return 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) return 100
            else try {
                return text.replace("\\D".toRegex(), "").toInt()
            } catch (_: NumberFormatException) {
                return 100
            }
        }

    val widgetWinde: Int
        get() {
            val text = widget_endwind2.text.toString()
            if (isEmpty(text)) return 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) return 100
            else try {
                return text.replace("\\D".toRegex(), "").toInt()
            } catch (_: NumberFormatException) {
                return 100
            }
        }

    val widgetClouds: Int
        // get clouds with plausi
        get() {
            val text = widget_startclouds2.text.toString()
            if (isEmpty(text)) return 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) return 200
            else try {
                return text.replace("\\D".toRegex(), "").toInt()
            } catch (_: NumberFormatException) {
                return 200
            }
        }

    val widgetCloude: Int
        get() {
            val text = widget_endclouds2.text.toString()
            if (isEmpty(text)) return 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) return 200
            else try {
                return text.replace("\\D".toRegex(), "").toInt()
            } catch (_: NumberFormatException) {
                return 200
            }
        }

    val widgetDate: String
        // get get date
        get() = widget_date2.text.toString()

    val widgetSTime: String
        // get start time
        get() = widget_stime2.text.toString()

    val widgetETime: String
        // get stop time
        get() = widget_etime2.text.toString()

    val widgetNote: String
        // get stop time
        get() = widget_note2.text.toString()

    companion object {
        /**
         * Checks if a CharSequence is empty ("") or null.
         *
         *
         * isEmpty(null)      = true
         * isEmpty("")        = true
         * isEmpty(" ")       = false
         * isEmpty("bob")     = false
         * isEmpty("  bob  ") = false
         *
         * @param cs the CharSequence to check, may be null
         * @return `true` if the CharSequence is empty or null
         */
        fun isEmpty(cs: CharSequence?): Boolean {
            return cs.isNullOrEmpty()
        }
    }

}
