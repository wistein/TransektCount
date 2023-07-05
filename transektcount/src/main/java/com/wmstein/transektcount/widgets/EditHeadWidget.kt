package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.transektcount.R
import java.util.Objects

/*****************************************************************
 * EditHeadWidget.java used by EditMetaActivity.java
 * Created by wmstein for TransektCount on 31.03.2016.
 * Last edited in Java on 2023-05-09
 * converted to Kotlin on 2023-06-26
 */
class EditHeadWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    val widget_no // used for transect_no title
            : TextView
    val widget_no1 // used for transect_no
            : EditText
    val widget_name // used for inspector_name title
            : TextView
    val widget_name1 // used for inspector_name
            : EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_head, this, true)
        widget_no = findViewById(R.id.widgetNo)
        widget_no1 = findViewById(R.id.widgetNo1)
        widget_name = findViewById(R.id.widgetName)
        widget_name1 = findViewById(R.id.widgetName1)
    }

    fun setWidgetNo(title: String?) {
        widget_no.text = title
    }

    fun setWidgetName(title: String?) {
        widget_name.text = title
    }

    var widgetNo1: String?
        get() = widget_no1.text.toString()
        set(name) {
            widget_no1.setText(name)
        }
    var widgetName1: String?
        get() = widget_name1.text.toString()
        set(name) {
            widget_name1.setText(name)
        }

    fun setHint(hint: String?) {
        widget_no.hint = hint
    }
}