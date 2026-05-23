package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

import com.wmstein.transektcount.R

import java.util.Objects

/**************************************************************
 * Used by EditSectionListActivity.kt and widget_edit_title.xml
 *
 * Created by milo on 05/05/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016,
 * last edited in Java on 2023-05-09,
 * converted to Kotlin on 2023-06-26,
 * Last edited on 2026-05-23.
 */
class EditSectionListTitleWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widgetTitle: TextView
    private val sectName: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_title, this, true)
        widgetTitle = findViewById(R.id.widgeteditTitle)
        sectName = findViewById(R.id.editsectionName)
    }

    fun setWidgetTitle(title: String?) {
        widgetTitle.text = title
    }

    var sectionName: String?
        get() = sectName.text.toString()
        set(name) {
            sectName.setText(name)
        }

}
