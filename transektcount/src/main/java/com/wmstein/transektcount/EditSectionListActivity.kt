package com.wmstein.transektcount

/***************************************************************************************************
 * Copyright © 2016-2026, Wilhelm Stein, Bonn, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use any of its files except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************************************
 *
 * EditSectionListActivity lets you edit the current section name and the species
 * lists (change new species for all sections).
 * EditSectionListActivity is called from CountingActivity,
 * uses EditSectionListWidget.kt, EditSectionListTitleWidget.kt,
 * activity_edit_section_list.xml, widget_edit_title.xml, widget_edit_count.xml.
 *
 * Parts based on EditProjectActivity.java of BeeCount created by Milo Thurston (knirirr)
 * on 2014-05-05, licensed under the Apache License, Version 2.0.

 * Adopted, modified and enhanced by wmstein since 2016-02-16,
 * last edited in Java on 2023-07-07,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2026-10-21.
 **************************************************************************************************/

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT

import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

import com.wmstein.transektcount.Utils.fromHtml
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.database.TrackDataSource
import com.wmstein.transektcount.widgets.EditSectionListHintWidget
import com.wmstein.transektcount.widgets.EditSectionListTitleWidget
import com.wmstein.transektcount.widgets.EditSectionListWidget

import java.util.Locale

class EditSectionListActivity : AppCompatActivity() {
    // Data
    private var section: Section? = null
    private var sectionId = 1
    private var sectionBackup: Section? = null
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null
    private var trackDataSource: TrackDataSource? = null

    // Layouts
    private var editingSpeciesArea: LinearLayout? = null
    private var speciesNotesArea: LinearLayout? = null
    private var editHintArea: LinearLayout? = null

    // Widgets
    private var etw: EditSectionListTitleWidget? = null
    private var esw: EditSectionListWidget? = null

    // Arraylists
    private var cmpSpeciesNames: ArrayList<String>? = null
    private var cmpSpeciesCodes: ArrayList<String>? = null
    private var savedCounts: ArrayList<EditSectionListWidget>? = null

    // 2 or more characters to limit selection
    private var searchChars = ""

    private var diffSpecies = 0 // No. of different species in DB
    private var childCnt = 0

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false
    private var awakePref = false
    private var sortPref = ""
    private var oldName = ""
    private var msg = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "111, onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        setContentView(R.layout.activity_edit_section_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editSectionList))
        { v, windowInsets ->
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

        savedCounts = ArrayList() // EditSectionListWidget: countName, countNameG, countCode, pSpecies

        speciesNotesArea = findViewById(R.id.editingLineLayout)
        editHintArea = findViewById(R.id.showHintLayout)
        editingSpeciesArea = findViewById(R.id.editingSpeciesLayout)

        //  Note variables to restore them
        val extras = intent.extras
        if (extras != null) {
            sectionId = extras.getInt("section_id")
            searchChars = extras.getString("search_Chars").toString()
        }

        // Set up the data sources
        sectionDataSource = SectionDataSource(this)
        countDataSource = CountDataSource(this)
        trackDataSource = TrackDataSource(this)

        // New onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = NavUtils.getParentActivityIntent(this@EditSectionListActivity)!!
                intent.putExtra("section_id", sectionId)
                intent.flags = FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                this@EditSectionListActivity.navigateUpTo(intent)
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "173, onResume")

        // Load preferences
        brightPref = prefs.getBoolean("pref_bright", true)
        awakePref = prefs.getBoolean("pref_awake", true)
        sortPref = prefs.getString("pref_sort_sp", "none").toString()

        // Set full brightness of screen
        if (brightPref) {
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        if (awakePref)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        countDataSource!!.open()
        sectionDataSource!!.open()
        trackDataSource!!.open()

        // Build the EditSectionList screen
        editingSpeciesArea!!.removeAllViews()
        speciesNotesArea!!.removeAllViews()
        editHintArea!!.removeAllViews()

        supportActionBar!!.setTitle(R.string.editSpeciesTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Load the sections data
        section = sectionDataSource!!.getSection(sectionId)
        oldName = section!!.name

        // Edit the section name
        etw = EditSectionListTitleWidget(this, null)
        etw!!.sectionName = oldName
        etw!!.setWidgetTitle(getString(R.string.titleEdit))
        speciesNotesArea!!.addView(etw)
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "212, onResume, EditSectionListTitleWidget, old section name: "
                        + oldName + ", new sectionName: " + etw!!.sectionName)

        // Display hint: Current species list
        val hew = EditSectionListHintWidget(this, null)
        if (searchChars.length >= 2)
            hew.setSearchE1(searchChars)
        else
            hew.setSearchE(getString(R.string.hintSearch))
        editHintArea!!.addView(hew)

        constructEditList()
    }
    // End of onResume()

    // Get 2 or more characters of species to select by search button
    // Parameter view is necessary for function call
    fun getEditSearchChars(view: View) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "231, getEditSearchChars, View: $view")

        // Read EditText searchEdit from widget_edit_hint.xml
        val searchEdit: EditText = findViewById(R.id.searchE)
        searchEdit.findFocus()

        // Get the search characters to build a species list to select from
        searchChars = searchEdit.text.toString().trim()
        if (searchChars.length == 1) {
            // Reminder: "Please, >1 chars"
            searchEdit.error = getString(R.string.searchCharsL)
        } else {
            searchEdit.error = null
            searchEdit.clearFocus()
            searchEdit.invalidate()

            // Re-enter EditSectionListActivity for the reduced species-edit-list
            val intent = Intent(this@EditSectionListActivity, EditSectionListActivity::class.java)
            intent.putExtra("section_id", sectionId)
            intent.putExtra("search_Chars", searchChars)
            intent.flags = FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    // Construct edit-species-list of contained species from 1. section
    //   and optionally reduce it to searchChars selection
    private fun constructEditList() {
        // Load the sorted species data from section 1
        val countsToEditList = when (sortPref) {
            "names_alpha" -> countDataSource!!.getAllSpeciesForSectionSrtName(1)
            "codes" -> countDataSource!!.getAllSpeciesForSectionSrtCode(1)
            else -> countDataSource!!.getAllSpeciesForSection(1)
        }

        diffSpecies = countsToEditList.size

        // Display all the countsToEditList by adding them to editingSpeciesArea
        // Get the counting list species into their EditSectionListWidget and add them to the view
        if (searchChars.length >= 2) {
            searchChars = searchChars.uppercase(Locale.getDefault()) //Compare searchChars in uppercase

            // Check name in countsToEditList for searchChars to reduce list
            for (count in countsToEditList) {
                if (count.name.uppercase(Locale.getDefault()).contains(searchChars)) {
                    esw = EditSectionListWidget(this, null)
                    esw!!.setCountName(count.name)
                    esw!!.setCountNameG(count.name_g)
                    esw!!.setCountCode(count.code)
                    esw!!.setPicSpec(count)
                    esw!!.setCountId(count.id)
                    editingSpeciesArea!!.addView(esw)
                }
            }
        } else {
            for (count in countsToEditList) {
                esw = EditSectionListWidget(this, null)
                esw!!.setCountName(count.name)
                esw!!.setCountNameG(count.name_g)
                esw!!.setCountCode(count.code)
                esw!!.setPicSpec(count)
                esw!!.setCountId(count.id)
                editingSpeciesArea!!.addView(esw)
            }
        }
    }

    // Test species list for double entry
    private fun testData(): Boolean {
        var retValue = true

        // Check for unique species names
        val isDbl: String = compSpeciesNames()
        if (isDbl != "") {
            msg = isDbl + " " + getString(R.string.isdouble) + " " + getString(R.string.duplicate)
            Toast.makeText(
                applicationContext,
                fromHtml("<font color='red'><b>$msg</b></font>"),
                LENGTH_LONG
            ).show()
            retValue = false
        }
        return retValue
    }

    // Save changed and unique section name,
    // call doEditedSpecies()
    private fun saveData(): Boolean {
        // Save section name only if it has been changed and is unique
        var saveState: Boolean

        // Add title if the user has written one
        val newSectName = etw!!.sectionName // edited section name
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "326, newSectName: $newSectName")

        if ((newSectName.isNotEmpty())) {
            // Check for duplicate section name
            sectionBackup = section
            if (compSectionNames(newSectName)) {
                msg = newSectName + " " + getString(R.string.isdouble)
                Toast.makeText(
                    applicationContext,
                    fromHtml("<font color='red'><b>$msg</b></font>"),
                    LENGTH_LONG
                ).show()
                saveState = false
            } else {
                sectionBackup!!.name = newSectName
                saveState = true
            }
            // Save edited section with new name
            section = sectionBackup

            // Save new section name also as track name in track table
            trackDataSource!!.saveTrackName(newSectName, oldName)
        } else {
            msg = getString(R.string.isempty)
            Toast.makeText(
                applicationContext,
                fromHtml("<font color='red'><b>$msg</b></font>"),
                LENGTH_LONG
            ).show()
            saveState = false
        }

        if (saveState) {
            // Save changed section name
            sectionDataSource!!.saveSectionName(section!!)
            msg = getString(R.string.sectSaving)
            Toast.makeText(
                applicationContext,
                fromHtml("<font color='#008000'>$msg</font>"),
                LENGTH_SHORT
            ).show()
        }

        saveState = doEditedSpecies()
        return saveState
    }

    // Save the changed species data for all sections
    private fun doEditedSpecies(): Boolean {
        // Read the edited species list
        var correct = false
        childCnt = editingSpeciesArea!!.childCount // No. of counts in selection list
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "379, childCnt: $childCnt")

        // Check for unique species names and codes before storing
        val isDblName: String = compSpeciesNames()
        val isDblCode: String = compSpeciesCodes()

        // Updates species names and code for all sections if no doubles
        if (isDblName == "" && isDblCode == "") {
            // Copy edited species list for all other sections
            val numSect: Int = sectionDataSource!!.numEntries
            var si = 1   // section index
            var ci = 1   // count index
            
            var cid: Int // count id
            var cName: String
            var cCode: String
            var cNameG: String

            // For all sections
            while (si <= numSect) {
                // for all selected species per section
                for (i in 0 until childCnt) {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG, "402, Section: $si, Species $ci")

                    val eSLW = editingSpeciesArea!!.getChildAt(i) as EditSectionListWidget
                    cid = eSLW.getCountId() + (diffSpecies * (si - 1)) // cid with offset for sections
                    cName = eSLW.getCountName()
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG, "408, id: $cid, name: $cName")

                    cCode = eSLW.getCountCode()
                    if ((cName.isNotEmpty()) && (cCode.isNotEmpty())) {
                        cNameG = eSLW.getCountNameG()
                        countDataSource!!.updateEditedSpecies(
                            si,
                            cid,
                            cName,
                            cCode,
                            cNameG
                        )
                        correct = true
                    } else {
                        msg = getString(R.string.isempty)
                        Toast.makeText(
                            applicationContext,
                            fromHtml("<font color='red'><b>$msg</b></font>"),
                            LENGTH_LONG
                        ).show()
                        correct = false
                        break
                    }

                    ci++
                }

                if (!correct) break
                si++
            }
        } else {
            msg = (getString(R.string.spec_Name) + " " + isDblName + " "
                    + getString(R.string.orcode) + " " + isDblCode + " " + getString(
                R.string.isdouble
            ))
            Toast.makeText(
                applicationContext,
                fromHtml("<font color='red'><b>$msg</b></font>"),
                LENGTH_LONG
            ).show()
            correct = false
        }
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "451, getEditSpecies, ok: $correct")

        return correct
    }

    // Compare section names for duplicates and return TRUE when duplicate found
    private fun compSectionNames(newSectName: String): Boolean {
        var isDblName = false
        var sName: String
        if (newSectName == oldName) {
            return false // name has not changed
        }
        val sectionList = sectionDataSource!!.allSectionNames
        childCnt = sectionList.size + 1

        // For all Sections
        for (i in 1 until childCnt) {
            section = sectionDataSource!!.getSection(i)
            sName = section!!.name
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "471, sName = $sName")

            if (newSectName == sName) {
                isDblName = true
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "476, Double section name = $sName")

                break
            }
        }
        return isDblName
    }

    // Compare species names for duplicates and returns name of 1. duplicate found
    private fun compSpeciesNames(): String {
        var name: String
        var isDblName = ""
        cmpSpeciesNames = ArrayList()
        childCnt = editingSpeciesArea!!.childCount // # of species

        // For all CountEditWidgets
        for (i in 0 until childCnt) {
            val eSLW = editingSpeciesArea!!.getChildAt(i) as EditSectionListWidget
            name = eSLW.getCountName()
            if (cmpSpeciesNames!!.contains(name)) {
                isDblName = name
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "498, Double name = $isDblName")

                break
            }
            cmpSpeciesNames!!.add(name)
        }
        return isDblName
    }

    // Compare count codes for duplicates and returns name of 1. duplicate found
    private fun compSpeciesCodes(): String {
        var code: String
        var isDblCode = ""
        cmpSpeciesCodes = ArrayList()
        childCnt = editingSpeciesArea!!.childCount

        // For all CountEditWidgets
        for (i in 0 until childCnt) {
            val eSLW = editingSpeciesArea!!.getChildAt(i) as EditSectionListWidget
            code = eSLW.getCountCode()
            if (cmpSpeciesCodes!!.contains(code)) {
                isDblCode = code
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "521, Double name = $isDblCode")

                break
            }
            cmpSpeciesCodes!!.add(code)
        }
        return isDblCode
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
            intent.flags = FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "561 onPause")

        // Close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()
        trackDataSource!!.close()

        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        editingSpeciesArea!!.clearFocus()
        editingSpeciesArea!!.removeAllViews()
        speciesNotesArea!!.clearFocus()
        speciesNotesArea!!.removeAllViews()
        editHintArea!!.clearFocus()
        editHintArea!!.removeAllViews()
    }

    override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "584 onStop")

        editingSpeciesArea = null
        speciesNotesArea = null
        editHintArea = null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "595, onDestroy")
    }

    companion object {
        private const val TAG = "EditSectionListAct"
    }

}
