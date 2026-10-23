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
 * CountOptionsActivity
 * Edit options for counted species.
 * Uses CountOptionsWidget.kt, CountOptionsLhWidget.kt,
 * widget_options.xml, widget_options_lh.xml and
 * CountOptionsNotesWidget.
 *
 * Parts derived from CountOptionsActivity.java of BeeCount created by Milo Thurston (knirirr)
 * on 2014-05-05, licensed under the Apache License, Version 2.0.
 *
 * Supplemented with functions for transect counters.
 *
 * Adapted and changed by wmstein since 2016-02-18,
 * last edited in Java on 2023-05-08,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2026-10-21.
 **************************************************************************************************/

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

class CountOptionsActivity : AppCompatActivity() {
    private var count: Count? = null
    private var countId = 0
    private var countDataSource: CountDataSource? = null
    private var sectionDataSource: SectionDataSource? = null
    private var sectionId = 0
    private var sectionName = ""
    private var staticWidgetArea: LinearLayout? = null
    private var dynamicWidgetArea: LinearLayout? = null
    private var countOW: CountOptionsWidget? = null
    private var countOWLh: CountOptionsLhWidget? = null
    private var countONW: CountOptionsNotesWidget? = null

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
    private var leftHandPref = false
    private var awakePref = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "107, onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }

        setContentView(R.layout.activity_count_options)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.count_options_view)) { v, windowInsets ->
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
            Log.i(TAG, "164, onResume")

        brightPref = prefs.getBoolean("pref_bright", true)
        awakePref = prefs.getBoolean("pref_awake", true)
        leftHandPref = prefs.getBoolean("pref_left_hand", false)

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
        if (leftHandPref) {
            countOWLh = CountOptionsLhWidget(this, null)
            sf1i = countOWLh!!.parameterValuef1i
            sf2i = countOWLh!!.parameterValuef2i
            sf3i = countOWLh!!.parameterValuef3i
            spi = countOWLh!!.parameterValuepi
            sli = countOWLh!!.parameterValueli
            sei = countOWLh!!.parameterValueei

            sf1e = countOWLh!!.parameterValuef1e
            sf2e = countOWLh!!.parameterValuef2e
            sf3e = countOWLh!!.parameterValuef3e
            spe = countOWLh!!.parameterValuepe
            sle = countOWLh!!.parameterValuele
            see = countOWLh!!.parameterValueee
        } else {
            countOW = CountOptionsWidget(this, null)
            sf1i = countOW!!.parameterValuef1i
            sf2i = countOW!!.parameterValuef2i
            sf3i = countOW!!.parameterValuef3i
            spi = countOW!!.parameterValuepi
            sli = countOW!!.parameterValueli
            sei = countOW!!.parameterValueei

            sf1e = countOW!!.parameterValuef1e
            sf2e = countOW!!.parameterValuef2e
            sf3e = countOW!!.parameterValuef3e
            spe = countOW!!.parameterValuepe
            sle = countOW!!.parameterValuele
            see = countOW!!.parameterValueee
        }
        countsInit = if (sf1i + sf2i + sf3i + spi + sli + sei + sf1e + sf2e + sf3e + spe + sle + see > 0)
            true // has initial counts
        else
            false // no count so far

        if (leftHandPref) {
            countOWLh!!.setInstructionsf1i(
                String.format(
                    getString(R.string.editCountValuef1i),
                    count!!.count_f1i
                )
            )
            countOWLh!!.setInstructionsf2i(
                String.format(
                    getString(R.string.editCountValuef2i),
                    count!!.count_f2i
                )
            )
            countOWLh!!.setInstructionsf3i(
                String.format(
                    getString(R.string.editCountValuef3i),
                    count!!.count_f3i
                )
            )
            countOWLh!!.setInstructionspi(
                String.format(
                    getString(R.string.editCountValuepi),
                    count!!.count_pi
                )
            )
            countOWLh!!.setInstructionsli(
                String.format(
                    getString(R.string.editCountValueli),
                    count!!.count_li
                )
            )
            countOWLh!!.setInstructionsei(
                String.format(
                    getString(R.string.editCountValueei),
                    count!!.count_ei
                )
            )
            countOWLh!!.setInstructionsf1e(
                String.format(
                    getString(R.string.editCountValuef1e),
                    count!!.count_f1e
                )
            )
            countOWLh!!.setInstructionsf2e(
                String.format(
                    getString(R.string.editCountValuef2e),
                    count!!.count_f2e
                )
            )
            countOWLh!!.setInstructionsf3e(
                String.format(
                    getString(R.string.editCountValuef3e),
                    count!!.count_f3e
                )
            )
            countOWLh!!.setInstructionspe(
                String.format(
                    getString(R.string.editCountValuepe),
                    count!!.count_pe
                )
            )
            countOWLh!!.setInstructionsle(
                String.format(
                    getString(R.string.editCountValuele),
                    count!!.count_le
                )
            )
            countOWLh!!.setInstructionsee(
                String.format(
                    getString(R.string.editCountValueee),
                    count!!.count_ee
                )
            )
            countOWLh!!.parameterValuef1i = count!!.count_f1i
            countOWLh!!.parameterValuef2i = count!!.count_f2i
            countOWLh!!.parameterValuef3i = count!!.count_f3i
            countOWLh!!.parameterValuepi = count!!.count_pi
            countOWLh!!.parameterValueli = count!!.count_li
            countOWLh!!.parameterValueei = count!!.count_ei
            countOWLh!!.parameterValuef1e = count!!.count_f1e
            countOWLh!!.parameterValuef2e = count!!.count_f2e
            countOWLh!!.parameterValuef3e = count!!.count_f3e
            countOWLh!!.parameterValuepe = count!!.count_pe
            countOWLh!!.parameterValuele = count!!.count_le
            countOWLh!!.parameterValueee = count!!.count_ee
            staticWidgetArea!!.addView(countOWLh)
        } else {
            countOW!!.setInstructionsf1i(
                String.format(
                    getString(R.string.editCountValuef1i),
                    count!!.count_f1i
                )
            )
            countOW!!.setInstructionsf2i(
                String.format(
                    getString(R.string.editCountValuef2i),
                    count!!.count_f2i
                )
            )
            countOW!!.setInstructionsf3i(
                String.format(
                    getString(R.string.editCountValuef3i),
                    count!!.count_f3i
                )
            )
            countOW!!.setInstructionspi(
                String.format(
                    getString(R.string.editCountValuepi),
                    count!!.count_pi
                )
            )
            countOW!!.setInstructionsli(
                String.format(
                    getString(R.string.editCountValueli),
                    count!!.count_li
                )
            )
            countOW!!.setInstructionsei(
                String.format(
                    getString(R.string.editCountValueei),
                    count!!.count_ei
                )
            )
            countOW!!.setInstructionsf1e(
                String.format(
                    getString(R.string.editCountValuef1e),
                    count!!.count_f1e
                )
            )
            countOW!!.setInstructionsf2e(
                String.format(
                    getString(R.string.editCountValuef2e),
                    count!!.count_f2e
                )
            )
            countOW!!.setInstructionsf3e(
                String.format(
                    getString(R.string.editCountValuef3e),
                    count!!.count_f3e
                )
            )
            countOW!!.setInstructionspe(
                String.format(
                    getString(R.string.editCountValuepe),
                    count!!.count_pe
                )
            )
            countOW!!.setInstructionsle(
                String.format(
                    getString(R.string.editCountValuele),
                    count!!.count_le
                )
            )
            countOW!!.setInstructionsee(
                String.format(
                    getString(R.string.editCountValueee),
                    count!!.count_ee
                )
            )
            countOW!!.parameterValuef1i = count!!.count_f1i
            countOW!!.parameterValuef2i = count!!.count_f2i
            countOW!!.parameterValuef3i = count!!.count_f3i
            countOW!!.parameterValuepi = count!!.count_pi
            countOW!!.parameterValueli = count!!.count_li
            countOW!!.parameterValueei = count!!.count_ei
            countOW!!.parameterValuef1e = count!!.count_f1e
            countOW!!.parameterValuef2e = count!!.count_f2e
            countOW!!.parameterValuef3e = count!!.count_f3e
            countOW!!.parameterValuepe = count!!.count_pe
            countOW!!.parameterValuele = count!!.count_le
            countOW!!.parameterValueee = count!!.count_ee
            staticWidgetArea!!.addView(countOW)
        }

        // Set the widget for species notes
        countONW = CountOptionsNotesWidget(this, null)
        countONW!!.setSpecNotesTitle(getString(R.string.notesSpecies, sectionName))
        countONW!!.spNotes = count!!.notes // Read existing notes into widget
        staticWidgetArea!!.addView(countONW)
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
            Log.i(TAG, "444, onPause")

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
            Log.i(TAG, "463, onStop")

        staticWidgetArea = null
        dynamicWidgetArea = null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "473, onDestroy")
    }

    fun saveData(): Boolean {
        sf1i = countOW!!.parameterValuef1i
        count!!.count_f1i = sf1i
        sf2i = countOW!!.parameterValuef2i
        count!!.count_f2i = sf2i
        sf3i = countOW!!.parameterValuef3i
        count!!.count_f3i = sf3i
        spi = countOW!!.parameterValuepi
        count!!.count_pi = spi
        sli = countOW!!.parameterValueli
        count!!.count_li = sli
        sei = countOW!!.parameterValueei
        count!!.count_ei = sei
        sf1e = countOW!!.parameterValuef1e
        count!!.count_f1e = sf1e
        sf2e = countOW!!.parameterValuef2e
        count!!.count_f2e = sf2e
        sf3e = countOW!!.parameterValuef3e
        count!!.count_f3e = sf3e
        spe = countOW!!.parameterValuepe
        count!!.count_pe = spe
        sle = countOW!!.parameterValuele
        count!!.count_le = sle
        see = countOW!!.parameterValueee
        count!!.count_ee = see
        count!!.notes = countONW!!.spNotes

        val msg = getString(R.string.saving) + " " + count!!.name + "!"
        Toast.makeText(
            this, fromHtml("<font color='#008000'>$msg</font>"),
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
