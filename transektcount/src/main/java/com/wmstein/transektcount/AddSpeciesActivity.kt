package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.wmstein.transektcount.database.Count
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.widgets.SpeciesAddWidget

/***********************************************************************************
 * AddSpeciesActivity lets you insert a new species into the sections' species lists
 * AddSpeciesActivity is called from EditSectionActivity
 * Uses SpeciesAddWidget.java, widget_add_spec.xml.
 *
 * The sorting order of the species to add cannot be changed, as it is determined
 * by 3 interdependent and correlated arrays in arrays.xml
 *
 * Created for TransektCount by wmstein on 2019-04-12,
 * last edited in Java on 2023-05-08,
 * converted to Kotlin on 2023-06-28,
 * last edited on 2023-12-08
 */
class AddSpeciesActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private var transektCount: TransektCountApplication? = null

    private var addArea: LinearLayout? = null

    // the actual data
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null
    private var sectionId = 0

    // Id list of missing species
    private lateinit var idArray: Array<String?>

    // complete ArrayLists of species
    private var namesCompleteArrayList: ArrayList<String>? = null
    private var namesGCompleteArrayList: ArrayList<String>? = null
    private var codesCompleteArrayList: ArrayList<String?>? = null
    private var specName: String? = null
    private var specCode: String? = null
    private var specNameG: String? = null
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transektCount = application as TransektCountApplication
        prefs.registerOnSharedPreferenceChangeListener(this)
        brightPref = prefs.getBoolean("pref_bright", true)

        if (MyDebug.LOG) Log.d(TAG,"71, onCreate")

        setContentView(R.layout.activity_add_species)
        val addScreen = findViewById<ScrollView>(R.id.addScreen)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }
        bMap = transektCount!!.decodeBitmap(
            R.drawable.abackground,
            transektCount!!.width,
            transektCount!!.height
        )
        bg = BitmapDrawable(addScreen.resources, bMap)
        addScreen.background = bg

        val extras = intent.extras
        if (extras != null) {
            sectionId = extras.getInt("section_id")
        }

        addArea = findViewById(R.id.addSpecLayout)

        // Load complete species ArrayList from arrays.xml (lists are sorted by code)
        namesCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs)))
        namesGCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs_l)))
        codesCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selCodes)))

    } // end of onCreate()

    override fun onResume() {
        super.onResume()

        prefs = TransektCountApplication.getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
        brightPref = prefs.getBoolean("pref_bright", true)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        // clear any existing views
        addArea!!.removeAllViews()

        // setup the data sources
        sectionDataSource = SectionDataSource(this)
        sectionDataSource!!.open()
        countDataSource = CountDataSource(this)
        countDataSource!!.open()
        supportActionBar!!.setTitle(R.string.addTitle)

        // list only new species not already contained in the species counting list

        // code list of contained species
        val specCodesContainedList = ArrayList<String?>()

        // get species of the section counting list
        val counts: List<Count> = countDataSource!!.getAllSpeciesForSectionSrtCode(sectionId)

        // build code ArrayList of already contained species
        for (count in counts) {
            specCodesContainedList.add(count.code)
        }

        // build lists of missing species
        val specCodesContainedListSize = specCodesContainedList.size
        var posSpec: Int

        // for already contained species reduce complete arraylists
        for (i in 0 until specCodesContainedListSize) {
            if (codesCompleteArrayList!!.contains(specCodesContainedList[i])) {
                // Remove species with code x from missing species lists.
                // Prerequisites: exactly correlated arrays of selCodes, selSpecs and selSpecs_l
                //   for all localisations of arrays.xml
                specCode = specCodesContainedList[i]
                posSpec = codesCompleteArrayList!!.indexOf(specCode)
                namesCompleteArrayList!!.removeAt(posSpec)
                namesGCompleteArrayList!!.removeAt(posSpec)
                codesCompleteArrayList!!.remove(specCode)
            }
        }
        idArray = setIdsSelSpecs(codesCompleteArrayList) // create idArray from codeArray

        // load the species data into the widgets
        var i = 0
        while (i < codesCompleteArrayList!!.size) {
            val saw = SpeciesAddWidget(this, null)
            saw.setSpecName(namesCompleteArrayList!![i])
            saw.setSpecNameG(namesGCompleteArrayList!![i])
            saw.setSpecCode(codesCompleteArrayList!![i])
            saw.setPSpec(codesCompleteArrayList!![i]!!)
            saw.setSpecId(idArray[i]!!)
            addArea!!.addView(saw)
            i++
        }
    } // end of onResume()

    // create idArray from codeArray
    private fun setIdsSelSpecs(speccodesm: ArrayList<String?>?): Array<String?> {
        idArray = arrayOfNulls(speccodesm!!.size)
        var i = 0
        while (i < speccodesm.size) {
            idArray[i] = (i + 1).toString()
            i++
        }
        return idArray
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("section_id", sectionId)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        if (savedInstanceState.getInt("section_id") != 0)
            sectionId = savedInstanceState.getInt("section_id")
    }

    override fun onPause() {
        super.onPause()

        // close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()
    }

    // called from widget_add_spec.xml
    fun saveAndExit(view: View) {
        if (saveData(view)) {
            super.finish()
        }
    }

    private fun saveData(view: View): Boolean {
        // save added species to species list
        var retValue = true
        val idToAdd = view.tag as Int // id for new species to add in current section

        val saw1 = addArea!!.getChildAt(idToAdd) as SpeciesAddWidget
        specName = saw1.getSpecName()
        specCode = saw1.getSpecCode()
        specNameG = saw1.getSpecNameG()

        try {
            val numSect: Int = sectionDataSource!!.numEntries
            var i = 1
            while (i <= numSect)
            {
                countDataSource!!.createCount(i, specName, specCode, specNameG)
                i++
            }
        } catch (e: Exception) {
            retValue = false
        }

        // store code of new species in sharedPreferences for Spinner in CountingActivity(A)
        val editor = prefs.edit()
        editor.putString("new_spec_code", specCode)
        editor.commit()

        sectionDataSource!!.close()
        countDataSource!!.close()
        return retValue
    }

    /*
     * Save the selected species to the species list
     */
    fun addCount(view: View) {
        if (saveData(view)) {
            super.finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.add_species, menu)
        return true
    }

    @SuppressLint("ApplySharedPref")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == android.R.id.home) {
            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NavUtils.navigateUpTo(this, intent)
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java", ReplaceWith("finish()"))
    @SuppressLint("ApplySharedPref", "MissingSuperCall")
    override fun onBackPressed() {
        finish()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        val addScreen = findViewById<ScrollView>(R.id.addScreen)
        if (prefs != null) {
            prefs.registerOnSharedPreferenceChangeListener(this)
            brightPref = prefs.getBoolean("pref_bright", true)
        }
        bMap = transektCount!!.decodeBitmap(
            R.drawable.abackground,
            transektCount!!.width,
            transektCount!!.height
        )
        addScreen.background = null
        bg = BitmapDrawable(addScreen.resources, bMap)
        addScreen.background = bg
    }

    companion object {
        private const val TAG = "TransektCountAddSpecAct"
    }

}