package com.wmstein.transektcount

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.MenuCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

import com.google.android.material.snackbar.Snackbar
import com.wmstein.changelog.ChangeLog
import com.wmstein.filechooser.AdvFileChooser
import com.wmstein.transektcount.TCLifecycleHandler.Companion.isApplicationVisible
import com.wmstein.transektcount.TransektCountApplication.Companion.getPrefs
import com.wmstein.transektcount.Utils.fromHtml
import com.wmstein.transektcount.database.CountDataSource
import com.wmstein.transektcount.database.DbHelper
import com.wmstein.transektcount.database.Head
import com.wmstein.transektcount.database.HeadDataSource
import com.wmstein.transektcount.database.Meta
import com.wmstein.transektcount.database.MetaDataSource
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.database.Track
import com.wmstein.transektcount.database.TrackDataSource

import org.dhatim.fastexcel.BorderSide
import org.dhatim.fastexcel.PaperSize
import org.dhatim.fastexcel.Position
import org.dhatim.fastexcel.Workbook

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.concurrent.Executors

import kotlin.system.exitProcess

/***************************************************************************************************
 * WelcomeActivity provides the starting page with buttons to start
 * EditMetaActivity, SelectSectionActivity and ShowResultsActivity.
 * and a menu for the import/export/help/info methods.
 *
 * It uses further LocationService and the PermissionDialogFragments.
 *
 * Database handling is mainly controlled in WelcomeActivity as e.g. upgrade
 * to current DB version when importing an older DB file by importDBFile().
 *
 * Based on BeeCount's WelcomeActivity.java by Milo Thurston from 2014-05-05.
 * Changes and additions for TransektCount by wmstein since 2016-02-18,
 * last edited in Java on 2026-07-18,
 * converted to Kotlin on 2026-07-20,
 * last edited on 2026-08-30.
 */
class WelcomeActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private var transektCount: TransektCountApplication? = null
    private var baseLayout: View? = null
    private var welcomeTitle: TextView? = null
    private var cl: ChangeLog? = null

    // Services
    private var locationService: LocationService? = null
    private var locServiceOn = false
    private lateinit var locIntent: Intent
    var soundService: SoundService? = null
    private var sndServiceOn = false
    var sndIntent: Intent? = null

    // Import/export stuff
    private var inFile: File? = null
    private var outFile: File? = null
    private var localLanguage = ""
    private var mExternalStorageWriteable = false
    private val storageState = Environment.getExternalStorageState()
    private val mHandler = Handler(Looper.getMainLooper())

    // File handling with plausi check
    private val regexFilename = "[^a-zA-Z_0-9äöüÄÖÜ-]"
    private var transNo = "" // The transect name/No. as shown
    private var transNoForDir = "" // The transect name as part of a filename

    // Preferences
    private var prefs = getPrefs()
    private var editor = prefs.edit()
    private var outPref = "" // Output sorting
    private var autoSection = false // true for enabled GPS track selection
    private var transectHasTrack = false // transect sections have tracks
    private var buttonSoundPref = false
    private var alertSoundPref = false
    private var dataLanguage = ""

    // Permissions
    private var storagePermGranted = false // initial storage permission state
    var stoIntent: Intent? = null
    private var fineLocationPermGranted = false // foreground location permission state

    // DB handling, data sources
    private var database: SQLiteDatabase? = null
    private var dbHelper: DbHelper? = null
    private var headDataSource: HeadDataSource? = null
    private var head: Head? = null
    private var sectionDataSource: SectionDataSource? = null
    private var metaDataSource: MetaDataSource? = null
    private var meta: Meta? = null
    private var countDataSource: CountDataSource? = null
    private var trackDataSource: TrackDataSource? = null

    // Track handling
    private var trCount = 0 // number of tracks
    private lateinit var trackPts: List<Track> // list of all transect track points
    private var secCount = 0 // number of sections
    private var sectionNameGPS = "" // track section name from track table

    // Other stuff
    private var mesg = ""
    private lateinit var alert: AlertDialog
    private var doubleBackToExitPressedTwice = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "178, onCreate")

        transektCount = application as TransektCountApplication

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        buttonSoundPref = prefs.getBoolean("pref_button_sound", false) // Prepare SoundService
        alertSoundPref = prefs.getBoolean("pref_alert_sound", false)
        transectHasTrack = prefs.getBoolean("transect_has_track", false)
        autoSection = prefs.getBoolean("pref_auto_section", false)

        // Initialize (button) sound service
        if (buttonSoundPref) {
            soundService = SoundService(applicationContext)
            sndIntent = Intent(applicationContext, SoundService::class.java)
            startService(sndIntent)
            sndServiceOn = true
        }

        // Proximity sensor handling in preferences menu
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        // Gray out preferences menu item pref_prox when max. proximity sensitivity = null
        val prefProx = proximitySensor != null

        // Gray out preferences menu item pref_button_vib when device has no vibrator
        val vibrator = applicationContext.getSystemService(Vibrator::class.java)
        val prefVib = vibrator.hasVibrator() // true if vibrator is available

        // Set pref_prox and pref_button_vib enabler, used in SettingsFragment
        editor.putBoolean("enable_prox", prefProx)
        editor.putBoolean("enable_vib", prefVib)
        editor.apply()

        // Set DarkMode when system is in BrightMode
        val nightModeFlags = Configuration.UI_MODE_NIGHT_MASK
        val confUi = resources.configuration.uiMode
        if ((nightModeFlags and confUi) == Configuration.UI_MODE_NIGHT_NO) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        // Use EdgeToEdge mode for Android 15+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)  // Android 15+, SDK 35+
        {
            this.enableEdgeToEdge()
        }

        setContentView(R.layout.activity_welcome)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.baseLayout))
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

        cl = ChangeLog(this, prefs)

        // Show changelog for new version
        if (cl!!.firstRun())
            Objects.requireNonNull<AlertDialog>(cl!!.logDialog).show()

        if (!transectHasTrack && autoSection) {
            autoSection = false
            editor.putBoolean("pref_auto_section", false)
            editor.commit()
        }

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, ("253, onCreate, autoSection: " + autoSection
                        + ", transect has tracks: " + transectHasTrack))

        // Check initial storage permission state and if not granted provide dialog
        storagePermGranted = isStoragePermGranted()
        if (!storagePermGranted)  // in self permission
        {
            // Provide dialog for storage permission request
            requestAllFilesAccessPermission(1)
        }

        // Check again after request
        storagePermGranted = isStoragePermGranted()
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "267, onCreate, storageGranted: $storagePermGranted")

        // Check current DB version and upgrade if necessary
        dbHelper = DbHelper(this)
        database = dbHelper!!.writableDatabase // Make DB upgrade if necessary
        val dbVer = database!!.version // DB version after upgrade
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "274, onCreate, dbVersion: $dbVer")

        dbHelper!!.close()

        // Set up the data sources
        headDataSource = HeadDataSource(this)
        sectionDataSource = SectionDataSource(this)
        metaDataSource = MetaDataSource(this)
        countDataSource = CountDataSource(this)
        trackDataSource = TrackDataSource(this)

        // Write initial Basic database
        if (storagePermGranted) {
            // Test for existence of directory /storage/emulated/0/Documents/TransektCount
            var path = Environment.getExternalStorageDirectory()
            path = File("$path/Documents/TransektCount")

            // Delete an old existing transektcount0.db if it does exist
            inFile = File(path, "/transektcount0.db") // old initial basic DB
            if (inFile!!.exists())
                inFile!!.delete()

            // Create directory TransektCount and write initial transektcount0_ll.db file
            localLanguage = Locale.getDefault().toString().substring(0, 2)
            inFile = File(path, "/transektcount0_$localLanguage.db") // new initial basic DB
            if (!inFile!!.exists()) {
                // Call with parameter 0: short name, local language and don't show a message
                exportBasicDb(0)
            }
        }

        // Get transect No. and check for DB integrity
        try {
            headDataSource!!.open()
            head = headDataSource!!.head
            transNo = head!!.transect_no // get transNo and test for DB integrity
            headDataSource!!.close()
        } catch (_: SQLiteException) {
            headDataSource!!.close()

            mesg = getString(R.string.corruptDb)
            Toast.makeText(
                this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()

            mHandler.postDelayed({ this.finishAndRemoveTask() }, 2000)
        }

        transNoForDir = transNo
        if (transNoForDir != "") transNoForDir = transNoForDir.replace(regexFilename.toRegex(), "")

        // ***************************************************
        // Prepare for GPS usage
        // Try to get list of trackpoints and number of tracks
        try {
            trackDataSource!!.open()
            trackPts = trackDataSource!!.getAllTrackPoints()
            trCount = trackDataSource!!.diffTrks
            trackDataSource!!.close()
        } catch (_: SQLiteException) {
            trackDataSource!!.close()
            trCount = 0
        }

        // Get number of sections and name for 1st use of new DB
        sectionDataSource!!.open()
        secCount = sectionDataSource!!.numEntries
        sectionNameGPS = sectionDataSource!!.getSection(1).name
        sectionDataSource!!.close()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG && autoSection)
            Log.i(TAG, ("347, onCreate, TrkPts: " + trackPts.size
                        + ", trCount: " + trCount + ", secCount: " + secCount))

        // Check if tracks exist and correspond to sections
        if (trCount > 0 && trCount != secCount) {
            autoSection = false

            editor.putBoolean("pref_auto_section", false)
            editor.commit()

            mesg = getString(R.string.track_err)
            Toast.makeText(
                applicationContext,
                fromHtml("<font color='red'>$mesg</font>"),
                Toast.LENGTH_LONG
            ).show()
        }

        // Check if track discovery is set and tracks correspond to sections
        if (autoSection) {
            if (trCount != secCount) {
                autoSection = false
                transectHasTrack = false

                editor.putBoolean("pref_auto_section", false)
                editor.putBoolean("transect_has_track", false)
                editor.commit()

                mesg = getString(R.string.track_err)
                Toast.makeText(
                    applicationContext,
                    fromHtml("<font color='red'>$mesg</font>"),
                    Toast.LENGTH_LONG
                ).show()
            } else if (trCount > 0) {
                transectHasTrack = true

                editor.putBoolean("transect_has_track", true)
                editor.commit()
            }
        }

        /*+*+++++++++++++++++++++++++++++++++++++++++++
         * Check and ask foreground location permission
         */
        if (autoSection && isStoragePermGranted()) {
            isFineLocationPermGranted()

            if (transectHasTrack && !fineLocationPermGranted) { // query foreground location permission
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "397, onCreate, ForegrndLocDialog")

                PermissionsForegroundDialogFragment.newInstance().show(
                    supportFragmentManager, PermissionsForegroundDialogFragment::class.java.name
                )

                editor.putBoolean("has_asked_background", false)
                editor.commit()
            } else if (!transectHasTrack) {
                autoSection = false

                editor.putBoolean("pref_auto_section", false) // deny auto section function
                editor.commit()
            }
        }

        if (autoSection && isStoragePermGranted()) {
            isFineLocationPermGranted()
            if (transectHasTrack && fineLocationPermGranted) {
                // Get flag 'has_asked_background'
                val hasAskedBackgroundLocation = prefs.getBoolean("has_asked_background", false)

                // Get background location with permissions check only once and if storage and fine location
                //   permissions are granted
                if (storagePermGranted && fineLocationPermGranted && !hasAskedBackgroundLocation) {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG, "423, onCreate, BackgrndLocDialog")

                    // Ask optional background location permission
                    PermissionsBackgroundDialogFragment.newInstance().show(
                        supportFragmentManager, PermissionsBackgroundDialogFragment::class.java.name
                    )

                    // Store flag 'hasAskedBackground = true' in SharedPreferences
                    editor.putBoolean("has_asked_background", true)
                    editor.commit()
                }
            }
        }

        /* New onBackPressed logic, use only when NavBarMode = 0 or 1
         * Different Navigation Bar modes and layouts:
         * - Classic three-button navigation: NavBarMode = 0
         * - Two-button navigation (Android P): NavBarMode = 1
         * - Full screen gesture mode (Android Q): NavBarMode = 2
         */
        if (this.navBarMode == 0 || this.navBarMode == 1) {
            val callback = this.onBackPressedCallback
            onBackPressedDispatcher.addCallback(this, callback)
        }
    }
    // End of onCreate()

    // Check storage self permission
    private fun isStoragePermGranted(): Boolean {
        // Check permission MANAGE_EXTERNAL_STORAGE for Android >= 11
        val storageGranted: Boolean = Environment.isExternalStorageManager()
        return storageGranted
    }

    // Request All-Files-Access Permission
    fun requestAllFilesAccessPermission(m: Int) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "460, requestAllFilesAccessPermission")

        if (m == 1) mesg = getString(R.string.dialog_storage_message1)
        else if (m == 2) mesg = getString(R.string.dialog_storage_message2)

        // Provide dialog for storage permission request
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_storage_title))
            .setMessage(mesg)
            .setPositiveButton(
                getString(R.string.ok_button)
            ) { _: DialogInterface?, _: Int ->
                stoIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val packageUri = Uri.fromParts("package", packageName, null)
                stoIntent!!.data = packageUri
                TransektCountApplication.isStorPermReq = true
                startActivity(stoIntent)
            }
            .setNegativeButton(
                getString(R.string.cancelButton)
            ) { _: DialogInterface?, _: Int ->
                showSnackbarRed(getString(R.string.storage_perm_denied))
            }
            .create().show()
    }

    val navBarMode: Int
        // Check for Navigation bar (1-, 2- or 3-button mode)
        get() {
            val resources = this.resources

            @SuppressLint("DiscouragedApi") val resourceId = resources.getIdentifier(
                "config_navBarInteractionMode",
                "integer", "android"
            )

            // navBarMode = 0: 3-button, = 1: 2-button, = 2: gesture
            val navBarMode = if (resourceId > 0) resources.getInteger(resourceId) else 0
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "499, NavBarMode = $navBarMode")

            return navBarMode
        }

    // Use onBackPressed logic for button navigation
    private val onBackPressedCallback: OnBackPressedCallback
        get() {
            val m1Handler = Handler(Looper.getMainLooper())
            val r1 = Runnable { doubleBackToExitPressedTwice = false }

            return object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (doubleBackToExitPressedTwice) {
                        m1Handler.removeCallbacks(r1)
                        if (fineLocationPermGranted && autoSection)
                            locationDispatcher(2) // stop locHandler and location service

                        // Stop sound server
                        if (sndServiceOn && locServiceOn) {
                            locationService!!.releaseSoundA()
                            soundService!!.releaseSoundM()
                            soundService!!.releaseSoundP()

                            stopService(sndIntent)
                            sndServiceOn = false
                        }
                        finish()
                        remove()
                    } else {
                        doubleBackToExitPressedTwice = true

                        mesg = getString(R.string.back_twice) // within 1.5 sec
                        Toast.makeText(
                            applicationContext,
                            fromHtml("<font color='blue'>$mesg</font>"),
                            Toast.LENGTH_SHORT
                        ).show()
                        m1Handler.postDelayed(r1, 1500)
                    }
                }
            }
        }

    @SuppressLint("SourceLockedOrientationActivity", "ApplySharedPref")
    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "548, onResume")

        prefs = getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)

        outPref = prefs.getString("pref_csv_out", "sections")!! // sort mode csv-export
        autoSection = prefs.getBoolean("pref_auto_section", false)
        transectHasTrack = prefs.getBoolean("transect_has_track", false)
        dataLanguage = prefs.getString("pref_sel_data_lang", "")!!
        if (dataLanguage.isEmpty()) dataLanguage = "--"

        // Set storagePermGranted from self permission
        storagePermGranted = isStoragePermGranted()

        headDataSource!!.open()
        sectionDataSource!!.open()
        metaDataSource!!.open()
        countDataSource!!.open()
        trackDataSource!!.open()

        baseLayout = findViewById(R.id.baseLayout)
        baseLayout!!.background = transektCount!!.setBackgr()

        // Try to get list of trackpoints and number of tracks
        try {
            trackPts = trackDataSource!!.getAllTrackPoints()
            trCount = trackDataSource!!.diffTrks
        } catch (_: SQLiteException) {
            trCount = 0
        }

        secCount = sectionDataSource!!.numEntries
        sectionNameGPS = sectionDataSource!!.getSection(1).name

        // Check if tracks exist and correspond to sections
        if (trCount > 0 && trCount != secCount) {
            autoSection = false

            editor.putBoolean("pref_auto_section", false)
            editor.commit()

            mesg = getString(R.string.track_err)
            Toast.makeText(
                applicationContext,
                fromHtml("<font color='red'>$mesg</font>"),
                Toast.LENGTH_LONG
            ).show()
        }

        // Set app title according to using GPS or not
        welcomeTitle = findViewById(R.id.welcomeTitle)
        if (autoSection) {
            welcomeTitle!!.text = getString(R.string.app_name1) // app title = TransektCountGPS
        } else {
            welcomeTitle!!.text = getString(R.string.app_name) // app title = TransektCount
        }

        // Set transect number as transect title
        head = headDataSource!!.head
        transNo = head!!.transect_no // read and test for DB integrity
        supportActionBar!!.title = transNo

        // Prepare modified transNo to be part of a filename
        transNoForDir = transNo
        if (transNoForDir != "") transNoForDir = transNoForDir.replace(regexFilename.toRegex(), "")

        // If all requirements are given start location service
        if (autoSection && fineLocationPermGranted && transectHasTrack) {
            // Start location service and get 1. location
            locationDispatcher(1)
        }
    }
    // End of onResume()

    // Check initial fine location permission
    private fun isFineLocationPermGranted() {
        fineLocationPermGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /* Control location service by locationDispatcherMode:
     *  1: Start location service and periodic location requests
     *  2: Stop location service on backpress, when running
     *  3: Stop location service by onStop() when app is invisible
     */
    private fun locationDispatcher(locationDispatcherMode: Int) {
        if (fineLocationPermGranted && autoSection)  // if GPS should be used
        {
            when (locationDispatcherMode) {
                1 -> {
                    // Start location service
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG, "642, locationDispatcher 1")

                    if (!locServiceOn) {
                        locationService = LocationService(applicationContext)
                        locIntent = Intent(applicationContext, LocationService::class.java)
                        startService(locIntent)

                        locServiceOn = true
                    }

                    // Get location to check distance to nearest transect
                    this.distance
                }

                2 -> {
                    // Stop location service on backpress, when running
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG, "659, location stop 2 by backpress")

                    if (locServiceOn) {
                        stopLocSrv()
                    }
                }

                3 -> {
                    // Stop location service by onStop() when app is invisible
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG, "669, location stop 3 by onStop")

                    if (locServiceOn) {
                        stopLocSrv()
                    }
                }
            }
        }
    }

    val distance: Unit
        // Get the current location for a hint telling the distance to transect
        get() {
            if (locationService!!.canGetLocation() && TransektCountApplication.distMin != 0.0) {
                // Show message: GPS: Distance to track: distance m
                val dst = DecimalFormat("#.#")
                    .format(TransektCountApplication.distMin)

                mesg = getString(R.string.distanceToTrack) + " " + dst + " m"
                Toast.makeText(
                    this,  // bright green
                    fromHtml("<font color='#008000'>$mesg</font>"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // Stop location service and periodic updating
    private fun stopLocSrv() {
        locationService!!.stopListener()
        locIntent = Intent(applicationContext, LocationService::class.java)
        stopService(locIntent)

        TransektCountApplication.sectionIdGPS = 0
        locServiceOn = false
        TransektCountApplication.lat = 0.0
        TransektCountApplication.lon = 0.0
    }

    // Show the action bar menu with present items
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.welcome, menu)
        MenuCompat.setGroupDividerEnabled(menu, true) // Show dividers in menu
        return true
    }

    // Handle clicks on the action bar and its menu items here
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent?
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(
                    Intent(this, SettingsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
                return true
            }

            R.id.exportBasisMenu -> {
                if (storagePermGranted) {
                    exportBasicDb(2) // 2: show message + long name
                } else {
                    requestAllFilesAccessPermission(2)
                }
                return true
            }

            R.id.exportMenu -> {
                if (storagePermGranted) {
                    exportDb()
                } else {
                    requestAllFilesAccessPermission(2)
                }
                return true
            }

            R.id.exportXLSXMenu -> {
                if (storagePermGranted) {
                    exportDb2XLSX()
                } else {
                    requestAllFilesAccessPermission(2)
                }
                return true
            }

            R.id.exportCSVMenu -> {
                if (storagePermGranted) {
                    exportDb2CSV()
                } else {
                    requestAllFilesAccessPermission(2)
                }
                return true
            }

            R.id.exportSpeciesListMenu -> {
                if (storagePermGranted) {
                    exportSpeciesList()
                } else {
                    requestAllFilesAccessPermission(2)
                }
                return true
            }

            R.id.importBasisMenu -> {
                if (storagePermGranted) {
                    importBasisDb()
                } else {
                    requestAllFilesAccessPermission(2)
                }
                return true
            }

            R.id.importFileMenu -> {
                if (storagePermGranted) {
                    importDBFile()
                } else {
                    requestAllFilesAccessPermission(2)
                }
                return true
            }

            R.id.importSpeciesListMenu -> {
                if (storagePermGranted) {
                    importSpeciesList()
                } else {
                    requestAllFilesAccessPermission(2)
                }
                return true
            }

            R.id.importGPSMenu -> {
                if (!transectHasTrack && storagePermGranted) importGPS()
                else if (!storagePermGranted) requestAllFilesAccessPermission(2)
                else {
                    mesg = getString(R.string.importTrackFail)
                    Toast.makeText(
                        this,  // orange
                        fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                        Toast.LENGTH_LONG
                    ).show()
                }
                return true
            }

            R.id.deleteGPXMenu -> {
                if (transectHasTrack) {
                    deleteGPS()
                    transectHasTrack = false
                    autoSection = false
                    // Reset app title to TransektCount
                    welcomeTitle!!.text = getString(R.string.app_name)
                } else {
                    mesg = getString(R.string.deleteTrackFail)
                    Toast.makeText(
                        this,  // orange
                        fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                        Toast.LENGTH_LONG
                    ).show()
                }
                return true
            }

            R.id.resetDBMenu -> {
                resetToBasisDb()
                return true
            }

            R.id.viewHelp -> {
                intent = Intent(this@WelcomeActivity, ShowTextDialog::class.java)
                intent.putExtra("dialog", "help")
                startActivity(intent)
                return true
            }

            R.id.changeLog -> {
                Objects.requireNonNull<AlertDialog>(cl!!.fullLogDialog).show()
                return true
            }

            R.id.viewLicense -> {
                intent = Intent(this@WelcomeActivity, ShowTextDialog::class.java)
                intent.putExtra("dialog", "license")
                startActivity(intent)
                return true
            }

            R.id.editMeta -> {
                startActivity(
                    Intent(this, EditMetaActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
                return true
            }

            R.id.selectSection -> {
                selSect()
                return true
            }

            R.id.showResults -> {
                mesg = getString(R.string.wait)
                Toast.makeText(
                    this,
                    fromHtml("<font color='blue'>$mesg</font>"),
                    Toast.LENGTH_SHORT
                ).show()

                // To show toast, pause for 100 msec before calling ShowResultsActivity
                mHandler.postDelayed({
                    startActivity(
                        Intent(applicationContext, ShowResultsActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                }, 100)
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }
    // End of onOptionsItemSelected

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        baseLayout = findViewById(R.id.baseLayout)
        baseLayout!!.background = transektCount!!.setBackgr()
        outPref = prefs.getString("pref_csv_out", "species")!!
        autoSection = prefs.getBoolean("pref_auto_section", false)
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false)
        alertSoundPref = prefs.getBoolean("pref_alert_sound", false)

        // Stop location service when denied in settings with alert sound first
        if (!autoSection && locServiceOn) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "902, location stop by setting")

            locationService!!.stopSoundA()
            stopLocSrv()
        }

        // Stop sound service when denied in settings
        if (!buttonSoundPref && sndServiceOn) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "911, button sound stop by setting")

            stopService(sndIntent)
            sndServiceOn = false
        }

        // Start sound service when set in settings
        if (buttonSoundPref && !sndServiceOn) {
            startService(sndIntent)
            sndServiceOn = true
        }

        // Stop alert sound when denied in settings
        if (!alertSoundPref && locServiceOn) {
            locationService!!.stopSoundA()
        }
    }

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "933, onPause")

        headDataSource!!.close()
        sectionDataSource!!.close()
        metaDataSource!!.close()
        countDataSource!!.close()
        trackDataSource!!.close()

        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "948, onStop")

        baseLayout!!.invalidate()

        // Stop location service when app is finished
        if (!isApplicationVisible) {
            if (sndServiceOn) {
                soundService!!.releaseSoundM()
                soundService!!.releaseSoundP()
                stopService(sndIntent)
                sndServiceOn = false
            }

            if (locServiceOn) locationService!!.releaseSoundA()

            locationDispatcher(3)

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "966, onStop, app not visible, LocationService running: $locServiceOn")

            finishAndRemoveTask() // In combination with System.exit(0) in onDestroy() closes app completely
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "976, onDestroy")

        exitProcess(0)
    }

    // Start SelectSectionActivity (by button)
    fun selectSection(view: View?) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "984, selectSection, View: $view")

        selSect()
    }

    private fun selSect() {
        if (autoSection && fineLocationPermGranted && transectHasTrack) {
            // Call SelectSectionActivity with the GPS identified section marked blue
            val intent = Intent(this@WelcomeActivity, SelectSectionActivity::class.java)
            mHandler.postDelayed(
                {
                    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                },
                100
            )
        } else {
            // Just call SelectSectionActivity to select section for counting
            mHandler.postDelayed(
                {
                    startActivity(
                        Intent(this, SelectSectionActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                },
                100
            )
        }
    }

    // Start EditMetaActivity (by button)
    fun editMeta(view: View?) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "1016, showResults, View: $view")

        startActivity(
            Intent(this, EditMetaActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    // Start ShowResultsActivity (by button)
    fun showResults(view: View?) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "1027, showResults, View: $view")

        mesg = getString(R.string.wait)
        Toast.makeText(
            this,
            fromHtml("<font color='blue'>$mesg</font>"),
            Toast.LENGTH_SHORT
        ).show()

        // Trick: Pause for 100 msec to show toast
        mHandler.postDelayed(
            {
                startActivity(
                    Intent(
                        applicationContext,
                        ShowResultsActivity::class.java
                    )
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
            }, 100
        )
    }

    /***********************************************************************************************
     * The next four function groups below are for importing data files.
     * They've been put here because no database should be left open at this point.
     */
    // Import the basic DB
    private fun importBasisDb() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "1057, importBasicDBFile")

        val fileExtension = ".db"
        val fileNameStart = "transektcount0"
        val fileHd = getString(R.string.fileHeadlineBasicDB)

        val intent = Intent(this, AdvFileChooser::class.java)
        intent.putExtra("filterFileExtension", fileExtension)
        intent.putExtra("filterFileNameStart", fileNameStart)
        intent.putExtra("fileHd", fileHd)
        dBActivityResultLauncher.launch(intent)
    }
    // End of part 1 of importBasisDb()

    // Choose a transektcount db-file to load and set it to transektcount.db
    private fun importDBFile() {
        val fileExtension = ".db"
        val fileNameStart = "transektcount_"
        val fileHd = getString(R.string.fileHeadlineDB)

        val intent = Intent(this, AdvFileChooser::class.java)
        intent.putExtra("filterFileExtension", fileExtension)
        intent.putExtra("filterFileNameStart", fileNameStart)
        intent.putExtra("fileHd", fileHd)
        dBActivityResultLauncher.launch(intent)
    }
    // End of part 1 of importDBFile()

    // dBActivityResultLauncher is part2 of importBasisDb() and importDBFile()
    //   and processes the result of AdvFileChooser
    @Suppress("UNCHECKED_CAST")
    val dBActivityResultLauncher: ActivityResultLauncher<Intent?> =
        registerForActivityResult(
            StartActivityForResult() as ActivityResultContract<Intent?, ActivityResult?>
        ) { result ->
            var selectedFile = ""
            inFile = null

            if (result?.resultCode == RESULT_OK)  // has a file
            {
                val data = result.data
                if (data != null) {
                    selectedFile = data.getStringExtra("fileSelected")!!
                    inFile = if (selectedFile != "") File(selectedFile)
                    else null
                }
            } else {
                if ((result?.resultCode == RESULT_FIRST_USER)) {
                    mesg = getString(R.string.noFile)
                    Toast.makeText(
                        applicationContext,  // orange
                        fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            if (inFile != null) {
                // outFile -> /data/data/com.wmstein.transektcount/databases/transektcount.db
                var destPath = applicationContext.filesDir.path
                destPath = destPath.substring(
                    0,
                    destPath.lastIndexOf("/")
                ) + "/databases/transektcount.db"
                outFile = File(destPath)

                // save current autoSection state
                val transectHasTrackTemp = transectHasTrack
                val autoSectionTemp = autoSection

                val builder = AlertDialog.Builder(this@WelcomeActivity)
                builder.setIcon(android.R.drawable.ic_dialog_alert)
                builder.setMessage(R.string.confirmDBImport)
                builder.setCancelable(false)
                builder.setPositiveButton(
                    R.string.importButton
                ) { _: DialogInterface?, _: Int ->
                    if (transectHasTrack) {
                        transectHasTrack = false
                        autoSection = false
                    }
                    try {
                        headDataSource!!.close()
                        sectionDataSource!!.close()
                        metaDataSource!!.close()
                        countDataSource!!.close()
                        trackDataSource!!.close()

                        copy(inFile, outFile)

                        headDataSource!!.open()
                        sectionDataSource!!.open()
                        metaDataSource!!.open()
                        countDataSource!!.open()
                        trackDataSource!!.open()

                        head = headDataSource!!.head
                        var headLanguage =
                            head!!.data_language // Could be null in head table
                        var hasDataLang = true

                        if (headLanguage == "") hasDataLang = false
                        else headLanguage = headLanguage.substring(0, 2)

                        editor.putString("pref_sel_data_lang", headLanguage)
                        editor.putBoolean("has_data_lang", hasDataLang)

                        // List new transNo as title
                        transNo = head!!.transect_no
                        supportActionBar!!.title = transNo

                        transNoForDir = transNo
                        if (transNoForDir != "") transNoForDir =
                            transNoForDir.replace(regexFilename.toRegex(), "")

                        // Test for number of existing tracks and matching sections
                        trCount = try {
                            trackDataSource!!.diffTrks
                        } catch (_: SQLiteException) {
                            0
                        }
                        secCount = sectionDataSource!!.numEntries

                        // Check to continue location service
                        if (trCount > 0 && trCount == secCount) {
                            editor.putBoolean("transect_has_track", true)
                            transectHasTrack = true
                            editor.putBoolean("pref_auto_section", true)
                            autoSection = true
                        } else {
                            editor.putBoolean("transect_has_track", false)
                            editor.putBoolean("pref_auto_section", false)
                            locationDispatcher(2)
                        }
                        editor.commit()

                        // Set app title according to using GPS or not
                        val welcomeTitle = findViewById<TextView>(R.id.welcomeTitle)
                        if (autoSection) {
                            // app title = TransektCountGPS
                            welcomeTitle.text = getString(R.string.app_name1)
                        } else {
                            // app title = TransektCount
                            welcomeTitle.text = getString(R.string.app_name)
                        }

                        // Reset invalid values
                        TransektCountApplication.lat = 0.0
                        TransektCountApplication.lon = 0.0
                        TransektCountApplication.sectionIdGPS = 0
                        TransektCountApplication.sectionNameCurrent = ""

                        mesg = if (trCount > 0) {
                            (getString(R.string.importDB) + ",\n"
                                    + getString(R.string.with) + " " + trCount + " "
                                    + getString(R.string.tracks))
                        } else {
                            getString(R.string.importDB) + "."
                        }
                        Toast.makeText(
                            applicationContext, // bright green
                            fromHtml("<font color='#008000'>$mesg</font>"),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (_: IOException) {
                        headDataSource!!.open()
                        sectionDataSource!!.open()
                        metaDataSource!!.open()
                        countDataSource!!.open()
                        trackDataSource!!.open()

                        locServiceOn = false
                        mesg = getString(R.string.importFail)
                        Toast.makeText(
                            applicationContext,
                            fromHtml("<font color='red'><b>$mesg</b></font>"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                builder.setNegativeButton(
                    R.string.cancelButton
                ) { dialog: DialogInterface?, _: Int ->
                    editor.putBoolean("pref_auto_section", autoSectionTemp)
                    editor.putBoolean("transect_has_track", transectHasTrackTemp)
                    editor.commit()
                    dialog!!.cancel()
                }
                alert = builder.create()
                alert.show()
            }
        }
    // End of part2 of import of DB files

    // Select and import a species list (also from TourCount file species_YYYY-MM-DD_hhmmss.csv)
    private fun importSpeciesList() {
        val fileExtension = ".csv"
        val fileNameStart = "species_"
        val fileHd = getString(R.string.fileHeadlineCSV)

        val intent = Intent(this, AdvFileChooser::class.java)
        intent.putExtra("filterFileExtension", fileExtension)
        intent.putExtra("filterFileNameStart", fileNameStart)
        intent.putExtra("fileHd", fileHd)
        listActivityResultLauncher.launch(intent)
    }

    // listActivityResultLauncher processes the result of AdvFileChooser
    @Suppress("UNCHECKED_CAST")
    val listActivityResultLauncher: ActivityResultLauncher<Intent?> =
        registerForActivityResult(
            StartActivityForResult() as ActivityResultContract<Intent?, ActivityResult?>
        ) { result ->
            var selectedFile = ""
            inFile = null

            if (result?.resultCode == RESULT_OK) {
                val data = result.data
                if (data != null) {
                    selectedFile = data.getStringExtra("fileSelected")!!
                    inFile = if (selectedFile != "") File(selectedFile)
                    else null
                }
            } else if ((result?.resultCode == RESULT_FIRST_USER)) {
                mesg = getString(R.string.noFile)
                Toast.makeText(
                    applicationContext,  // orange
                    fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG
                ).show()
            }

            if (inFile != null) {
                val csvLine: String
                var brError = false

                // Check for old version of species list
                try {
                    val br = BufferedReader(FileReader(inFile))
                    csvLine = br.readLine() // Read 1. line only
                    val specLine: Array<String?> =
                        csvLine.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    mesg = if (specLine[0] == "nocode")
                        getString(R.string.confirmListImport)
                    else
                        getString(R.string.specsCommonLangMissing) + "\n\n" + getString(R.string.confirmListImport)
                    br.close()
                } catch (_: Exception) {
                    mesg = getString(R.string.br_Error)
                    brError = true
                }

                val builder = AlertDialog.Builder(this@WelcomeActivity)
                builder.setIcon(android.R.drawable.ic_dialog_alert)
                builder.setMessage(mesg)
                if (brError) {
                    builder.setCancelable(true)
                    builder.setNegativeButton(
                        R.string.cancelButton
                    ) { dialog: DialogInterface?, _: Int -> dialog!!.cancel() }
                } else {
                    builder.setCancelable(false)
                    builder.setPositiveButton(
                        R.string.importButton
                    ) { _: DialogInterface?, _: Int ->
                        // Load .csv species list
                        clearDBforImport()
                        readSpeciesCSV(inFile)
                    }
                    builder.setNegativeButton(
                        R.string.cancelButton
                    ) { dialog: DialogInterface?, _: Int -> dialog!!.cancel() }
                }
                alert = builder.create()
                alert.show()
            }
        }

    // Clear DB for import of an external species list:
    //  clear COUNT_TABLE and update SECTION_TABLE
    private fun clearDBforImport() {
        dbHelper = DbHelper(this)
        database = dbHelper!!.writableDatabase

        var sql = "DELETE FROM " + DbHelper.COUNT_TABLE
        database!!.execSQL(sql)

        sql = ("UPDATE " + DbHelper.SECTION_TABLE + " SET "
                + DbHelper.S_CREATED_AT + " = '', "
                + DbHelper.S_NOTES + " = '';")
        database!!.execSQL(sql)

        dbHelper!!.close()

        TransektCountApplication.lat = 0.0
        TransektCountApplication.lon = 0.0
        TransektCountApplication.isFirstLoc = true
        locServiceOn = false
        TransektCountApplication.isSelectSectionActivityResumed = false
        TransektCountApplication.sectionIdGPS = 0
        TransektCountApplication.sectionNameCurrent = ""

        editor.putInt("item_Position", 0)
        editor.putInt("section_id", 1)
        editor.apply()
    }

    // Read a species list and write items to table counts
    private fun readSpeciesCSV(inFile: File?) {
        try {
            mesg = getString(R.string.waitImport)
            Toast.makeText(
                this,
                fromHtml("<font color='blue'>$mesg</font>"),
                Toast.LENGTH_SHORT
            ).show()

            var csvLine: String
            val codeArray: MutableList<String?> = ArrayList()
            val nameArray: MutableList<String?> = ArrayList()
            val nameGArray: MutableList<String?> = ArrayList()

            val br = BufferedReader(FileReader(inFile))
            var hasDataLang = true

            csvLine = br.readLine() // Read 1. line only
            val specLine: Array<String?> =
                csvLine.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (specLine[0] == "nocode") {
                dataLanguage = specLine[2]!!
                editor.putString("pref_sel_data_lang", dataLanguage)
            } else {
                hasDataLang = false
                editor.putString("pref_sel_data_lang", "--")
            }

            editor.putBoolean("has_data_lang", hasDataLang) // controls data language setting
            editor.apply()

            br.close()

            var iList: Int // index of imported list
            var iCounts = 1 // index of id in table counts
            val maxSec = sectionDataSource!!.numEntries // number of sections
            var iSec = 1 // index of section

            // For all sections
            while (iSec <= maxSec) {
                iList = 0

                inFile?.bufferedReader().use { reader ->
                    val lines = reader?.readLines()

                    // For all species list lines
                    lines?.forEach { it1 ->
                        val spLine = it1.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        // 1. line fields contain String[0]: "nocode", [1]: "language", [2]: "de"|"en"|"fr"|"it"|"es"
                        if (spLine[0] == "nocode") {
                            if (hasDataLang) {
                                iCounts--
                                iList--
                            }
                        } else {
                            // comma-separated 0:code, 1:name, 2:nameL
                            codeArray.add(iList, spLine[0])
                            nameArray.add(iList, spLine[1])
                            nameGArray.add(iList, spLine[2])
                            countDataSource!!.writeCountItem(
                                iCounts.toString(), iSec.toString(),
                                codeArray[iList]!!, nameArray[iList]!!, nameGArray[iList]!!
                            )
                        }
                        iList++
                        iCounts++
                    }
                }

                iSec++
            }
            mesg = getString(R.string.importList)
            Toast.makeText(
                this,  // bright green
                fromHtml("<font color='#008000'>$mesg</font>"),
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            mesg = getString(R.string.importListFail)
            Toast.makeText(
                this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    // End of importSpeciesList()

    /***********************************************************************************************
     * Select and import gpx-file to store track coords into the TRACK_TABLE of transektcount.db
     */
    private fun importGPS() {
        if (transectHasTrack) deleteGPS()

        val fileExtension = ".gpx"
        val fileNameStart = "transektcount"
        val fileHd = getString(R.string.fileHeadlineGPS)

        val intent = Intent(this, AdvFileChooser::class.java)
        intent.putExtra("filterFileExtension", fileExtension)
        intent.putExtra("filterFileNameStart", fileNameStart)
        intent.putExtra("fileHd", fileHd)
        gpxActivityResultLauncher.launch(intent)
    }

    // gpxActivityResultLauncher processes the result of AdvFileChooser
    @Suppress("UNCHECKED_CAST")
    val gpxActivityResultLauncher: ActivityResultLauncher<Intent?> =
        registerForActivityResult(
            StartActivityForResult() as ActivityResultContract<Intent?, ActivityResult?>
        ) { result ->
            var selectedFile = ""

            if (result?.resultCode == RESULT_OK) {
                val data = result.data
                if (data != null) {
                    selectedFile = data.getStringExtra("fileSelected")!!
                    inFile = if (selectedFile != "")
                        File(selectedFile)
                    else null
                }
            } else if ((result?.resultCode == RESULT_FIRST_USER)) {
                mesg = getString(R.string.noFile)
                Toast.makeText(
                    applicationContext,  // orange
                    fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG
                ).show()
            }

            // Process the read GPX file
            if (inFile != null) {
                val gpxsb = StringBuilder()
                val builder = AlertDialog.Builder(this@WelcomeActivity)
                builder.setIcon(android.R.drawable.ic_dialog_alert)
                builder.setMessage(R.string.confirmGPSImport)
                builder.setCancelable(true).setPositiveButton(
                    R.string.importButton
                ) { _: DialogInterface?, _: Int ->
                    if (selectedFile.isNotEmpty()) {
                        val fileIS: FileInputStream?
                        try {
                            fileIS = FileInputStream(selectedFile)
                            var gpxLine: String?
                            val xmlBR = BufferedReader(InputStreamReader(fileIS))

                            while ((xmlBR.readLine().also { gpxLine = it }) != null) {
                                gpxsb.append(gpxLine).append('\n')
                            }
                            fileIS.close()
                        } catch (e: IOException) {
                            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                                Log.e(TAG,"1518, decodeGPX, Problem converting Stream to String: $e")
                        }

                        val gpxString = gpxsb.toString()

                        mesg = getString(R.string.waitImport)
                        Toast.makeText(
                            this@WelcomeActivity,
                            fromHtml("<font color='blue'>$mesg</font>"),
                            Toast.LENGTH_SHORT
                        ).show()

                        // Parse gpxString to get number of tracks
                        var gpxStringT = gpxString
                        var numTrk = 0 // number of tracks
                        var indexRestString: Int

                        while (gpxStringT.contains("<trkseg>")) {
                            numTrk++
                            indexRestString = gpxStringT.indexOf("</trkseg>") + 9
                            gpxStringT = gpxStringT.substring(indexRestString)
                        }

                        // Get number of sections and compare with number of tracks
                        val numSect = sectionDataSource!!.numEntries
                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.i(TAG, ("1544, decodeGPX, numSect: $numSect, numTrk: $numTrk"))

                        if (numSect != numTrk) {
                            mesg = getString(R.string.track_err)
                            Toast.makeText(
                                this@WelcomeActivity,
                                fromHtml("<font color='red'><b>$mesg</b></font>"),
                                Toast.LENGTH_LONG
                            ).show()
                            return@setPositiveButton
                        }

                        val executor = Executors.newSingleThreadExecutor()
                        executor.execute {
                            decodeGPX(gpxString) // do it in background
                        }

                        mesg = getString(R.string.importGPS)
                        Toast.makeText(
                            this@WelcomeActivity,  // bright green
                            fromHtml("<font color='#008000'>$mesg</font>"),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        mesg = getString(R.string.no_GPSfile)
                        Toast.makeText(
                            this@WelcomeActivity,  // orange
                            fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }.setNegativeButton(
                    R.string.cancelButton
                ) { dialog: DialogInterface?, _: Int -> dialog!!.cancel() }

                alert = builder.create()
                alert.show()
            }
        }


    // Parse the GPX string for its data items
    private fun decodeGPX(gpxString: String) {
        var gpxString = gpxString
        var gpxTrkString: String

        // Begin with start of 1. track segment as start of string to be checked
        var trkPt = 1 // trackpoint counter
        var trkseg = 1 // track counter
        var strStart = gpxString.indexOf("<trkseg>")
        var strEnd: Int // end of string to be checked
        var trkStart: Int
        var trkEnd: Int // start and end of a track segment

        // Check for gpx syntax (Garmin/Viking)
        var gpxVik = false
        val gpxStringT = gpxString
        if (gpxStringT.contains("</trkpt>")) {
            gpxVik = true
        }

        if (gpxString.contains("<trkseg>"))  // test for track data
        {
            // reduce gpxString to all tracksegments <trkseg> ... </gpx>
            gpxString = gpxString.substring(strStart) // rest string with all tracks

            // For each track
            do {
                // get data of indexed track
                trkStart = gpxString.indexOf("<trkseg>")
                trkEnd = gpxString.indexOf("</trkseg>")
                // add offset = length of "</trkseg>" = 9
                gpxTrkString = gpxString.substring(trkStart, trkEnd + 9)
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "1618, decodeGPX, gpxTrkString: $gpxTrkString")

                // set track name from section name
                // record of transect section for trkseg
                val section = sectionDataSource!!.getSection(trkseg)
                sectionNameGPS = section.name

                // reduce gpxString to rest after </trkseg>
                strEnd = gpxString.indexOf("</trkseg>")

                // add offset = length of "</trkseg>" = 9
                gpxString = gpxString.substring(strEnd + 9)

                // get coords of all track points
                if (gpxTrkString.contains("<trkseg>")) {
                    // for each track point in trkseg
                    do {
                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.i(TAG, "1636, decodeGPX, do trackpt")

                        val nextTp: Int // index for next track point (after /> or /trkpt>)
                        strStart = gpxTrkString.indexOf("lat=") + 5
                        strEnd = gpxTrkString.indexOf("lat=") + 14
                        val tlat = gpxTrkString.substring(strStart, strEnd)

                        strStart = gpxTrkString.indexOf("lon=") + 5
                        strEnd = gpxTrkString.indexOf("lon=") + 14
                        val tlon = gpxTrkString.substring(strStart, strEnd)

                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.i(TAG, ("1648, decodeGPX, sectionNameGPS: "
                                    + sectionNameGPS + ", " + tlat + ", " + tlon))

                        trackDataSource!!.createTrackTp(sectionNameGPS, tlat, tlon)

                        // increment gpxTrkString line for next track point
                        trkPt += 1

                        if (gpxVik)  // test for track data (Viking/Garmin)
                        {
                            // reduce gpxTrkString to remainder after current trkpt line
                            nextTp = gpxTrkString.indexOf("</trkpt>")
                            gpxTrkString = gpxTrkString.substring(nextTp + 8)

                            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                                Log.i(TAG, "1663, decodeGPX, trackpt $trkPt")
                        } else {
                            nextTp = gpxTrkString.indexOf("/>")
                            gpxTrkString = gpxTrkString.substring(nextTp + 2)

                            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                                Log.i(TAG, "1669, decodeGPX, trackpt $trkPt")
                        }
                    } while (gpxTrkString.contains("<trkpt"))
                }

                // increment trkseg and reduce gpxString for next track segment
                trkseg += 1
            } while (gpxString.contains("<trkseg>"))

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "1679, decodeGPX, gpxString finished: $gpxString")
        }

        transectHasTrack = true
        editor.putBoolean("transect_has_track", true)
        editor.apply()
        exportBasicDb(1) // Write modified DB as Basic DB without message
    }
    // End of importGPS()
    // End of import file functions group

    // Delete all track info from TRACK_TABLE
    @SuppressLint("ApplySharedPref")
    private fun deleteGPS() {
        // Confirm dialogue before anything else takes place
        val builder = AlertDialog.Builder(this)
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setMessage(R.string.confirmGPSDelete)
        builder.setCancelable(false)
        builder.setPositiveButton(
            R.string.deleteButton
        ) { _: DialogInterface?, _: Int ->
            val resOK = clearGPSValues()
            if (resOK) {
                // switch autoSection off
                exportBasicDb(1) // Write modified DB as Basic DB without message

                if (locServiceOn) {
                    stopLocSrv()
                    locServiceOn = false
                }
                transectHasTrack = false
                autoSection = false
                editor.putBoolean("pref_auto_section", false)
                editor.putBoolean("transect_has_track", false)
                editor.commit()

                mesg = getString(R.string.resetTracks)
                Toast.makeText(
                    this,  // bright green
                    fromHtml("<font color='#008000'><b>$mesg</b></font>"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        builder.setNegativeButton(
            R.string.cancelButton
        ) { dialog: DialogInterface?, _: Int -> dialog!!.cancel() }
        alert = builder.create()
        alert.show()
    }

    // Clear track coordinates from TRACK_TABLE
    private fun clearGPSValues(): Boolean {
        dbHelper = DbHelper(this)
        database = dbHelper!!.writableDatabase
        var resOK = false // is false when reset fails

        try {
            val sql = "DELETE FROM " + DbHelper.TRACK_TABLE
            database!!.execSQL(sql)
            resOK = true
        } catch (_: Exception) {
            mesg = getString(R.string.resetFail)
            Toast.makeText(
                this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
        }
        dbHelper!!.close()
        return resOK
    }
    // End of deleteGPS()

    /***********************************************************************************************
     * The next five functions below are for exporting data files.
     */
    // Exports Basic DB to Documents/TransektCount/transektcount0_ll.db
    private fun exportBasicDb(i: Int) {
        // i = 0: don't show a message, use local language and short name
        // i = 1: don't show a message and use data language and long name
        // i = 2: show message and use data language and long name
        // inFile <- /data/data/com.wmstein.transektcount/databases/transektcount.db
        var inPath = applicationContext.filesDir.path
        inPath = inPath.substring(0, inPath.lastIndexOf("/")) + "/databases/transektcount.db"
        inFile = File(inPath)

        // tmpFile -> /data/data/com.wmstein.transektcount/files/transektcount_tmp.db
        var tmpPath = applicationContext.filesDir.path
        tmpPath = tmpPath.substring(0, tmpPath.lastIndexOf("/")) + "/files/transektcount_tmp.db"
        val tmpFile = File(tmpPath)

        // outFile in Public Directory Documents/TransektCount/
        // outFile -> /storage/emulated/0/Documents/TransektCount/transektcount0_dl_.db
        var path = Environment.getExternalStorageDirectory()
        path = File("$path/Documents/TransektCount")

        path.mkdirs() // just verify path, result ignored

        if (i == 0)  // Initial creation of basic DB with
            outFile = File(path, "/transektcount0_$localLanguage.db")
        else if (i > 0) { // For all later creations of basic DB
            dataLanguage = prefs.getString("pref_sel_data_lang", "")!!
            if (dataLanguage.isEmpty()) dataLanguage = "--"

            outFile = if (transNoForDir == "")
                File(path, "/transektcount0_$dataLanguage.db")
            else
                File(path, "/transektcount0_" + dataLanguage + "_" + transNoForDir + ".db")
        }

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED == storageState

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard)
            Toast.makeText(
                this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Export the basic db
            try {
                // Save current db as backup db tmpFile
                copy(inFile, tmpFile)

                // Clear DB and location values for basic DB
                val resOK = clearDBValues()

                // Write Basic DB
                if (resOK) {
                    copy(inFile, outFile)
                    // Restore actual db from tmpFile
                    copy(tmpFile, inFile)
                }

                // Delete backup db
                val d0 = tmpFile.delete()

                // Show message success
                if (d0 && i == 2) {
                    mesg = getString(R.string.saveBasisDB)
                    Toast.makeText(
                        this,
                        fromHtml("<font color='blue'>$mesg</font>"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (_: IOException) {
                mesg = getString(R.string.saveFail)
                Toast.makeText(
                    this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    // End of exportBasicDb()

    @SuppressLint("SdCardPath", "LongLogTag")
    private fun exportDb() {
        // Public data directory for outFile: Documents/TransektCount/
        var path = Environment.getExternalStorageDirectory()
        path = File("$path/Documents/TransektCount")

        // Open Meta table for meta info
        meta = metaDataSource!!.meta
        val date: String = meta!!.date // has system date
        val startTime: String = meta!!.start_tm

        var engl = false
        var hasDate = true
        var dbDate: String
        val dbTime: String
        val dbDateEN: String
        val dbDateEU: String

        // Get year from date
        if (date != "") {
            // "-" at position 4 of the date means EN
            if (date.substring(4, 5) == "-") engl = true
            // Create date for filename as YYYYMMDD from any format
            dbDateEU = date.substring(6, 10) + date.substring(3, 5) + date.substring(0, 2)
            dbDateEN = date.substring(0, 4) + date.substring(5, 7) + date.substring(8, 10)
        } else {
            dbDateEU = ""
            dbDateEN = ""
            hasDate = false
        }

        dbDate = if (engl) dbDateEN
        else dbDateEU

        if (startTime != "") {
            dbTime = startTime.substring(0, 2) + startTime.substring(3, 5)

            if (hasDate) dbDate = "$dbDate-$dbTime" // yyyymmdd-hhmm
        } else dbDate = "" // has only a value when both date and start time are given


        path.mkdirs() // Just verify path, result ignored

        dataLanguage = prefs.getString("pref_sel_data_lang", "")!!
        if (dataLanguage.isEmpty()) dataLanguage = "--"

        // outFile -> /storage/emulated/0/Documents/TransektCount/transektcount_Tr-No_yyyyMMdd-HHmm.db
        outFile = if (transNoForDir == "" && dbDate == "")
            File(path, "/transektcount_" + dataLanguage + "_" + getcurDate() + ".db")
        else if (transNoForDir == "")
            File(path, "/transektcount_" + dataLanguage + "_" + dbDate + ".db")
        else if (dbDate == "")
            File(path,
                "/transektcount_" + dataLanguage + "_" + transNoForDir + "_" + getcurDate() + ".db")
        else
            File(path,
                "/transektcount_" + dataLanguage + "_" + transNoForDir + "_" + dbDate + ".db")

        // inFile <- /data/data/com.wmstein.transektcount/databases/transektcount.db
        var inPath = applicationContext.filesDir.path
        inPath = (inPath.substring(0, inPath.lastIndexOf("/"))
                + "/databases/transektcount.db")
        inFile = File(inPath)

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED == storageState

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard)
            Toast.makeText(
                this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Export the db
            try {
                copy(inFile, outFile)

                mesg = getString(R.string.saveDB)
                Toast.makeText(
                    this,
                    fromHtml("<font color='blue'>$mesg</font>"),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: IOException) {
                mesg = getString(R.string.saveFail)
                Toast.makeText(
                    this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    // End of exportDb()

    /***********************************************************************************************
     * // Exports DB contents as Transekt_dL_TransNo_yyyyMMdd_HHmm.csv to
     * // Documents/TransektCount/ with purged data set.
     * // Spreadsheet programs can import this csv file with
     * //   - Unicode UTF-8 filter,
     * //   - comma delimiter and
     * //   - "" for text recognition.
     */
    private fun exportDb2CSV() {
        // Public data directory for outFile: Documents/TransektCount/
        var path: File = Environment.getExternalStorageDirectory()
        path = File("$path/Documents/TransektCount")

        // Prepare metadata
        val inspecName: String // Inspector's name
        val kw: String // kw = calendar week (String)

        // Open Meta table for meta info
        meta = metaDataSource!!.meta
        val temps: Int = meta!!.temps
        val tempe: Int = meta!!.tempe // temperature at start time and end time
        val winds: Int = meta!!.winds
        val winde: Int = meta!!.winde // wind
        val clouds: Int = meta!!.clouds
        val cloude: Int = meta!!.cloude // clouds
        val date: String = meta!!.date // has system date
        val startTime: String = meta!!.start_tm
        val endTime: String = meta!!.end_tm
        val inspecNote: String = meta!!.note

        var engl = false
        var hasDate = true
        var csvDate: String
        val csvTime: String
        val csvDateEU: String
        val csvDateEN: String

        // Get year from date
        if (date != "") {
            // "-" at position 4 of the date means EN
            if (date.substring(4, 5) == "-") engl = true
            // Create date for filename as YYYYMMDD from any format
            csvDateEU = date.substring(6, 10) + date.substring(3, 5) + date.substring(0, 2)
            csvDateEN = date.substring(0, 4) + date.substring(5, 7) + date.substring(8, 10)
        } else {
            hasDate = false
            csvDateEN = ""
            csvDateEU = ""
        }

        csvDate = if (engl) csvDateEN
        else csvDateEU

        if (startTime != "") {
            csvTime = startTime.substring(0, 2) + startTime.substring(3, 5)

            if (hasDate) csvDate = "$csvDate-$csvTime" // yyyymmdd-hhmm
        } else csvDate = "" // has only a value when both date and start time are given

        path.mkdirs() // Just verify path, result ignored

        val tranSect: String?
        val language = Locale.getDefault().toString().substring(0, 2)

        tranSect = when (language) {
            "de" -> {
                "/Transekt_"
            }

            "it" -> {
                "/Transetto_"
            }

            else -> {
                "/Transect_"
            }
        }

        val sortIdent: String?
        val sortMode: String?
        when (outPref) {
            "names" -> {
                sortIdent = "n"
                sortMode = getString(R.string.sortModeN)
            }

            "codes" -> {
                sortIdent = "c"
                sortMode = getString(R.string.sortModeC)
            }

            "sections_names" -> {
                sortIdent = "sn"
                sortMode = getString(R.string.sortModeSN)
            }

            else -> {
                // section_codes
                sortIdent = "sc"
                sortMode = getString(R.string.sortModeSC)
            }
        }

        dataLanguage = prefs.getString("pref_sel_data_lang", "")!!
        if (dataLanguage.isEmpty()) dataLanguage = "--"

        outFile = if (transNoForDir == "" && csvDate == "")
            File(path,
            tranSect + dataLanguage + "_" + getcurDate() + "_" + sortIdent + ".csv")
        else if (transNoForDir == "")
            File(path,
            tranSect + dataLanguage + "_" + csvDate + "_" + sortIdent + ".csv")
        else if (csvDate == "")
            File(path,
            tranSect + dataLanguage + "_" + transNoForDir + "_" + getcurDate() + "_" + sortIdent + ".csv")
        else File(path,
            tranSect + dataLanguage + "_" + transNoForDir + "_" + csvDate + "_" + sortIdent + ".csv")

        var section: Section
        var sectIdent: Int

        //  ♂|♀, ♂, ♀, pupa, larva, ovo (internal and external)
        var summf = 0
        var summ = 0
        var sumf = 0
        var sump = 0
        var suml = 0
        var sumo = 0

        var summfe = 0
        var summe = 0
        var sumfe = 0
        var sumpe = 0
        var sumle = 0
        var sumoe = 0

        val totali: Int
        val totale: Int
        val total: Int
        val sumSpec: Int

        val yyyy: Int
        val mm: Int
        val dd: Int
        var kWeek = 0 // calendar week (Int)

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED == storageState

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard)
            Toast.makeText(
                this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Export purged db as csv
            dbHelper = DbHelper(this)
            database = dbHelper!!.writableDatabase

            // Get number of different species
            sumSpec = countDataSource!!.diffSpec

            //********************************
            // Start creating csv table output
            try {
                val csvWrite = CSVWriter(FileWriter(outFile))

                // Set header according to table representation in spreadsheet
                val arrCol: Array<String?>
                when (outPref) {
                    "names", "codes" -> arrCol = arrayOf(
                        getString(R.string.transectnumber),
                        getString(R.string.inspector),
                        getString(R.string.date),
                        "",
                        getString(R.string.timehead),
                        getString(R.string.temperature),
                        getString(R.string.wind),
                        getString(R.string.clouds),
                        "",
                        getString(R.string.kal_w),
                        "",
                        sortMode,
                        "", "", "", "",
                        getString(R.string.inspection_note),
                    )

                    else -> arrCol = arrayOf(
                        getString(R.string.transectnumber),
                        getString(R.string.inspector),
                        getString(R.string.date),
                        "",
                        getString(R.string.timehead),
                        getString(R.string.temperature),
                        getString(R.string.wind),
                        getString(R.string.clouds),
                        "",
                        getString(R.string.kal_w),
                        "", "",
                        sortMode,
                        "", "", "", "",
                        getString(R.string.inspection_note),
                    )
                }

                csvWrite.writeNext(arrCol) // write line to csv-file

                // Open Head table for head info
                head = headDataSource!!.head
                inspecName = "\"" + head!!.inspector_name + "\""

                // Calculating the week of the year (ISO 8601)
                val cal = Calendar.getInstance()

                if (date.isNotEmpty()) {
                    if (engl) {
                        yyyy = date.substring(0, 4).toInt()
                        mm = date.substring(5, 7).toInt()
                        dd = date.substring(8, 10).toInt()
                    } else {
                        yyyy = date.substring(6, 10).toInt()
                        mm = date.substring(3, 5).toInt()
                        dd = date.substring(0, 2).toInt()
                    }

                    // Example: cal.set(2017, 3, 9) -> 09.04.2017
                    cal.set(yyyy, mm - 1, dd)
                    kWeek = cal.get(Calendar.WEEK_OF_YEAR)
                }

                // 1. headline info with
                //    transect no., inspector name, date, start-time, start-temperature, start-wind,
                //    clouds, calendar week and inspection note
                kw = kWeek.toString()
                val arrMeta: Array<String?>
                when (outPref) {
                    "names", "codes" -> arrMeta = arrayOf(
                        "\"" + transNo + "\"",
                        inspecName,
                        "\"" + date + "\"",
                        getString(R.string.from),
                        "\"" + startTime + "\"",
                        temps.toString(),
                        winds.toString(),
                        clouds.toString(),
                        "",
                        kw,
                        "", "", "", "", "", "",
                        "\"" + inspecNote + "\"",
                    )

                    else -> arrMeta = arrayOf(
                        "\"" + transNo + "\"",
                        inspecName,
                        "\"" + date + "\"",
                        getString(R.string.from),
                        "\"" + startTime + "\"",
                        temps.toString(),
                        winds.toString(),
                        clouds.toString(),
                        "",
                        kw,
                        "", "", "", "", "", "", "",
                        "\"" + inspecNote + "\"",
                    )
                }
                csvWrite.writeNext(arrMeta)

                // 2. headline info with
                //    end-time, end-temperature, end-clouds
                val arrMeta1: Array<String?> = arrayOf(
                    "", "", "", getString(R.string.to), "\"" + endTime + "\"",
                    tempe.toString(), winde.toString(), cloude.toString(),
                )
                csvWrite.writeNext(arrMeta1)

                // Empty row
                val arrEmpt: Array<String?> = arrayOf()
                csvWrite.writeNext(arrEmpt)

                // 1. headline of species counts table
                val arrIE: Array<String?>
                when (outPref) {
                    "names" -> arrIE = arrayOf(
                        "",
                        "",
                        "",
                        "",
                        getString(R.string.internal),
                        "",
                        "",
                        "",
                        "",
                        "",
                        getString(R.string.external)
                    )

                    "codes" -> arrIE = arrayOf(
                        "",
                        "",
                        "",
                        "",
                        getString(R.string.internal),
                        "",
                        "",
                        "",
                        "",
                        "",
                        getString(R.string.external)
                    )

                    "sections_names" -> arrIE = arrayOf(
                        getString(R.string.name_sect),
                        "",
                        getString(R.string.specis),
                        "",
                        "",
                        getString(R.string.internal),
                        "",
                        "",
                        "",
                        "",
                        "",
                        getString(R.string.external)
                    )

                    else -> arrIE = arrayOf(
                        getString(R.string.name_sect),
                        "",
                        getString(R.string.specis),
                        "",
                        "",
                        getString(R.string.internal),
                        "",
                        "",
                        "",
                        "",
                        "",
                        getString(R.string.external)
                    )
                }
                csvWrite.writeNext(arrIE)

                val nameSpecG = Utils.nameSpecG(dataLanguage)

                // 2. Headline of species table
                val arrCol1: Array<String?> = when (outPref) {
                    // Species Name, Local Name, Code, Section, Internal Counts, External Counts, Spec.-Notes
                    "names" ->
                        arrayOf(
                            getString(R.string.name_spec),
                            nameSpecG,
                            getString(R.string.code_spec),
                            getString(R.string.name_sect),
                            getString(R.string.countImagomfHint),
                            getString(R.string.countImagomHint),
                            getString(R.string.countImagofHint),
                            getString(R.string.countPupaHint),
                            getString(R.string.countLarvaHint),
                            getString(R.string.countOvoHint),
                            getString(R.string.countImagomfHint),
                            getString(R.string.countImagomHint),
                            getString(R.string.countImagofHint),
                            getString(R.string.countPupaHint),
                            getString(R.string.countLarvaHint),
                            getString(R.string.countOvoHint),
                            getString(R.string.rem_spec)
                        )
                    // Species Code, Name, Local Name, Section, Internal Counts, External Counts, Spec.-Notes
                    "codes" ->
                        arrayOf(
                            getString(R.string.code_spec),
                            getString(R.string.name_spec),
                            nameSpecG,
                            getString(R.string.name_sect),
                            getString(R.string.countImagomfHint),
                            getString(R.string.countImagomHint),
                            getString(R.string.countImagofHint),
                            getString(R.string.countPupaHint),
                            getString(R.string.countLarvaHint),
                            getString(R.string.countOvoHint),
                            getString(R.string.countImagomfHint),
                            getString(R.string.countImagomHint),
                            getString(R.string.countImagofHint),
                            getString(R.string.countPupaHint),
                            getString(R.string.countLarvaHint),
                            getString(R.string.countOvoHint),
                            getString(R.string.rem_spec)
                        )
                    // Section, Time of 1. Count, Species Name, Local Name, Code, Internal Counts, Spec.-Notes
                    "sections_names" ->
                        arrayOf(
                            getString(R.string.strName),
                            getString(R.string.time_sect),
                            getString(R.string.name_spec),
                            nameSpecG,
                            getString(R.string.code_spec),
                            getString(R.string.countImagomfHint),
                            getString(R.string.countImagomHint),
                            getString(R.string.countImagofHint),
                            getString(R.string.countPupaHint),
                            getString(R.string.countLarvaHint),
                            getString(R.string.countOvoHint),
                            getString(R.string.countImagomfHint),
                            getString(R.string.countImagomHint),
                            getString(R.string.countImagofHint),
                            getString(R.string.countPupaHint),
                            getString(R.string.countLarvaHint),
                            getString(R.string.countOvoHint),
                            getString(R.string.rem_spec)
                        )
                    // Section, Time of 1. Count, Code, Species Name, Local Name, Internal Counts, Spec.-Notes
                    else ->
                        arrayOf(
                            getString(R.string.strName),
                            getString(R.string.time_sect),
                            getString(R.string.code_spec),
                            getString(R.string.name_spec),
                            nameSpecG,
                            getString(R.string.countImagomfHint),
                            getString(R.string.countImagomHint),
                            getString(R.string.countImagofHint),
                            getString(R.string.countPupaHint),
                            getString(R.string.countLarvaHint),
                            getString(R.string.countOvoHint),
                            getString(R.string.countImagomfHint),
                            getString(R.string.countImagomHint),
                            getString(R.string.countImagofHint),
                            getString(R.string.countPupaHint),
                            getString(R.string.countLarvaHint),
                            getString(R.string.countOvoHint),
                            getString(R.string.rem_spec)
                        )
                }
                csvWrite.writeNext(arrCol1)

                //*****************************************************************************
                // Build the internal species array according to the sorted species or sections
                val curCSV: Cursor
                when (outPref) {
                    "names" ->  // Cursor contains list sorted by section and name with all internal count entries > 0
                        //   and all empty counts with a "0" in a section's species remark
                        curCSV = database!!.rawQuery(
                            ("select * from " + DbHelper.COUNT_TABLE
                                    + " WHERE ("
                                    + DbHelper.C_NOTES + " = '0' or "
                                    + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                    + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                    + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                    + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                    + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                    + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                    + " order by " + DbHelper.C_NAME + ", " + DbHelper.C_SECTION_ID),
                            null
                        )

                    "codes" ->  // Cursor contains list sorted by name and section with all internal count entries > 0
                        //   and all empty counts with a "0" in a section's species remark
                        curCSV = database!!.rawQuery(
                            ("select * from " + DbHelper.COUNT_TABLE
                                    + " WHERE ("
                                    + DbHelper.C_NOTES + " = '0' or "
                                    + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                    + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                    + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                    + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                    + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                    + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                    + " order by " + DbHelper.C_CODE + ", " + DbHelper.C_SECTION_ID),
                            null
                        )

                    "sections_names" ->  // Cursor contains list sorted by name and section with all internal count entries > 0
                        //   and all empty counts with a "0" in a section's species remark
                        curCSV = database!!.rawQuery(
                            ("select * from " + DbHelper.COUNT_TABLE
                                    + " WHERE ("
                                    + DbHelper.C_NOTES + " = '0' or "
                                    + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                    + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                    + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                    + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                    + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                    + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                    + " order by " + DbHelper.C_SECTION_ID + ", " + DbHelper.C_NAME),
                            null
                        )

                    else ->  // Cursor contains list sorted by name and section with all internal count entries > 0
                        //   and all empty counts with a "0" in a section's species remark
                        curCSV = database!!.rawQuery(
                            ("select * from " + DbHelper.COUNT_TABLE
                                    + " WHERE ("
                                    + DbHelper.C_NOTES + " = '0' or "
                                    + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                    + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                    + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                    + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                    + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                    + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                    + " order by " + DbHelper.C_SECTION_ID + ", " + DbHelper.C_CODE),
                            null
                        )
                }

                var code: String // current species code
                var nameScifi: String // scientific name
                var nameLocal: String // local name
                var spNotes: String // species notes

                // Internal counts variables
                var countmf: Int
                var countm: Int
                var countf: Int
                var countp: Int
                var countl: Int
                var counte: Int
                var strcountmf: String
                var strcountm: String
                var strcountf: String
                var strcountp: String
                var strcountl: String
                var strcounte: String

                // External counts variables
                var countmfe: Int
                var countme: Int
                var countfe: Int
                var countpe: Int
                var countle: Int
                var countee: Int
                var strcountmfe: String
                var strcountme: String
                var strcountfe: String
                var strcountpe: String
                var strcountle: String
                var strcountee: String

                var sectName: String // name shown in list
                var sectName1 = ""
                var sectName2: String
                var sectTime: String
                val timePattern = "HH:mm:ss"
                val sdf = SimpleDateFormat(timePattern, Locale.getDefault())
                var sectTimeValue: Long

                curCSV.moveToFirst()
                while (!curCSV.isAfterLast) {
                    sectIdent = curCSV.getInt(1)
                    section = sectionDataSource!!.getSection(sectIdent)
                    sectName = section.name

                    if (sectName1 != sectName) { // sectName != ""
                        sectName1 = sectName
                        sectName2 = sectName
                        sectTimeValue = section.datNum() // get Long created_at value
                        val result = Date(sectTimeValue)
                        sectTime = sdf.format(result)
                    } else {
                        sectTime = ""
                        sectName2 = ""
                    }

                    code = "\"" + curCSV.getString(3) + "\"" //species code
                    nameScifi = curCSV.getString(2)
                    nameLocal = curCSV.getString(17)
                    spNotes = "\"" + curCSV.getString(16) + "\""

                    countmf = curCSV.getInt(4)
                    strcountmf = if (countmf > 0) countmf.toString()
                    else ""

                    countm = curCSV.getInt(5)
                    strcountm = if (countm > 0) countm.toString()
                    else ""

                    countf = curCSV.getInt(6)
                    strcountf = if (countf > 0) countf.toString()
                    else ""

                    countp = curCSV.getInt(7)
                    strcountp = if (countp > 0) countp.toString()
                    else ""

                    countl = curCSV.getInt(8)
                    strcountl = if (countl > 0) countl.toString()
                    else ""

                    counte = curCSV.getInt(9)
                    strcounte = if (counte > 0) counte.toString()
                    else ""

                    countmfe = curCSV.getInt(10)
                    strcountmfe = if (countmfe > 0) countmfe.toString()
                    else ""

                    countme = curCSV.getInt(11)
                    strcountme = if (countme > 0) countme.toString()
                    else ""

                    countfe = curCSV.getInt(12)
                    strcountfe = if (countfe > 0) countfe.toString()
                    else ""

                    countpe = curCSV.getInt(13)
                    strcountpe = if (countpe > 0) countpe.toString()
                    else ""

                    countle = curCSV.getInt(14)
                    strcountle = if (countle > 0) countle.toString()
                    else ""

                    countee = curCSV.getInt(15)
                    strcountee = if (countee > 0) countee.toString()
                    else ""

                    val arrStr: Array<String?> = when (outPref) {
                        "names" ->
                            // Build line in species table for order of species names
                            arrayOf(
                                nameScifi,  // species name
                                nameLocal,  // species local name
                                code,  // species code as String
                                sectName,  // section name
                                strcountmf,  // count mf
                                strcountm,  // count m
                                strcountf,  // count f
                                strcountp,  // count p
                                strcountl,  // count l
                                strcounte,  // count e
                                strcountmfe,  // count mfe
                                strcountme,  // count me
                                strcountfe,  // count fe
                                strcountpe,  // count pe
                                strcountle,  // count le
                                strcountee,  // count ee
                                spNotes // spec. notes
                            )

                        "codes" ->
                            // Build line in species table for order of species codes
                            arrayOf(
                                code,  // species code as String
                                nameScifi,  // species name
                                nameLocal,  // species local name
                                sectName,  // section name
                                strcountmf,  // count mf
                                strcountm,  // count m
                                strcountf,  // count f
                                strcountp,  // count p
                                strcountl,  // count l
                                strcounte,  // count e
                                strcountmfe,  // count mfe
                                strcountme,  // count me
                                strcountfe,  // count fe
                                strcountpe,  // count pe
                                strcountle,  // count le
                                strcountee,  // count ee
                                spNotes // spec. notes
                            )

                        "sections_names" ->
                            // Build line in species table for order of sections and species names
                            arrayOf(
                                sectName2,  // section name
                                sectTime,  // time of 1. count in section
                                nameScifi,  // species name
                                nameLocal,  // species local name
                                code,  // species code as String
                                strcountmf,  // count mf
                                strcountm,  // count m
                                strcountf,  // count f
                                strcountp,  // count p
                                strcountl,  // count l
                                strcounte,  // count e
                                strcountmfe,  // count mfe
                                strcountme,  // count me
                                strcountfe,  // count fe
                                strcountpe,  // count pe
                                strcountle,  // count le
                                strcountee,  // count ee
                                spNotes // spec. notes
                            )

                        else ->  // section_codes
                            // Build line in species table for order of sections and species codes
                            arrayOf(
                                sectName2,  // section name
                                sectTime,  // time of 1. count in section
                                code,  // species code as String
                                nameScifi,  // species name
                                nameLocal,  // species local name
                                strcountmf,  // count mf
                                strcountm,  // count m
                                strcountf,  // count f
                                strcountp,  // count p
                                strcountl,  // count l
                                strcounte,  // count e
                                strcountmfe,  // count mfe
                                strcountme,  // count me
                                strcountfe,  // count fe
                                strcountpe,  // count pe
                                strcountle,  // count le
                                strcountee,  // count ee
                                spNotes // spec. notes
                            )
                    }
                    csvWrite.writeNext(arrStr)

                    summf += curCSV.getInt(4)
                    summ += curCSV.getInt(5)
                    sumf += curCSV.getInt(6)
                    sump += curCSV.getInt(7)
                    suml += curCSV.getInt(8)
                    sumo += curCSV.getInt(9)
                    summfe += curCSV.getInt(10)
                    summe += curCSV.getInt(11)
                    sumfe += curCSV.getInt(12)
                    sumpe += curCSV.getInt(13)
                    sumle += curCSV.getInt(14)
                    sumoe += curCSV.getInt(15)
                    curCSV.moveToNext()
                }
                curCSV.close()

                //**************************
                // Empty row before sum area
                csvWrite.writeNext(arrEmpt)

                totali = summf + summ + sumf + sump + suml + sumo
                totale = summfe + summe + sumfe + sumpe + sumle + sumoe
                total = totali + totale

                // Sum area headline: Internal, External
                val arrIE1: Array<String?>
                when (outPref) {
                    "names" -> arrIE1 = arrayOf(
                        "",
                        "",
                        "",
                        "",
                        getString(R.string.internal),
                        "",
                        "",
                        "",
                        "",
                        "",
                        getString(R.string.external)
                    )

                    "codes" -> arrIE1 = arrayOf(
                        "",
                        "",
                        "",
                        "",
                        getString(R.string.internal),
                        "",
                        "",
                        "",
                        "",
                        "",
                        getString(R.string.external)
                    )

                    "sections_names" -> arrIE1 = arrayOf(
                        "",
                        "",
                        "",
                        "",
                        "",
                        getString(R.string.internal),
                        "",
                        "",
                        "",
                        "",
                        "",
                        getString(R.string.external)
                    )

                    else -> arrIE1 = arrayOf(
                        "",
                        "",
                        "",
                        "",
                        "",
                        getString(R.string.internal),
                        "",
                        "",
                        "",
                        "",
                        "",
                        getString(R.string.external)
                    )
                }
                csvWrite.writeNext(arrIE1)

                // Internal counts, External counts, Totals
                val arrCol2: Array<String?>
                when (outPref) {
                    "names", "codes" -> arrCol2 = arrayOf(
                        "",
                        getString(R.string.sumSpec),
                        "",
                        "",
                        getString(R.string.countImagomfHint),
                        getString(R.string.countImagomHint),
                        getString(R.string.countImagofHint),
                        getString(R.string.countPupaHint),
                        getString(R.string.countLarvaHint),
                        getString(R.string.countOvoHint),
                        getString(R.string.countImagomfHint),
                        getString(R.string.countImagomHint),
                        getString(R.string.countImagofHint),
                        getString(R.string.countPupaHint),
                        getString(R.string.countLarvaHint),
                        getString(R.string.countOvoHint),
                        getString(R.string.hintTotal)
                    )

                    else -> arrCol2 = arrayOf(
                        "",
                        "",
                        getString(R.string.sumSpec),
                        "",
                        "",
                        getString(R.string.countImagomfHint),
                        getString(R.string.countImagomHint),
                        getString(R.string.countImagofHint),
                        getString(R.string.countPupaHint),
                        getString(R.string.countLarvaHint),
                        getString(R.string.countOvoHint),
                        getString(R.string.countImagomfHint),
                        getString(R.string.countImagomHint),
                        getString(R.string.countImagofHint),
                        getString(R.string.countPupaHint),
                        getString(R.string.countLarvaHint),
                        getString(R.string.countOvoHint),
                        getString(R.string.hintTotal)
                    )
                }
                csvWrite.writeNext(arrCol2)

                // Internal sums

                // Sums and totals variables
                val strsummf = if (summf > 0) summf.toString()
                else ""

                val strsumm = if (summ > 0) summ.toString()
                else ""

                val strsumf = if (sumf > 0) sumf.toString()
                else ""

                val strsump = if (sump > 0) sump.toString()
                else ""

                val strsuml = if (suml > 0) suml.toString()
                else ""

                val strsumo = if (sumo > 0) sumo.toString()
                else ""

                val strtotali = if (totali > 0) totali.toString()
                else ""

                // Write internal total sums
                val arrSumi: Array<String?> = when (outPref) {
                    "names", "codes" -> arrayOf(
                        "",
                        sumSpec.toString(), "", getString(R.string.sumi),
                        strsummf, strsumm, strsumf, strsump, strsuml, strsumo,
                        "", "", "", "", "", "", strtotali
                    )

                    else -> arrayOf(
                        "", "",
                        sumSpec.toString(), "", getString(R.string.sumi),
                        strsummf, strsumm, strsumf, strsump, strsuml, strsumo,
                        "", "", "", "", "", "", strtotali
                    )
                }
                csvWrite.writeNext(arrSumi)

                // External sums
                val strsummfe = if (summfe > 0) summfe.toString()
                else ""

                val strsumme = if (summe > 0) summe.toString()
                else ""

                val strsumfe = if (sumfe > 0) sumfe.toString()
                else ""

                val strsumpe = if (sumpe > 0) sumpe.toString()
                else ""

                val strsumle = if (sumle > 0) sumle.toString()
                else ""

                val strsumoe = if (sumoe > 0) sumoe.toString()
                else ""

                val strtotale = if (totale > 0) totale.toString()
                else ""

                // Write external total sums
                val arrSume: Array<String?> = when (outPref) {
                    "names", "codes" -> arrayOf(
                        "", "", "",
                        getString(R.string.sume), "", "", "", "", "", "",
                        strsummfe, strsumme, strsumfe, strsumpe, strsumle, strsumoe, strtotale
                    )

                    else -> arrayOf(
                        "", "", "", "",
                        getString(R.string.sume), "", "", "", "", "", "",
                        strsummfe, strsumme, strsumfe, strsumpe, strsumle, strsumoe, strtotale
                    )
                }
                csvWrite.writeNext(arrSume)

                // Overall totals
                val strtotal = if (total > 0) total.toString()
                else ""

                // Write overall total sum
                val arrTotal: Array<String?> = when (outPref) {
                    "names", "codes" -> arrayOf(
                        "", "", "",
                        getString(R.string.sum_total), "", "", "", "", "", "",
                        "", "", "", "", "", "", strtotal
                    )

                    else -> arrayOf(
                        "", "", "", "",
                        getString(R.string.sum_total), "", "", "", "", "", "",
                        "", "", "", "", "", "", strtotal
                    )
                }
                csvWrite.writeNext(arrTotal)

                csvWrite.close()

                mesg = getString(R.string.saveCSV)
                Toast.makeText(
                    this,
                    fromHtml("<font color='blue'>$mesg</font>"),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: Exception) {
                mesg = getString(R.string.saveFail)
                Toast.makeText(
                    this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG
                ).show()

                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.e(TAG, "2899, csv write external failed")
            }
            dbHelper!!.close()
        }
    }
    // End of exportDb2CSV()

    /***********************************************************************************************
     * // Exports DB contents as Transekt_dL_TransNo_yyyyMMdd_HHmm.xlsx to
     * // Documents/TransektCount/ with purged data set.
     * // Spreadsheet programs can directly load this formatted xlsx file
     */
    private fun exportDb2XLSX() {
        mesg = getString(R.string.saveXLSX)
        Toast.makeText(
            this,
            fromHtml("<font color='blue'>$mesg</font>"),
            Toast.LENGTH_SHORT
        ).show()

        val inspecName: String
        val kw: String // kw = calendar week (String)

        // Open Meta table for meta info
        meta = metaDataSource!!.meta
        val temps: Int = meta!!.temps
        val tempe: Int = meta!!.tempe // temperature at start time and end time
        val winds: Int = meta!!.winds
        val winde: Int = meta!!.winde // wind
        val clouds: Int = meta!!.clouds
        val cloude: Int = meta!!.cloude // clouds
        val date: String = meta!!.date // has system date
        val startTime: String = meta!!.start_tm
        val endTime: String = meta!!.end_tm
        val inspecNote: String = meta!!.note

        var engl = false
        var hasDate = true
        var xlsxDate: String
        val xlsxTime: String
        val xlsxDateEU: String
        val xlsxDateEN: String

        // Get year from date
        if (date != "") {
            // "-" at position 4 of the date means EN
            if (date.substring(4, 5) == "-") engl = true
            // Create date for filename as YYYYMMDD from any format
            xlsxDateEU = date.substring(6, 10) + date.substring(3, 5) + date.substring(0, 2)
            xlsxDateEN = date.substring(0, 4) + date.substring(5, 7) + date.substring(8, 10)
        } else {
            hasDate = false
            xlsxDateEN = ""
            xlsxDateEU = ""
        }

        xlsxDate = if (engl) xlsxDateEN
        else xlsxDateEU

        if (startTime != "") {
            xlsxTime = startTime.substring(0, 2) + startTime.substring(3, 5)

            if (hasDate) xlsxDate = "$xlsxDate-$xlsxTime" // yyyymmdd-hhmm
        } else xlsxDate = ""
        // has only a value when both date and start time are given


        // outFile -> /storage/emulated/0/Documents/TransektCount/tranSect_DL_transNo_yyyyMMdd_HHmm_si.xlsx
        var path: File = Environment.getExternalStorageDirectory()
        path = File("$path/Documents/TransektCount")

        path.mkdirs() // Just verify path, result ignored

        val tranSect: String?
        val language = Locale.getDefault().toString().substring(0, 2)

        tranSect = when (language) {
            "de" -> {
                "/Transekt_"
            }

            "it" -> {
                "/Transetto_"
            }

            else -> {
                "/Transect_"
            }
        }

        val sortIdent: String?
        val sortMode: String?
        when (outPref) {
            "names" -> {
                sortIdent = "n"
                sortMode = getString(R.string.sortModeN)
            }

            "codes" -> {
                sortIdent = "c"
                sortMode = getString(R.string.sortModeC)
            }

            "sections_names" -> {
                sortIdent = "sn"
                sortMode = getString(R.string.sortModeSN)
            }

            else -> {
                // section_codes
                sortIdent = "sc"
                sortMode = getString(R.string.sortModeSC)
            }
        }

        dataLanguage = prefs.getString("pref_sel_data_lang", "")!!
        if (dataLanguage.isEmpty()) dataLanguage = "--"

        outFile = if (transNoForDir == "" && xlsxDate == "")
            File(path,
            tranSect + dataLanguage + "_" + getcurDate() + "_" + sortIdent + ".xlsx")
        else if (transNoForDir == "")
            File(path,
            tranSect + dataLanguage + "_" + xlsxDate + "_" + sortIdent + ".xlsx")
        else if (xlsxDate == "")
            File(path,
            tranSect + dataLanguage + "_" + transNoForDir + "_" + getcurDate() + "_" + sortIdent + ".xlsx")
        else File(path,
            tranSect + dataLanguage + "_" + transNoForDir + "_" + xlsxDate + "_" + sortIdent + ".xlsx")

        val outputStream: FileOutputStream
        try {
            outputStream = FileOutputStream(outFile)
        } catch (e: FileNotFoundException) {
            throw RuntimeException(e)
        }

        var section: Section
        var sectIdent: Int

        //  ♂|♀, ♂, ♀, pupa, larva, ovo (internal and external)
        var summf = 0
        var summ = 0
        var sumf = 0
        var sump = 0
        var suml = 0
        var sumo = 0

        var summfe = 0
        var summe = 0
        var sumfe = 0
        var sumpe = 0
        var sumle = 0
        var sumoe = 0

        val totali: Int
        val totale: Int
        val total: Int
        val sumSpec: Int

        val yyyy: Int
        val mm: Int
        val dd: Int
        var kWeek = 0 // calendar week (Int)

        // Prepare the fastexcel sheet
        val wb = Workbook(outputStream, "TransektCount", "1.0")
        wb.properties().setTitle(getString(R.string.results)) //
        wb.setGlobalDefaultFont("Arial", 11.0)

        val ws = wb.newWorksheet(getString(R.string.results))
        ws.paperSize(PaperSize.A4_PAPER)
        ws.pageOrientation("landscape")
        ws.freezePane(0, 6) // Fixed lines when scrolling
        ws.repeatRows(0, 5) // Fixed lines for printing
        ws.firstPageNumber(1)
        ws.footer("&P / &N", Position.CENTER)
        ws.fitToWidth(1.toShort())
        ws.fitToHeight(0.toShort())

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED == storageState

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard)
            Toast.makeText(
                this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Export purged db as xlsx
            dbHelper = DbHelper(this)
            database = dbHelper!!.writableDatabase

            // Get number of different species
            sumSpec = countDataSource!!.diffSpec

            //**************************
            // Start creating xlsx table
            // Row 0: Headline
            ws.value(0, 0, getString(R.string.transectnumber))
            ws.value(0, 1, getString(R.string.inspector))
            ws.value(0, 2, getString(R.string.date))
            ws.value(0, 4, getString(R.string.timehead))
            ws.value(0, 5, getString(R.string.temperature))
            ws.value(0, 6, getString(R.string.wind))
            ws.value(0, 7, getString(R.string.clouds))
            ws.value(0, 9, getString(R.string.kal_w))
            ws.value(0, 11, getString(R.string.inspection_note))
            ws.value(0, 15, sortMode)

            ws.range(0, 0, 0, 14).style().fillColor("CCCCCC")
                .borderStyle(BorderSide.BOTTOM, "thin").fontSize(12).bold().set()
            ws.range(0, 4, 0, 9).style().horizontalAlignment("center").set()
            ws.range(0, 11, 0, 14).merge()
            ws.style(0, 15).horizontalAlignment("right").set()

            when (outPref) {
                "names", "codes" -> ws.range(0, 15, 0, 16).merge()
                else -> ws.range(0, 15, 0, 17).merge()
            }

            // Open Head table for head info
            head = headDataSource!!.head
            inspecName = head!!.inspector_name

            // Calculating the week of the year (ISO 8601)
            val cal = Calendar.getInstance()

            if (date.isNotEmpty()) {
                if (engl) {
                    yyyy = date.substring(0, 4).toInt()
                    mm = date.substring(5, 7).toInt()
                    dd = date.substring(8, 10).toInt()
                } else {
                    yyyy = date.substring(6, 10).toInt()
                    mm = date.substring(3, 5).toInt()
                    dd = date.substring(0, 2).toInt()
                }

                // Example: cal.set(2017, 3, 9) -> 09.04.2017
                cal.set(yyyy, mm - 1, dd)
                kWeek = cal.get(Calendar.WEEK_OF_YEAR)
            }

            // 1. metadata info with
            //    transect no., inspector name, date, start-time, start-temperature, start-wind,
            //    clouds, calendar week and inspection note
            kw = kWeek.toString()
            ws.value(1, 0, transNo)
            ws.value(1, 1, inspecName)
            ws.value(1, 2, date)
            ws.value(1, 3, getString(R.string.from))
            ws.value(1, 4, startTime)
            ws.value(1, 5, temps.toString())
            ws.value(1, 6, winds.toString())
            ws.value(1, 7, clouds.toString())
            ws.value(1, 9, kw)
            ws.value(1, 11, inspecNote)
            ws.style(1, 3).horizontalAlignment("right").set()
            ws.range(1, 4, 0, 9).style().horizontalAlignment("center").set()
            ws.range(1, 11, 1, 14).merge()

            // 2. metadata info with
            //    end-time, end-temperature, end-clouds
            ws.value(2, 3, getString(R.string.to))
            ws.value(2, 4, endTime)
            ws.value(2, 5, tempe.toString())
            ws.value(2, 6, winde.toString())
            ws.value(2, 7, cloude.toString())
            ws.style(2, 3).horizontalAlignment("right").set()
            ws.range(2, 4, 0, 7).style().horizontalAlignment("center").set()

            // Headline 1 of species table
            when (outPref) {
                "names" -> {
                    ws.value(4, 4, getString(R.string.internal))
                    ws.value(4, 10, getString(R.string.external))
                    ws.style(4, 4).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(4, 10).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(4, 16).borderStyle(BorderSide.LEFT, "thin").bold().set()
                }

                "codes" -> {
                    ws.value(4, 4, getString(R.string.internal))
                    ws.value(4, 10, getString(R.string.external))
                    ws.style(4, 4).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(4, 10).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(4, 16).borderStyle(BorderSide.LEFT, "thin").bold().set()
                }

                "sections_names" -> {
                    ws.value(4, 0, getString(R.string.name_sect))
                    ws.value(4, 2, getString(R.string.specis))
                    ws.value(4, 5, getString(R.string.internal))
                    ws.value(4, 11, getString(R.string.external))
                    ws.style(4, 0).bold().set()
                    ws.style(4, 2).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(4, 5).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(4, 11).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(4, 17).borderStyle(BorderSide.LEFT, "thin").bold().set()
                }

                else -> {
                    ws.value(4, 0, getString(R.string.name_sect))
                    ws.value(4, 2, getString(R.string.specis))
                    ws.value(4, 5, getString(R.string.internal))
                    ws.value(4, 11, getString(R.string.external))
                    ws.style(4, 0).bold().set()
                    ws.style(4, 2).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(4, 5).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(4, 11).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(4, 17).borderStyle(BorderSide.LEFT, "thin").bold().set()
                }
            }

            val nameSpecG = Utils.nameSpecG(dataLanguage)

            // Headline 2 of species table
            when (outPref) {
                "names" -> {
                    // Species Name, Local Name, Code, Section, Internal Counts, External Counts,
                    //   Spec.-Notes
                    ws.value(5, 0, getString(R.string.name_spec))
                    ws.value(5, 1, nameSpecG)
                    ws.value(5, 2, getString(R.string.code_spec))
                    ws.value(5, 3, getString(R.string.name_sect))
                    ws.value(5, 4, getString(R.string.countImagomfHint))
                    ws.value(5, 5, getString(R.string.countImagomHint))
                    ws.value(5, 6, getString(R.string.countImagofHint))
                    ws.value(5, 7, getString(R.string.countPupaHint))
                    ws.value(5, 8, getString(R.string.countLarvaHint))
                    ws.value(5, 9, getString(R.string.countOvoHint))
                    ws.value(5, 10, getString(R.string.countImagomfHint))
                    ws.value(5, 11, getString(R.string.countImagomHint))
                    ws.value(5, 12, getString(R.string.countImagofHint))
                    ws.value(5, 13, getString(R.string.countPupaHint))
                    ws.value(5, 14, getString(R.string.countLarvaHint))
                    ws.value(5, 15, getString(R.string.countOvoHint))
                    ws.value(5, 16, getString(R.string.rem_spec))

                    ws.range(5, 0, 5, 16).style()
                        .borderStyle(BorderSide.BOTTOM, "thin").bold().set()
                    ws.range(5, 2, 5, 15).style()
                        .horizontalAlignment("center").set()
                    ws.style(5, 4).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").bold().set()
                    ws.style(5, 10).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").bold().set()
                    ws.style(5, 16).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").bold().set()
                }

                "codes" -> {
                    // Species Code, Name, Local Name, Section, Internal Counts, External Counts,
                    //   Spec.-Notes
                    ws.value(5, 0, getString(R.string.code_spec))
                    ws.value(5, 1, getString(R.string.name_spec))
                    ws.value(5, 2, nameSpecG)
                    ws.value(5, 3, getString(R.string.name_sect))
                    ws.value(5, 4, getString(R.string.countImagomfHint))
                    ws.value(5, 5, getString(R.string.countImagomHint))
                    ws.value(5, 6, getString(R.string.countImagofHint))
                    ws.value(5, 7, getString(R.string.countPupaHint))
                    ws.value(5, 8, getString(R.string.countLarvaHint))
                    ws.value(5, 9, getString(R.string.countOvoHint))
                    ws.value(5, 10, getString(R.string.countImagomfHint))
                    ws.value(5, 11, getString(R.string.countImagomHint))
                    ws.value(5, 12, getString(R.string.countImagofHint))
                    ws.value(5, 13, getString(R.string.countPupaHint))
                    ws.value(5, 14, getString(R.string.countLarvaHint))
                    ws.value(5, 15, getString(R.string.countOvoHint))
                    ws.value(5, 16, getString(R.string.rem_spec))

                    ws.range(5, 0, 5, 16).style()
                        .borderStyle(BorderSide.BOTTOM, "thin").bold().set()
                    ws.style(5, 0).horizontalAlignment("center").set()
                    ws.range(5, 3, 5, 15).style()
                        .horizontalAlignment("center").set()
                    ws.style(5, 4).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").bold().set()
                    ws.style(5, 10).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").bold().set()
                    ws.style(5, 16).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").bold().set()
                }

                "sections_names" -> {
                    // Section, Time of 1. Count, Species Name, Local Name, Code, Internal Counts,
                    //   External Counts, Spec.-Notes
                    ws.value(5, 0, getString(R.string.strName))
                    ws.value(5, 1, getString(R.string.time_sect))
                    ws.value(5, 2, getString(R.string.strName))
                    ws.value(5, 3, nameSpecG)
                    ws.value(5, 4, getString(R.string.code_spec))
                    ws.value(5, 5, getString(R.string.countImagomfHint))
                    ws.value(5, 6, getString(R.string.countImagomHint))
                    ws.value(5, 7, getString(R.string.countImagofHint))
                    ws.value(5, 8, getString(R.string.countPupaHint))
                    ws.value(5, 9, getString(R.string.countLarvaHint))
                    ws.value(5, 10, getString(R.string.countOvoHint))
                    ws.value(5, 11, getString(R.string.countImagomfHint))
                    ws.value(5, 12, getString(R.string.countImagomHint))
                    ws.value(5, 13, getString(R.string.countImagofHint))
                    ws.value(5, 14, getString(R.string.countPupaHint))
                    ws.value(5, 15, getString(R.string.countLarvaHint))
                    ws.value(5, 16, getString(R.string.countOvoHint))
                    ws.value(5, 17, getString(R.string.rem_spec))

                    ws.range(5, 0, 5, 17).style()
                        .borderStyle(BorderSide.BOTTOM, "thin").bold().set()
                    ws.range(5, 4, 5, 16).style()
                        .horizontalAlignment("center").set()
                    ws.style(5, 2).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").set()
                    ws.style(5, 5).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").set()
                    ws.style(5, 11).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").set()
                    ws.style(5, 17).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin")
                        .horizontalAlignment("left").set()
                }

                else -> {
                    // section_codes
                    // Section, Time of 1. Count, Code, Species Name, Local Name, Internal Counts,
                    //   External Counts, Spec.-Notes
                    ws.value(5, 0, getString(R.string.strName))
                    ws.value(5, 1, getString(R.string.time_sect))
                    ws.value(5, 2, getString(R.string.code_spec))
                    ws.value(5, 3, getString(R.string.name_spec))
                    ws.value(5, 4, nameSpecG)
                    ws.value(5, 5, getString(R.string.countImagomfHint))
                    ws.value(5, 6, getString(R.string.countImagomHint))
                    ws.value(5, 7, getString(R.string.countImagofHint))
                    ws.value(5, 8, getString(R.string.countPupaHint))
                    ws.value(5, 9, getString(R.string.countLarvaHint))
                    ws.value(5, 10, getString(R.string.countOvoHint))
                    ws.value(5, 11, getString(R.string.countImagomfHint))
                    ws.value(5, 12, getString(R.string.countImagomHint))
                    ws.value(5, 13, getString(R.string.countImagofHint))
                    ws.value(5, 14, getString(R.string.countPupaHint))
                    ws.value(5, 15, getString(R.string.countLarvaHint))
                    ws.value(5, 16, getString(R.string.countOvoHint))
                    ws.value(5, 17, getString(R.string.rem_spec))

                    ws.range(5, 0, 5, 17).style()
                        .borderStyle(BorderSide.BOTTOM, "thin").bold().set()
                    ws.style(5, 2).horizontalAlignment("center").set()
                    ws.range(5, 5, 5, 17).style()
                        .horizontalAlignment("center").set()
                    ws.style(5, 2).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").set()
                    ws.style(5, 5).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").set()
                    ws.style(5, 11).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").set()
                    ws.style(5, 17).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin")
                        .horizontalAlignment("left").set()
                }
            }

            //*****************************************************************************
            // Build the internal species array according to the sorted species or sections
            val curXLSX: Cursor
            when (outPref) {
                "names" ->  // Cursor contains list sorted by section and name with all internal count entries > 0
                    //   and all empty counts with a "0" in a section's species remark
                    curXLSX = database!!.rawQuery(
                        ("select * from " + DbHelper.COUNT_TABLE
                                + " WHERE ("
                                + DbHelper.C_NOTES + " = '0' or "
                                + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                + " order by " + DbHelper.C_NAME + ", " + DbHelper.C_SECTION_ID),
                        null
                    )

                "codes" ->  // Cursor contains list sorted by name and section with all internal count entries > 0
                    //   and all empty counts with a "0" in a section's species remark
                    curXLSX = database!!.rawQuery(
                        ("select * from " + DbHelper.COUNT_TABLE
                                + " WHERE ("
                                + DbHelper.C_NOTES + " = '0' or "
                                + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                + " order by " + DbHelper.C_CODE + ", " + DbHelper.C_SECTION_ID),
                        null
                    )

                "sections_names" ->  // Cursor contains list sorted by name and section with all internal count entries > 0
                    //   and all empty counts with a "0" in a section's species remark
                    curXLSX = database!!.rawQuery(
                        ("select * from " + DbHelper.COUNT_TABLE
                                + " WHERE ("
                                + DbHelper.C_NOTES + " = '0' or "
                                + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                + " order by " + DbHelper.C_SECTION_ID + ", " + DbHelper.C_NAME),
                        null
                    )

                else ->  // Cursor contains list sorted by name and section with all internal count entries > 0
                    //   and all empty counts with a "0" in a section's species remark
                    curXLSX = database!!.rawQuery(
                        ("select * from " + DbHelper.COUNT_TABLE
                                + " WHERE ("
                                + DbHelper.C_NOTES + " = '0' or "
                                + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                + " order by " + DbHelper.C_SECTION_ID + ", " + DbHelper.C_CODE),
                        null
                    )
            }

            var code: String  // current species code
            var nameScifi: String  // scientific name
            var nameLocal: String  // local name
            var spNotes: String  // species notes

            // Internal counts variables
            var countmf: Int
            var countm: Int
            var countf: Int
            var countp: Int
            var countl: Int
            var counte: Int

            // External counts variables
            var countmfe: Int
            var countme: Int
            var countfe: Int
            var countpe: Int
            var countle: Int
            var countee: Int

            var sectName: String  // name shown in list
            var sectName1 = ""
            var sectName2: String
            var sectTime: String
            val timePattern = "HH:mm:ss"
            val sdf = SimpleDateFormat(timePattern, Locale.getDefault())
            var sectTimeValue: Long

            // Prepare species table data
            var tableIndex = 6
            curXLSX.moveToFirst()
            while (!curXLSX.isAfterLast) {
                sectIdent = curXLSX.getInt(1)
                section = sectionDataSource!!.getSection(sectIdent)
                sectName = section.name

                if (sectName1 != sectName) { // sectName != ""
                    sectName1 = sectName
                    sectName2 = sectName
                    sectTimeValue = section.datNum() // get Long created_at value
                    val result = Date(sectTimeValue)
                    sectTime = sdf.format(result)
                } else {
                    sectTime = ""
                    sectName2 = ""
                }

                code = curXLSX.getString(3) //species code
                nameScifi = curXLSX.getString(2)
                nameLocal = curXLSX.getString(17)
                spNotes = curXLSX.getString(16)

                countmf = curXLSX.getInt(4)
                countm = curXLSX.getInt(5)
                countf = curXLSX.getInt(6)
                countp = curXLSX.getInt(7)
                countl = curXLSX.getInt(8)
                counte = curXLSX.getInt(9)

                countmfe = curXLSX.getInt(10)
                countme = curXLSX.getInt(11)
                countfe = curXLSX.getInt(12)
                countpe = curXLSX.getInt(13)
                countle = curXLSX.getInt(14)
                countee = curXLSX.getInt(15)

                val even = tableIndex % 2 == 0

                // Build the species table with alternating white and gray background
                when (outPref) {
                    "names" -> {
                        // Build line in species table for order of species
                        ws.value(tableIndex, 0, nameScifi)
                        ws.value(tableIndex, 1, nameLocal)
                        ws.value(tableIndex, 2, code)
                        ws.value(tableIndex, 3, sectName)

                        if (countmf == 0) ws.value(tableIndex, 4, "")
                        else ws.value(tableIndex, 4, countmf)
                        if (countm == 0) ws.value(tableIndex, 5, "")
                        else ws.value(tableIndex, 5, countm)
                        if (countf == 0) ws.value(tableIndex, 6, "")
                        else ws.value(tableIndex, 6, countf)
                        if (countp == 0) ws.value(tableIndex, 7, "")
                        else ws.value(tableIndex, 7, countp)
                        if (countl == 0) ws.value(tableIndex, 8, "")
                        else ws.value(tableIndex, 8, countl)
                        if (counte == 0) ws.value(tableIndex, 9, "")
                        else ws.value(tableIndex, 9, counte)

                        if (countmfe == 0) ws.value(tableIndex, 10, "")
                        else ws.value(tableIndex, 10, countmfe)
                        if (countme == 0) ws.value(tableIndex, 11, "")
                        else ws.value(tableIndex, 11, countme)
                        if (countfe == 0) ws.value(tableIndex, 12, "")
                        else ws.value(tableIndex, 12, countfe)
                        if (countpe == 0) ws.value(tableIndex, 13, "")
                        else ws.value(tableIndex, 13, countpe)
                        if (countle == 0) ws.value(tableIndex, 14, "")
                        else ws.value(tableIndex, 14, countle)
                        if (countee == 0) ws.value(tableIndex, 15, "")
                        else ws.value(tableIndex, 15, countee)

                        ws.value(tableIndex, 16, spNotes)

                        // Conditional species notes, if "0" -> red
                        if (spNotes == "0") ws.style(tableIndex, 16).fontColor("FF0000").set()

                        // Internal counts in green
                        ws.range(tableIndex, 4, tableIndex, 9)
                            .style().fontColor("008000").bold().set()

                        // External counts in red
                        ws.range(tableIndex, 10, tableIndex, 15)
                            .style().fontColor("FF0000").bold().set()

                        // Set alignment of columns code, section name and counts to center
                        ws.range(tableIndex, 2, tableIndex, 15)
                            .style().horizontalAlignment("center").set()

                        // Left borders
                        ws.style(tableIndex, 4).borderStyle(BorderSide.LEFT, "thin").set()
                        ws.style(tableIndex, 10).borderStyle(BorderSide.LEFT, "thin").set()
                        ws.style(tableIndex, 16).borderStyle(BorderSide.LEFT, "thin").set()
                    }

                    "codes" -> {
                        // Build line in species table for order of codes
                        ws.value(tableIndex, 0, code)
                        ws.value(tableIndex, 1, nameScifi)
                        ws.value(tableIndex, 2, nameLocal)
                        ws.value(tableIndex, 3, sectName)

                        if (countmf == 0) ws.value(tableIndex, 4, "")
                        else ws.value(tableIndex, 4, countmf)
                        if (countm == 0) ws.value(tableIndex, 5, "")
                        else ws.value(tableIndex, 5, countm)
                        if (countf == 0) ws.value(tableIndex, 6, "")
                        else ws.value(tableIndex, 6, countf)
                        if (countp == 0) ws.value(tableIndex, 7, "")
                        else ws.value(tableIndex, 7, countp)
                        if (countl == 0) ws.value(tableIndex, 8, "")
                        else ws.value(tableIndex, 8, countl)
                        if (counte == 0) ws.value(tableIndex, 9, "")
                        else ws.value(tableIndex, 9, counte)

                        if (countmfe == 0) ws.value(tableIndex, 10, "")
                        else ws.value(tableIndex, 10, countmfe)
                        if (countme == 0) ws.value(tableIndex, 11, "")
                        else ws.value(tableIndex, 11, countme)
                        if (countfe == 0) ws.value(tableIndex, 12, "")
                        else ws.value(tableIndex, 12, countfe)
                        if (countpe == 0) ws.value(tableIndex, 13, "")
                        else ws.value(tableIndex, 13, countpe)
                        if (countle == 0) ws.value(tableIndex, 14, "")
                        else ws.value(tableIndex, 14, countle)
                        if (countee == 0) ws.value(tableIndex, 15, "")
                        else ws.value(tableIndex, 15, countee)

                        ws.value(tableIndex, 16, spNotes)

                        ws.style(tableIndex, 0).horizontalAlignment("center").set()

                        // Conditional species notes, if "0" -> red
                        if (spNotes == "0") ws.style(tableIndex, 16).fontColor("FF0000").set()

                        // Internal counts in green
                        ws.range(tableIndex, 4, tableIndex, 9)
                            .style().fontColor("008000").bold().set()

                        // External counts in red
                        ws.range(tableIndex, 10, tableIndex, 15)
                            .style().fontColor("FF0000").bold().set()

                        // Set alignment of columns section name and counts to center
                        ws.range(tableIndex, 3, tableIndex, 15)
                            .style().horizontalAlignment("center").set()

                        // Left borders
                        ws.style(tableIndex, 4).borderStyle(BorderSide.LEFT, "thin").set()
                        ws.style(tableIndex, 10).borderStyle(BorderSide.LEFT, "thin").set()
                        ws.style(tableIndex, 16).borderStyle(BorderSide.LEFT, "thin").set()
                    }

                    "sections_names" -> {
                        // Build line in species table for order of sections
                        ws.value(tableIndex, 0, sectName2)
                        ws.value(tableIndex, 1, sectTime)
                        ws.value(tableIndex, 2, nameScifi)
                        ws.value(tableIndex, 3, nameLocal)
                        ws.value(tableIndex, 4, code)

                        if (countmf == 0) ws.value(tableIndex, 5, "")
                        else ws.value(tableIndex, 5, countmf)
                        if (countm == 0) ws.value(tableIndex, 6, "")
                        else ws.value(tableIndex, 6, countm)
                        if (countf == 0) ws.value(tableIndex, 7, "")
                        else ws.value(tableIndex, 7, countf)
                        if (countp == 0) ws.value(tableIndex, 8, "")
                        else ws.value(tableIndex, 8, countp)
                        if (countl == 0) ws.value(tableIndex, 9, "")
                        else ws.value(tableIndex, 9, countl)
                        if (counte == 0) ws.value(tableIndex, 10, "")
                        else ws.value(tableIndex, 10, counte)

                        if (countmfe == 0) ws.value(tableIndex, 11, "")
                        else ws.value(tableIndex, 11, countmfe)
                        if (countme == 0) ws.value(tableIndex, 12, "")
                        else ws.value(tableIndex, 12, countme)
                        if (countfe == 0) ws.value(tableIndex, 13, "")
                        else ws.value(tableIndex, 13, countfe)
                        if (countpe == 0) ws.value(tableIndex, 14, "")
                        else ws.value(tableIndex, 14, countpe)
                        if (countle == 0) ws.value(tableIndex, 15, "")
                        else ws.value(tableIndex, 15, countle)
                        if (countee == 0) ws.value(tableIndex, 16, "")
                        else ws.value(tableIndex, 16, countee)

                        ws.value(tableIndex, 17, spNotes)

                        // Conditional species notes, if "0" -> red
                        if (spNotes == "0") ws.style(tableIndex, 17).fontColor("FF0000").set()

                        // Internal counts in green
                        ws.range(tableIndex, 5, tableIndex, 10)
                            .style().fontColor("008000").bold().set()

                        // External counts in red
                        ws.range(tableIndex, 11, tableIndex, 16)
                            .style().fontColor("FF0000").bold().set()

                        // Set alignment of columns code and counts to center
                        ws.range(tableIndex, 4, tableIndex, 16).style()
                            .horizontalAlignment("center").set()

                        // Left borders
                        ws.style(tableIndex, 2).borderStyle(BorderSide.LEFT, "thin").set()
                        ws.style(tableIndex, 5).borderStyle(BorderSide.LEFT, "thin").set()
                        ws.style(tableIndex, 11).borderStyle(BorderSide.LEFT, "thin").set()
                        ws.style(tableIndex, 17).borderStyle(BorderSide.LEFT, "thin").set()
                    }

                    else -> { // section_codes
                        // Build line in species table for order of species
                        ws.value(tableIndex, 0, sectName2)
                        ws.value(tableIndex, 1, sectTime)
                        ws.value(tableIndex, 2, code)
                        ws.value(tableIndex, 3, nameScifi)
                        ws.value(tableIndex, 4, nameLocal)

                        if (countmf == 0) ws.value(tableIndex, 5, "")
                        else ws.value(tableIndex, 5, countmf)
                        if (countm == 0) ws.value(tableIndex, 6, "")
                        else ws.value(tableIndex, 6, countm)
                        if (countf == 0) ws.value(tableIndex, 7, "")
                        else ws.value(tableIndex, 7, countf)
                        if (countp == 0) ws.value(tableIndex, 8, "")
                        else ws.value(tableIndex, 8, countp)
                        if (countl == 0) ws.value(tableIndex, 9, "")
                        else ws.value(tableIndex, 9, countl)
                        if (counte == 0) ws.value(tableIndex, 10, "")
                        else ws.value(tableIndex, 10, counte)

                        if (countmfe == 0) ws.value(tableIndex, 11, "")
                        else ws.value(tableIndex, 11, countmfe)
                        if (countme == 0) ws.value(tableIndex, 12, "")
                        else ws.value(tableIndex, 12, countme)
                        if (countfe == 0) ws.value(tableIndex, 13, "")
                        else ws.value(tableIndex, 13, countfe)
                        if (countpe == 0) ws.value(tableIndex, 14, "")
                        else ws.value(tableIndex, 14, countpe)
                        if (countle == 0) ws.value(tableIndex, 15, "")
                        else ws.value(tableIndex, 15, countle)
                        if (countee == 0) ws.value(tableIndex, 16, "")
                        else ws.value(tableIndex, 16, countee)

                        ws.value(tableIndex, 17, spNotes)

                        ws.style(tableIndex, 2).horizontalAlignment("center").set()

                        // Conditional species notes, if "0" -> red
                        if (spNotes == "0") ws.style(tableIndex, 17).fontColor("FF0000").set()

                        // Internal counts in green
                        ws.range(tableIndex, 5, tableIndex, 10)
                            .style().fontColor("008000").bold().set()

                        // External counts in red
                        ws.range(tableIndex, 11, tableIndex, 16)
                            .style().fontColor("FF0000").bold().set()

                        // Set alignment of columns counts to center
                        ws.range(tableIndex, 5, tableIndex, 16).style()
                            .horizontalAlignment("center").set()

                        // Left borders
                        ws.style(tableIndex, 2).borderStyle(BorderSide.LEFT, "thin").set()
                        ws.style(tableIndex, 5).borderStyle(BorderSide.LEFT, "thin").set()
                        ws.style(tableIndex, 11).borderStyle(BorderSide.LEFT, "thin").set()
                        ws.style(tableIndex, 17).borderStyle(BorderSide.LEFT, "thin").set()
                    }
                }

                if (!even) {
                    // Fill line light gray
                    ws.range(tableIndex, 0, tableIndex, 16).style()
                        .fillColor("DDDDDD").set()
                }

                summf += curXLSX.getInt(4)
                summ += curXLSX.getInt(5)
                sumf += curXLSX.getInt(6)
                sump += curXLSX.getInt(7)
                suml += curXLSX.getInt(8)
                sumo += curXLSX.getInt(9)
                summfe += curXLSX.getInt(10)
                summe += curXLSX.getInt(11)
                sumfe += curXLSX.getInt(12)
                sumpe += curXLSX.getInt(13)
                sumle += curXLSX.getInt(14)
                sumoe += curXLSX.getInt(15)
                curXLSX.moveToNext()

                tableIndex++
            }
            curXLSX.close()

            tableIndex++ // Empty row

            totali = summf + summ + sumf + sump + suml + sumo
            totale = summfe + summe + sumfe + sumpe + sumle + sumoe
            total = totali + totale

            // Headline 1 for totals of Internal, External counts
            when (outPref) {
                "names" -> {
                    ws.value(tableIndex, 4, getString(R.string.internal))
                    ws.value(tableIndex, 10, getString(R.string.external))
                    ws.style(tableIndex, 4).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(tableIndex, 10).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(tableIndex, 16).borderStyle(BorderSide.LEFT, "thin").bold().set()
                }

                "codes" -> {
                    ws.value(tableIndex, 4, getString(R.string.internal))
                    ws.value(tableIndex, 10, getString(R.string.external))
                    ws.style(tableIndex, 4).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(tableIndex, 10).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(tableIndex, 16).borderStyle(BorderSide.LEFT, "thin").bold().set()
                }

                "sections_names" -> {
                    ws.value(tableIndex, 5, getString(R.string.internal))
                    ws.value(tableIndex, 11, getString(R.string.external))
                    ws.style(tableIndex, 5).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(tableIndex, 11).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(tableIndex, 17).borderStyle(BorderSide.LEFT, "thin").bold().set()
                }

                else -> {
                    ws.value(tableIndex, 5, getString(R.string.internal))
                    ws.value(tableIndex, 11, getString(R.string.external))
                    ws.style(tableIndex, 5).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(tableIndex, 11).borderStyle(BorderSide.LEFT, "thin").bold().set()
                    ws.style(tableIndex, 17).borderStyle(BorderSide.LEFT, "thin").bold().set()
                }
            }

            // Headline 2 for totals of Internal, External counts
            when (outPref) {
                "names", "codes" -> {
                    ws.value(tableIndex + 1, 1, getString(R.string.sumSpec))
                    ws.value(tableIndex + 1, 4, getString(R.string.countImagomfHint))
                    ws.value(tableIndex + 1, 5, getString(R.string.countImagomHint))
                    ws.value(tableIndex + 1, 6, getString(R.string.countImagofHint))
                    ws.value(tableIndex + 1, 7, getString(R.string.countPupaHint))
                    ws.value(tableIndex + 1, 8, getString(R.string.countLarvaHint))
                    ws.value(tableIndex + 1, 9, getString(R.string.countOvoHint))
                    ws.value(tableIndex + 1, 10, getString(R.string.countImagomfHint))
                    ws.value(tableIndex + 1, 11, getString(R.string.countImagomHint))
                    ws.value(tableIndex + 1, 12, getString(R.string.countImagofHint))
                    ws.value(tableIndex + 1, 13, getString(R.string.countPupaHint))
                    ws.value(tableIndex + 1, 14, getString(R.string.countLarvaHint))
                    ws.value(tableIndex + 1, 15, getString(R.string.countOvoHint))
                    ws.value(tableIndex + 1, 16, getString(R.string.hintTotal))

                    ws.range(tableIndex + 1, 1, tableIndex + 1, 16)
                        .style().horizontalAlignment("center").bold()
                        .borderStyle(BorderSide.BOTTOM, "thin").set()
                    ws.style(tableIndex + 1, 4).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").set()
                    ws.style(tableIndex + 1, 10).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").set()
                    ws.style(tableIndex + 1, 16).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").set()
                }

                else -> {
                    ws.value(tableIndex + 1, 2, getString(R.string.sumSpec))
                    ws.value(tableIndex + 1, 5, getString(R.string.countImagomfHint))
                    ws.value(tableIndex + 1, 6, getString(R.string.countImagomHint))
                    ws.value(tableIndex + 1, 7, getString(R.string.countImagofHint))
                    ws.value(tableIndex + 1, 8, getString(R.string.countPupaHint))
                    ws.value(tableIndex + 1, 9, getString(R.string.countLarvaHint))
                    ws.value(tableIndex + 1, 10, getString(R.string.countOvoHint))
                    ws.value(tableIndex + 1, 11, getString(R.string.countImagomfHint))
                    ws.value(tableIndex + 1, 12, getString(R.string.countImagomHint))
                    ws.value(tableIndex + 1, 13, getString(R.string.countImagofHint))
                    ws.value(tableIndex + 1, 14, getString(R.string.countPupaHint))
                    ws.value(tableIndex + 1, 15, getString(R.string.countLarvaHint))
                    ws.value(tableIndex + 1, 16, getString(R.string.countOvoHint))
                    ws.value(tableIndex + 1, 17, getString(R.string.hintTotal))

                    ws.range(tableIndex + 1, 2, tableIndex + 1, 17)
                        .style().horizontalAlignment("center").bold()
                        .borderStyle(BorderSide.BOTTOM, "thin").set()
                    ws.range(tableIndex + 1, 2, tableIndex + 1, 17).style()
                        .borderStyle(BorderSide.BOTTOM, "thin").set()
                    ws.style(tableIndex + 1, 5).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").set()
                    ws.style(tableIndex + 1, 11).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").set()
                    ws.style(tableIndex + 1, 17).borderStyle(BorderSide.LEFT, "thin")
                        .borderStyle(BorderSide.BOTTOM, "thin").set()
                }
            }

            // Write internal total sums
            when (outPref) {
                "names", "codes" -> {
                    ws.value(tableIndex + 2, 1, sumSpec)
                    ws.value(tableIndex + 2, 3, getString(R.string.sumi))
                    ws.value(tableIndex + 2, 4, summf)
                    ws.value(tableIndex + 2, 5, summ)
                    ws.value(tableIndex + 2, 6, sumf)
                    ws.value(tableIndex + 2, 7, sump)
                    ws.value(tableIndex + 2, 8, suml)
                    ws.value(tableIndex + 2, 9, sumo)
                    ws.value(tableIndex + 2, 16, totali)

                    if (summf > 0)
                        ws.style(tableIndex + 2, 4).fontColor("008000").bold().set() // green
                    else
                        ws.style(tableIndex + 2, 4).fontColor("00FFFFFF").set() // transparent

                    if (summ > 0) ws.style(tableIndex + 2, 5).fontColor("008000").bold().set()
                    else ws.style(tableIndex + 2, 5).fontColor("00FFFFFF").set()
                    if (sumf > 0) ws.style(tableIndex + 2, 6).fontColor("008000").bold().set()
                    else ws.style(tableIndex + 2, 6).fontColor("00FFFFFF").set()
                    if (sump > 0) ws.style(tableIndex + 2, 7).fontColor("008000").bold().set()
                    else ws.style(tableIndex + 2, 7).fontColor("00FFFFFF").set()
                    if (suml > 0) ws.style(tableIndex + 2, 8).fontColor("008000").bold().set()
                    else ws.style(tableIndex + 2, 8).fontColor("00FFFFFF").set()
                    if (sumo > 0) ws.style(tableIndex + 2, 9).fontColor("008000").bold().set()
                    else ws.style(tableIndex + 2, 9).fontColor("00FFFFFF").set()
                    ws.style(tableIndex + 2, 1).horizontalAlignment("center").bold().set()
                    ws.style(tableIndex + 2, 3).horizontalAlignment("right").bold().set()
                    ws.range(tableIndex + 2, 4, tableIndex + 2, 16).style()
                        .horizontalAlignment("center").set()
                    ws.style(tableIndex + 2, 4).borderStyle(BorderSide.LEFT, "thin").set()
                    ws.style(tableIndex + 2, 10).borderStyle(BorderSide.LEFT, "thin").set()
                    ws.style(tableIndex + 2, 16).bold().borderStyle(BorderSide.LEFT, "thin").set()
                }

                else -> {
                    ws.value(tableIndex + 2, 2, sumSpec)
                    ws.value(tableIndex + 2, 4, getString(R.string.sumi))
                    ws.value(tableIndex + 2, 5, summf)
                    ws.value(tableIndex + 2, 6, summ)
                    ws.value(tableIndex + 2, 7, sumf)
                    ws.value(tableIndex + 2, 8, sump)
                    ws.value(tableIndex + 2, 9, suml)
                    ws.value(tableIndex + 2, 10, sumo)
                    ws.value(tableIndex + 2, 17, totali)

                    if (summf > 0)
                        ws.style(tableIndex + 2, 5).fontColor("008000").bold().set() // green
                    else
                        ws.style(tableIndex + 2, 5).fontColor("00FFFFFF").set() // transparent

                    if (summ > 0) ws.style(tableIndex + 2, 6).fontColor("008000").bold().set()
                    else ws.style(tableIndex + 2, 6).fontColor("00FFFFFF").set()
                    if (sumf > 0) ws.style(tableIndex + 2, 7).fontColor("008000").bold().set()
                    else ws.style(tableIndex + 2, 7).fontColor("00FFFFFF").set()
                    if (sump > 0) ws.style(tableIndex + 2, 8).fontColor("008000").bold().set()
                    else ws.style(tableIndex + 2, 8).fontColor("00FFFFFF").set()
                    if (suml > 0) ws.style(tableIndex + 2, 9).fontColor("008000").bold().set()
                    else ws.style(tableIndex + 2, 9).fontColor("00FFFFFF").set()
                    if (sumo > 0) ws.style(tableIndex + 2, 10).fontColor("008000").bold().set()
                    else ws.style(tableIndex + 2, 10).fontColor("00FFFFFF").set()
                    ws.style(tableIndex + 2, 2).horizontalAlignment("center").bold().set()
                    ws.style(tableIndex + 2, 4).horizontalAlignment("right").bold().set()
                    ws.range(tableIndex + 2, 5, tableIndex + 2, 17).style()
                        .horizontalAlignment("center").set()
                    ws.style(tableIndex + 2, 5).borderStyle(BorderSide.LEFT, "thin").set()
                    ws.style(tableIndex + 2, 11).borderStyle(BorderSide.LEFT, "thin").set()
                    ws.style(tableIndex + 2, 17).bold().borderStyle(BorderSide.LEFT, "thin").set()
                }
            }

            // Write external total sums
            when (outPref) {
                "names", "codes" -> {
                    ws.value(tableIndex + 3, 3, getString(R.string.sume))
                    ws.value(tableIndex + 3, 10, summfe)
                    ws.value(tableIndex + 3, 11, summe)
                    ws.value(tableIndex + 3, 12, sumfe)
                    ws.value(tableIndex + 3, 13, sumpe)
                    ws.value(tableIndex + 3, 14, sumle)
                    ws.value(tableIndex + 3, 15, sumoe)
                    ws.value(tableIndex + 3, 16, totale)

                    ws.style(tableIndex + 3, 3).horizontalAlignment("right").bold().set()
                    if (summfe > 0) ws.style(tableIndex + 3, 10).fontColor("FF0000").bold()
                        .set() // rot
                    else ws.style(tableIndex + 3, 10).fontColor("00FFFFFF").set() // transparent

                    if (summe > 0) ws.style(tableIndex + 3, 11).fontColor("FF0000").bold().set()
                    else ws.style(tableIndex + 3, 11).fontColor("00FFFFFF").set()
                    if (sumfe > 0) ws.style(tableIndex + 3, 12).fontColor("FF0000").bold().set()
                    else ws.style(tableIndex + 3, 12).fontColor("00FFFFFF").set()
                    if (sumpe > 0) ws.style(tableIndex + 3, 13).fontColor("FF0000").bold().set()
                    else ws.style(tableIndex + 3, 13).fontColor("00FFFFFF").set()
                    if (sumle > 0) ws.style(tableIndex + 3, 14).fontColor("FF0000").bold().set()
                    else ws.style(tableIndex + 3, 14).fontColor("00FFFFFF").set()
                    if (sumoe > 0) ws.style(tableIndex + 3, 15).fontColor("FF0000").bold().set()
                    else ws.style(tableIndex + 3, 15).fontColor("00FFFFFF").set()
                    ws.range(tableIndex + 3, 4, tableIndex + 2, 16).style()
                        .horizontalAlignment("center").set()
                    ws.style(tableIndex + 3, 4).borderStyle(BorderSide.LEFT, "thin").set()
                    ws.style(tableIndex + 3, 10).borderStyle(BorderSide.LEFT, "thin").set()
                    ws.style(tableIndex + 3, 16).bold().borderStyle(BorderSide.LEFT, "thin").set()
                }

                else -> {
                    ws.value(tableIndex + 3, 4, getString(R.string.sume))
                    ws.value(tableIndex + 3, 11, summfe)
                    ws.value(tableIndex + 3, 12, summe)
                    ws.value(tableIndex + 3, 13, sumfe)
                    ws.value(tableIndex + 3, 14, sumpe)
                    ws.value(tableIndex + 3, 15, sumle)
                    ws.value(tableIndex + 3, 16, sumoe)
                    ws.value(tableIndex + 3, 17, totale)

                    ws.style(tableIndex + 3, 4).horizontalAlignment("right").bold().set()
                    if (summfe > 0) ws.style(tableIndex + 3, 11).fontColor("FF0000").bold()
                        .set() // rot
                    else ws.style(tableIndex + 3, 11).fontColor("00FFFFFF").set() // transparent

                    if (summe > 0) ws.style(tableIndex + 3, 12).fontColor("FF0000").bold().set()
                    else ws.style(tableIndex + 3, 12).fontColor("00FFFFFF").set()
                    if (sumfe > 0) ws.style(tableIndex + 3, 13).fontColor("FF0000").bold().set()
                    else ws.style(tableIndex + 3, 13).fontColor("00FFFFFF").set()
                    if (sumpe > 0) ws.style(tableIndex + 3, 14).fontColor("FF0000").bold().set()
                    else ws.style(tableIndex + 3, 14).fontColor("00FFFFFF").set()
                    if (sumle > 0) ws.style(tableIndex + 3, 15).fontColor("FF0000").bold().set()
                    else ws.style(tableIndex + 3, 15).fontColor("00FFFFFF").set()
                    if (sumoe > 0) ws.style(tableIndex + 3, 16).fontColor("FF0000").bold().set()
                    else ws.style(tableIndex + 3, 16).fontColor("00FFFFFF").set()
                    ws.range(tableIndex + 3, 5, tableIndex + 2, 17).style()
                        .horizontalAlignment("center").set()
                    ws.style(tableIndex + 3, 5).borderStyle(BorderSide.LEFT, "thin").set()
                    ws.style(tableIndex + 3, 11).borderStyle(BorderSide.LEFT, "thin").set()
                    ws.style(tableIndex + 3, 17).bold().borderStyle(BorderSide.LEFT, "thin").set()
                }
            }

            // Write overall total sum
            when (outPref) {
                "names", "codes" -> {
                    ws.value(tableIndex + 4, 3, getString(R.string.sum_total))
                    ws.value(tableIndex + 4, 16, total)
                    ws.style(tableIndex + 4, 3).horizontalAlignment("right").bold().set()
                    ws.style(tableIndex + 4, 16).horizontalAlignment("center").set()
                    ws.style(tableIndex + 4, 4).borderStyle(BorderSide.LEFT, "thin").set()
                    ws.style(tableIndex + 4, 10).borderStyle(BorderSide.LEFT, "thin").set()
                    ws.style(tableIndex + 4, 16).bold().borderStyle(BorderSide.LEFT, "thin").set()
                }

                else -> {
                    ws.value(tableIndex + 4, 4, getString(R.string.sum_total))
                    ws.value(tableIndex + 4, 17, total)
                    ws.style(tableIndex + 4, 4).horizontalAlignment("right").bold().set()
                    ws.style(tableIndex + 4, 17).horizontalAlignment("center").set()
                    ws.style(tableIndex + 4, 5).borderStyle(BorderSide.LEFT, "thin").set()
                    ws.style(tableIndex + 4, 11).borderStyle(BorderSide.LEFT, "thin").set()
                    ws.style(tableIndex + 4, 17).bold().borderStyle(BorderSide.LEFT, "thin").set()
                }
            }

            try {
                // Export purged db as xlsx
                wb.finish()
                outputStream.close()
                wb.close()
            } catch (_: Exception) {
                mesg = getString(R.string.saveFail)
                Toast.makeText(
                    this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG
                ).show()
            }
            dbHelper!!.close()
        }
    }
    // End of exportDb2XLSX()

    // Export current species list species_ll_transNo_yyyyMMdd_HHmmss.csv to both data directories
    //  /Documents/TourCount and /Documents/TransektCount
    private fun exportSpeciesList() {
        val outFileTour: File?
        val outFileTransect: File?

        val pathTour =
            File(Environment.getExternalStorageDirectory().toString() + "/Documents/TourCount")
        val pathTransect =
            File(Environment.getExternalStorageDirectory().toString() + "/Documents/TransektCount")

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED == storageState

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard)
            Toast.makeText(
                this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Export species list into species_ll_yyyy-MM-dd_HHmmss.csv
            dataLanguage = prefs.getString("pref_sel_data_lang", "")!!
            if (dataLanguage.isEmpty()) dataLanguage = "--"

            val codeArray = countDataSource!!.getAllStringsForSectionSrtCode(1, "code")
            val nameArray = countDataSource!!.getAllStringsForSectionSrtCode(1, "name")
            val nameArrayL = countDataSource!!.getAllStringsForSectionSrtCode(1, "name_g")

            val specNum = codeArray.size

            pathTransect.mkdirs() // Just verify pathTransect, result ignored
            pathTour.mkdirs() // Just verify pathTour, result ignored

            when (dataLanguage) {
                "de" -> {
                    if (transNoForDir == "") {
                        outFileTour = File(
                            pathTour, ("/species_de_"
                                    + getcurDate() + ".csv")
                        )
                        outFileTransect = File(
                            pathTransect, ("/species_de_"
                                    + getcurDate() + ".csv")
                        )
                    } else {
                        outFileTour = File(
                            pathTour, ("/species_de_"
                                    + transNoForDir + "_" + getcurDate() + ".csv")
                        )
                        outFileTransect = File(
                            pathTransect, ("/species_de_"
                                    + transNoForDir + "_" + getcurDate() + ".csv")
                        )
                    }
                }

                "en" -> {
                    if (transNoForDir == "") {
                        outFileTour = File(
                            pathTour, ("/species_en_"
                                    + getcurDate() + ".csv")
                        )
                        outFileTransect = File(
                            pathTransect, ("/species_en_"
                                    + getcurDate() + ".csv")
                        )
                    } else {
                        outFileTour = File(
                            pathTour, ("/species_en_"
                                    + transNoForDir + "_" + getcurDate() + ".csv")
                        )
                        outFileTransect = File(
                            pathTransect, ("/species_en_"
                                    + transNoForDir + "_" + getcurDate() + ".csv")
                        )
                    }
                }

                "fr" -> {
                    if (transNoForDir == "") {
                        outFileTour = File(
                            pathTour, ("/species_fr_"
                                    + getcurDate() + ".csv")
                        )
                        outFileTransect = File(
                            pathTransect, ("/species_fr_"
                                    + getcurDate() + ".csv")
                        )
                    } else {
                        outFileTour = File(
                            pathTour, ("/species_fr_"
                                    + transNoForDir + "_" + getcurDate() + ".csv")
                        )
                        outFileTransect = File(
                            pathTransect, ("/species_fr_"
                                    + transNoForDir + "_" + getcurDate() + ".csv")
                        )
                    }
                }

                "it" -> {
                    if (transNoForDir == "") {
                        outFileTour = File(
                            pathTour, ("/species_it_"
                                    + getcurDate() + ".csv")
                        )
                        outFileTransect = File(
                            pathTransect, ("/species_it_"
                                    + getcurDate() + ".csv")
                        )
                    } else {
                        outFileTour = File(
                            pathTour, ("/species_it_"
                                    + transNoForDir + "_" + getcurDate() + ".csv")
                        )
                        outFileTransect = File(
                            pathTransect, ("/species_it_"
                                    + transNoForDir + "_" + getcurDate() + ".csv")
                        )
                    }
                }

                "es" -> {
                    if (transNoForDir == "") {
                        outFileTour = File(
                            pathTour, ("/species_es_"
                                    + getcurDate() + ".csv")
                        )
                        outFileTransect = File(
                            pathTransect, ("/species_es_"
                                    + getcurDate() + ".csv")
                        )
                    } else {
                        outFileTour = File(
                            pathTour, ("/species_es_"
                                    + transNoForDir + "_" + getcurDate() + ".csv")
                        )
                        outFileTransect = File(
                            pathTransect, ("/species_es_"
                                    + transNoForDir + "_" + getcurDate() + ".csv")
                        )
                    }
                }

                else -> {
                    // No data language given
                    if (transNoForDir == "") {
                        outFileTour = File(
                            pathTour, ("/species_--_"
                                    + getcurDate() + ".csv")
                        )
                        outFileTransect = File(
                            pathTransect, ("/species_--_"
                                    + getcurDate() + ".csv")
                        )
                    } else {
                        outFileTour = File(
                            pathTour, ("/species_--_"
                                    + transNoForDir + "_" + getcurDate() + ".csv")
                        )
                        outFileTransect = File(
                            pathTransect, ("/species_--_"
                                    + transNoForDir + "_" + getcurDate() + ".csv")
                        )
                    }
                }
            }

            // If TourCount is installed export to /Documents/TourCount
            if (pathTour.exists() && pathTour.isDirectory) {
                try {
                    val csvWrite = CSVWriter(FileWriter(outFileTour))

                    // 1. line with nocode, language, de|en|fr|it|es
                    val specLine1 = arrayOf<String?>("nocode,language,$dataLanguage")
                    csvWrite.writeNext(specLine1)

                    var i = 0
                    while (i < specNum) {
                        val specLine =
                            arrayOf(
                                codeArray[i],
                                nameArray[i],
                                nameArrayL[i]
                            )
                        i++
                        csvWrite.writeNext(specLine)
                    }
                    csvWrite.close()
                } catch (_: Exception) {
                    mesg = getString(R.string.saveFailList)
                    Toast.makeText(
                        this,
                        fromHtml("<font color='red'><b>$mesg</b></font>"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            // Export to /Documents/TransektCount
            if (pathTransect.exists() && pathTransect.isDirectory) {
                try {
                    val csvWrite = CSVWriter(FileWriter(outFileTransect))

                    // 1. line with nocode, language, de|en|fr|it|es
                    val specLine1 = arrayOf<String?>("nocode,language,$dataLanguage")
                    csvWrite.writeNext(specLine1)

                    var i = 0
                    while (i < specNum) {
                        val specLine =
                            arrayOf(
                                codeArray[i],
                                nameArray[i],
                                nameArrayL[i]
                            )
                        i++
                        csvWrite.writeNext(specLine)
                    }
                    csvWrite.close()

                    mesg = getString(R.string.saveList)
                    Toast.makeText(
                        this,
                        fromHtml("<font color='blue'>$mesg</font>"),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (_: Exception) {
                    mesg = getString(R.string.saveFailList)
                    Toast.makeText(
                        this,
                        fromHtml("<font color='red'><b>$mesg</b></font>"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    // End of exportSpeciesList()

    // Clear all relevant DB values, reset to basic DB
    private fun resetToBasisDb() {
        // Confirm dialogue before anything else takes place
        val builder = AlertDialog.Builder(this)
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setMessage(R.string.confirmResetDB)
        builder.setCancelable(false)
        builder.setPositiveButton(
            R.string.deleteButton
        ) { _: DialogInterface?, _: Int ->
            val resOK = clearDBValues()
            if (resOK) {
                mesg = getString(R.string.reset2basic)
                Toast.makeText(
                    this,  // bright green
                    fromHtml("<font color='#008000'>$mesg</font>"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        builder.setNegativeButton(
            R.string.cancelButton
        ) { dialog: DialogInterface?, _: Int -> dialog!!.cancel() }
        alert = builder.create()
        alert.show()
    }

    // Clear DB values for basic DB
    @SuppressLint("LongLogTag")
    private fun clearDBValues(): Boolean {
        // Clear values in DB
        dbHelper = DbHelper(this)
        database = dbHelper!!.writableDatabase
        var resOK = true // Gets false when reset fails

        try {
            var sql = ("UPDATE " + DbHelper.COUNT_TABLE + " SET "
                    + DbHelper.C_COUNT_F1I + " = 0, "
                    + DbHelper.C_COUNT_F2I + " = 0, "
                    + DbHelper.C_COUNT_F3I + " = 0, "
                    + DbHelper.C_COUNT_PI + " = 0, "
                    + DbHelper.C_COUNT_LI + " = 0, "
                    + DbHelper.C_COUNT_EI + " = 0, "
                    + DbHelper.C_COUNT_F1E + " = 0, "
                    + DbHelper.C_COUNT_F2E + " = 0, "
                    + DbHelper.C_COUNT_F3E + " = 0, "
                    + DbHelper.C_COUNT_PE + " = 0, "
                    + DbHelper.C_COUNT_LE + " = 0, "
                    + DbHelper.C_COUNT_EE + " = 0, "
                    + DbHelper.C_NOTES + " = '';")
            database!!.execSQL(sql)

            sql = ("UPDATE " + DbHelper.SECTION_TABLE + " SET "
                    + DbHelper.S_CREATED_AT + " = '', "
                    + DbHelper.S_NOTES + " = '';")
            database!!.execSQL(sql)

            sql = ("UPDATE " + DbHelper.META_TABLE + " SET "
                    + DbHelper.M_TEMPS + " = 0, "
                    + DbHelper.M_TEMPE + " = 0, "
                    + DbHelper.M_WINDS + " = 0, "
                    + DbHelper.M_WINDE + " = 0, "
                    + DbHelper.M_CLOUDS + " = 0, "
                    + DbHelper.M_CLOUDE + " = 0, "
                    + DbHelper.M_DATE + " = '', "
                    + DbHelper.M_START_TM + " = '', "
                    + DbHelper.M_END_TM + " = '', "
                    + DbHelper.M_NOTE + " = '';")
            database!!.execSQL(sql)
        } catch (_: Exception) {
            mesg = getString(R.string.resetFail)
            Toast.makeText(
                this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
            resOK = false
        }
        dbHelper!!.close()

        locServiceOn = false
        TransektCountApplication.lat = 0.0
        TransektCountApplication.lon = 0.0
        TransektCountApplication.isFirstLoc = true
        TransektCountApplication.isSelectSectionActivityResumed = false
        TransektCountApplication.sectionIdGPS = 0
        TransektCountApplication.sectionNameCurrent = ""

        return resOK
    }
    // End of resetToBasisDb()

    // Red warning message
    private fun showSnackbarRed(str: String) {
        baseLayout = findViewById(R.id.baseLayout) // in WelcomeActivity
        val sB = Snackbar.make(baseLayout!!, str, Snackbar.LENGTH_LONG)
        val tv = sB.getView().findViewById<TextView>(R.id.snackbar_text)
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setTextColor(Color.RED)
        tv.gravity = Gravity.CENTER
        sB.show()
    }

    // Get date for filename of exported data
    private fun getcurDate(): String {
        val date = Date()
        @SuppressLint("SimpleDateFormat") val dform: DateFormat =
            SimpleDateFormat("yyyyMMdd-HHmm")
        return dform.format(date)
    }

    // Copy file block-wise
    @Throws(IOException::class)
    private fun copy(src: File?, dst: File?) {
        val fIS = FileInputStream(src)
        val fOS = FileOutputStream(dst)

        // Transfer bytes from in to out
        val buf = ByteArray(1024)
        var len: Int
        while ((fIS.read(buf).also { len = it }) > 0) {
            fOS.write(buf, 0, len)
        }

        fIS.close()
        fOS.close()
    }

    companion object {
        private const val TAG = "WelcomeAct"

    }

}
