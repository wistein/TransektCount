package com.wmstein.transektcount

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.wmstein.transektcount.database.AlertDataSource
import com.wmstein.transektcount.database.Count
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.widgets.AddAlertWidget
import com.wmstein.transektcount.widgets.AlertEditWidget
import com.wmstein.transektcount.widgets.EditNotesWidget
import com.wmstein.transektcount.widgets.OptionsWidget

/************************************************************
 * CountOptionsActivity
 * Edit options for counting species
 * uses optionsWidget.kt, optionsWidgetExt.kt and widget_options.xml
 * Supplemented with functions for transect external counter
 * Based on CountOptionsActivity.java by milo on 05/05/2014.
 * Adapted and changed by wmstein since 2016-02-18,
 * last edited in Java on 2023-05-08,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2024-12-17
 */
class CountOptionsActivity : AppCompatActivity() {
    private var count: Count? = null
    private var countId = 0
    private var countDataSource: CountDataSource? = null
    private var alertDataSource: AlertDataSource? = null
    private var markedForDelete: View? = null
    private var deleteAlert = 0
    private var sectionId = 0
    private var sectionName = ""
    private var staticWidgetArea: LinearLayout? = null
    private var dynamicWidgetArea: LinearLayout? = null
    private var optWidget: OptionsWidget? = null
    private var enw: EditNotesWidget? = null
    private var aaWidget: AddAlertWidget? = null
    private var savedAlerts: ArrayList<AlertEditWidget>? = null

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.DLOG) Log.i(TAG, "62, onCreate")

        brightPref = prefs.getBoolean("pref_bright", true)

        setContentView(R.layout.activity_count_options)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        val extras = intent.extras
        if (extras != null) {
            countId = extras.getInt("count_id")
            sectionId = extras.getInt("section_id")
            sectionName = extras.getString("section_name").toString()
        }

        staticWidgetArea = findViewById(R.id.static_widget_area)
        dynamicWidgetArea = findViewById(R.id.dynamic_widget_area)

        savedAlerts = ArrayList()
        if (savedInstanceState != null) {
            if (Build.VERSION.SDK_INT < 33) {
                @Suppress("DEPRECATION")
                if (savedInstanceState.getSerializable("savedAlerts") != null) {
                    @Suppress("UNCHECKED_CAST")
                    savedAlerts =
                        savedInstanceState.getSerializable("savedAlerts") as ArrayList<AlertEditWidget>?
                }
            } else {
                if (savedInstanceState.getSerializable("savedAlerts", T::class.java) != null) {
                    @Suppress("UNCHECKED_CAST")
                    savedAlerts =
                        savedInstanceState.getSerializable(
                            "savedAlerts", T::class.java
                        ) as ArrayList<AlertEditWidget>? // Removing "useless" cast produces error
                }
            }
        }

        countDataSource = CountDataSource(this)
        alertDataSource = AlertDataSource(this)

        // New onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                savedAlerts!!.clear()
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

        if (MyDebug.DLOG) Log.i(TAG, "126, onResume")

        // Clear any existing views
        staticWidgetArea!!.removeAllViews()
        dynamicWidgetArea!!.removeAllViews()

        // Get the data sources
        countDataSource!!.open()
        alertDataSource!!.open()
        count = countDataSource!!.getCountById(countId)

        supportActionBar!!.title = count!!.name
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val alerts = alertDataSource!!.getAllAlertsForCount(countId)

        // Setup the static widgets in the following order
        // 1. Current count values (internal counters)
        // 2. Current count values (external counters)
        // 3. Alert add/remove
        optWidget = OptionsWidget(this, null)
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

        enw = EditNotesWidget(this, null)
        enw!!.sNotes = count!!.notes
        enw!!.setWidgetNotes(getString(R.string.notesSpecies, sectionName))
        enw!!.setHint(getString(R.string.notesHint))
        staticWidgetArea!!.addView(enw)

        aaWidget = AddAlertWidget(this, null)
        staticWidgetArea!!.addView(aaWidget)

        for (alert in alerts) {
            val aew = AlertEditWidget(this, null)
            aew.alertName = alert.alert_text
            aew.alertValue = alert.alert
            aew.alertId = alert.id
            dynamicWidgetArea!!.addView(aew)
        }

        // Add saved alert create widgets
        for (aew in savedAlerts!!) {
            dynamicWidgetArea!!.addView(aew)
        }
    }
    // End of onResume()

    override fun onSaveInstanceState(outState: Bundle) {
        // Before these widgets can be serialised they must be removed from their parent, or else
        // trying to add them to a new parent causes a crash because they've already got one.
        for (aew in savedAlerts!!) {
            (aew.parent as ViewGroup).removeView(aew)
        }
        outState.putSerializable("savedAlerts", savedAlerts)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.count_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId
        if (id == android.R.id.home) {
            savedAlerts!!.clear()

            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.putExtra("section_id", sectionId)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            this@CountOptionsActivity.navigateUpTo(intent)
            return true
        } else if (id == R.id.menuSaveExit) {
            saveData()
            savedAlerts!!.clear()

            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.putExtra("section_id", sectionId)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            this@CountOptionsActivity.navigateUpTo(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        countDataSource!!.close()
        alertDataSource!!.close()
    }

    override fun onDestroy() {
        super.onDestroy()

        staticWidgetArea!!.clearFocus()
        dynamicWidgetArea!!.clearFocus()
    }

    fun saveData() {
        // Toast here, as snackbar doesn't show up
        Toast.makeText(
            applicationContext,
            getString(R.string.saving) + " " + count!!.name + "!",
            Toast.LENGTH_SHORT
        ).show()

        count!!.count_f1i = optWidget!!.parameterValuef1i
        count!!.count_f2i = optWidget!!.parameterValuef2i
        count!!.count_f3i = optWidget!!.parameterValuef3i
        count!!.count_pi = optWidget!!.parameterValuepi
        count!!.count_li = optWidget!!.parameterValueli
        count!!.count_ei = optWidget!!.parameterValueei
        count!!.count_f1e = optWidget!!.parameterValuef1e
        count!!.count_f2e = optWidget!!.parameterValuef2e
        count!!.count_f3e = optWidget!!.parameterValuef3e
        count!!.count_pe = optWidget!!.parameterValuepe
        count!!.count_le = optWidget!!.parameterValuele
        count!!.count_ee = optWidget!!.parameterValueee
        count!!.notes = enw!!.sNotes
        countDataSource!!.saveCount(count!!)

        /*
        * Get all the alerts from the dynamicWidgetArea and save each one.
        * If it has an alertId value set to anything higher than 0 then it should be an update,
        * if it is 0 then it's a new alert and must be created instead.
        */
        val childcount = dynamicWidgetArea!!.childCount
        for (i in 0 until childcount) {
            val aew = dynamicWidgetArea!!.getChildAt(i) as AlertEditWidget
            if (isNotEmpty(aew.alertName)) {
                // save or create
                if (aew.alertId == 0) {
                    alertDataSource!!.createAlert(countId, aew.alertValue, aew.alertName)
                } else {
                    alertDataSource!!.saveAlert(aew.alertId, aew.alertValue, aew.alertName)
                }
            } else {
                if (MyDebug.DLOG) Log.d(TAG, "350, Failed to save alert: " + aew.alertId)
            }
        }
    }

    // Scroll to end of view
    private fun scrollToEndOfView(scrlV: View) {
        var scrollAmount = scrlV.bottom
        var scrollY = scrollAmount
        var pageend = false
        while (!pageend) {
            scrlV.scrollTo(0, scrollAmount) //scroll
            scrollAmount += scrollAmount //increase scrollAmount
            scrollY += scrlV.scrollY     //scroll position of 1. row
            if (scrollAmount > scrollY) {
                pageend = true
            }
        }
    }

    // Add alert to species counter
    fun addAnAlert(view: View?) {
        val aew = AlertEditWidget(this, null)
        savedAlerts!!.add(aew)

        // Scroll to end of view
        val scrollV = findViewById<View>(R.id.count_options)
        scrollToEndOfView(scrollV)
        aew.requestFocus()
        dynamicWidgetArea!!.addView(aew)
    }

    // Delete alert from species counter and its widget from the view
    fun deleteAnAlert(view: View) {
        val areYouSure: AlertDialog.Builder
        markedForDelete = view
        deleteAlert = view.tag as Int
        if (deleteAlert == 0) {
            // the actual AlertEditWidget is two levels up from the button in which it is embedded
            dynamicWidgetArea!!.removeView(view.parent.parent as AlertEditWidget)
        } else {
            // Confirm before removing this widget and, if
            //   delete the associated alert from the database.
            areYouSure = AlertDialog.Builder(this)
            areYouSure.setTitle(getString(R.string.delAlert))
            areYouSure.setMessage(getString(R.string.reallyDeleteAlert))
            areYouSure.setPositiveButton(R.string.yesDeleteIt) { _: DialogInterface?, _: Int ->
                // go ahead for the delete
                try {
                    alertDataSource!!.deleteAlertById(deleteAlert)
                    dynamicWidgetArea!!.removeView(markedForDelete!!.parent.parent as AlertEditWidget)
                } catch (e: Exception) {
                    if (MyDebug.DLOG) Log.e(TAG, "402, Failed to delete a widget: $e")
                }
            }
            areYouSure.setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int -> }
            areYouSure.show()
        }
    }

    companion object {
        private const val TAG = "CntOptAct"

        /**
         * Following functions are taken from the Apache commons-lang3-3.4 library
         * licensed under Apache License Version 2.0, January 2004
         *
         *
         * Checks if a CharSequence is not empty ("") and not null.
         *
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

        /**
         * Checks if a CharSequence is empty ("") or null.
         *
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
