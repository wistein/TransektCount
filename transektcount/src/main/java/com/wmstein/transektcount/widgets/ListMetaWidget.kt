package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.transektcount.R
import java.util.Objects

/****************************************************
 * ListMetaWidget.java used by ListSpeciesActivity.java
 * Created by wmstein for TransektCount on 03.04.2016,
 * last edited in Java on 2023-05-09,
 * converted to Kotlin on 2023-06-26,
 * Last edit on 2023-09-23.
 */
class ListMetaWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widgetLmeta1 // temperature
            : TextView
    private val widgetLitem1: TextView
    private val widgetLmeta2 // wind
            : TextView
    private val widgetLitem2: TextView
    private val widgetLmeta3 // clouds
            : TextView
    private val widgetLitem3: TextView
    private val widgetLdate1 // date
            : TextView
    private val widgetLdate2: TextView
    private val widgetLtime1 // start_tm
            : TextView
    private val widgetLitem4: TextView
    private val widgetLtime2 // end_tm
            : TextView
    private val widgetLitem5: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_meta, this, true)
        widgetLmeta1 = findViewById(R.id.widgetLMeta1)
        widgetLitem1 = findViewById(R.id.widgetLItem1)
        widgetLmeta2 = findViewById(R.id.widgetLMeta2)
        widgetLitem2 = findViewById(R.id.widgetLItem2)
        widgetLmeta3 = findViewById(R.id.widgetLMeta3)
        widgetLitem3 = findViewById(R.id.widgetLItem3)
        widgetLdate1 = findViewById(R.id.widgetLDate1)
        widgetLdate2 = findViewById(R.id.widgetLDate2)
        widgetLtime1 = findViewById(R.id.widgetLTime1)
        widgetLitem4 = findViewById(R.id.widgetLItem4)
        widgetLtime2 = findViewById(R.id.widgetLTime2)
        widgetLitem5 = findViewById(R.id.widgetLItem5)
    }

    // Following the SETS
    // temperature
    fun setWidgetLMeta1(title: String?) {
        widgetLmeta1.text = title
    }

    fun setWidgetLItem1(name: Int) {
        widgetLitem1.text = name.toString()
    }

    // wind
    fun setWidgetLMeta2(title: String?) {
        widgetLmeta2.text = title
    }

    fun setWidgetLItem2(name: Int) {
        widgetLitem2.text = name.toString()
    }

    // clouds
    fun setWidgetLMeta3(title: String?) {
        widgetLmeta3.text = title
    }

    fun setWidgetLItem3(name: Int) {
        widgetLitem3.text = name.toString()
    }

    // date
    fun setWidgetLDate1(title: String?) {
        widgetLdate1.text = title
    }

    fun setWidgetLDate2(name: String?) {
        widgetLdate2.text = name
    }

    // start_tm
    fun setWidgetLTime1(title: String?) {
        widgetLtime1.text = title
    }

    fun setWidgetLItem4(name: String?) {
        widgetLitem4.text = name
    }

    // end_tm
    fun setWidgetLTime2(title: String?) {
        widgetLtime2.text = title
    }

    fun setWidgetLItem5(name: String?) {
        widgetLitem5.text = name
    }
}