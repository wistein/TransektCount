package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.transektcount.R
import com.wmstein.transektcount.Utils.fromHtml
import java.util.Objects

/*****************************************************
 * NotesWidget.kt is used by CountingActivity.java and
 * CountOptionsActivity.kt
 *
 * Based on NotesWidget.java from milo of 26/05/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016.
 *
 * Uses widget_notes.xml.
 * Last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2026-02-28
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
        textView.text = fromHtml("<font color='#BEFDFD'>$intro</font> $notes")
    }

}
