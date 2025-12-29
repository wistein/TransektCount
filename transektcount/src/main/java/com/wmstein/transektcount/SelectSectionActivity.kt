package com.wmstein.transektcount

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
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.database.TrackDataSource

/************************************************************************************
 * Shows the list of selectable sections which is put together by SelectSectionAdapter.
 * Based on ListProjectActivity.java by milo on 05/05/2014.
 * Starts EditSectionListActivity.
 * Changes and additions for TransektCount by wmstein since 2016-02-16,
 * last edited in Java on 2023-07-07,
 * converted to Kotlin on 2023-07-17,
 * renamed from ListSectionActivity.kt on 2024-11-26,
 * last edited on 2025-12-29
 */
class SelectSectionActivity : AppCompatActivity() {
    private var transektCount: TransektCountApplication? = null

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false
    private var awakePref = false
    private var transectHasTrack = false
    private var autoSection = false // true for enabled GPS track selection

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
            Log.i(TAG, "65, onCreate")

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

        transektCount = application as TransektCountApplication

        val listView = findViewById<LinearLayout>(R.id.list_view)
        listView.background = transektCount!!.setBackgr()

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
            Log.i(TAG, "131, onResume")

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
                val mesg = getString(R.string.hasTrack)
                Toast.makeText(
                    this,
                    HtmlCompat.fromHtml(
                        "<font color='red'><b>$mesg</b></font>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    ), Toast.LENGTH_LONG
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
            Log.i(TAG, "181, onPause")

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
            Log.i(TAG, "196, onStop")

        list!!.invalidate()
    }

    // Clone section with check for double names
    private fun cloneSection() {
        val aDialog = AlertDialog.Builder(this)

        aDialog.setTitle(getString(R.string.dpSectTitle))

        // Set up the input an specify the type of input expected
        val input = EditText(this)
        input.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        var mesg: String

        // Set up the buttons
        aDialog.setView(input)
        aDialog.setPositiveButton(
            "OK",
            DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                // Enter a new section name
                val newSectName = input.text.toString()

                // Check for empty section name
                if (newSectName.isEmpty()) {
                    mesg = getString(R.string.attention) + " " + getString(R.string.newSectName)
                    Toast.makeText(
                        this,
                        HtmlCompat.fromHtml(
                            "<font color='red'><b>$mesg</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                        ), Toast.LENGTH_LONG
                    ).show()
                    return@OnClickListener
                }

                // Check if this is not a duplicate of an existing name
                if (compSectionNames(newSectName)) {
                    mesg =
                        getString(R.string.attention) + " " + newSectName + " " + getString(R.string.isdouble)
                    Toast.makeText(
                        this,
                        HtmlCompat.fromHtml(
                            "<font color='red'><b>$mesg</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                        ), Toast.LENGTH_LONG
                    ).show()
                    return@OnClickListener
                }

                // Check if section is contiguous
                var entries = -1
                try {
                    entries = sectionDataSource!!.numEntries
                } catch (e: Exception) {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.e(TAG, "254 getNumEntries failed, $e")
                }

                try {
                    maxId = sectionDataSource!!.maxId
                } catch (e: Exception) {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.e(TAG, "261 getMaxId failed, $e")
                }

                if (entries != maxId) {
                    mesg = getString(R.string.notContiguous)
                    Toast.makeText(
                        this,
                        HtmlCompat.fromHtml(
                            "<font color='red'><b>$mesg</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                        ), Toast.LENGTH_LONG
                    ).show()
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.d(TAG, "274 maxId: $maxId, entries: $entries")
                    return@OnClickListener
                }

                // Creating the new section
                val newSection = sectionDataSource!!.createSection(newSectName)
                sectionDataSource!!.saveSection(newSection)
                for (c in countDataSource!!.getAllCountsForSection(1)) {
                    val newCount =
                        countDataSource!!.createCount(newSection.id, c.name, c.code, c.name_g)
                    if (newCount != null) {
                        countDataSource!!.saveCount(newCount)
                    }
                }

                // Exit this and go to the list of new sections
                mesg = newSectName + " " + getString(R.string.newCopyCreated)
                Toast.makeText(
                    this,
                    HtmlCompat.fromHtml(
                        "<font color='#008000'>$mesg</font>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    ), Toast.LENGTH_SHORT
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

        val childcount = sectionList.size + 1

        // For all sections
        for (i in 1..<childcount) {
            section = sectionDataSource!!.getSection(i)
            selSectName = section!!.name
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "332, compSectionNames, selSectName = $selSectName")

            if (newName == selSectName) {
                isDblName = true
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.d(TAG, "337, compSectionNames, Double name = $selSectName")
                break
            }
        }
        return isDblName
    }

    // Delete last section with respective alerts and track points
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
