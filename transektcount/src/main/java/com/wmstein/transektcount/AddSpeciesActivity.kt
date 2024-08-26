package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.wmstein.transektcount.database.Count
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.widgets.AddSpeciesWidget
import com.wmstein.transektcount.widgets.HintWidget

/*******************************************************************************
 * AddSpeciesActivity lets you insert new species into the counting species list
 * AddSpeciesActivity is called from CountingActivity
 * Uses AddSpeciesWidget.kt, widget_add_spec.xml.
 *
 * The sorting order of the species to add cannot be changed, as it is determined
 * by 3 interdependent and correlated arrays in arrays.xml
 *
 * Created for TransektCount by wmstein on 2019-04-12,
 * last edited in Java on 2023-05-08,
 * converted to Kotlin on 2023-06-28,
 * last edited on 2024-08-23
 */
class AddSpeciesActivity : AppCompatActivity() {
    private var transektCount: TransektCountApplication? = null

    private var addArea: LinearLayout? = null
    private var hintArea: LinearLayout? = null

    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null
    private var sectionId = 0

    // ID list of not yet included species in counting list
    private lateinit var idsRemainingArrayList: Array<String?>

    // complete ArrayLists of species
    private var namesCompleteArrayList: ArrayList<String>? = null
    private var namesGCompleteArrayList: ArrayList<String>? = null
    private var codesCompleteArrayList: ArrayList<String?>? = null

    private var specName: String? = null
    private var specCode: String? = null
    private var specNameG: String? = null

    // list of species to add
    private var listToAdd: ArrayList<AddSpeciesWidget>? = null

    // screen background
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transektCount = application as TransektCountApplication

        // Load preferences
        brightPref = prefs.getBoolean("pref_bright", true)

        if (MyDebug.LOG) Log.d(TAG, "79, onCreate")

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
            R.drawable.addbackground,
            transektCount!!.width,
            transektCount!!.height
        )
        bg = BitmapDrawable(addScreen.resources, bMap)
        addScreen.background = bg

        val extras = intent.extras
        if (extras != null) {
            sectionId = extras.getInt("section_id")
        }

        listToAdd = ArrayList()

        hintArea = findViewById(R.id.showHintAddLayout)
        addArea = findViewById(R.id.addSpecLayout)

        // Load complete species ArrayList from arrays.xml (lists are sorted by code)
        namesCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs)))
        namesGCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs_l)))
        codesCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selCodes)))

        // setup the data sources
        countDataSource = CountDataSource(this)
        sectionDataSource = SectionDataSource(this)

        // New onBackPressed logic
        if (Build.VERSION.SDK_INT >= 33) {
            onBackPressedDispatcher.addCallback(object :
                OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    saveData()
                    countDataSource!!.close()
                    sectionDataSource!!.close()

                    val intent = NavUtils.getParentActivityIntent(this@AddSpeciesActivity)!!
                    intent.putExtra("section_id", sectionId)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    NavUtils.navigateUpTo(this@AddSpeciesActivity, intent)
                }
            })
        }
    }
    // end of onCreate()

    override fun onResume() {
        super.onResume()

        sectionDataSource!!.open()
        countDataSource!!.open()

        // clear any existing views
        addArea!!.removeAllViews()
        hintArea!!.removeAllViews()

        supportActionBar!!.setTitle(R.string.addTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Display hint: Further available species
        val nw = HintWidget(this, null)
        nw.setHint1(getString(R.string.specsToAdd))
        hintArea!!.addView(nw)

        // list only new species not already contained in the species counting list
        // 1. code list of contained species
        val specCodesContainedList = ArrayList<String?>()

        // get species of the section 1 counting list
        val counts: List<Count> = countDataSource!!.getAllSpeciesForSectionSrtCode(1)

        // build code ArrayList of already contained species
        for (count in counts) {
            specCodesContainedList.add(count.code)
        }

        // 2. build lists of missing species
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

        // create idsRemainingArrayList for all remaining species of codesCompleteArrayList
        idsRemainingArrayList = arrayOfNulls(codesCompleteArrayList!!.size)
        var i = 0
        while (i < codesCompleteArrayList!!.size) {
            idsRemainingArrayList[i] = (i + 1).toString()
            i++
        }

        // load the species data into the widgets
        i = 0
        while (i < codesCompleteArrayList!!.size) {
            val asw = AddSpeciesWidget(this, null)
            asw.setSpecName(namesCompleteArrayList!![i])
            asw.setSpecNameG(namesGCompleteArrayList!![i])
            asw.setSpecCode(codesCompleteArrayList!![i])
            asw.setPSpec(codesCompleteArrayList!![i]!!)
            asw.setSpecId(idsRemainingArrayList[i]!!)
            addArea!!.addView(asw)
            i++
        }
    }
    // end of onResume()

    // mark the selected species and consider it for the species counts list
    fun checkBoxAdd(view: View) {
        val idToAdd = view.tag as Int
        val asw = addArea!!.getChildAt(idToAdd) as AddSpeciesWidget

        val checked = asw.getMarkSpec() // return boolean isChecked

        // put species on add list
        if (checked) {
            listToAdd!!.add(asw)
            if (MyDebug.LOG) {
                val codeA = asw.getSpecCode()
                Log.d(TAG, "221, addCount, code: $codeA")
            }
        } else {
            // remove species previously added from add list
            listToAdd!!.remove(asw)
            if (MyDebug.LOG) {
                val codeA = asw.getSpecCode()
                Log.d(TAG, "228, removeCount, code: $codeA")
            }
        }
    }

    // save added species to species lists of all sections
    private fun saveData() {
        var i = 0 // index of species list to add
        val numSect: Int = sectionDataSource!!.numEntries
        while (i < listToAdd!!.size) {
            specName = listToAdd!![i].getSpecName()
            specCode = listToAdd!![i].getSpecCode()
            specNameG = listToAdd!![i].getSpecNameG()
            if (MyDebug.LOG) {
                Log.d(TAG, "242, saveData, code: $specCode")
            }
            try {
                var sectid = 1
                while (sectid <= numSect) {
                    countDataSource!!.createCount(sectid, specName, specCode, specNameG)
                    sectid++
                }
            } catch (e: Exception) {
                // nothing
            }
            i++
        }

        // sort counts table for section, code and contiguous index
        countDataSource!!.sortCounts()

        // store code of last selected species in sharedPreferences
        //  for Spinner in CountingActivity
        if (i > 0) {
            val editor = prefs.edit()
            editor.putString("new_spec_code", specCode)
            editor.commit()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("section_id", sectionId)
        outState.putString("new_spec_code", specCode)
        super.onSaveInstanceState(outState)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        if (savedInstanceState.getInt("section_id") != 0)
            sectionId = savedInstanceState.getInt("section_id")
        if (savedInstanceState.getString("new_spec_code")!!.isNotBlank())
            specCode = savedInstanceState.getString("new_spec_code")
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.add_species, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId
        if (id == android.R.id.home) {
            saveData()
            countDataSource!!.close()
            sectionDataSource!!.close()

            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.putExtra("section_id", sectionId)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NavUtils.navigateUpTo(this, intent)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        // close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("ApplySharedPref", "MissingSuperCall")
    override fun onBackPressed() {
        saveData()
        countDataSource!!.close()
        sectionDataSource!!.close()

        val intent = NavUtils.getParentActivityIntent(this)!!
        intent.putExtra("section_id", sectionId)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        NavUtils.navigateUpTo(this, intent)

        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    companion object {
        const val TAG = "AddSpecAct"
    }

}
