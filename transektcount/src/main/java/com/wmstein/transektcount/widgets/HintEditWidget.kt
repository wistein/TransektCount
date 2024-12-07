package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import com.wmstein.transektcount.R
import java.util.Objects

/************************************************
 * HintEditWidget used by EditSectionListActivity
 * shows single Hint line
 * Created by wmstein on 2023-05-16,
 * last edited in java on 2023-05-16,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2024-10-16
 */
class HintEditWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var searchE: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_hint, this, true)
        searchE = findViewById(R.id.searchE)
    }

    fun setSearchE(name: String?) {
        searchE.hint = name
    }

}
