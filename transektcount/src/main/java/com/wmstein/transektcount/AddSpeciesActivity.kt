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
import com.wmstein.transektcount.widgets.SpeciesAddWidget
import java.util.Objects

/********************************************************************************
 * AddSpeciesActivity lets you insert a new species into a section's species list
 * AddSpeciesActivity is called from EditSectionActivity
 * Uses SpeciesAddWidget.java, widget_add_spec.xml.
 *
 * The sorting order of the species to add cannot be changed, as it is determined
 * by 3 interdependent and correlated arrays in arrays.xml
 *
 * Created for TourCount by wmstein on 2019-04-12,
 * last edited in Java on 2023-05-08
 * converted to Kotlin on 2023-06-28
 */
class AddSpeciesActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private var add_area: LinearLayout? = null

    // the actual data
    private var countDataSource: CountDataSource? = null
    private var section_id = 0

    // Id list of missing species
    private lateinit var idArray: Array<String?>

    // complete ArrayLists of species
    private var namesCompleteArrayList: ArrayList<String>? = null
    private var namesGCompleteArrayList: ArrayList<String>? = null
    private var codesCompleteArrayList: ArrayList<String?>? = null
    private var specCode: String? = null
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null

    // Preferences
    private var brightPref = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transektCount = application as TransektCountApplication
        val prefs = TransektCountApplication.getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
        brightPref = prefs.getBoolean("pref_bright", true)
        setContentView(R.layout.activity_add_species)
        val add_screen = findViewById<ScrollView>(R.id.addScreen)

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
        bg = BitmapDrawable(add_screen.resources, bMap)
        add_screen.background = bg
        val extras = intent.extras
        if (extras != null) {
            section_id = extras.getInt("section_id")
        }
        if (MyDebug.LOG) Log.e(TAG, "onCreate getIntent Section Id = $section_id")
        add_area = findViewById(R.id.addSpecLayout)

        // Load complete species ArrayList from arrays.xml (lists are sorted by code)
        namesCompleteArrayList =
            ArrayList(listOf(*resources.getStringArray(R.array.selSpecs)))
        namesGCompleteArrayList =
            ArrayList(listOf(*resources.getStringArray(R.array.selSpecs_g)))
        codesCompleteArrayList =
            ArrayList(listOf(*resources.getStringArray(R.array.selCodes)))
    }

    override fun onResume() {
        super.onResume()
        val prefs = TransektCountApplication.getPrefs()
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
        add_area!!.removeAllViews()

        // setup the data sources
        countDataSource = CountDataSource(this)
        countDataSource!!.open()
        Objects.requireNonNull(supportActionBar)!!.setTitle(R.string.addTitle)

        // list of only new species not already contained in the species counting list
        val counts: List<Count>

        // code list of contained species
        val specCodesContainedList = ArrayList<String?>()

        // get species of the section counting list
        counts = countDataSource!!.getAllSpeciesForSectionSrtCode(section_id)

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
                // Prerequisites: exactly correlated arrays of selCodes, selSpecs and selSpecs_g
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
        var i: Int
        i = 0
        while (i < codesCompleteArrayList!!.size) {
            val saw = SpeciesAddWidget(this, null)
            saw.setSpecName(namesCompleteArrayList!![i])
            saw.setSpecNameG(namesGCompleteArrayList!![i])
            saw.setSpecCode(codesCompleteArrayList!![i])
            saw.setPSpec(codesCompleteArrayList!![i]!!)
            saw.setSpecId(idArray[i]!!)
            add_area!!.addView(saw)
            i++
        }
    } // end of Resume

    // create idArray from codeArray
    private fun setIdsSelSpecs(speccodesm: ArrayList<String?>?): Array<String?> {
        var i: Int
        idArray = arrayOfNulls(speccodesm!!.size)
        i = 0
        while (i < speccodesm.size) {
            idArray[i] = (i + 1).toString()
            i++
        }
        return idArray
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("section_id", section_id)
        super.onSaveInstanceState(outState)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        if (savedInstanceState.getInt("section_id") != 0) section_id =
            savedInstanceState.getInt("section_id")
        if (MyDebug.LOG) Log.e(TAG, "savedInstanceState Section Id = $section_id")
    }

    override fun onPause() {
        super.onPause()

        // close the data sources
        countDataSource!!.close()
    }

    fun saveAndExit(view: View) {
        if (saveData(view)) {
            super.finish()
        }
    }

    private fun saveData(view: View): Boolean {
        // save added species to species list
        var retValue = true
        val idToAdd = view.tag as Int
        val saw1 = add_area!!.getChildAt(idToAdd) as SpeciesAddWidget
        val specName = saw1.getSpecName()
        specCode = saw1.getSpecCode()
        val specNameG = saw1.getSpecNameG()
        try {
            countDataSource!!.createCount(section_id, specName, specCode, specNameG)
        } catch (e: Exception) {
            retValue = false
        }
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
    @SuppressLint("ApplySharedPref")
    override fun onBackPressed() {
        //Intent intent = NavUtils.getParentActivityIntent(this);
        finish()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        val add_screen = findViewById<ScrollView>(R.id.addScreen)
        bMap = transektCount!!.decodeBitmap(
            R.drawable.abackground,
            transektCount!!.width,
            transektCount!!.height
        )
        add_screen.background = null
        bg = BitmapDrawable(add_screen.resources, bMap)
        add_screen.background = bg
    }

    companion object {
        private const val TAG = "TransektCountAddSpecAct"

        @SuppressLint("StaticFieldLeak")
        private var transektCount: TransektCountApplication? = null
    }
}