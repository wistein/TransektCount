package com.wmstein.transektcount.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.wmstein.transektcount.AutoFitText
import com.wmstein.transektcount.R
import com.wmstein.transektcount.database.Count
import com.wmstein.transektcount.database.Section
import java.util.Objects

/*******************************************************
 * ResultsSpeciesWidget shows count info area for a species
 * on the results page.
 * Created for TransektCount by wmstein on 15.03.2016,
 * last edited in Java on 2023-05-09,
 * converted to Kotlin on 2023-08-31,
 * Last edit on 2025-05-11
 */
class ResultsSpeciesWidget(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private val txtSectName: TextView
    private val txtSectRem: TextView
    private val txtSpecName: TextView
    private val txtSpecNameG: TextView
    private val picSpecies: ImageView
    private val specCountf1i: AutoFitText
    private val specCountf2i: AutoFitText
    private val specCountf3i: AutoFitText
    private val specCountpi: AutoFitText
    private val specCountli: AutoFitText
    private val specCountei: AutoFitText
    private val txtSpecRem: TextView
    private val specCountf1e: AutoFitText
    private val specCountf2e: AutoFitText
    private val specCountf3e: AutoFitText
    private val specCountpe: AutoFitText
    private val specCountle: AutoFitText
    private val specCountee: AutoFitText

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
            as LayoutInflater

    init {
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_species, this, true)
        txtSectName = findViewById(R.id.txtSectName)
        txtSectRem = findViewById(R.id.txtSectRem)
        txtSpecName = findViewById(R.id.txtSpecName)
        txtSpecNameG = findViewById(R.id.txtSpecNameG)
        txtSpecRem = findViewById(R.id.txtSpecRem)
        picSpecies = findViewById(R.id.picSpecies)
        specCountf1i = findViewById(R.id.specCountf1i)
        specCountf2i = findViewById(R.id.specCountf2i)
        specCountf3i = findViewById(R.id.specCountf3i)
        specCountpi = findViewById(R.id.specCountpi)
        specCountli = findViewById(R.id.specCountli)
        specCountei = findViewById(R.id.specCountei)
        specCountf1e = findViewById(R.id.specCountf1e)
        specCountf2e = findViewById(R.id.specCountf2e)
        specCountf3e = findViewById(R.id.specCountf3e)
        specCountpe = findViewById(R.id.specCountpe)
        specCountle = findViewById(R.id.specCountle)
        specCountee = findViewById(R.id.specCountee)
    }

    @SuppressLint("SetTextI18n")
    fun setCount(spec: Count, section: Section) {
        txtSectName.text = section.name
        txtSpecName.text = spec.name
        if (spec.name_g != null) {
            if (spec.name_g!!.isNotEmpty()) {
                txtSpecNameG.text = spec.name_g
            } else {
                txtSpecNameG.text = ""
            }
        }
        setPicSpec(spec) // get picSpecies

        if (spec.notes != null) {
            if (spec.notes!!.isNotEmpty()) {
                txtSectRem.text = context.getString(R.string.rem_sp)
                txtSectRem.visibility = VISIBLE
                txtSpecRem.text = spec.notes
                txtSpecRem.visibility = VISIBLE
            } else {
                txtSectRem.visibility = GONE
                txtSpecRem.visibility = GONE
            }
        }
        if (spec.count_f1i > 0) specCountf1i.text = spec.count_f1i.toString()
        if (spec.count_f2i > 0) specCountf2i.text = spec.count_f2i.toString()
        if (spec.count_f3i > 0) specCountf3i.text = spec.count_f3i.toString()
        if (spec.count_pi > 0) specCountpi.text = spec.count_pi.toString()
        if (spec.count_li > 0) specCountli.text = spec.count_li.toString()
        if (spec.count_ei > 0) specCountei.text = spec.count_ei.toString()
        if (spec.count_f1e > 0) specCountf1e.text = spec.count_f1e.toString()
        if (spec.count_f2e > 0) specCountf2e.text = spec.count_f2e.toString()
        if (spec.count_f3e > 0) specCountf3e.text = spec.count_f3e.toString()
        if (spec.count_pe > 0) specCountpe.text = spec.count_pe.toString()
        if (spec.count_le > 0) specCountle.text = spec.count_le.toString()
        if (spec.count_ee > 0) specCountee.text = spec.count_ee.toString()
    }

    //Parameters spec_* for use in ShowResultsActivity
    fun getSpecSectionid(spec: Count): Int {
        return spec.section_id
    }

    fun getSpecCountf1i(spec: Count): Int {
        return spec.count_f1i
    }

    fun getSpecCountf2i(spec: Count): Int {
        return spec.count_f2i
    }

    fun getSpecCountf3i(spec: Count): Int {
        return spec.count_f3i
    }

    fun getSpecCountpi(spec: Count): Int {
        return spec.count_pi
    }

    fun getSpecCountli(spec: Count): Int {
        return spec.count_li
    }

    fun getSpecCountei(spec: Count): Int {
        return spec.count_ei
    }

    fun getSpecCountf1e(spec: Count): Int {
        return spec.count_f1e
    }

    fun getSpecCountf2e(spec: Count): Int {
        return spec.count_f2e
    }

    fun getSpecCountf3e(spec: Count): Int {
        return spec.count_f3e
    }

    fun getSpecCountpe(spec: Count): Int {
        return spec.count_pe
    }

    fun getSpecCountle(spec: Count): Int {
        return spec.count_le
    }

    fun getSpecCountee(spec: Count): Int {
        return spec.count_ee
    }

    @SuppressLint("DiscouragedApi")
    private fun setPicSpec(spec: Count) {
        val rName = "p" + spec.code // species picture resource name
        val resId = resources.getIdentifier(rName, "drawable", context.packageName)
        if (resId != 0) {
            picSpecies.setImageResource(resId)
        }
    }

}