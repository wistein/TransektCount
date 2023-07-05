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
 * last edited in Java on 2023-05-09
 * converted to Kotlin on 2023-06-26
 */
class ListMetaWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    val widget_lmeta1 // temperature
            : TextView
    val widget_litem1: TextView
    val widget_lmeta2 // wind
            : TextView
    val widget_litem2: TextView
    val widget_lmeta3 // clouds
            : TextView
    val widget_litem3: TextView
    val widget_ldate1 // date
            : TextView
    val widget_ldate2: TextView
    val widget_ltime1 // start_tm
            : TextView
    val widget_litem4: TextView
    val widget_ltime2 // end_tm
            : TextView
    val widget_litem5: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_meta, this, true)
        widget_lmeta1 = findViewById(R.id.widgetLMeta1)
        widget_litem1 = findViewById(R.id.widgetLItem1)
        widget_lmeta2 = findViewById(R.id.widgetLMeta2)
        widget_litem2 = findViewById(R.id.widgetLItem2)
        widget_lmeta3 = findViewById(R.id.widgetLMeta3)
        widget_litem3 = findViewById(R.id.widgetLItem3)
        widget_ldate1 = findViewById(R.id.widgetLDate1)
        widget_ldate2 = findViewById(R.id.widgetLDate2)
        widget_ltime1 = findViewById(R.id.widgetLTime1)
        widget_litem4 = findViewById(R.id.widgetLItem4)
        widget_ltime2 = findViewById(R.id.widgetLTime2)
        widget_litem5 = findViewById(R.id.widgetLItem5)
    }

    // Following the SETS
    // temperature
    fun setWidgetLMeta1(title: String?) {
        widget_lmeta1.text = title
    }

    fun setWidgetLItem1(name: Int) {
        widget_litem1.text = name.toString()
    }

    // wind
    fun setWidgetLMeta2(title: String?) {
        widget_lmeta2.text = title
    }

    fun setWidgetLItem2(name: Int) {
        widget_litem2.text = name.toString()
    }

    // clouds
    fun setWidgetLMeta3(title: String?) {
        widget_lmeta3.text = title
    }

    fun setWidgetLItem3(name: Int) {
        widget_litem3.text = name.toString()
    }

    // date
    fun setWidgetLDate1(title: String?) {
        widget_ldate1.text = title
    }

    fun setWidgetLDate2(name: String?) {
        widget_ldate2.text = name
    }

    // start_tm
    fun setWidgetLTime1(title: String?) {
        widget_ltime1.text = title
    }

    fun setWidgetLItem4(name: String?) {
        widget_litem4.text = name
    }

    // end_tm
    fun setWidgetLTime2(title: String?) {
        widget_ltime2.text = title
    }

    fun setWidgetLItem5(name: String?) {
        widget_litem5.text = name
    }
}