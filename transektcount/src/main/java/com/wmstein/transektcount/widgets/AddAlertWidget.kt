package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.wmstein.transektcount.R
import java.util.Objects

/****************************************************
 * Created by milo on 01/06/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016
 * last edited in Java on 2019-02-12
 * converted to Kotlin on 2023-06-26
 */
class AddAlertWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_add_alert, this, true)
    }
}