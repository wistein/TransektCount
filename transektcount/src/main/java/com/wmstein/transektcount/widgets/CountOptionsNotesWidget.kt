package com.wmstein.transektcount.widgets

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView

import com.wmstein.transektcount.R

import java.util.Objects

/********************************************************
 * CountOptionsNotesWidget.kt
 * Used by CountOptionsActivity and widget_edit_notes.xml
 *
 * Created by wmstein on 23.10.2016,
 * last edited in Java on 2023-05-09,
 * converted to Kotlin on 2023-06-26,
 * Last edited on 2026-08-13
 */
class CountOptionsNotesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val specNotesTitle: TextView
    private val specNotesNotes: AutoCompleteTextView
    val suggestions = ArrayList(listOf(*resources.getStringArray(R.array.notes_options)))
    val specNotesAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line,
        suggestions)

    var selectedValue = ""

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_edit_notes, this, true)

        specNotesTitle = findViewById(R.id.widgetNotesTitle)
        specNotesNotes = findViewById(R.id.sNotes)
        specNotesNotes.setAdapter(specNotesAdapter)
        specNotesNotes.setOnFocusChangeListener {
                _, hasFocus ->
            if (hasFocus)
                specNotesNotes.showDropDown()
        }
        specNotesNotes.setOnItemClickListener {
                parent, _, position, _ ->
            selectedValue = parent.getItemAtPosition(position).toString()
        }

        setSpecNotes(selectedValue)
    }

    // Called by CountOptionsActivity
    fun setSpecNotesTitle(title: String) {
        specNotesTitle.text = title
    }

    fun setSpecNotes(note: String) {
        specNotesNotes.text = Editable.Factory.getInstance().newEditable(note)
    }

    var spNotes: String
        get() = specNotesNotes.text.toString()
        set(name) {
            specNotesNotes.setText(name)
        }

}
