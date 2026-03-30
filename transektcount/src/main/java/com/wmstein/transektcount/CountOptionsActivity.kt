@file:Suppress("USELESS_CAST")

package com.wmstein.transektcount

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.wmstein.transektcount.Utils.fromHtml
import com.wmstein.transektcount.database.Count
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.widgets.EditSectionListNotesWidget
import com.wmstein.transektcount.widgets.CountOptionsWidget
import com.wmstein.transektcount.widgets.CountOptionsLhWidget

/************************************************************
 * CountOptionsActivity
 * Edit options for counting species
 * uses optionsWidget.kt, optionsWidgetExt.kt and widget_options.xml
 * Supplemented with functions for transect external counter
 * Based on CountOptionsActivity.java by milo on 05/05/2014.
 * Adapted and changed by wmstein since 2016-02-18,
 * last edited in Java on 2023-05-08,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2026-02-22
 */
class CountOptionsActivity : AppCompatActivity() {
    private var count: Count? = null
    private var countId = 0
    private var countDataSource: CountDataSource? = null
    private var sectionDataSource: SectionDataSource? = null
    private var sectionId = 0
    private var sectionName = ""
    private var staticWidgetArea: LinearLayout? = null
    private var dynamicWidgetArea: LinearLayout? = null
    private var optWidget: CountOptionsWidget? = null
    private var optWidgetLh: CountOptionsLhWidget? = null
    private var enw: EditSectionListNotesWidget? = null

    private var sf1i = 0
    private var sf2i = 0
    private var sf3i = 0
    private var spi = 0
    private var sli = 0
    private var sei = 0
    private var sf1e = 0
    private var sf2e = 0
    private var sf3e = 0
    private var spe = 0
    private var sle = 0
    private var see = 0

    // Criteria on setting or removing the date for section:
    //  countsInit = false, countsCurrent = false -> delete date
    //  countsInit = false, countsCurrent = true -> save date
    //  countsInit = true, countsCurrent = false -> delete date
    //  countsInit = true, countsCurrent = true -> no change
    private var countsInit = false
    private var countsCurrent = false

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false
    private var lhandPref = false
    private var awakePref = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "85, onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }

        setContentView(R.layout.activity_count_options)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.count_options)) { v, windowInsets ->
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

        val extras = intent.extras
        if (extras != null) {
            countId = extras.getInt("count_id")
            sectionId = extras.getInt("section_id")
            sectionName = extras.getString("section_name").toString()
        }

        staticWidgetArea = findViewById(R.id.static_widget_area)
        dynamicWidgetArea = findViewById(R.id.dynamic_widget_area)

        countDataSource = CountDataSource(this)
        sectionDataSource = SectionDataSource(this)

        // New onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = NavUtils.getParentActivityIntent(this@CountOptionsActivity)!!
                intent.putExtra("section_id", sectionId)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                this@CountOptionsActivity.navigateUpTo(intent)
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "142, onResume")

        brightPref = prefs.getBoolean("pref_bright", true)
        awakePref = prefs.getBoolean("pref_awake", true)
        lhandPref = prefs.getBoolean("pref_left_hand", false)

        // Set full brightness of screen
        if (brightPref) {
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        if (awakePref)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Clear any existing views
        staticWidgetArea!!.removeAllViews()
        dynamicWidgetArea!!.removeAllViews()

        // Get the data sources
        countDataSource!!.open()
        sectionDataSource!!.open()
        count = countDataSource!!.getCountById(countId)

        supportActionBar!!.title = count!!.name
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Set up the static widgets in the following order
        //  1. Current count values (internal counters)
        //  2. Current count values (external counters)
        if (lhandPref) {
            optWidgetLh = CountOptionsLhWidget(this, null)
            sf1i = optWidgetLh!!.parameterValuef1i
            sf2i = optWidgetLh!!.parameterValuef2i
            sf3i = optWidgetLh!!.parameterValuef3i
            spi = optWidgetLh!!.parameterValuepi
            sli = optWidgetLh!!.parameterValueli
            sei = optWidgetLh!!.parameterValueei

            sf1e = optWidgetLh!!.parameterValuef1e
            sf2e = optWidgetLh!!.parameterValuef2e
            sf3e = optWidgetLh!!.parameterValuef3e
            spe = optWidgetLh!!.parameterValuepe
            sle = optWidgetLh!!.parameterValuele
            see = optWidgetLh!!.parameterValueee
        } else {
            optWidget = CountOptionsWidget(this, null)
            sf1i = optWidget!!.parameterValuef1i
            sf2i = optWidget!!.parameterValuef2i
            sf3i = optWidget!!.parameterValuef3i
            spi = optWidget!!.parameterValuepi
            sli = optWidget!!.parameterValueli
            sei = optWidget!!.parameterValueei

            sf1e = optWidget!!.parameterValuef1e
            sf2e = optWidget!!.parameterValuef2e
            sf3e = optWidget!!.parameterValuef3e
            spe = optWidget!!.parameterValuepe
            sle = optWidget!!.parameterValuele
            see = optWidget!!.parameterValueee
        }
        if (sf1i + sf2i + sf3i + spi + sli + sei + sf1e + sf2e + sf3e + spe + sle + see > 0)
            countsInit = true // has initial counts
        else
            countsInit = false // no count so far

        if (lhandPref) {
            optWidgetLh!!.setInstructionsf1i(
                String.format(
                    getString(R.string.editCountValuef1i),
                    count!!.count_f1i
                )
            )
            optWidgetLh!!.setInstructionsf2i(
                String.format(
                    getString(R.string.editCountValuef2i),
                    count!!.count_f2i
                )
            )
            optWidgetLh!!.setInstructionsf3i(
                String.format(
                    getString(R.string.editCountValuef3i),
                    count!!.count_f3i
                )
            )
            optWidgetLh!!.setInstructionspi(
                String.format(
                    getString(R.string.editCountValuepi),
                    count!!.count_pi
                )
            )
            optWidgetLh!!.setInstructionsli(
                String.format(
                    getString(R.string.editCountValueli),
                    count!!.count_li
                )
            )
            optWidgetLh!!.setInstructionsei(
                String.format(
                    getString(R.string.editCountValueei),
                    count!!.count_ei
                )
            )
            optWidgetLh!!.setInstructionsf1e(
                String.format(
                    getString(R.string.editCountValuef1e),
                    count!!.count_f1e
                )
            )
            optWidgetLh!!.setInstructionsf2e(
                String.format(
                    getString(R.string.editCountValuef2e),
                    count!!.count_f2e
                )
            )
            optWidgetLh!!.setInstructionsf3e(
                String.format(
                    getString(R.string.editCountValuef3e),
                    count!!.count_f3e
                )
            )
            optWidgetLh!!.setInstructionspe(
                String.format(
                    getString(R.string.editCountValuepe),
                    count!!.count_pe
                )
            )
            optWidgetLh!!.setInstructionsle(
                String.format(
                    getString(R.string.editCountValuele),
                    count!!.count_le
                )
            )
            optWidgetLh!!.setInstructionsee(
                String.format(
                    getString(R.string.editCountValueee),
                    count!!.count_ee
                )
            )
            optWidgetLh!!.parameterValuef1i = count!!.count_f1i
            optWidgetLh!!.parameterValuef2i = count!!.count_f2i
            optWidgetLh!!.parameterValuef3i = count!!.count_f3i
            optWidgetLh!!.parameterValuepi = count!!.count_pi
            optWidgetLh!!.parameterValueli = count!!.count_li
            optWidgetLh!!.parameterValueei = count!!.count_ei
            optWidgetLh!!.parameterValuef1e = count!!.count_f1e
            optWidgetLh!!.parameterValuef2e = count!!.count_f2e
            optWidgetLh!!.parameterValuef3e = count!!.count_f3e
            optWidgetLh!!.parameterValuepe = count!!.count_pe
            optWidgetLh!!.parameterValuele = count!!.count_le
            optWidgetLh!!.parameterValueee = count!!.count_ee
            staticWidgetArea!!.addView(optWidgetLh)
        } else {
            optWidget!!.setInstructionsf1i(
                String.format(
                    getString(R.string.editCountValuef1i),
                    count!!.count_f1i
                )
            )
            optWidget!!.setInstructionsf2i(
                String.format(
                    getString(R.string.editCountValuef2i),
                    count!!.count_f2i
                )
            )
            optWidget!!.setInstructionsf3i(
                String.format(
                    getString(R.string.editCountValuef3i),
                    count!!.count_f3i
                )
            )
            optWidget!!.setInstructionspi(
                String.format(
                    getString(R.string.editCountValuepi),
                    count!!.count_pi
                )
            )
            optWidget!!.setInstructionsli(
                String.format(
                    getString(R.string.editCountValueli),
                    count!!.count_li
                )
            )
            optWidget!!.setInstructionsei(
                String.format(
                    getString(R.string.editCountValueei),
                    count!!.count_ei
                )
            )
            optWidget!!.setInstructionsf1e(
                String.format(
                    getString(R.string.editCountValuef1e),
                    count!!.count_f1e
                )
            )
            optWidget!!.setInstructionsf2e(
                String.format(
                    getString(R.string.editCountValuef2e),
                    count!!.count_f2e
                )
            )
            optWidget!!.setInstructionsf3e(
                String.format(
                    getString(R.string.editCountValuef3e),
                    count!!.count_f3e
                )
            )
            optWidget!!.setInstructionspe(
                String.format(
                    getString(R.string.editCountValuepe),
                    count!!.count_pe
                )
            )
            optWidget!!.setInstructionsle(
                String.format(
                    getString(R.string.editCountValuele),
                    count!!.count_le
                )
            )
            optWidget!!.setInstructionsee(
                String.format(
                    getString(R.string.editCountValueee),
                    count!!.count_ee
                )
            )
            optWidget!!.parameterValuef1i = count!!.count_f1i
            optWidget!!.parameterValuef2i = count!!.count_f2i
            optWidget!!.parameterValuef3i = count!!.count_f3i
            optWidget!!.parameterValuepi = count!!.count_pi
            optWidget!!.parameterValueli = count!!.count_li
            optWidget!!.parameterValueei = count!!.count_ei
            optWidget!!.parameterValuef1e = count!!.count_f1e
            optWidget!!.parameterValuef2e = count!!.count_f2e
            optWidget!!.parameterValuef3e = count!!.count_f3e
            optWidget!!.parameterValuepe = count!!.count_pe
            optWidget!!.parameterValuele = count!!.count_le
            optWidget!!.parameterValueee = count!!.count_ee
            staticWidgetArea!!.addView(optWidget)
        }

        enw = EditSectionListNotesWidget(this, null)
        enw!!.sNotes = count!!.notes
        enw!!.setWidgetNotes(getString(R.string.notesSpecies, sectionName))
        enw!!.setHint(getString(R.string.notesHint))
        staticWidgetArea!!.addView(enw)
    }
    // End of onResume()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.count_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId
        if (id == android.R.id.home) {
            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.putExtra("section_id", sectionId)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            this@CountOptionsActivity.navigateUpTo(intent)
            return true
        } else if (id == R.id.menuSaveExit) {
            if (saveData()) {
                val intent = NavUtils.getParentActivityIntent(this)!!
                intent.putExtra("section_id", sectionId)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                this@CountOptionsActivity.navigateUpTo(intent)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "422, onPause")

        countDataSource!!.close()
        sectionDataSource!!.close()

        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        staticWidgetArea!!.clearFocus()
        staticWidgetArea!!.removeAllViews()
        dynamicWidgetArea!!.clearFocus()
        dynamicWidgetArea!!.removeAllViews()
    }

    override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "441, onStop")

        staticWidgetArea = null
        dynamicWidgetArea = null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "451, onDestroy")
    }

    fun saveData(): Boolean {
        sf1i = optWidget!!.parameterValuef1i
        count!!.count_f1i = sf1i
        sf2i = optWidget!!.parameterValuef2i
        count!!.count_f2i = sf2i
        sf3i = optWidget!!.parameterValuef3i
        count!!.count_f3i = sf3i
        spi = optWidget!!.parameterValuepi
        count!!.count_pi = spi
        sli = optWidget!!.parameterValueli
        count!!.count_li = sli
        sei = optWidget!!.parameterValueei
        count!!.count_ei = sei
        sf1e = optWidget!!.parameterValuef1e
        count!!.count_f1e = sf1e
        sf2e = optWidget!!.parameterValuef2e
        count!!.count_f2e = sf2e
        sf3e = optWidget!!.parameterValuef3e
        count!!.count_f3e = sf3e
        spe = optWidget!!.parameterValuepe
        count!!.count_pe = spe
        sle = optWidget!!.parameterValuele
        count!!.count_le = sle
        see = optWidget!!.parameterValueee
        count!!.count_ee = see
        count!!.notes = enw!!.sNotes

        val mesg = getString(R.string.saving) + " " + count!!.name + "!"
        Toast.makeText(
            this, fromHtml("<font color='#008000'>$mesg</font>"),
            Toast.LENGTH_SHORT
        ).show()

        if (sf1i + sf2i + sf3i + spi + sli + sei + sf1e + sf2e + sf3e + spe + sle + see > 0)
            countsCurrent = true // has count(s)

        countDataSource!!.saveCount(count!!)
        if (!countsInit && countsCurrent) // if no previous count and a new count entered 
            sectionDataSource!!.saveDateSectionOfId(sectionId)

        if (!countsCurrent) // if no count
            sectionDataSource!!.clearDateSectionOfId(sectionId)

        return true
    }

    companion object {
        private const val TAG = "CntOptAct"
    }

}
