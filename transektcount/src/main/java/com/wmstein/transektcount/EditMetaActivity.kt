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
 * EditMetaActivity collects, partly edits and shows metadata for a transect inspection
 *
 * Created by wmstein on 2016-03-31,
 * last edited in Java on 2026-05-23,
 * converted to kotlin on 2026-07-17,
 * last edited on 2026-10-21.
 **************************************************************************************************/

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG

import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

import com.wmstein.transektcount.TransektCountApplication.Companion.getPrefs
import com.wmstein.transektcount.Utils.fromHtml
import com.wmstein.transektcount.database.Head
import com.wmstein.transektcount.database.HeadDataSource
import com.wmstein.transektcount.database.Meta
import com.wmstein.transektcount.database.MetaDataSource
import com.wmstein.transektcount.widgets.EditMetaHeadWidget
import com.wmstein.transektcount.widgets.EditMetaWidget

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditMetaActivity : AppCompatActivity() {
    private val prefs = getPrefs()

    // Data from DB tables
    private var head: Head? = null
    private var meta: Meta? = null
    private var headDataSource: HeadDataSource? = null
    private var metaDataSource: MetaDataSource? = null

    private var pDate: Calendar? = null
    private var pTime: Calendar? = null

    private var metaArea: LinearLayout? = null
    private var sDate: TextView? = null
    private var sTime: TextView? = null
    private var eTime: TextView? = null

    private var eMHW: EditMetaHeadWidget? = null
    private var eMW: EditMetaWidget? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "92, onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)  // SDK 35+
        {
            this.enableEdgeToEdge()
        }
        setContentView(R.layout.activity_edit_meta)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editHeadScreen))
        { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }
            WindowInsetsCompat.CONSUMED
        }

        // Option for full bright screen
        val brightPref = prefs.getBoolean("pref_bright", true)

        // Set full brightness of screen
        if (brightPref) {
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        metaArea = findViewById(R.id.meta_area)

        supportActionBar!!.title = getString(R.string.editHeadTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        headDataSource = HeadDataSource(this)
        metaDataSource = MetaDataSource(this)

        // New onBackPressed logic
        if (this.navBarMode == 0 || this.navBarMode == 1) {
            val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG,"135, handleOnBackPressed")

                    finish()
                    remove()
                }
            }
            onBackPressedDispatcher.addCallback(this, callback)
        }
    }
    // End of onCreate()

    val navBarMode: Int
        get() {
            val resources = this.resources
            @SuppressLint("DiscouragedApi") val resourceId = resources.getIdentifier(
                "config_navBarInteractionMode",
                "integer", "android"
            )
            return if (resourceId > 0) resources.getInteger(resourceId) else 0
        }

    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "160, onResume")

        // Setup data sources
        headDataSource!!.open()
        metaDataSource!!.open()

        // Build the Edit Metadata screen
        // Clear existing view
        metaArea!!.removeAllViews()

        // Load head and metadata
        head = headDataSource!!.head
        meta = metaDataSource!!.meta

        // Display the head data
        eMHW = EditMetaHeadWidget(this, null)
        eMHW!!.setWidgetNo(getString(R.string.transectnumber) + ":")
        eMHW!!.setWidgetName(getString(R.string.inspector) + ":")

        // Display the editable head data
        eMHW!!.widgetNo1 = head!!.transect_no
        eMHW!!.widgetName1 = head!!.inspector_name
        metaArea!!.addView(eMHW)

        // Display the metadata
        eMW = EditMetaWidget(this, null)
        eMW!!.setWidgetTemp1(getString(R.string.temperature) + ":")
        eMW!!.setWidgetWind1(getString(R.string.wind) + ":")
        eMW!!.setWidgetClouds1(getString(R.string.clouds) + ":")

        // Display the editable metadata
        eMW!!.widgetTempS = meta!!.temps
        eMW!!.widgetTempE = meta!!.tempe

        eMW!!.widgetWinds = meta!!.winds
        eMW!!.widgetWinde = meta!!.winde

        eMW!!.widgetCloudsS = meta!!.clouds
        eMW!!.widgetCloudsE = meta!!.cloude

        eMW!!.setWidgetDate1(getString(R.string.date) + ":")
        eMW!!.setWidgetDate2(meta!!.date)
        eMW!!.setWidgetSTime1(getString(R.string.starttm))
        eMW!!.setWidgetSTime2(meta!!.start_tm)
        eMW!!.setWidgetETime1(getString(R.string.endtm))
        eMW!!.setWidgetETime2(meta!!.end_tm)

        eMW!!.setWidgetNote1(getString(R.string.note) + ":")
        eMW!!.setWidgetNote2(meta!!.note)
        metaArea!!.addView(eMW)

        // Check for focus
        val newTransectNo = head!!.transect_no
        if (newTransectNo != "") {
            eMW!!.requestFocus()
        } else {
            eMHW!!.requestFocus()
        }

        pDate = Calendar.getInstance()
        pTime = Calendar.getInstance()

        sDate = findViewById(R.id.widgetDateValue)
        sTime = findViewById(R.id.widgetStartTimeValue)
        eTime = findViewById(R.id.widgetEndTimeValue)

        // Get current date by click
        sDate!!.setOnClickListener { _: View? ->
            val date = Date()
            sDate!!.text = getFormDate(date)
        }

        // Get date picker result
        val dpd =
            OnDateSetListener { _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                pDate!!.set(Calendar.YEAR, year)
                pDate!!.set(Calendar.MONTH, monthOfYear)
                pDate!!.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val date = pDate!!.getTime()
                sDate!!.text = getFormDate(date)
            }

        // Select date by long click
        sDate!!.setOnLongClickListener { _: View? ->
            DatePickerDialog(
                this@EditMetaActivity, dpd,
                pDate!!.get(Calendar.YEAR),
                pDate!!.get(Calendar.MONTH),
                pDate!!.get(Calendar.DAY_OF_MONTH)
            ).show()
            true
        }

        // Get current start time
        sTime!!.setOnClickListener { _: View? ->
            val date = Date()
            sTime!!.text = getFormTime(date)
        }

        // Get start time picker result
        val sTP = OnTimeSetListener { _: TimePicker?, hourOfDay: Int, minute: Int ->
            pTime!!.set(Calendar.HOUR_OF_DAY, hourOfDay)
            pTime!!.set(Calendar.MINUTE, minute)
            val date = pTime!!.getTime()
            sTime!!.text = getFormTime(date)
        }

        // Select start time
        sTime!!.setOnLongClickListener { _: View? ->
            TimePickerDialog(
                this@EditMetaActivity, sTP,
                pTime!!.get(Calendar.HOUR_OF_DAY),
                pTime!!.get(Calendar.MINUTE),
                true
            ).show()
            true
        }

        // Get current end time
        eTime!!.setOnClickListener { _: View? ->
            val date = Date()
            eTime!!.text = getFormTime(date)
        }

        // Get start time picker result
        val eTP = OnTimeSetListener { _: TimePicker?, hourOfDay: Int, minute: Int ->
            pTime!!.set(Calendar.HOUR_OF_DAY, hourOfDay)
            pTime!!.set(Calendar.MINUTE, minute)
            val date = pTime!!.getTime()
            eTime!!.text = getFormTime(date)
        }

        // Select end time
        eTime!!.setOnLongClickListener { _: View? ->
            TimePickerDialog(
                this@EditMetaActivity, eTP,
                pTime!!.get(Calendar.HOUR_OF_DAY),
                pTime!!.get(Calendar.MINUTE),
                true
            ).show()
            true
        }
    }
    // End of onResume()

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.edit_meta, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId
        if (id == android.R.id.home)  // back button in actionBar
        {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "317, MenuItem home")

            finish()
            return true
        }

        if (id == R.id.menuSaveExit) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "335, MenuItem saveExit")

            if (saveData()) finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "337, onPause")

        headDataSource!!.close()
        metaDataSource!!.close()

        sDate!!.setOnClickListener(null)
        sDate!!.setOnLongClickListener(null)
        sTime!!.setOnClickListener(null)
        sTime!!.setOnLongClickListener(null)
        eTime!!.setOnClickListener(null)
        eTime!!.setOnLongClickListener(null)

        metaArea!!.clearFocus()
        metaArea!!.removeAllViews()
    }

    override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "357, onStop")

        metaArea = null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "366, onDestroy")
    }

    fun saveData(): Boolean {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "371, saveData")

        // Save head data
        head!!.transect_no = eMHW!!.widgetNo1
        head!!.inspector_name = eMHW!!.widgetName1

        headDataSource!!.saveHead(head!!)

        var msg: String

        // Save metadata with plausi
        if (eMW!!.widgetTempS !in 0..50 || eMW!!.widgetTempE !in 0..50) {
            msg = getString(R.string.valTemp)
            Toast.makeText(this,  //orange
                fromHtml("<font color='#ff6000'><b>$msg</b></font>"),
                LENGTH_LONG).show()
            return false
        } else {
            meta!!.temps = eMW!!.widgetTempS
            meta!!.tempe = eMW!!.widgetTempE
        }

        if (eMW!!.widgetWinds !in 0..4 || eMW!!.widgetWinde !in 0..4) {
            msg = getString(R.string.valWind)
            Toast.makeText(this,  //orange
                fromHtml("<font color='#ff6000'><b>$msg</b></font>"),
                LENGTH_LONG).show()
            return false
        } else {
            meta!!.winds = eMW!!.widgetWinds
            meta!!.winde = eMW!!.widgetWinde
        }

        if (eMW!!.widgetCloudsS !in 0..100 || eMW!!.widgetCloudsE !in 0..100) {
            msg = getString(R.string.valClouds)
            Toast.makeText(this,  //orange
                fromHtml("<font color='#ff6000'><b>$msg</b></font>"),
                LENGTH_LONG).show()
            return false
        } else {
            meta!!.clouds = eMW!!.widgetCloudsS
            meta!!.cloude = eMW!!.widgetCloudsE
        }

        meta!!.date = eMW!!.widgetDate
        meta!!.start_tm = eMW!!.widgetSTime
        meta!!.end_tm = eMW!!.widgetETime
        meta!!.note = eMW!!.widgetNote

        metaDataSource!!.saveMeta(meta!!)
        return true
    }
    // End of saveData()

    // Formatted date
    fun getFormDate(date: Date): String {
        val dFormDate: DateFormat?
        val lng = Locale.getDefault().toString().substring(0, 2)

        dFormDate = when (lng) {
            "de" -> SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
            "en" -> SimpleDateFormat("yyyy-MM-dd", Locale.US)
            else  // for fr, it and es
                -> SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
        }
        return dFormDate.format(date)
    }

    // Derive start_tm and end_tm from date
    fun getFormTime(date: Date): String {
        val dFormTime: DateFormat = SimpleDateFormat("HH:mm", Locale.US)
        return dFormTime.format(date)
    }

    companion object {
        private const val TAG = "EditMetaAct"
    }

}
