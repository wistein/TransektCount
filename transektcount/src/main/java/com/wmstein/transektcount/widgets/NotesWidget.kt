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
 * NotesWidget.kt is used by CountingActivity.java and CountOptionsActivity.kt.
 * It uses widget_notes.xml.
 *
 * Created by wmstein on 18.02.2016-02-18,
 * last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2026-10-21.
 **************************************************************************************************/

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView

import com.wmstein.transektcount.R
import com.wmstein.transektcount.Utils.fromHtml

import java.util.Objects

class NotesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val textView: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_notes, this, true)

        textView = findViewById(R.id.notes_text)
    }

    // Set notes with light blue introducer e.g. "Species notes: "
    fun setNotesC(intro: String, notes: String) {
        textView.text = fromHtml("<font color='#BEFDFD'>$intro</font> $notes")
    }

}
