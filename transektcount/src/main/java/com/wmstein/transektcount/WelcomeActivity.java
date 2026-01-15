package com.wmstein.transektcount;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;

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
import com.wmstein.transektcount.database.AlertDataSource;
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

import static com.wmstein.transektcount.TransektCountApplication.lat;
import static com.wmstein.transektcount.TransektCountApplication.lon;
import static com.wmstein.transektcount.TransektCountApplication.distMin;
import static com.wmstein.transektcount.TransektCountApplication.locServiceOn;
import static com.wmstein.transektcount.TransektCountApplication.sectionIdGPS;

/**********************************************************************
 * WelcomeActivity provides the starting page with menu and buttons for
 * import/export/help/info methods and lets you call
 * EditMetaActivity, SelectSectionActivity and ShowResultsActivity.
 * It uses further LocationService and the PermissionDialogFragments.
 * <p>
 * Database handling is mainly done in WelcomeActivity as upgrade to current
 * DB version when importing an older DB file by importDBFile().
 * <p>
 * Based on BeeCount's WelcomeActivity.java by Milo Thurston from 2014-05-05.
 * Changes and additions for TransektCount by wmstein since 2016-02-18,
 * last edited on 2026-01-15
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
    private boolean mExternalStorageWriteable = false;
    private final String sState = Environment.getExternalStorageState();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private View baseLayout;
    private AlertDialog alert;
    private String transNo = "";
    private String mesg;
    private int secCount; // number of sections

    // Preferences
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private String outPref;
    private boolean autoSection = false; // true for enabled GPS track selection
    private boolean transectHasTrack = false; // transect sections have tracks

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
    private CountDataSource countDataSource;
    private AlertDataSource alertDataSource;
    private TrackDataSource trackDataSource;

    // Track handling
    private int trCount = 0; // number of tracks
    private List<Track> trackPts; // list of all transect track points
    private LocationService locationService;
    private String sectionNameGPS; // track section name from track table
    private TextView welcomeTitle;

    @SuppressLint({"SourceLockedOrientationActivity", "ApplySharedPref"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "163, onCreate");

        transektCount = (TransektCountApplication) getApplication();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Get preferences
        prefs = TransektCountApplication.getPrefs();

        // Proximity sensor handling in preferences menu
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        // Grey out preferences menu item pref_prox when max. proximity sensitivity = null
        boolean prefProx = proximitySensor != null;

        // Grey out preferences menu item pref_button_vib when device has no vibrator
        Vibrator vibrator = getApplicationContext().getSystemService(Vibrator.class);
        boolean prefVib = vibrator.hasVibrator();

        // Set pref_prox enabler, used in SettingsFragment
        editor = prefs.edit();
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

        transectHasTrack = prefs.getBoolean("transect_has_track", false);
        autoSection = prefs.getBoolean("pref_auto_section", false);

        if (!transectHasTrack && autoSection) {
            autoSection = false;
            editor.putBoolean("pref_auto_section", false);
            editor.commit();
        }

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "232, onCreate, autoSection: " + autoSection
                    + ", Transect has track: " + transectHasTrack);

        // Check and ask storage permission
        storagePermGranted = isStoragePermGranted();
        if (!storagePermGranted) // in self permission
        {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "240, onCreate, StoragePermDialog");

            PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                    PermissionsStorageDialogFragment.class.getName());
        }

        storagePermGranted = isStoragePermGranted();
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "248, onCreate, storageGranted: " + storagePermGranted);

        // Check DB version and upgrade if necessary
        dbHelper = new DbHelper(this);
        database = dbHelper.getWritableDatabase();
        dbHelper.close();

        // Setup the data sources
        headDataSource = new HeadDataSource(this);
        sectionDataSource = new SectionDataSource(this);
        metaDataSource = new MetaDataSource(this);
        countDataSource = new CountDataSource(this);
        alertDataSource = new AlertDataSource(this);
        trackDataSource = new TrackDataSource(this);

        // Get transect No. and check for DB integrity
        try {
            headDataSource.open();
            head = headDataSource.getHead();
            transNo = head.transect_no; // just read to test for DB integrity
            headDataSource.close();
        } catch (SQLiteException e) {
            headDataSource.close();
            mesg = getString(R.string.corruptDb);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
            mHandler.postDelayed(this::finishAndRemoveTask, 2000);
        }

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
            Log.d(TAG, "298, onCreate, TrkPts: " + trackPts.size()
                    + ", trCount: " + trCount + ", secCount: " + secCount);

        // Check if tracks correspond to sections
        if (trCount > 0 && trCount != secCount) {
            autoSection = false;

            editor.putBoolean("pref_auto_section", false);
            editor.commit();
            mesg = getString(R.string.track_err);
            Toast.makeText(getApplicationContext(),
                    fromHtml("<font color='red'>" + mesg + "</font>"),
                    Toast.LENGTH_LONG).show();
        }

        // Check if tracks exist and correspond to sections
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
                    Log.d(TAG, "342, onCreate, ForegrndLocDialog");

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

        storagePermGranted = isStoragePermGranted();
        if (storagePermGranted) {
            // Test for existence of directory /storage/emulated/0/Documents/TransektCount/transektcount0.db
            File path;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
            {
                path = Environment.getExternalStorageDirectory();
                path = new File(path + "/Documents/TransektCount");
            } else {
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                path = new File(path + "/TransektCount");
            }

            // Create preliminary transektcount0.db if it does not exist
            inFile = new File(path, "/transektcount0.db"); // Initial basic DB
            File inFile1 = new File(path, "/transektcount0_" + transNo + ".db"); // Standard basic DB

            if (!inFile.exists() && !inFile1.exists())
                exportBasisDb(0); // create directory and copy internal DB-data to initial Basis DB-file
        }

        if (autoSection && isStoragePermGranted()) {
            fineLocationPermGranted = isFineLocationPermGranted();
            if (transectHasTrack && fineLocationPermGranted) {

                // Get flag 'has_asked_background'
                boolean hasAskedBackgroundLocation = prefs.getBoolean("has_asked_background", false);

                // Get background location with permissions check only once and if storage and fine location
                //   permissions are granted
                if (storagePermGranted && fineLocationPermGranted && !hasAskedBackgroundLocation
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.d(TAG, "390, onCreate, BackgrndLocDialog");

                    // Ask optional background location permission with info in Snackbar
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

        if (autoSection && fineLocationPermGranted && transectHasTrack)
            locationService = new LocationService(getApplicationContext());
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
            Log.d(TAG, "430, NavBarMode = " + navBarMode);

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
            Log.i(TAG, "468, onResume");

        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        outPref = prefs.getString("pref_csv_out", "sections"); // sort mode csv-export
        autoSection = prefs.getBoolean("pref_auto_section", false);
        locServiceOn = prefs.getBoolean("loc_srv_on", false);
        transectHasTrack = prefs.getBoolean("transect_has_track", false);

        headDataSource.open();
        sectionDataSource.open();
        metaDataSource.open();
        countDataSource.open();
        alertDataSource.open();
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

        editor = prefs.edit();

        // Check if tracks correspond to sections
        if (trCount > 0 && trCount != secCount) {
            autoSection = false;

            editor.putBoolean("pref_auto_section", false);
            editor.commit();
            mesg = getString(R.string.track_err);
            Toast.makeText(getApplicationContext(),
                    fromHtml("<font color='red'>" + mesg + "</font>"),
                    Toast.LENGTH_LONG).show();
        }

        // Set app title according to using GPS and ask location permission
        welcomeTitle = findViewById(R.id.welcomeTitle);
        if (autoSection) {
            welcomeTitle.setText(getString(R.string.app_name1)); // app title = TransektCountGPS
        } else {
            welcomeTitle.setText(getString(R.string.app_name)); // app title = TransektCount
        }

        // Set transect number as transect title
        head = headDataSource.getHead();
        transNo = head.transect_no; // read and test for DB integrity
        try {
            Objects.requireNonNull(getSupportActionBar()).setTitle(transNo);
        } catch (NullPointerException e) {
            // nothing
        }

        if (autoSection && fineLocationPermGranted && transectHasTrack) {
            // Start location service and get 1. location
            locationDispatcher(1);
        }
    }
    // End of onResume()

    // Check external storage self permission
    private Boolean isStoragePermGranted() {
        boolean storGranted;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) // Android >= 11
        {
            // Check permission MANAGE_EXTERNAL_STORAGE for Android >= 11
            storGranted = Environment.isExternalStorageManager();
        } else {
            storGranted = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
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
    @SuppressLint("ApplySharedPref")
    private void locationDispatcher(int locationDispatcherMode) {
        if (fineLocationPermGranted && autoSection) // if GPS should be used
        {
            switch (locationDispatcherMode) {
                case 1 -> {
                    // Start location service
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.d(TAG, "569, locationDispatcher 1");

                    if (!locServiceOn) {
                        Intent sIntent = new Intent(getApplicationContext(), LocationService.class);
                        startService(sIntent);

                        locServiceOn = true;
                        editor = prefs.edit();
                        editor.putBoolean("loc_srv_on", true);
                        editor.commit();
                    }

                    // Get location to check distance to transect
                    getDistance();
                }
                case 2 -> {
                    // Stop location service on backpress, when running
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.d(TAG, "587, location stop 2 by backpress");

                    if (locServiceOn) {
                        stopLocSrv();
                    }
                }
                case 3 -> {
                    // Stop location service by onStop() when app is invisible
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.d(TAG, "596 location stop 3 by onStop");

                    if (locServiceOn) {
                        stopLocSrv();
                    }
                }
            }
        }
    }

    // Get the current location for a hint with the distance to transect
    public void getDistance() {
        if (locationService.canGetLocation() && distMin != 0.0) {
            // Show message: GPS: Distance to track: distance m
            String dst = new DecimalFormat("#.#").format(distMin);
            mesg = getString(R.string.distanceToTrack) + " " + dst + " m";
            Toast.makeText(WelcomeActivity.this,
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
        editor = prefs.edit();
        editor.putBoolean("loc_srv_on", false);
        editor.apply();
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
        } else if (id == R.id.exportBasisMenu) {
            if (storagePermGranted) {
                exportBasisDb(1);
            } else {
                PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                        PermissionsStorageDialogFragment.class.getName());
                if (storagePermGranted) {
                    exportBasisDb(1);
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
                Toast.makeText(this,
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
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
                Toast.makeText(this,
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
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
                    fromHtml("<font color='#008000'>" + mesg + "</font>"),
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

        // Stop location service when denied in settings
        if (!autoSection && locServiceOn) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "797, location stop by setting");
            stopLocSrv();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "807, onPause");

        headDataSource.close();
        sectionDataSource.close();
        metaDataSource.close();
        countDataSource.close();
        alertDataSource.close();
        trackDataSource.close();

        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "824, onStop");

        baseLayout.invalidate();

        // Stop location service when app is finished
        if (!TCLifecycleHandler.isApplicationVisible()) {
            locationDispatcher(3);

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "833, onStop, app not visible. locationDispatcher 3");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "842 onDestroy");
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
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "859, selectSection without GPS");
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
                fromHtml("<font color='#008000'>" + mesg + "</font>"),
                Toast.LENGTH_SHORT).show();
        // Trick: Pause for 100 msec to show toast
        mHandler.postDelayed(() ->
                startActivity(new Intent(getApplicationContext(), ShowResultsActivity.class)
                        .addFlags(FLAG_ACTIVITY_CLEAR_TOP)), 100);
    }

    // Date for filename of exported data
    private static String getcurDate() {
        Date date = new Date();
        @SuppressLint("SimpleDateFormat")
        DateFormat dform = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return dform.format(date);
    }

    /******************************************************************************
     * The next four functions below are for importing data files.
     * They've been put here because no database should be left open at this point.
     */
    // Import the basic DB
    private void importBasisDb() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "899, importBasicDBFile");

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

    /***********************************************************************/
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
                            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                                Log.d(TAG, "945, Selected file: " + selectedFile);

                            if (selectedFile != null)
                                inFile = new File(selectedFile);
                            else
                                inFile = null;
                        }
                    } else if ((result.getResultCode() == Activity.RESULT_FIRST_USER)) {
                        mesg = getString(R.string.noFile);
                        Toast.makeText(getApplicationContext(),
                                fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                                Toast.LENGTH_LONG).show();
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
                                copy(inFile, outFile);

                                // List transNo as title
                                head = headDataSource.getHead();
                                transNo = head.transect_no;
                                Objects.requireNonNull(getSupportActionBar()).setTitle(transNo);

                                // Test for number of existing tracks and matching sections
                                try {
                                    trCount = trackDataSource.getDiffTrks();
                                } catch (SQLiteException e) {
                                    trCount = 0;
                                }
                                secCount = sectionDataSource.getNumEntries();

                                editor = prefs.edit();

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

                                if (trCount > 0) {
                                    mesg = getString(R.string.importDB) + ",\n"
                                            + getString(R.string.with) + " " + trCount + " "
                                            + getString(R.string.tracks);
                                } else {
                                    mesg = getString(R.string.importDB);
                                }
                                Toast.makeText(
                                        getApplicationContext(),
                                        fromHtml("<font color='#008000'>" + mesg + "</font>"),
                                        Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                mesg = getString(R.string.importFail);
                                Toast.makeText(getApplicationContext(),
                                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                        builder.setNegativeButton(R.string.cancelButton, (dialog, id) ->
                        {
                            dialog.cancel();

                            editor = prefs.edit(); // restore previous autoSection state
                            editor.putBoolean("pref_auto_section", autoSectionTemp);
                            editor.putBoolean("transect_has_track", transectHasTrackTemp);
                            editor.commit();
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
                        Toast.makeText(getApplicationContext(),
                                fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                                Toast.LENGTH_LONG).show();
                    }
                    if (inFile != null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.setMessage(R.string.confirmListImport);
                        builder.setCancelable(false);
                        builder.setPositiveButton(R.string.importButton, (dialog, id) ->
                        {
                            // Load .csv species list
                            clearDBforImport();
                            readSpeciesCSV(inFile);
                        });
                        builder.setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
                        alert = builder.create();
                        alert.show();
                    }
                }
            });

    // Clear DB for import of an external species list:
    //  clear COUNT_TABLE and ALERT_TABLE, update SECTION_TABLE
    private void clearDBforImport() {
        dbHelper = new DbHelper(this);
        database = dbHelper.getWritableDatabase();

        String sql = "DELETE FROM " + DbHelper.COUNT_TABLE;
        database.execSQL(sql);

        sql = "DELETE FROM " + DbHelper.ALERT_TABLE;
        database.execSQL(sql);

        sql = "UPDATE " + DbHelper.SECTION_TABLE + " SET "
                + DbHelper.S_CREATED_AT + " = '', "
                + DbHelper.S_NOTES + " = '';";
        database.execSQL(sql);

        dbHelper.close();

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
                    fromHtml("<font color='#008000'>" + mesg + "</font>"),
                    Toast.LENGTH_SHORT).show();
            List<String> codeArray = new ArrayList<>();
            List<String> nameArray = new ArrayList<>();
            List<String> nameGArray = new ArrayList<>();
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            String csvLine;
            int iList;       // index of imported list
            int iCounts = 1; // index of id in table counts
            int iSec;        // index of section
            int maxSec = sectionDataSource.getNumEntries(); // number of sections

            // For all sections
            for (iSec = 1; iSec <= maxSec; iSec++) {
                iList = 0;
                while ((csvLine = br.readLine()) != null) // for each csvLine
                {
                    // comma-separated 0:id (not used), 1:code, 2:name, 3:nameL
                    String[] specLine = csvLine.split(",");
                    codeArray.add(iList, specLine[0]);
                    nameArray.add(iList, specLine[1]);
                    nameGArray.add(iList, specLine[2]);
                    countDataSource.writeCountItem(String.valueOf(iCounts), String.valueOf(iSec),
                            codeArray.get(iList), nameArray.get(iList), nameGArray.get(iList));
                    iList++;
                    iCounts++;
                }
                br.close();
                br = new BufferedReader(new FileReader(inFile));
            }
            br.close();
            mesg = getString(R.string.importList);
            Toast.makeText(this,
                    fromHtml("<font color='green'>" + mesg + "</font>"),
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            mesg = getString(R.string.importListFail);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
        }
    }
    // End of importSpeciesList()

    /*******************************************************************************************
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

                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.d(TAG, "1222 importGPS, Selected file: " + selectedFile);

                        if (!selectedFile.isEmpty())
                            inFile = new File(selectedFile);
                        else
                            inFile = null;
                    } else {
                        selectedFile = "";
                        inFile = null;
                        if ((result.getResultCode() == Activity.RESULT_FIRST_USER)) {
                            mesg = getString(R.string.noFile);
                            Toast.makeText(getApplicationContext(),
                                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
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
                                        Log.e(TAG, "1260, " +
                                                "decodeGPX, Problem converting Stream to String: " + e);
                                }

                                String gpxString = gpxsb.toString();

                                mesg = getString(R.string.waitImport);
                                Toast.makeText(WelcomeActivity.this,
                                        fromHtml("<font color='#005000'>" + mesg + "</font>"),
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

                                // Parse gpxString to write fields into TRACK_TABLE
                                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                                    Log.d(TAG, "1284, decodeGPX, Datasources open");

                                // get number of sections and compare with number of tracks
                                int numSect = sectionDataSource.getNumEntries();
                                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                                    Log.d(TAG, "1289, decodeGPX, numSect: "
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
                                Toast.makeText(WelcomeActivity.this,
                                        fromHtml("<font color='#005000'>" + mesg + "</font>"),
                                        Toast.LENGTH_SHORT).show();

                            } else {
                                mesg = getString(R.string.no_GPSfile);
                                Toast.makeText(WelcomeActivity.this,
                                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                                        Toast.LENGTH_LONG).show();
                            }
                        }).setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());

                        alert = builder.create();
                        alert.show();
                    }
                }
            });

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
                    Log.d(TAG, "1354, decodeGPX, gpxTrkString: " + gpxTrkString);

                // set track name from section name
                // record of transect section
                Section section = sectionDataSource.getSection(trk);
                sectionNameGPS = section.name;

                // reduce gpxString to rest after </trk>
                strEnd = gpxString.indexOf("</trk>");

                // add offset = length of "</trk>" = 6
                gpxString = gpxString.substring(strEnd + 6);

                if (gpxTrkString.contains("<trkseg>")) {
                    // for each track point in trkseg
                    do {
                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.d(TAG, "1371, decodeGPX, do trackpt");
                        int nextTp; // index for next track point (after /> or /trkpt>)
                        strStart = gpxTrkString.indexOf("lat=") + 5;
                        strEnd = gpxTrkString.indexOf("lat=") + 14;
                        String tlat = gpxTrkString.substring(strStart, strEnd);

                        strStart = gpxTrkString.indexOf("lon=") + 5;
                        strEnd = gpxTrkString.indexOf("lon=") + 14;
                        String tlon = gpxTrkString.substring(strStart, strEnd);
                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.d(TAG, "1381 decodeGPX, sectionNameGPS: "
                                    + sectionNameGPS + ", " + tlat + ", " + tlon);

                        trackDataSource.createTrackTp(sectionNameGPS, tlat, tlon);

                        // increment gpxTrkString line
                        trkPt = trkPt + 1;

                        if (gpxVik) // test for track data (Viking/Garmin)
                        {
                            // reduce gpxTrkString to remainder after current trkpt line
                            nextTp = gpxTrkString.indexOf("</trkpt>");
                            gpxTrkString = gpxTrkString.substring(nextTp + 8);

                            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                                Log.d(TAG, "1396, decodeGPX, trackpt " + trkPt);
                        } else {
                            nextTp = gpxTrkString.indexOf("/>");
                            gpxTrkString = gpxTrkString.substring(nextTp + 2);

                            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                                Log.d(TAG, "1402, decodeGPX, trackpt " + trkPt);
                        }
                    } while (gpxTrkString.contains("<trkpt"));
                }

                // increment trk and reduce gpxString for next track segment
                trk = trk + 1;
            } while (gpxString.contains("<trk>"));

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "1412, decodeGPX, gpxString finished: " + gpxString);
        }

        transectHasTrack = true;
        editor = prefs.edit();
        editor.putBoolean("transect_has_track", transectHasTrack);
        editor.apply();
        exportBasisDb(0); // Write modified DB as Basic DB
    }
    // End of importGPS()

    /****************************************/
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
                exportBasisDb(0); // Write modified DB as Basic DB

                if (locServiceOn)
                    stopLocSrv();
                transectHasTrack = false;
                autoSection = false;
                editor = prefs.edit();
                editor.putBoolean("pref_auto_section", false);
                editor.putBoolean("transect_has_track", false);
                editor.commit();

                mesg = getString(R.string.resetTracks);
                Toast.makeText(this,
                        fromHtml("<font color='green'><b>" + mesg + "</b></font>"),
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

    /*********************************************************************************
     * The next four functions below are for exporting data files.
     */
    // Exports Basis DB to Documents/TransektCount/transektcount0.db
    private void exportBasisDb(int i) {
        // i = 1: show message only when executed by menu command
        // inFile <- /data/data/com.wmstein.transektcount/databases/transektcount.db
        String inPath = getApplicationContext().getFilesDir().getPath();
        inPath = inPath.substring(0, inPath.lastIndexOf("/")) + "/databases/transektcount.db";
        inFile = new File(inPath);

        // tmpfile -> /data/data/com.wmstein.transektcount/files/transektcount_tmp.db
        String tmpPath = getApplicationContext().getFilesDir().getPath();
        tmpPath = tmpPath.substring(0, tmpPath.lastIndexOf("/")) + "/files/transektcount_tmp.db";
        File tmpfile = new File(tmpPath);

        // outFile -> /storage/emulated/0/Documents/TransektCount/transektcount0.db
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TransektCount");
        } else {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TransektCount");
        }

        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // just verify path, result ignored
        if (Objects.equals(transNo, ""))
            outFile = new File(path, "/transektcount0.db");
        else
            outFile = new File(path, "/transektcount0_" + transNo + ".db");

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
                // Save current db as backup db tmpfile
                copy(inFile, tmpfile);

                // Clear DB values for basic DB
                clearDBValues();

                // Write Basis DB
                copy(inFile, outFile);

                // Restore actual db from tmpfile
                copy(tmpfile, inFile);

                // Delete backup db
                boolean d0 = tmpfile.delete();
                if (d0 && i == 1) {
                    mesg = getString(R.string.saveBasisDB);
                    Toast.makeText(this,
                            fromHtml("<font color='#006400'>" + mesg + "</font>"),
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
    // End of exportBasisDb()

    @SuppressLint({"SdCardPath", "LongLogTag"})
    private void exportDb() {
        // New data directory:
        //   outFile -> Public Directory Documents/TransektCount/
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TransektCount");
        } else {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TransektCount");
        }

        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // Just verify path, result ignored

        // outFile -> /storage/emulated/0/Documents/TransektCount/transektcount_Tr-No_yyyyMMdd_HHmmss.db
        if (Objects.equals(transNo, ""))
            outFile = new File(path, "/transektcount_" + getcurDate() + ".db");
        else
            outFile = new File(path, "/transektcount_" + transNo + "_" + getcurDate() + ".db");

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
                        fromHtml("<font color='#008000'>" + mesg + "</font>"),
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

    /*****************************************************************************
     // Exports DB contents as Transekt_TransNo_yyyyMMdd_HHmmss.csv-file to
     // Documents/TransektCount/ with purged data set.
     // Spreadsheet programs can import this csv file with
     //   - Unicode UTF-8 filter,
     //   - comma delimiter and
     //   - "" for text recognition.
     */
    private void exportDb2CSV() {
        /* outFile -> /storage/emulated/0/Documents/TransektCount/Transekt_Tr-No_yyyyMMdd_HHmmss.csv
        //
        // 1. Alternative for Android >= 10 (Q):
        //    path = new File(Environment.getExternalStorageDirectory() + "/Documents/TransektCount");
        //
        // 2. Alternative for Android < 10 (deprecated in Q):
        //    path = new File(Environment.getExternalStoragePublicDirectory
        //    (Environment.DIRECTORY_DOCUMENTS) + "/TransektCount");
        */
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TransektCount");
        } else {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TransektCount");
        }

        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // Just verify path, result ignored

        String language = Locale.getDefault().toString().substring(0, 2);
        if (language.equals("de")) {
            if (Objects.equals(transNo, ""))
                outFile = new File(path, "/Transekt_" + getcurDate() + ".csv");
            else
                outFile = new File(path, "/Transekt_" + transNo + "_" + getcurDate() + ".csv");
        } else {
            if (Objects.equals(transNo, ""))
                outFile = new File(path, "/Transect_" + getcurDate() + ".csv");
            else
                outFile = new File(path, "/Transect_" + transNo + "_" + getcurDate() + ".csv");
        }

        Section section;
        int sect_id;

        Meta meta; // Meta database instance
        String inspecName;
        int temps, tempe;   // temperature at start time and end time
        int winds, winde;   // wind
        int clouds, cloude; // clouds

        String date, start_tm, end_tm, kw, inspection_note; // kw = calendar week (String)
        int yyyy, mm, dd;
        int Kw = 0; // calendar week (Int)

        //  ♂|♀        ♂         ♀         pupa      larva     ovo
        int summf = 0, summ = 0, sumf = 0, sump = 0, suml = 0, sumo = 0;
        int summfe = 0, summe = 0, sumfe = 0, sumpe = 0, sumle = 0, sumoe = 0;

        int totali, totale;
        int total, sumSpec;

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
                String[] arrCol =
                        {
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
                                "", "", "", "", "", "", "",
                                getString(R.string.note),
                        };
                csvWrite.writeNext(arrCol); // write line to csv-file

                // Open Head table for head info
                head = headDataSource.getHead();
                inspecName = head.inspector_name;

                // Open Meta table for meta info
                meta = metaDataSource.getMeta();
                temps = meta.temps;
                tempe = meta.tempe;
                winds = meta.winds;
                winde = meta.winde;
                clouds = meta.clouds;
                cloude = meta.cloude;
                date = meta.date;
                start_tm = meta.start_tm;
                end_tm = meta.end_tm;
                inspection_note = meta.note;

                // Calculating the week of the year (ISO 8601)
                Calendar cal = Calendar.getInstance();

                assert date != null;
                if (!date.isEmpty()) {
                    if (language.equals("de")) {
                        try {
                            yyyy = Integer.parseInt(date.substring(6, 10));
                            mm = Integer.parseInt(date.substring(3, 5));
                            dd = Integer.parseInt(date.substring(0, 2));
                        } catch (Exception e) {
                            // Wrong date format (English DB in German), use
                            yyyy = Integer.parseInt(date.substring(0, 4));
                            mm = Integer.parseInt(date.substring(5, 7));
                            dd = Integer.parseInt(date.substring(8, 10));
                        }
                    } else {
                        try {
                            yyyy = Integer.parseInt(date.substring(0, 4));
                            mm = Integer.parseInt(date.substring(5, 7));
                            dd = Integer.parseInt(date.substring(8, 10));
                        } catch (Exception e) {
                            // Wrong date format (German DB in English), use
                            yyyy = Integer.parseInt(date.substring(6, 10));
                            mm = Integer.parseInt(date.substring(3, 5));
                            dd = Integer.parseInt(date.substring(0, 2));
                        }
                    }

                    // Example: cal.set(2017, 3, 9) -> 09.04.2017
                    cal.set(yyyy, mm - 1, dd);
                    Kw = cal.get(Calendar.WEEK_OF_YEAR);
                }

                // 1. headline info with
                //    transect no., inspector name, date, start-time, start-temperature, start-wind,
                //    clouds, calendar week
                kw = String.valueOf(Kw);
                String[] arrMeta =
                        {
                                transNo,
                                inspecName,
                                date,
                                getString(R.string.from),
                                start_tm,
                                String.valueOf(temps),
                                String.valueOf(winds),
                                String.valueOf(clouds),
                                "",
                                kw,
                                "", "", "", "", "", "", "",
                                inspection_note,
                        };
                csvWrite.writeNext(arrMeta);

                // 2. headline info with
                //    end-time, end-temperature, end-clouds
                String[] arrMeta1 =
                        {
                                "", "", "",
                                getString(R.string.to),
                                end_tm,
                                String.valueOf(tempe),
                                String.valueOf(winde),
                                String.valueOf(cloude),
                        };
                csvWrite.writeNext(arrMeta1);

                // Empty row
                String[] arrEmpt = {};
                csvWrite.writeNext(arrEmpt);

                // Intern, extern
                String[] arrIE = {"", "", "", "", "", getString(R.string.internal), "", "", "", "", "", getString(R.string.external)};
                csvWrite.writeNext(arrIE);

                // Headline of species table with
                String[] arrCol1;
                if (outPref.equals("sections")) {
                    // Section, Time of 1. Count, Species Name, Local Name, Code, Internal Counts,
                    //   External Counts, Spec.-Notes
                    arrCol1 = new String[]{
                            getString(R.string.name_sect),
                            getString(R.string.time_sect),
                            getString(R.string.name_spec),
                            getString(R.string.name_spec_g),
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
                } else {
                    // Species Name, Local Name, Code, Section, Internal Counts, External Counts,
                    //   Spec.-Notes
                    arrCol1 = new String[]{
                            getString(R.string.name_spec),
                            getString(R.string.name_spec_g),
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
                }
                csvWrite.writeNext(arrCol1);

                //*****************************************************************************
                // Build the internal species array according to the sorted species or sections
                Cursor curCSV;
                if (outPref.equals("sections")) {
                    // Cursor contains list sorted by section and name with all internal count entries > 0
                    curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                            + " WHERE ("
                            + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                            + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                            + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                            + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                            + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                            + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                            + " order by " + DbHelper.C_SECTION_ID + ", " + DbHelper.C_NAME, null);
                } else {
                    // Cursor contains list sorted by name and section with all internal count entries > 0
                    curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                            + " WHERE ("
                            + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                            + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                            + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                            + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                            + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                            + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                            + " order by " + DbHelper.C_NAME + ", " + DbHelper.C_SECTION_ID, null);
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

                    if (!Objects.equals(sectName1, sectName)) {
                        sectName1 = sectName;
                        sectName2 = sectName;
                        sectTimeValue = section.datNum(); // get Long created_at value
                        Date result = new Date(sectTimeValue);
                        sectTime = sdf.format(result);
                    } else {
                        sectTime = "";
                        sectName2 = "";
                    }

                    code = curCSV.getString(3); //species code
                    name_s = curCSV.getString(2);
                    name_l = curCSV.getString(17);
                    sp_notes = curCSV.getString(16);

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

                    String[] arrStr;
                    if (outPref.equals("sections")) {
                        // Build line in species table for order of sections
                        arrStr = new String[]{
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
                    } else {
                        // Build line in species table for order of species
                        arrStr = new String[]{
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
                    }
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
                csvWrite.writeNext(arrIE);

                // Internal counts, External counts, Totals
                String[] arrCol2 =
                        {
                                "", "", "", "", "",
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
                        };
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
                String[] arrSumi =
                        {
                                "", "",
                                getString(R.string.sumSpec),
                                Integer.toString(sumSpec),
                                getString(R.string.sum),
                                strsummf,
                                strsumm,
                                strsumf,
                                strsump,
                                strsuml,
                                strsumo,
                                "", "", "", "", "", "",
                                strtotali
                        };
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
                String[] arrSume =
                        {
                                "", "", "", "",
                                getString(R.string.sume),
                                "", "", "", "", "", "",
                                strsummfe,
                                strsumme,
                                strsumfe,
                                strsumpe,
                                strsumle,
                                strsumoe,
                                strtotale,
                        };
                csvWrite.writeNext(arrSume);

                // Overall totals
                if (total > 0)
                    strtotal = Integer.toString(total);
                else
                    strtotal = "";

                // Write overall total sum
                String[] arrTotal =
                        {
                                "", "", "", "",
                                getString(R.string.sum_total),
                                "", "", "", "", "", "",
                                "", "", "", "", "", "",
                                strtotal
                        };
                csvWrite.writeNext(arrTotal);

                csvWrite.close();
                mesg = getString(R.string.savecsv);
                Toast.makeText(this,
                        fromHtml("<font color='#008000'>" + mesg + "</font>"),
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                mesg = getString(R.string.saveFail);
                Toast.makeText(this,
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.e(TAG, "2233, csv write external failed");
            }
            dbHelper.close();
        }
    }
    // End of exportDb2CSV()

    /**********************************************************************************************/
    // Export current species list to both data directories
    //  /Documents/TourCount/species_YYYYMMDD_hhmmss.csv and
    //  /Documents/TransektCount/species_YYYYMMDD_hhmmss.csv
    private void exportSpeciesList() {
        // outFileTour -> /storage/emulated/0/Documents/TourCount/species_yyyyMMdd_HHmmss.csv
        // outFileTransect -> /storage/emulated/0/Documents/TransektCount/species_yyyyMMdd_HHmmss.csv
        File pathTour, outFileTour, pathTransect, outFileTransect = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            pathTour = new File(Environment.getExternalStorageDirectory() + "/Documents/TourCount");
            pathTransect = new File(Environment.getExternalStorageDirectory() + "/Documents/TransektCount");
        } else {
            pathTour = new File(Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/TourCount");
            pathTransect = new File(Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/TransektCount");
        }

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED.equals(sState);

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
        } else {
            // Export species list into species_yyyy-MM-dd_HHmmss.csv
            dbHelper = new DbHelper(this);
            database = dbHelper.getWritableDatabase();

            String[] codeArray;
            String[] nameArray;
            String[] nameArrayL;

            codeArray = countDataSource.getAllStringsForSectionSrtCode(1, "code");
            nameArray = countDataSource.getAllStringsForSectionSrtCode(1, "name");
            nameArrayL = countDataSource.getAllStringsForSectionSrtCode(1, "name_g");

            int specNum = codeArray.length;

            // If TourCount is installed export to /Documents/TourCount
            if (pathTour.exists() && pathTour.isDirectory()) {
                String language = Locale.getDefault().toString().substring(0, 2);
                if (language.equals("de")) {
                    if (Objects.equals(transNo, ""))
                        outFileTour = new File(pathTour, "/species_Transekt_de_"
                                + getcurDate() + ".csv");
                    else
                        outFileTour = new File(pathTour, "/species_Transekt_de_"
                                + getcurDate() + "_" + transNo + ".csv");
                } else {
                    if (Objects.equals(transNo, ""))
                        outFileTour = new File(pathTour, "/species_Transect_en_"
                                + getcurDate() + ".csv");
                    else
                        outFileTour = new File(pathTour, "/species_Transect_en_"
                                + getcurDate() + "_" + transNo + ".csv");
                }

                try {
                    CSVWriter csvWrite = new CSVWriter(new FileWriter(outFileTour));

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
                String language = Locale.getDefault().toString().substring(0, 2);
                if (language.equals("de")) {
                    if (Objects.equals(transNo, ""))
                        outFileTransect = new File(pathTransect, "/species_Transekt_" + getcurDate() + ".csv");
                    else
                        outFileTransect = new File(pathTransect, "/species_Transekt_" + transNo + "_" + getcurDate() + ".csv");
                } else {
                    if (Objects.equals(transNo, ""))
                        outFileTransect = new File(pathTransect, "/species_Transect_" + getcurDate() + ".csv");
                    else
                        outFileTransect = new File(pathTransect, "/species_Transect_" + transNo + "_" + getcurDate() + ".csv");
                }
            }

            try {
                CSVWriter csvWrite = new CSVWriter(new FileWriter(outFileTransect));

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
                        fromHtml("<font color='#008000'>" + mesg + "</font>"),
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                mesg = getString(R.string.saveFailList);
                Toast.makeText(this,
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
            }
            dbHelper.close();
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
                Toast.makeText(this,
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

            sql = "DELETE FROM " + DbHelper.ALERT_TABLE;
            database.execSQL(sql);

        } catch (Exception e) {
            mesg = getString(R.string.resetFail);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
            r_ok = false;
        }
        dbHelper.close();
        return r_ok;
    }
    // End of resetToBasisDb()

}
