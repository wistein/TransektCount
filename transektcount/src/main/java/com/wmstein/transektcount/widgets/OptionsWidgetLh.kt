package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.transektcount.AutoFitEditText
import com.wmstein.transektcount.R
import java.util.Objects

/************************************************************************
 * Edit options for species
 * used by CountOptionsActivity in conjunction with widget_options_lh.xml
 * Created by wmstein on 2016-02-16,
 * last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2025-07-05
 */
class OptionsWidgetLh(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val instructionsf1i: TextView
    private val numberf1i: AutoFitEditText
    private val instructionsf2i: TextView
    private val numberf2i: AutoFitEditText
    private val instructionsf3i: TextView
    private val numberf3i: AutoFitEditText
    private val instructionspi: TextView
    private val numberpi: AutoFitEditText
    private val instructionsli: TextView
    private val numberli: AutoFitEditText
    private val instructionsei: TextView
    private val numberei: AutoFitEditText
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
        Objects.requireNonNull(inflater).inflate(R.layout.widget_options_lh, this, true)

        //For transect internal counters
        instructionsf1i = findViewById(R.id.help_textf1i)
        numberf1i = findViewById(R.id.count_parameter_editf1i)
        instructionsf2i = findViewById(R.id.help_textf2i)
        numberf2i = findViewById(R.id.count_parameter_editf2i)
        instructionsf3i = findViewById(R.id.help_textf3i)
        numberf3i = findViewById(R.id.count_parameter_editf3i)
        instructionspi = findViewById(R.id.help_textpi)
        numberpi = findViewById(R.id.count_parameter_editpi)
        instructionsli = findViewById(R.id.help_textli)
        numberli = findViewById(R.id.count_parameter_editli)
        instructionsei = findViewById(R.id.help_textei)
        numberei = findViewById(R.id.count_parameter_editei)

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

    fun setInstructionsf1i(i: String?) {
        instructionsf1i.text = i
    }

    fun setInstructionsf2i(i: String?) {
        instructionsf2i.text = i
    }

    fun setInstructionsf3i(i: String?) {
        instructionsf3i.text = i
    }

    fun setInstructionspi(i: String?) {
        instructionspi.text = i
    }

    fun setInstructionsli(i: String?) {
        instructionsli.text = i
    }

    fun setInstructionsei(i: String?) {
        instructionsei.text = i
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

    // This is set to return 0 if it can't parse a value from the box
    //   in order that transektcount doesn't crash.
    // For transect internal counters
    var parameterValuef1i: Int
        get() {
            val text = numberf1i.text.toString()
            return if (isEmpty(text)) {
                0
            } else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (_: NumberFormatException) {
                    0
                }
            }
        }
        set(i) {
            numberf1i.setText(i.toString())
        }

    var parameterValuef2i: Int
        get() {
            val text = numberf2i.text.toString()
            return if (isEmpty(text)) {
                0
            } else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (_: NumberFormatException) {
                    0
                }
            }
        }
        set(i) {
            numberf2i.setText(i.toString())
        }

    var parameterValuef3i: Int
        get() {
            val text = numberf3i.text.toString()
            return if (isEmpty(text)) {
                0
            } else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (_: NumberFormatException) {
                    0
                }
            }
        }
        set(i) {
            numberf3i.setText(i.toString())
        }

    var parameterValuepi: Int
        get() {
            val text = numberpi.text.toString()
            return if (isEmpty(text)) {
                0
            } else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (_: NumberFormatException) {
                    0
                }
            }
        }
        set(i) {
            numberpi.setText(i.toString())
        }

    var parameterValueli: Int
        get() {
            val text = numberli.text.toString()
            return if (isEmpty(text)) {
                0
            } else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (_: NumberFormatException) {
                    0
                }
            }
        }
        set(i) {
            numberli.setText(i.toString())
        }

    var parameterValueei: Int
        get() {
            val text = numberei.text.toString()
            return if (isEmpty(text)) {
                0
            } else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (_: NumberFormatException) {
                    0
                }
            }
        }
        set(i) {
            numberei.setText(i.toString())
        }

    // For transect external counters
    var parameterValuef1e: Int
        get() {
            val text = numberf1e.text.toString()
            return if (isEmpty(text)) {
                0
            } else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (_: NumberFormatException) {
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
                } catch (_: NumberFormatException) {
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
                } catch (_: NumberFormatException) {
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
                } catch (_: NumberFormatException) {
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
                } catch (_: NumberFormatException) {
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
                } catch (_: NumberFormatException) {
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
         * Checks if a CharSequence is empty ("") or null.
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