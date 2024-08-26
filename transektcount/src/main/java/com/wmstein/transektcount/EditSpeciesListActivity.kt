package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Build
import android.os.Bundle
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
import com.wmstein.transektcount.widgets.EditSpeciesWidget
import com.wmstein.transektcount.widgets.EditTitleWidget
import com.wmstein.transektcount.widgets.HintWidget
import java.util.Objects

/********************************************************************************
 * EditSpeciesListActivity lets you edit the current section name and the species
 * lists (change new species for all sections).
 * EditSpeciesListActivity is called from ListSectionActivity or CountingActivity.
 * Uses EditSpeciesWidget.kt, EditTitleWidget.kt,
 * activity_edit_species_list.xml, widget_edit_title.xml.
 * Based on EditProjectActivity.java by milo on 05/05/2014.
 * Changed by wmstein since 2016-02-16,
 * last edited in Java on 2023-07-07,
 * converted to Kotlin on 2023-07-17,
 * renamed from EditSectionActivity on 2024-07-03,
 * last edited on 2024-08-21
 */
class EditSpeciesListActivity : AppCompatActivity() {
    private var transektCount: TransektCountApplication? = null

    // Screen background
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null

    // Data
    var section: Section? = null
    private var sectionId = 1
    private var sectionBackup: Section? = null
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null

    // Layouts
    private var editingSpeciesArea: LinearLayout? = null
    private var speciesNotesArea: LinearLayout? = null
    private var hintArea: LinearLayout? = null

    // Widgets
    private var etw: EditTitleWidget? = null

    // Arraylists
    private var cmpCountNames: ArrayList<String>? = null
    private var cmpCountCodes: ArrayList<String>? = null
    private var savedCounts: ArrayList<EditSpeciesWidget>? = null

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

        if (MyDebug.LOG) Log.d(TAG, "91 onCreate")

        setContentView(R.layout.activity_edit_species_list)
        val editSpecListScreen = findViewById<LinearLayout>(R.id.editSpecList)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        savedCounts = ArrayList()

        speciesNotesArea = findViewById(R.id.editingLineLayout)
        hintArea = findViewById(R.id.showHintLayout)
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
                    savedCounts = savedInstanceState.getSerializable("savedCounts",
                        T::class.java) as ArrayList<EditSpeciesWidget>?
                }
            }
        }

        bMap = transektCount!!.decodeBitmap(
            R.drawable.edbackground,
            transektCount!!.width, transektCount!!.height
        )
        bg = BitmapDrawable(editSpecListScreen.resources, bMap)
        editSpecListScreen.background = bg

        //  note the section id to restore it in CountingActivity
        val extras = intent.extras
        if (extras != null) {
            sectionId = extras.getInt("section_id")
        }

        // Setup the data sources
        sectionDataSource = TransektCountApplication.getSectionDS()
        countDataSource = TransektCountApplication.getCountDS()

        // New onBackPressed logic
        if (Build.VERSION.SDK_INT >= 33) {
            onBackPressedDispatcher.addCallback(object :
                OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (testData()) {
                        saveData()
                        savedCounts!!.clear()
                        countDataSource!!.close()
                        sectionDataSource!!.close()

                        val intent = NavUtils.getParentActivityIntent(this@EditSpeciesListActivity)!!
                        intent.putExtra("section_id", sectionId)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        NavUtils.navigateUpTo(this@EditSpeciesListActivity, intent)
                    } else return
                }
            })
        }
    }
    // End of onCreate

    override fun onResume() {
        super.onResume()

        // Load preferences
        prefs = TransektCountApplication.getPrefs()
        brightPref = prefs.getBoolean("pref_bright", true)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        // Build the Edit Section screen
        editingSpeciesArea!!.removeAllViews()
        speciesNotesArea!!.removeAllViews()
        hintArea!!.removeAllViews()

        countDataSource!!.open()
        sectionDataSource!!.open()

        // Load the sections data
        section = sectionDataSource!!.getSection(sectionId)
        oldName = section!!.name
        try {
            supportActionBar!!.title = getString(R.string.headEdit)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        } catch (e: NullPointerException) {
            if (MyDebug.LOG)
                Log.e(TAG, "199, NullPointerException: No section name!")
        }

        // Edit the section name
        etw = EditTitleWidget(this, null)
        etw!!.sectionName = oldName
        etw!!.setWidgetTitle(getString(R.string.titleEdit))
        speciesNotesArea!!.addView(etw)
        if (MyDebug.LOG)
            Log.d(TAG, "208, onResume, EditTitleWidget, old section name: " + oldName
                        + ", new sectionName: " + etw!!.sectionName
            )

        // Display hint current species list:
        val nw = HintWidget(this, null)
        nw.setHint1(getString(R.string.presentSpecs))
        hintArea!!.addView(nw)

        // Load the sorted species data from section 1
        val counts = when (Objects.requireNonNull(sortPref)) {
            "names_alpha" -> countDataSource!!.getAllSpeciesForSectionSrtName(1)
            "codes" -> countDataSource!!.getAllSpeciesForSectionSrtCode(1)
            else -> countDataSource!!.getAllCountsForSection(1)
        }

        // Display all the counts by adding them to CountEditWidget
        for (count in counts) {
            val cew = EditSpeciesWidget(this, null)
            cew.setCountName(count.name)
            cew.setCountNameG(count.name_g)
            cew.setCountCode(count.code)
            cew.setPSpec(count)
            cew.setCountId(count.id)
            editingSpeciesArea!!.addView(cew)
            if (MyDebug.LOG) Log.d(TAG, "233, name: " + count.name)
        }
    }
    // End of onResume

    override fun onPause() {
        super.onPause()

        // Close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Widgets must be removed from their parent before they can be serialised,
        for (cew in savedCounts!!) {
            (cew.parent as ViewGroup).removeView(cew)
        }
        outState.putSerializable("savedCounts", savedCounts)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.edit_section, menu)
        return true
    }

    @SuppressLint("ApplySharedPref")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        if (!testData()) return true

        val id = item.itemId
        if (id == android.R.id.home) {
            if (saveData()) {
                savedCounts!!.clear()

                val intent = NavUtils.getParentActivityIntent(this)!!
                intent.putExtra("section_id", sectionId)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                NavUtils.navigateUpTo(this, intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // Test for double entries and save species lists
    private fun saveData(): Boolean {
        // Save section name only if it has changed
        var saveState: Boolean

        // Add title if the user has written one
        val newSectName = etw!!.sectionName // edited section name
        if (MyDebug.LOG) Log.d(TAG, "288, newSectName: $newSectName")

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
            Toast.makeText(this@EditSpeciesListActivity, getString(R.string.sectSaving),
                Toast.LENGTH_SHORT).show()
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
        if (MyDebug.LOG) Log.d(TAG, "327, childcount: $childcount")

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
                var cew: EditSpeciesWidget
                // for all species per section
                for (i in 0 until childcount) {
                    if (MyDebug.LOG) Log.d(TAG, "348, Section: $si, Species $i")
                    cew = editingSpeciesArea!!.getChildAt(i) as EditSpeciesWidget
                    cname = cew.getCountName()
                    ccode = cew.getCountCode()
                    if (isNotEmpty(cname) && isNotEmpty(ccode)) {
                        cnameg = cew.getCountNameG()
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
                getString(R.string.spname) + " " + isDblName + " " + getString(R.string.orcode) + " " + isDblCode + " " + getString(R.string.isdouble))
            correct = false
        }
        if (MyDebug.LOG) Log.d(TAG, "377, getEditSpecies, ok: $correct")
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
            if (MyDebug.LOG)
                Log.d(TAG, "412, sname = $sname")
            if (newSectName == sname) {
                isDblName = true
                if (MyDebug.LOG)
                    Log.d(TAG, "416, Double name = $sname")
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

        var cew: EditSpeciesWidget
        // For all CountEditWidgets
        for (i in 0 until childcount) {
            cew = editingSpeciesArea!!.getChildAt(i) as EditSpeciesWidget
            name = cew.getCountName()
            if (cmpCountNames!!.contains(name)) {
                isDblName = name
                if (MyDebug.LOG) Log.d(TAG, "437, Double name = $isDblName")
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

        var cew: EditSpeciesWidget
        // For all CountEditWidgets
        for (i in 0 until childcount) {
            cew = editingSpeciesArea!!.getChildAt(i) as EditSpeciesWidget
            code = cew.getCountCode()
            if (cmpCountCodes!!.contains(code)) {
                isDblCode = code
                if (MyDebug.LOG) Log.d(TAG, "459, Double name = $isDblCode")
                break
            }
            cmpCountCodes!!.add(code)
        }
        return isDblCode
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("ApplySharedPref", "MissingSuperCall")
    override fun onBackPressed() {
        if (testData()) {
            saveData()
            savedCounts!!.clear()
            countDataSource!!.close()
            sectionDataSource!!.close()

            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.putExtra("section_id", sectionId)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NavUtils.navigateUpTo(this, intent)
        } else return
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
