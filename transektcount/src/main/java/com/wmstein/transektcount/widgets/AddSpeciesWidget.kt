package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.transektcount.R
import com.wmstein.transektcount.TransektCountApplication
import java.io.Serializable
import java.util.Objects

/************************************************
 * Used by AddSpeciesActivity
 * shows list of selectable species with name, code, picture and add checkbox
 *
 * Created for TourCount by wmstein on 2019-04-03,
 * last edited in Java by wmstein on 2023-05-09,
 * converted to Kotlin on 2023-05-26,
 * renamed to AddSpeciesWidget.kt on 2024-06-14
 * last edited in Kotlin on 2023-08-23
 */
class AddSpeciesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs),
    Serializable {
    @Transient
    private val specName: TextView

    @Transient
    private val specNameG: TextView

    @Transient
    private val specCode: TextView

    @Transient
    private val specId: TextView

    @Transient
    private val specPic: ImageView
    private val markButton: CheckBox

    val inflater: LayoutInflater

    init {
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_add_spec, this, true)
        specName = findViewById(R.id.specName)
        specNameG = findViewById(R.id.specNameG)
        specCode = findViewById(R.id.specCode)
        specId = findViewById(R.id.specId)
        specPic = findViewById(R.id.specPic)
        markButton = findViewById(R.id.checkBoxAdd)
        markButton.tag = 0
    }

    fun getSpecName(): String {
        return specName.text.toString()
    }

    fun setSpecName(name: String?) {
        specName.text = name
    }

    fun getSpecNameG(): String {
        return specNameG.text.toString()
    }

    fun setSpecNameG(nameg: String?) {
        specNameG.text = nameg
    }

    fun getSpecCode(): String {
        return specCode.text.toString()
    }

    fun setSpecCode(code: String?) {
        specCode.text = code
    }

    // get state of add checkbox
    fun getMarkSpec(): Boolean {
        val checked: Boolean
        if (markButton.isChecked)
            checked = true
        else
            checked = false
        return checked
    }

    fun setSpecId(id: String) {
        specId.text = id
        markButton.tag = id.toInt() - 1
    }

    // set picture resource by code
    fun setPSpec(ucode: String) {
        val rname = "p$ucode" // species picture resource name

        // make instance of class TransektCountApplication to reference non-static method 
        val transektCountApp = TransektCountApplication()
        val resId = transektCountApp.getResId(rname)

        if (resId != 0) {
            specPic.setImageResource(resId)
        }
    }
}