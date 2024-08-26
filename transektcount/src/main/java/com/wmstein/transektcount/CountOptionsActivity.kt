package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.wmstein.transektcount.database.AlertDataSource
import com.wmstein.transektcount.database.Count
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.widgets.AddAlertWidget
import com.wmstein.transektcount.widgets.AlertCreateWidget
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
 * last edited on 2024-07-27
 */
class CountOptionsActivity : AppCompatActivity() {
    private var transektCount: TransektCountApplication? = null

    private var count: Count? = null
    private var countId = 0
    private var countDataSource: CountDataSource? = null
    private var alertDataSource: AlertDataSource? = null
    private var markedForDelete: View? = null
    private var deleteAnAlert = 0
    private var sectionId = 0
    private var sectionName = ""
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null
    private var staticWidgetArea: LinearLayout? = null
    private var dynamicWidgetArea: LinearLayout? = null
    private var optWidget: OptionsWidget? = null
    private var enw: EditNotesWidget? = null
    private var aaWidget: AddAlertWidget? = null

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false

    private var savedAlerts: ArrayList<AlertCreateWidget>? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transektCount = application as TransektCountApplication
        prefs = TransektCountApplication.getPrefs()
        brightPref = prefs.getBoolean("pref_bright", true)

        setContentView(R.layout.activity_count_options)
        val countOptScreen = findViewById<ScrollView>(R.id.count_options)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        bMap = transektCount!!.decodeBitmap(
            R.drawable.edbackground,
            transektCount!!.width,
            transektCount!!.height
        )
        bg = BitmapDrawable(countOptScreen.resources, bMap)
        countOptScreen.background = bg
        staticWidgetArea = findViewById(R.id.static_widget_area)
        dynamicWidgetArea = findViewById(R.id.dynamic_widget_area)

        val extras = intent.extras
        if (extras != null) {
            countId = extras.getInt("count_id")
            sectionId = extras.getInt("section_id")
            sectionName = extras.getString("section_name").toString()
        }

        savedAlerts = ArrayList()
        if (savedInstanceState != null) {
            @Suppress("DEPRECATION")
            if (savedInstanceState.getSerializable("savedAlerts") != null) {
                savedAlerts =
                    savedInstanceState.getSerializable("savedAlerts") as ArrayList<AlertCreateWidget>?
            }
        }

        countDataSource = TransektCountApplication.getCountDS()
        alertDataSource = TransektCountApplication.getAlertDS()
    }
    // end of onCreate()

    override fun onResume() {
        super.onResume()

        // build the count options screen
        // clear any existing views
        staticWidgetArea!!.removeAllViews()
        dynamicWidgetArea!!.removeAllViews()

        // get the data sources
        countDataSource!!.open()
        alertDataSource!!.open()

        count = countDataSource!!.getCountById(countId)
        try {
            supportActionBar!!.title = count!!.name
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        } catch (e: NullPointerException) {
            if (MyDebug.LOG) Log.e(TAG, "131, Problem setting title bar: $e")
        }
        val alerts = alertDataSource!!.getAllAlertsForCount(countId)

        // setup the static widgets in the following order
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
            val acw = AlertCreateWidget(this, null)
            acw.alertName = alert.alert_text
            acw.alertValue = alert.alert
            acw.alertId = alert.id
            dynamicWidgetArea!!.addView(acw)
        }

        /*
        * Add saved alert create widgets
        */
        for (acw in savedAlerts!!) {
            dynamicWidgetArea!!.addView(acw)
        }
    } // end of onResume()

    override fun onSaveInstanceState(outState: Bundle) {
        // Before these widgets can be serialised they must be removed from their parent, or else
        // trying to add them to a new parent causes a crash because they've already got one.
        for (acw in savedAlerts!!) {
            (acw.parent as ViewGroup).removeView(acw)
        }
        outState.putSerializable("savedAlerts", savedAlerts)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()

        // finally, close the database
        countDataSource!!.close()
        alertDataSource!!.close()
    }

    fun saveAndExit(view: View?) {
        saveData()
        savedAlerts!!.clear()

        super.finish()
    }

    @SuppressLint("LongLogTag")
    fun saveData() {
        // Toast here, as snackbar doesn't show up
        Toast.makeText(
            this@CountOptionsActivity,
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
        * If it has an id value set to anything higher than 0 then it should be an update, if it is 0
        * then it's a new alert and should be created instead.
        */
        val childcount = dynamicWidgetArea!!.childCount
        for (i in 0 until childcount) {
            val acw = dynamicWidgetArea!!.getChildAt(i) as AlertCreateWidget
            if (isNotEmpty(acw.alertName)) {
                // save or create
                if (acw.alertId == 0) {
                    alertDataSource!!.createAlert(countId, acw.alertValue, acw.alertName)
                } else {
                    alertDataSource!!.saveAlert(acw.alertId, acw.alertValue, acw.alertName)
                }
            } else {
                if (MyDebug.LOG) Log.d(TAG, "316, Failed to save alert: " + acw.alertId)
            }
        }
    }

    // Scroll to end of view, by wmstein
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
        val acw = AlertCreateWidget(this, null)
        savedAlerts!!.add(acw)

        // Scroll to end of view
        val scrollV = findViewById<View>(R.id.count_options)
        scrollToEndOfView(scrollV)
        acw.requestFocus()
        dynamicWidgetArea!!.addView(acw)
    }

    // Delete alert from species counter and its widget from the view
    fun deleteWidget(view: View) {
        /*
        * These global variables keep a track of the view containing an alert to be deleted and also the id
        * of the alert itself, to make sure that they're available inside the code for the alert dialog by
        * which they will be deleted.
        */
        val areYouSure: AlertDialog.Builder
        markedForDelete = view
        deleteAnAlert = view.tag as Int
        if (deleteAnAlert == 0) {
            // the actual AlertCreateWidget is two levels up from the button in which it is embedded
            dynamicWidgetArea!!.removeView(view.parent.parent as AlertCreateWidget)
        } else {
            // before removing this widget it is necessary to do the following:
            // (1) Check the user is sure they want to delete it and, if so...
            // (2) Delete the associated alert from the database.
            areYouSure = AlertDialog.Builder(this)
            areYouSure.setTitle(getString(R.string.deleteAlert))
            areYouSure.setMessage(getString(R.string.reallyDeleteAlert))
            areYouSure.setPositiveButton(R.string.yesDeleteIt) { _: DialogInterface?, _: Int ->
                // go ahead for the delete
                try {
                    alertDataSource!!.deleteAlertById(deleteAnAlert)
                    dynamicWidgetArea!!.removeView(markedForDelete!!.parent.parent as AlertCreateWidget)
                } catch (e: Exception) {
                    if (MyDebug.LOG) Log.e(TAG, "374, Failed to delete a widget: $e")
                }
            }
            areYouSure.setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int -> }
            areYouSure.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.count_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == android.R.id.home) {
            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.putExtra("section_id", sectionId)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NavUtils.navigateUpTo(this, intent)
        } else if (id == R.id.menuSaveExit) {
            saveData()
            super.finish()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TAG = "transektcountCntOptAct"

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