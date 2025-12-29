package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.wmstein.transektcount.R
import java.util.Objects

/*****************************************************
 * Created by milo on 26/05/2014.
 * NotesWidget.kt is used by CountingActivity.java and
 * CountOptionsActivity.kt
 * Adopted for TransektCount by wmstein on 18.02.2016.
 * Uses widget_notes.xml.
 * Last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-06-26,
 * last edit on 2025-11-15
 */
class NotesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val textView: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_notes, this, true)
        textView = findViewById(R.id.notes_text)
    }

    // Set notes with light blue introducer e.g. "Species notes: "
    fun setNotesC(intro: String?, notes: String?) {
        textView.text = HtmlCompat.fromHtml(
            "<font color='#BEFDFD'>$intro</font> $notes",
            HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    // Set simple notes
    fun setNotes(notes: String?) {
        textView.text = notes
    }

}
