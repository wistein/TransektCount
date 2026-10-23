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
 * EditSectionListWidget is used by EditSectionListActivity.kt
 *
 * Created by wmstein on 18.02.2016,
 * last edited in Java on 2020-10-18,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2026-10-21.
 **************************************************************************************************/

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout

import com.wmstein.transektcount.R
import com.wmstein.transektcount.database.Count

import java.io.Serializable
import java.util.Objects

class EditSectionListWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs),
    Serializable {
    @Transient
    private val countName: EditText

    @Transient
    private val countNameG: EditText

    @Transient
    private val countCode: EditText

    @Transient
    private val pSpecies: ImageView

    @JvmField
    var countId = 0

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_edit_count, this, true)

        countName = findViewById(R.id.countName)
        countNameG = findViewById(R.id.countNameG)
        countCode = findViewById(R.id.countCode)
        pSpecies = findViewById(R.id.pSpec)
    }

    fun getCountName(): String {
        return countName.text.toString()
    }

    fun setCountName(name: String) {
        countName.setText(name)
    }

    fun getCountNameG(): String {
        return countNameG.text.toString()
    }

    fun setCountNameG(name: String) {
        countNameG.setText(name)
    }

    fun getCountCode(): String {
        return countCode.text.toString()
    }

    fun setCountCode(name: String) {
        countCode.setText(name)
    }

    fun getCountId(): Int {
        return countId
    }

    fun setCountId(id: Int) {
        countId = id
    }

    @SuppressLint("DiscouragedApi")
    fun setPicSpec(spec: Count) {
        val rName = "p" + spec.code // species picture resource name
        val resId = resources.getIdentifier(rName, "drawable", context.packageName)
        if (resId != 0) {
            pSpecies.setImageResource(resId)
        }
    }
}