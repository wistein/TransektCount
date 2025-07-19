/*
 * Copyright © 2016-2025. Wilhelm Stein, Bonn, Germany.
 */
package com.wmstein.transektcount.widgets

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.wmstein.transektcount.R

/*************************************************************
 * Interface for widget_counting_head1.xml
 * fills the species row with names, code, spinner and picture.
 * Created by wmstein 18.12.2016,
 * last edited in Java on 2023-05-09,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2025-07-16
 */
class CountingWidgetHead1(
    context: Context,
    private val idArray: Array<String>,    // species ids
    private val nameArray: Array<String>,  // species names
    private val nameArrayL: Array<String>, // species local names
    private val codeArray: Array<String>,  // species codes
    private val imageArray: Array<Int>     // species images
) : ArrayAdapter<String?>(context, R.layout.widget_counting_head1, R.id.countName, nameArray) {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    // Shows Spinner list
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, parent)
    }

    // Shows rest of counting page
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, parent)
    }

    private fun getCustomView(position: Int, parent: ViewGroup): View {
        val head1 = inflater.inflate(R.layout.widget_counting_head1, parent, false)
        val countId = head1.findViewById<TextView>(R.id.countId) // used for spinner interface  
        countId.text = idArray[position]
        val countName = head1.findViewById<TextView>(R.id.countName)
        countName.text = nameArray[position]
        val countNameg = head1.findViewById<TextView>(R.id.countNameg)
        countNameg.text = nameArrayL[position]
        val countCode = head1.findViewById<TextView>(R.id.countCode)
        countCode.text = codeArray[position]
        val pSpecies = head1.findViewById<ImageView>(R.id.pSpecies)
        pSpecies.setImageResource(imageArray[position])
        return head1
    }

}
