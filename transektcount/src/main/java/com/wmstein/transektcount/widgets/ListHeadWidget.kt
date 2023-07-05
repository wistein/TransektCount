package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.transektcount.R
import java.util.Objects

/****************************************************
 * EditHeadWidget.java used by EditMetaActivity.java
 * Created by wmstein for TransektCount on 03.04.2016
 * Last edited in Java on 2021-01-26
 * converted to Kotlin on 2023-06-26
 */
class ListHeadWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widget_lno // used for transect_no title
            : TextView
    private val widget_lno1 // used for transect_no
            : TextView
    private val widget_lname // used for inspector_name title
            : TextView
    private val widget_lname1 // used for inspector_name
            : TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_head, this, true)
        widget_lno = findViewById(R.id.widgetLNo)
        widget_lno1 = findViewById(R.id.widgetLNo1)
        widget_lname = findViewById(R.id.widgetLName)
        widget_lname1 = findViewById(R.id.widgetLName1)
    }

    fun setWidgetLNo(title: String?) {
        widget_lno.text = title
    }

    fun setWidgetLNo1(name: String?) {
        widget_lno1.text = name
    }

    fun setWidgetLName(title: String?) {
        widget_lname.text = title
    }

    fun setWidgetLName1(name: String?) {
        widget_lname1.text = name
    }
}