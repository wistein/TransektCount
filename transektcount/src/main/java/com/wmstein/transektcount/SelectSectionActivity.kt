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
 * Shows the list of selectable sections which is put together by SelectSectionAdapter.
 * Starts EditSectionListActivity.
 *
 * Parts based on ListProjectActivity.java of BeeCount created by Milo Thurston (knirirr)
 * on 2014-05-05, licensed under the Apache License, Version 2.0.

 * Changes and additions for TransektCount by wmstein since 2016-02-16,
 * last edited in Java on 2023-07-07,
 * converted to Kotlin on 2023-07-17,
 * renamed from ListSectionActivity.kt on 2024-11-26,
 * last edited on 2026-10-21.
 **************************************************************************************************/

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
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

import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.database.TrackDataSource
import com.wmstein.transektcount.Utils.fromHtml

class SelectSectionActivity : AppCompatActivity() {
    private lateinit var transektCount: TransektCountApplication

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false
    private var awakePref = false
    private var transectHasTrack = false
    private var autoSection = false // true for enabled GPS track selection
    private var buttonSoundPref = false

    private var soundService: SoundService? = null

    // Data
    private var sectionDataSource: SectionDataSource? = null
    private var section: Section? = null // record in SQLite sections table
    private var sections: List<Section>? = null // list of all sections
    private var maxId = 0
    private var list: ListView? = null
    private var countDataSource: CountDataSource? = null
    private var trackDataSource: TrackDataSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "71, onCreate")

        transektCount = application as TransektCountApplication

        // Register SelectSectionActivity to observe its lifecycle.
        //  Used to modify the color of the GPS-selected section name
        //  by restarting it in LocationService
        val lifeCycleObserver = ActivityLifecycleObserver() // instance of observer
        lifecycle.addObserver(lifeCycleObserver)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        setContentView(R.layout.activity_list_section)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.list_view))
        { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }
            WindowInsetsCompat.CONSUMED
        }

        transectHasTrack = prefs.getBoolean("transect_has_track", false)
        autoSection = prefs.getBoolean("pref_auto_section", false)
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false)

        val listView = findViewById<LinearLayout>(R.id.list_view)
        listView.background = transektCount.setBackground()

        sectionDataSource = SectionDataSource(this)
        trackDataSource = TrackDataSource(this)
        countDataSource = CountDataSource(this)

        brightPref = prefs.getBoolean("pref_bright", true)
        awakePref = prefs.getBoolean("pref_awake", true)

        // Set full brightness of screen
        if (brightPref) {
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        if (awakePref)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sectionDataSource!!.open()
        trackDataSource!!.open()
        countDataSource!!.open()
        sections = sectionDataSource!!.getAllSections(prefs)
        maxId = sectionDataSource!!.maxId

        // New onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavUtils.navigateUpFromSameTask(this@SelectSectionActivity)
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "144, onResume")

        list = findViewById(android.R.id.list)
        showData()
    }

    // Show sections list by SelectSectionAdapter
    private fun showData() {
        val adapter = SelectSectionAdapter(
            this,
            R.layout.listview_section_row,
            sections!!,
            maxId
        )
        val lv = findViewById<ListView>(android.R.id.list)
        lv.adapter = adapter
    }

    // Inflate the menu; this adds items to the action bar if it is present.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.select_section, menu)
        return true
    }

    // Handle action bar item clicks here.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menuCloneSection) {
            if (transectHasTrack) {
                val msg = getString(R.string.hasTrack)
                Toast.makeText(
                    this,
                    fromHtml("<font color='red'><b>$msg</b></font>"),
                    LENGTH_LONG
                ).show()
            } else {
                cloneSection()
                showData()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "192, onPause")

        sectionDataSource!!.close()
        trackDataSource!!.close()
        countDataSource!!.close()

        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "207, onStop")

        list!!.invalidate()

        if (buttonSoundPref) {
            soundService = SoundService(applicationContext)
            soundService!!.releaseSoundP()
        }
    }

    // Clone section with check for double names
    private fun cloneSection() {
        val aDialog = AlertDialog.Builder(this)

        aDialog.setTitle(getString(R.string.cloneSectTitle))

        // Set up the input and specify the type of input expected
        val input = EditText(this)
        input.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        var msg: String

        // Set up the buttons
        aDialog.setView(input)
        aDialog.setPositiveButton(
            "OK",
            DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                // Enter a new section name
                val newSectName = input.text.toString()

                // Check for empty section name
                if (newSectName.isEmpty()) {
                    msg = getString(R.string.attention) + " " + getString(R.string.newSectName)
                    Toast.makeText(
                        this,
                        fromHtml("<font color='red'><b>$msg</b></font>"),
                        LENGTH_LONG
                    ).show()
                    return@OnClickListener
                }

                // Check if this is not a duplicate of an existing name
                if (compSectionNames(newSectName)) {
                    msg =
                        getString(R.string.attention) + " " + newSectName + " " + getString(R.string.isdouble)
                    Toast.makeText(
                        this,
                        fromHtml("<font color='red'><b>$msg</b></font>"),
                        LENGTH_LONG
                    ).show()
                    return@OnClickListener
                }

                // Check if section is contiguous
                var entries = -1
                try {
                    entries = sectionDataSource!!.numEntries
                } catch (e: Exception) {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.e(TAG, "266 getNumEntries failed, $e")
                }

                try {
                    maxId = sectionDataSource!!.maxId
                } catch (e: Exception) {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.e(TAG, "273 getMaxId failed, $e")
                }

                if (entries != maxId) {
                    msg = getString(R.string.notContiguous)
                    Toast.makeText(
                        this,
                        fromHtml("<font color='red'><b>$msg</b></font>"),
                        LENGTH_LONG
                    ).show()
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG, "284 maxId: $maxId, entries: $entries")
                    return@OnClickListener
                }

                // Creating the new section
                val newSection = sectionDataSource!!.createSection(newSectName)
                sectionDataSource!!.saveSectionName(newSection)
                for (c in countDataSource!!.getAllSpeciesForSection(1)) {
                    val newCount =
                        countDataSource!!.createCount(newSection.id, c.name, c.code, c.name_g)
                    if (newCount != null) {
                        countDataSource!!.saveCount(newCount)
                    }
                }

                // Exit this and go to the list of new sections
                msg = newSectName + " " + getString(R.string.newCopyCreated)
                Toast.makeText(
                    this,
                    fromHtml("<font color='#008000'>$msg</font>"),
                    LENGTH_SHORT
                ).show()
                sections =
                    sectionDataSource!!.getAllSections(prefs) // with sorting order of pref_sort_sect
                maxId = sectionDataSource!!.maxId

                // Restart SelectSectionActivity with modified section list
                val intent = Intent(
                    this@SelectSectionActivity,
                    SelectSectionActivity::class.java
                )
                intent.flags = FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            })
        aDialog.setNegativeButton(
            "Cancel"
        ) { dialog: DialogInterface?, _: Int -> dialog!!.cancel() }
        aDialog.show()
    }
    // End of cloneSection()

    // Compare section names for duplicates and return state of duplicate found
    private fun compSectionNames(newName: String): Boolean {
        var isDblName = false
        var selSectName: String?

        val sectionList: List<Section?> = sectionDataSource!!.getAllSections(prefs)

        val childCnt = sectionList.size + 1

        // For all sections
        for (i in 1..<childCnt) {
            section = sectionDataSource!!.getSection(i)
            selSectName = section!!.name
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "340, compSectionNames, selSectName = $selSectName")

            if (newName == selSectName) {
                isDblName = true
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "345, compSectionNames, Double name = $selSectName")
                break
            }
        }
        return isDblName
    }

    // Delete last section with respective track points
    fun deleteSection(sct: Section?) {
        val sctName = sct!!.name
        if (transectHasTrack) {
            // Delete track points of section to be deleted
            trackDataSource!!.deleteAllTrkPointsOfSection(sctName)
        }

        // Delete last section
        sectionDataSource!!.deleteSection(sct)
        sections = sectionDataSource!!.getAllSections(prefs)
        maxId = sectionDataSource!!.maxId

        showData()
        list!!.invalidate() //force list to draw
    }

    companion object {
        private const val TAG = "SelSectAct"
    }

}
