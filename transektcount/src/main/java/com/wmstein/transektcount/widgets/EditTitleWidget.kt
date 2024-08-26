/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */
package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.transektcount.R
import java.util.Objects

/*******************************************************
 * Used by EditSpeciesListActivity and widget_edit_title.xml
 * Created by by milo on 05/05/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016,
 * last edited in Java on 2023-05-09,
 * converted to Kotlin on 2023-06-26,
 * Last edit on 2023-10-06.
 */
class EditTitleWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widgettitle: TextView
    private val sectionname: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_title, this, true)
        widgettitle = findViewById(R.id.widgeteditTitle)
        sectionname = findViewById(R.id.editsectionName)
    }

    fun setWidgetTitle(title: String?) {
        widgettitle.text = title
    }

    var sectionName: String?
        get() = sectionname.text.toString()
        set(name) {
            sectionname.setText(name)
        }

}