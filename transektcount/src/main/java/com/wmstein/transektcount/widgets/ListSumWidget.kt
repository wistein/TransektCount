package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.transektcount.R
import java.util.Objects

/**********************************************************
 * ListSumWidget shows count totals area in the result page
 * created by ListSpeciesActivity
 * Created for TransektCount by wmstein on 15.03.2016
 * last edited in Java on 2021-01-26
 * converted to Kotlin on 2023-06-26
 */
class ListSumWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val sumCountf1i: TextView
    private val sumCountf2i: TextView
    private val sumCountf3i: TextView
    private val sumCountpi: TextView
    private val sumCountli: TextView
    private val sumCountei: TextView
    private val sumCountf1e: TextView
    private val sumCountf2e: TextView
    private val sumCountf3e: TextView
    private val sumCountpe: TextView
    private val sumCountle: TextView
    private val sumCountee: TextView
    private val sumIndInt: TextView
    private val sumIndExt: TextView
    private val sumDiffInd: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_sum_species, this, true)
        sumCountf1i = findViewById(R.id.sumCountf1i)
        sumCountf2i = findViewById(R.id.sumCountf2i)
        sumCountf3i = findViewById(R.id.sumCountf3i)
        sumCountpi = findViewById(R.id.sumCountpi)
        sumCountli = findViewById(R.id.sumCountli)
        sumCountei = findViewById(R.id.sumCountei)
        sumCountf1e = findViewById(R.id.sumCountf1e)
        sumCountf2e = findViewById(R.id.sumCountf2e)
        sumCountf3e = findViewById(R.id.sumCountf3e)
        sumCountpe = findViewById(R.id.sumCountpe)
        sumCountle = findViewById(R.id.sumCountle)
        sumCountee = findViewById(R.id.sumCountee)
        sumIndInt = findViewById(R.id.sumIndInt)
        sumIndExt = findViewById(R.id.sumIndExt)
        sumDiffInd = findViewById(R.id.sumDiffInd)
    }

    fun setSum(
        summf: Int, summ: Int, sumf: Int, sump: Int, suml: Int, sumo: Int,
        summfe: Int, summe: Int, sumfe: Int, sumpe: Int, sumle: Int, sumoe: Int,
        sumInt: Int, sumExt: Int, sumDiff: Int
    ) {
        sumCountf1i.text = summf.toString()
        sumCountf2i.text = summ.toString()
        sumCountf3i.text = sumf.toString()
        sumCountpi.text = sump.toString()
        sumCountli.text = suml.toString()
        sumCountei.text = sumo.toString()
        sumCountf1e.text = summfe.toString()
        sumCountf2e.text = summe.toString()
        sumCountf3e.text = sumfe.toString()
        sumCountpe.text = sumpe.toString()
        sumCountle.text = sumle.toString()
        sumCountee.text = sumoe.toString()
        sumIndInt.text = sumInt.toString()
        sumIndExt.text = sumExt.toString()
        sumDiffInd.text = sumDiff.toString()
    }
}