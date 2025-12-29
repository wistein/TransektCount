package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.transektcount.R
import java.util.Objects

/********************************************************
 * ResultsMetaWidget.kt is used by ShowResultsActivity.kt
 * Created by wmstein for TransektCount on 03.04.2016,
 * last edited in Java on 2023-05-09,
 * converted to Kotlin on 2023-06-26,
 * Last edited on 2025-11-15
 */
class ResultsMetaWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // temperature
    private val widgetLTemp: TextView
    private val widgetLTemps: TextView
    private val widgetLTempe: TextView
    // wind
    private val widgetLWind: TextView
    private val widgetLWinds: TextView
    private val widgetLWinde: TextView
    // clouds
    private val widgetLCloud: TextView
    private val widgetLClouds: TextView
    private val widgetLCloude: TextView
    // date
    private val widgetLdate1: TextView
    private val widgetLdate2: TextView
    // start_tm
    private val widgetLtime1: TextView
    private val widgetLitem4: TextView
    // end_tm
    private val widgetLtime2: TextView
    private val widgetLitem5: TextView
    // note
    private val widgetLNote1: TextView
    private val widgetLNote2: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_meta, this, true)
        widgetLTemp = findViewById(R.id.widgetLTemp)
        widgetLTemps = findViewById(R.id.widgetLTemps)
        widgetLTempe = findViewById(R.id.widgetLTempe)

        widgetLWind = findViewById(R.id.widgetLWind)
        widgetLWinds = findViewById(R.id.widgetLWinds)
        widgetLWinde = findViewById(R.id.widgetLWinde)

        widgetLCloud = findViewById(R.id.widgetLCloud)
        widgetLClouds = findViewById(R.id.widgetLClouds)
        widgetLCloude = findViewById(R.id.widgetLCloude)

        widgetLdate1 = findViewById(R.id.widgetLDate1)
        widgetLdate2 = findViewById(R.id.widgetLDate2)

        widgetLtime1 = findViewById(R.id.widgetLTime1)
        widgetLitem4 = findViewById(R.id.widgetLItem4)

        widgetLtime2 = findViewById(R.id.widgetLTime2)
        widgetLitem5 = findViewById(R.id.widgetLItem5)

        widgetLNote1 = findViewById(R.id.widgetLNote1)
        widgetLNote2 = findViewById(R.id.widgetLNote2)
    }

    // Following the SETS
    // temperature
    fun setWidgetLTemp(title: String?) {
        widgetLTemp.text = title
    }

    fun setWidgetLTemps(name: Int) {
        if (name > 0) widgetLTemps.text = name.toString()
    }

    fun setWidgetLTempe(name: Int) {
        if (name > 0) widgetLTempe.text = name.toString()
    }

    // wind
    fun setWidgetLWind(title: String?) {
        widgetLWind.text = title
    }

    fun setWidgetLWinds(name: Int) {
        if (name > 0) widgetLWinds.text = name.toString()
    }

    fun setWidgetLWinde(name: Int) {
        if (name > 0) widgetLWinde.text = name.toString()
    }

    // clouds
    fun setWidgetLCloud(title: String?) {
        widgetLCloud.text = title
    }

    fun setWidgetLClouds(name: Int) {
        if (name > 0) widgetLClouds.text = name.toString()
    }

    fun setWidgetLCloude(name: Int) {
        if (name > 0) widgetLCloude.text = name.toString()
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

    // note
    fun setWidgetLNote1(title: String?) {
        widgetLNote1.text = title
    }

    fun setWidgetLNote2(name: String?) {
        widgetLNote2.text = name
    }

}