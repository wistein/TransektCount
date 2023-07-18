package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.wmstein.transektcount.database.Count
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.Head
import com.wmstein.transektcount.database.HeadDataSource
import com.wmstein.transektcount.database.Meta
import com.wmstein.transektcount.database.MetaDataSource
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.widgets.ListHeadWidget
import com.wmstein.transektcount.widgets.ListMetaWidget
import com.wmstein.transektcount.widgets.ListSpeciesWidget
import com.wmstein.transektcount.widgets.ListSumWidget

/***********************************************************
 * ListSpeciesActivity shows results list of counted Species
 * Created by wmstein on 2016-03-15,
 * last edited in Java on 2022-04-30,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2023-07-17
 */
class ListSpeciesActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private var transektCount: TransektCountApplication? = null
    private var spec_area: LinearLayout? = null
    var head: Head? = null
    var meta: Meta? = null

    // preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var awakePref = false
    private var sortPref: String? = null

    // the actual data
    private var countDataSource: CountDataSource? = null
    private var sectionDataSource: SectionDataSource? = null
    private var headDataSource: HeadDataSource? = null
    private var metaDataSource: MetaDataSource? = null
    private var lhw: ListHeadWidget? = null
    private var lmw: ListMetaWidget? = null
    private var lsw: ListSumWidget? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_list_species)
        countDataSource = CountDataSource(this)
        sectionDataSource = SectionDataSource(this)
        headDataSource = HeadDataSource(this)
        metaDataSource = MetaDataSource(this)
        transektCount = application as TransektCountApplication
        prefs = TransektCountApplication.getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
        awakePref = prefs.getBoolean("pref_awake", true)
        sortPref = prefs.getString("pref_sort_sp", "none") // sorted species list
        val listSpec_screen = findViewById<ScrollView>(R.id.listSpecScreen)
        listSpec_screen.background = transektCount!!.background
        supportActionBar!!.title = getString(R.string.viewSpecTitle)
        spec_area = findViewById(R.id.listSpecLayout)
        if (awakePref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onResume() {
        super.onResume()
        if (awakePref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // build Show Results screen
        spec_area!!.removeAllViews()
        loadData()
    }

    // fill ListSpeciesWidget with relevant counts and sections data
    private fun loadData() {
        var summf = 0
        var summ = 0
        var sumf = 0
        var sump = 0
        var suml = 0
        var sumo = 0
        var summfe = 0
        var summe = 0
        var sumfe = 0
        var sumpe = 0
        var sumle = 0
        var sumoe = 0
        headDataSource!!.open()
        metaDataSource!!.open()

        //load head and meta data
        head = headDataSource!!.head
        meta = metaDataSource!!.meta

        // display the editable transect No.
        lhw = ListHeadWidget(this, null)
        lhw!!.setWidgetLNo(getString(R.string.transectnumber))
        lhw!!.setWidgetLNo1(head!!.transect_no)
        lhw!!.setWidgetLName(getString(R.string.inspector))
        lhw!!.setWidgetLName1(head!!.inspector_name)
        spec_area!!.addView(lhw)

        // display the editable meta data
        lmw = ListMetaWidget(this, null)
        lmw!!.setWidgetLMeta1(getString(R.string.temperature))
        lmw!!.setWidgetLItem1(meta!!.tempe)
        lmw!!.setWidgetLMeta2(getString(R.string.wind))
        lmw!!.setWidgetLItem2(meta!!.wind)
        lmw!!.setWidgetLMeta3(getString(R.string.clouds))
        lmw!!.setWidgetLItem3(meta!!.clouds)
        lmw!!.setWidgetLDate1(getString(R.string.date))
        lmw!!.setWidgetLDate2(meta!!.date)
        lmw!!.setWidgetLTime1(getString(R.string.starttm))
        lmw!!.setWidgetLItem4(meta!!.start_tm)
        lmw!!.setWidgetLTime2(getString(R.string.endtm))
        lmw!!.setWidgetLItem5(meta!!.end_tm)
        spec_area!!.addView(lmw)

        // display all the sorted counts by adding them to listSpecies layout
        var sect_id: Int
        var section: Section
        countDataSource!!.open()
        sectionDataSource!!.open()
        val specs: List<Count> = when (sortPref) {
            "names_alpha" -> countDataSource!!.allCountsForSrtName
            "codes" -> countDataSource!!.allCountsForSrtCode
            else -> countDataSource!!.allCounts
        } // List of species
        val sumSpec: Int = countDataSource!!.diffSpec // get number of different species
        var spec_countf1i: Int
        var spec_countf2i: Int
        var spec_countf3i: Int
        var spec_countpi: Int
        var spec_countli: Int
        var spec_countei: Int
        var spec_countf1e: Int
        var spec_countf2e: Int
        var spec_countf3e: Int
        var spec_countpe: Int
        var spec_countle: Int
        var spec_countee: Int
        for (spec in specs) {
            val widget = ListSpeciesWidget(this, null)
            sect_id = widget.getSpec_sectionid(spec)
            section = sectionDataSource!!.getSection(sect_id)
            widget.setCount(spec, section)
            spec_countf1i = widget.getSpec_countf1i(spec)
            spec_countf2i = widget.getSpec_countf2i(spec)
            spec_countf3i = widget.getSpec_countf3i(spec)
            spec_countpi = widget.getSpec_countpi(spec)
            spec_countli = widget.getSpec_countli(spec)
            spec_countei = widget.getSpec_countei(spec)
            spec_countf1e = widget.getSpec_countf1e(spec)
            spec_countf2e = widget.getSpec_countf2e(spec)
            spec_countf3e = widget.getSpec_countf3e(spec)
            spec_countpe = widget.getSpec_countpe(spec)
            spec_countle = widget.getSpec_countle(spec)
            spec_countee = widget.getSpec_countee(spec)
            summf += spec_countf1i
            summ += spec_countf2i
            sumf += spec_countf3i
            sump += spec_countpi
            suml += spec_countli
            sumo += spec_countei
            summfe += spec_countf1e
            summe += spec_countf2e
            sumfe += spec_countf3e
            sumpe += spec_countpe
            sumle += spec_countle
            sumoe += spec_countee
        }
        val sumInt: Int = summf + summ + sumf + sump + suml + sumo // sum of internal counts
        val sumExt: Int = summfe + summe + sumfe + sumpe + sumle + sumoe // sum of external counts

        // display the totals
        lsw = ListSumWidget(this, null)
        lsw!!.setSum(
            summf,
            summ,
            sumf,
            sump,
            suml,
            sumo,
            summfe,
            summe,
            sumfe,
            sumpe,
            sumle,
            sumoe,
            sumInt,
            sumExt,
            sumSpec
        )
        spec_area!!.addView(lsw)
        for (spec in specs) {
            val widget = ListSpeciesWidget(this, null)
            sect_id = widget.getSpec_sectionid(spec)
            section = sectionDataSource!!.getSection(sect_id)
            widget.setCount(spec, section)
            spec_area!!.addView(widget)
        }
    }

    override fun onPause() {
        super.onPause()

        // close the data sources
        headDataSource!!.close()
        metaDataSource!!.close()
        countDataSource!!.close()
        sectionDataSource!!.close()
        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onStop() {
        super.onStop()

        // close the data sources
        headDataSource!!.close()
        metaDataSource!!.close()
        countDataSource!!.close()
        sectionDataSource!!.close()
        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /** */
    fun saveAndExit(view: View?) {
        super.finish()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        val listSpec_screen = findViewById<ScrollView>(R.id.listSpecScreen)
        listSpec_screen.background = null
        listSpec_screen.background = transektCount!!.setBackground()
        awakePref = prefs.getBoolean("pref_awake", true)
        sortPref = prefs.getString("pref_sort_sp", "none") // sorted species list
    }

    companion object {
        //private static final String TAG = "transektcountListSpecAct"; // for future use
    }
}