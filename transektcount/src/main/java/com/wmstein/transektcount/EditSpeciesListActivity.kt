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
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Build
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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.google.android.material.snackbar.Snackbar
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.widgets.CountEditWidget
import com.wmstein.transektcount.widgets.EditTitleWidget
import com.wmstein.transektcount.widgets.HintWidget
import java.util.Objects

/**********************************************************************************
 * EditSectionActivity lets you edit the section lists (change, delete and insert
 * new species for all sections).
 * EditSectionActivity is called from ListSectionActivity, NewSectionActivity
 * or CountingActivity.
 * Uses CountEditWidget.java, EditTitleWidget.java,
 * activity_edit_section.xml, widget_edit_title.xml, widget_edit_notes.xml.
 * Based on EditProjectActivity.java by milo on 05/05/2014.
 * Changed by wmstein since 2016-02-16,
 * last edited in Java on 2023-07-07,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2024-06-27
 */
class EditSectionActivity : AppCompatActivity() {
    private var transektCount: TransektCountApplication? = null

    // the actual data
    var section: Section? = null
    private var sectionBackup: Section? = null
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null

    private var editingCountsArea: LinearLayout? = null
    private var speciesNotesArea: LinearLayout? = null
    private var hintArea: LinearLayout? = null
    private var etw: EditTitleWidget? = null
    private var cew: CountEditWidget? = null
    private var viewMarkedForDelete: View? = null
    private var idToDelete = 0
    private var areYouSure: AlertDialog.Builder? = null
    private var countNames: ArrayList<String>? = null
    private var countNamesG: ArrayList<String>? = null
    private var countCodes: ArrayList<String>? = null
    private var cmpCountNames: ArrayList<String>? = null
    private var cmpCountCodes: ArrayList<String>? = null
    private var countIds: ArrayList<Int>? = null
    private var savedCounts: ArrayList<CountEditWidget>? = null
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null
    private var sectionId = 1
    private var hasDeleted = false // marker: has deleted a species (count)
    private var hasEdited = false  // marker: has edited a species (count)
    private var hasAdded = false   // marker: has added a species (count)

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false
    private var sortPref: String? = null
    private var oldName: String? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transektCount = application as TransektCountApplication

        // Load preference
        brightPref = prefs.getBoolean("pref_bright", true)
        sortPref = prefs.getString("pref_sort_sp", "none")

        if (MyDebug.LOG) Log.d(TAG, "97, onCreate")

        setContentView(R.layout.activity_edit_section)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        val editor = prefs.edit()
        editor.putBoolean("add_spec", false) // species not yet added
        editor.apply()

        countNames = ArrayList()
        countNamesG = ArrayList()
        countCodes = ArrayList()
        countIds = ArrayList()
        savedCounts = ArrayList()

        speciesNotesArea = findViewById(R.id.editingNotesLayout)
        hintArea = findViewById(R.id.showHintLayout)
        editingCountsArea = findViewById(R.id.editingCountsLayout)

        // Restore any edit widgets the user has added previously and the section id
        if (savedInstanceState != null) {
            if (Build.VERSION.SDK_INT < 33) {
                @Suppress("DEPRECATION")
                if (savedInstanceState.getSerializable("savedCounts") != null) {
                    @Suppress("UNCHECKED_CAST")
                    savedCounts = savedInstanceState.getSerializable("savedCounts")
                            as ArrayList<CountEditWidget>?
                }
            } else {
                if (savedInstanceState.getSerializable("savedCounts", T::class.java) != null) {
                    @Suppress("UNCHECKED_CAST")
                    savedCounts = savedInstanceState.getSerializable("savedCounts",
                        T::class.java) as ArrayList<CountEditWidget>?
                }
            }
        }

        val countingScreen = findViewById<LinearLayout>(R.id.editSect)
        bMap = transektCount!!.decodeBitmap(
            R.drawable.kbackground,
            transektCount!!.width,
            transektCount!!.height
        )
        bg = BitmapDrawable(countingScreen.resources, bMap)
        countingScreen.background = bg

        val extras = intent.extras
        if (extras != null) {
            sectionId = extras.getInt("section_id")
        }

        // setup the data sources
        sectionDataSource = TransektCountApplication.getSectionDS()
        countDataSource = TransektCountApplication.getCountDS()

        // new onBackPressed logic
        if (Build.VERSION.SDK_INT >= 33) {
            onBackPressedDispatcher.addCallback(object :
                OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    NavUtils.navigateUpFromSameTask(this@EditSectionActivity)
                }
            })
        }
    }
    // end of onCreate

    override fun onResume() {
        super.onResume()

        // Load preferences
        prefs = TransektCountApplication.getPrefs()
        brightPref = prefs.getBoolean("pref_bright", true)
        hasAdded = prefs.getBoolean("add_spec", false)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        // build the Edit Section screen
        editingCountsArea!!.removeAllViews()
        speciesNotesArea!!.removeAllViews()
        hintArea!!.removeAllViews()

        countDataSource!!.open()
        sectionDataSource!!.open()

        // load the sections data
        section = sectionDataSource!!.getSection(sectionId)
        oldName = section!!.name
        try {
            supportActionBar!!.title = oldName
        } catch (e: NullPointerException) {
            if (MyDebug.LOG)
                Log.e(TAG, "204, NullPointerException: No section name!")
        }

        // edit the section name
        etw = EditTitleWidget(this, null)
        etw!!.sectionName = oldName
        etw!!.setWidgetTitle(getString(R.string.titleEdit))
        speciesNotesArea!!.addView(etw)
        if (MyDebug.LOG)
            Log.d(TAG, "213, onResume, EditTitleWidget, old section name: " + oldName
                        + ", new sectionName: " + etw!!.sectionName
            )

        // display hint current species list:
        val nw = HintWidget(this, null)
        nw.setHint1(getString(R.string.presentSpecs))
        hintArea!!.addView(nw)

        // load the sorted species data from section 1
        val counts = when (Objects.requireNonNull(sortPref)) {
            "names_alpha" ->
                countDataSource!!.getAllSpeciesForSectionSrtName(1)

            "codes" ->
                countDataSource!!.getAllSpeciesForSectionSrtCode(1)

            else -> countDataSource!!.getAllCountsForSection(1)
        }

        // get all the species into their CountEditWidgets and add these to the view
        for (count in counts) {
            // widget
            cew = CountEditWidget(this, null)
            cew!!.setCountName(count.name)
            cew!!.setCountNameG(count.name_g)
            cew!!.setCountCode(count.code)
            cew!!.setPSpec(count)
            cew!!.setCountId(count.id)
            editingCountsArea!!.addView(cew)
        }

        // if savedInstanceState != add all counting widgets from savedInstanceState to the view
        for (cew in savedCounts!!) {
            editingCountsArea!!.addView(cew)
        }

        if (getEditedSpecies()) {
            hasEdited = true
        }
    }
    // end of onResume

    override fun onPause() {
        super.onPause()

        // close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Widgets must be removed from their parent before they can be serialised,
        // else they cause a crash.
        for (cew in savedCounts!!) {
            (cew.parent as ViewGroup).removeView(cew)
        }
        outState.putSerializable("savedCounts", savedCounts)
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
            savedCounts!!.clear()
            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.putExtra("add_species", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NavUtils.navigateUpTo(this, intent)
        }
        else if (id == R.id.menuSaveExit) {
            if (hasDeleted && !hasEdited) {
                if (MyDebug.LOG) Log.d(TAG, "296, hasDeleted && !hasEdited")
                savedCounts!!.clear()
                super.finish()
            } else if (hasAdded && !hasEdited) {
                if (MyDebug.LOG) Log.d(TAG, "300, hasAdded && !hasEdited")
                savedCounts!!.clear()
                super.finish()
            } else if (hasDeleted && hasAdded) {
                if (MyDebug.LOG) Log.d(TAG, "304, hasDeleted && hasAdded")
                savedCounts!!.clear()
                super.finish()
            } else if (hasEdited && !hasDeleted) {
                if (MyDebug.LOG) Log.d(TAG, "308, hasEdited && !hasDeleted")
                if (saveData()) {
                    savedCounts!!.clear()
                    super.finish()
                }
            } else {
                if (MyDebug.LOG) Log.d(TAG, "314, hasDeleted && hasAdded && hasEdited")
                showSnackbarRed(getString(R.string.isfail))
                savedCounts!!.clear()
                super.finish()
            }
        }
        else if (id == R.id.newCount) {
            // a Snackbar here comes incomplete
            Toast.makeText(applicationContext, getString(R.string.wait), Toast.LENGTH_SHORT)
                .show()

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

    fun doneAndExit(view: View) {
        if (hasDeleted && !hasEdited) {
            if (MyDebug.LOG) Log.d(TAG, "351, hasDeleted && !hasEdited")
            savedCounts!!.clear()
            super.finish()
        } else if (hasAdded && !hasEdited) {
            if (MyDebug.LOG) Log.d(TAG, "355, hasAdded && !hasEdited")
            savedCounts!!.clear()
            super.finish()
        } else if (hasDeleted && hasAdded) {
            if (MyDebug.LOG) Log.d(TAG, "359, hasDeleted && hasAdded")
            savedCounts!!.clear()
            super.finish()
        } else if (hasEdited && !hasDeleted) {
            if (MyDebug.LOG) Log.d(TAG, "363, hasEdited && !hasDeleted")
            if (saveData()) {
                savedCounts!!.clear()
                super.finish()
            }
        } else {
            if (MyDebug.LOG) Log.d(TAG, "369, hasDeleted && hasAdded && hasEdited")
            showSnackbarRed(getString(R.string.isfail))
            savedCounts!!.clear()
            super.finish()
        }
    }

    private fun getEditedSpecies(): Boolean {
        // read edited species list
        var retValue = false
        val childcount: Int = editingCountsArea!!.childCount //No. of counts in list
        if (MyDebug.LOG) Log.d(TAG, "380, childcount: $childcount")

        // check for unique species names and codes before storing
        val isDblName: String = compCountNames()
        val isDblCode: String = compCountCodes()

        // updates species names and code for all sections if no doubles
        if (isDblName == "" && isDblCode == "") {
            // copy edited species list for all other sections
            val numSect: Int = sectionDataSource!!.numEntries
            var si = 1   // section id
            var ci = 1   // count id
            var cname: String
            var ccode: String
            var cnameg: String

            // for all sections
            while (si <= numSect) {
                // for all species per section
                for (i in 0 until childcount) {
                    if (MyDebug.LOG) Log.d(TAG, "400, Section: $si, Species $i")
                    cew = editingCountsArea!!.getChildAt(i) as CountEditWidget
                    cname = cew!!.getCountName()
                    ccode = cew!!.getCountCode()
                    if (isNotEmpty(cname) && isNotEmpty(ccode)) {
                        cnameg = cew!!.getCountNameG()
                        countDataSource!!.updateCountForAllSections(
                            si,
                            ci,
                            cname,
                            ccode,
                            cnameg
                        )
                        ci++
                        retValue = true
                    } else {
                        showSnackbarRed(getString(R.string.isempt))
                        retValue = false
                        break
                    }
                }
                if (!retValue)
                    break
                si++
            }
        } else {
            showSnackbarRed(
                getString(R.string.spname) + " " + isDblName + " " + getString(R.string.orcode) + " " + isDblCode + " " + getString(R.string.isdouble))
            retValue = false
        }
        if (MyDebug.LOG) Log.d(TAG, "430, getEditSpecies, retValue; $retValue")
        return retValue
    }

    // test for double entries and save species lists
    private fun saveData(): Boolean {
        // save section name only if it has changed
        val saveSectionState: Boolean
        val newName = etw!!.sectionName // edited section name
        if (MyDebug.LOG) Log.d(TAG, "439, newName: $newName")

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
            section = sectionBackup // save edited section with new name

        } else {
            showSnackbarRed(getString(R.string.isempty))
            saveSectionState = false
        }

        if (saveSectionState) {
            // save changed section name
            sectionDataSource!!.saveSection(section!!)
            // Toast here, as snackbar doesn't show up
            Toast.makeText(this@EditSectionActivity, getString(R.string.sectSaving),
                Toast.LENGTH_SHORT).show()
        }
        return saveSectionState
    }

    // Start AddSpeciesActivity to add a new species to the species list
    @SuppressLint("ApplySharedPref")
    fun newCount(view: View?) {
        // a Snackbar here comes incomplete
        Toast.makeText(applicationContext, getString(R.string.wait), Toast.LENGTH_SHORT)
            .show()

        // Trick: Pause for 100 msec to show toast
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this@EditSectionActivity, AddSpeciesActivity::class.java)
            intent.putExtra("section_id", sectionId)
            startActivity(intent)
        }, 100)
    }

    // purging a species from all section lists (with associated alerts)
    // called by ImageButton in widget_edit_count.xml
    fun deleteCount(view: View) {
        /*
         * These global variables keep a track of the view containing an alert to be deleted and also the id
         * of the alert itself, to make sure that they're available inside the code for the alert dialog by
         * which they will be deleted.
         */
        viewMarkedForDelete = view
        idToDelete = view.tag as Int
        val specCode: String? = countDataSource?.getCodeById(idToDelete)

        // the actual CountEditWidget is 3 levels up from the button in which it is embedded
        if (idToDelete == 0) {
            // nothing to delete
            editingCountsArea!!.removeView(view.parent.parent.parent as CountEditWidget)
        } else {
            // Before removing this widget it is necessary to do the following:
            //   (1) Check the user is sure to delete it and, if so...
            //   (2) Delete the associated alert from the database.
            areYouSure = AlertDialog.Builder(this)
            areYouSure!!.setTitle(getString(R.string.deleteCount))
            areYouSure!!.setMessage(getString(R.string.reallyDeleteCount))
            areYouSure!!.setPositiveButton(R.string.yesDeleteIt) {   _: DialogInterface?, _: Int ->
                // go ahead for the delete
                countDataSource!!.deleteAllCountsWithCode(specCode) // includes associated alerts
                editingCountsArea!!.removeView(viewMarkedForDelete!!.parent.parent.parent as CountEditWidget)
//                editingCountsArea!!.removeAllViews()

                // load the sorted species data from section 1
                val counts = when (sortPref) {
                    "names_alpha" -> countDataSource!!.getAllSpeciesForSectionSrtName(1)

                    "codes" -> countDataSource!!.getAllSpeciesForSectionSrtCode(1)

                    else -> countDataSource!!.getAllCountsForSection(1)
                }

                // get all the species into their CountEditWidgets and add these to the view
                for (count in counts) {
                    // widget
                    cew = CountEditWidget(this, null)
                    cew!!.setCountName(count.name)
                    cew!!.setCountNameG(count.name_g)
                    cew!!.setCountCode(count.code)
                    cew!!.setPSpec(count)
                    cew!!.setCountId(count.id)
                    editingCountsArea!!.addView(cew)
                }
            }
            areYouSure!!.setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int -> }
            areYouSure!!.show()
        }
        hasDeleted = true
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
                Log.d(TAG, "559, sname = $sname")
            if (newName == sname) {
                isDblName = true
                if (MyDebug.LOG)
                    Log.d(TAG, "563, Double name = $sname")
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
        val childcount = editingCountsArea!!.childCount

        // for all CountEditWidgets
        for (i in 0 until childcount) {
            cew = editingCountsArea!!.getChildAt(i) as CountEditWidget
            name = cew!!.getCountName()
            if (cmpCountNames!!.contains(name)) {
                isDblName = name
                if (MyDebug.LOG) Log.d(TAG, "583, Double name = $isDblName")
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
            cew = editingCountsArea!!.getChildAt(i) as CountEditWidget
            code = cew!!.getCountCode()
            if (cmpCountCodes!!.contains(code)) {
                isDblCode = code
                if (MyDebug.LOG) Log.d(TAG, "604, Double name = $isDblCode")
                break
            }
            cmpCountCodes!!.add(code)
        }
        return isDblCode
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("ApplySharedPref", "MissingSuperCall")
    override fun onBackPressed() {
        // close the data sources
        countDataSource!!.close()
        sectionDataSource!!.close()

        NavUtils.navigateUpFromSameTask(this)
        @Suppress("DEPRECATION")
        super.onBackPressed()
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
        private const val TAG = "EditSectAct"

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
            return cs.isNullOrEmpty()
        }
    }

}
