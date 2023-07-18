package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
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
 * uses optionsWidget.java and widget_options.xml
 * Supplemented with functions for transect external counter
 * Based on CountOptionsActivity.java by milo on 05/05/2014.
 * Adapted and changed by wmstein since 2016-02-18,
 * last edited in Java on 2023-05-08,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2023-07-17
 */
class CountOptionsActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private var transektCount: TransektCountApplication? = null

    private var count: Count? = null
    private var count_id = 0
    private var countDataSource: CountDataSource? = null
    private var alertDataSource: AlertDataSource? = null
    private var markedForDelete: View? = null
    private var deleteAnAlert = 0
    private var section_id = 0
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null
    private var static_widget_area: LinearLayout? = null
    private var dynamic_widget_area: LinearLayout? = null
    private var curr_val_widget: OptionsWidget? = null
    private var enw: EditNotesWidget? = null
    private var aa_widget: AddAlertWidget? = null

    // Preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false

    private var savedAlerts: ArrayList<AlertCreateWidget>? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transektCount = application as TransektCountApplication
        prefs = TransektCountApplication.getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
        brightPref = prefs.getBoolean("pref_bright", true)

        setContentView(R.layout.activity_count_options)
        val counting_screen = findViewById<ScrollView>(R.id.count_options)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        bMap = transektCount!!.decodeBitmap(
            R.drawable.kbackground,
            transektCount!!.width,
            transektCount!!.height
        )
        bg = BitmapDrawable(counting_screen.resources, bMap)
        counting_screen.background = bg
        static_widget_area = findViewById(R.id.static_widget_area)
        dynamic_widget_area = findViewById(R.id.dynamic_widget_area)
        val extras = intent.extras
        if (extras != null) {
            count_id = extras.getInt("count_id")
            section_id = extras.getInt("section_id")
            //spinnerPosition = extras.getInt("itemposition");
        }

        savedAlerts = ArrayList()
        if (savedInstanceState != null) {
            @Suppress("DEPRECATION")
            if (savedInstanceState.getSerializable("savedAlerts") != null) {
                savedAlerts = savedInstanceState.getSerializable("savedAlerts") as ArrayList<AlertCreateWidget>?
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // build the count options screen
        // clear any existing views
        static_widget_area!!.removeAllViews()
        dynamic_widget_area!!.removeAllViews()

        // get the data sources
        countDataSource = CountDataSource(this)
        countDataSource!!.open()
        alertDataSource = AlertDataSource(this)
        alertDataSource!!.open()
        count = countDataSource!!.getCountById(count_id)
        try {
            supportActionBar!!.title = count!!.name
        } catch (e: NullPointerException) {
            if (MyDebug.LOG) Log.e(TAG, "Problem setting title bar: $e")
        }
        val alerts = alertDataSource!!.getAllAlertsForCount(count_id)

        // setup the static widgets in the following order
        // 1. Current count value (internal counter)
        // 2. Current counta value (external counter)
        // 3. Alert add/remove
        curr_val_widget = OptionsWidget(this, null)
        curr_val_widget!!.setInstructionsf1i(
            String.format(
                getString(R.string.editCountValuef1i),
                count!!.count_f1i
            )
        )
        curr_val_widget!!.setInstructionsf2i(
            String.format(
                getString(R.string.editCountValuef2i),
                count!!.count_f2i
            )
        )
        curr_val_widget!!.setInstructionsf3i(
            String.format(
                getString(R.string.editCountValuef3i),
                count!!.count_f3i
            )
        )
        curr_val_widget!!.setInstructionspi(
            String.format(
                getString(R.string.editCountValuepi),
                count!!.count_pi
            )
        )
        curr_val_widget!!.setInstructionsli(
            String.format(
                getString(R.string.editCountValueli),
                count!!.count_li
            )
        )
        curr_val_widget!!.setInstructionsei(
            String.format(
                getString(R.string.editCountValueei),
                count!!.count_ei
            )
        )
        curr_val_widget!!.setInstructionsf1e(
            String.format(
                getString(R.string.editCountValuef1e),
                count!!.count_f1e
            )
        )
        curr_val_widget!!.setInstructionsf2e(
            String.format(
                getString(R.string.editCountValuef2e),
                count!!.count_f2e
            )
        )
        curr_val_widget!!.setInstructionsf3e(
            String.format(
                getString(R.string.editCountValuef3e),
                count!!.count_f3e
            )
        )
        curr_val_widget!!.setInstructionspe(
            String.format(
                getString(R.string.editCountValuepe),
                count!!.count_pe
            )
        )
        curr_val_widget!!.setInstructionsle(
            String.format(
                getString(R.string.editCountValuele),
                count!!.count_le
            )
        )
        curr_val_widget!!.setInstructionsee(
            String.format(
                getString(R.string.editCountValueee),
                count!!.count_ee
            )
        )
        curr_val_widget!!.parameterValuef1i = count!!.count_f1i
        curr_val_widget!!.parameterValuef2i = count!!.count_f2i
        curr_val_widget!!.parameterValuef3i = count!!.count_f3i
        curr_val_widget!!.parameterValuepi = count!!.count_pi
        curr_val_widget!!.parameterValueli = count!!.count_li
        curr_val_widget!!.parameterValueei = count!!.count_ei
        curr_val_widget!!.parameterValuef1e = count!!.count_f1e
        curr_val_widget!!.parameterValuef2e = count!!.count_f2e
        curr_val_widget!!.parameterValuef3e = count!!.count_f3e
        curr_val_widget!!.parameterValuepe = count!!.count_pe
        curr_val_widget!!.parameterValuele = count!!.count_le
        curr_val_widget!!.parameterValueee = count!!.count_ee
        static_widget_area!!.addView(curr_val_widget)
        enw = EditNotesWidget(this, null)
        enw!!.sectionNotes = count!!.notes
        enw!!.setWidgetNotes(getString(R.string.notesSpecies))
        enw!!.setHint(getString(R.string.notesHint))
        static_widget_area!!.addView(enw)
        aa_widget = AddAlertWidget(this, null)
        static_widget_area!!.addView(aa_widget)

        for (alert in alerts) {
            val acw = AlertCreateWidget(this, null)
            acw.alertName = alert.alert_text
            acw.alertValue = alert.alert
            acw.alertId = alert.id
            dynamic_widget_area!!.addView(acw)
        }

        /*
        * Add saved alert create widgets
        */
        for (acw in savedAlerts!!) {
            dynamic_widget_area!!.addView(acw)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        /*
        * Before these widgets can be serialised they must be removed from their parent, or else
        * trying to add them to a new parent causes a crash because they've already got one.
        */
        super.onSaveInstanceState(outState)
        for (acw in savedAlerts!!) {
            (acw.parent as ViewGroup).removeView(acw)
        }
        outState.putSerializable("savedAlerts", savedAlerts)
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
        // don't crash if the user hasn't filled things in...
        // Toast here, as snackbar doesn't show up
        Toast.makeText(
            this@CountOptionsActivity,
            getString(R.string.sectSaving) + " " + count!!.name + "!",
            Toast.LENGTH_SHORT
        ).show()
        count!!.count_f1i = curr_val_widget!!.parameterValuef1i
        count!!.count_f2i = curr_val_widget!!.parameterValuef2i
        count!!.count_f3i = curr_val_widget!!.parameterValuef3i
        count!!.count_pi = curr_val_widget!!.parameterValuepi
        count!!.count_li = curr_val_widget!!.parameterValueli
        count!!.count_ei = curr_val_widget!!.parameterValueei
        count!!.count_f1e = curr_val_widget!!.parameterValuef1e
        count!!.count_f2e = curr_val_widget!!.parameterValuef2e
        count!!.count_f3e = curr_val_widget!!.parameterValuef3e
        count!!.count_pe = curr_val_widget!!.parameterValuepe
        count!!.count_le = curr_val_widget!!.parameterValuele
        count!!.count_ee = curr_val_widget!!.parameterValueee
        count!!.notes = enw!!.sectionNotes
        countDataSource!!.saveCount(count!!)

        /*
        * Get all the alerts from the dynamic_widget_area and save each one.
        * If it has an id value set to anything higher than 0 then it should be an update, if it is 0
        * then it's a new alert and should be created instead.
        */
        val childcount = dynamic_widget_area!!.childCount
        for (i in 0 until childcount) {
            val acw = dynamic_widget_area!!.getChildAt(i) as AlertCreateWidget
            if (isNotEmpty(acw.alertName)) {
                // save or create
                if (acw.alertId == 0) {
                    alertDataSource!!.createAlert(count_id, acw.alertValue, acw.alertName)
                } else {
                    alertDataSource!!.saveAlert(acw.alertId, acw.alertValue, acw.alertName)
                }
            } else {
                if (MyDebug.LOG) Log.d(TAG, "Failed to save alert: " + acw.alertId)
            }
        }
    }

    // Scroll to end of view, by wmstein
    fun scrollToEndOfView(scrlV: View) {
        var scroll_amount = scrlV.bottom
        var scrollY = scroll_amount
        var pageend = false
        while (!pageend) {
            scrlV.scrollTo(0, scroll_amount) //scroll
            scroll_amount = scroll_amount + scroll_amount //increase scroll_amount
            scrollY = scrollY + scrlV.scrollY //scroll position of 1. row
            if (scroll_amount > scrollY) {
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
        dynamic_widget_area!!.addView(acw)
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
            //Log.i(TAG, "(1) View tag was " + String.valueOf(deleteAnAlert));
            // the actual AlertCreateWidget is two levels up from the button in which it is embedded
            dynamic_widget_area!!.removeView(view.parent.parent as AlertCreateWidget)
        } else {
            //Log.i(TAG, "(2) View tag was " + String.valueOf(deleteAnAlert));
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
                    dynamic_widget_area!!.removeView(markedForDelete!!.parent.parent as AlertCreateWidget)
                } catch (e: Exception) {
                    if (MyDebug.LOG) Log.e(TAG, "Failed to delete a widget: $e")
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
            intent.putExtra("section_id", section_id)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NavUtils.navigateUpTo(this, intent)
        } else if (id == R.id.menuSaveExit) {
            saveData()
            super.finish()
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        val counting_screen = findViewById<ScrollView>(R.id.count_options)
        counting_screen.background = null
        brightPref = prefs.getBoolean("pref_bright", true)
        bMap = transektCount!!.decodeBitmap(
            R.drawable.kbackground,
            transektCount!!.width,
            transektCount!!.height
        )
        bg = BitmapDrawable(counting_screen.resources, bMap)
        counting_screen.background = bg
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
        fun isEmpty(cs: CharSequence?): Boolean {
            return cs == null || cs.length == 0
        }
    }

}