package com.wmstein.transektcount

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

/**************************************************************************************
 * EditMetaActivity collects, partly edits and shows metadata for a transect inspection
 *
 * Created by wmstein on 2016-03-31,
 * last edited in Java on 2026-05-23,
 * converted to kotlin on 2026-07-17,
 * last edited on 2026-07-21
 */
class EditMetaActivity : AppCompatActivity() {
    // Preferences
    private val prefs = getPrefs()

    // Data from DB tables
    private var head: Head? = null
    private var meta: Meta? = null
    private var headDataSource: HeadDataSource? = null
    private var metaDataSource: MetaDataSource? = null

    private var pdate: Calendar? = null
    private var ptime: Calendar? = null

    private var metaArea: LinearLayout? = null
    private var sDate: TextView? = null
    private var sTime: TextView? = null
    private var eTime: TextView? = null

    private var ehw: EditMetaHeadWidget? = null
    private var emw: EditMetaWidget? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "77, onCreate")

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
                        Log.i(TAG,"120, handleOnBackPressed")

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
            val resources = this.getResources()
            @SuppressLint("DiscouragedApi") val resourceId = resources.getIdentifier(
                "config_navBarInteractionMode",
                "integer", "android"
            )
            return if (resourceId > 0) resources.getInteger(resourceId) else 0
        }

    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "145, onResume")

        // Setup data sources
        headDataSource!!.open()
        metaDataSource!!.open()

        // Build the Edit Metadata screen
        // Clear existing view
        metaArea!!.removeAllViews()

        // Load head and metadata
        head = headDataSource!!.head
        meta = metaDataSource!!.meta

        // Display the editable head data
        ehw = EditMetaHeadWidget(this, null)
        ehw!!.setWidgetNo(getString(R.string.transectnumber) + ":")
        ehw!!.widgetNo1 = head!!.transect_no
        ehw!!.setWidgetName(getString(R.string.inspector) + ":")
        ehw!!.widgetName1 = head!!.inspector_name
        metaArea!!.addView(ehw)

        // Display the editable metadata
        emw = EditMetaWidget(this, null)
        emw!!.setWidgetTemp1(getString(R.string.temperature) + ":")
        emw!!.setWidgetWind1(getString(R.string.wind) + ":")
        emw!!.setWidgetClouds1(getString(R.string.clouds) + ":")

        emw!!.setWidgetStartTemp2(meta!!.temps)
        emw!!.setWidgetEndTemp2(meta!!.tempe)
        emw!!.setWidgetStartWind2(meta!!.winds)
        emw!!.setWidgetEndWind2(meta!!.winde)
        emw!!.setWidgetStartClouds2(meta!!.clouds)
        emw!!.setWidgetEndClouds2(meta!!.cloude)

        emw!!.setWidgetDate1(getString(R.string.date) + ":")
        emw!!.setWidgetDate2(meta!!.date)
        emw!!.setWidgetSTime1(getString(R.string.starttm))
        emw!!.setWidgetSTime2(meta!!.start_tm)
        emw!!.setWidgetETime1(getString(R.string.endtm))
        emw!!.setWidgetETime2(meta!!.end_tm)
        emw!!.setWidgetNote1(getString(R.string.note) + ":")
        emw!!.setWidgetNote2(meta!!.note)
        metaArea!!.addView(emw)

        // Check for focus
        val newTransectNo = head!!.transect_no
        if (newTransectNo != "") {
            emw!!.requestFocus()
        } else {
            ehw!!.requestFocus()
        }

        pdate = Calendar.getInstance()
        ptime = Calendar.getInstance()

        sDate = findViewById(R.id.widgetDate2)
        sTime = findViewById(R.id.widgetSTime2)
        eTime = findViewById(R.id.widgetETime2)

        // Get current date by click
        sDate!!.setOnClickListener { _: View? ->
            val date = Date()
            sDate!!.text = getFormDate(date)
        }

        // Get date picker result
        val dpd =
            OnDateSetListener { _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                pdate!!.set(Calendar.YEAR, year)
                pdate!!.set(Calendar.MONTH, monthOfYear)
                pdate!!.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val date = pdate!!.getTime()
                sDate!!.text = getFormDate(date)
            }

        // Select date by long click
        sDate!!.setOnLongClickListener { _: View? ->
            DatePickerDialog(
                this@EditMetaActivity, dpd,
                pdate!!.get(Calendar.YEAR),
                pdate!!.get(Calendar.MONTH),
                pdate!!.get(Calendar.DAY_OF_MONTH)
            ).show()
            true
        }

        // Get current start time
        sTime!!.setOnClickListener { _: View? ->
            val date = Date()
            sTime!!.text = getFormTime(date)
        }

        // Get start time picker result
        val stpd = OnTimeSetListener { _: TimePicker?, hourOfDay: Int, minute: Int ->
            ptime!!.set(Calendar.HOUR_OF_DAY, hourOfDay)
            ptime!!.set(Calendar.MINUTE, minute)
            val date = ptime!!.getTime()
            sTime!!.text = getFormTime(date)
        }

        // Select start time
        sTime!!.setOnLongClickListener { _: View? ->
            TimePickerDialog(
                this@EditMetaActivity, stpd,
                ptime!!.get(Calendar.HOUR_OF_DAY),
                ptime!!.get(Calendar.MINUTE),
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
        val etpd = OnTimeSetListener { _: TimePicker?, hourOfDay: Int, minute: Int ->
            ptime!!.set(Calendar.HOUR_OF_DAY, hourOfDay)
            ptime!!.set(Calendar.MINUTE, minute)
            val date = ptime!!.getTime()
            eTime!!.text = getFormTime(date)
        }

        // Select end time
        eTime!!.setOnLongClickListener { _: View? ->
            TimePickerDialog(
                this@EditMetaActivity, etpd,
                ptime!!.get(Calendar.HOUR_OF_DAY),
                ptime!!.get(Calendar.MINUTE),
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
                Log.i(TAG, "296, MenuItem home")

            finish()
            return true
        }

        if (id == R.id.menuSaveExit) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "304, MenuItem saveExit")

            if (saveData()) finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "316, onPause")

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
            Log.i(TAG, "336, onStop")

        metaArea = null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "345, onDestroy")
    }

    fun saveData(): Boolean {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "350, saveData")

        // Save head data
        head!!.transect_no = ehw!!.widgetNo1
        head!!.inspector_name = ehw!!.widgetName1

        headDataSource!!.saveHead(head!!)

        var mesg: String

        // Save metadata with plausi
        meta!!.temps = emw!!.widgetTemps
        meta!!.tempe = emw!!.widgetTempe
        if (meta!!.temps !in 0..50 || meta!!.tempe !in 0..50) {
            mesg = getString(R.string.valTemp)
            Toast.makeText(this,  //orange
                fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG).show()
            return false
        }

        meta!!.winds = emw!!.widgetWinds
        meta!!.winde = emw!!.widgetWinde
        if (meta!!.winds !in 0..4 || meta!!.winde !in 0..4) {
            mesg = getString(R.string.valWind)
            Toast.makeText(this,  //orange
                fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG).show()
            return false
        }

        meta!!.clouds = emw!!.widgetClouds
        meta!!.cloude = emw!!.widgetCloude
        if (meta!!.clouds !in 0..100 || meta!!.cloude !in 0..100) {
            mesg = getString(R.string.valClouds)
            Toast.makeText(this,  //orange
                fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG).show()
            return false
        }

        meta!!.date = emw!!.widgetDate
        meta!!.start_tm = emw!!.widgetSTime
        meta!!.end_tm = emw!!.widgetETime
        meta!!.note = emw!!.widgetNote

        metaDataSource!!.saveMeta(meta!!)
        return true
    }
    // End of saveData()

    companion object {
        private const val TAG = "EditMetaAct"

        // Formatted date
        fun getFormDate(date: Date): String {
            val dform: DateFormat?
            val lng = Locale.getDefault().toString().substring(0, 2)

            dform = when (lng) {
                "de" -> SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
                "en" -> SimpleDateFormat("yyyy-MM-dd", Locale.US)
                else  // for fr, it and es
                    -> SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
            }
            return dform.format(date)
        }

        // Derive start_tm and end_tm from date
        fun getFormTime(date: Date): String {
            val dform: DateFormat = SimpleDateFormat("HH:mm", Locale.US)
            return dform.format(date)
        }
    }

}
