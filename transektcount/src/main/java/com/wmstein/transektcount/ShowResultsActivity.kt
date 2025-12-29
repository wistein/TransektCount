package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.wmstein.transektcount.database.Count
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.Head
import com.wmstein.transektcount.database.HeadDataSource
import com.wmstein.transektcount.database.Meta
import com.wmstein.transektcount.database.MetaDataSource
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.widgets.ResultsHeadWidget
import com.wmstein.transektcount.widgets.ResultsMetaWidget
import com.wmstein.transektcount.widgets.ResultsSpeciesWidget
import com.wmstein.transektcount.widgets.ResultsSumWidget

/******************************************************************
 * ShowResultsActivity.kt shows results list of counted species,
 * uses ResultsSpeciesWidget, ResultsHeadWidget, ResultsMetaWidget,
 * ResultsSumWidget
 * Created by wmstein on 2016-03-15,
 * last edited in Java on 2022-04-30,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2025-12-29
 */
class ShowResultsActivity : AppCompatActivity() {
    private var specArea: LinearLayout? = null

    // Data
    private var countDataSource: CountDataSource? = null
    private var sectionDataSource: SectionDataSource? = null
    private var headDataSource: HeadDataSource? = null
    private var metaDataSource: MetaDataSource? = null
    private var head: Head? = null
    private var meta: Meta? = null
    private var lhw: ResultsHeadWidget? = null
    private var lmw: ResultsMetaWidget? = null
    private var lsw: ResultsSumWidget? = null

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false
    private var awakePref = false
    private var outPref: String? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "63, onCreate")

        awakePref = prefs.getBoolean("pref_awake", true)
        outPref = prefs.getString("pref_csv_out", "species") // sort mode output
        brightPref = prefs.getBoolean("pref_bright", true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        setContentView(R.layout.activity_list_species)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.listSpecScreen)) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. This solution sets
            // only the bottom, left, and right dimensions, but you can apply whichever
            // insets are appropriate to your layout. You can also update the view padding
            // if that's more appropriate.
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }

            // Return CONSUMED if you don't want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        // Set full brightness of screen
        if (brightPref) {
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        if (awakePref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        headDataSource = HeadDataSource(this)
        sectionDataSource = SectionDataSource(this)
        metaDataSource = MetaDataSource(this)
        countDataSource = CountDataSource(this)

        supportActionBar!!.setTitle(R.string.viewSpecTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        specArea = findViewById(R.id.listSpecLayout)

        // new onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.d(TAG, "118, handleOnBackPressed")
                finish()
                remove()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "131, onResume")

        headDataSource!!.open()
        metaDataSource!!.open()
        countDataSource!!.open()
        sectionDataSource!!.open()

        // build Show Results screen
        specArea!!.removeAllViews()
        loadData()
    }
    // End of onResume()

    // fill ResultsSpeciesWidget with relevant counts and sections data
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
        lhw = ResultsHeadWidget(this, null)
        lhw!!.setWidgetLNo(getString(R.string.transectnumber))
        lhw!!.setWidgetLNo1(head!!.transect_no)
        lhw!!.setWidgetLName(getString(R.string.inspector))
        lhw!!.setWidgetLName1(head!!.inspector_name)
        specArea!!.addView(lhw)

        // display the editable meta data
        lmw = ResultsMetaWidget(this, null)
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
        specArea!!.addView(lmw)

        // display all the sorted counts by adding them to listSpecies layout
        var sectId: Int
        var section: Section

        val specs: List<Count> = if (outPref.equals("sections")) {
            // sort criteria are section and name
            countDataSource!!.allCountsForSrtSectionName
        } else {
            // sort criteria are name and section
            countDataSource!!.allCountsForSrtNameSection
        } // List of sorted species

        val sumSpec: Int = countDataSource!!.diffSpec // get number of different species
        var specCntf1i: Int
        var specCntf2i: Int
        var specCntf3i: Int
        var specCntpi: Int
        var specCntli: Int
        var specCntei: Int
        var specCntf1e: Int
        var specCntf2e: Int
        var specCntf3e: Int
        var specCntpe: Int
        var specCntle: Int
        var specCntee: Int

        for (spec in specs) {
            val widget = ResultsSpeciesWidget(this, null)
            sectId = widget.getSpecSectionid(spec)
            section = sectionDataSource!!.getSection(sectId)
            widget.setCount(spec, section)
            specCntf1i = widget.getSpecCountf1i(spec)
            specCntf2i = widget.getSpecCountf2i(spec)
            specCntf3i = widget.getSpecCountf3i(spec)
            specCntpi = widget.getSpecCountpi(spec)
            specCntli = widget.getSpecCountli(spec)
            specCntei = widget.getSpecCountei(spec)
            specCntf1e = widget.getSpecCountf1e(spec)
            specCntf2e = widget.getSpecCountf2e(spec)
            specCntf3e = widget.getSpecCountf3e(spec)
            specCntpe = widget.getSpecCountpe(spec)
            specCntle = widget.getSpecCountle(spec)
            specCntee = widget.getSpecCountee(spec)

            summf += specCntf1i
            summ += specCntf2i
            sumf += specCntf3i
            sump += specCntpi
            suml += specCntli
            sumo += specCntei
            summfe += specCntf1e
            summe += specCntf2e
            sumfe += specCntf3e
            sumpe += specCntpe
            sumle += specCntle
            sumoe += specCntee
        }

        val sumInt: Int = summf + summ + sumf + sump + suml + sumo // sum of internal counts
        val sumExt: Int = summfe + summe + sumfe + sumpe + sumle + sumoe // sum of external counts

        // display the totals
        lsw = ResultsSumWidget(this, null)
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
        specArea!!.addView(lsw)

        // display all counted soecies per section
        for (spec in specs) {
            val widget = ResultsSpeciesWidget(this, null)
            sectId = widget.getSpecSectionid(spec)
            section = sectionDataSource!!.getSection(sectId)
            widget.setCount(spec, section)
            specArea!!.addView(widget)
        }
    }
    // End of loadData()

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "289, onPause")

        // close the data sources
        headDataSource!!.close()
        metaDataSource!!.close()
        countDataSource!!.close()
        sectionDataSource!!.close()

        specArea!!.clearFocus()
        specArea!!.removeAllViews()
    }

    override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "305, onStop")

        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        specArea = null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "318, onDestroy")
    }

    companion object {
        private const val TAG = "ShowResultsAct"
    }

}
