package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.transektcount.R
import java.util.Objects

/*******************************************************
 * Used by EditSectionActivity and widget_edit_notes.xml
 * Created by wmstein on 23.10.2016,
 * last edited in Java on 2023-05-09,
 * converted to Kotlin on 2023-06-26,
 * Last edit on 2023-09-23.
 */
class EditNotesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widgetnotes: TextView
    private val sectionnotes: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_notes, this, true)
        widgetnotes = findViewById(R.id.widgetNotes)
        sectionnotes = findViewById(R.id.sectionNotes)
    }

    fun setWidgetNotes(title: String?) {
        widgetnotes.text = title
    }

    var sectionNotes: String?
        get() = sectionnotes.text.toString()
        set(name) {
            sectionnotes.setText(name)
        }

    fun setHint(hint: String?) {
        sectionnotes.hint = hint
    }

}
