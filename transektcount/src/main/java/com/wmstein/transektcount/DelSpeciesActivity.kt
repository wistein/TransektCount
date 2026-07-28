package com.wmstein.transektcount

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

import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.widgets.DeleteSpeciesWidget
import com.wmstein.transektcount.widgets.DeleteSpeciesHintWidget

import java.util.Locale

/********************************************************************
 * DelSpeciesActivity lets you delete species from the species lists.
 * It is called from CountingActivity.
 * Uses DelSpeciesWidget.kt, EditSectionListTitleWidget.kt,
 * activity_del_species.xml, widget_edit_title.xml.
 *
 * Based on EditSectionListActivity.kt.
 * Created on 2024-07-27 by wmstein,
 * last edited on 2025-07-21.
 */
class DelSpeciesActivity : AppCompatActivity() {
    // Data
    private var sectionId = 1
    private var specCode = ""
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null

    // Layouts
    private var delHintArea: LinearLayout? = null
    private var deleteArea: LinearLayout? = null

    // 2 or more characters to limit selection
    private var searchChars = ""

    // Arraylists
    private var listToDelete: ArrayList<DeleteSpeciesWidget>? = null

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false
    private var awakePref = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "67 onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        setContentView(R.layout.activity_del_species)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.delSpec))
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

        //  Note variables to restore them
        val extras = intent.extras
        if (extras != null) {
            sectionId = extras.getInt("section_id")
            searchChars = extras.getString("search_Chars").toString()
        }

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "102 onCreate, searchChars: $searchChars")

        listToDelete = ArrayList()

        deleteArea = findViewById(R.id.deleteSpecLayout)
        delHintArea = findViewById(R.id.showHintDelLayout)

        // Set up the data sources
        sectionDataSource = SectionDataSource(this)
        countDataSource = CountDataSource(this)

        // New onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = NavUtils.getParentActivityIntent(this@DelSpeciesActivity)!!
                intent.putExtra("section_id", sectionId)
                intent.flags = FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                this@DelSpeciesActivity.navigateUpTo(intent)
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "130 onResume")

        // Load preference
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
        countDataSource!!.open()

        // Clear any existing views
        deleteArea!!.removeAllViews()
        delHintArea!!.removeAllViews()

        supportActionBar!!.setTitle(R.string.delSpeciesTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Display hint: Species in counting list
        val hdw = DeleteSpeciesHintWidget(this, null)
        if (searchChars.length >= 2) {
            hdw.setSearchD1(searchChars)
        }
        else
            hdw.setSearchD(getString(R.string.hintSearch))
        delHintArea!!.addView(hdw)

        constructDelList()
    }
    // End of onResume()

    // Get 2 or more characters of species to select by search button
    // Parameter view is necessary for function call
    fun getDelSearchChars(view: View) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "173, getDelSearchChars, View: $view")

        // Read EditText searchDel from widget_del_hint.xml
        val searchDel: EditText = findViewById(R.id.searchD)
        searchDel.findFocus()

        // Get the search characters to build a species list to select from
        searchChars = searchDel.text.toString().trim()
        if (searchChars.length == 1) {
            // Reminder: "Please, >1 characters"
            searchDel.error = getString(R.string.searchCharsL)
        } else {
            searchDel.error = null
            searchDel.clearFocus()
            searchDel.invalidate()

            // Re-enter DelSpeciesActivity for the reduced species delete-list
            val intent = Intent(this@DelSpeciesActivity, DelSpeciesActivity::class.java)
            intent.putExtra("section_id", sectionId)
            intent.putExtra("search_Chars", searchChars)
            intent.flags = FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    // Construct a delete-species-list of contained species in the current counting list
    //   and optionally reduce it by searchChars selection
    private fun constructDelList() {
        // Load the sorted species data from section 1
        val countsCodesSortList = countDataSource!!.getAllSpeciesForSectionSrtCode(1)

        // Get the counting list species into their DeleteSpeciesWidget and add these to the view
        if (searchChars.length >= 2) {
            searchChars = searchChars.uppercase(Locale.getDefault()) //Compare searchChars in uppercase
            var cnt = 1
            // Check name in countsCodesSortList for searchChars to reduce list
            for (count in countsCodesSortList) {
                val checkedName = count.name.uppercase(Locale.getDefault()).contains(searchChars)
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "213, checkedName: $checkedName")
                if (checkedName) {
                    val dsw = DeleteSpeciesWidget(this, null)
                    dsw.setSpecName(count.name)
                    dsw.setSpecNameG(count.name_g)
                    dsw.setSpecCode(count.code)
                    dsw.setPicSpec(count)
                    dsw.setSpecId(cnt.toString()) // Index in reduced list
                    deleteArea!!.addView(dsw)
                    cnt++
                }
            }
        } else {
            for (count in countsCodesSortList) {
                val dsw = DeleteSpeciesWidget(this, null)
                dsw.setSpecName(count.name)
                dsw.setSpecNameG(count.name_g)
                dsw.setSpecCode(count.code)
                dsw.setPicSpec(count)
                dsw.setSpecId(count.id.toString()) // Index in complete list
                deleteArea!!.addView(dsw)
            }
        }
    }

    // Mark the selected species and consider it for delete from the species countsCodesSortList list
    fun checkBoxDel(view: View) {
        val idToDel = view.tag as Int
        val dsw = deleteArea!!.getChildAt(idToDel) as DeleteSpeciesWidget

        val checked = dsw.getMarkSpec() // return boolean isChecked

        // Put species on delete list
        if (checked) {
            listToDelete!!.add(dsw)
        } else {
            // Remove species previously added from delete list
            listToDelete!!.remove(dsw)
        }
    }

    // Delete selected species from species lists of all sections
    private fun delSpecs() {
        var i = 0 // index of species list to delete
        val numSect: Int = sectionDataSource!!.numEntries
        while (i < listToDelete!!.size) {
            specCode = listToDelete!![i].getSpecCode()
            try {
                var sectid = 1
                while (sectid <= numSect) {
                    countDataSource!!.deleteAllCountsWithCode(specCode)
                    sectid++
                }
            } catch (_: Exception) {
                // nothing
            }
            i++
        }

        // Re-index and sort counts table
        countDataSource!!.sortCounts()

        // Rebuild the species list
        val countsCodesSortList = countDataSource!!.getAllSpeciesForSectionSrtCode(1)

        delHintArea!!.removeAllViews()
        val hdw = DeleteSpeciesHintWidget(this, null)
        hdw.setSearchD(getString(R.string.hintSearch))
        delHintArea!!.addView(hdw)

        deleteArea!!.removeAllViews()
        for (count in countsCodesSortList) {
            val dsw = DeleteSpeciesWidget(this, null)
            dsw.setSpecName(count.name)
            dsw.setSpecNameG(count.name_g)
            dsw.setSpecCode(count.code)
            dsw.setPicSpec(count)
            dsw.setSpecId(count.id.toString())
            deleteArea!!.addView(dsw)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.delete_species, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId
        if (id == android.R.id.home) {
            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.putExtra("section_id", sectionId)
            intent.flags = FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            this@DelSpeciesActivity.navigateUpTo(intent)
            return true
        } else if (id == R.id.deleteSpec) {
            if (listToDelete!!.isNotEmpty())
                delSpecs()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "322 onPause")

        sectionDataSource!!.close()
        countDataSource!!.close()

        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            deleteArea!!.clearFocus()
            deleteArea!!.removeAllViews()
            delHintArea!!.clearFocus()
            delHintArea!!.removeAllViews()
        }
    }

    override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "341, onStop")

        deleteArea = null
        delHintArea = null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "351, onDestroy")
    }

    companion object {
        private const val TAG = "DelSpecAct"
    }

}
