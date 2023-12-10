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
 * Last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-06-26,
 * Last edit on 2023-09-23.
 */
class ListHeadWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widgetLno // used for transect_no title
            : TextView
    private val widgetLno1 // used for transect_no
            : TextView
    private val widgetLname // used for inspector_name title
            : TextView
    private val widgetLname1 // used for inspector_name
            : TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_head, this, true)
        widgetLno = findViewById(R.id.widgetLNo)
        widgetLno1 = findViewById(R.id.widgetLNo1)
        widgetLname = findViewById(R.id.widgetLName)
        widgetLname1 = findViewById(R.id.widgetLName1)
    }

    fun setWidgetLNo(title: String?) {
        widgetLno.text = title
    }

    fun setWidgetLNo1(name: String?) {
        widgetLno1.text = name
    }

    fun setWidgetLName(title: String?) {
        widgetLname.text = title
    }

    fun setWidgetLName1(name: String?) {
        widgetLname1.text = name
    }
}