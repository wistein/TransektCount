package com.wmstein.transektcount.widgets

/***************************************************************************************************
 * Copyright © 2019-2026, Wilhelm Stein, Bonn, Germany
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
 * AddSpeciesWidget.kt is used by AddSpeciesActivity.kt
 * It shows the list of selectable species with
 * name, code, picture and an add checkbox.
 *
 * SpeciesAddWidget.java created for TourCount by wmstein on 2019-04-03,
 * last edited for TourCount in Java by wmstein on 2023-05-09,
 * converted to Kotlin on 2023-05-26,
 * renamed from SpeciesAddWidget.kt to AddSpeciesWidget.kt on 2024-06-14
 * AddSpeciesWidget.kt adapted for TransektCount on 2023-08-23
 * last edited on 2026-10-21.
 **************************************************************************************************/

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.wmstein.transektcount.R

import java.io.Serializable
import java.util.Objects

class AddSpeciesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs),
    Serializable {
    @Transient
    private val specName: TextView

    @Transient
    private val specNameG: TextView

    @Transient
    private val specCode: TextView

    @Transient
    private val specId: TextView

    @Transient
    private val specPic: ImageView
    private val markButton: CheckBox

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
            as LayoutInflater

    init {
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_add_spec, this, true)

        specName = findViewById(R.id.specName)
        specNameG = findViewById(R.id.specNameG)
        specCode = findViewById(R.id.specCode)
        specId = findViewById(R.id.specId)
        specPic = findViewById(R.id.specPic)
        markButton = findViewById(R.id.checkBoxAdd)
        markButton.tag = 0
    }

    fun getSpecName(): String {
        return specName.text.toString()
    }

    fun setSpecName(name: String) {
        specName.text = name
    }

    fun getSpecNameG(): String {
        return specNameG.text.toString()
    }

    fun setSpecNameG(nameg: String) {
        specNameG.text = nameg
    }

    fun getSpecCode(): String {
        return specCode.text.toString()
    }

    fun setSpecCode(code: String) {
        specCode.text = code
    }

    // get state of add checkbox
    fun getMarkSpec(): Boolean {
        val checked: Boolean = markButton.isChecked
        return checked
    }

    // set state of add checkbox
    fun setMarkSpec(state: Boolean) {
        markButton.isChecked = state
    }

    fun setSpecId(id: String) {
        specId.text = id
        markButton.tag = id.toInt() - 1
    }

    // set picture resource by code
    @SuppressLint("DiscouragedApi")
    fun setPicSpec(uCode: String) {
        val rName = "p$uCode" // species picture resource name
        val resId = resources.getIdentifier(rName, "drawable", context.packageName)
        if (resId != 0) {
            specPic.setImageResource(resId)
        }
    }

}
