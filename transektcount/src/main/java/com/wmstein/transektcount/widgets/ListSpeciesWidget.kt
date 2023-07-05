package com.wmstein.transektcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.wmstein.transektcount.R
import com.wmstein.transektcount.TransektCountApplication
import com.wmstein.transektcount.database.Count
import com.wmstein.transektcount.database.Section
import java.util.Objects

/*******************************************************
 * ListSpeciesWidget shows count info area for a species.
 * ListSpeciesActivity shows the result page.
 * Created for TransektCount by wmstein on 15.03.2016
 * last edited in Java on 2023-05-09
 * converted to Kotlin on 2023-06-26
 */
class ListSpeciesWidget(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private val txtSectName: TextView
    private val txtSectRem: TextView
    private val txtSpecName: TextView
    private val txtSpecNameG: TextView
    private val picSpecies: ImageView
    private val specCountf1i: TextView
    private val specCountf2i: TextView
    private val specCountf3i: TextView
    private val specCountpi: TextView
    private val specCountli: TextView
    private val specCountei: TextView
    private val txtSpecRem: TextView
    private val specCountf1e: TextView
    private val specCountf2e: TextView
    private val specCountf3e: TextView
    private val specCountpe: TextView
    private val specCountle: TextView
    private val specCountee: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
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
        setImage(spec) // get picSpecies
        if (section.notes != null) {
            if (section.notes!!.isNotEmpty()) {
                txtSectRem.text = section.notes
                txtSectRem.visibility = VISIBLE
            } else if (spec.notes != null) {
                if (spec.notes!!.isNotEmpty()) {
                    txtSectRem.visibility = INVISIBLE
                } else {
                    txtSectRem.visibility = GONE
                }
            } else {
                txtSectRem.visibility = GONE
            }
        }
        if (spec.notes != null) {
            if (spec.notes!!.isNotEmpty()) {
                txtSpecRem.text = spec.notes
                txtSpecRem.visibility = VISIBLE
            } else {
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

    //Parameters spec_* for use in ListSpeciesActivity
    fun getSpec_sectionid(spec: Count): Int {
        return spec.section_id
    }

    fun getSpec_countf1i(spec: Count): Int {
        return spec.count_f1i
    }

    fun getSpec_countf2i(spec: Count): Int {
        return spec.count_f2i
    }

    fun getSpec_countf3i(spec: Count): Int {
        return spec.count_f3i
    }

    fun getSpec_countpi(spec: Count): Int {
        return spec.count_pi
    }

    fun getSpec_countli(spec: Count): Int {
        return spec.count_li
    }

    fun getSpec_countei(spec: Count): Int {
        return spec.count_ei
    }

    fun getSpec_countf1e(spec: Count): Int {
        return spec.count_f1e
    }

    fun getSpec_countf2e(spec: Count): Int {
        return spec.count_f2e
    }

    fun getSpec_countf3e(spec: Count): Int {
        return spec.count_f3e
    }

    fun getSpec_countpe(spec: Count): Int {
        return spec.count_pe
    }

    fun getSpec_countle(spec: Count): Int {
        return spec.count_le
    }

    fun getSpec_countee(spec: Count): Int {
        return spec.count_ee
    }

    private fun setImage(newcount: Count) // static context
    {
        val rname = "p" + newcount.code // species picture resource name

        // make instance of class TransektCountApplication to reference non-static method 
        val transektCountApp = TransektCountApplication()
        val resId = transektCountApp.getResID(rname)
        if (resId != 0) {
            picSpecies.setImageResource(resId)
        }
    }
}