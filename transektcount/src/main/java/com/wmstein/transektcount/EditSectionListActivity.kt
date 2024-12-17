package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils

import com.google.android.material.snackbar.Snackbar

import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.widgets.EditSpeciesWidget
import com.wmstein.transektcount.widgets.EditTitleWidget
import com.wmstein.transektcount.widgets.HintEditWidget

/********************************************************************************
 * EditSectionListActivity lets you edit the current section name and the species
 * lists (change new species for all sections).
 * EditSectionListActivity is called from CountingActivity,
 * uses EditSpeciesWidget.kt, EditTitleWidget.kt,
 * activity_edit_section_list.xml, widget_edit_title.xml.
 *
 * Based on EditProjectActivity.java by milo on 05/05/2014.
 * Adopted, modified and enhanced by wmstein since 2016-02-16,
 * last edited in Java on 2023-07-07,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2024-12-17
 */
class EditSectionListActivity : AppCompatActivity() {
    // Data
    var section: Section? = null
    private var sectionId = 1
    private var sectionBackup: Section? = null
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null

    // Layouts
    private var editingSpeciesArea: LinearLayout? = null
    private var speciesNotesArea: LinearLayout? = null
    private var editHintArea: LinearLayout? = null

    // Widgets
    private var etw: EditTitleWidget? = null
    private var esw: EditSpeciesWidget? = null

    // Arraylists
    private var cmpCountNames: ArrayList<String>? = null
    private var cmpCountCodes: ArrayList<String>? = null
    private var savedCounts: ArrayList<EditSpeciesWidget>? = null
    private var listToEdit: ArrayList<EditSpeciesWidget>? = null

    // 2 initial characters to limit selection
    private var initChars: String = ""

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false
    private var sortPref: String? = null
    private var oldName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.dLOG) Log.d(TAG, "82 onCreate")

        // Load preference
        brightPref = prefs.getBoolean("pref_bright", true)
        sortPref = prefs.getString("pref_sort_sp", "none")

        setContentView(R.layout.activity_edit_section_list)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        savedCounts = ArrayList()
        listToEdit = ArrayList()

        speciesNotesArea = findViewById(R.id.editingLineLayout)
        editHintArea = findViewById(R.id.showHintLayout)
        editingSpeciesArea = findViewById(R.id.editingSpeciesLayout)

        // Restore any edit widgets the user has added previously
        if (savedInstanceState != null) {
            if (Build.VERSION.SDK_INT < 33) {
                @Suppress("DEPRECATION")
                if (savedInstanceState.getSerializable("savedCounts") != null) {
                    @Suppress("UNCHECKED_CAST")
                    savedCounts = savedInstanceState.getSerializable("savedCounts")
                            as ArrayList<EditSpeciesWidget>?
                }
            } else {
                if (savedInstanceState.getSerializable("savedCounts", T::class.java) != null) {
                    @Suppress("UNCHECKED_CAST")
                    savedCounts = savedInstanceState.getSerializable(
                        "savedCounts", T::class.java
                    ) as ArrayList<EditSpeciesWidget>?
                }
            }
        }

        //  Note variables to restore them
        val extras = intent.extras
        if (extras != null) {
            sectionId = extras.getInt("section_id")
            initChars = extras.getString("init_Chars").toString()
        }

        // Setup the data sources
        sectionDataSource = SectionDataSource(this)
        countDataSource = CountDataSource(this)

        // New onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = NavUtils.getParentActivityIntent(this@EditSectionListActivity)!!
                intent.putExtra("section_id", sectionId)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                this@EditSectionListActivity.navigateUpTo(intent)
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (MyDebug.dLOG) Log.d(TAG, "151 onResume")

        // Load preferences
        prefs = TransektCountApplication.getPrefs()
        brightPref = prefs.getBoolean("pref_bright", true)

        // Build the Edit Section screen
        editingSpeciesArea!!.removeAllViews()
        speciesNotesArea!!.removeAllViews()
        editHintArea!!.removeAllViews()

        countDataSource!!.open()
        sectionDataSource!!.open()

        // Load the sections data
        section = sectionDataSource!!.getSection(sectionId)
        oldName = section!!.name
        try {
            supportActionBar!!.title = getString(R.string.headEdit)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        } catch (_: NullPointerException) {
            if (MyDebug.dLOG) Log.e(TAG, "172, NullPointerException: No section name!")
        }

        // Edit the section name
        etw = EditTitleWidget(this, null)
        etw!!.sectionName = oldName
        etw!!.setWidgetTitle(getString(R.string.titleEdit))
        speciesNotesArea!!.addView(etw)
        if (MyDebug.dLOG)
            Log.d(TAG, "181, onResume, EditTitleWidget, old section name: "
                    + oldName + ", new sectionName: " + etw!!.sectionName
            )

        // Display hint: Current species list
        val hew = HintEditWidget(this, null)
        if (initChars.length == 2)
            hew.setSearchE(initChars)
        else
            hew.setSearchE(getString(R.string.hintSearch))
        editHintArea!!.addView(hew)

        constructEditList()
    }
    // End of onResume()

    // Get initial 2 characters of species to select by search button
    fun getEditInitialChars(view: View) {
        // Read EditText searchEdit from widget_edit_hint.xml
        val searchEdit: EditText = findViewById(R.id.searchE)
        searchEdit.findFocus()

        // Get the initial characters of species to select from
        initChars = searchEdit.text.toString().trim()
        if (initChars.length == 1) {
            // Reminder: "Please, 2 characters"
            searchEdit.error = getString(R.string.initCharsL)
        } else {
            searchEdit.error = null

            if (MyDebug.dLOG) Log.d(TAG, "211, initChars: $initChars")

            // Call DummyActivity to reenter EditSectionListActivity for reduced add list
            countDataSource!!.close()
            sectionDataSource!!.close()
            val intent = Intent(this@EditSectionListActivity, DummyActivity::class.java)
            intent.putExtra("section_id", sectionId)
            intent.putExtra("init_Chars", initChars)
            intent.putExtra("is_Flag", "isEdit")
            startActivity(intent)
        }
    }

    // Construct edit-species-list of contained species in the counting list
    //   and optionally reduce it by initChar selection
    private fun constructEditList() {
        // Load the sorted species data from section 1
        val counts = when (sortPref) {
            "names_alpha" -> countDataSource!!.getAllSpeciesForSectionSrtName(1)
            "codes" -> countDataSource!!.getAllSpeciesForSectionSrtCode(1)
            else -> countDataSource!!.getAllCountsForSection(1)
        }

        // Display all the counts by adding them to editingSpeciesArea
        // Get the counting list species into their EditSpeciesWidget and add them to the view
        if (initChars.length == 2) {
            // Check name in counts for InitChars to reduce list
            var cnt = 1
            for (count in counts) {
                if (count.name?.substring(0, 2) == initChars) {
                    esw = EditSpeciesWidget(this, null)
                    esw!!.setCountName(count.name)
                    esw!!.setCountNameG(count.name_g)
                    esw!!.setCountCode(count.code)
                    esw!!.setPicSpec(count)
                    esw!!.setCountId(count.id)
                    editingSpeciesArea!!.addView(esw)
                    cnt++
                }
            }
        } else {
            for (count in counts) {
                esw = EditSpeciesWidget(this, null)
                esw!!.setCountName(count.name)
                esw!!.setCountNameG(count.name_g)
                esw!!.setCountCode(count.code)
                esw!!.setPicSpec(count)
                esw!!.setCountId(count.id)
                editingSpeciesArea!!.addView(esw)
                if (MyDebug.dLOG) Log.d(TAG, "260, name: " + count.name)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (MyDebug.dLOG) Log.d(TAG, "268 onPause")

        // Close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()
    }

    override fun onDestroy() {
        super.onDestroy()

        editHintArea = null
        speciesNotesArea = null
        editingSpeciesArea = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Widgets must be removed from their parent before they can be serialised,
        for (esw in savedCounts!!) {
            (esw.parent as ViewGroup).removeView(esw)
        }
        outState.putSerializable("savedCounts", savedCounts)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.edit_species_list, menu)
        return true
    }

    @SuppressLint("ApplySharedPref")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId

        if (id == android.R.id.home) {
            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.putExtra("section_id", sectionId)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            this@EditSectionListActivity.navigateUpTo(intent)
            return true
        } else if (id == R.id.editSpecL) {
            if (testData()) {
                if (saveData())
                    savedCounts!!.clear()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Test for double entries and save species lists
    private fun saveData(): Boolean {
        // Save section name only if it has changed
        var saveState: Boolean

        // Add title if the user has written one
        val newSectName = etw!!.sectionName // edited section name
        if (MyDebug.dLOG) Log.d(TAG, "327, newSectName: $newSectName")

        if (isNotEmpty(newSectName)) {
            // Check if this is not a duplicate of an existing section name
            sectionBackup = section
            if (compSectionNames(newSectName)) {
                showSnackbarRed(newSectName + " " + getString(R.string.isdouble))
                saveState = false
            } else {
                sectionBackup!!.name = newSectName
                saveState = true
            }
            // Save edited section with new name
            section = sectionBackup

        } else {
            showSnackbarRed(getString(R.string.isempty))
            saveState = false
        }

        if (saveState) {
            // Save changed section name
            sectionDataSource!!.saveSection(section!!)
            // Toast here, as snackbar doesn't show up
            Toast.makeText(
                applicationContext, getString(R.string.sectSaving),
                Toast.LENGTH_SHORT
            ).show()
        }

        if (doEditedSpecies())
            saveState = true
        else
            saveState = false
        return saveState
    }

    private fun doEditedSpecies(): Boolean {
        // Read the edited species list
        var correct = false
        val childcount: Int = editingSpeciesArea!!.childCount //No. of counts in list
        if (MyDebug.dLOG) Log.d(TAG, "368, childcount: $childcount")

        // Check for unique species names and codes before storing
        val isDblName: String = compCountNames()
        val isDblCode: String = compCountCodes()

        // Updates species names and code for all sections if no doubles
        if (isDblName == "" && isDblCode == "") {
            // Copy edited species list for all other sections
            val numSect: Int = sectionDataSource!!.numEntries
            var si = 1   // section id
            var ci = 1   // count id
            var cname: String
            var ccode: String
            var cnameg: String

            // For all sections
            while (si <= numSect) {
                // for all species per section
                for (i in 0 until childcount) {
                    if (MyDebug.dLOG) Log.d(TAG, "388, Section: $si, Species $i")
                    esw = editingSpeciesArea!!.getChildAt(i) as EditSpeciesWidget
                    cname = esw!!.getCountName()
                    ccode = esw!!.getCountCode()
                    if (isNotEmpty(cname) && isNotEmpty(ccode)) {
                        cnameg = esw!!.getCountNameG()
                        countDataSource!!.updateCountForAllSections(
                            si,
                            ci,
                            cname,
                            ccode,
                            cnameg
                        )
                        ci++
                        correct = true
                    } else {
                        showSnackbarRed(getString(R.string.isempt))
                        correct = false
                        break
                    }
                }
                if (!correct) break
                si++
            }
        } else {
            showSnackbarRed(
                getString(R.string.spname) + " " + isDblName + " " + getString(R.string.orcode) + " " + isDblCode + " " + getString(
                    R.string.isdouble
                )
            )
            correct = false
        }
        if (MyDebug.dLOG) Log.d(TAG, "420, getEditSpecies, ok: $correct")
        return correct
    }

    // Test species list for double entry
    private fun testData(): Boolean {
        var retValue = true
        val isDbl: String

        // Check for unique species names
        isDbl = compCountNames()
        if (isDbl != "") {
            showSnackbarRed(
                isDbl + " " + getString(R.string.isdouble) + " " + getString(R.string.duplicate)
            )
            retValue = false
        }
        return retValue
    }

    // Compare section names for duplicates and return TRUE when duplicate found
    private fun compSectionNames(newSectName: String?): Boolean {
        var isDblName = false
        var sname: String?
        if (newSectName == oldName) {
            return false // name has not changed
        }
        val sectionList = sectionDataSource!!.allSectionNames
        val childcount = sectionList.size + 1

        // For all Sections
        for (i in 1 until childcount) {
            section = sectionDataSource!!.getSection(i)
            sname = section!!.name
            if (MyDebug.dLOG)
                Log.d(TAG, "447, sname = $sname")
            if (newSectName == sname) {
                isDblName = true
                if (MyDebug.dLOG)
                    Log.d(TAG, "451, Double name = $sname")
                break
            }
        }
        return isDblName
    }

    // Compare count names for duplicates and returns name of 1. duplicate found
    private fun compCountNames(): String {
        var name: String
        var isDblName = ""
        cmpCountNames = ArrayList()
        val childcount = editingSpeciesArea!!.childCount

        // For all CountEditWidgets
        for (i in 0 until childcount) {
            esw = editingSpeciesArea!!.getChildAt(i) as EditSpeciesWidget
            name = esw!!.getCountName()
            if (cmpCountNames!!.contains(name)) {
                isDblName = name
                if (MyDebug.dLOG) Log.d(TAG, "471, Double name = $isDblName")
                break
            }
            cmpCountNames!!.add(name)
        }
        return isDblName
    }

    // Compare count codes for duplicates and returns name of 1. duplicate found
    private fun compCountCodes(): String {
        var code: String
        var isDblCode = ""
        cmpCountCodes = ArrayList()
        val childcount = editingSpeciesArea!!.childCount

        // For all CountEditWidgets
        for (i in 0 until childcount) {
            esw = editingSpeciesArea!!.getChildAt(i) as EditSpeciesWidget
            code = esw!!.getCountCode()
            if (cmpCountCodes!!.contains(code)) {
                isDblCode = code
                if (MyDebug.dLOG) Log.d(TAG, "492, Double name = $isDblCode")
                break
            }
            cmpCountCodes!!.add(code)
        }
        return isDblCode
    }

    private fun showSnackbarRed(str: String) // bold red text
    {
        val view = findViewById<View>(R.id.editingScreen)
        val sB = Snackbar.make(view, str, Snackbar.LENGTH_LONG)
        sB.setTextColor(Color.RED)
        val tv = sB.view.findViewById<TextView>(R.id.snackbar_text)
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        sB.show()
    }

    companion object {
        private const val TAG = "EditSpecListAct"

        /**
         * Checks if a CharSequence is empty ("") or null.
         *
         * isEmpty(null)      = true
         * isEmpty("")        = true
         * isEmpty(" ")       = false
         * isEmpty("bob")     = false
         * isEmpty("  bob  ") = false
         *
         * @param cs the CharSequence to check, may be null
         * @return `true` if the CharSequence is empty or null
         */
        private fun isEmpty(cs: CharSequence?): Boolean {
            return cs.isNullOrEmpty()
        }

        /**
         * Checks if a CharSequence is not empty ("") and not null.
         *
         * isNotEmpty(null)      = false
         * isNotEmpty("")        = false
         * isNotEmpty(" ")       = true
         * isNotEmpty("bob")     = true
         * isNotEmpty("  bob  ") = true
         *
         * @param cs the CharSequence to check, may be null
         * @return `true` if the CharSequence is not empty and not null
         */
        fun isNotEmpty(cs: CharSequence?): Boolean {
            return !isEmpty(cs)
        }
    }

}
