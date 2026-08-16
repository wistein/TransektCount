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
import com.wmstein.transektcount.widgets.CountOptionsNotesWidget
import com.wmstein.transektcount.widgets.CountOptionsWidget
import com.wmstein.transektcount.widgets.CountOptionsLhWidget

/************************************************************
 * CountOptionsActivity
 * Edit options for counted species.
 * Uses CountOptionsWidget.kt, CountOptionsLhWidget.kt,
 * widget_options.xml, widget_options_lh.xml and
 * CountOptionsNotesWidget.
 *
 * Based on CountOptionsActivity.java by milo on 05/05/2014.
 * Supplemented with functions for transect external counters.
 *
 * Adapted and changed by wmstein since 2016-02-18,
 * last edited in Java on 2023-05-08,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2026-08-12.
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
    private var cow: CountOptionsWidget? = null
    private var cowLh: CountOptionsLhWidget? = null
    private var conw: CountOptionsNotesWidget? = null

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
            Log.i(TAG, "89, onCreate")

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
            Log.i(TAG, "146, onResume")

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
            cowLh = CountOptionsLhWidget(this, null)
            sf1i = cowLh!!.parameterValuef1i
            sf2i = cowLh!!.parameterValuef2i
            sf3i = cowLh!!.parameterValuef3i
            spi = cowLh!!.parameterValuepi
            sli = cowLh!!.parameterValueli
            sei = cowLh!!.parameterValueei

            sf1e = cowLh!!.parameterValuef1e
            sf2e = cowLh!!.parameterValuef2e
            sf3e = cowLh!!.parameterValuef3e
            spe = cowLh!!.parameterValuepe
            sle = cowLh!!.parameterValuele
            see = cowLh!!.parameterValueee
        } else {
            cow = CountOptionsWidget(this, null)
            sf1i = cow!!.parameterValuef1i
            sf2i = cow!!.parameterValuef2i
            sf3i = cow!!.parameterValuef3i
            spi = cow!!.parameterValuepi
            sli = cow!!.parameterValueli
            sei = cow!!.parameterValueei

            sf1e = cow!!.parameterValuef1e
            sf2e = cow!!.parameterValuef2e
            sf3e = cow!!.parameterValuef3e
            spe = cow!!.parameterValuepe
            sle = cow!!.parameterValuele
            see = cow!!.parameterValueee
        }
        countsInit = if (sf1i + sf2i + sf3i + spi + sli + sei + sf1e + sf2e + sf3e + spe + sle + see > 0)
            true // has initial counts
        else
            false // no count so far

        if (lhandPref) {
            cowLh!!.setInstructionsf1i(
                String.format(
                    getString(R.string.editCountValuef1i),
                    count!!.count_f1i
                )
            )
            cowLh!!.setInstructionsf2i(
                String.format(
                    getString(R.string.editCountValuef2i),
                    count!!.count_f2i
                )
            )
            cowLh!!.setInstructionsf3i(
                String.format(
                    getString(R.string.editCountValuef3i),
                    count!!.count_f3i
                )
            )
            cowLh!!.setInstructionspi(
                String.format(
                    getString(R.string.editCountValuepi),
                    count!!.count_pi
                )
            )
            cowLh!!.setInstructionsli(
                String.format(
                    getString(R.string.editCountValueli),
                    count!!.count_li
                )
            )
            cowLh!!.setInstructionsei(
                String.format(
                    getString(R.string.editCountValueei),
                    count!!.count_ei
                )
            )
            cowLh!!.setInstructionsf1e(
                String.format(
                    getString(R.string.editCountValuef1e),
                    count!!.count_f1e
                )
            )
            cowLh!!.setInstructionsf2e(
                String.format(
                    getString(R.string.editCountValuef2e),
                    count!!.count_f2e
                )
            )
            cowLh!!.setInstructionsf3e(
                String.format(
                    getString(R.string.editCountValuef3e),
                    count!!.count_f3e
                )
            )
            cowLh!!.setInstructionspe(
                String.format(
                    getString(R.string.editCountValuepe),
                    count!!.count_pe
                )
            )
            cowLh!!.setInstructionsle(
                String.format(
                    getString(R.string.editCountValuele),
                    count!!.count_le
                )
            )
            cowLh!!.setInstructionsee(
                String.format(
                    getString(R.string.editCountValueee),
                    count!!.count_ee
                )
            )
            cowLh!!.parameterValuef1i = count!!.count_f1i
            cowLh!!.parameterValuef2i = count!!.count_f2i
            cowLh!!.parameterValuef3i = count!!.count_f3i
            cowLh!!.parameterValuepi = count!!.count_pi
            cowLh!!.parameterValueli = count!!.count_li
            cowLh!!.parameterValueei = count!!.count_ei
            cowLh!!.parameterValuef1e = count!!.count_f1e
            cowLh!!.parameterValuef2e = count!!.count_f2e
            cowLh!!.parameterValuef3e = count!!.count_f3e
            cowLh!!.parameterValuepe = count!!.count_pe
            cowLh!!.parameterValuele = count!!.count_le
            cowLh!!.parameterValueee = count!!.count_ee
            staticWidgetArea!!.addView(cowLh)
        } else {
            cow!!.setInstructionsf1i(
                String.format(
                    getString(R.string.editCountValuef1i),
                    count!!.count_f1i
                )
            )
            cow!!.setInstructionsf2i(
                String.format(
                    getString(R.string.editCountValuef2i),
                    count!!.count_f2i
                )
            )
            cow!!.setInstructionsf3i(
                String.format(
                    getString(R.string.editCountValuef3i),
                    count!!.count_f3i
                )
            )
            cow!!.setInstructionspi(
                String.format(
                    getString(R.string.editCountValuepi),
                    count!!.count_pi
                )
            )
            cow!!.setInstructionsli(
                String.format(
                    getString(R.string.editCountValueli),
                    count!!.count_li
                )
            )
            cow!!.setInstructionsei(
                String.format(
                    getString(R.string.editCountValueei),
                    count!!.count_ei
                )
            )
            cow!!.setInstructionsf1e(
                String.format(
                    getString(R.string.editCountValuef1e),
                    count!!.count_f1e
                )
            )
            cow!!.setInstructionsf2e(
                String.format(
                    getString(R.string.editCountValuef2e),
                    count!!.count_f2e
                )
            )
            cow!!.setInstructionsf3e(
                String.format(
                    getString(R.string.editCountValuef3e),
                    count!!.count_f3e
                )
            )
            cow!!.setInstructionspe(
                String.format(
                    getString(R.string.editCountValuepe),
                    count!!.count_pe
                )
            )
            cow!!.setInstructionsle(
                String.format(
                    getString(R.string.editCountValuele),
                    count!!.count_le
                )
            )
            cow!!.setInstructionsee(
                String.format(
                    getString(R.string.editCountValueee),
                    count!!.count_ee
                )
            )
            cow!!.parameterValuef1i = count!!.count_f1i
            cow!!.parameterValuef2i = count!!.count_f2i
            cow!!.parameterValuef3i = count!!.count_f3i
            cow!!.parameterValuepi = count!!.count_pi
            cow!!.parameterValueli = count!!.count_li
            cow!!.parameterValueei = count!!.count_ei
            cow!!.parameterValuef1e = count!!.count_f1e
            cow!!.parameterValuef2e = count!!.count_f2e
            cow!!.parameterValuef3e = count!!.count_f3e
            cow!!.parameterValuepe = count!!.count_pe
            cow!!.parameterValuele = count!!.count_le
            cow!!.parameterValueee = count!!.count_ee
            staticWidgetArea!!.addView(cow)
        }

        // Set the widget for species notes
        conw = CountOptionsNotesWidget(this, null)
        conw!!.setSpecNotesTitle(getString(R.string.notesSpecies, sectionName))
        conw!!.spNotes = count!!.notes // Read existing notes into widget
        staticWidgetArea!!.addView(conw)
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
            Log.i(TAG, "426, onPause")

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
            Log.i(TAG, "445, onStop")

        staticWidgetArea = null
        dynamicWidgetArea = null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "455, onDestroy")
    }

    fun saveData(): Boolean {
        sf1i = cow!!.parameterValuef1i
        count!!.count_f1i = sf1i
        sf2i = cow!!.parameterValuef2i
        count!!.count_f2i = sf2i
        sf3i = cow!!.parameterValuef3i
        count!!.count_f3i = sf3i
        spi = cow!!.parameterValuepi
        count!!.count_pi = spi
        sli = cow!!.parameterValueli
        count!!.count_li = sli
        sei = cow!!.parameterValueei
        count!!.count_ei = sei
        sf1e = cow!!.parameterValuef1e
        count!!.count_f1e = sf1e
        sf2e = cow!!.parameterValuef2e
        count!!.count_f2e = sf2e
        sf3e = cow!!.parameterValuef3e
        count!!.count_f3e = sf3e
        spe = cow!!.parameterValuepe
        count!!.count_pe = spe
        sle = cow!!.parameterValuele
        count!!.count_le = sle
        see = cow!!.parameterValueee
        count!!.count_ee = see
        count!!.notes = conw!!.spNotes

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
