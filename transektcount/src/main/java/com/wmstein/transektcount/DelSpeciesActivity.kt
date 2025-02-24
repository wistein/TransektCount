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
import com.wmstein.transektcount.database.AlertDataSource
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.widgets.DeleteSpeciesWidget
import com.wmstein.transektcount.widgets.HintDelWidget

/********************************************************************
 * DelSpeciesActivity lets you delete species from the species lists.
 * It is called from CountingActivity.
 * Uses DelSpeciesWidget.kt, EditTitleWidget.kt,
 * activity_del_species.xml, widget_edit_title.xml.
 * Based on EditSectionListActivity.kt.
 * Created on 2024-07-27 by wmstein,
 * last edited on 2024-11-25
 */
class DelSpeciesActivity : AppCompatActivity() {
    // Data
    var section: Section? = null
    private var sectionId = 1
    private var specCode: String? = null
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null
    private var alertDataSource: AlertDataSource? = null

    // Layouts
    private var delHintArea: LinearLayout? = null
    private var deleteArea: LinearLayout? = null

    // 2 initial characters to limit selection
    private var initChars: String = ""

    // Arraylists
    private var listToDelete: ArrayList<DeleteSpeciesWidget>? = null

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.DLOG) Log.d(TAG, "58 onCreate")

        // Load preference
        brightPref = prefs.getBoolean("pref_bright", true)

        setContentView(R.layout.activity_del_species)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        //  Note variables to restore them
        val extras = intent.extras
        if (extras != null) {
            sectionId = extras.getInt("section_id")
            initChars = extras.getString("init_Chars").toString()
        }

        listToDelete = ArrayList()

        delHintArea = findViewById(R.id.showHintDelLayout)
        deleteArea = findViewById(R.id.deleteSpecLayout)

        // Setup the data sources
        sectionDataSource = SectionDataSource(this)
        countDataSource = CountDataSource(this)
        alertDataSource = AlertDataSource(this)

        // New onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = NavUtils.getParentActivityIntent(this@DelSpeciesActivity)!!
                intent.putExtra("section_id", sectionId)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                this@DelSpeciesActivity.navigateUpTo(intent)
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (MyDebug.DLOG) Log.d(TAG, "106 onResume")

        sectionDataSource!!.open()
        countDataSource!!.open()
        alertDataSource!!.open()

        // Clear any existing views
        deleteArea!!.removeAllViews()
        delHintArea!!.removeAllViews()

        supportActionBar!!.setTitle(R.string.deleteSpecies)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Display hint: Species in counting list
        val hdw = HintDelWidget(this, null)
        if (initChars.length == 2)
            hdw.setSearchD(initChars)
        else
            hdw.setSearchD(getString(R.string.hintSearch))
        delHintArea!!.addView(hdw)

        constructDelList()
    }
    // End of onResume()

    // Get initial 2 characters of species to select by search button
    fun getDelInitialChars(view: View) {
        // Read EditText searchDel from widget_del_hint.xml
        val searchDel: EditText = findViewById(R.id.searchD)
        searchDel.findFocus()

        // Get the initial characters of species to select from
        initChars = searchDel.text.toString().trim()
        if (initChars.length == 1) {
            // Reminder: "Please, 2 characters"
            searchDel.error = getString(R.string.initCharsL)
        } else {
            searchDel.error = null

            if (MyDebug.DLOG) Log.d(TAG, "145, initChars: $initChars")

            // Call DummyActivity to reenter DelSpeciesActivity for reduced add list
            val intent = Intent(this@DelSpeciesActivity, DummyActivity::class.java)
            intent.putExtra("section_id", sectionId)
            intent.putExtra("init_Chars", initChars)
            intent.putExtra("is_Flag", "isDel")
            startActivity(intent)
        }
    }

    // Construct delete-species-list of contained species in the counting list
    //   and optionally reduce it by initChar selection
    private fun constructDelList() {
        // Load the sorted species data from section 1
        val counts = countDataSource!!.getAllSpeciesForSectionSrtCode(1)

        // Get the counting list species into their DeleteSpeciesWidget and add these to the view
        if (initChars.length == 2) {
            // Check name in counts for InitChars to reduce list
            var cnt = 1
            for (count in counts) {
                if (count.name?.substring(0, 2) == initChars) {
                    val dsw = DeleteSpeciesWidget(this, null)
                    dsw.setSpecName(count.name)
                    dsw.setSpecNameG(count.name_g)
                    dsw.setSpecCode(count.code)
                    dsw.setPicSpec(count)
                    dsw.setSpecId(cnt.toString()) // Index in reduced list
                    cnt++
                    deleteArea!!.addView(dsw)
                    if (MyDebug.DLOG) Log.d(TAG, "176, name: " + count.name)
                }
            }
        } else {
            for (count in counts) {
                val dsw = DeleteSpeciesWidget(this, null)
                dsw.setSpecName(count.name)
                dsw.setSpecNameG(count.name_g)
                dsw.setSpecCode(count.code)
                dsw.setPicSpec(count)
                dsw.setSpecId(count.id.toString()) // Index in complete list
                deleteArea!!.addView(dsw)
                if (MyDebug.DLOG) Log.d(TAG, "188, name: " + count.name)
            }
        }
    }

    // Mark the selected species and consider it for delete from the species counts list
    fun checkBoxDel(view: View) {
        val idToDel = view.tag as Int
        if (MyDebug.DLOG) Log.d(TAG, "196, View.tag: $idToDel")
        val dsw = deleteArea!!.getChildAt(idToDel) as DeleteSpeciesWidget

        val checked = dsw.getMarkSpec() // return boolean isChecked

        // Put species on delete list
        if (checked) {
            listToDelete!!.add(dsw)
            if (MyDebug.DLOG) {
                val codeD = dsw.getSpecCode()
                Log.d(TAG, "206, mark delete code: $codeD")
            }
        } else {
            // Remove species previously added from delete list
            listToDelete!!.remove(dsw)
            if (MyDebug.DLOG) {
                val codeD = dsw.getSpecCode()
                Log.d(TAG, "213, mark delete code: $codeD")
            }
        }
    }

    // Delete selected species from species lists of all sections
    private fun delSpecs() {
        var i = 0 // index of species list to delete
        val numSect: Int = sectionDataSource!!.numEntries
        while (i < listToDelete!!.size) {
            specCode = listToDelete!![i].getSpecCode()
            if (MyDebug.DLOG) {
                Log.d(TAG, "225, delete code: $specCode")
            }
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

        // Delete all alerts
        alertDataSource!!.deleteAlerts()

        // Re-index and sort counts table
        countDataSource!!.sortCounts()

        // Rebuild the species list
        val counts = countDataSource!!.getAllSpeciesForSectionSrtCode(1)

        delHintArea!!.removeAllViews()
        val hdw = HintDelWidget(this, null)
        hdw.setSearchD(getString(R.string.hintSearch))
        delHintArea!!.addView(hdw)

        deleteArea!!.removeAllViews()
        for (count in counts) {
            val dsw = DeleteSpeciesWidget(this, null)
            dsw.setSpecName(count.name)
            dsw.setSpecNameG(count.name_g)
            dsw.setSpecCode(count.code)
            dsw.setPicSpec(count)
            dsw.setSpecId(count.id.toString())
            deleteArea!!.addView(dsw)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("section_id", sectionId)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        if (savedInstanceState.getInt("section_id") != 0)
            sectionId = savedInstanceState.getInt("section_id")
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.delete_species, menu)
        return true
    }

    @SuppressLint("ApplySharedPref")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId
        if (id == android.R.id.home) {
            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            this@DelSpeciesActivity.navigateUpTo(intent)
            return true
        } else if (id == R.id.deleteSpec) {
            if (listToDelete!!.size > 0)
                delSpecs()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (MyDebug.DLOG) Log.d(TAG, "302 onPause")

        sectionDataSource!!.close()
        countDataSource!!.close()
        alertDataSource!!.close()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (MyDebug.DLOG) Log.d(AddSpeciesActivity.Companion.TAG, "312, onDestroy")

        delHintArea!!.clearFocus()
    }

    companion object {
        private const val TAG = "DelSpecAct"
    }

}
