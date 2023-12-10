package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.google.android.material.snackbar.Snackbar
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.database.Track
import com.wmstein.transektcount.database.TrackDataSource
import com.wmstein.transektcount.widgets.CountEditWidget
import com.wmstein.transektcount.widgets.EditNotesWidget
import com.wmstein.transektcount.widgets.EditTitleWidget
import com.wmstein.transektcount.widgets.HintWidget
import java.util.Objects

/**********************************************************************************
 * Edit the section lists (change, delete and insert new species for all sections),
 * and add notes to the current section.
 * EditSectionActivity is called from ListSectionActivity, NewSectionActivity
 * or CountingActivity.
 * Uses CountEditWidget.java, EditTitleWidget.java, EditNotesWidget.java,
 * activity_edit_section.xml, widget_edit_title.xml, widget_edit_notes.xml.
 * Based on EditProjectActivity.java by milo on 05/05/2014.
 * Changed by wmstein since 2016-02-16,
 * last edited in Java on 2023-07-07,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2023-12-08
 */
class EditSectionActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private var transektCount: TransektCountApplication? = null

    // the actual data
    var section: Section? = null
    var track: Track? = null
    private var sectionBackup: Section? = null
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null
    private var trackDataSource: TrackDataSource? = null

    private var editingCountsArea: LinearLayout? = null
    private var speciesRemarkArea7: LinearLayout? = null
    private var hintArea1: LinearLayout? = null
    private var etw: EditTitleWidget? = null
    private var enw: EditNotesWidget? = null
    private var viewMarkedForDelete: View? = null
    private var idToDelete = 0
    private var areYouSure: AlertDialog.Builder? = null
    private var countNames: ArrayList<String>? = null
    private var countCodes: ArrayList<String>? = null
    private var cmpCountNames: ArrayList<String>? = null
    private var cmpCountCodes: ArrayList<String>? = null
    private var countIds: ArrayList<Int>? = null
    private var savedCounts: ArrayList<CountEditWidget>? = null
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null
    private var sectionId = 0
    private var sectionNote: String? = null

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false
    private var oldName: String? = null
    private var sectionHasTrack = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_section)
        countNames = ArrayList()
        countCodes = ArrayList()
        countIds = ArrayList()
        savedCounts = ArrayList()
        speciesRemarkArea7 = findViewById(R.id.editingNotesLayout)
        hintArea1 = findViewById(R.id.showHintLayout)
        editingCountsArea = findViewById(R.id.editingCountsLayout)

        // Restore any edit widgets the user has added previously and the section id
        if (savedInstanceState != null) {
            @Suppress("DEPRECATION")
            if (savedInstanceState.getSerializable("savedCounts") != null) {
                @Suppress("UNCHECKED_CAST")
                savedCounts =
                    savedInstanceState.getSerializable("savedCounts") as ArrayList<CountEditWidget>?
            }
        }

        // Load preferences
        transektCount = application as TransektCountApplication
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

        val extras = intent.extras
        if (extras != null) {
            sectionId = extras.getInt("section_id")
        }

        val countingScreen = findViewById<LinearLayout>(R.id.editSect)
        bMap = transektCount!!.decodeBitmap(
            R.drawable.kbackground,
            transektCount!!.width,
            transektCount!!.height
        )
        bg = BitmapDrawable(countingScreen.resources, bMap)
        countingScreen.background = bg
    }
    // end of onCreate

    @SuppressLint("LongLogTag")
    override fun onResume() {
        super.onResume()

        // Load preferences
        transektCount = application as TransektCountApplication
        prefs = TransektCountApplication.getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
        brightPref = prefs.getBoolean("pref_bright", true)
        val sortPref = prefs.getString("pref_sort_sp", "none")
        sectionId = prefs.getInt("section_id", 1)
        sectionNote = prefs.getString("section_notes", "")
        sectionHasTrack = prefs.getBoolean("section_has_track", false)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        // build the Edit Section screen
        editingCountsArea!!.removeAllViews()
        speciesRemarkArea7!!.removeAllViews()
        hintArea1!!.removeAllViews()

        // setup the data sources
        sectionDataSource = SectionDataSource(this)
        sectionDataSource!!.open()
        countDataSource = CountDataSource(this)
        countDataSource!!.open()
        trackDataSource = TrackDataSource(this)
        trackDataSource!!.open()

        // load the sections data
        section = sectionDataSource!!.getSection(sectionId)
        oldName = section!!.name
        try {
            supportActionBar!!.title = oldName
        } catch (e: NullPointerException) {
            if (MyDebug.LOG)
                Log.e(TAG, "185, NullPointerException: No section name!")
        }

        // edit the section name
        etw = EditTitleWidget(this, null)
        etw!!.sectionName = oldName
        etw!!.setWidgetTitle(getString(R.string.titleEdit))
        speciesRemarkArea7!!.addView(etw)
        if (MyDebug.LOG)
            Log.d(
                TAG, "194, onResume, EditTitleWidget, old section name: " + oldName
                        + ", new sectionName: " + etw!!.sectionName
            )

        // display editable section notes; the same class
        enw = EditNotesWidget(this, null)
        enw!!.sectionNotes = section!!.notes
        enw!!.setWidgetNotes(getString(R.string.notesHere))
        enw!!.setHint(getString(R.string.notesHint))
        speciesRemarkArea7!!.addView(enw)

        // display hint current species list:
        val nw = HintWidget(this, null)
        nw.setHint1(getString(R.string.presentSpecs))
        hintArea1!!.addView(nw)

        // load the sorted species data
        val counts = when (Objects.requireNonNull(sortPref)) {
            "names_alpha" -> countDataSource!!.getAllSpeciesForSectionSrtName(
                section!!.id
            )

            "codes" -> countDataSource!!.getAllSpeciesForSectionSrtCode(section!!.id)
            else -> countDataSource!!.getAllCountsForSection(section!!.id)
        }

        // display all the counts by adding them to CountEditWidget
        for (count in counts) {
            // widget
            val cew = CountEditWidget(this, null)
            cew.setCountName(count.name)
            cew.setCountNameG(count.name_g)
            cew.setCountCode(count.code)
            cew.setPSpec(count)
            cew.setCountId(count.id)
            editingCountsArea!!.addView(cew)
        }
        for (cew in savedCounts!!) {
            editingCountsArea!!.addView(cew)
        }
        getCountNames()
    }
    // end of onResume

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        if (isNotEmpty(savedInstanceState.getString("section_notes"))) {
            sectionNote = savedInstanceState.getString("section_notes")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        /*
         * Widgets must be removed from their parent before they can be serialised,
         * else they cause a crash.
         */
        for (cew in savedCounts!!) {
            (cew.parent as ViewGroup).removeView(cew)
        }

        outState.putSerializable("savedCounts", savedCounts)
        outState.putString("section_notes", enw!!.sectionNotes)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.edit_section, menu)
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
            intent.putExtra("add_species", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NavUtils.navigateUpTo(this, intent)
        } else if (id == R.id.menuSaveExit) {
            if (saveData()) {
                savedCounts!!.clear()
                super.finish()
            }
        } else if (id == R.id.newCount) {
            // Save edited notes first
            section!!.notes = enw!!.sectionNotes
            if (isNotEmpty(section!!.notes)) sectionDataSource!!.saveSectionNotes(
                section!!
            )

            // a Snackbar here comes incomplete
            val toast =
                Toast.makeText(applicationContext, getString(R.string.wait), Toast.LENGTH_SHORT)
            toast.show()

            // Trick: Pause for 100 msec to show toast
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this@EditSectionActivity, AddSpeciesActivity::class.java)
                intent.putExtra("section_id", sectionId)
                startActivity(intent)
            }, 100)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        val countingScreen = findViewById<LinearLayout>(R.id.editSect)
        bMap = transektCount!!.decodeBitmap(
            R.drawable.kbackground,
            transektCount!!.width,
            transektCount!!.height
        )
        countingScreen.background = null
        bg = BitmapDrawable(countingScreen.resources, bMap)
        countingScreen.background = bg
    }

    override fun onPause() {
        super.onPause()

        // close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()
        trackDataSource!!.close()
    }

    private fun getCountNames() {
        /*
         * The plan here is that names, codes and ids arrays contain the entries in the same
         * order, so I can link count and code name to its id by knowing the index.
         */
        countNames!!.clear()
        countCodes!!.clear()
        countIds!!.clear()
        val childcount = editingCountsArea!!.childCount
        for (i in 0 until childcount) {
            val cew = editingCountsArea!!.getChildAt(i) as CountEditWidget
            val name = cew.getCountName()
            val code = cew.getCountCode()
            // ignore count widgets where the user has filled nothing in. 
            // Id will be 0 if this is a new count.
            if (isNotEmpty(name)) {
                countNames!!.add(name)
                countCodes!!.add(code)
                countIds!!.add(cew.countId)
            }
        }
    }

    // Compare count names for duplicates and returns name of 1. duplicate found
    private fun compCountNames(): String {
        var name: String
        var isDblName = ""
        cmpCountNames = ArrayList()
        val childcount = editingCountsArea!!.childCount
        // for all CountEditWidgets
        for (i in 0 until childcount) {
            val cew = editingCountsArea!!.getChildAt(i) as CountEditWidget
            name = cew.getCountName()
            if (cmpCountNames!!.contains(name)) {
                isDblName = name
                if (MyDebug.LOG) Log.d(TAG, "360, Double name = $isDblName")
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
        val childcount = editingCountsArea!!.childCount
        // for all CountEditWidgets
        for (i in 0 until childcount) {
            val cew = editingCountsArea!!.getChildAt(i) as CountEditWidget
            code = cew.getCountCode()
            if (cmpCountCodes!!.contains(code)) {
                isDblCode = code
                if (MyDebug.LOG) Log.d(TAG, "380, Double name = $isDblCode")
                break
            }
            cmpCountCodes!!.add(code)
        }
        return isDblCode
    }

    fun saveAndExit(view: View?) {
        if (saveData()) {
            savedCounts!!.clear()
            super.finish()
        }
    }

    private fun saveData(): Boolean {
        // save section name only if it has changed
        val saveSectionState: Boolean
        val newName = etw!!.sectionName
        if (MyDebug.LOG) Log.d(TAG, "399, newName: $newName")
        if (isNotEmpty(newName)) {
            //check if this is not a duplicate of an existing name and
            // backup current section as compSectionNames replaces current section with last section
            sectionBackup = section
            if (compSectionNames(newName)) {
                showSnackbarRed(newName + " " + getString(R.string.isdouble))
                saveSectionState = false
            } else {
                sectionBackup!!.name = newName
                saveSectionState = true
            }
            section = sectionBackup
        } else {
            showSnackbarRed(getString(R.string.isempty))
            saveSectionState = false
        }

        // add notes if the user has written some...
        sectionNote = enw!!.sectionNotes
        if (isNotEmpty(sectionNote) && saveSectionState) {
            section!!.notes = sectionNote
        } else {
            if (isNotEmpty(section!!.notes)) {
                section!!.notes = sectionNote
            }
        }
        var retValue = false
        if (saveSectionState) {
            sectionDataSource!!.saveSection(section!!)

            // rename corresponding track names
            if (MyDebug.LOG) Log.d(TAG, "431, newName: $newName")
            if (sectionHasTrack && isNotEmpty(newName))
                trackDataSource!!.saveTrackName(newName, oldName)

            // save counts (species list)
            val isDblName: String
            val isDblCode: String
            val childcount: Int = editingCountsArea!!.childCount //No. of species in list
            if (MyDebug.LOG) Log.d(TAG, "439, childcount: $childcount")

            // check for unique species names
            isDblName = compCountNames()
            isDblCode = compCountCodes()
            if (isDblName == "" && isDblCode == "") {
                // do for all species
                for (i in 0 until childcount) {
                    val cew = editingCountsArea!!.getChildAt(i) as CountEditWidget
                    retValue =
                        if (isNotEmpty(cew.getCountName()) && isNotEmpty(cew.getCountCode())) {
                            if (MyDebug.LOG)
                                Log.d(
                                    TAG, "452, cntId: " + cew.countId + ", " + cew.getCountName()
                                )

                            // updates species name and code
                            countDataSource!!.updateCountName(
                                cew.countId,
                                cew.getCountName(),
                                cew.getCountCode(),
                                cew.getCountNameG()
                            )
                            true
                        } else {
                            showSnackbarRed(getString(R.string.isempt))
                            false
                        }
                }
            } else {
                showSnackbarRed(
                    getString(R.string.spname) + " " + isDblName + " " + getString(R.string.orcode) + " " + isDblCode + " "
                            + getString(R.string.isdouble)
                )
                // retValue = false;
            }

            if (retValue) {
                // Toast here, as snackbar doesn't show up
                Toast.makeText(
                    this@EditSectionActivity, getString(R.string.sectSaving), Toast.LENGTH_SHORT
                ).show()
            }
        }
        return retValue
    }

    // Compare section names for duplicates and return TRUE when duplicate found
    // created by wmstein on 10.04.2016
    private fun compSectionNames(newName: String?): Boolean {
        var isDblName = false
        var sname: String?
        if (newName == oldName) {
            return false // name has not changed
        }
        val sectionList = sectionDataSource!!.allSectionNames
        val childcount = sectionList.size + 1
        // for all Sections
        for (i in 1 until childcount) {
            section = sectionDataSource!!.getSection(i)
            sname = section!!.name
            if (MyDebug.LOG)
                Log.d(TAG, "502, sname = $sname")
            if (newName == sname) {
                isDblName = true
                if (MyDebug.LOG)
                    Log.d(TAG, "506, Double name = $sname")
                break
            }
        }
        return isDblName
    }

    // Start AddSpeciesActivity to add a new species to the species list
    @SuppressLint("ApplySharedPref")
    fun newCount(view: View?) {
        // Save edited section notes
        section!!.notes = enw!!.sectionNotes
        if (isNotEmpty(section!!.notes)) sectionDataSource!!.saveSectionNotes(
            section!!
        )

        // a Snackbar here comes incomplete
        val toast = Toast.makeText(applicationContext, getString(R.string.wait), Toast.LENGTH_SHORT)
        toast.show()

        // Trick: Pause for 100 msec to show toast
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this@EditSectionActivity, AddSpeciesActivity::class.java)
            intent.putExtra("section_id", sectionId)
            startActivity(intent)
        }, 100)
    }

    // purging a species from all section lists (with associated alerts)
    fun deleteCount(view: View) {
        /*
         * These global variables keep a track of the view containing an alert to be deleted and also the id
         * of the alert itself, to make sure that they're available inside the code for the alert dialog by
         * which they will be deleted.
         */
        viewMarkedForDelete = view
        idToDelete = view.tag as Int
        val specCode: String? = countDataSource?.getCodeById(idToDelete)
        if (idToDelete == 0) {
            // the actual CountEditWidget is 3 levels up from the button in which it is embedded
            editingCountsArea!!.removeView(view.parent.parent.parent as CountEditWidget)
        } else {
            // Before removing this widget it is necessary to do the following:
            //   (1) Check the user is sure they want to delete it and, if so...
            //   (2) Delete the associated alert from the database.
            areYouSure = AlertDialog.Builder(this)
            areYouSure!!.setTitle(getString(R.string.deleteCount))
            areYouSure!!.setMessage(getString(R.string.reallyDeleteCount))
            areYouSure!!.setPositiveButton(R.string.yesDeleteIt) { _: DialogInterface?, _: Int ->
                // go ahead for the delete

                countDataSource!!.deleteAllCountsWithCode(specCode) // includes associated alerts
                editingCountsArea!!.removeView(viewMarkedForDelete!!.parent.parent.parent as CountEditWidget)
            }
            areYouSure!!.setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int -> }
            areYouSure!!.show()
        }
        getCountNames()
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
        const val TAG = "EditSectAct"

        /*************************************************************************
         * Following functions are taken from the Apache commons-lang3-3.4 library
         * licensed under Apache License Version 2.0, January 2004
         *
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

        /*************************************************
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
            return cs == null || cs.length == 0
        }
    }

}