package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils

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

/************************************************************
 * ListSpeciesActivity shows results list of counted Species,
 * uses ListSpeciesWidget, ListHeadWidget, ListMetaWidget,
 * ListSumWidget
 * Created by wmstein on 2016-03-15,
 * last edited in Java on 2022-04-30,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2024-11-26
 */
class ListSpeciesActivity : AppCompatActivity() {
    private var transektCount: TransektCountApplication? = null

    private var spec_area: LinearLayout? = null

    // Data
    private var countDataSource: CountDataSource? = null
    private var sectionDataSource: SectionDataSource? = null
    private var headDataSource: HeadDataSource? = null
    private var metaDataSource: MetaDataSource? = null
    private var head: Head? = null
    private var meta: Meta? = null
    private var lhw: ListHeadWidget? = null
    private var lmw: ListMetaWidget? = null
    private var lsw: ListSumWidget? = null

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var awakePref = false
    private var outPref: String? = null
    private var brightPref = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.dLOG) Log.i(TAG, "62, onCreate")

        transektCount = application as TransektCountApplication
        awakePref = prefs.getBoolean("pref_awake", true)
        outPref = prefs.getString("pref_csv_out", "species") // sort mode output
        brightPref = prefs.getBoolean("pref_bright", true)

        setContentView(R.layout.activity_list_species)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        headDataSource = HeadDataSource(this)
        sectionDataSource = SectionDataSource(this)
        metaDataSource = MetaDataSource(this)
        countDataSource = CountDataSource(this)

        val resultsScreen = findViewById<ScrollView>(R.id.listSpecScreen)
        resultsScreen.background = transektCount!!.setBackgr()

        supportActionBar!!.title = getString(R.string.viewSpecTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        spec_area = findViewById(R.id.listSpecLayout)

        if (awakePref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // new onBackPressed logic
        onBackPressedDispatcher.addCallback(object :
            OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (MyDebug.dLOG) Log.i(TAG, "100, handleOnBackPressed")

                NavUtils.navigateUpFromSameTask(this@ListSpeciesActivity)
            }
        })
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (MyDebug.dLOG) Log.i(TAG, "111, onResume")

        if (awakePref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        headDataSource!!.open()
        metaDataSource!!.open()
        countDataSource!!.open()
        sectionDataSource!!.open()

        // build Show Results screen
        spec_area!!.removeAllViews()
        loadData()
    }
    // End of onResume()

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
        lmw!!.setWidgetLTemp(getString(R.string.temperature))
        lmw!!.setWidgetLTemps(meta!!.temps)
        lmw!!.setWidgetLTempe(meta!!.tempe)
        lmw!!.setWidgetLWind(getString(R.string.wind))
        lmw!!.setWidgetLWinds(meta!!.winds)
        lmw!!.setWidgetLWinde(meta!!.winde)
        lmw!!.setWidgetLCloud(getString(R.string.clouds))
        lmw!!.setWidgetLClouds(meta!!.clouds)
        lmw!!.setWidgetLCloude(meta!!.cloude)
        lmw!!.setWidgetLDate1(getString(R.string.date))
        lmw!!.setWidgetLDate2(meta!!.date)
        lmw!!.setWidgetLTime1(getString(R.string.starttm))
        lmw!!.setWidgetLItem4(meta!!.start_tm)
        lmw!!.setWidgetLTime2(getString(R.string.endtm))
        lmw!!.setWidgetLItem5(meta!!.end_tm)
        lmw!!.setWidgetLNote1(getString(R.string.note))
        lmw!!.setWidgetLNote2(meta!!.note)
        spec_area!!.addView(lmw)

        // display all the sorted counts by adding them to listSpecies layout
        var sect_id: Int
        var section: Section
        val specs: List<Count> // List of sorted species

        if (outPref.equals("sections")) {
            // sort criteria are section and name
            specs = countDataSource!!.allCountsForSrtSectionName
        } else {
            // sort criteria are name and section
            specs = countDataSource!!.allCountsForSrtNameSection
        }

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
            sect_id = widget.getSpecSectionid(spec)
            section = sectionDataSource!!.getSection(sect_id)
            widget.setCount(spec, section)
            spec_countf1i = widget.getSpecCountf1i(spec)
            spec_countf2i = widget.getSpecCountf2i(spec)
            spec_countf3i = widget.getSpecCountf3i(spec)
            spec_countpi = widget.getSpecCountpi(spec)
            spec_countli = widget.getSpecCountli(spec)
            spec_countei = widget.getSpecCountei(spec)
            spec_countf1e = widget.getSpecCountf1e(spec)
            spec_countf2e = widget.getSpecCountf2e(spec)
            spec_countf3e = widget.getSpecCountf3e(spec)
            spec_countpe = widget.getSpecCountpe(spec)
            spec_countle = widget.getSpecCountle(spec)
            spec_countee = widget.getSpecCountee(spec)

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

        // display all counted soecies per section
        for (spec in specs) {
            val widget = ListSpeciesWidget(this, null)
            sect_id = widget.getSpecSectionid(spec)
            section = sectionDataSource!!.getSection(sect_id)
            widget.setCount(spec, section)
            spec_area!!.addView(widget)
        }
    }
    // End of loadData()

    override fun onPause() {
        super.onPause()

        if (MyDebug.dLOG) Log.i(TAG, "273, onPause")

        // close the data sources
        headDataSource!!.close()
        metaDataSource!!.close()
        countDataSource!!.close()
        sectionDataSource!!.close()

        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    companion object {
        private const val TAG = "ListSpecAct"
    }

}
