package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import com.wmstein.transektcount.R
import java.io.Serializable
import java.util.Objects

/**************************************************************************
 * This is the widget for creating an alert in the CountOptionsActivity.
 * Created by milo on 02/06/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016
 * last edited in Java on 2023-05-09
 * converted to Kotlin on 2023-06-26
 * Last edit on 2023-09-23.
 */
class AlertCreateWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs),
    Serializable {
    private val alertname: EditText
    private val alertvalue: EditText
    private var alertid: Int
    private val deleteButton: ImageButton

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_alert_create, this, true)
        alertname = findViewById(R.id.alert_name)
        alertvalue = findViewById(R.id.alert_value)
        alertid = 0
        deleteButton = findViewById(R.id.delete_button)
        deleteButton.tag = 0
    }

    var alertName: String?
        get() = alertname.text.toString()
        set(name) {
            alertname.setText(name)
        }

    // this is set to return 0 if it can't parse a value from the box in order
    //   that transektcount doesn't crash
    var alertValue: Int
        get() {
            val text = alertvalue.text.toString()
            return if (isEmpty(text)) {
                0
            } else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (e: NumberFormatException) {
                    0
                }
            }
        }
        set(value) {
            alertvalue.setText(value.toString())
        }
    var alertId: Int
        get() = alertid
        set(id) {
            alertid = id
            deleteButton.tag = id
        }

    companion object {
        /**
         * Following function is taken from the Apache commons-lang3-3.4 library
         * licensed under Apache License Version 2.0, January 2004
         *
         *
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