package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.transektcount.AutoFitEditText
import com.wmstein.transektcount.R
import java.util.Objects

/***********************************************************************
 * Edit options for species
 * used by CountOptionsActivity in conjunction with widget_options.xml
 * Created by wmstein on 2023-09-03,
 * last edited on 2023-09-18
 */
class OptionsWidgetExt(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val instructionsf1e: TextView
    private val numberf1e: AutoFitEditText
    private val instructionsf2e: TextView
    private val numberf2e: AutoFitEditText
    private val instructionsf3e: TextView
    private val numberf3e: AutoFitEditText
    private val instructionspe: TextView
    private val numberpe: AutoFitEditText
    private val instructionsle: TextView
    private val numberle: AutoFitEditText
    private val instructionsee: TextView
    private val numberee: AutoFitEditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_options_ext, this, true)

        //For transect external counters
        instructionsf1e = findViewById(R.id.help_textf1e)
        numberf1e = findViewById(R.id.counta_parameter_editf1e)
        instructionsf2e = findViewById(R.id.help_textf2e)
        numberf2e = findViewById(R.id.counta_parameter_editf2e)
        instructionsf3e = findViewById(R.id.help_textf3e)
        numberf3e = findViewById(R.id.counta_parameter_editf3e)
        instructionspe = findViewById(R.id.help_textpe)
        numberpe = findViewById(R.id.counta_parameter_editpe)
        instructionsle = findViewById(R.id.help_textle)
        numberle = findViewById(R.id.counta_parameter_editle)
        instructionsee = findViewById(R.id.help_textee)
        numberee = findViewById(R.id.counta_parameter_editee)
    }

    fun setInstructionsf1e(i: String?) {
        instructionsf1e.text = i
    }

    fun setInstructionsf2e(i: String?) {
        instructionsf2e.text = i
    }

    fun setInstructionsf3e(i: String?) {
        instructionsf3e.text = i
    }

    fun setInstructionspe(i: String?) {
        instructionspe.text = i
    }

    fun setInstructionsle(i: String?) {
        instructionsle.text = i
    }

    fun setInstructionsee(i: String?) {
        instructionsee.text = i
    }

    // this is set to return 0 if it can't parse a value from the box in order
    // that transektcount doesn't crash
    // For transect internal counters
    // For transect external counters
    var parameterValuef1e: Int
        get() {
            val text = numberf1e.text.toString()
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
        set(i) {
            numberf1e.setText(i.toString())
        }

    var parameterValuef2e: Int
        get() {
            val text = numberf2e.text.toString()
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
        set(i) {
            numberf2e.setText(i.toString())
        }

    var parameterValuef3e: Int
        get() {
            val text = numberf3e.text.toString()
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
        set(i) {
            numberf3e.setText(i.toString())
        }

    var parameterValuepe: Int
        get() {
            val text = numberpe.text.toString()
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
        set(i) {
            numberpe.setText(i.toString())
        }

    var parameterValuele: Int
        get() {
            val text = numberle.text.toString()
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
        set(i) {
            numberle.setText(i.toString())
        }

    var parameterValueee: Int
        get() {
            val text = numberee.text.toString()
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
        set(i) {
            numberee.setText(i.toString())
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
            return cs == null || cs.length == 0
        }
    }
}