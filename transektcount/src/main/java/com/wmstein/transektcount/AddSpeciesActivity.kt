package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.wmstein.transektcount.database.Count
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.widgets.AddSpeciesWidget
import com.wmstein.transektcount.widgets.HintAddWidget

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
 * last edited on 2024-12-14
 */
class AddSpeciesActivity : AppCompatActivity() {
    private var addArea: LinearLayout? = null
    private var addHintArea: LinearLayout? = null

    // Data
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null
    private var sectionId = 1

    // ID list of not yet included species in counting list
    private lateinit var remainingIdArrayList: Array<String?>

    // Complete ArrayLists of species
    private var namesCompleteArrayList: ArrayList<String>? = null
    private var namesReducedArrayList: ArrayList<String>? = null
    private var namesGCompleteArrayList: ArrayList<String>? = null
    private var namesGReducedArrayList: ArrayList<String>? = null
    private var codesCompleteArrayList: ArrayList<String?>? = null
    private var codesReducedArrayList: ArrayList<String?>? = null

    private var specName: String? = null
    private var specCode: String? = null
    private var specNameG: String? = null
    private var posSpec: Int = 0

    // 2 initial characters to limit selection
    private var initChars: String = ""

    // List of species to add
    private var listToAdd: ArrayList<AddSpeciesWidget>? = null

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.DLOG) Log.d(TAG, "73, onCreate")

        // Load preferences
        brightPref = prefs.getBoolean("pref_bright", true)

        setContentView(R.layout.activity_add_species)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        val extras = intent.extras
        if (extras != null) {
            sectionId = extras.getInt("section_id")
            initChars = extras.getString("init_Chars").toString()
        }
        if (MyDebug.DLOG) Log.d(TAG, "93, initChars: $initChars")

        listToAdd = ArrayList()

        addHintArea = findViewById(R.id.showHintAddLayout)
        addArea = findViewById(R.id.addSpecLayout)

        // Setup the data sources
        countDataSource = CountDataSource(this)
        sectionDataSource = SectionDataSource(this)

        // New onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (MyDebug.DLOG) Log.d(TAG, "107, handleOnBackPressed")

                val intent = NavUtils.getParentActivityIntent(this@AddSpeciesActivity)!!
                intent.putExtra("section_id", sectionId)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                this@AddSpeciesActivity.navigateUpTo(intent)
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (MyDebug.DLOG) Log.d(TAG, "122, onResume")

        countDataSource!!.open()
        sectionDataSource!!.open()

        // Load complete species ArrayList from arrays.xml (lists are sorted by code)
        namesCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs)))
        namesGCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs_l)))
        codesCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selCodes)))

        // Clear any existing views
        addArea!!.removeAllViews()
        addHintArea!!.removeAllViews()

        supportActionBar!!.setTitle(R.string.addTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Display hint: Further available species
        val haw = HintAddWidget(this, null)
        if (initChars.length == 2)
            haw.setSearchA(initChars)
        else
            haw.setSearchA(getString(R.string.hintSearch))
        addHintArea!!.addView(haw)

        constructAddList()
    }
    // End of onResume()

    // Get initial 2 characters of species to select by search button
    fun getAddInitialChars(view: View) {
        // Read EditText searchAdd from widget_add_hint.xml
        val searchAdd: EditText = findViewById(R.id.searchA)
        searchAdd.findFocus()

        // Get the initial characters of species to select from
        initChars = searchAdd.text.toString().trim()
        if (initChars.length == 1) {
            // Reminder: "Please, 2 characters"
            searchAdd.error = getString(R.string.initCharsL)
        } else {
            searchAdd.error = null

            if (MyDebug.DLOG) Log.d(TAG, "165, initChars: $initChars")

            // Call DummyActivity to reenter AddSpeciesActivity for reduced add list
            val intent = Intent(this@AddSpeciesActivity, DummyActivity::class.java)
            intent.putExtra("section_id", sectionId)
            intent.putExtra("init_Chars", initChars)
            intent.putExtra("is_Flag", "isAdd")
            startActivity(intent)
        }
    }

    // Construct add-species-list of not already contained species in the counting list
    //   and optionally reduce it further by initChar selection
    @SuppressLint("ApplySharedPref")
    private fun constructAddList() {
        // 1. Build list of codes of contained species in counting list
        val specCodesContainedList = ArrayList<String?>()

        // Get sorted species of the section 1 counting list
        val counts: List<Count> = countDataSource!!.getAllSpeciesForSectionSrtCode(1)

        // build ArrayList of codes of already contained species
        for (count in counts) {
            specCodesContainedList.add(count.code)
        }

        // 2. Build lists of all yet missing species
        val specCodesContainedListSize = specCodesContainedList.size
        if (MyDebug.DLOG) Log.d(TAG, "193, codesCountListSize: $specCodesContainedListSize")

        // Reduce complete arraylists for already contained species
        for (i in 0 until specCodesContainedListSize) {
            if (codesCompleteArrayList!!.contains(specCodesContainedList[i])) {
                // Remove species with specCode[i] from missing species lists.
                // Prerequisites: Exactly correlated arrays of selCodes, selSpecs and selSpecs_l
                specCode = specCodesContainedList[i]
                posSpec = codesCompleteArrayList!!.indexOf(specCode)
                if (MyDebug.DLOG) Log.d(TAG, "202, 1. specCode: $specCode, posSpec: $posSpec")
                namesCompleteArrayList!!.removeAt(posSpec)
                namesGCompleteArrayList!!.removeAt(posSpec)
                codesCompleteArrayList!!.removeAt(posSpec)
            }
        }

        if (MyDebug.DLOG) Log.d(TAG, "209, initChars: $initChars")
        if (MyDebug.DLOG) Log.d(TAG, "210, namesCompleteArrayListSize: "
                    + namesCompleteArrayList!!.size)

        // Copy ...CompleteArrayLists to ...ReducedArrayLists
        namesReducedArrayList = namesCompleteArrayList
        namesGReducedArrayList = namesGCompleteArrayList
        codesReducedArrayList = codesCompleteArrayList

        // 3. Further, optionally reduce the complete Arraylists for all but initChar species
        if (initChars.length == 2) {
            // Empty ...ReducedArrayLists
            namesReducedArrayList = arrayListOf()
            namesGReducedArrayList = arrayListOf()
            codesReducedArrayList = arrayListOf()

            // Check NamesCompleteArrayList for InitChars
            for (i in 0 until namesCompleteArrayList!!.size) {
                if (namesCompleteArrayList!![i].substring(0, 2) == initChars) {
                    specName = namesCompleteArrayList!![i]
                    specNameG = namesGCompleteArrayList!![i]
                    specCode = codesCompleteArrayList!![i]
                    if (MyDebug.DLOG) Log.d(TAG, "231, 2. specName: $specName, specCode: $specCode")

                    // Assemble remaining ReducedArrayLists for all Species with initChars
                    namesReducedArrayList!!.add(specName!!)
                    namesGReducedArrayList!!.add(specNameG!!)
                    codesReducedArrayList!!.add(specCode!!)
                }
            }
        }

        // Create remainingIdArrayList for all remaining species of codesCompleteArrayList
        remainingIdArrayList = arrayOfNulls(codesReducedArrayList!!.size)
        if (MyDebug.DLOG) Log.d(TAG, "243, remainingIdArrayListSize: " + remainingIdArrayList.size)
        var i = 0
        while (i < codesReducedArrayList!!.size) {
            remainingIdArrayList[i] = (i + 1).toString()
            i++
        }

        // Load the data of remaining species into the AddSpeciesWidget
        i = 0
        while (i < codesReducedArrayList!!.size) {
            val asw = AddSpeciesWidget(this, null)
            asw.setSpecName(namesReducedArrayList!![i])
            asw.setSpecNameG(namesGReducedArrayList!![i])
            asw.setSpecCode(codesReducedArrayList!![i])
            asw.setPicSpec(codesReducedArrayList!![i]!!)
            asw.setSpecId(remainingIdArrayList[i]!!)
            asw.setMarkSpec(false)
            addArea!!.addView(asw)
            i++
        }

        val editor = prefs.edit()
        editor.putString("is_Add", "")
        editor.commit()
    }

    // Mark the selected species and consider it for the species counts list
    fun checkBoxAdd(view: View) {
        val idToAdd = view.tag as Int
        if (MyDebug.DLOG) Log.d(TAG, "272, View.tag: $idToAdd")
        val asw = addArea!!.getChildAt(idToAdd) as AddSpeciesWidget

        val checked = asw.getMarkSpec() // return boolean isChecked

        // Put species on add list
        if (checked) {
            listToAdd!!.add(asw)
            if (MyDebug.DLOG) {
                val codeA = asw.getSpecCode()
                Log.d(TAG, "282, addCount, code: $codeA")
            }
        } else {
            // Remove species previously added from add list
            listToAdd!!.remove(asw)
            if (MyDebug.DLOG) {
                val codeA = asw.getSpecCode()
                Log.d(TAG, "289, removeCount, code: $codeA")
            }
        }
    }

    // Save added species to species lists of all sections
    @SuppressLint("ApplySharedPref")
    private fun addSpecs() {
        var i = 0 // index of species list to add
        val numSect: Int = sectionDataSource!!.numEntries
        while (i < listToAdd!!.size) {
            specName = listToAdd!![i].getSpecName()
            specCode = listToAdd!![i].getSpecCode()
            specNameG = listToAdd!![i].getSpecNameG()
            if (MyDebug.DLOG) {
                Log.d(TAG, "304, addSpecs, code: $specCode")
            }
            try {
                var sectid = 1
                while (sectid <= numSect) {
                    countDataSource!!.createCount(sectid, specName, specCode, specNameG)
                    sectid++
                }
            } catch (_: Exception) {
                // nothing
            }
            i++
        }

        // Re-index and sort counts table for code
        countDataSource!!.sortCounts()

        // Store code of last selected species in sharedPreferences
        //  for Spinner in CountingActivity
        if (i > 0) {
            val editor = prefs.edit()
            editor.putString("new_spec_code", specCode)
            editor.commit()
        }

        // Call DummyActivity to reenter AddSpeciesActivity to rebuild the species list
        val intent = Intent(this@AddSpeciesActivity, DummyActivity::class.java)
        intent.putExtra("init_Chars", "")
        intent.putExtra("is_Flag", "isAdd")
        startActivity(intent)
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
            finish()
            return true
        } else if (id == R.id.addSpecs) {
            if (listToAdd!!.isNotEmpty())
                addSpecs()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (MyDebug.DLOG) Log.d(TAG, "373, onPause")

        sectionDataSource!!.close()
        countDataSource!!.close()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (MyDebug.DLOG) Log.d(TAG, "382, onDestroy")

        addArea!!.removeAllViews()
        addHintArea!!.clearFocus()
        addHintArea!!.removeAllViews()
//        addHintArea!!.invalidate()
        addHintArea = null
    }

    companion object {
        const val TAG = "AddSpecAct"
    }

}
