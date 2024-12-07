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
 * EditMetaHeadWidget.kt used by EditMetaActivity.java
 * Created by wmstein for TransektCount on 31.03.2016.
 * Last edited in Java on 2023-05-09,
 * converted to Kotlin on 2023-06-26,
 * Last edit on 2024-12-06
 */
class EditMetaHeadWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widgetno // used for transect_no title
            : TextView
    private var widgetno1 // used for transect_no
            : EditText
    private val widgetname // used for inspector_name title
            : TextView
    private var widgetname1 // used for inspector_name
            : EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_meta_head, this, true)
        widgetno = findViewById(R.id.widgetNo)
        widgetno1 = findViewById(R.id.widgetNo1)
        widgetname = findViewById(R.id.widgetName)
        widgetname1 = findViewById(R.id.widgetName1)
    }

    fun setWidgetNo(title: String?) {
        widgetno.text = title
    }

    fun setWidgetName(title: String?) {
        widgetname.text = title
    }

    var widgetNo1: String?
        get() = widgetno1.text.toString()
        set(name) {
            widgetno1.setText(name)
        }
    var widgetName1: String?
        get() = widgetname1.text.toString()
        set(name) {
            widgetname1.setText(name)
        }

    fun clearWidgetsMetaHead() {
        widgetno1.setText(null)
        widgetname1.setText(null)
    }

}
