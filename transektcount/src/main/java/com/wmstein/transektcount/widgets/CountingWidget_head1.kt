/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
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

/****************************************************
 * Interface for widget_counting_head1.xml
 * Created by wmstein 18.12.2016
 * Last edited in Java on 2023-05-09
 * converted to Kotlin on 2023-06-26
 */
class CountingWidget_head1(
    context: Context,
    resource: Int,
    private val idArray: Array<String>,
    private val contentArray1: Array<String>,
    private val contentArray3: Array<String>,
    private val contentArray2: Array<String>,
    private val imageArray: Array<Int>
) : ArrayAdapter<String?>(context, resource, R.id.countName, contentArray1) {
    private val inflater: LayoutInflater

    init {
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, parent)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, parent)
    }

    private fun getCustomView(position: Int, parent: ViewGroup): View {
        val head1 = inflater.inflate(R.layout.widget_counting_head1, parent, false)
        val countId = head1.findViewById<TextView>(R.id.countId) // used for spinner interface  
        countId.text = idArray[position]
        val countName = head1.findViewById<TextView>(R.id.countName)
        countName.text = contentArray1[position]
        val countNameg = head1.findViewById<TextView>(R.id.countNameg)
        countNameg.text = contentArray3[position]
        val countCode = head1.findViewById<TextView>(R.id.countCode)
        countCode.text = contentArray2[position]
        val pSpecies = head1.findViewById<ImageView>(R.id.pSpecies)
        pSpecies.setImageResource(imageArray[position])
        return head1
    }
}