package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.transektcount.R
import java.util.Objects

/*************************************************************
 * HintWidget used by CountingActivity and EditSpeciesListActivity
 * shows single Hint line
 * Created by wmstein on 2023-05-16,
 * last edited in java on 2023-05-16,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2023-09-03
 */
class HintWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val textView: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_hint, this, true)
        textView = findViewById(R.id.hint_text)
    }

    fun setHint1(notes: String?) {
        textView.text = notes
    }

}
