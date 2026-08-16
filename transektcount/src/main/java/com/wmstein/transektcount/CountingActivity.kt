package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.database.CursorIndexOutOfBoundsException
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

import com.wmstein.transektcount.TransektCountApplication.Companion.getPrefs
import com.wmstein.transektcount.Utils.fromHtml
import com.wmstein.transektcount.database.Count
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.HeadDataSource
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.widgets.CountingExternWidget
import com.wmstein.transektcount.widgets.CountingHead1Widget
import com.wmstein.transektcount.widgets.CountingHead2Widget
import com.wmstein.transektcount.widgets.CountingHead3Widget
import com.wmstein.transektcount.widgets.CountingInternWidget
import com.wmstein.transektcount.widgets.CountingLhExternWidget
import com.wmstein.transektcount.widgets.CountingLhInternWidget
import com.wmstein.transektcount.widgets.NotesWidget

/******************************************************************************
 * CountingActivity is the central activity of TransektCount and is called from
 * SelectSectionActivity for the selected section.
 * It provides 12 counters and calls AddSpeciesActivity, DelSpeciesActivity,
 * EditSectionListActivity and CountOptionsActivity.
 * It switches screen off when the device is pocketed
 * and allows taking pictures and sending notes.
 *
 * CountingActivity uses CountingWidget*.kt, NotesWidget.kt and activity_counting*.xml
 *
 * Basic counting functions created by milo for BeeCount on 2014-05-05.
 * Adopted, modified and enhanced for TransektCount by wmstein since 2016-02-18,
 * last edited in Java on 2026-06-08,
 * converted to Kotlin on 2026-07-17,
 * last edited on 2026-07-21.
 */
class CountingActivity : AppCompatActivity(), OnSharedPreferenceChangeListener, SensorEventListener {
    private var countScreen: LinearLayout? = null
    private var countsFieldHeadArea1: LinearLayout? = null // headline internal/external
    private var countsFieldArea1: LinearLayout? = null // internal counts field
    private var countsFieldHeadArea2: LinearLayout? = null // headline external
    private var countsFieldArea2: LinearLayout? = null // external counts field
    private var speciesNotesArea: LinearLayout? = null // species notes line

    // Data
    private var iid = 1 // count ID
    private var itemPosition = 0 // 0 = 1. position of selected species in array for Spinner
    private var sectionId = 0 // section ID
    private var count: Count? = null // record in SQLite counts table for a counted species
    private var section: Section? = null // record in SQLite sections table
    private var spinner: Spinner? = null // species selector
    private var oldCounter = 0
    private var newCounter = 0

    // CountingWidgets
    private lateinit var countingWidgetI: MutableList<CountingInternWidget>
    private lateinit var countingWidgetE: MutableList<CountingExternWidget>
    private lateinit var countingWidgetLHI: MutableList<CountingLhInternWidget>
    private lateinit var countingWidgetLHE: MutableList<CountingLhExternWidget>

    // Proximity sensor handling for screen on/off
    private var powerManager: PowerManager? = null
    private var proximityWakeLock: WakeLock? = null
    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null

    // Preferences
    private var prefs = getPrefs()
    private var editor = prefs.edit()
    private var awakePref = false
    private var brightPref = false
    private var sortPref = ""
    private var lhandPref = false // true for lefthand mode of counting screen
    private var buttonSoundPref = false
    private var buttonVibPref = false
    private var specCode = ""
    private var proxSensorPref = ""
    private var sensorSensitivity = 0.0

    // Data sources
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null
    private var headDataSource: HeadDataSource? = null

    private lateinit var soundService: SoundService

    private lateinit var vibrator: Vibrator
    
    private var mesg = ""

    private val mHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) 
            Log.i(TAG, "133, onCreate")

        val transektCount = application as TransektCountApplication
        prefs = getPrefs()
        setPrefVariables() // set all stored preferences into their variables

        // Get values from calling activity
        val extras = intent.extras
        if (extras != null) {
            sectionId = extras.getInt("section_id")
            itemPosition = extras.getInt("item_position")
        } else {
            sectionId = 1
            itemPosition = 0
        }

        sectionDataSource = SectionDataSource(this)
        countDataSource = CountDataSource(this)
        headDataSource = HeadDataSource(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)  // SDK 35+
            this.enableEdgeToEdge()

        // Distinguish between left-/ right-handed counting page layout
        if (lhandPref) {
            setContentView(R.layout.activity_counting_lh)
            countScreen = findViewById(R.id.countingScreenLH)
            countScreen!!.background = transektCount.setBackgr()
            countsFieldHeadArea1 = findViewById(R.id.countsFieldHead1LH)
            countsFieldArea1 = findViewById(R.id.countsField1LH)
            countsFieldHeadArea2 = findViewById(R.id.countsFieldHead2LH)
            countsFieldArea2 = findViewById(R.id.countsField2LH)
            speciesNotesArea = findViewById(R.id.speciesNotesLH)
            
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.countingScreenLH))
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
        } else {
            setContentView(R.layout.activity_counting)
            countScreen = findViewById(R.id.countingScreen)
            countScreen!!.background = transektCount.setBackgr()
            countsFieldHeadArea1 = findViewById(R.id.countsFieldHead1RH)
            countsFieldArea1 = findViewById(R.id.countsField1RH)
            countsFieldHeadArea2 = findViewById(R.id.countsFieldHead2RH)
            countsFieldArea2 = findViewById(R.id.countsField2RH)
            speciesNotesArea = findViewById(R.id.speciesNotesRH)

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.countingScreen))
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
        }

        // Proximity sensor handling screen on/off
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        powerManager = this.getSystemService(POWER_SERVICE) as PowerManager
        proximityWakeLock =
            if (powerManager!!.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK))
                powerManager!!.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "TransektCount:WAKELOCK")
            else null

        // Get max. proximity sensitivity
        val sensorSensitivityMax: Double = 
            if (proximitySensor != null)
                proximitySensor!!.maximumRange.toDouble()
            else 0.0

        // Get proximity sensitivity selection from preferences
        if (sensorSensitivityMax != 0.0) {
            // Set sensorSensitivity proportional to value for max. sensitivity
            when (proxSensorPref) {
                "Off" -> sensorSensitivity = 0.0
                "Medium" -> sensorSensitivity = sensorSensitivityMax / 2
                "High" -> sensorSensitivity = sensorSensitivityMax - 0.1
            }
        }

        // Start button sound service
        if (buttonSoundPref) soundService = SoundService(applicationContext)

        // new onBackPressed logic
        // Different Navigation Bar modes and layouts:
        // - Classic three-button navigation: NavBarMode = 0
        // - Two-button navigation (Android P): NavBarMode = 1
        // - Full screen gesture mode (Android Q): NavBarMode = 2
        // Use only if NavBarMode = 0 or 1.
        if (this.navBarMode == 0 || this.navBarMode == 1) {
            val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    disableProximitySensor()
                    val intent = Intent(this@CountingActivity,
                        SelectSectionActivity::class.java)
                    startActivity(intent)
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

    // Load preferences at start, and also when a change is detected
    private fun setPrefVariables() {
        awakePref = prefs.getBoolean("pref_awake", true)
        brightPref = prefs.getBoolean("pref_bright", true)
        sortPref = prefs.getString("pref_sort_sp", "none")!! // sorted species list on counting page
        lhandPref = prefs.getBoolean("pref_left_hand", false) // left-handed counting page
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false)
        buttonVibPref = prefs.getBoolean("pref_button_vib", false)
        proxSensorPref = prefs.getString("pref_prox", "Off")!!
    }

    @SuppressLint("DiscouragedApi")
    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) 
            Log.i(TAG, "279, onResume")

        sensorManager!!.registerListener(this,
            proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)

        prefs = getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
        setPrefVariables() // set prefs into their variables

        // Prepare vibrator service
        if (buttonVibPref) vibrator =
            applicationContext.getSystemService(Vibrator::class.java)

        // Set full brightness of screen
        if (brightPref) {
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        if (awakePref) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Prepare the counting screen
        //   Clear any existing views
        countsFieldHeadArea1!!.removeAllViews() // headline internal/external
        countsFieldArea1!!.removeAllViews() // internal counts
        countsFieldHeadArea2!!.removeAllViews() // headline for external counts
        countsFieldArea2!!.removeAllViews() // external counts
        speciesNotesArea!!.removeAllViews() // species remarks

        // Set up the data sources
        sectionDataSource!!.open()
        countDataSource!!.open()

        try {
            section = sectionDataSource!!.getSection(sectionId)
        } catch (_: CursorIndexOutOfBoundsException) {
            mesg = getString(R.string.getHelp)
            Toast.makeText(this,
                fromHtml("<font color='#008000'>$mesg</font>"),
                Toast.LENGTH_LONG).show()
            disableProximitySensor()
            finish()
        }

        // Load and show the data,
        //   set title in ActionBar
        supportActionBar!!.title = section!!.name
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val idArray: Array<String?>?
        val nameArray: Array<String?>?
        val nameArrayL: Array<String?>?
        val codeArray: Array<String?>?

        when (sortPref) {
            "names_alpha" -> {
                idArray = countDataSource!!.getAllIdsForSectionSrtName(section!!.id)
                nameArray = countDataSource!!.getAllStringsForSectionSrtName(section!!.id, "name")
                codeArray = countDataSource!!.getAllStringsForSectionSrtName(section!!.id, "code")
                nameArrayL =
                    countDataSource!!.getAllStringsForSectionSrtName(section!!.id, "name_g")
            }

            "codes" -> {
                idArray = countDataSource!!.getAllIdsForSectionSrtCode(section!!.id)
                nameArray = countDataSource!!.getAllStringsForSectionSrtCode(section!!.id, "name")
                codeArray = countDataSource!!.getAllStringsForSectionSrtCode(section!!.id, "code")
                nameArrayL =
                    countDataSource!!.getAllStringsForSectionSrtCode(section!!.id, "name_g")
            }

            else -> {
                idArray = countDataSource!!.getAllIdsForSection(section!!.id)
                nameArray = countDataSource!!.getAllStringsForSection(section!!.id, "name")
                codeArray = countDataSource!!.getAllStringsForSection(section!!.id, "code")
                nameArrayL = countDataSource!!.getAllStringsForSection(section!!.id, "name_g")
            }
        }

        var rName: String
        var resId: Int
        val resId0: Int = getResources().getIdentifier(
            "p00000", "drawable", this.packageName
        )

        val imageArray = arrayOfNulls<Int>(codeArray.size)
        for ((iPic, code) in codeArray.withIndex()) {
            rName = "p$code"
            resId = getResources().getIdentifier(
                rName, "drawable",
                this.packageName
            )
            if (resId != 0) imageArray[iPic] = resId
            else imageArray[iPic] = resId0
        }

        countingWidgetI = ArrayList()
        countingWidgetE = ArrayList()
        countingWidgetLHI = ArrayList()
        countingWidgetLHE = ArrayList()

        // Show head2: Species with spinner to select
        spinner = if (lhandPref)  // if left-handed counting page
            findViewById(R.id.countHead1SpinnerLH)
        else findViewById(R.id.countHead1Spinner)

        // Get itemPosition of the species to be shown from sharedPreference
        if (prefs.getString("new_spec_code", "")!! != "") {
            specCode = prefs.getString("new_spec_code", "")!!
            editor.putString("new_spec_code", "") // clear prefs value after use
            editor.apply()
        }

        if (specCode != "") {
            var i = 0
            while (i <= codeArray.size) {
                if (specCode == codeArray[i]) {
                    itemPosition = i
                    break
                }
                i++
            }
            specCode = ""
        }

        // Set spinner part of the counting screen by itemPosition
        @Suppress("UNCHECKED_CAST") val adapter = CountingHead1Widget(
            this,
            idArray as Array<String>, nameArray as Array<String>, nameArrayL as Array<String>,
            codeArray as Array<String>, imageArray as Array<Int>
        )
        spinner!!.adapter = adapter
        spinner!!.setSelection(itemPosition) // from savedInstanceState
        spinnerListener()
    }
    // End of onResume()

    // Proximity sensor handling
    override fun onSensorChanged(event: SensorEvent) {
        if (proximitySensor != null) {
            if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) 
                    Log.i(TAG, ("422 Value0: " + event.values[0] + ", "
                            + "Sensitivity: " + (sensorSensitivity)))

                // if ([0|5] >= [-0|-2.5|-4.9] && [0|5] < [0|2.5|4.9])
                val sensi = event.values[0]
                if (sensi >= -sensorSensitivity && sensi < sensorSensitivity) {
                    // near
                    if (proximityWakeLock == null) 
                        proximityWakeLock = powerManager!!.newWakeLock(
                        PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                        "TransektCount:WAKELOCK")

                    if (!proximityWakeLock!!.isHeld)
                        proximityWakeLock!!.acquire(30 * 60 * 1000L) // 30 minutes
                } else {
                    // far
                    disableProximitySensor()
                }
            }
        }
    }

    // Necessary for SensorEventListener
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun disableProximitySensor() // far
    {
        if (proximityWakeLock == null) return
        if (proximityWakeLock!!.isHeld) {
            val flags = PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY
            proximityWakeLock!!.release(flags)
            proximityWakeLock = null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.counting, menu)
        return true
    }

    // Handle menu selections
    @SuppressLint("QueryPermissionsNeeded")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home  // back button in actionBar
                -> {
                disableProximitySensor()

                val intent = Intent(this@CountingActivity, SelectSectionActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                return true
            }

            R.id.menuAddSpecies -> {
                disableProximitySensor()

                val intent = Intent(this@CountingActivity, AddSpeciesActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.putExtra("section_id", sectionId)
                intent.putExtra("search_Chars", "")
                startActivity(intent)
                return true
            }

            R.id.menuDelSpecies -> {
                disableProximitySensor()

                mesg = getString(R.string.wait)
                Toast.makeText(
                    this,
                    fromHtml("<font color='blue'>$mesg</font>"),
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(this@CountingActivity, DelSpeciesActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.putExtra("section_id", sectionId)
                intent.putExtra("search_Chars", "")

                // Trick: Pause for 100 msec to show toast
                mHandler.postDelayed({ startActivity(intent) }, 100)
                return true
            }

            R.id.menuEditSection -> {
                disableProximitySensor()

                mesg = getString(R.string.wait)
                Toast.makeText(
                    this,
                    fromHtml("<font color='blue'>$mesg</font>"),
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(this@CountingActivity, EditSectionListActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.putExtra("section_id", sectionId)
                intent.putExtra("search_Chars", "")

                // Trick: Pause for 100 msec to show toast
                mHandler.postDelayed({ startActivity(intent) }, 100)
                return true
            }

            R.id.menuTakePhoto -> {
                val camIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)

                val packageManager = getPackageManager()
                val activities = packageManager.queryIntentActivities(
                    camIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )

                // Select from available camera apps
                val isIntentSafe = activities.isNotEmpty()
                if (isIntentSafe) {
                    val title = getResources().getString(R.string.chooserTitle)
                    val chooser = Intent.createChooser(camIntent, title)
                    if (camIntent.resolveActivity(getPackageManager()) != null) {
                        try {
                            startActivity(chooser)
                        } catch (_: Exception) {
                            mesg = getString(R.string.noPhotoPermit)
                            Toast.makeText(
                                this,
                                fromHtml("<font color='red'><b>$mesg</b></font>"),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    // Only default camera available
                    startActivity(camIntent)
                }
                return true
            }

            R.id.action_share -> {
                headDataSource!!.open()
                val head = headDataSource!!.head
                headDataSource!!.close()
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "TransektCount " + head.transect_no)
                sendIntent.putExtra(Intent.EXTRA_TITLE, "Message of TransektCount")
                sendIntent.putExtra(Intent.EXTRA_TEXT, section!!.name + ": ")
                sendIntent.type = "text/plain"
                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)))
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }
    // End of onOptionsItemSelected()

    // Edit count options by CountOptionsActivity by button in widget_counting_head2.xml
    fun editOptions(view: View?) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "584, editOptions, View: $view")

        disableProximitySensor()

        val intent = Intent(this@CountingActivity, CountOptionsActivity::class.java)
        intent.putExtra("count_id", iid)
        intent.putExtra("section_id", sectionId)
        intent.putExtra("section_name", section!!.name)
        startActivity(intent)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        setPrefVariables()
    }

    // Save activity state for getting back to CountingActivity
    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt("section_id", sectionId)
        outState.putInt("count_id", iid)
        outState.putInt("item_position", itemPosition)
        outState.putString("new_spec_code", "")
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        sectionId = savedInstanceState.getInt("section_id")
        iid = savedInstanceState.getInt("count_id")
        itemPosition = savedInstanceState.getInt("item_position")
    }

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) 
            Log.i(TAG, "618, onPause")

        disableProximitySensor()

        // Close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()

        // On some Custom ROMS a wakelock might not be held, if wakelock permission is not granted
        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        prefs.unregisterOnSharedPreferenceChangeListener(this)
        sensorManager!!.unregisterListener(this)
    }
    // End of onPause()

    override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "640, onStop")

        countScreen!!.invalidate()

        if (buttonSoundPref) {
            soundService.releaseSoundM()
            soundService.releaseSoundP()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) 
            Log.i(TAG, "654, onDestroy")
    }

    // Spinner listener
    private fun spinnerListener() {
        spinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                aid: Long
            ) {
                try {
                    countsFieldHeadArea1!!.removeAllViews() // headline internal
                    countsFieldArea1!!.removeAllViews() // internal counts
                    countsFieldHeadArea2!!.removeAllViews() // headline for external counts
                    countsFieldArea2!!.removeAllViews() // external counts
                    speciesNotesArea!!.removeAllViews() // species remark

                    val sid =
                        (view.findViewById<View?>(R.id.countId) as TextView).text.toString()
                    iid = sid.toInt()
                    itemPosition = position // position of new selected item in Spinner

                    count = countDataSource!!.getCountById(iid)
                    countingScreen(count!!)
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) 
                        Log.i(TAG, ("681, SpinnerListener, count id: " + count!!.id
                                + ", code: " + count!!.code + ", name: " + count!!.name))
                } catch (e: Exception) {
                    // Exception may occur when permissions are changed while activity is paused
                    //  or when spinner is rapidly repeatedly pressed
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.e(TAG, "687, SpinnerListener, catch: $e"
                    )
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Stub, necessary to make Spinner work correctly when repeatedly used
            }
        }
    }

    // Show rest of widgets for counting screen
    private fun countingScreen(count: Count) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) 
            Log.i(TAG, "701, countingScreen")

        // 1. Species line is set by CountingHead1Widget in onResume, Spinner
        
        // 2. Headline Counting Area 1 (internal)
        val head2 = CountingHead2Widget(this, null)
        head2.setCountHead2(count)
        countsFieldHeadArea1!!.addView(head2)

        // 3. Counts internal
        if (lhandPref) // if left-handed counting page
        {
            val widgeti = CountingLhInternWidget(this, null)
            widgeti.setCountLHi(count)
            countingWidgetLHI.add(widgeti)
            countsFieldArea1!!.addView(widgeti)
        } else {
            val widgeti = CountingInternWidget(this, null)
            widgeti.setCounti(count)
            countingWidgetI.add(widgeti)
            countsFieldArea1!!.addView(widgeti)
        }

        // 4. Headline Counting Area 2 (external)
        val head3 = CountingHead3Widget(this, null)
        head3.setCountHead3()
        countsFieldHeadArea2!!.addView(head3)

        // 5. Counts external
        if (lhandPref) // if left-handed counting page
        {
            val widgete = CountingLhExternWidget(this, null)
            widgete.setCountLHe(count)
            countingWidgetLHE.add(widgete)
            countsFieldArea2!!.addView(widgete)
        } else {
            val widgete = CountingExternWidget(this, null)
            widgete.setCounte(count)
            countingWidgetE.add(widgete)
            countsFieldArea2!!.addView(widgete)
        }

        // 6. Species note widget if there are any notes
        if (count.notes != "") {
            val countNotes = NotesWidget(this, null)

            // Set notes with light blue introducer
            countNotes.setNotesC(getString(R.string.species_notes), count.notes)
            speciesNotesArea!!.addView(countNotes)
        }
    }
    // End of countingScreen

    // Get the referenced counting widgets
    // CountingWidget_i (internal, right-handed)
    private fun getCountFromIdI(id: Int): CountingInternWidget? {
        for (widget in countingWidgetI) {
            checkNotNull(widget.count)
            if (widget.count!!.id == id) return widget
        }
        return null
    }

    // CountingWidgetLH_i (internal, left-handed)
    private fun getCountFromIdLHI(id: Int): CountingLhInternWidget? {
        for (widget in countingWidgetLHI) {
            checkNotNull(widget.count)
            if (widget.count!!.id == id) return widget
        }
        return null
    }

    // CountingWidget_e (external, right-handed)
    private fun getCountFromIdE(id: Int): CountingExternWidget? {
        for (widget in countingWidgetE) {
            checkNotNull(widget.count)
            if (widget.count!!.id == id) return widget
        }
        return null
    }

    // CountingWidgetLH_e (external, left-handed)
    private fun getCountFromIdLHE(id: Int): CountingLhExternWidget? {
        for (widget in countingWidgetLHE) {
            checkNotNull(widget.count)
            if (widget.count!!.id == id) return widget
        }
        return null
    }

    /*****************************************************************
     * The functions below are triggered by the count buttons
     * on the righthand/lefthand (LH) views
     * 
     * countUpf1i is triggered by buttonUpf1i in widget_counting_i.xml
     * and widget_counting_lhi.xml
     */
    fun countUpf1i(view: View) {
        // Run reenterActivity to re-enter CountingActivity and so fix spinner's 1. misbehavior:
        //  no action by 1st click when previous species selected again
        val tempCountId = view.tag.toString().toInt()
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "803, countUpf1i, section Id: $sectionId, count Id: $tempCountId")

        val widget = getCountFromIdI(tempCountId)
        if (widget != null) {
            // Desperate workaround for spinner's 2. misbehavior:
            // When returning from species that got no count to previous selected species: 
            //   1st count button press is ignored,
            //   so use button sound only for 2nd press when actually counted
            oldCounter = count!!.count_f1i
            widget.countUpf1i() // count up and set value on screen
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f1i
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_f1i = newCounter
                buttonVib(200)
                checkNotNull(widget.count)
                if (buttonSoundPref) soundService.soundPlusButtonSound()

                // Save the data
                countDataSource!!.saveCountf1i(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countUpLHf1i(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f1i
            widget.countUpLHf1i()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f1i
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_f1i = newCounter
                buttonVib(200)
                checkNotNull(widget.count)
                if (buttonSoundPref) soundService.soundPlusButtonSound()

                countDataSource!!.saveCountf1i(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countDownf1i(view: View) {
        val tempCountId = view.tag.toString().toInt()
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) 
            Log.i(TAG,"856, countDownf1i, section Id: $sectionId, count Id: $tempCountId")

        val widget = getCountFromIdI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f1i
            widget.countDownf1i()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f1i
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_f1i = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountf1i(count!!)
            }
            reenterActivity()
        }
    }

    fun countDownLHf1i(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f1i
            widget.countDownLHf1i()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f1i
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_f1i = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountf1i(count!!)
            }
            reenterActivity()
        }
    }

    fun countUpf2i(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f2i
            widget.countUpf2i()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f2i
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_f2i = newCounter
                buttonVib(200)
                checkNotNull(widget.count)
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                countDataSource!!.saveCountf2i(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countUpLHf2i(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f2i
            widget.countUpLHf2i()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f2i
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_f2i = newCounter
                buttonVib(200)
                checkNotNull(widget.count)
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                countDataSource!!.saveCountf2i(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countDownf2i(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f2i
            widget.countDownf2i()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f2i
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_f2i = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountf2i(count!!)
            }
            reenterActivity()
        }
    }

    fun countDownLHf2i(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f2i
            widget.countDownLHf2i()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f2i
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_f2i = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountf2i(count!!)
            }
            reenterActivity()
        }
    }

    fun countUpf3i(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f3i
            widget.countUpf3i()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f3i
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_f3i = newCounter
                buttonVib(200)
                checkNotNull(widget.count)
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                countDataSource!!.saveCountf3i(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countUpLHf3i(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f3i
            widget.countUpLHf3i()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f3i
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_f3i = newCounter
                buttonVib(200)
                checkNotNull(widget.count)
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                countDataSource!!.saveCountf3i(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countDownf3i(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f3i
            widget.countDownf3i()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f3i
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_f3i = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountf3i(count!!)
            }
            reenterActivity()
        }
    }

    fun countDownLHf3i(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f3i
            widget.countDownLHf3i()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f3i
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_f3i = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountf3i(count!!)
            }
            reenterActivity()
        }
    }

    fun countUppi(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_pi
            widget.countUppi()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_pi
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_pi = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountpi(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countUpLHpi(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_pi
            widget.countUpLHpi()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_pi
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_pi = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountpi(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countDownpi(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_pi
            widget.countDownpi()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_pi
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_pi = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountpi(count!!)
            }
            reenterActivity()
        }
    }

    fun countDownLHpi(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_pi
            widget.countDownLHpi()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_pi
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_pi = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountpi(count!!)
            }
            reenterActivity()
        }
    }

    fun countUpli(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_li
            widget.countUpli()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_li
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_li = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountli(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countUpLHli(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_li
            widget.countUpLHli()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_li
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_li = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountli(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countDownli(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_li
            widget.countDownli()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_li
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_li = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountli(count!!)
            }
            reenterActivity()
        }
    }

    fun countDownLHli(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_li
            widget.countDownLHli()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_li
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_li = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountli(count!!)
            }
            reenterActivity()
        }
    }

    fun countUpei(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_ei
            widget.countUpei()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_ei
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_ei = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountei(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countUpLHei(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_ei
            widget.countUpLHei()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_ei
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_ei = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountei(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countDownei(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_ei
            widget.countDownei()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_ei
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_ei = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountei(count!!)
            }
            reenterActivity()
        }
    }

    fun countDownLHei(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHI(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_ei
            widget.countDownLHei()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_ei
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_ei = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountei(count!!)
            }
            reenterActivity()
        }
    }

    // count functions for external counters
    fun countUpf1e(view: View) {
        val tempCountId = view.tag.toString().toInt()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, ("1302, countUpf1e, section Id: " + sectionId
                    + ", tempCountId: " + tempCountId))

        val widget = getCountFromIdE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f1e
            widget.countUpf1e()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f1e
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_f1e = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)

                // save the data
                countDataSource!!.saveCountf1e(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countUpLHf1e(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f1e
            widget.countUpLHf1e()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f1e
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_f1e = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountf1e(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countDownf1e(view: View) {
        val tempCountId = view.tag.toString().toInt()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) 
            Log.i(TAG, ("1350 countDownf1e, section Id: " + sectionId
                    + ", tempCountId: " + tempCountId))

        val widget = getCountFromIdE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f1e
            widget.countDownf1e()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f1e
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_f1e = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountf1e(count!!)
            }
            reenterActivity()
        }
    }

    fun countDownLHf1e(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f1e
            widget.countDownLHf1e()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f1e
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_f1e = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountf1e(count!!)
            }
            reenterActivity()
        }
    }

    fun countUpf2e(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f2e
            widget.countUpf2e()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f2e
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_f2e = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountf2e(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countUpLHf2e(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f2e
            widget.countUpLHf2e()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f2e
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_f2e = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountf2e(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countDownf2e(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f2e
            widget.countDownf2e()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f2e
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_f2e = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountf2e(count!!)
            }
            reenterActivity()
        }
    }

    fun countDownLHf2e(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f2e
            widget.countDownLHf2e()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f2e
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_f2e = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountf2e(count!!)
            }
            reenterActivity()
        }
    }

    fun countUpf3e(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f3e
            widget.countUpf3e()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f3e
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_f3e = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountf3e(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countUpLHf3e(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f3e
            widget.countUpLHf3e()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f3e
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_f3e = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountf3e(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countDownf3e(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f3e
            widget.countDownf3e()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f3e
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_f3e = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountf3e(count!!)
            }
            reenterActivity()
        }
    }

    fun countDownLHf3e(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_f3e
            widget.countDownLHf3e()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_f3e
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_f3e = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountf3e(count!!)
            }
            reenterActivity()
        }
    }

    fun countUppe(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_pe
            widget.countUppe()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_pe
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_pe = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountpe(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countUpLHpe(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_pe
            widget.countUpLHpe()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_pe
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_pe = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountpe(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countDownpe(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_pe
            widget.countDownpe()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_pe
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_pe = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountpe(count!!)
            }
            reenterActivity()
        }
    }

    fun countDownLHpe(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_pe
            widget.countDownLHpe()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_pe
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_pe = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountpe(count!!)
            }
            reenterActivity()
        }
    }

    fun countUple(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_le
            widget.countUple()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_le
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_le = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountle(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countUpLHle(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_le
            widget.countUpLHle()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_le
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_le = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountle(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countDownle(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_le
            widget.countDownle()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_le
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_le = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountle(count!!)
            }
            reenterActivity()
        }
    }

    fun countDownLHle(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_le
            widget.countDownLHle()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_le
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_le = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountle(count!!)
            }
            reenterActivity()
        }
    }

    fun countUpee(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_ee
            widget.countUpee()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_ee
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_ee = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountee(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countUpLHee(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_ee
            widget.countUpLHee()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_ee
            if (newCounter > oldCounter)  // has actually counted up
            {
                count!!.count_ee = newCounter
                if (buttonSoundPref) soundService.soundPlusButtonSound()
                buttonVib(200)
                countDataSource!!.saveCountee(count!!)
                sectionDataSource!!.saveDateSection(section!!)
            }
            reenterActivity()
        }
    }

    fun countDownee(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_ee
            widget.countDownee()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_ee
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_ee = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountee(count!!)
            }
            reenterActivity()
        }
    }

    fun countDownLHee(view: View) {
        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLHE(tempCountId)
        if (widget != null) {
            oldCounter = count!!.count_ee
            widget.countDownLHee()
            checkNotNull(widget.count)
            newCounter = widget.count!!.count_ee
            if (newCounter < oldCounter || newCounter == 0) {
                count!!.count_ee = newCounter
                if (buttonSoundPref) soundService.soundMinusButtonSound()
                buttonVib(450)
                countDataSource!!.saveCountee(count!!)
            }
            reenterActivity()
        }
    }
    // End of counters

    // Re-enter CountingActivity to overcome Spinner deficiency for repeated item
    private fun reenterActivity() {
        val intent = Intent(this@CountingActivity, CountingActivity::class.java)
        intent.putExtra("section_id", sectionId)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    private fun buttonVib(dur: Long) {
        if (buttonVibPref) {
            if (Build.VERSION.SDK_INT >= 31) { // S, Android 12
                vibrator.vibrate(VibrationEffect.createPredefined(
                    VibrationEffect.EFFECT_CLICK))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(
                        dur,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
                vibrator.cancel()
            }
        }
    }

    companion object {
        private const val TAG = "CountAct"
    }
    
}
