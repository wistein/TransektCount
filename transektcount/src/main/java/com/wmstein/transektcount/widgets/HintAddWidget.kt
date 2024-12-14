package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import com.wmstein.transektcount.R
import java.util.Objects

/******************************************
 * HintAddWidget used by AddSpeciesActivity
 * shows single Hint line with search field
 * last edited on 2024-10-15
 */
class HintAddWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    var searchA: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_add_hint, this, true)
        searchA = findViewById(R.id.searchA)
    }

    fun setSearchA(name: String?) {
        searchA.hint = name
    }

}
