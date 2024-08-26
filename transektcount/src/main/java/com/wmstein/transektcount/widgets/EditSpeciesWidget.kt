package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import com.wmstein.transektcount.R
import com.wmstein.transektcount.TransektCountApplication
import com.wmstein.transektcount.database.Count
import java.io.Serializable
import java.util.Objects

/****************************************************
 * EditSpeciesWidget is used by EditSpeciesListActivity
 * Adopted for TransektCount by wmstein on 18.02.2016,
 * last edited in Java on 2020-10-18,
 * converted to Kotlin on 2023-06-26,
 * Last edited on 2024-08-23
 */
class EditSpeciesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs),
    Serializable {
    @Transient
    private val countName: EditText

    @Transient
    private val countNameG: EditText

    @Transient
    private val countCode: EditText

    @Transient
    private val pSpecies: ImageView

    @JvmField
    var countId = 0

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_count, this, true)
        countName = findViewById(R.id.countName)
        countNameG = findViewById(R.id.countNameG)
        countCode = findViewById(R.id.countCode)
        pSpecies = findViewById(R.id.pSpec)
    }

    fun getCountName(): String {
        return countName.text.toString()
    }

    fun setCountName(name: String?) {
        countName.setText(name)
    }

    fun getCountNameG(): String {
        return countNameG.text.toString()
    }

    fun setCountNameG(name: String?) {
        countNameG.setText(name)
    }

    fun getCountCode(): String {
        return countCode.text.toString()
    }

    fun setCountCode(name: String?) {
        countCode.setText(name)
    }

    fun setCountId(id: Int) {
        countId = id
    }

    fun setPSpec(spec: Count) {
        val rname = "p" + spec.code // species picture resource name
        val transektCountApp = TransektCountApplication()
        val resId = transektCountApp.getResId(rname)

        if (resId != 0) {
            pSpecies.setImageResource(resId)
        }
    }
}