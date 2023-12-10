package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.transektcount.MyDebug
import com.wmstein.transektcount.R
import java.util.Objects

/****************************************************
 * Created by milo on 26/05/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016,
 * last edited on 2021-01-26,
 * converted to Kotlin on 2023-06-26,
 * last edit on 2023-11-29.
 */
class NotesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val textView: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_notes, this, true)
        textView = findViewById(R.id.notes_text)
    }

    fun setNotes(notes: String?) {
        textView.text = notes
    }

    fun setFont(large: Boolean) {
        if (large) {
            if (MyDebug.LOG) Log.d(TAG, "35, Setzt große Schrift.")
            textView.textSize = 15f
        } else {
            if (MyDebug.LOG) Log.d(TAG, "38, Setzt kleine Schrift.")
            textView.textSize = 12f
        }
    }

    companion object {
        private const val TAG = "NotesWidget"
    }
}