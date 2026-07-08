package com.wmstein.transektcount;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static com.wmstein.transektcount.TransektCountApplication.distMin;
import static com.wmstein.transektcount.TransektCountApplication.isFirstLoc;
import static com.wmstein.transektcount.TransektCountApplication.isSelectSectionActivityResumed;
import static com.wmstein.transektcount.TransektCountApplication.lat;
import static com.wmstein.transektcount.TransektCountApplication.lon;
import static com.wmstein.transektcount.TransektCountApplication.sectionIdGPS;
import static com.wmstein.transektcount.TransektCountApplication.sectionNameCurrent;
import static com.wmstein.transektcount.Utils.fromHtml;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wmstein.changelog.ChangeLog;
import com.wmstein.filechooser.AdvFileChooser;
import com.wmstein.transektcount.database.CountDataSource;
import com.wmstein.transektcount.database.DbHelper;
import com.wmstein.transektcount.database.Head;
import com.wmstein.transektcount.database.HeadDataSource;
import com.wmstein.transektcount.database.Meta;
import com.wmstein.transektcount.database.MetaDataSource;
import com.wmstein.transektcount.database.Section;
import com.wmstein.transektcount.database.SectionDataSource;
import com.wmstein.transektcount.database.Track;
import com.wmstein.transektcount.database.TrackDataSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.dhatim.fastexcel.BorderSide;
import org.dhatim.fastexcel.PaperSize;
import org.dhatim.fastexcel.Position;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

/***************************************************************************************************
 * WelcomeActivity provides the starting page with buttons to start
 * EditMetaActivity, SelectSectionActivity and ShowResultsActivity.
 * and a menu for the import/export/help/info methods.
 * <p>
 * It uses further LocationService and the PermissionDialogFragments.
 * <p>
 * Database handling is mainly controlled in WelcomeActivity as e.g. upgrade
 * to current DB version when importing an older DB file by importDBFile().
 * <p>
 * Based on BeeCount's WelcomeActivity.java by Milo Thurston from 2014-05-05.
 * Changes and additions for TransektCount by wmstein since 2016-02-18,
 * last edited on 2026-07-08
 */
public class WelcomeActivity
        extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "WelcomeAct";

    private TransektCountApplication transektCount;

    private ChangeLog cl;
    private boolean doubleBackToExitPressedTwice = false;

    // Import/export stuff
    private File inFile = null;
    private File outFile = null;
    private String localLanguage = "";
    private boolean mExternalStorageWriteable = false;
    private final String sState = Environment.getExternalStorageState();

    // Inserting tourName into filename with plausi check
    private final String regexFilename = "[^a-zA-Z_0-9äöüÄÖÜ-]";
    private String transNo = ""; // The transect name as shown
    private String transNoDir = ""; // The transect name as part of a filename
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    // Preferences
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private String outPref;
    private boolean autoSection = false; // true for enabled GPS track selection
    private boolean transectHasTrack = false; // transect sections have tracks
    private boolean buttonSoundPref;
    private boolean alertSoundPref;
    private String dataLanguage = "";

    // Permissions
    private boolean storagePermGranted = false; // initial storage permission state
    private boolean fineLocationPermGranted = false; // foreground location permission state

    // DB handling
    private SQLiteDatabase database;
    private DbHelper dbHelper;
    private HeadDataSource headDataSource;
    private Head head;
    private SectionDataSource sectionDataSource;
    private MetaDataSource metaDataSource;
    private Meta meta;
    private CountDataSource countDataSource;
    private TrackDataSource trackDataSource;

    // Track handling
    private int trCount = 0; // number of tracks
    private List<Track> trackPts; // list of all transect track points
    private LocationService locationService;
    private boolean locServiceOn = false;
    private int secCount; // number of sections
    private String sectionNameGPS; // track section name from track table

    // Sound handling
    SoundService soundService;
    Intent sndIntent;
    private boolean sndServiceOn = false;

    private View baseLayout;
    private TextView welcomeTitle;
    private AlertDialog alert;
    private String mesg;

    @SuppressLint({"SourceLockedOrientationActivity", "ApplySharedPref"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "184, onCreate");

        transektCount = (TransektCountApplication) getApplication();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Get preferences
        prefs = TransektCountApplication.getPrefs();
        editor = prefs.edit();

        buttonSoundPref = prefs.getBoolean("pref_button_sound", false); // Prepare SoundService
        alertSoundPref = prefs.getBoolean("pref_alert_sound", false);
        transectHasTrack = prefs.getBoolean("transect_has_track", false);
        autoSection = prefs.getBoolean("pref_auto_section", false);

        // Initialize (button) sound service
        if (buttonSoundPref) {
            soundService = new SoundService(getApplicationContext());
            sndIntent = new Intent(getApplicationContext(), SoundService.class);
            startService(sndIntent);
            sndServiceOn = true;
        }

        // Entry handling for Proximity and Vibrator sensor in preferences menu
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        // Gray out preferences menu item pref_prox when max. proximity sensitivity = null
        boolean prefProx = proximitySensor != null;

        // Gray out preferences menu item pref_button_vib when device has no vibrator
        Vibrator vibrator = getApplicationContext().getSystemService(Vibrator.class);
        boolean prefVib = vibrator.hasVibrator();

        // Set pref_prox enabler, used in SettingsFragment
        editor.putBoolean("enable_prox", prefProx);
        editor.putBoolean("enable_vib", prefVib);
        editor.apply();

        // Set DarkMode when system is in BrightMode
        int nightModeFlags = Configuration.UI_MODE_NIGHT_MASK;
        int confUi = getResources().getConfiguration().uiMode;
        if ((nightModeFlags & confUi) == Configuration.UI_MODE_NIGHT_NO) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        // Use EdgeToEdge mode for Android 15+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // Android 15+, SDK 35+
        {
            EdgeToEdge.enable(this);
        }

        setContentView(R.layout.activity_welcome);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.baseLayout),
                (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    mlp.topMargin = insets.top;
                    mlp.bottomMargin = insets.bottom;
                    mlp.leftMargin = insets.left;
                    mlp.rightMargin = insets.right;
                    v.setLayoutParams(mlp);
                    return WindowInsetsCompat.CONSUMED;
                });

        cl = new ChangeLog(this, prefs);

        // Show changelog for new version
        if (cl.firstRun())
            cl.getLogDialog().show();

        if (!transectHasTrack && autoSection) {
            autoSection = false;
            editor.putBoolean("pref_auto_section", false);
            editor.commit();
        }

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "263, onCreate, autoSection: " + autoSection
                    + ", Transect has track: " + transectHasTrack);

        // Check and ask storage permission
        storagePermGranted = isStoragePermGranted();
        if (!storagePermGranted) // in self permission
        {
            PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                    PermissionsStorageDialogFragment.class.getName());
        }

        storagePermGranted = isStoragePermGranted();
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "276, onCreate, storageGranted: " + storagePermGranted);

        // Check current DB version and upgrade if necessary
        dbHelper = new DbHelper(this);
        database = dbHelper.getWritableDatabase(); // Make DB upgrade if necessary
        int dbVer = database.getVersion(); // DB version after upgrade
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "283, onCreate, dbVersion: " + dbVer);

        dbHelper.close();

        // Set up the data sources
        headDataSource = new HeadDataSource(this);
        sectionDataSource = new SectionDataSource(this);
        metaDataSource = new MetaDataSource(this);
        countDataSource = new CountDataSource(this);
        trackDataSource = new TrackDataSource(this);

        // Write initial Basic database
        storagePermGranted = isStoragePermGranted();
        if (storagePermGranted) {
            // Test for existence of directory /storage/emulated/0/Documents/TransektCount
            File path;
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TransektCount");

            // Delete an old existing transektcount0.db if it does exist
            inFile = new File(path, "/transektcount0.db"); // old initial basic DB
            if (inFile.exists())
                //noinspection ResultOfMethodCallIgnored
                inFile.delete();

            // Create directory TransektCount and write initial transektcount0_ll.db file
            localLanguage = Locale.getDefault().toString().substring(0, 2);
            inFile = new File(path, "/transektcount0_" + localLanguage + ".db"); // new initial basic DB
            if (!inFile.exists()) {
                // parameter 0: short name, local language and don't show a message
                exportBasicDb(0);
            }
        }

        // Get transect No. and check for DB integrity
        try {
            headDataSource.open();
            head = headDataSource.getHead();
            transNo = head.transect_no; // get transNo and test for DB integrity
            headDataSource.close();
        } catch (SQLiteException e) {
            headDataSource.close();

            mesg = getString(R.string.corruptDb);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();

            mHandler.postDelayed(this::finishAndRemoveTask, 2000);
        }

        transNoDir = transNo;
        if (!Objects.equals(transNoDir, ""))
            transNoDir = transNoDir.replaceAll(regexFilename, "");

        // ***************************************************
        // Prepare for GPS usage
        // Try to get list of trackpoints and number of tracks
        try {
            trackDataSource.open();
            trackPts = trackDataSource.getAllTrackPoints();
            trCount = trackDataSource.getDiffTrks();
            trackDataSource.close();
        } catch (SQLiteException e) {
            trackDataSource.close();
            trCount = 0;
        }

        // Get number of sections and name for 1st use of new DB
        sectionDataSource.open();
        secCount = sectionDataSource.getNumEntries();
        sectionNameGPS = sectionDataSource.getSection(1).name;
        sectionDataSource.close();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG && autoSection)
            Log.i(TAG, "358, onCreate, TrkPts: " + trackPts.size()
                    + ", trCount: " + trCount + ", secCount: " + secCount);

        // Check if tracks exist and correspond to sections
        if (trCount > 0 && trCount != secCount) {
            autoSection = false;

            editor.putBoolean("pref_auto_section", false);
            editor.commit();

            mesg = getString(R.string.track_err);
            Toast.makeText(getApplicationContext(),
                    fromHtml("<font color='red'>" + mesg + "</font>"),
                    Toast.LENGTH_LONG).show();
        }

        // Check if track discovery is set and tracks correspond to sections
        if (autoSection) {
            if (trCount != secCount) {
                autoSection = false;
                transectHasTrack = false;

                editor.putBoolean("pref_auto_section", false);
                editor.putBoolean("transect_has_track", false);
                editor.commit();

                mesg = getString(R.string.track_err);
                Toast.makeText(getApplicationContext(),
                        fromHtml("<font color='red'>" + mesg + "</font>"),
                        Toast.LENGTH_LONG).show();
            } else if (trCount > 0) {
                transectHasTrack = true;

                editor.putBoolean("transect_has_track", true);
                editor.commit();
            }
        }

        /*+*+++++++++++++++++++++++++++++++++++++++++++
         * Check and ask foreground location permission
         */
        if (autoSection && isStoragePermGranted()) {
            fineLocationPermGranted = isFineLocationPermGranted();

            if (transectHasTrack && !fineLocationPermGranted) { // query foreground location permission
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "404, onCreate, ForegrndLocDialog");

                PermissionsForegroundDialogFragment.newInstance().show(getSupportFragmentManager(),
                        PermissionsForegroundDialogFragment.class.getName());

                editor.putBoolean("has_asked_background", false);
                editor.commit();
            } else if (!transectHasTrack) {
                autoSection = false;

                editor.putBoolean("pref_auto_section", false); // deny auto section function
                editor.commit();
            }
        }

        if (autoSection && isStoragePermGranted()) {
            fineLocationPermGranted = isFineLocationPermGranted();
            if (transectHasTrack && fineLocationPermGranted) {

                // Get flag 'has_asked_background'
                boolean hasAskedBackgroundLocation = prefs.getBoolean("has_asked_background", false);

                // Get background location with permissions check only once and if storage and fine location
                //   permissions are granted
                if (storagePermGranted && fineLocationPermGranted && !hasAskedBackgroundLocation) {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG, "430, onCreate, BackgrndLocDialog");

                    // Ask optional background location permission
                    PermissionsBackgroundDialogFragment.newInstance().show(getSupportFragmentManager(),
                            PermissionsBackgroundDialogFragment.class.getName());

                    // Store flag 'hasAskedBackground = true' in SharedPreferences
                    editor.putBoolean("has_asked_background", true);
                    editor.commit();
                }
            }
        }

        /* New onBackPressed logic, use only when NavBarMode = 0 or 1
         * Different Navigation Bar modes and layouts:
         * - Classic three-button navigation: NavBarMode = 0
         * - Two-button navigation (Android P): NavBarMode = 1
         * - Full screen gesture mode (Android Q): NavBarMode = 2
         */
        if (getNavBarMode() == 0 || getNavBarMode() == 1) {
            OnBackPressedCallback callback = getOnBackPressedCallback();
            getOnBackPressedDispatcher().addCallback(this, callback);
        }
    }
    // End of onCreate()

    // Check for Navigation bar (1-, 2- or 3-button mode)
    public int getNavBarMode() {
        Resources resources = this.getResources();

        @SuppressLint("DiscouragedApi")
        int resourceId = resources.getIdentifier("config_navBarInteractionMode",
                "integer", "android");

        // navBarMode = 0: 3-button, = 1: 2-button, = 2: gesture
        int navBarMode = resourceId > 0 ? resources.getInteger(resourceId) : 0;
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "467, NavBarMode = " + navBarMode);

        return navBarMode;
    }

    // Use onBackPressed logic for button navigation
    @NonNull
    private OnBackPressedCallback getOnBackPressedCallback() {
        final Handler m1Handler = new Handler(Looper.getMainLooper());
        final Runnable r1 = () -> doubleBackToExitPressedTwice = false;

        return new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (doubleBackToExitPressedTwice) {
                    m1Handler.removeCallbacks(r1);
                    if (fineLocationPermGranted && autoSection)
                        locationDispatcher(2); // stop locHandler and location service

                    // Stop sound server
                    if (sndServiceOn && locServiceOn) {
                        locationService.releaseSoundA();
                        soundService.releaseSoundM();
                        soundService.releaseSoundP();

                        stopService(sndIntent);
                        sndServiceOn = false;
                    }
                    finish();
                    remove();
                } else {
                    doubleBackToExitPressedTwice = true;

                    mesg = getString(R.string.back_twice);
                    Toast.makeText(getApplicationContext(),
                            fromHtml("<font color='blue'>" + mesg + "</font>"),
                            Toast.LENGTH_SHORT).show();
                    m1Handler.postDelayed(r1, 1500);
                }
            }
        };
    }

    @SuppressLint({"SourceLockedOrientationActivity", "ApplySharedPref"})
    @Override
    protected void onResume() {
        super.onResume();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "516, onResume");

        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        editor = prefs.edit();

        outPref = prefs.getString("pref_csv_out", "sections"); // sort mode csv-export
        autoSection = prefs.getBoolean("pref_auto_section", false);
        transectHasTrack = prefs.getBoolean("transect_has_track", false);
        dataLanguage = prefs.getString("pref_sel_data_lang", "");
        if (dataLanguage.isEmpty())
            dataLanguage = "--";

        headDataSource.open();
        sectionDataSource.open();
        metaDataSource.open();
        countDataSource.open();
        trackDataSource.open();

        baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(transektCount.setBackgr());

        // Try to get list of trackpoints and number of tracks
        try {
            trackPts = trackDataSource.getAllTrackPoints();
            trCount = trackDataSource.getDiffTrks();
        } catch (SQLiteException e) {
            trCount = 0;
        }

        secCount = sectionDataSource.getNumEntries();
        sectionNameGPS = sectionDataSource.getSection(1).name;

        // Check if tracks exist and correspond to sections
        if (trCount > 0 && trCount != secCount) {
            autoSection = false;

            editor.putBoolean("pref_auto_section", false);
            editor.commit();

            mesg = getString(R.string.track_err);
            Toast.makeText(getApplicationContext(),
                    fromHtml("<font color='red'>" + mesg + "</font>"),
                    Toast.LENGTH_LONG).show();
        }

        // Set app title according to using GPS or not
        welcomeTitle = findViewById(R.id.welcomeTitle);
        if (autoSection) {
            welcomeTitle.setText(getString(R.string.app_name1)); // app title = TransektCountGPS
        } else {
            welcomeTitle.setText(getString(R.string.app_name));    // app title = TransektCount
        }

        // Set transect number as transect title
        head = headDataSource.getHead();
        transNo = head.transect_no; // read and test for DB integrity
        try {
            Objects.requireNonNull(getSupportActionBar()).setTitle(transNo);
        } catch (NullPointerException e) {
            // nothing
        }

        // Prepare modified transNo to be part of a filename
        transNoDir = transNo;
        if (!Objects.equals(transNoDir, ""))
            transNoDir = transNoDir.replaceAll(regexFilename, "");

        // If all requirements are given start location service
        if (autoSection && fineLocationPermGranted && transectHasTrack) {
            // Start location service and get 1. location
            locationDispatcher(1);
        }
    }
    // End of onResume()

    // Check external storage self permission
    private Boolean isStoragePermGranted() {
        boolean storGranted;
        // Check permission MANAGE_EXTERNAL_STORAGE for Android >= 11
        storGranted = Environment.isExternalStorageManager();
        return storGranted;
    }

    // Check initial fine location permission
    private Boolean isFineLocationPermGranted() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /* Control location service by locationDispatcherMode:
     *  1: Start location service and periodic location request
     *  2: Stop location service on backpress, when running
     *  3: Stop location service by onStop() when app is invisible
     */
    private void locationDispatcher(int locationDispatcherMode) {
        if (fineLocationPermGranted && autoSection) // if GPS should be used
        {
            switch (locationDispatcherMode) {
                case 1 -> {
                    // Start location service
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG, "618, locationDispatcher 1");

                    if (!locServiceOn) {
                        locationService = new LocationService(getApplicationContext());
                        Intent sIntent = new Intent(getApplicationContext(), LocationService.class);
                        startService(sIntent);

                        locServiceOn = true;
                    }

                    // Get location to check distance to nearest transect
                    getDistance();
                }
                case 2 -> {
                    // Stop location service on backpress, when running
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG, "634, location stop 2 by backpress");

                    if (locServiceOn) {
                        stopLocSrv();
                    }
                }
                case 3 -> {
                    // Stop location service by onStop() when app is invisible
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG, "643, location stop 3 by onStop");

                    if (locServiceOn) {
                        stopLocSrv();
                    }
                }
            }
        }
    }

    // Get the current location for a hint telling the distance to transect
    public void getDistance() {
        if (locationService.canGetLocation() && distMin != 0.0) {
            // Show message: GPS: Distance to track: distance m
            String dst = new DecimalFormat("#.#").format(distMin);

            mesg = getString(R.string.distanceToTrack) + " " + dst + " m";
            Toast.makeText(this, // bright green
                    fromHtml("<font color='#008000'>" + mesg + "</font>"),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Stop location service and periodic updating
    private void stopLocSrv() {
        locationService.stopListener();
        Intent sIntent = new Intent(getApplicationContext(), LocationService.class);
        stopService(sIntent);

        sectionIdGPS = 0;
        locServiceOn = false;
        lat = 0.0;
        lon = 0.0;
    }

    // Show the action bar menu with present items
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.welcome, menu);
        MenuCompat.setGroupDividerEnabled(menu, true); // Show dividers in menu
        return true;
    }

    // Handle clicks on the action bar and its menu items here
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class)
                    .addFlags(FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        } else if (id == R.id.exportBasisMenu) {
            if (storagePermGranted) {
                exportBasicDb(2); // 2: show message + long name
            } else {
                PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                        PermissionsStorageDialogFragment.class.getName());
                if (storagePermGranted) {
                    exportBasicDb(2); // 2: show message + long name
                } else {
                    mesg = getString(R.string.storage_not_possible);
                    Toast.makeText(this,
                            fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                            Toast.LENGTH_LONG).show();
                }
            }
            return true;
        } else if (id == R.id.exportMenu) {
            if (storagePermGranted) {
                exportDb();
            } else {
                PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                        PermissionsStorageDialogFragment.class.getName());
                if (storagePermGranted) {
                    exportDb();
                } else {
                    mesg = getString(R.string.storage_not_possible);
                    Toast.makeText(this,
                            fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                            Toast.LENGTH_LONG).show();
                }
            }
            return true;
        } else if (id == R.id.exportXLSXMenu) {
            if (storagePermGranted) {
                exportDb2XLSX();
            } else {
                PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                        PermissionsStorageDialogFragment.class.getName());
                if (storagePermGranted) {
                    exportDb2XLSX();
                } else {
                    mesg = getString(R.string.storage_not_possible);
                    Toast.makeText(this,
                            fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                            Toast.LENGTH_LONG).show();
                }
            }
            return true;
        } else if (id == R.id.exportCSVMenu) {
            if (storagePermGranted) {
                exportDb2CSV();
            } else {
                PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                        PermissionsStorageDialogFragment.class.getName());
                if (storagePermGranted) {
                    exportDb2CSV();
                } else {
                    mesg = getString(R.string.storage_not_possible);
                    Toast.makeText(this,
                            fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                            Toast.LENGTH_LONG).show();
                }
            }
            return true;
        } else if (id == R.id.exportSpeciesListMenu) {
            if (storagePermGranted) {
                exportSpeciesList();
            } else {
                PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                        PermissionsStorageDialogFragment.class.getName());
                if (storagePermGranted) {
                    exportSpeciesList();
                } else {
                    mesg = getString(R.string.storage_not_possible);
                    Toast.makeText(this,
                            fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                            Toast.LENGTH_LONG).show();
                }
            }
            return true;
        } else if (id == R.id.importBasisMenu) {
            importBasisDb();
            return true;
        } else if (id == R.id.importFileMenu) {
            inFile = null;
            importDBFile();
            return true;
        } else if (id == R.id.importGPSMenu) {
            if (!transectHasTrack)
                importGPS();
            else {
                mesg = getString(R.string.importTrackFail);
                Toast.makeText(this, // orange
                        fromHtml("<font color='#ff6000'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
            }
            return true;
        } else if (id == R.id.deleteGPXMenu) {
            if (transectHasTrack) {
                deleteGPS();
                transectHasTrack = false;
                autoSection = false;
                // Reset app title to TransektCount
                welcomeTitle.setText(getString(R.string.app_name));
            } else {
                mesg = getString(R.string.deleteTrackFail);
                Toast.makeText(this, // orange
                        fromHtml("<font color='#ff6000'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
            }
            return true;
        } else if (id == R.id.resetDBMenu) {
            resetToBasisDb();
            return true;
        } else if (id == R.id.importSpeciesListMenu) {
            importSpeciesList();
            return true;
        } else if (id == R.id.viewHelp) {
            intent = new Intent(WelcomeActivity.this, ShowTextDialog.class);
            intent.putExtra("dialog", "help");
            startActivity(intent);
            return true;
        } else if (id == R.id.changeLog) {
            cl.getFullLogDialog().show();
            return true;
        } else if (id == R.id.viewLicense) {
            intent = new Intent(WelcomeActivity.this, ShowTextDialog.class);
            intent.putExtra("dialog", "license");
            startActivity(intent);
            return true;
        } else if (id == R.id.editMeta) {
            startActivity(new Intent(this, EditMetaActivity.class)
                    .addFlags(FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        } else if (id == R.id.selectSection) {
            selSect();
            return true;
        } else if (id == R.id.showResults) {
            mesg = getString(R.string.wait);
            Toast.makeText(this,
                    fromHtml("<font color='blue'>" + mesg + "</font>"),
                    Toast.LENGTH_SHORT).show();

            // To show toast, pause for 100 msec before calling ShowResultsActivity
            mHandler.postDelayed(() ->
                    startActivity(new Intent(getApplicationContext(), ShowResultsActivity
                            .class).addFlags(FLAG_ACTIVITY_CLEAR_TOP)), 100);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    // End of onOptionsItemSelected

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(transektCount.setBackgr());
        outPref = prefs.getString("pref_csv_out", "species");
        autoSection = prefs.getBoolean("pref_auto_section", false);
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false);
        alertSoundPref = prefs.getBoolean("pref_alert_sound", false);

        // Stop location service when denied in settings with alert sound first
        if (!autoSection && locServiceOn) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "861, location stop by setting");
            locationService.stopSoundA();
            stopLocSrv();
        }

        // Stop sound service when denied in settings
        if (!buttonSoundPref && sndServiceOn) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "869, button sound stop by setting");
            stopService(sndIntent);
            sndServiceOn = false;
        }

        if (buttonSoundPref && !sndServiceOn) {
            startService(sndIntent);
            sndServiceOn = true;
        }

        // Stop alert sound when denied in settings
        if (!alertSoundPref && locServiceOn) {
            locationService.stopSoundA();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "890, onPause");

        headDataSource.close();
        sectionDataSource.close();
        metaDataSource.close();
        countDataSource.close();
        trackDataSource.close();

        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "906, onStop");

        baseLayout.invalidate();

        // Stop location service when app is finished
        if (!TCLifecycleHandler.isApplicationVisible()) {
            if (sndServiceOn) {
                soundService.releaseSoundM();
                soundService.releaseSoundP();
                stopService(sndIntent);
                sndServiceOn = false;
            }

            if (locServiceOn)
                locationService.releaseSoundA();

            locationDispatcher(3);

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "925, onStop, app not visible, LocationService running: "
                        + locServiceOn);

            finishAndRemoveTask(); // In combination with System.exit(0) in onDestroy() closes app completely
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "937 onDestroy");

        System.exit(0);
    }

    // Start SelectSectionActivity (by button)
    public void selectSection(View view) {
        selSect();
    }

    private void selSect() {
        if (autoSection && fineLocationPermGranted && transectHasTrack) {
            // Call SelectSectionActivity with the GPS identified section marked blue
            Intent intent = new Intent(WelcomeActivity.this, SelectSectionActivity.class);
            mHandler.postDelayed(() ->
                    startActivity(intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP)), 100);
        } else {
            // Just call SelectSectionActivity to select section for counting
            mHandler.postDelayed(() ->
                    startActivity(new Intent(this, SelectSectionActivity.class)
                            .addFlags(FLAG_ACTIVITY_CLEAR_TOP)), 100);
        }
    }

    // Start EditMetaActivity (by button)
    public void editMeta(View view) {
        startActivity(new Intent(this, EditMetaActivity.class)
                .addFlags(FLAG_ACTIVITY_CLEAR_TOP));
    }

    // Start ShowResultsActivity (by button)
    public void showResults(View view) {
        mesg = getString(R.string.wait);
        Toast.makeText(this,
                fromHtml("<font color='blue'>" + mesg + "</font>"),
                Toast.LENGTH_SHORT).show();

        // Trick: Pause for 100 msec to show toast
        mHandler.postDelayed(() ->
                startActivity(new Intent(getApplicationContext(), ShowResultsActivity.class)
                        .addFlags(FLAG_ACTIVITY_CLEAR_TOP)), 100);
    }

    // Get date for filename of exported data
    private static String getcurDate() {
        Date date = new Date();
        @SuppressLint("SimpleDateFormat")
        DateFormat dform = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return dform.format(date);
    }

    /***********************************************************************************************
     * The next four function groups below are for importing data files.
     * They've been put here because no database should be left open at this point.
     */
    // Import the basic DB
    private void importBasisDb() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "995, importBasicDBFile");

        String fileExtension = ".db";
        String fileNameStart = "transektcount0";
        String fileHd = getString(R.string.fileHeadlineBasicDB);

        Intent intent;
        intent = new Intent(this, AdvFileChooser.class);
        intent.putExtra("filterFileExtension", fileExtension);
        intent.putExtra("filterFileNameStart", fileNameStart);
        intent.putExtra("fileHd", fileHd);
        DBActivityResultLauncher.launch(intent);
    }
    // End of part 1 of importBasisDb()

    /**********************************************************************************************/
    // Choose a transektcount db-file to load and set it to transektcount.db
    private void importDBFile() {
        String fileExtension = ".db";
        String fileNameStart = "transektcount_";
        String fileHd = getString(R.string.fileHeadlineDB);

        Intent intent;
        intent = new Intent(this, AdvFileChooser.class);
        intent.putExtra("filterFileExtension", fileExtension);
        intent.putExtra("filterFileNameStart", fileNameStart);
        intent.putExtra("fileHd", fileHd);
        DBActivityResultLauncher.launch(intent);
    }
    // End of part 1 of importDBFile()

    // ActivityResultLauncher is part2 of importBasisDb() and importDBFile()
    //   and processes the result of AdvFileChooser
    final ActivityResultLauncher<Intent> DBActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    String selectedFile;
                    inFile = null;

                    if (result.getResultCode() == Activity.RESULT_OK) // has a file
                    {
                        Intent data = result.getData();
                        if (data != null) {
                            selectedFile = data.getStringExtra("fileSelected");
                            if (selectedFile != null)
                                inFile = new File(selectedFile);
                            else
                                inFile = null;
                        }
                    } else {
                        if ((result.getResultCode() == Activity.RESULT_FIRST_USER)) {
                            mesg = getString(R.string.noFile);
                            Toast.makeText(getApplicationContext(), // orange
                                    fromHtml("<font color='#ff6000'><b>" + mesg + "</b></font>"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    if (inFile != null) {
                        // outFile -> /data/data/com.wmstein.transektcount/databases/transektcount.db
                        String destPath = getApplicationContext().getFilesDir().getPath();
                        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases/transektcount.db";
                        outFile = new File(destPath);

                        // save current autoSection state
                        boolean transectHasTrackTemp = transectHasTrack;
                        boolean autoSectionTemp = autoSection;

                        AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.setMessage(R.string.confirmDBImport);
                        builder.setCancelable(false);
                        builder.setPositiveButton(R.string.importButton, (dialog, id) ->
                        {
                            if (transectHasTrack) {
                                transectHasTrack = false;
                                autoSection = false;
                            }

                            try {
                                headDataSource.close();
                                sectionDataSource.close();
                                metaDataSource.close();
                                countDataSource.close();
                                trackDataSource.close();

                                copy(inFile, outFile);

                                headDataSource.open();
                                sectionDataSource.open();
                                metaDataSource.open();
                                countDataSource.open();
                                trackDataSource.open();

                                head = headDataSource.getHead();
                                String headLanguage = head.data_language; // Could be null in head table
                                boolean hasDataLang = true;

                                if (Objects.equals(headLanguage, "") || headLanguage == null)
                                    hasDataLang = false;
                                else
                                    headLanguage = headLanguage.substring(0, 2);

                                editor = prefs.edit();
                                editor.putString("pref_sel_data_lang", headLanguage);
                                editor.putBoolean("has_data_lang", hasDataLang);

                                // List new transNo as title
                                transNo = head.transect_no;
                                Objects.requireNonNull(getSupportActionBar()).setTitle(transNo);

                                transNoDir = transNo;
                                if (!Objects.equals(transNoDir, ""))
                                    transNoDir = transNoDir.replaceAll(regexFilename, "");

                                // Test for number of existing tracks and matching sections
                                try {
                                    trCount = trackDataSource.getDiffTrks();
                                } catch (SQLiteException e) {
                                    trCount = 0;
                                }
                                secCount = sectionDataSource.getNumEntries();

                                // Check to continue location service
                                if (trCount > 0 && trCount == secCount) {
                                    editor.putBoolean("transect_has_track", true);
                                    transectHasTrack = true;
                                    editor.putBoolean("pref_auto_section", true);
                                    autoSection = true;
                                } else {
                                    editor.putBoolean("transect_has_track", false);
                                    editor.putBoolean("pref_auto_section", false);
                                    locationDispatcher(2);
                                }
                                editor.commit();

                                // Set app title according to using GPS or not
                                TextView welcomeTitle = findViewById(R.id.welcomeTitle);
                                if (autoSection) {
                                    // app title = TransektCountGPS
                                    welcomeTitle.setText(getString(R.string.app_name1));
                                } else {
                                    // app title = TransektCount
                                    welcomeTitle.setText(getString(R.string.app_name));
                                }

                                // Reset invalid values
                                lat = 0.0;
                                lon = 0.0;
                                sectionIdGPS = 0;
                                sectionNameCurrent = "";

                                if (trCount > 0) {
                                    mesg = getString(R.string.importDB) + ",\n"
                                            + getString(R.string.with) + " " + trCount + " "
                                            + getString(R.string.tracks);
                                } else {
                                    mesg = getString(R.string.importDB) + ".";
                                }
                                Toast.makeText( // bright green
                                        getApplicationContext(),
                                        fromHtml("<font color='#008000'>" + mesg + "</font>"),
                                        Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                headDataSource.open();
                                sectionDataSource.open();
                                metaDataSource.open();
                                countDataSource.open();
                                trackDataSource.open();

                                locServiceOn = false;
                                mesg = getString(R.string.importFail);
                                Toast.makeText(getApplicationContext(),
                                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                        builder.setNegativeButton(R.string.cancelButton, (dialog, id) ->
                        {
                            editor = prefs.edit(); // restore previous autoSection state
                            editor.putBoolean("pref_auto_section", autoSectionTemp);
                            editor.putBoolean("transect_has_track", transectHasTrackTemp);
                            editor.commit();

                            dialog.cancel();
                        });
                        alert = builder.create();
                        alert.show();
                    }
                }
            });
    // End of part2 of import of DB files

    /**********************************************************************************************/
    // Select and import a species list from TourCount file species_YYYY-MM-DD_hhmmss.csv
    private void importSpeciesList() {
        // Select exported TourCount species list file
        String fileExtension = ".csv";
        String fileNameStart = "species_";
        String fileHd = getString(R.string.fileHeadlineCSV);

        Intent intent;
        intent = new Intent(this, AdvFileChooser.class);
        intent.putExtra("filterFileExtension", fileExtension);
        intent.putExtra("filterFileNameStart", fileNameStart);
        intent.putExtra("fileHd", fileHd);
        listActivityResultLauncher.launch(intent);
    }

    // ActivityResultLauncher processes the result of AdvFileChooser
    final ActivityResultLauncher<Intent> listActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<>() {
                @SuppressLint("ApplySharedPref")
                @Override
                public void onActivityResult(ActivityResult result) {
                    String selectedFile;
                    inFile = null;

                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            selectedFile = data.getStringExtra("fileSelected");
                            if (selectedFile != null)
                                inFile = new File(selectedFile);
                            else
                                inFile = null;
                        }
                    }
                    // RESULT_FIRST_USER is set in AdvFileChooser for no file
                    else if ((result.getResultCode() == Activity.RESULT_FIRST_USER)) {
                        mesg = getString(R.string.noFile);
                        Toast.makeText(getApplicationContext(), // orange
                                fromHtml("<font color='#ff6000'><b>" + mesg + "</b></font>"),
                                Toast.LENGTH_LONG).show();
                    }

                    if (inFile != null) {
                        String csvLine;
                        boolean brError = false;

                        // Check for old version of species list
                        try {
                            BufferedReader br = new BufferedReader(new FileReader(inFile));
                            csvLine = br.readLine(); // Read 1. line only
                            String[] specLine = csvLine.split(",");
                            if (Objects.equals(specLine[0], "nocode"))
                                mesg = getString(R.string.confirmListImport);
                            else
                                mesg = getString(R.string.specsCommonLangMissing) + "\n\n" + getString(R.string.confirmListImport);
                            br.close();
                        } catch (Exception e) {
                            mesg = getString(R.string.br_Error);
                            brError = true;
                        }

                        AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.setMessage(mesg);
                        if (brError) {
                            builder.setCancelable(true);
                            builder.setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
                        } else {
                            builder.setCancelable(false);
                            builder.setPositiveButton(R.string.importButton, (dialog, id) ->
                            {
                                // Load .csv species list
                                clearDBforImport();
                                readSpeciesCSV(inFile);
                            });
                            builder.setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
                        }
                        alert = builder.create();
                        alert.show();
                    }
                }
            });

    // Clear DB for import of an external species list:
    //  clear COUNT_TABLE and update SECTION_TABLE
    private void clearDBforImport() {
        dbHelper = new DbHelper(this);
        database = dbHelper.getWritableDatabase();

        String sql = "DELETE FROM " + DbHelper.COUNT_TABLE;
        database.execSQL(sql);

        sql = "UPDATE " + DbHelper.SECTION_TABLE + " SET "
                + DbHelper.S_CREATED_AT + " = '', "
                + DbHelper.S_NOTES + " = '';";
        database.execSQL(sql);

        dbHelper.close();

        lat = 0.0;
        lon = 0.0;
        isFirstLoc = true;
        locServiceOn = false;
        isSelectSectionActivityResumed = false;
        sectionIdGPS = 0;
        sectionNameCurrent = "";

        editor = prefs.edit();
        editor.putInt("item_Position", 0);
        editor.putInt("section_id", 1);
        editor.apply();
    }

    // Read a species list and write items to table counts
    private void readSpeciesCSV(File inFile) {
        try {
            mesg = getString(R.string.waitImport);
            Toast.makeText(this,
                    fromHtml("<font color='blue'>" + mesg + "</font>"),
                    Toast.LENGTH_SHORT).show();

            String csvLine;
            List<String> codeArray = new ArrayList<>();
            List<String> nameArray = new ArrayList<>();
            List<String> nameGArray = new ArrayList<>();

            BufferedReader br = new BufferedReader(new FileReader(inFile));
            boolean hasDataLang = true;

            editor = prefs.edit();
            csvLine = br.readLine(); // Read 1. line only
            String[] specLine = csvLine.split(",");
            if (Objects.equals(specLine[0], "nocode")) {
                dataLanguage = specLine[2];
                editor.putString("pref_sel_data_lang", dataLanguage);
            } else {
                hasDataLang = false;
                editor.putString("pref_sel_data_lang", "--");
            }

            editor.putBoolean("has_data_lang", hasDataLang); // controls data language setting
            editor.apply();

            br.close();

            int iList;       // index of imported list
            int iCounts = 1; // index of id in table counts
            int iSec;        // index of section
            int maxSec = sectionDataSource.getNumEntries(); // number of sections

            // For all sections
            for (iSec = 1; iSec <= maxSec; iSec++) {
                br = new BufferedReader(new FileReader(inFile));
                iList = 0;
                // For all species list lines
                while ((csvLine = br.readLine()) != null) // for each csvLine
                {
                    // comma-separated 0:id (not used), 1:code, 2:name, 3:nameL
                    specLine = csvLine.split(",");

                    // 1. line fields contain String[0]: "nocode", [1]: "language", [2]: "de"|"en"|"fr"|"it"|"es"
                    if (Objects.equals(specLine[0], "nocode")) {
                        if (hasDataLang) {
                            iCounts--;
                            iList--;
                        }
                    } else {
                        codeArray.add(iList, specLine[0]);
                        nameArray.add(iList, specLine[1]);
                        nameGArray.add(iList, specLine[2]);
                        countDataSource.writeCountItem(String.valueOf(iCounts), String.valueOf(iSec),
                                codeArray.get(iList), nameArray.get(iList), nameGArray.get(iList));
                    }
                    iList++;
                    iCounts++;
                }
                br.close();
            }
            mesg = getString(R.string.importList);
            Toast.makeText(this, // bright green
                    fromHtml("<font color='#008000'>" + mesg + "</font>"),
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            mesg = getString(R.string.importListFail);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
        }
    }
    // End of importSpeciesList()

    /***********************************************************************************************
     * Select and import gpx-file to store track coords into the TRACK_TABLE of transektcount.db
     */
    private void importGPS() {
        if (transectHasTrack)
            deleteGPS();

        String fileExtension = ".gpx";
        String fileNameStart = "transektcount";
        String fileHd = getString(R.string.fileHeadlineGPS);

        Intent intent;
        intent = new Intent(this, AdvFileChooser.class);
        intent.putExtra("filterFileExtension", fileExtension);
        intent.putExtra("filterFileNameStart", fileNameStart);
        intent.putExtra("fileHd", fileHd);
        gpxActivityResultLauncher.launch(intent);
    }

    // Function processes the result of AdvFileChooser
    final ActivityResultLauncher<Intent> gpxActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    String selectedFile;
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            selectedFile = data.getStringExtra("fileSelected");
                            assert selectedFile != null;
                        } else {
                            selectedFile = "";
                        }

                        if (!selectedFile.isEmpty())
                            inFile = new File(selectedFile);
                        else
                            inFile = null;
                    } else {
                        selectedFile = "";
                        inFile = null;
                        if ((result.getResultCode() == Activity.RESULT_FIRST_USER)) {
                            mesg = getString(R.string.noFile);
                            Toast.makeText(getApplicationContext(), // orange
                                    fromHtml("<font color='#ff6000'><b>" + mesg + "</b></font>"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    // Process the read GPX file
                    if (inFile != null) {
                        StringBuilder gpxsb = new StringBuilder();
                        AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.setMessage(R.string.confirmGPSImport);
                        builder.setCancelable(true).setPositiveButton(R.string.importButton, (dialog, id) ->
                        {
                            if (!selectedFile.isEmpty()) {
                                FileInputStream fileIS;
                                try {
                                    fileIS = new FileInputStream(selectedFile);
                                    String gpxLine;
                                    BufferedReader xmlBR = new BufferedReader(new InputStreamReader(fileIS));

                                    while ((gpxLine = xmlBR.readLine()) != null) {
                                        gpxsb.append(gpxLine).append('\n');
                                    }
                                    fileIS.close();
                                } catch (IOException e) {
                                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                                        Log.e(TAG, "1452, decodeGPX, " +
                                                "Problem converting Stream to String: " + e);
                                }

                                String gpxString = gpxsb.toString();

                                mesg = getString(R.string.waitImport);
                                Toast.makeText(WelcomeActivity.this,
                                        fromHtml("<font color='blue'>" + mesg + "</font>"),
                                        Toast.LENGTH_SHORT).show();

                                // Parse gpxString to get number of tracks
                                String gpxStringT = gpxString;
                                int numTrk = 0; // number of tracks
                                int indexRestString;

                                while (gpxStringT.contains("<trk>")) {
                                    numTrk++;
                                    indexRestString = gpxStringT.indexOf("</trk>") + 6;
                                    gpxStringT = gpxStringT.substring(indexRestString);
                                }

                                // Get number of sections and compare with number of tracks
                                int numSect = sectionDataSource.getNumEntries();
                                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                                    Log.i(TAG, "1477, decodeGPX, numSect: "
                                            + numSect + ", numTrk: " + numTrk);

                                if (numSect != numTrk) {
                                    mesg = getString(R.string.track_err);
                                    Toast.makeText(WelcomeActivity.this,
                                            fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                ExecutorService executor = Executors.newSingleThreadExecutor();
                                executor.execute(() -> {
                                    decodeGPX(gpxString); // do it in background
                                });

                                mesg = getString(R.string.importGPS);
                                Toast.makeText(WelcomeActivity.this, // bright green
                                        fromHtml("<font color='#008000'>" + mesg + "</font>"),
                                        Toast.LENGTH_SHORT).show();

                            } else {
                                mesg = getString(R.string.no_GPSfile);
                                Toast.makeText(WelcomeActivity.this, // orange
                                        fromHtml("<font color='#ff6000'><b>" + mesg + "</b></font>"),
                                        Toast.LENGTH_LONG).show();
                            }
                        }).setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());

                        alert = builder.create();
                        alert.show();
                    }
                }
            });

    // Parse the GPX string for its data items
    private void decodeGPX(String gpxString) {
        String gpxTrkString;

        // Begin with start of 1. track segment as start of string to be checked
        int trkPt = 1; // trackpoint counter
        int trk = 1;   // track counter
        int strStart = gpxString.indexOf("<trk>");
        int strEnd; // end of string to be checked
        int trkStart, trkEnd; // start and end of a track segment

        // Check for gpx syntax (Garmin/Viking)
        boolean gpxVik = false;
        String gpxStringT = gpxString;
        if (gpxStringT.contains("</trkpt>")) {
            gpxVik = true;
        }

        if (gpxString.contains("<trk>")) // test for track data
        {
            // reduce gpxString to all tracks <trk> ... </gpx>
            gpxString = gpxString.substring(strStart); // rest string with all tracks

            // For each track
            do {
                // get data of indexed track
                trkStart = gpxString.indexOf("<trk>");
                trkEnd = gpxString.indexOf("</trk>");
                // add offset = length of "</trk>" = 6
                gpxTrkString = gpxString.substring(trkStart, trkEnd + 6);
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "1543, decodeGPX, gpxTrkString: " + gpxTrkString);

                // set track name from section name
                // record of transect section for trk
                Section section = sectionDataSource.getSection(trk);
                sectionNameGPS = section.name;

                // reduce gpxString to rest after </trk>
                strEnd = gpxString.indexOf("</trk>");

                // add offset = length of "</trk>" = 6
                gpxString = gpxString.substring(strEnd + 6);

                // get coords of all track points
                if (gpxTrkString.contains("<trkseg>")) {
                    // for each track point in trkseg
                    do {
                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.i(TAG, "1561, decodeGPX, do trackpt");
                        int nextTp; // index for next track point (after /> or /trkpt>)
                        strStart = gpxTrkString.indexOf("lat=") + 5;
                        strEnd = gpxTrkString.indexOf("lat=") + 14;
                        String tlat = gpxTrkString.substring(strStart, strEnd);

                        strStart = gpxTrkString.indexOf("lon=") + 5;
                        strEnd = gpxTrkString.indexOf("lon=") + 14;
                        String tlon = gpxTrkString.substring(strStart, strEnd);
                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.i(TAG, "1571 decodeGPX, sectionNameGPS: "
                                    + sectionNameGPS + ", " + tlat + ", " + tlon);

                        trackDataSource.createTrackTp(sectionNameGPS, tlat, tlon);

                        // increment gpxTrkString line for next track point
                        trkPt = trkPt + 1;

                        if (gpxVik) // test for track data (Viking/Garmin)
                        {
                            // reduce gpxTrkString to remainder after current trkpt line
                            nextTp = gpxTrkString.indexOf("</trkpt>");
                            gpxTrkString = gpxTrkString.substring(nextTp + 8);

                            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                                Log.i(TAG, "1586, decodeGPX, trackpt " + trkPt);
                        } else {
                            nextTp = gpxTrkString.indexOf("/>");
                            gpxTrkString = gpxTrkString.substring(nextTp + 2);

                            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                                Log.i(TAG, "1592, decodeGPX, trackpt " + trkPt);
                        }
                    } while (gpxTrkString.contains("<trkpt"));
                }

                // increment trk and reduce gpxString for next track segment
                trk = trk + 1;
            } while (gpxString.contains("<trk>"));

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "1602, decodeGPX, gpxString finished: " + gpxString);
        }

        transectHasTrack = true;
        editor = prefs.edit();
        editor.putBoolean("transect_has_track", transectHasTrack);
        editor.apply();
        exportBasicDb(1); // Write modified DB as Basic DB without message
    }
    // End of importGPS()
    // End of import file functions group

    /**********************************************************************************************/
    // Delete all track info from TRACK_TABLE
    @SuppressLint("ApplySharedPref")
    private void deleteGPS() {
        // Confirm dialogue before anything else takes place
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmGPSDelete);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.deleteButton, (dialog, id) ->
        {
            boolean r_ok = clearGPSValues();

            if (r_ok) {
                // switch autoSection off
                exportBasicDb(1); // Write modified DB as Basic DB without message

                if (locServiceOn) {
                    stopLocSrv();
                    locServiceOn = false;
                }
                transectHasTrack = false;
                autoSection = false;
                editor = prefs.edit();
                editor.putBoolean("pref_auto_section", false);
                editor.putBoolean("transect_has_track", false);
                editor.commit();

                mesg = getString(R.string.resetTracks);
                Toast.makeText(this, // bright green
                        fromHtml("<font color='#008000'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
        alert = builder.create();
        alert.show();
    }

    // Clear track coordinates from TRACK_TABLE
    private boolean clearGPSValues() {
        dbHelper = new DbHelper(this);
        database = dbHelper.getWritableDatabase();
        boolean r_ok = false; // is false when reset fails

        try {
            String sql = "DELETE FROM " + DbHelper.TRACK_TABLE;
            database.execSQL(sql);
            r_ok = true;
        } catch (Exception e) {
            mesg = getString(R.string.resetFail);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
        }
        dbHelper.close();
        return r_ok;
    }
    // End of deleteGPS()

    /***********************************************************************************************
     * The next five functions below are for exporting data files.
     */
    // Exports Basic DB to Documents/TransektCount/transektcount0_ll.db
    private void exportBasicDb(int i) {
        // i = 0: don't show a message, use local language and short name
        // i = 1: don't show a message and use data language and long name
        // i = 2: show message and use data language and long name
        // inFile <- /data/data/com.wmstein.transektcount/databases/transektcount.db
        String inPath = getApplicationContext().getFilesDir().getPath();
        inPath = inPath.substring(0, inPath.lastIndexOf("/")) + "/databases/transektcount.db";
        inFile = new File(inPath);

        // tmpFile -> /data/data/com.wmstein.transektcount/files/transektcount_tmp.db
        String tmpPath = getApplicationContext().getFilesDir().getPath();
        tmpPath = tmpPath.substring(0, tmpPath.lastIndexOf("/")) + "/files/transektcount_tmp.db";
        File tmpFile = new File(tmpPath);

        // outFile in Public Directory Documents/TransektCount/
        // outFile -> /storage/emulated/0/Documents/TransektCount/transektcount0_dl_.db
        File path;
        path = Environment.getExternalStorageDirectory();
        path = new File(path + "/Documents/TransektCount");

        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // just verify path, result ignored

        if (i == 0) // Initial creation of basic DB with
            outFile = new File(path, "/transektcount0_" + localLanguage + ".db");
        else if (i > 0) { // For all later creations of basic DB
            dataLanguage = prefs.getString("pref_sel_data_lang", "");
            if (dataLanguage.isEmpty())
                dataLanguage = "--";

            if (Objects.equals(transNoDir, ""))
                outFile = new File(path, "/transektcount0_" + dataLanguage + ".db");
            else
                outFile = new File(path, "/transektcount0_" + dataLanguage + "_" + transNoDir + ".db");
        }

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED.equals(sState);

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
        } else {
            // Export the basic db
            try {
                // Save current db as backup db tmpFile
                copy(inFile, tmpFile);

                // Clear DB and location values for basic DB
                boolean r_ok = clearDBValues();

                // Write Basic DB
                if (r_ok) {
                    copy(inFile, outFile);
                    // Restore actual db from tmpFile
                    copy(tmpFile, inFile);
                }

                // Delete backup db
                boolean d0 = tmpFile.delete();

                // Show message success
                if (d0 && i == 2) {
                    mesg = getString(R.string.saveBasisDB);
                    Toast.makeText(this,
                            fromHtml("<font color='blue'>" + mesg + "</font>"),
                            Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                mesg = getString(R.string.saveFail);
                Toast.makeText(this,
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    // End of exportBasicDb()

    @SuppressLint({"SdCardPath", "LongLogTag"})
    private void exportDb() {
        // Public data directory for outFile: Documents/TransektCount/
        File path;
        path = Environment.getExternalStorageDirectory();
        path = new File(path + "/Documents/TransektCount");

        String date, start_tm;

        // Open Meta table for meta info
        meta = metaDataSource.getMeta();
        date = meta.date; // has system date
        start_tm = meta.start_tm;

        if (date == null) // 'date' field could be null in meta table
            date = "";
        if (start_tm == null) // 'start_tm' field could be null in meta table
            start_tm = "";

        boolean engl = false;
        boolean hasDate = true;
        String dbDate, dbTime;
        String dbDateEN, dbDateEU;

        // Get year from date
        if (!Objects.equals(date, "")) {
            // "-" at position 4 of the date means EN
            if (Objects.equals(date.substring(4, 5), "-"))
                engl = true;
            // Create date for filename as YYYYMMDD from any format
            dbDateEU = date.substring(6, 10) + date.substring(3, 5) + date.substring(0, 2);
            dbDateEN = date.substring(0, 4) + date.substring(5, 7) + date.substring(8, 10);
        } else {
            dbDateEU = "";
            dbDateEN = "";
            hasDate = false;
        }

        if (engl)
            dbDate = dbDateEN;
        else
            dbDate = dbDateEU;

        if (!Objects.equals(start_tm, "")) {
            dbTime = start_tm.substring(0, 2) + start_tm.substring(3, 5);

            if (hasDate)
                dbDate = dbDate + "_" + dbTime; // yyyymmdd_hhmm
        } else
            dbDate = ""; // has only a value when both date and start time are given

        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // Just verify path, result ignored

        dataLanguage = prefs.getString("pref_sel_data_lang", "");
        if (dataLanguage.isEmpty())
            dataLanguage = "--";

        // outFile -> /storage/emulated/0/Documents/TransektCount/transektcount_Tr-No_yyyyMMdd_HHmm.db
        if (Objects.equals(transNoDir, "") && Objects.equals(dbDate, ""))
            outFile = new File(path, "/transektcount_" + dataLanguage + "_" + getcurDate() + ".db");
        else if (Objects.equals(transNoDir, ""))
            outFile = new File(path, "/transektcount_" + dataLanguage + "_" + dbDate + ".db");
        else if (Objects.equals(dbDate, ""))
            outFile = new File(path, "/transektcount_" + dataLanguage + "_" + transNoDir + "_" + getcurDate() + ".db");
        else
            outFile = new File(path, "/transektcount_" + dataLanguage + "_" + transNoDir + "_" + dbDate + ".db");

        // inFile <- /data/data/com.wmstein.transektcount/databases/transektcount.db
        String inPath = getApplicationContext().getFilesDir().getPath();
        inPath = inPath.substring(0, inPath.lastIndexOf("/"))
                + "/databases/transektcount.db";
        inFile = new File(inPath);

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED.equals(sState);

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
        } else {
            // Export the db
            try {
                copy(inFile, outFile);

                mesg = getString(R.string.saveDB);
                Toast.makeText(this,
                        fromHtml("<font color='blue'>" + mesg + "</font>"),
                        Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                mesg = getString(R.string.saveFail);
                Toast.makeText(this,
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    // End of exportDb()

    /***********************************************************************************************
     // Exports DB contents as Transekt_dL_TransNo_yyyyMMdd_HHmm.csv to
     // Documents/TransektCount/ with purged data set.
     // Spreadsheet programs can import this csv file with
     //   - Unicode UTF-8 filter,
     //   - comma delimiter and
     //   - "" for text recognition.
     */
    private void exportDb2CSV() {
        // Public data directory for outFile: Documents/TransektCount/
        File path;
        path = Environment.getExternalStorageDirectory();
        path = new File(path + "/Documents/TransektCount");

        // Prepare metadata
        String inspecName;  // Inspector's name
        int temps, tempe;   // temperature at start time and end time
        int winds, winde;   // wind
        int clouds, cloude; // clouds
        String date, start_tm;
        String end_tm, kw, inspection_note; // kw = calendar week (String)

        // Open Meta table for meta info
        meta = metaDataSource.getMeta();
        temps = meta.temps;
        tempe = meta.tempe;
        winds = meta.winds;
        winde = meta.winde;
        clouds = meta.clouds;
        cloude = meta.cloude;
        date = meta.date; // has system date
        start_tm = meta.start_tm;
        end_tm = meta.end_tm;
        inspection_note = meta.note;

        if (inspection_note == null)
            inspection_note = "";
        if (date == null)
            date = "";
        if (start_tm == null)
            start_tm = "";

        boolean engl = false;
        boolean hasDate = true;
        String csvDate, csvTime;
        String csvDateEU, csvDateEN;

        // Get year from date
        if (!Objects.equals(date, "")) {
            // "-" at position 4 of the date means EN
            if (Objects.equals(date.substring(4, 5), "-"))
                engl = true;
            // Create date for filename as YYYYMMDD from any format
            csvDateEU = date.substring(6, 10) + date.substring(3, 5) + date.substring(0, 2);
            csvDateEN = date.substring(0, 4) + date.substring(5, 7) + date.substring(8, 10);
        } else {
            hasDate = false;
            csvDateEN = "";
            csvDateEU = "";
        }

        if (engl)
            csvDate = csvDateEN;
        else
            csvDate = csvDateEU;

        if (!Objects.equals(start_tm, "")) {
            csvTime = start_tm.substring(0, 2) + start_tm.substring(3, 5);

            if (hasDate)
                csvDate = csvDate + "_" + csvTime; // yyyymmdd_hhmm
        } else
            csvDate = ""; // has only a value when both date and start time are given

        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // Just verify path, result ignored

        String tranSect;
        String language = Locale.getDefault().toString().substring(0, 2);

        if (language.equals("de")) {
            tranSect = "/Transekt_";
        } else if (language.equals("it")) {
            tranSect = "/Transetto_";
        } else {
            tranSect = "/Transect_";
        }

        String sortIdent, sortMode;
        switch (outPref) {
            case "names" -> {
                sortIdent = "n";
                sortMode = getString(R.string.sortModeN);
            }
            case "codes" -> {
                sortIdent = "c";
                sortMode = getString(R.string.sortModeC);
            }
            case "sections_names" -> {
                sortIdent = "sn";
                sortMode = getString(R.string.sortModeSN);
            }
            default -> { // section_codes
                sortIdent = "sc";
                sortMode = getString(R.string.sortModeSC);
            }
        }

        dataLanguage = prefs.getString("pref_sel_data_lang", "");
        if (dataLanguage.isEmpty())
            dataLanguage = "--";

        if (Objects.equals(transNoDir, "") && Objects.equals(csvDate, ""))
            outFile = new File(path, tranSect + dataLanguage + "_" + getcurDate() + "_" + sortIdent + ".csv");
        else if (Objects.equals(transNoDir, ""))
            outFile = new File(path, tranSect + dataLanguage + "_" + csvDate + "_" + sortIdent + ".csv");
        else if (Objects.equals(csvDate, ""))
            outFile = new File(path, tranSect + dataLanguage + "_" + transNoDir + "_" + getcurDate() + "_" + sortIdent + ".csv");
        else
            outFile = new File(path, tranSect + dataLanguage + "_" + transNoDir + "_" + csvDate + "_" + sortIdent + ".csv");

        Section section;
        int sect_id;

        //  ♂|♀        ♂         ♀         pupa      larva     ovo
        int summf = 0, summ = 0, sumf = 0, sump = 0, suml = 0, sumo = 0;
        int summfe = 0, summe = 0, sumfe = 0, sumpe = 0, sumle = 0, sumoe = 0;

        int totali, totale;
        int total, sumSpec;

        int yyyy, mm, dd;
        int Kw = 0; // calendar week (Int)

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED.equals(sState);

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
        } else {
            // Export purged db as csv
            dbHelper = new DbHelper(this);
            database = dbHelper.getWritableDatabase();

            // Get number of different species
            sumSpec = countDataSource.getDiffSpec();

            //********************************
            // Start creating csv table output
            try {
                CSVWriter csvWrite = new CSVWriter(new FileWriter(outFile));

                // Set header according to table representation in spreadsheet
                String[] arrCol;
                switch (outPref) {
                    case "names", "codes" -> arrCol = new String[]{
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
                    };
                    default -> arrCol = new String[]{
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
                    };
                }

                csvWrite.writeNext(arrCol); // write line to csv-file

                // Open Head table for head info
                head = headDataSource.getHead();
                inspecName = "\"" + head.inspector_name + "\"";

                // Calculating the week of the year (ISO 8601)
                Calendar cal = Calendar.getInstance();

                if (!date.isEmpty()) {
                    if (engl) {
                        yyyy = Integer.parseInt(date.substring(0, 4));
                        mm = Integer.parseInt(date.substring(5, 7));
                        dd = Integer.parseInt(date.substring(8, 10));
                    } else {
                        yyyy = Integer.parseInt(date.substring(6, 10));
                        mm = Integer.parseInt(date.substring(3, 5));
                        dd = Integer.parseInt(date.substring(0, 2));
                    }

                    // Example: cal.set(2017, 3, 9) -> 09.04.2017
                    cal.set(yyyy, mm - 1, dd);
                    Kw = cal.get(Calendar.WEEK_OF_YEAR);
                }

                // 1. headline info with
                //    transect no., inspector name, date, start-time, start-temperature, start-wind,
                //    clouds, calendar week and inspection note
                kw = String.valueOf(Kw);
                String[] arrMeta;
                switch (outPref) {
                    case "names", "codes" -> arrMeta = new String[]{
                            "\"" + transNo + "\"",
                            inspecName,
                            "\"" + date + "\"",
                            getString(R.string.from),
                            "\"" + start_tm + "\"",
                            String.valueOf(temps),
                            String.valueOf(winds),
                            String.valueOf(clouds),
                            "",
                            kw,
                            "", "", "", "", "", "",
                            "\"" + inspection_note + "\"",
                    };
                    default -> arrMeta = new String[]{
                            "\"" + transNo + "\"",
                            inspecName,
                            "\"" + date + "\"",
                            getString(R.string.from),
                            "\"" + start_tm + "\"",
                            String.valueOf(temps),
                            String.valueOf(winds),
                            String.valueOf(clouds),
                            "",
                            kw,
                            "", "", "", "", "", "", "",
                            "\"" + inspection_note + "\"",
                    };
                }
                csvWrite.writeNext(arrMeta);

                // 2. headline info with
                //    end-time, end-temperature, end-clouds
                String[] arrMeta1 =
                        {"", "", "", getString(R.string.to), "\"" + end_tm + "\"", String.valueOf(tempe),
                                String.valueOf(winde), String.valueOf(cloude),};
                csvWrite.writeNext(arrMeta1);

                // Empty row
                String[] arrEmpt = {};
                csvWrite.writeNext(arrEmpt);

                String[] arrIE;
                switch (outPref) {
                    case "names" ->
                            arrIE = new String[]{"", "", "", "", getString(R.string.internal), "", "", "", "", "", getString(R.string.external)};
                    case "codes" ->
                            arrIE = new String[]{"", "", "", "", getString(R.string.internal), "", "", "", "", "", getString(R.string.external)};
                    case "sections_names" ->
                            arrIE = new String[]{getString(R.string.name_sect), "", getString(R.string.specis), "", "", getString(R.string.internal), "", "", "", "", "", getString(R.string.external)};
                    default ->
                            arrIE = new String[]{getString(R.string.name_sect), "", getString(R.string.specis), "", "", getString(R.string.internal), "", "", "", "", "", getString(R.string.external)};
                }
                csvWrite.writeNext(arrIE);

                String nameSpecG = Utils.nameSpecG(dataLanguage);

                // Headline of species table with
                String[] arrCol1 = switch (outPref) {
                    case "names" ->
                        // Species Name, Local Name, Code, Section, Internal Counts, External Counts,
                        //   Spec.-Notes
                            new String[]{
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
                            };
                    case "codes" ->
                        // Species Code, Name, Local Name, Section, Internal Counts, External Counts,
                        //   Spec.-Notes
                            new String[]{
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
                            };
                    case "sections_names" ->
                        // Section, Time of 1. Count, Species Name, Local Name, Code, Internal Counts,
                        //   External Counts, Spec.-Notes
                            new String[]{
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
                            };
                    default -> // section_codes
                        // Section, Time of 1. Count, Code, Species Name, Local Name, Internal Counts,
                        //   External Counts, Spec.-Notes
                            new String[]{
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
                            };
                };
                csvWrite.writeNext(arrCol1);

                //*****************************************************************************
                // Build the internal species array according to the sorted species or sections
                Cursor curCSV;
                switch (outPref) {
                    case "names" ->
                        // Cursor contains list sorted by section and name with all internal count entries > 0
                        //   and all empty counts with a "0" in a section's species remark
                            curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                                    + " WHERE ("
                                    + DbHelper.C_NOTES + " = '0' or "
                                    + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                    + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                    + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                    + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                    + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                    + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                    + " order by " + DbHelper.C_NAME + ", " + DbHelper.C_SECTION_ID, null);
                    case "codes" ->
                        // Cursor contains list sorted by name and section with all internal count entries > 0
                        //   and all empty counts with a "0" in a section's species remark
                            curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                                    + " WHERE ("
                                    + DbHelper.C_NOTES + " = '0' or "
                                    + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                    + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                    + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                    + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                    + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                    + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                    + " order by " + DbHelper.C_CODE + ", " + DbHelper.C_SECTION_ID, null);
                    case "sections_names" ->
                        // Cursor contains list sorted by name and section with all internal count entries > 0
                        //   and all empty counts with a "0" in a section's species remark
                            curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                                    + " WHERE ("
                                    + DbHelper.C_NOTES + " = '0' or "
                                    + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                    + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                    + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                    + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                    + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                    + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                    + " order by " + DbHelper.C_SECTION_ID + ", " + DbHelper.C_NAME, null);
                    default ->
                        // Cursor contains list sorted by name and section with all internal count entries > 0
                        //   and all empty counts with a "0" in a section's species remark
                            curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                                    + " WHERE ("
                                    + DbHelper.C_NOTES + " = '0' or "
                                    + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                    + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                    + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                    + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                    + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                    + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                    + " order by " + DbHelper.C_SECTION_ID + ", " + DbHelper.C_CODE, null);
                }

                String code;       // current species code
                String name_s;     // scientific name
                String name_l;     // local name
                String sp_notes;   // species notes

                // Internal counts variables
                int countmf, countm, countf, countp, countl, counte;
                String strcountmf, strcountm, strcountf, strcountp, strcountl, strcounte;

                // External counts variables
                int countmfe, countme, countfe, countpe, countle, countee;
                String strcountmfe, strcountme, strcountfe, strcountpe, strcountle, strcountee;

                // Sums and totals variables
                String strsummf, strsumm, strsumf, strsump, strsuml, strsumo;
                String strsummfe, strsumme, strsumfe, strsumpe, strsumle, strsumoe;

                String strtotali, strtotale, strtotal;

                String sectName;  // name shown in list
                String sectName1 = "";
                String sectName2;
                String sectTime;
                String timePattern = "HH:mm:ss";
                SimpleDateFormat sdf = new SimpleDateFormat(timePattern, Locale.getDefault());
                long sectTimeValue;

                curCSV.moveToFirst();
                while (!curCSV.isAfterLast()) {
                    sect_id = curCSV.getInt(1);
                    section = sectionDataSource.getSection(sect_id);
                    sectName = section.name;

                    if (!Objects.equals(sectName1, sectName)) { // sectName != ""
                        sectName1 = sectName;
                        sectName2 = sectName;
                        sectTimeValue = section.datNum(); // get Long created_at value
                        Date result = new Date(sectTimeValue);
                        sectTime = sdf.format(result);
                    } else {
                        sectTime = "";
                        sectName2 = "";
                    }

                    code = "\"" + curCSV.getString(3) + "\""; //species code
                    name_s = curCSV.getString(2);
                    name_l = curCSV.getString(17);
                    sp_notes = curCSV.getString(16);
                    if (sp_notes == null)
                        sp_notes = "";
                    else
                        sp_notes = "\"" + sp_notes + "\"";

                    countmf = curCSV.getInt(4);
                    if (countmf > 0)
                        strcountmf = Integer.toString(countmf);
                    else
                        strcountmf = "";

                    countm = curCSV.getInt(5);
                    if (countm > 0)
                        strcountm = Integer.toString(countm);
                    else
                        strcountm = "";

                    countf = curCSV.getInt(6);
                    if (countf > 0)
                        strcountf = Integer.toString(countf);
                    else
                        strcountf = "";

                    countp = curCSV.getInt(7);
                    if (countp > 0)
                        strcountp = Integer.toString(countp);
                    else
                        strcountp = "";

                    countl = curCSV.getInt(8);
                    if (countl > 0)
                        strcountl = Integer.toString(countl);
                    else
                        strcountl = "";

                    counte = curCSV.getInt(9);
                    if (counte > 0)
                        strcounte = Integer.toString(counte);
                    else
                        strcounte = "";

                    countmfe = curCSV.getInt(10);
                    if (countmfe > 0)
                        strcountmfe = Integer.toString(countmfe);
                    else
                        strcountmfe = "";

                    countme = curCSV.getInt(11);
                    if (countme > 0)
                        strcountme = Integer.toString(countme);
                    else
                        strcountme = "";

                    countfe = curCSV.getInt(12);
                    if (countfe > 0)
                        strcountfe = Integer.toString(countfe);
                    else
                        strcountfe = "";

                    countpe = curCSV.getInt(13);
                    if (countpe > 0)
                        strcountpe = Integer.toString(countpe);
                    else
                        strcountpe = "";

                    countle = curCSV.getInt(14);
                    if (countle > 0)
                        strcountle = Integer.toString(countle);
                    else
                        strcountle = "";

                    countee = curCSV.getInt(15);
                    if (countee > 0)
                        strcountee = Integer.toString(countee);
                    else
                        strcountee = "";

                    String[] arrStr = switch (outPref) {
                        case "names" ->  // names
                            // Build line in species table for order of species
                                new String[]{
                                        name_s,      // species name
                                        name_l,      // species local name
                                        code,        // species code as String
                                        sectName,    // section name
                                        strcountmf,  // count mf
                                        strcountm,   // count m
                                        strcountf,   // count f
                                        strcountp,   // count p
                                        strcountl,   // count l
                                        strcounte,   // count e
                                        strcountmfe, // count mfe
                                        strcountme,  // count me
                                        strcountfe,  // count fe
                                        strcountpe,  // count pe
                                        strcountle,  // count le
                                        strcountee,  // count ee
                                        sp_notes     // spec. notes
                                };
                        case "codes" ->  // names
                            // Build line in species table for order of species
                                new String[]{
                                        code,        // species code as String
                                        name_s,      // species name
                                        name_l,      // species local name
                                        sectName,    // section name
                                        strcountmf,  // count mf
                                        strcountm,   // count m
                                        strcountf,   // count f
                                        strcountp,   // count p
                                        strcountl,   // count l
                                        strcounte,   // count e
                                        strcountmfe, // count mfe
                                        strcountme,  // count me
                                        strcountfe,  // count fe
                                        strcountpe,  // count pe
                                        strcountle,  // count le
                                        strcountee,  // count ee
                                        sp_notes     // spec. notes
                                };
                        case "sections_names" ->
                            // Build line in species table for order of sections
                                new String[]{
                                        sectName2,   // section name
                                        sectTime,    // time of 1. count in section
                                        name_s,      // species name
                                        name_l,      // species local name
                                        code,        // species code as String
                                        strcountmf,  // count mf
                                        strcountm,   // count m
                                        strcountf,   // count f
                                        strcountp,   // count p
                                        strcountl,   // count l
                                        strcounte,   // count e
                                        strcountmfe, // count mfe
                                        strcountme,  // count me
                                        strcountfe,  // count fe
                                        strcountpe,  // count pe
                                        strcountle,  // count le
                                        strcountee,  // count ee
                                        sp_notes     // spec. notes
                                };
                        default ->  // section_codes
                            // Build line in species table for order of species
                                new String[]{
                                        sectName2,   // section name
                                        sectTime,    // time of 1. count in section
                                        code,        // species code as String
                                        name_s,      // species name
                                        name_l,      // species local name
                                        strcountmf,  // count mf
                                        strcountm,   // count m
                                        strcountf,   // count f
                                        strcountp,   // count p
                                        strcountl,   // count l
                                        strcounte,   // count e
                                        strcountmfe, // count mfe
                                        strcountme,  // count me
                                        strcountfe,  // count fe
                                        strcountpe,  // count pe
                                        strcountle,  // count le
                                        strcountee,  // count ee
                                        sp_notes     // spec. notes
                                };
                    };
                    csvWrite.writeNext(arrStr);

                    summf = summf + curCSV.getInt(4);
                    summ = summ + curCSV.getInt(5);
                    sumf = sumf + curCSV.getInt(6);
                    sump = sump + curCSV.getInt(7);
                    suml = suml + curCSV.getInt(8);
                    sumo = sumo + curCSV.getInt(9);
                    summfe = summfe + curCSV.getInt(10);
                    summe = summe + curCSV.getInt(11);
                    sumfe = sumfe + curCSV.getInt(12);
                    sumpe = sumpe + curCSV.getInt(13);
                    sumle = sumle + curCSV.getInt(14);
                    sumoe = sumoe + curCSV.getInt(15);
                    curCSV.moveToNext();
                }
                curCSV.close();

                //**************************
                // Empty row before sum area
                csvWrite.writeNext(arrEmpt);

                totali = summf + summ + sumf + sump + suml + sumo;
                totale = summfe + summe + sumfe + sumpe + sumle + sumoe;
                total = totali + totale;

                // Intern, extern
                String[] arrIE1;
                switch (outPref) {
                    case "names" ->
                            arrIE1 = new String[]{"", "", "", "", getString(R.string.internal), "", "", "", "", "", getString(R.string.external)};
                    case "codes" ->
                            arrIE1 = new String[]{"", "", "", "", getString(R.string.internal), "", "", "", "", "", getString(R.string.external)};
                    case "sections_names" ->
                            arrIE1 = new String[]{"", "", "", "", "", getString(R.string.internal), "", "", "", "", "", getString(R.string.external)};
                    default ->
                            arrIE1 = new String[]{"", "", "", "", "", getString(R.string.internal), "", "", "", "", "", getString(R.string.external)};
                }
                csvWrite.writeNext(arrIE1);

                // Internal counts, External counts, Totals
                String[] arrCol2;
                switch (outPref) {
                    case "names", "codes" -> arrCol2 = new String[]{"",
                            getString(R.string.sumSpec), "", "",
                            getString(R.string.countImagomfHint), getString(R.string.countImagomHint), getString(R.string.countImagofHint), getString(R.string.countPupaHint), getString(R.string.countLarvaHint), getString(R.string.countOvoHint),
                            getString(R.string.countImagomfHint), getString(R.string.countImagomHint), getString(R.string.countImagofHint), getString(R.string.countPupaHint), getString(R.string.countLarvaHint), getString(R.string.countOvoHint), getString(R.string.hintTotal)};
                    default -> arrCol2 = new String[]{"", "",
                            getString(R.string.sumSpec), "", "",
                            getString(R.string.countImagomfHint), getString(R.string.countImagomHint), getString(R.string.countImagofHint), getString(R.string.countPupaHint), getString(R.string.countLarvaHint), getString(R.string.countOvoHint),
                            getString(R.string.countImagomfHint), getString(R.string.countImagomHint), getString(R.string.countImagofHint), getString(R.string.countPupaHint), getString(R.string.countLarvaHint), getString(R.string.countOvoHint), getString(R.string.hintTotal)};
                }
                csvWrite.writeNext(arrCol2);

                // Internal sums
                if (summf > 0)
                    strsummf = Integer.toString(summf);
                else
                    strsummf = "";

                if (summ > 0)
                    strsumm = Integer.toString(summ);
                else
                    strsumm = "";

                if (sumf > 0)
                    strsumf = Integer.toString(sumf);
                else
                    strsumf = "";

                if (sump > 0)
                    strsump = Integer.toString(sump);
                else
                    strsump = "";

                if (suml > 0)
                    strsuml = Integer.toString(suml);
                else
                    strsuml = "";

                if (sumo > 0)
                    strsumo = Integer.toString(sumo);
                else
                    strsumo = "";

                if (totali > 0)
                    strtotali = Integer.toString(totali);
                else
                    strtotali = "";

                // Write internal total sums
                String[] arrSumi;
                switch (outPref) {
                    case "names", "codes" -> arrSumi = new String[]{"",
                            Integer.toString(sumSpec), "", getString(R.string.sumi),
                            strsummf, strsumm, strsumf, strsump, strsuml, strsumo,
                            "", "", "", "", "", "", strtotali};
                    default -> arrSumi = new String[]{"", "",
                            Integer.toString(sumSpec), "", getString(R.string.sumi),
                            strsummf, strsumm, strsumf, strsump, strsuml, strsumo,
                            "", "", "", "", "", "", strtotali};
                }
                csvWrite.writeNext(arrSumi);

                // External sums
                if (summfe > 0)
                    strsummfe = Integer.toString(summfe);
                else
                    strsummfe = "";

                if (summe > 0)
                    strsumme = Integer.toString(summe);
                else
                    strsumme = "";

                if (sumfe > 0)
                    strsumfe = Integer.toString(sumfe);
                else
                    strsumfe = "";

                if (sumpe > 0)
                    strsumpe = Integer.toString(sumpe);
                else
                    strsumpe = "";

                if (sumle > 0)
                    strsumle = Integer.toString(sumle);
                else
                    strsumle = "";

                if (sumoe > 0)
                    strsumoe = Integer.toString(sumoe);
                else
                    strsumoe = "";

                if (totale > 0)
                    strtotale = Integer.toString(totale);
                else
                    strtotale = "";

                // Write external total sums
                String[] arrSume;
                switch (outPref) {
                    case "names", "codes" -> arrSume = new String[]{"", "", "",
                            getString(R.string.sume), "", "", "", "", "", "",
                            strsummfe, strsumme, strsumfe, strsumpe, strsumle, strsumoe, strtotale};
                    default -> arrSume = new String[]{"", "", "", "",
                            getString(R.string.sume), "", "", "", "", "", "",
                            strsummfe, strsumme, strsumfe, strsumpe, strsumle, strsumoe, strtotale};
                }
                csvWrite.writeNext(arrSume);

                // Overall totals
                if (total > 0)
                    strtotal = Integer.toString(total);
                else
                    strtotal = "";

                // Write overall total sum
                String[] arrTotal;
                switch (outPref) {
                    case "names", "codes" -> arrTotal = new String[]{"", "", "",
                            getString(R.string.sum_total), "", "", "", "", "", "",
                            "", "", "", "", "", "", strtotal};
                    default -> arrTotal = new String[]{"", "", "", "",
                            getString(R.string.sum_total), "", "", "", "", "", "",
                            "", "", "", "", "", "", strtotal};
                }
                csvWrite.writeNext(arrTotal);

                csvWrite.close();

                mesg = getString(R.string.saveCSV);
                Toast.makeText(this,
                        fromHtml("<font color='blue'>" + mesg + "</font>"),
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                mesg = getString(R.string.saveFail);
                Toast.makeText(this,
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();

                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.e(TAG, "2688, csv write external failed");
            }
            dbHelper.close();
        }
    }
    // End of exportDb2CSV()

    /***********************************************************************************************
     // Exports DB contents as Transekt_dL_TransNo_yyyyMMdd_HHmm.xlsx to
     // Documents/TransektCount/ with purged data set.
     // Spreadsheet programs can directly load this formatted xlsx file
     */
    private void exportDb2XLSX() {
        mesg = getString(R.string.saveXLSX);
        Toast.makeText(this,
                fromHtml("<font color='blue'>" + mesg + "</font>"),
                Toast.LENGTH_SHORT).show();

        String inspecName;
        int temps, tempe;   // temperature at start time and end time
        int winds, winde;   // wind
        int clouds, cloude; // clouds

        String date, start_tm;
        String end_tm, kw, inspection_note; // kw = calendar week (String)

        // Open Meta table for meta info
        meta = metaDataSource.getMeta();
        temps = meta.temps;
        tempe = meta.tempe;
        winds = meta.winds;
        winde = meta.winde;
        clouds = meta.clouds;
        cloude = meta.cloude;
        date = meta.date; // has system date
        start_tm = meta.start_tm;
        end_tm = meta.end_tm;
        inspection_note = meta.note;

        if (inspection_note == null)
            inspection_note = "";
        if (date == null)
            date = "";
        if (start_tm == null)
            start_tm = "";

        boolean engl = false;
        boolean hasDate = true;
        String xlsxDate, xlsxTime;
        String xlsxDateEU, xlsxDateEN;

        // Get year from date
        if (!Objects.equals(date, "")) {
            // "-" at position 4 of the date means EN
            if (Objects.equals(date.substring(4, 5), "-"))
                engl = true;
            // Create date for filename as YYYYMMDD from any format
            xlsxDateEU = date.substring(6, 10) + date.substring(3, 5) + date.substring(0, 2);
            xlsxDateEN = date.substring(0, 4) + date.substring(5, 7) + date.substring(8, 10);
        } else {
            hasDate = false;
            xlsxDateEN = "";
            xlsxDateEU = "";
        }

        if (engl)
            xlsxDate = xlsxDateEN;
        else
            xlsxDate = xlsxDateEU;

        if (!Objects.equals(start_tm, "")) {
            xlsxTime = start_tm.substring(0, 2) + start_tm.substring(3, 5);

            if (hasDate)
                xlsxDate = xlsxDate + "_" + xlsxTime; // yyyymmdd_hhmm
        } else
            xlsxDate = ""; // has only a value when both date and start time are given

        // outFile -> /storage/emulated/0/Documents/TransektCount/tranSect_DL_transNo_yyyyMMdd_HHmm_si.xlsx
        File path;
        path = Environment.getExternalStorageDirectory();
        path = new File(path + "/Documents/TransektCount");

        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // Just verify path, result ignored

        String tranSect;
        String language = Locale.getDefault().toString().substring(0, 2);

        if (language.equals("de")) {
            tranSect = "/Transekt_";
        } else if (language.equals("it")) {
            tranSect = "/Transetto_";
        } else {
            tranSect = "/Transect_";
        }

        String sortIdent, sortMode;
        switch (outPref) {
            case "names" -> {
                sortIdent = "n";
                sortMode = getString(R.string.sortModeN);
            }
            case "codes" -> {
                sortIdent = "c";
                sortMode = getString(R.string.sortModeC);
            }
            case "sections_names" -> {
                sortIdent = "sn";
                sortMode = getString(R.string.sortModeSN);
            }
            default -> { // section_codes
                sortIdent = "sc";
                sortMode = getString(R.string.sortModeSC);
            }
        }

        dataLanguage = prefs.getString("pref_sel_data_lang", "");
        if (dataLanguage.isEmpty())
            dataLanguage = "--";

        if (Objects.equals(transNoDir, "") && Objects.equals(xlsxDate, ""))
            outFile = new File(path, tranSect + dataLanguage + "_" + getcurDate() + "_" + sortIdent + ".xlsx");
        else if (Objects.equals(transNoDir, ""))
            outFile = new File(path, tranSect + dataLanguage + "_" + xlsxDate + "_" + sortIdent + ".xlsx");
        else if (Objects.equals(xlsxDate, ""))
            outFile = new File(path, tranSect + dataLanguage + "_" + transNoDir + "_" + getcurDate() + "_" + sortIdent + ".xlsx");
        else
            outFile = new File(path, tranSect + dataLanguage + "_" + transNoDir + "_" + xlsxDate + "_" + sortIdent + ".xlsx");

        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(outFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        Section section;
        int sect_id;

        //  ♂|♀        ♂         ♀         pupa      larva     ovo
        int summf = 0, summ = 0, sumf = 0, sump = 0, suml = 0, sumo = 0;
        int summfe = 0, summe = 0, sumfe = 0, sumpe = 0, sumle = 0, sumoe = 0;

        int totali, totale;
        int total, sumSpec;

        int yyyy, mm, dd;
        int Kw = 0; // calendar week (Int)

        // Prepare the fastexcel sheet
        Workbook wb = new Workbook(outputStream, "TransektCount", "1.0");
        wb.properties().setTitle(getString(R.string.results)); //
        wb.setGlobalDefaultFont("Arial", 11);

        Worksheet ws = wb.newWorksheet(getString(R.string.results));
        ws.paperSize(PaperSize.A4_PAPER);
        ws.pageOrientation("landscape");
        ws.freezePane(0, 6); // Fixed lines when scrolling
        ws.repeatRows(0, 5);                       // Fixed lines for printing
        ws.firstPageNumber(1);
        ws.footer("&P / &N", Position.CENTER);
        ws.fitToWidth((short) 1);
        ws.fitToHeight((short) 0);

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED.equals(sState);

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
        } else {
            // Export purged db as xlsx
            dbHelper = new DbHelper(this);
            database = dbHelper.getWritableDatabase();

            // Get number of different species
            sumSpec = countDataSource.getDiffSpec();

            //**************************
            // Start creating xlsx table
            // Row 0: Headline
            ws.value(0, 0, getString(R.string.transectnumber));
            ws.value(0, 1, getString(R.string.inspector));
            ws.value(0, 2, getString(R.string.date));
            ws.value(0, 4, getString(R.string.timehead));
            ws.value(0, 5, getString(R.string.temperature));
            ws.value(0, 6, getString(R.string.wind));
            ws.value(0, 7, getString(R.string.clouds));
            ws.value(0, 9, getString(R.string.kal_w));
            ws.value(0, 11, getString(R.string.inspection_note));
            ws.value(0, 15, sortMode);

            ws.range(0, 0, 0, 14).style().fillColor("CCCCCC")
                    .borderStyle(BorderSide.BOTTOM, "thin").fontSize(12).bold().set();
            ws.range(0, 4, 0, 9).style().horizontalAlignment("center").set();
            ws.range(0,11,0,14).merge();
            ws.style(0,15).horizontalAlignment("right").set();

            switch (outPref) {
                case "names", "codes" ->
                        ws.range(0,15,0,16).merge();
                default ->
                        ws.range(0,15,0,17).merge();
            }

            // Open Head table for head info
            head = headDataSource.getHead();
            inspecName = head.inspector_name;

            // Calculating the week of the year (ISO 8601)
            Calendar cal = Calendar.getInstance();

            if (!date.isEmpty()) {
                if (engl) {
                    yyyy = Integer.parseInt(date.substring(0, 4));
                    mm = Integer.parseInt(date.substring(5, 7));
                    dd = Integer.parseInt(date.substring(8, 10));
                } else {
                    yyyy = Integer.parseInt(date.substring(6, 10));
                    mm = Integer.parseInt(date.substring(3, 5));
                    dd = Integer.parseInt(date.substring(0, 2));
                }

                // Example: cal.set(2017, 3, 9) -> 09.04.2017
                cal.set(yyyy, mm - 1, dd);
                Kw = cal.get(Calendar.WEEK_OF_YEAR);
            }

            // 1. metadata info with
            //    transect no., inspector name, date, start-time, start-temperature, start-wind,
            //    clouds, calendar week and inspection note
            kw = String.valueOf(Kw);
            ws.value(1, 0, transNo);
            ws.value(1, 1, inspecName);
            ws.value(1, 2, date);
            ws.value(1, 3, getString(R.string.from));
            ws.value(1, 4, start_tm);
            ws.value(1, 5, String.valueOf(temps));
            ws.value(1, 6, String.valueOf(winds));
            ws.value(1, 7, String.valueOf(clouds));
            ws.value(1, 9, kw);
            ws.value(1, 11, inspection_note);
            ws.style(1, 3).horizontalAlignment("right").set();
            ws.range(1, 4, 0, 9).style().horizontalAlignment("center").set();
            ws.range(1,11,1,14).merge();

            // 2. metadata info with
            //    end-time, end-temperature, end-clouds
            ws.value(2, 3, getString(R.string.to));
            ws.value(2, 4, end_tm);
            ws.value(2, 5, String.valueOf(tempe));
            ws.value(2, 6, String.valueOf(winde));
            ws.value(2, 7, String.valueOf(cloude));
            ws.style(2, 3).horizontalAlignment("right").set();
            ws.range(2, 4, 0, 7).style().horizontalAlignment("center").set();

            // Headline 1 of species table
            switch (outPref) {
                case "names" -> {
                    ws.value(4, 4, getString(R.string.internal));
                    ws.value(4, 10, getString(R.string.external));
                    ws.style(4, 4).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(4, 10).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(4, 16).borderStyle(BorderSide.LEFT, "thin").bold().set();
                }
                case "codes" -> {
                    ws.value(4, 4, getString(R.string.internal));
                    ws.value(4, 10, getString(R.string.external));
                    ws.style(4, 4).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(4, 10).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(4, 16).borderStyle(BorderSide.LEFT, "thin").bold().set();
                }
                case "sections_names" -> {
                    ws.value(4, 0, getString(R.string.name_sect));
                    ws.value(4, 2, getString(R.string.specis));
                    ws.value(4, 5, getString(R.string.internal));
                    ws.value(4, 11, getString(R.string.external));
                    ws.style(4, 0).bold().set();
                    ws.style(4, 2).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(4, 5).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(4, 11).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(4, 17).borderStyle(BorderSide.LEFT, "thin").bold().set();
                }
                default -> {
                    ws.value(4, 0, getString(R.string.name_sect));
                    ws.value(4, 2, getString(R.string.specis));
                    ws.value(4, 5, getString(R.string.internal));
                    ws.value(4, 11, getString(R.string.external));
                    ws.style(4, 0).bold().set();
                    ws.style(4, 2).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(4, 5).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(4, 11).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(4, 17).borderStyle(BorderSide.LEFT, "thin").bold().set();
                }
            }

            String nameSpecG = Utils.nameSpecG(dataLanguage);

            // Headline 2 of species table
            switch (outPref) {
                case "names" -> {
                    // Species Name, Local Name, Code, Section, Internal Counts, External Counts,
                    //   Spec.-Notes
                    ws.value(5, 0, getString(R.string.name_spec));
                    ws.value(5, 1, nameSpecG);
                    ws.value(5, 2, getString(R.string.code_spec));
                    ws.value(5, 3, getString(R.string.name_sect));
                    ws.value(5, 4, getString(R.string.countImagomfHint));
                    ws.value(5, 5, getString(R.string.countImagomHint));
                    ws.value(5, 6, getString(R.string.countImagofHint));
                    ws.value(5, 7, getString(R.string.countPupaHint));
                    ws.value(5, 8, getString(R.string.countLarvaHint));
                    ws.value(5, 9, getString(R.string.countOvoHint));
                    ws.value(5, 10, getString(R.string.countImagomfHint));
                    ws.value(5, 11, getString(R.string.countImagomHint));
                    ws.value(5, 12, getString(R.string.countImagofHint));
                    ws.value(5, 13, getString(R.string.countPupaHint));
                    ws.value(5, 14, getString(R.string.countLarvaHint));
                    ws.value(5, 15, getString(R.string.countOvoHint));
                    ws.value(5, 16, getString(R.string.rem_spec));
                    ws.range(5, 0, 5, 16).style()
                            .borderStyle(BorderSide.BOTTOM, "thin").bold().set();
                    ws.range(5, 2, 5, 15).style()
                            .horizontalAlignment("center").set();
                    ws.style(5, 4).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").bold().set();
                    ws.style(5, 10).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").bold().set();
                    ws.style(5, 16).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").bold().set();
                }
                case "codes" -> {
                    // Species Code, Name, Local Name, Section, Internal Counts, External Counts,
                    //   Spec.-Notes
                    ws.value(5, 0, getString(R.string.code_spec));
                    ws.value(5, 1, getString(R.string.name_spec));
                    ws.value(5, 2, nameSpecG);
                    ws.value(5, 3, getString(R.string.name_sect));
                    ws.value(5, 4, getString(R.string.countImagomfHint));
                    ws.value(5, 5, getString(R.string.countImagomHint));
                    ws.value(5, 6, getString(R.string.countImagofHint));
                    ws.value(5, 7, getString(R.string.countPupaHint));
                    ws.value(5, 8, getString(R.string.countLarvaHint));
                    ws.value(5, 9, getString(R.string.countOvoHint));
                    ws.value(5, 10, getString(R.string.countImagomfHint));
                    ws.value(5, 11, getString(R.string.countImagomHint));
                    ws.value(5, 12, getString(R.string.countImagofHint));
                    ws.value(5, 13, getString(R.string.countPupaHint));
                    ws.value(5, 14, getString(R.string.countLarvaHint));
                    ws.value(5, 15, getString(R.string.countOvoHint));
                    ws.value(5, 16, getString(R.string.rem_spec));
                    ws.range(5, 0, 5, 16).style()
                            .borderStyle(BorderSide.BOTTOM, "thin").bold().set();
                    ws.style(5, 0).horizontalAlignment("center").set();
                    ws.range(5, 3, 5, 15).style()
                            .horizontalAlignment("center").set();
                    ws.style(5, 4).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").bold().set();
                    ws.style(5, 10).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").bold().set();
                    ws.style(5, 16).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").bold().set();
                }
                case "sections_names" -> {
                    // Section, Time of 1. Count, Species Name, Local Name, Code, Internal Counts,
                    //   External Counts, Spec.-Notes
                    ws.value(5, 0, getString(R.string.strName));
                    ws.value(5, 1, getString(R.string.time_sect));
                    ws.value(5, 2, getString(R.string.strName));
                    ws.value(5, 3, nameSpecG);
                    ws.value(5, 4, getString(R.string.code_spec));
                    ws.value(5, 5, getString(R.string.countImagomfHint));
                    ws.value(5, 6, getString(R.string.countImagomHint));
                    ws.value(5, 7, getString(R.string.countImagofHint));
                    ws.value(5, 8, getString(R.string.countPupaHint));
                    ws.value(5, 9, getString(R.string.countLarvaHint));
                    ws.value(5, 10, getString(R.string.countOvoHint));
                    ws.value(5, 11, getString(R.string.countImagomfHint));
                    ws.value(5, 12, getString(R.string.countImagomHint));
                    ws.value(5, 13, getString(R.string.countImagofHint));
                    ws.value(5, 14, getString(R.string.countPupaHint));
                    ws.value(5, 15, getString(R.string.countLarvaHint));
                    ws.value(5, 16, getString(R.string.countOvoHint));
                    ws.value(5, 17, getString(R.string.rem_spec));
                    ws.range(5, 0, 5, 17).style()
                            .borderStyle(BorderSide.BOTTOM, "thin").bold().set();
                    ws.range(5, 4, 5, 16).style()
                            .horizontalAlignment("center").set();
                    ws.style(5, 2).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").set();
                    ws.style(5, 5).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").set();
                    ws.style(5, 11).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").set();
                    ws.style(5, 17).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin")
                            .horizontalAlignment("left").set();
                }
                default -> {
                    // section_codes
                    // Section, Time of 1. Count, Code, Species Name, Local Name, Internal Counts,
                    //   External Counts, Spec.-Notes
                    ws.value(5, 0, getString(R.string.strName));
                    ws.value(5, 1, getString(R.string.time_sect));
                    ws.value(5, 2, getString(R.string.code_spec));
                    ws.value(5, 3, getString(R.string.name_spec));
                    ws.value(5, 4, nameSpecG);
                    ws.value(5, 5, getString(R.string.countImagomfHint));
                    ws.value(5, 6, getString(R.string.countImagomHint));
                    ws.value(5, 7, getString(R.string.countImagofHint));
                    ws.value(5, 8, getString(R.string.countPupaHint));
                    ws.value(5, 9, getString(R.string.countLarvaHint));
                    ws.value(5, 10, getString(R.string.countOvoHint));
                    ws.value(5, 11, getString(R.string.countImagomfHint));
                    ws.value(5, 12, getString(R.string.countImagomHint));
                    ws.value(5, 13, getString(R.string.countImagofHint));
                    ws.value(5, 14, getString(R.string.countPupaHint));
                    ws.value(5, 15, getString(R.string.countLarvaHint));
                    ws.value(5, 16, getString(R.string.countOvoHint));
                    ws.value(5, 17, getString(R.string.rem_spec));
                    ws.range(5, 0, 5, 17).style()
                            .borderStyle(BorderSide.BOTTOM, "thin").bold().set();
                    ws.style(5, 2).horizontalAlignment("center").set();
                    ws.range(5, 5, 5, 17).style()
                            .horizontalAlignment("center").set();
                    ws.style(5, 2).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").set();
                    ws.style(5, 5).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").set();
                    ws.style(5, 11).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").set();
                    ws.style(5, 17).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin")
                            .horizontalAlignment("left").set();
                }
            }

            //*****************************************************************************
            // Build the internal species array according to the sorted species or sections
            Cursor curXLSX;
            switch (outPref) {
                case "names" ->
                    // Cursor contains list sorted by section and name with all internal count entries > 0
                    //   and all empty counts with a "0" in a section's species remark
                        curXLSX = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                                + " WHERE ("
                                + DbHelper.C_NOTES + " = '0' or "
                                + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                + " order by " + DbHelper.C_NAME + ", " + DbHelper.C_SECTION_ID, null);
                case "codes" ->
                    // Cursor contains list sorted by name and section with all internal count entries > 0
                    //   and all empty counts with a "0" in a section's species remark
                        curXLSX = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                                + " WHERE ("
                                + DbHelper.C_NOTES + " = '0' or "
                                + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                + " order by " + DbHelper.C_CODE + ", " + DbHelper.C_SECTION_ID, null);
                case "sections_names" ->
                    // Cursor contains list sorted by name and section with all internal count entries > 0
                    //   and all empty counts with a "0" in a section's species remark
                        curXLSX = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                                + " WHERE ("
                                + DbHelper.C_NOTES + " = '0' or "
                                + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                + " order by " + DbHelper.C_SECTION_ID + ", " + DbHelper.C_NAME, null);
                default ->
                    // Cursor contains list sorted by name and section with all internal count entries > 0
                    //   and all empty counts with a "0" in a section's species remark
                        curXLSX = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                                + " WHERE ("
                                + DbHelper.C_NOTES + " = '0' or "
                                + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                                + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                                + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                                + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                                + " order by " + DbHelper.C_SECTION_ID + ", " + DbHelper.C_CODE, null);
            }

            String code;       // current species code
            String name_s;     // scientific name
            String name_l;     // local name
            String sp_notes;   // species notes

            // Internal counts variables
            int countmf, countm, countf, countp, countl, counte;

            // External counts variables
            int countmfe, countme, countfe, countpe, countle, countee;

            String sectName;  // name shown in list
            String sectName1 = "";
            String sectName2;
            String sectTime;
            String timePattern = "HH:mm:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(timePattern, Locale.getDefault());
            long sectTimeValue;

            int tableIndex = 6;
            curXLSX.moveToFirst();
            while (!curXLSX.isAfterLast()) {
                sect_id = curXLSX.getInt(1);
                section = sectionDataSource.getSection(sect_id);
                sectName = section.name;

                if (!Objects.equals(sectName1, sectName)) { // sectName != ""
                    sectName1 = sectName;
                    sectName2 = sectName;
                    sectTimeValue = section.datNum(); // get Long created_at value
                    Date result = new Date(sectTimeValue);
                    sectTime = sdf.format(result);
                } else {
                    sectTime = "";
                    sectName2 = "";
                }

                code = curXLSX.getString(3); //species code
                name_s = curXLSX.getString(2);
                name_l = curXLSX.getString(17);
                sp_notes = curXLSX.getString(16);
                if (sp_notes == null)
                    sp_notes = "";

                countmf = curXLSX.getInt(4);
                countm = curXLSX.getInt(5);
                countf = curXLSX.getInt(6);
                countp = curXLSX.getInt(7);
                countl = curXLSX.getInt(8);
                counte = curXLSX.getInt(9);
                countmfe = curXLSX.getInt(10);
                countme = curXLSX.getInt(11);
                countfe = curXLSX.getInt(12);
                countpe = curXLSX.getInt(13);
                countle = curXLSX.getInt(14);
                countee = curXLSX.getInt(15);

                boolean even = tableIndex % 2 == 0;

                // Build the species table with alternating white and gray background
                if (even) {
                    switch (outPref) {
                        case "names" -> {
                            // Build line in species table for order of species
                            ws.value(tableIndex, 0, name_s);
                            ws.value(tableIndex, 1, name_l);
                            ws.value(tableIndex, 2, code);
                            ws.value(tableIndex, 3, sectName);
                            ws.value(tableIndex, 4, countmf);
                            ws.value(tableIndex, 5, countm);
                            ws.value(tableIndex, 6, countf);
                            ws.value(tableIndex, 7, countp);
                            ws.value(tableIndex, 8, countl);
                            ws.value(tableIndex, 9, counte);
                            ws.value(tableIndex, 10, countmfe);
                            ws.value(tableIndex, 11, countme);
                            ws.value(tableIndex, 12, countfe);
                            ws.value(tableIndex, 13, countpe);
                            ws.value(tableIndex, 14, countle);
                            ws.value(tableIndex, 15, countee);
                            ws.value(tableIndex, 16, sp_notes);

                            // Conditional species notes, if "0" -> red
                            if (Objects.equals(sp_notes, "0"))
                                ws.style(tableIndex, 16).fontColor("FF0000").set();

                            if (countmf > 0)
                                ws.style(tableIndex, 4).fontColor("008000").bold().set(); // green
                            else
                                ws.style(tableIndex, 4).fontColor("00FFFFFF").set(); // transparent
                            if (countm > 0)
                                ws.style(tableIndex, 5).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 5).fontColor("00FFFFFF").set();
                            if (countf > 0)
                                ws.style(tableIndex, 6).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 6).fontColor("00FFFFFF").set();
                            if (countp > 0)
                                ws.style(tableIndex, 7).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 7).fontColor("00FFFFFF").set();
                            if (countl > 0)
                                ws.style(tableIndex, 8).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 8).fontColor("00FFFFFF").set();
                            if (counte > 0)
                                ws.style(tableIndex, 9).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 9).fontColor("00FFFFFF").set();
                            if (countmfe > 0)
                                ws.style(tableIndex, 10).fontColor("FF0000").bold().set(); // red
                            else
                                ws.style(tableIndex, 10).fontColor("00FFFFFF").set();
                            if (countme > 0)
                                ws.style(tableIndex, 11).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 11).fontColor("00FFFFFF").set();
                            if (countfe > 0)
                                ws.style(tableIndex, 12).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 12).fontColor("00FFFFFF").set();
                            if (countpe > 0)
                                ws.style(tableIndex, 13).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 13).fontColor("00FFFFFF").set();
                            if (countle > 0)
                                ws.style(tableIndex, 14).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 14).fontColor("00FFFFFF").set();
                            if (countee > 0)
                                ws.style(tableIndex, 15).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 15).fontColor("00FFFFFF").set();

                            ws.range(tableIndex, 2, tableIndex, 15).style().horizontalAlignment("center")
                                    .set();
                            ws.style(tableIndex, 4).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 10).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 16).borderStyle(BorderSide.LEFT, "thin").set();
                        }
                        case "codes" -> {
                            // Build line in species table for order of codes
                            ws.value(tableIndex, 0, code);
                            ws.value(tableIndex, 1, name_s);
                            ws.value(tableIndex, 2, name_l);
                            ws.value(tableIndex, 3, sectName);
                            ws.value(tableIndex, 4, countmf);
                            ws.value(tableIndex, 5, countm);
                            ws.value(tableIndex, 6, countf);
                            ws.value(tableIndex, 7, countp);
                            ws.value(tableIndex, 8, countl);
                            ws.value(tableIndex, 9, counte);
                            ws.value(tableIndex, 10, countmfe);
                            ws.value(tableIndex, 11, countme);
                            ws.value(tableIndex, 12, countfe);
                            ws.value(tableIndex, 13, countpe);
                            ws.value(tableIndex, 14, countle);
                            ws.value(tableIndex, 15, countee);
                            ws.value(tableIndex, 16, sp_notes);

                            ws.style(tableIndex, 0).horizontalAlignment("center").set();

                            // Conditional species notes, if "0" -> red
                            if (Objects.equals(sp_notes, "0"))
                                ws.style(tableIndex, 16).fontColor("FF0000").set();

                            if (countmf > 0)
                                ws.style(tableIndex, 4).fontColor("008000").bold().set(); // green
                            else
                                ws.style(tableIndex, 4).fontColor("00FFFFFF").set(); // transparent
                            if (countm > 0)
                                ws.style(tableIndex, 5).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 5).fontColor("00FFFFFF").set();
                            if (countf > 0)
                                ws.style(tableIndex, 6).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 6).fontColor("00FFFFFF").set();
                            if (countp > 0)
                                ws.style(tableIndex, 7).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 7).fontColor("00FFFFFF").set();
                            if (countl > 0)
                                ws.style(tableIndex, 8).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 8).fontColor("00FFFFFF").set();
                            if (counte > 0)
                                ws.style(tableIndex, 9).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 9).fontColor("00FFFFFF").set();
                            if (countmfe > 0)
                                ws.style(tableIndex, 10).fontColor("FF0000").bold().set(); // red
                            else
                                ws.style(tableIndex, 10).fontColor("00FFFFFF").set();
                            if (countme > 0)
                                ws.style(tableIndex, 11).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 11).fontColor("00FFFFFF").set();
                            if (countfe > 0)
                                ws.style(tableIndex, 12).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 12).fontColor("00FFFFFF").set();
                            if (countpe > 0)
                                ws.style(tableIndex, 13).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 13).fontColor("00FFFFFF").set();
                            if (countle > 0)
                                ws.style(tableIndex, 14).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 14).fontColor("00FFFFFF").set();
                            if (countee > 0)
                                ws.style(tableIndex, 15).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 15).fontColor("00FFFFFF").set();
                            ws.range(tableIndex, 3, tableIndex, 15).style().horizontalAlignment("center")
                                    .set();
                            ws.style(tableIndex, 4).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 10).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 16).borderStyle(BorderSide.LEFT, "thin").set();
                        }
                        case "sections_names" -> {
                            // Build line in species table for order of sections
                            ws.value(tableIndex, 0, sectName2);
                            ws.value(tableIndex, 1, sectTime);
                            ws.value(tableIndex, 2, name_s);
                            ws.value(tableIndex, 3, name_l);
                            ws.value(tableIndex, 4, code);
                            ws.value(tableIndex, 5, countmf);
                            ws.value(tableIndex, 6, countm);
                            ws.value(tableIndex, 7, countf);
                            ws.value(tableIndex, 8, countp);
                            ws.value(tableIndex, 9, countl);
                            ws.value(tableIndex, 10, counte);
                            ws.value(tableIndex, 11, countmfe);
                            ws.value(tableIndex, 12, countme);
                            ws.value(tableIndex, 13, countfe);
                            ws.value(tableIndex, 14, countpe);
                            ws.value(tableIndex, 15, countle);
                            ws.value(tableIndex, 16, countee);
                            ws.value(tableIndex, 17, sp_notes);

                            // Conditional species notes, if "0" -> red
                            if (Objects.equals(sp_notes, "0"))
                                ws.style(tableIndex, 17).fontColor("FF0000").set();

                            if (countmf > 0)
                                ws.style(tableIndex, 5).fontColor("008000").bold().set(); // green
                            else
                                ws.style(tableIndex, 5).fontColor("00FFFFFF").set(); // gray
                            if (countm > 0)
                                ws.style(tableIndex, 6).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 6).fontColor("00FFFFFF").set();
                            if (countf > 0)
                                ws.style(tableIndex, 7).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 7).fontColor("00FFFFFF").set();
                            if (countp > 0)
                                ws.style(tableIndex, 8).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 8).fontColor("00FFFFFF").set();
                            if (countl > 0)
                                ws.style(tableIndex, 9).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 9).fontColor("00FFFFFF").set();
                            if (counte > 0)
                                ws.style(tableIndex, 10).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 10).fontColor("00FFFFFF").set();
                            if (countmfe > 0)
                                ws.style(tableIndex, 11).fontColor("FF0000").bold().set(); // red
                            else
                                ws.style(tableIndex, 11).fontColor("00FFFFFF").set();
                            if (countme > 0)
                                ws.style(tableIndex, 12).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 12).fontColor("00FFFFFF").set();
                            if (countfe > 0)
                                ws.style(tableIndex, 13).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 13).fontColor("00FFFFFF").set();
                            if (countpe > 0)
                                ws.style(tableIndex, 14).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 14).fontColor("00FFFFFF").set();
                            if (countle > 0)
                                ws.style(tableIndex, 15).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 15).fontColor("00FFFFFF").set();
                            if (countee > 0)
                                ws.style(tableIndex, 16).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 16).fontColor("00FFFFFF").set(); // transparent
                            ws.range(tableIndex, 4, tableIndex, 16).style().horizontalAlignment("center")
                                    .set();
                            ws.style(tableIndex, 2).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 5).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 11).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 17).borderStyle(BorderSide.LEFT, "thin").set();
                        }
                        default -> {
                            // section_codes
                            // Build line in species table for order of species
                            ws.value(tableIndex, 0, sectName2);
                            ws.value(tableIndex, 1, sectTime);
                            ws.value(tableIndex, 2, code);
                            ws.value(tableIndex, 3, name_s);
                            ws.value(tableIndex, 4, name_l);
                            ws.value(tableIndex, 5, countmf);
                            ws.value(tableIndex, 6, countm);
                            ws.value(tableIndex, 7, countf);
                            ws.value(tableIndex, 8, countp);
                            ws.value(tableIndex, 9, countl);
                            ws.value(tableIndex, 10, counte);
                            ws.value(tableIndex, 11, countmfe);
                            ws.value(tableIndex, 12, countme);
                            ws.value(tableIndex, 13, countfe);
                            ws.value(tableIndex, 14, countpe);
                            ws.value(tableIndex, 15, countle);
                            ws.value(tableIndex, 16, countee);
                            ws.value(tableIndex, 17, sp_notes);

                            ws.style(tableIndex, 2).horizontalAlignment("center").set();

                            // Conditional species notes, if "0" -> red
                            if (Objects.equals(sp_notes, "0"))
                                ws.style(tableIndex, 17).fontColor("FF0000").set();

                            if (countmf > 0)
                                ws.style(tableIndex, 5).fontColor("008000").bold().set(); // green
                            else
                                ws.style(tableIndex, 5).fontColor("00FFFFFF").set(); // transparent
                            if (countm > 0)
                                ws.style(tableIndex, 6).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 6).fontColor("00FFFFFF").set();
                            if (countf > 0)
                                ws.style(tableIndex, 7).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 7).fontColor("00FFFFFF").set();
                            if (countp > 0)
                                ws.style(tableIndex, 8).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 8).fontColor("00FFFFFF").set();
                            if (countl > 0)
                                ws.style(tableIndex, 9).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 9).fontColor("00FFFFFF").set();
                            if (counte > 0)
                                ws.style(tableIndex, 10).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 10).fontColor("00FFFFFF").set();
                            if (countmfe > 0)
                                ws.style(tableIndex, 11).fontColor("FF0000").bold().set(); // red
                            else
                                ws.style(tableIndex, 11).fontColor("00FFFFFF").set();
                            if (countme > 0)
                                ws.style(tableIndex, 12).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 12).fontColor("00FFFFFF").set();
                            if (countfe > 0)
                                ws.style(tableIndex, 13).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 13).fontColor("00FFFFFF").set();
                            if (countpe > 0)
                                ws.style(tableIndex, 14).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 14).fontColor("00FFFFFF").set();
                            if (countle > 0)
                                ws.style(tableIndex, 15).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 15).fontColor("00FFFFFF").set();
                            if (countee > 0)
                                ws.style(tableIndex, 16).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 16).fontColor("00FFFFFF").set();
                            ws.range(tableIndex, 5, tableIndex, 16).style().horizontalAlignment("center")
                                    .set();
                            ws.style(tableIndex, 2).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 5).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 11).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 17).borderStyle(BorderSide.LEFT, "thin").set();
                        }
                    }
                } else {
                    switch (outPref) {
                        case "names" -> {
                            // Build line in species table for order of species
                            ws.value(tableIndex, 0, name_s);
                            ws.value(tableIndex, 1, name_l);
                            ws.value(tableIndex, 2, code);
                            ws.value(tableIndex, 3, sectName);
                            ws.value(tableIndex, 4, countmf);
                            ws.value(tableIndex, 5, countm);
                            ws.value(tableIndex, 6, countf);
                            ws.value(tableIndex, 7, countp);
                            ws.value(tableIndex, 8, countl);
                            ws.value(tableIndex, 9, counte);
                            ws.value(tableIndex, 10, countmfe);
                            ws.value(tableIndex, 11, countme);
                            ws.value(tableIndex, 12, countfe);
                            ws.value(tableIndex, 13, countpe);
                            ws.value(tableIndex, 14, countle);
                            ws.value(tableIndex, 15, countee);
                            ws.value(tableIndex, 16, sp_notes);

                            ws.style(tableIndex, 2).horizontalAlignment("center").set();

                            // Conditional species notes, if "0" -> red
                            if (Objects.equals(sp_notes, "0"))
                                ws.style(tableIndex, 16).fontColor("FF0000").set();

                            if (countmf > 0)
                                ws.style(tableIndex, 4).fontColor("008000").bold().set(); // green
                            else
                                ws.style(tableIndex, 4).fontColor("DDDDDD").set(); // gray
                            if (countm > 0)
                                ws.style(tableIndex, 5).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 5).fontColor("DDDDDD").set();
                            if (countf > 0)
                                ws.style(tableIndex, 6).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 6).fontColor("DDDDDD").set();
                            if (countp > 0)
                                ws.style(tableIndex, 7).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 7).fontColor("DDDDDD").set();
                            if (countl > 0)
                                ws.style(tableIndex, 8).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 8).fontColor("DDDDDD").set();
                            if (counte > 0)
                                ws.style(tableIndex, 9).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 9).fontColor("DDDDDD").set();
                            if (countmfe > 0)
                                ws.style(tableIndex, 10).fontColor("FF0000").bold().set(); // red
                            else
                                ws.style(tableIndex, 10).fontColor("DDDDDD").set();
                            if (countme > 0)
                                ws.style(tableIndex, 11).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 11).fontColor("DDDDDD").set();
                            if (countfe > 0)
                                ws.style(tableIndex, 12).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 12).fontColor("DDDDDD").set();
                            if (countpe > 0)
                                ws.style(tableIndex, 13).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 13).fontColor("DDDDDD").set();
                            if (countle > 0)
                                ws.style(tableIndex, 14).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 14).fontColor("DDDDDD").set();
                            if (countee > 0)
                                ws.style(tableIndex, 15).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 15).fontColor("DDDDDD").set();
                            ws.range(tableIndex, 3, tableIndex, 15).style().horizontalAlignment("center")
                                    .set();
                            ws.style(tableIndex, 4).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 10).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 16).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.range(tableIndex, 0, tableIndex, 16).style().fillColor("DDDDDD").set();
                        }
                        case "codes" -> {
                            // Build line in species table for order of codes
                            ws.value(tableIndex, 0, code);
                            ws.value(tableIndex, 1, name_s);
                            ws.value(tableIndex, 2, name_l);
                            ws.value(tableIndex, 3, sectName);
                            ws.value(tableIndex, 4, countmf);
                            ws.value(tableIndex, 5, countm);
                            ws.value(tableIndex, 6, countf);
                            ws.value(tableIndex, 7, countp);
                            ws.value(tableIndex, 8, countl);
                            ws.value(tableIndex, 9, counte);
                            ws.value(tableIndex, 10, countmfe);
                            ws.value(tableIndex, 11, countme);
                            ws.value(tableIndex, 12, countfe);
                            ws.value(tableIndex, 13, countpe);
                            ws.value(tableIndex, 14, countle);
                            ws.value(tableIndex, 15, countee);
                            ws.value(tableIndex, 16, sp_notes);

                            ws.style(tableIndex, 0).horizontalAlignment("center").set();

                            // Conditional species notes, if "0" -> red
                            if (Objects.equals(sp_notes, "0"))
                                ws.style(tableIndex, 16).fontColor("FF0000").set();

                            if (countmf > 0)
                                ws.style(tableIndex, 4).fontColor("008000").bold().set(); // green
                            else
                                ws.style(tableIndex, 4).fontColor("DDDDDD").set(); // transparent
                            if (countm > 0)
                                ws.style(tableIndex, 5).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 5).fontColor("DDDDDD").set();
                            if (countf > 0)
                                ws.style(tableIndex, 6).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 6).fontColor("DDDDDD").set();
                            if (countp > 0)
                                ws.style(tableIndex, 7).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 7).fontColor("DDDDDD").set();
                            if (countl > 0)
                                ws.style(tableIndex, 8).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 8).fontColor("DDDDDD").set();
                            if (counte > 0)
                                ws.style(tableIndex, 9).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 9).fontColor("DDDDDD").set();
                            if (countmfe > 0)
                                ws.style(tableIndex, 10).fontColor("FF0000").bold().set(); // red
                            else
                                ws.style(tableIndex, 10).fontColor("DDDDDD").set();
                            if (countme > 0)
                                ws.style(tableIndex, 11).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 11).fontColor("DDDDDD").set();
                            if (countfe > 0)
                                ws.style(tableIndex, 12).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 12).fontColor("DDDDDD").set();
                            if (countpe > 0)
                                ws.style(tableIndex, 13).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 13).fontColor("DDDDDD").set();
                            if (countle > 0)
                                ws.style(tableIndex, 14).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 14).fontColor("DDDDDD").set();
                            if (countee > 0)
                                ws.style(tableIndex, 15).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 15).fontColor("DDDDDD").set();
                            ws.range(tableIndex, 3, tableIndex, 15).style().horizontalAlignment("center")
                                    .set();
                            ws.style(tableIndex, 4).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 10).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 16).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.range(tableIndex, 0, tableIndex, 16).style().fillColor("DDDDDD").set();
                        }
                        case "sections_names" -> {
                            // Build line in species table for order of sections
                            ws.value(tableIndex, 0, sectName2);
                            ws.value(tableIndex, 1, sectTime);
                            ws.value(tableIndex, 2, name_s);
                            ws.value(tableIndex, 3, name_l);
                            ws.value(tableIndex, 4, code);
                            ws.value(tableIndex, 5, countmf);
                            ws.value(tableIndex, 6, countm);
                            ws.value(tableIndex, 7, countf);
                            ws.value(tableIndex, 8, countp);
                            ws.value(tableIndex, 9, countl);
                            ws.value(tableIndex, 10, counte);
                            ws.value(tableIndex, 11, countmfe);
                            ws.value(tableIndex, 12, countme);
                            ws.value(tableIndex, 13, countfe);
                            ws.value(tableIndex, 14, countpe);
                            ws.value(tableIndex, 15, countle);
                            ws.value(tableIndex, 16, countee);
                            ws.value(tableIndex, 17, sp_notes);

                            ws.style(tableIndex, 4).horizontalAlignment("center").set();

                            // Conditional species notes, if "0" -> red
                            if (Objects.equals(sp_notes, "0"))
                                ws.style(tableIndex, 17).fontColor("FF0000").set();

                            if (countmf > 0)
                                ws.style(tableIndex, 5).fontColor("008000").bold().set(); // green
                            else
                                ws.style(tableIndex, 5).fontColor("DDDDDD").set(); // gray
                            if (countm > 0)
                                ws.style(tableIndex, 6).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 6).fontColor("DDDDDD").set();
                            if (countf > 0)
                                ws.style(tableIndex, 7).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 7).fontColor("DDDDDD").set();
                            if (countp > 0)
                                ws.style(tableIndex, 8).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 8).fontColor("DDDDDD").set();
                            if (countl > 0)
                                ws.style(tableIndex, 9).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 9).fontColor("DDDDDD").set();
                            if (counte > 0)
                                ws.style(tableIndex, 10).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 10).fontColor("DDDDDD").set();
                            if (countmfe > 0)
                                ws.style(tableIndex, 11).fontColor("FF0000").bold().set(); // red
                            else
                                ws.style(tableIndex, 11).fontColor("DDDDDD").set();
                            if (countme > 0)
                                ws.style(tableIndex, 12).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 12).fontColor("DDDDDD").set();
                            if (countfe > 0)
                                ws.style(tableIndex, 13).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 13).fontColor("DDDDDD").set();
                            if (countpe > 0)
                                ws.style(tableIndex, 14).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 14).fontColor("DDDDDD").set();
                            if (countle > 0)
                                ws.style(tableIndex, 15).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 15).fontColor("DDDDDD").set();
                            if (countee > 0)
                                ws.style(tableIndex, 16).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 16).fontColor("DDDDDD").set();
                            ws.range(tableIndex, 5, tableIndex, 16).style().horizontalAlignment("center")
                                    .set();
                            ws.style(tableIndex, 2).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 5).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 11).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 17).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.range(tableIndex, 0, tableIndex, 17).style().fillColor("DDDDDD").set();
                        }
                        default -> {
                            // section_codes
                            // Build line in species table for order of species
                            ws.value(tableIndex, 0, sectName2);
                            ws.value(tableIndex, 1, sectTime);
                            ws.value(tableIndex, 2, code);
                            ws.value(tableIndex, 3, name_s);
                            ws.value(tableIndex, 4, name_l);
                            ws.value(tableIndex, 5, countmf);
                            ws.value(tableIndex, 6, countm);
                            ws.value(tableIndex, 7, countf);
                            ws.value(tableIndex, 8, countp);
                            ws.value(tableIndex, 9, countl);
                            ws.value(tableIndex, 10, counte);
                            ws.value(tableIndex, 11, countmfe);
                            ws.value(tableIndex, 12, countme);
                            ws.value(tableIndex, 13, countfe);
                            ws.value(tableIndex, 14, countpe);
                            ws.value(tableIndex, 15, countle);
                            ws.value(tableIndex, 16, countee);
                            ws.value(tableIndex, 17, sp_notes);

                            ws.style(tableIndex, 2).horizontalAlignment("center").set();

                            // Conditional species notes, if "0" -> red
                            if (Objects.equals(sp_notes, "0"))
                                ws.style(tableIndex, 17).fontColor("FF0000").set();

                            if (countmf > 0)
                                ws.style(tableIndex, 5).fontColor("008000").bold().set(); // green
                            else
                                ws.style(tableIndex, 5).fontColor("DDDDDD").set(); // gray
                            if (countm > 0)
                                ws.style(tableIndex, 6).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 6).fontColor("DDDDDD").set();
                            if (countf > 0)
                                ws.style(tableIndex, 7).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 7).fontColor("DDDDDD").set();
                            if (countp > 0)
                                ws.style(tableIndex, 8).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 8).fontColor("DDDDDD").set();
                            if (countl > 0)
                                ws.style(tableIndex, 9).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 9).fontColor("DDDDDD").set();
                            if (counte > 0)
                                ws.style(tableIndex, 10).fontColor("008000").bold().set();
                            else
                                ws.style(tableIndex, 10).fontColor("DDDDDD").set();
                            if (countmfe > 0)
                                ws.style(tableIndex, 11).fontColor("FF0000").bold().set(); // red
                            else
                                ws.style(tableIndex, 11).fontColor("DDDDDD").set();
                            if (countme > 0)
                                ws.style(tableIndex, 12).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 12).fontColor("DDDDDD").set();
                            if (countfe > 0)
                                ws.style(tableIndex, 13).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 13).fontColor("DDDDDD").set();
                            if (countpe > 0)
                                ws.style(tableIndex, 14).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 14).fontColor("DDDDDD").set();
                            if (countle > 0)
                                ws.style(tableIndex, 15).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 15).fontColor("DDDDDD").set();
                            if (countee > 0)
                                ws.style(tableIndex, 16).fontColor("FF0000").bold().set();
                            else
                                ws.style(tableIndex, 16).fontColor("DDDDDD").set();
                            ws.range(tableIndex, 5, tableIndex, 16).style().horizontalAlignment("center")
                                    .set();
                            ws.style(tableIndex, 2).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 5).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 11).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.style(tableIndex, 17).borderStyle(BorderSide.LEFT, "thin").set();
                            ws.range(tableIndex, 0, tableIndex, 17).style().fillColor("DDDDDD").set();
                        }
                    }
                }

                summf = summf + curXLSX.getInt(4);
                summ = summ + curXLSX.getInt(5);
                sumf = sumf + curXLSX.getInt(6);
                sump = sump + curXLSX.getInt(7);
                suml = suml + curXLSX.getInt(8);
                sumo = sumo + curXLSX.getInt(9);
                summfe = summfe + curXLSX.getInt(10);
                summe = summe + curXLSX.getInt(11);
                sumfe = sumfe + curXLSX.getInt(12);
                sumpe = sumpe + curXLSX.getInt(13);
                sumle = sumle + curXLSX.getInt(14);
                sumoe = sumoe + curXLSX.getInt(15);
                curXLSX.moveToNext();

                tableIndex++;
            }
            curXLSX.close();

            tableIndex++; // Empty row

            totali = summf + summ + sumf + sump + suml + sumo;
            totale = summfe + summe + sumfe + sumpe + sumle + sumoe;
            total = totali + totale;

            // Headline 1 for totals of Internal, External counts
            switch (outPref) {
                case "names" -> {
                    ws.value(tableIndex, 4, getString(R.string.internal));
                    ws.value(tableIndex, 10, getString(R.string.external));
                    ws.style(tableIndex, 4).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(tableIndex, 10).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(tableIndex, 16).borderStyle(BorderSide.LEFT, "thin").bold().set();
                }
                case "codes" -> {
                    ws.value(tableIndex, 4, getString(R.string.internal));
                    ws.value(tableIndex, 10, getString(R.string.external));
                    ws.style(tableIndex, 4).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(tableIndex, 10).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(tableIndex, 16).borderStyle(BorderSide.LEFT, "thin").bold().set();
                }
                case "sections_names" -> {
                    ws.value(tableIndex, 5, getString(R.string.internal));
                    ws.value(tableIndex, 11, getString(R.string.external));
                    ws.style(tableIndex, 5).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(tableIndex, 11).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(tableIndex, 17).borderStyle(BorderSide.LEFT, "thin").bold().set();
                }
                default -> {
                    ws.value(tableIndex, 5, getString(R.string.internal));
                    ws.value(tableIndex, 11, getString(R.string.external));
                    ws.style(tableIndex, 5).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(tableIndex, 11).borderStyle(BorderSide.LEFT, "thin").bold().set();
                    ws.style(tableIndex, 17).borderStyle(BorderSide.LEFT, "thin").bold().set();
                }
            }

            // Headline 2 for totals of Internal, External counts
            switch (outPref) {
                case "names", "codes" -> {
                    ws.value(tableIndex + 1, 1, getString(R.string.sumSpec));
                    ws.value(tableIndex + 1, 4, getString(R.string.countImagomfHint));
                    ws.value(tableIndex + 1, 5, getString(R.string.countImagomHint));
                    ws.value(tableIndex + 1, 6, getString(R.string.countImagofHint));
                    ws.value(tableIndex + 1, 7, getString(R.string.countPupaHint));
                    ws.value(tableIndex + 1, 8, getString(R.string.countLarvaHint));
                    ws.value(tableIndex + 1, 9, getString(R.string.countOvoHint));
                    ws.value(tableIndex + 1, 10, getString(R.string.countImagomfHint));
                    ws.value(tableIndex + 1, 11, getString(R.string.countImagomHint));
                    ws.value(tableIndex + 1, 12, getString(R.string.countImagofHint));
                    ws.value(tableIndex + 1, 13, getString(R.string.countPupaHint));
                    ws.value(tableIndex + 1, 14, getString(R.string.countLarvaHint));
                    ws.value(tableIndex + 1, 15, getString(R.string.countOvoHint));
                    ws.value(tableIndex + 1, 16, getString(R.string.hintTotal));
                    ws.range(tableIndex + 1, 1, tableIndex + 1, 16)
                            .style().horizontalAlignment("center").bold()
                            .borderStyle(BorderSide.BOTTOM, "thin").set();
                    ws.style(tableIndex + 1, 4).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").set();
                    ws.style(tableIndex + 1, 10).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").set();
                    ws.style(tableIndex + 1, 16).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").set();
                }
                default -> {
                    ws.value(tableIndex + 1, 2, getString(R.string.sumSpec));
                    ws.value(tableIndex + 1, 5, getString(R.string.countImagomfHint));
                    ws.value(tableIndex + 1, 6, getString(R.string.countImagomHint));
                    ws.value(tableIndex + 1, 7, getString(R.string.countImagofHint));
                    ws.value(tableIndex + 1, 8, getString(R.string.countPupaHint));
                    ws.value(tableIndex + 1, 9, getString(R.string.countLarvaHint));
                    ws.value(tableIndex + 1, 10, getString(R.string.countOvoHint));
                    ws.value(tableIndex + 1, 11, getString(R.string.countImagomfHint));
                    ws.value(tableIndex + 1, 12, getString(R.string.countImagomHint));
                    ws.value(tableIndex + 1, 13, getString(R.string.countImagofHint));
                    ws.value(tableIndex + 1, 14, getString(R.string.countPupaHint));
                    ws.value(tableIndex + 1, 15, getString(R.string.countLarvaHint));
                    ws.value(tableIndex + 1, 16, getString(R.string.countOvoHint));
                    ws.value(tableIndex + 1, 17, getString(R.string.hintTotal));
                    ws.range(tableIndex + 1, 2, tableIndex + 1, 17)
                            .style().horizontalAlignment("center").bold()
                            .borderStyle(BorderSide.BOTTOM, "thin").set();
                    ws.range(tableIndex + 1, 2, tableIndex + 1, 17).style()
                            .borderStyle(BorderSide.BOTTOM, "thin").set();
                    ws.style(tableIndex + 1, 5).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").set();
                    ws.style(tableIndex + 1, 11).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").set();
                    ws.style(tableIndex + 1, 17).borderStyle(BorderSide.LEFT, "thin")
                            .borderStyle(BorderSide.BOTTOM, "thin").set();
                }
            }

            // Write internal total sums
            switch (outPref) {
                case "names", "codes" -> {
                    ws.value(tableIndex + 2, 1, sumSpec);
                    ws.value(tableIndex + 2, 3, getString(R.string.sumi));
                    ws.value(tableIndex + 2, 4, summf);
                    ws.value(tableIndex + 2, 5, summ);
                    ws.value(tableIndex + 2, 6, sumf);
                    ws.value(tableIndex + 2, 7, sump);
                    ws.value(tableIndex + 2, 8, suml);
                    ws.value(tableIndex + 2, 9, sumo);
                    ws.value(tableIndex + 2, 16, totali);
                    if (summf > 0)
                        ws.style(tableIndex + 2, 4).fontColor("008000").bold().set(); // green
                    else
                        ws.style(tableIndex + 2, 4).fontColor("00FFFFFF").set(); // transparent
                    if (summ > 0)
                        ws.style(tableIndex + 2, 5).fontColor("008000").bold().set();
                    else
                        ws.style(tableIndex + 2, 5).fontColor("00FFFFFF").set();
                    if (sumf > 0)
                        ws.style(tableIndex + 2, 6).fontColor("008000").bold().set();
                    else
                        ws.style(tableIndex + 2, 6).fontColor("00FFFFFF").set();
                    if (sump > 0)
                        ws.style(tableIndex + 2, 7).fontColor("008000").bold().set();
                    else
                        ws.style(tableIndex + 2, 7).fontColor("00FFFFFF").set();
                    if (suml > 0)
                        ws.style(tableIndex + 2, 8).fontColor("008000").bold().set();
                    else
                        ws.style(tableIndex + 2, 8).fontColor("00FFFFFF").set();
                    if (sumo > 0)
                        ws.style(tableIndex + 2, 9).fontColor("008000").bold().set();
                    else
                        ws.style(tableIndex + 2, 9).fontColor("00FFFFFF").set();
                    ws.style(tableIndex + 2, 1).horizontalAlignment("center").bold().set();
                    ws.style(tableIndex + 2, 3).horizontalAlignment("right").bold().set();
                    ws.range(tableIndex + 2, 4, tableIndex + 2, 16).style()
                            .horizontalAlignment("center").set();
                    ws.style(tableIndex + 2, 4).borderStyle(BorderSide.LEFT, "thin").set();
                    ws.style(tableIndex + 2, 10).borderStyle(BorderSide.LEFT, "thin").set();
                    ws.style(tableIndex + 2, 16).bold().borderStyle(BorderSide.LEFT, "thin").set();
                }
                default -> {
                    ws.value(tableIndex + 2, 2, sumSpec);
                    ws.value(tableIndex + 2, 4, getString(R.string.sumi));
                    ws.value(tableIndex + 2, 5, summf);
                    ws.value(tableIndex + 2, 6, summ);
                    ws.value(tableIndex + 2, 7, sumf);
                    ws.value(tableIndex + 2, 8, sump);
                    ws.value(tableIndex + 2, 9, suml);
                    ws.value(tableIndex + 2, 10, sumo);
                    ws.value(tableIndex + 2, 17, totali);
                    if (summf > 0)
                        ws.style(tableIndex + 2, 5).fontColor("008000").bold().set(); // green
                    else
                        ws.style(tableIndex + 2, 5).fontColor("00FFFFFF").set(); // transparent
                    if (summ > 0)
                        ws.style(tableIndex + 2, 6).fontColor("008000").bold().set();
                    else
                        ws.style(tableIndex + 2, 6).fontColor("00FFFFFF").set();
                    if (sumf > 0)
                        ws.style(tableIndex + 2, 7).fontColor("008000").bold().set();
                    else
                        ws.style(tableIndex + 2, 7).fontColor("00FFFFFF").set();
                    if (sump > 0)
                        ws.style(tableIndex + 2, 8).fontColor("008000").bold().set();
                    else
                        ws.style(tableIndex + 2, 8).fontColor("00FFFFFF").set();
                    if (suml > 0)
                        ws.style(tableIndex + 2, 9).fontColor("008000").bold().set();
                    else
                        ws.style(tableIndex + 2, 9).fontColor("00FFFFFF").set();
                    if (sumo > 0)
                        ws.style(tableIndex + 2, 10).fontColor("008000").bold().set();
                    else
                        ws.style(tableIndex + 2, 10).fontColor("00FFFFFF").set();
                    ws.style(tableIndex + 2, 2).horizontalAlignment("center").bold().set();
                    ws.style(tableIndex + 2, 4).horizontalAlignment("right").bold().set();
                    ws.range(tableIndex + 2, 5, tableIndex + 2, 17).style()
                            .horizontalAlignment("center").set();
                    ws.style(tableIndex + 2, 5).borderStyle(BorderSide.LEFT, "thin").set();
                    ws.style(tableIndex + 2, 11).borderStyle(BorderSide.LEFT, "thin").set();
                    ws.style(tableIndex + 2, 17).bold().borderStyle(BorderSide.LEFT, "thin").set();
                }
            }

            // Write external total sums
            switch (outPref) {
                case "names", "codes" -> {
                    ws.value(tableIndex + 3, 3, getString(R.string.sume));
                    ws.value(tableIndex + 3, 10, summfe);
                    ws.value(tableIndex + 3, 11, summe);
                    ws.value(tableIndex + 3, 12, sumfe);
                    ws.value(tableIndex + 3, 13, sumpe);
                    ws.value(tableIndex + 3, 14, sumle);
                    ws.value(tableIndex + 3, 15, sumoe);
                    ws.value(tableIndex + 3, 16, totale);
                    ws.style(tableIndex + 3, 3).horizontalAlignment("right").bold().set();
                    if (summfe > 0)
                        ws.style(tableIndex + 3, 10).fontColor("FF0000").bold().set(); // rot
                    else
                        ws.style(tableIndex + 3, 10).fontColor("00FFFFFF").set(); // transparent
                    if (summe > 0)
                        ws.style(tableIndex + 3, 11).fontColor("FF0000").bold().set();
                    else
                        ws.style(tableIndex + 3, 11).fontColor("00FFFFFF").set();
                    if (sumfe > 0)
                        ws.style(tableIndex + 3, 12).fontColor("FF0000").bold().set();
                    else
                        ws.style(tableIndex + 3, 12).fontColor("00FFFFFF").set();
                    if (sumpe > 0)
                        ws.style(tableIndex + 3, 13).fontColor("FF0000").bold().set();
                    else
                        ws.style(tableIndex + 3, 13).fontColor("00FFFFFF").set();
                    if (sumle > 0)
                        ws.style(tableIndex + 3, 14).fontColor("FF0000").bold().set();
                    else
                        ws.style(tableIndex + 3, 14).fontColor("00FFFFFF").set();
                    if (sumoe > 0)
                        ws.style(tableIndex + 3, 15).fontColor("FF0000").bold().set();
                    else
                        ws.style(tableIndex + 3, 15).fontColor("00FFFFFF").set();
                    ws.range(tableIndex + 3, 4, tableIndex + 2, 16).style()
                            .horizontalAlignment("center").set();
                    ws.style(tableIndex + 3, 4).borderStyle(BorderSide.LEFT, "thin").set();
                    ws.style(tableIndex + 3, 10).borderStyle(BorderSide.LEFT, "thin").set();
                    ws.style(tableIndex + 3, 16).bold().borderStyle(BorderSide.LEFT, "thin").set();
                }
                default -> {
                    ws.value(tableIndex + 3, 4, getString(R.string.sume));
                    ws.value(tableIndex + 3, 11, summfe);
                    ws.value(tableIndex + 3, 12, summe);
                    ws.value(tableIndex + 3, 13, sumfe);
                    ws.value(tableIndex + 3, 14, sumpe);
                    ws.value(tableIndex + 3, 15, sumle);
                    ws.value(tableIndex + 3, 16, sumoe);
                    ws.value(tableIndex + 3, 17, totale);
                    ws.style(tableIndex + 3, 4).horizontalAlignment("right").bold().set();
                    if (summfe > 0)
                        ws.style(tableIndex + 3, 11).fontColor("FF0000").bold().set(); // rot
                    else
                        ws.style(tableIndex + 3, 11).fontColor("00FFFFFF").set(); // transparent
                    if (summe > 0)
                        ws.style(tableIndex + 3, 12).fontColor("FF0000").bold().set();
                    else
                        ws.style(tableIndex + 3, 12).fontColor("00FFFFFF").set();
                    if (sumfe > 0)
                        ws.style(tableIndex + 3, 13).fontColor("FF0000").bold().set();
                    else
                        ws.style(tableIndex + 3, 13).fontColor("00FFFFFF").set();
                    if (sumpe > 0)
                        ws.style(tableIndex + 3, 14).fontColor("FF0000").bold().set();
                    else
                        ws.style(tableIndex + 3, 14).fontColor("00FFFFFF").set();
                    if (sumle > 0)
                        ws.style(tableIndex + 3, 15).fontColor("FF0000").bold().set();
                    else
                        ws.style(tableIndex + 3, 15).fontColor("00FFFFFF").set();
                    if (sumoe > 0)
                        ws.style(tableIndex + 3, 16).fontColor("FF0000").bold().set();
                    else
                        ws.style(tableIndex + 3, 16).fontColor("00FFFFFF").set();
                    ws.range(tableIndex + 3, 5, tableIndex + 2, 17).style()
                            .horizontalAlignment("center").set();
                    ws.style(tableIndex + 3, 5).borderStyle(BorderSide.LEFT, "thin").set();
                    ws.style(tableIndex + 3, 11).borderStyle(BorderSide.LEFT, "thin").set();
                    ws.style(tableIndex + 3, 17).bold().borderStyle(BorderSide.LEFT, "thin").set();
                }
            }

            // Write overall total sum
            switch (outPref) {
                case "names", "codes" -> {
                    ws.value(tableIndex + 4, 3, getString(R.string.sum_total));
                    ws.value(tableIndex + 4, 16, total);
                    ws.style(tableIndex + 4, 3).horizontalAlignment("right").bold().set();
                    ws.style(tableIndex + 4, 16).horizontalAlignment("center").set();
                    ws.style(tableIndex + 4, 4).borderStyle(BorderSide.LEFT, "thin").set();
                    ws.style(tableIndex + 4, 10).borderStyle(BorderSide.LEFT, "thin").set();
                    ws.style(tableIndex + 4, 16).bold().borderStyle(BorderSide.LEFT, "thin").set();
                }
                default -> {
                    ws.value(tableIndex + 4, 4, getString(R.string.sum_total));
                    ws.value(tableIndex + 4, 17, total);
                    ws.style(tableIndex + 4, 4).horizontalAlignment("right").bold().set();
                    ws.style(tableIndex + 4, 17).horizontalAlignment("center").set();
                    ws.style(tableIndex + 4, 5).borderStyle(BorderSide.LEFT, "thin").set();
                    ws.style(tableIndex + 4, 11).borderStyle(BorderSide.LEFT, "thin").set();
                    ws.style(tableIndex + 4, 17).bold().borderStyle(BorderSide.LEFT, "thin").set();
                }
            }

            try {
                // Export purged db as xlsx
                wb.finish();
                outputStream.close();
                wb.close();
            } catch (Exception e) {
                mesg = getString(R.string.saveFail);
                Toast.makeText(this,
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
            }
            dbHelper.close();
        }
    }
    // End of exportDb2XLSX()

    /**********************************************************************************************/
    // Export current species list to both data directories
    //  /Documents/TourCount/species_ll_Transect_YYYYMMDD_hhmmss.csv and
    //  /Documents/TransektCount/species_ll_Transect_YYYYMMDD_hhmmss.csv
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void exportSpeciesList() {
        // outFileTour -> /storage/emulated/0/Documents/TourCount/species_ll_Transekt_transNo_yyyyMMdd_HHmmss.csv
        // outFileTransect -> /storage/emulated/0/Documents/TransektCount/species_ll_Transekt_transNo_yyyyMMdd_HHmmss.csv
        File pathTour, outFileTour, pathTransect, outFileTransect;
        pathTour = new File(Environment.getExternalStorageDirectory() + "/Documents/TourCount");
        pathTransect = new File(Environment.getExternalStorageDirectory() + "/Documents/TransektCount");

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED.equals(sState);

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
        } else {
            // Export species list into species_ll_yyyy-MM-dd_HHmmss.csv
            dataLanguage = prefs.getString("pref_sel_data_lang", "");
            if (dataLanguage.isEmpty())
                dataLanguage = "--";

            String[] codeArray;
            String[] nameArray;
            String[] nameArrayL;

            codeArray = countDataSource.getAllStringsForSectionSrtCode(1, "code");
            nameArray = countDataSource.getAllStringsForSectionSrtCode(1, "name");
            nameArrayL = countDataSource.getAllStringsForSectionSrtCode(1, "name_g");

            int specNum = codeArray.length;

            pathTransect.mkdirs(); // Just verify pathTransect, result ignored
            pathTour.mkdirs(); // Just verify pathTour, result ignored

            switch (dataLanguage) {
                case "de" -> {
                    if (Objects.equals(transNoDir, "")) {
                        outFileTour = new File(pathTour, "/species_de_Transekt_"
                                + getcurDate() + ".csv");
                        outFileTransect = new File(pathTransect, "/species_de_Transekt_"
                                + getcurDate() + ".csv");
                    } else {
                        outFileTour = new File(pathTour, "/species_de_Transekt_"
                                + transNoDir + "_" + getcurDate() + ".csv");
                        outFileTransect = new File(pathTransect, "/species_de_Transekt_"
                                + transNoDir + "_" + getcurDate() + ".csv");
                    }
                }
                case "en" -> {
                    if (Objects.equals(transNoDir, "")) {
                        outFileTour = new File(pathTour, "/species_en_Transect_"
                                + getcurDate() + ".csv");
                        outFileTransect = new File(pathTransect, "/species_en_Transect_"
                                + getcurDate() + ".csv");
                    } else {
                        outFileTour = new File(pathTour, "/species_en_Transect_"
                                + transNoDir + "_" + getcurDate() + ".csv");
                        outFileTransect = new File(pathTransect, "/species_en_Transect_"
                                + transNoDir + "_" + getcurDate() + ".csv");
                    }
                }
                case "fr" -> {
                    if (Objects.equals(transNoDir, "")) {
                        outFileTour = new File(pathTour, "/species_fr_Transect_"
                                + getcurDate() + ".csv");
                        outFileTransect = new File(pathTransect, "/species_fr_Transect_"
                                + getcurDate() + ".csv");
                    } else {
                        outFileTour = new File(pathTour, "/species_fr_Transect_"
                                + transNoDir + "_" + getcurDate() + ".csv");
                        outFileTransect = new File(pathTransect, "/species_fr_Transect_"
                                + transNoDir + "_" + getcurDate() + ".csv");
                    }
                }
                case "it" -> {
                    if (Objects.equals(transNoDir, "")) {
                        outFileTour = new File(pathTour, "/species_it_Transetto_"
                                + getcurDate() + ".csv");
                        outFileTransect = new File(pathTransect, "/species_it_Transetto_"
                                + getcurDate() + ".csv");
                    } else {
                        outFileTour = new File(pathTour, "/species_it_Transetto_"
                                + transNoDir + "_" + getcurDate() + ".csv");
                        outFileTransect = new File(pathTransect, "/species_it_Transetto_"
                                + transNoDir + "_" + getcurDate() + ".csv");
                    }
                }
                case "es" -> {
                    if (Objects.equals(transNoDir, "")) {
                        outFileTour = new File(pathTour, "/species_es_Transecto_"
                                + getcurDate() + ".csv");
                        outFileTransect = new File(pathTransect, "/species_es_Transecto_"
                                + getcurDate() + ".csv");
                    } else {
                        outFileTour = new File(pathTour, "/species_es_Transecto_"
                                + transNoDir + "_" + getcurDate() + ".csv");
                        outFileTransect = new File(pathTransect, "/species_es_Transecto_"
                                + transNoDir + "_" + getcurDate() + ".csv");
                    }
                }
                default -> {
                    // No data language given
                    if (Objects.equals(transNoDir, "")) {
                        outFileTour = new File(pathTour, "/species_--_Transekt_"
                                + getcurDate() + ".csv");
                        outFileTransect = new File(pathTransect, "/species_--_Transekt_"
                                + getcurDate() + ".csv");
                    } else {
                        outFileTour = new File(pathTour, "/species_--_Transekt_"
                                + transNoDir + "_" + getcurDate() + ".csv");
                        outFileTransect = new File(pathTransect, "/species_--_Transekt_"
                                + transNoDir + "_" + getcurDate() + ".csv");
                    }
                }
            }

            // If TourCount is installed export to /Documents/TourCount
            if (pathTour.exists() && pathTour.isDirectory()) {
                try {
                    CSVWriter csvWrite = new CSVWriter(new FileWriter(outFileTour));

                    // 1. line with nocode, language, de|en|fr|it|es
                    String[] specLine1 = {"nocode,language," + dataLanguage};
                    csvWrite.writeNext(specLine1);

                    int i = 0;
                    while (i < specNum) {
                        String[] specLine =
                                {
                                        codeArray[i],
                                        nameArray[i],
                                        nameArrayL[i]
                                };
                        i++;
                        csvWrite.writeNext(specLine);
                    }
                    csvWrite.close();
                } catch (Exception e) {
                    mesg = getString(R.string.saveFailList);
                    Toast.makeText(this,
                            fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                            Toast.LENGTH_LONG).show();
                }
            }

            // Export to /Documents/TransektCount
            if (pathTransect.exists() && pathTransect.isDirectory()) {
                try {
                    CSVWriter csvWrite = new CSVWriter(new FileWriter(outFileTransect));

                    // 1. line with nocode, language, de|en|fr|it|es
                    String[] specLine1 = {"nocode,language," + dataLanguage};
                    csvWrite.writeNext(specLine1);

                    int i = 0;
                    while (i < specNum) {
                        String[] specLine =
                                {
                                        codeArray[i],
                                        nameArray[i],
                                        nameArrayL[i]
                                };
                        i++;
                        csvWrite.writeNext(specLine);
                    }
                    csvWrite.close();

                    mesg = getString(R.string.saveList);
                    Toast.makeText(this,
                            fromHtml("<font color='blue'>" + mesg + "</font>"),
                            Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    mesg = getString(R.string.saveFailList);
                    Toast.makeText(this,
                            fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    // End of exportSpeciesList()

    /**********************************************************************************************/
    // Copy file block-wise
    private static void copy(File src, File dst) throws IOException {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }

        in.close();
        out.close();
    }

    /**********************************************************************************************/
    // Clear all relevant DB values, reset to basic DB
    private void resetToBasisDb() {
        // Confirm dialogue before anything else takes place
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmResetDB);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.deleteButton, (dialog, id) ->
        {
            boolean r_ok = clearDBValues();
            if (r_ok) {
                mesg = getString(R.string.reset2basic);
                Toast.makeText(this, // bright green
                        fromHtml("<font color='#008000'>" + mesg + "</font>"),
                        Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
        alert = builder.create();
        alert.show();
    }

    // Clear DB values for basic DB
    @SuppressLint({"LongLogTag"})
    private boolean clearDBValues() {
        // Clear values in DB
        dbHelper = new DbHelper(this);
        database = dbHelper.getWritableDatabase();
        boolean r_ok = true; // Gets false when reset fails

        try {
            String sql = "UPDATE " + DbHelper.COUNT_TABLE + " SET "
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
                    + DbHelper.C_NOTES + " = '';";
            database.execSQL(sql);

            sql = "UPDATE " + DbHelper.SECTION_TABLE + " SET "
                    + DbHelper.S_CREATED_AT + " = '', "
                    + DbHelper.S_NOTES + " = '';";
            database.execSQL(sql);

            sql = "UPDATE " + DbHelper.META_TABLE + " SET "
                    + DbHelper.M_TEMPS + " = 0, "
                    + DbHelper.M_TEMPE + " = 0, "
                    + DbHelper.M_WINDS + " = 0, "
                    + DbHelper.M_WINDE + " = 0, "
                    + DbHelper.M_CLOUDS + " = 0, "
                    + DbHelper.M_CLOUDE + " = 0, "
                    + DbHelper.M_DATE + " = '', "
                    + DbHelper.M_START_TM + " = '', "
                    + DbHelper.M_END_TM + " = '', "
                    + DbHelper.M_NOTE + " = '';";
            database.execSQL(sql);
        } catch (Exception e) {
            mesg = getString(R.string.resetFail);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
            r_ok = false;
        }
        dbHelper.close();

        lat = 0.0;
        lon = 0.0;
        isFirstLoc = true;
        locServiceOn = false;
        isSelectSectionActivityResumed = false;
        sectionIdGPS = 0;
        sectionNameCurrent = "";

        return r_ok;
    }
    // End of resetToBasisDb()

}
