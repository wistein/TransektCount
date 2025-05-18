package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.transektcount.AutoFitText
import com.wmstein.transektcount.R
import java.util.Objects

/**********************************************************
 * ResultsSumWidget shows count totals area in the result page
 * created by ResultsActivity
 * Created for TransektCount by wmstein on 15.03.2016,
 * last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-06-26,
 * Last edit on 2025-05-11.
 */
class ResultsSumWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val sumCountf1i: AutoFitText
    private val sumCountf2i: AutoFitText
    private val sumCountf3i: AutoFitText
    private val sumCountpi: AutoFitText
    private val sumCountli: AutoFitText
    private val sumCountei: AutoFitText
    private val sumCountf1e: AutoFitText
    private val sumCountf2e: AutoFitText
    private val sumCountf3e: AutoFitText
    private val sumCountpe: AutoFitText
    private val sumCountle: AutoFitText
    private val sumCountee: AutoFitText
    private val sumIndInt: AutoFitText
    private val sumIndExt: AutoFitText
    private val sumDiffSpec: TextView

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
        sumDiffSpec = findViewById(R.id.sumDiffSpec)
    }

    fun setSum(
        summf: Int, summ: Int, sumf: Int, sump: Int, suml: Int, sumo: Int,
        summfe: Int, summe: Int, sumfe: Int, sumpe: Int, sumle: Int, sumoe: Int,
        sumInt: Int, sumExt: Int, sumDiff: Int
    ) {
        if (summf > 0) sumCountf1i.text = summf.toString()
        if (summ > 0) sumCountf2i.text = summ.toString()
        if (sumf > 0) sumCountf3i.text = sumf.toString()
        if (sump > 0) sumCountpi.text = sump.toString()
        if (suml > 0) sumCountli.text = suml.toString()
        if (sumo > 0) sumCountei.text = sumo.toString()
        if (summfe > 0) sumCountf1e.text = summfe.toString()
        if (summe > 0) sumCountf2e.text = summe.toString()
        if (sumfe > 0) sumCountf3e.text = sumfe.toString()
        if (sumpe > 0) sumCountpe.text = sumpe.toString()
        if (sumle > 0) sumCountle.text = sumle.toString()
        if (sumoe > 0) sumCountee.text = sumoe.toString()
        if (sumInt > 0) sumIndInt.text = sumInt.toString()
        if (sumExt > 0) sumIndExt.text = sumExt.toString()
        if (sumDiff > 0) sumDiffSpec.text = sumDiff.toString()
    }

}