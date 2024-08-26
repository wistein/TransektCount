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
import com.wmstein.transektcount.database.Count
import java.io.Serializable
import java.util.Objects

/************************************************
 * Used by DelSpeciesActivity
 * shows list of selectable species with name, code, picture and add checkbox
 *
 * Created for TransektCount by wmstein on 2024-07-27,
 * last edited on 2024-08-23
 */
class DeleteSpeciesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs),
    Serializable {
    @Transient
    private val specName: TextView

    @Transient
    private val specNameG: TextView

    @Transient
    private val specCode: TextView

    @Transient
    private val spId: TextView

    @Transient
    private val specPic: ImageView
    private val markButton: CheckBox

    val inflater: LayoutInflater

    init {
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_delete_count, this, true)
        specName = findViewById(R.id.spName)
        specNameG = findViewById(R.id.spNameG)
        specCode = findViewById(R.id.spCode)
        spId = findViewById(R.id.spId)
        specPic = findViewById(R.id.spPic)
        markButton = findViewById(R.id.checkBoxDel)
        markButton.tag = 0
    }

    fun setSpecName(name: String?) {
        specName.text = name
    }

    fun setSpecNameG(nameg: String?) {
        specNameG.text = nameg
    }

    fun setSpecCode(code: String?) {
        specCode.text = code
    }

    fun setSpecId(id: String) {
        spId.text = id
        markButton.tag = id.toInt() - 1
    }

    // get code from Count to set picture resource
    fun setPSpec(spec: Count) {
        val rname = "p" + spec.code // species picture resource name
        val transektCountApp = TransektCountApplication()
        val resId = transektCountApp.getResId(rname)

        if (resId != 0) {
            specPic.setImageResource(resId)
        }
    }
    fun getSpecCode(): String {
        return specCode.text.toString()
    }

    // get state of delete checkbox
    fun getMarkSpec(): Boolean {
        val checked: Boolean
        if (markButton.isChecked)
            checked = true
        else
            checked = false
        return checked
    }

}
