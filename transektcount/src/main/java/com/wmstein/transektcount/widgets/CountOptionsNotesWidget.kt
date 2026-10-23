package com.wmstein.transektcount.widgets

/***************************************************************************************************
 * Copyright © 2016-2026, Wilhelm Stein, Bonn, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use any of its files except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************************************
 *
 * CountOptionsNotesWidget.kt
 * Used by CountOptionsActivity and widget_edit_notes.xml
 *
 * Created by wmstein on 2016-10-23,
 * last edited in Java on 2023-05-09,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2026-10-21.
 **************************************************************************************************/

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
