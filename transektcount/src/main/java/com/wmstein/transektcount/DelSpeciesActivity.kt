package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.wmstein.transektcount.database.AlertDataSource
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.widgets.DeleteSpeciesWidget
import com.wmstein.transektcount.widgets.HintWidget
import java.util.Objects

/********************************************************************
 * DelSpeciesActivity lets you delete species from the species lists.
 * It is called from CountingActivity.
 * Uses DelSpeciesWidget.kt, EditTitleWidget.kt,
 * activity_del_species.xml, widget_edit_title.xml.
 * Based on EditSpeciesListActivity.kt.
 * Created on 2024-07-27 by wmstein,
 * last edited on 2024-08-23
 */
class DelSpeciesActivity : AppCompatActivity() {
    private var transektCount: TransektCountApplication? = null

    // Screen background
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null

    // Data
    var section: Section? = null
    private var sectionId = 1
    private var specCode: String? = null
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null
    private var alertDataSource: AlertDataSource? = null

    // Layouts
    private var deleteArea: LinearLayout? = null
    private var hintArea: LinearLayout? = null

    // Arraylists
    private var listToDelete: ArrayList<DeleteSpeciesWidget>? = null

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false
    private var sortPref: String? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transektCount = application as TransektCountApplication

        // Load preference
        brightPref = prefs.getBoolean("pref_bright", true)
        sortPref = prefs.getString("pref_sort_sp", "none")

        if (MyDebug.LOG) Log.d(TAG, "75 onCreate")

        setContentView(R.layout.activity_del_species)
        val deleteScreen = findViewById<LinearLayout>(R.id.delSpec)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        bMap = transektCount!!.decodeBitmap(
            R.drawable.delbackground,
            transektCount!!.width,
            transektCount!!.height
        )
        bg = BitmapDrawable(deleteScreen.resources, bMap)
        deleteScreen.background = bg

        //  note the section id to restore it in CountingActivity
        val extras = intent.extras
        if (extras != null) {
            sectionId = extras.getInt("section_id")
        }

        listToDelete = ArrayList()

        hintArea = findViewById(R.id.showHintDelLayout)
        deleteArea = findViewById(R.id.deleteSpecLayout)

        // Setup the data sources
        sectionDataSource = SectionDataSource(this)
        countDataSource = CountDataSource(this)
        alertDataSource = AlertDataSource(this)

        // New onBackPressed logic
        if (Build.VERSION.SDK_INT >= 33) {
            onBackPressedDispatcher.addCallback(object :
                OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    countDataSource!!.close()
                    sectionDataSource!!.close()
                    alertDataSource!!.close()

                    val intent = NavUtils.getParentActivityIntent(this@DelSpeciesActivity)!!
                    intent.putExtra("section_id", sectionId)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    NavUtils.navigateUpTo(this@DelSpeciesActivity, intent)
                }
            })
        }
    }
    // End of onCreate

    override fun onResume() {
        super.onResume()

        // Build the Delete Species screen
        countDataSource!!.open()
        sectionDataSource!!.open()
        alertDataSource!!.open()

        // clear any existing views
        deleteArea!!.removeAllViews()
        hintArea!!.removeAllViews()

        supportActionBar!!.setTitle(R.string.deleteSpecies)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Display hint: Species in counting list
        val nw = HintWidget(this, null)
        nw.setHint1(getString(R.string.presentSpecs))
        hintArea!!.addView(nw)

        // Load the sorted species data from section 1
        val counts = when (Objects.requireNonNull(sortPref)) {
            "names_alpha" -> countDataSource!!.getAllSpeciesForSectionSrtName(1)
            "codes" -> countDataSource!!.getAllSpeciesForSectionSrtCode(1)
            else -> countDataSource!!.getAllCountsForSection(1)
        }

        // Get all counting list species into their CountEditWidgets and add these to the view
        for (count in counts) {
            val dew = DeleteSpeciesWidget(this, null)
            dew.setSpecName(count.name)
            dew.setSpecNameG(count.name_g)
            dew.setSpecCode(count.code)
            dew.setPSpec(count)
            dew.setSpecId(count.id.toString())
            deleteArea!!.addView(dew)
            if (MyDebug.LOG) Log.d(TAG, "167, name: " + count.name)
        }
    }
    // End of onResume

    // mark the selected species and consider it for delete from the species counts list
    fun checkBoxDel(view: View) {
        val idToDel = view.tag as Int
        if (MyDebug.LOG) Log.d(TAG, "175, View.tag: $idToDel")
        val dew = deleteArea!!.getChildAt(idToDel) as DeleteSpeciesWidget

        val checked = dew.getMarkSpec() // return boolean isChecked

        // put species on add list
        if (checked) {
            listToDelete!!.add(dew)
            if (MyDebug.LOG) {
                val codeD = dew.getSpecCode()
                Log.d(TAG, "185, mark delete code: $codeD")
            }
        } else {
            // remove species previously added from add list
            listToDelete!!.remove(dew)
            if (MyDebug.LOG) {
                val codeD = dew.getSpecCode()
                Log.d(TAG, "192, mark delete code: $codeD")
            }
        }
    }

    // delete selected species from species lists of all sections
    private fun delSpecs() {
        var i = 0 // index of species list to delete
        val numSect: Int = sectionDataSource!!.numEntries

        val areYouSure = AlertDialog.Builder(this)
        areYouSure.setTitle(getString(R.string.deleteSpecs))
        areYouSure.setMessage(getString(R.string.reallyDeleteSpecs))
        areYouSure.setPositiveButton(R.string.yesDeleteIt) { _: DialogInterface?, _: Int ->
            // Go ahead for the delete
            while (i < listToDelete!!.size) {
                specCode = listToDelete!![i].getSpecCode()
                if (MyDebug.LOG) {
                    Log.d(TAG, "210, delete code: $specCode")
                }
                try {
                    var sectid = 1
                    while (sectid <= numSect) {
                        countDataSource!!.deleteAllCountsWithCode(specCode)
                        sectid++
                    }
                } catch (e: Exception) {
                    // nothing
                }
                i++
            }
            alertDataSource!!.deleteAlerts()

            // re-index and sort counts table
            countDataSource!!.sortCounts()

            // rebuild the species list
            val counts = when (Objects.requireNonNull(sortPref)) {
                "names_alpha" -> countDataSource!!.getAllSpeciesForSectionSrtName(1)
                "codes" -> countDataSource!!.getAllSpeciesForSectionSrtCode(1)
                else -> countDataSource!!.getAllCountsForSection(1)
            }

            deleteArea!!.removeAllViews()
            for (count in counts) {
                val dew = DeleteSpeciesWidget(this, null)
                dew.setSpecName(count.name)
                dew.setSpecNameG(count.name_g)
                dew.setSpecCode(count.code)
                dew.setPSpec(count)
                dew.setSpecId(count.id.toString())
                deleteArea!!.addView(dew)
            }
        }
        areYouSure.setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int -> }
        areYouSure.show()
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
            sectionDataSource!!.close()
            countDataSource!!.close()
            alertDataSource!!.close()

            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NavUtils.navigateUpTo(this, intent)
        } else if (id == R.id.deleteSpec) {
            delSpecs()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        // Close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()
        alertDataSource!!.close()
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("ApplySharedPref", "MissingSuperCall")
    override fun onBackPressed() {
        countDataSource!!.close()
        sectionDataSource!!.close()
        alertDataSource!!.close()

        val intent = NavUtils.getParentActivityIntent(this)!!
        intent.putExtra("section_id", sectionId)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        NavUtils.navigateUpTo(this, intent)

        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    companion object {
        private const val TAG = "DelSpecAct"
    }

}
