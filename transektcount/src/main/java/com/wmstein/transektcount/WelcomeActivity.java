package com.wmstein.transektcount;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import com.google.android.material.snackbar.Snackbar;
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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import sheetrock.panda.changelog.ChangeLog;
import sheetrock.panda.changelog.ViewHelp;

import static android.graphics.Color.RED;

/**********************************************************************
 * WelcomeActivity provides the starting page with menu and buttons for
 * import/export/help/info methods and starts
 * EditMetaActivity, ListSectionActivity and ListSpeciesActivity.
 * It uses further LocationService and PermissionDialogFragment.
 * <p>
 * Based on BeeCount's WelcomeActivity.java by milo from 2014-05-05.
 * Changes and additions for TransektCount by wmstein since 2016-02-18,
 * last edited on 2024-03-10.
 */
public class WelcomeActivity
    extends AppCompatActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener,
               PermissionsDialogFragment.PermissionsGrantedCallback
{
    private static final String TAG = "WelcomeAct";

    @SuppressLint("StaticFieldLeak")
    private static TransektCountApplication transektCount;

    // GPS tracks related
    LocationService locationService;
    private boolean locServiceOn = false;
    private double latitude, longitude; // gps position
    private String tSecName;            // track section name
    private String tSectionMatch;       // outside track section name
    private boolean insideOfTrack = true; // inside of track (always true for manual section selection)
    private DataTrkpt dataTrkpt;        // data class with tSecName and insideOfTrack

    // Permission dispatcher mode locationPermissionDispatcherMode:
    //  1 = use location service
    //  2 = end location service
    private int locationPermissionDispatcherMode;

    // locationPermission contains initial location permission state that controls
    // if location listener has to be stopped after permission changed:
    // Stop listener if permission was denied after listener start.
    // Don't stop listener if permission was allowed later and listener has not been started
    private boolean locationPermission;

    private ChangeLog cl;
    private ViewHelp vh;

    private final Handler mHandler = new Handler();

    public boolean doubleBackToExitPressedTwice = false;

    // import/export stuff
    private File infile;
    private File outfile;
    private String selectedFile;

    boolean mExternalStorageAvailable = false;
    boolean mExternalStorageWriteable = false;
    final String state = Environment.getExternalStorageState();

    AlertDialog alert;

    // preferences
    private SharedPreferences prefs;
    private String sortPref;
    private boolean autoSection; // true for GPS track selection
    private boolean sectionHasTrack = false; // transect sections have tracks
    private SharedPreferences.Editor editor;

    // db handling
    private SQLiteDatabase database;
    private DbHelper dbHandler;
    private HeadDataSource headDataSource;
    private Head head;
    private SectionDataSource sectionDataSource;
    private Section section;
    private TrackDataSource trackDataSource;
    private List<Track> trackPts;

    private String gpxString;
    private String gpxTrkString;
    private int strStart;
    private int strEnd;    // temp. index end of string
    private int trkStart;  // temp index start of trk
    private int trkEnd;    // temp index end of trk
    private int trkpt = 1; // trackpoint counter
    private int trk = 1;   // track counter

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        transektCount = (TransektCountApplication) getApplication();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_welcome);
        View baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(transektCount.getBackground());

        if (!isStorageGranted())
        {
            PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(), PermissionsDialogFragment.class.getName());
            if (!isStorageGranted())
            {
                showSnackbarRed(getString(R.string.perm_cancel));
            }
        }

        selectedFile = "";
        infile = null;
        outfile = null;

        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);

        sectionHasTrack = prefs.getBoolean("section_has_track", false);
        autoSection = prefs.getBoolean("pref_auto_section", false);
        if (!sectionHasTrack && autoSection)
        {
            autoSection = false;
            editor = prefs.edit();
            editor.putBoolean("pref_auto_section", autoSection);
            editor.commit();
        }
        if (MyDebug.LOG)
            Log.d(TAG, "194, onCreate, autoSection: " + autoSection
                + ", Section has track: " + sectionHasTrack);

        // check for DB integrity
        try
        {
            headDataSource = new HeadDataSource(this);
            headDataSource.open();
            head = headDataSource.getHead();
            String tName = head.transect_no; // just dummy read for DB integrity test
            headDataSource.close();
        } catch (SQLiteException e)
        {
            headDataSource.close();
            showSnackbarRed(getString(R.string.corruptDb));
        }

        // try to get list of trackpts and number of tracks
        int trCount;
        try
        {
            trackDataSource = new TrackDataSource(this);
            trackDataSource.open();
            trackPts = trackDataSource.getAllTrackPoints();
            trCount = trackDataSource.getDiffTrks();
            trackDataSource.close();
        } catch (SQLiteException e)
        {
            trackDataSource.close();
            trCount = 0;
        }

        // get number of sections and name for 1st use of new DB
        int secCount;
        try
        {
            sectionDataSource = new SectionDataSource(this);
            sectionDataSource.open();
            secCount = sectionDataSource.getNumEntries();
            tSecName = sectionDataSource.getSection(1).name;
            sectionDataSource.close();
        } catch (SQLiteException e)
        {
            sectionDataSource.close();
            secCount = 0;
        }
        if (MyDebug.LOG && autoSection)
            Log.d(TAG, "241, onCreate, TrkPts: " + trackPts.size()
                + ", trCount: " + trCount + ", secCount: " + secCount);

        // check if tracks correspond to sections
        if (trCount > 0 && trCount != secCount)
        {
            autoSection = false;

            // store autoSection in SharedPreferences
            editor = prefs.edit();
            editor.putBoolean("pref_auto_section", autoSection);
            editor.commit();
            showSnackbarRed(getString(R.string.track_err));
        }

        // check if tracks exist and correspond to sections
        if (autoSection && (trCount != secCount))
        {
            autoSection = false;
            sectionHasTrack = false;

            // store autoSection and sectionHasTrack in SharedPreferences
            editor = prefs.edit();
            editor.putBoolean("pref_auto_section", autoSection);
            editor.putBoolean("section_has_track", sectionHasTrack);
            editor.commit();
            showSnackbarRed(getString(R.string.track_err));
        }

        // check initial location permission state
        if (autoSection && sectionHasTrack)
        {
            locationPermission =
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        }

        // store locationPermission in SharedPreferences
        editor = prefs.edit();
        editor.putBoolean("location_permission", locationPermission);
        editor.apply();

        // Show changelog for new version
        cl = new ChangeLog(this);
        vh = new ViewHelp(this);
        if (cl.firstRun())
            cl.getLogDialog().show();

        // test for existence of directory /storage/emulated/0/Android/data/com.wmstein.transektcount/files/transektcount0.db
        // test for existence of directory /storage/emulated/0/Documents/TransektCount/transektcount0.db
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TransektCount");
        }
        else
        {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TransektCount");
        }
        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // Verify path
        infile = new File(path, "/transektcount0.db");
        if (!infile.exists())
            exportBasisDb(); // create directory and initial Basis DB (getExternalFilesDir, getExternalDir)
    }
    // end of onCreate

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onResume()
    {
        super.onResume();

        if (MyDebug.LOG) Log.d(TAG, "316, onResume");
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        sortPref = prefs.getString("pref_sort_sp", "none"); // sort mode species list
        sectionHasTrack = prefs.getBoolean("section_has_track", false);
        locationPermission = prefs.getBoolean("location_permission", false);
        autoSection = prefs.getBoolean("pref_auto_section", false);
        if (!sectionHasTrack && autoSection)
        {
            showSnackbarRed(getString(R.string.track_err1));
            autoSection = false;
            editor = prefs.edit();
            editor.putBoolean("pref_auto_section", autoSection);
            editor.commit();
        }

        headDataSource = new HeadDataSource(this);
        headDataSource.open();
        head = headDataSource.getHead();
        headDataSource.close();

        // set transect name as title
        try
        {
            Objects.requireNonNull(getSupportActionBar()).setTitle(head.transect_no);
        } catch (NullPointerException e)
        {
            // nothing
        }

        // for GPS check permission and get location
        if (autoSection)
        {
            locationPermissionDispatcherMode = 1;
            locationCaptureFragment();
        }

        // new onBackPressed logic TODO
        if (Build.VERSION.SDK_INT >= 33)
        {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                () ->
                {
                    /**
                     * onBackPressed logic goes here - For instance:
                     * Prevents closing the app to go home screen when in the
                     * middle of entering data to a form
                     * or from accidentally leaving a fragment with a WebView in it
                     *
                     * Unregistering the callback to stop intercepting the back gesture:
                     * When the user transitions to the topmost screen (activity, fragment)
                     * in the BackStack, unregister the callback by using
                     * OnBackInvokeDispatcher.unregisterOnBackInvokedCallback
                     */
                    if (doubleBackToExitPressedTwice)
                    {
                        finish();
                    }

                    this.doubleBackToExitPressedTwice = true;
                    Toast.makeText(this, R.string.back_twice, Toast.LENGTH_SHORT).show();

                    mHandler.postDelayed(() -> doubleBackToExitPressedTwice = false, 1500);
                }
                                                                      );
        }
    } // end of onResume

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.welcome, menu);
        return true;
    }

    // Handle action bar item clicks here. The action bar will automatically handle clicks on
    // the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        }
        else if (id == R.id.exportMenu)
        {
            exportDb();
            return true;
        }
        else if (id == R.id.exportCSVMenu)
        {
            if (isStorageGranted())
            {
                exportDb2CSV();
                return true;
            }
            else
            {
                PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(), PermissionsDialogFragment.class.getName());
                if (isStorageGranted())
                {
                    exportDb2CSV();
                }
                else
                {
                    showSnackbarRed(getString(R.string.perm_cancel));
                }
            }
            return true;
        }
        else if (id == R.id.exportBasisMenu)
        {
            exportBasisDb();
            return true;
        }
        else if (id == R.id.importBasisMenu)
        {
            importBasisDb();
            return true;
        }
        else if (id == R.id.importFileMenu)
        {
            importDBFile();
            return true;
        }
        else if (id == R.id.importGPXMenu)
        {
            if (!sectionHasTrack)
            {
                importGPX();
            }
            else
            {
                showSnackbarRed(getString(R.string.importTrackFail));
            }
            return true;
        }
        else if (id == R.id.deleteGPXMenu)
        {
            if (sectionHasTrack)
            {
                deleteGPX();
            }
            else
            {
                showSnackbarRed(getString(R.string.deleteTrackFail));
            }
            return true;
        }
        else if (id == R.id.resetDBMenu)
        {
            resetToBasisDb();
            return true;
        }
        else if (id == R.id.viewHelp)
        {
            vh.getFullLogDialog().show();
            return true;
        }
        else if (id == R.id.changeLog)
        {
            cl.getFullLogDialog().show();
            return true;
        }
        else if (id == R.id.startCounting)
        {
            if (autoSection && locationPermission && sectionHasTrack)
            {
                /*****************************************
                 Automatic selection of transect section:
                 - get location,
                 - read track info,
                 - check position on track,
                 - get section for track and
                 - determine location is inside of tracks
                 */
                // Get location with permissions check
                locationPermissionDispatcherMode = 1; // allow to get location
                locationCaptureFragment();            // get location

                dataTrkpt = checkSectionTrack();
                if (dataTrkpt.tsection != null)
                    tSecName = dataTrkpt.tsection; // set section name of located section
                sectionDataSource = new SectionDataSource(this);
                sectionDataSource.open();
                section = sectionDataSource.getSectionByName(tSecName);
                sectionDataSource.close();
                insideOfTrack = dataTrkpt.insideOfTrack; // true = position is inside of tracks
                if (MyDebug.LOG)
                    Log.d(TAG, "509, startCounting, Section ID: " + section.id + " Name: "
                        + tSecName + " insideOfTrack: " + insideOfTrack);

                // call CountingActivityA for section
                Intent intent;
                intent = new Intent(WelcomeActivity.this, CountingActivityA.class);
                intent.putExtra("section_id", section.id);
                intent.putExtra("welcome_act", true); // controls itemPosition handling
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
            else
            {
                // manual selection of transect section
                Intent intent;
                intent = new Intent(WelcomeActivity.this, ListSectionActivity.class);
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
            return true;
        }
        else if (id == R.id.editMeta)
        {
            startActivity(new Intent(this, EditMetaActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        }
        else if (id == R.id.viewSpecies)
        {
            Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show();

            // Trick: Pause for 100 msec to show toast
            mHandler.postDelayed(() ->
                startActivity(new Intent(getApplicationContext(), ListSpeciesActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)), 100);
            return true;
        }
        return super.onOptionsItemSelected(item);

    } // end of onOptionsItemSelected

    // Save activity state for CountingActivityA and ListSectionActivity
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        View baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(null);
        baseLayout.setBackground(transektCount.setBackground());
        sortPref = prefs.getString("pref_sort_sp", "none");
        autoSection = prefs.getBoolean("pref_auto_section", false);
        sectionHasTrack = prefs.getBoolean("section_has_track", false);
    }

    public void onPause()
    {
        super.onPause();

        editor = prefs.edit();
        editor.putBoolean("location_permission", locationPermission);
        editor.putInt("j", 1); // reset counter in DummyActivity
        editor.apply();
    }

    /**
     * @noinspection deprecation
     */
    // press Back twice to end the app
    @Override
    public void onBackPressed()
    {
        if (doubleBackToExitPressedTwice)
        {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedTwice = true;
        Toast.makeText(this, R.string.back_twice, Toast.LENGTH_SHORT).show();

        mHandler.postDelayed(() -> doubleBackToExitPressedTwice = false, 1500);
    }

    public void onStop()
    {
        super.onStop();

        if (locationPermission && autoSection && sectionHasTrack)
        {
            // Stop location service with permissions check
            locationPermissionDispatcherMode = 2;
            locationCaptureFragment();
        }
    }

    // check initial external storage permission
    private boolean isStorageGranted()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) // Android 11+
        {
            return Environment.isExternalStorageManager(); // check permission MANAGE_EXTERNAL_STORAGE for Android 11+
        }
        else
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // Part of permission handling
    @Override
    public void locationCaptureFragment()
    {
        if (MyDebug.LOG) Log.d(TAG, "620 locationCaptureFragment()");

        locationPermission =
            (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);

        if (locationPermission) // current location permission state
        {
            switch (locationPermissionDispatcherMode)
            {
                case 1 ->
                {
                    if (autoSection && sectionHasTrack)
                    {
                        if (locServiceOn)
                        {
                            getLoc(); // start location service and get location
                        }
                        else
                        {
                            locationService = new LocationService(this);
                            locServiceOn = true;
                            getLoc();
                        }
                    }
                }
                case 2 ->
                {
                    if (locServiceOn)
                    {
                        locationService.stopListener(); // stop location service
                        locServiceOn = false;
                    }
                }
            }
        }
        else
        {
            if (locationPermissionDispatcherMode == 1 && autoSection && sectionHasTrack)
                PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(),
                    PermissionsDialogFragment.class.getName());
        }
    }

    // get the location data
    private void getLoc()
    {
        if (locationService.canGetLocation())
        {
            longitude = locationService.getLongitude();
            latitude = locationService.getLatitude();
        }
    }

    // Date for filename of Export-DB
    public static String getcurDate()
    {
        Date date = new Date();
        @SuppressLint("SimpleDateFormat")
        DateFormat dform = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        return dform.format(date);
    }

    // Prepare for and start the appropriate CountingActivity depending on
    //   manual or automatic selection of sections
    public void startCounting(View view)
    {
        if (autoSection && locationPermission && sectionHasTrack)
        {
            /********************************************
             * Automatic selection of transect section:
             *   get location,
             *   read track info,
             *   check position on track,
             *   get section for track and
             *   determine if location is inside of track
             */
            // Get location with permissions check
            locationPermissionDispatcherMode = 1; // allow to get location
            locationCaptureFragment();            // get location

            dataTrkpt = checkSectionTrack();
            if (dataTrkpt.tsection != null)
                tSecName = dataTrkpt.tsection; // set section name of located section
            sectionDataSource = new SectionDataSource(this);
            sectionDataSource.open();
            section = sectionDataSource.getSectionByName(tSecName);
            sectionDataSource.close();
            insideOfTrack = dataTrkpt.insideOfTrack; // true = position is inside of tracks
            if (MyDebug.LOG)
                Log.d(TAG, "710, startCounting, Section ID: " + section.id + " Name: "
                    + tSecName + " insideOfTrack: " + insideOfTrack);

            // call CountingActivityA for section
            Intent intent;
            intent = new Intent(WelcomeActivity.this, CountingActivityA.class);
            intent.putExtra("section_id", section.id);
            intent.putExtra("welcome_act", true); // controls itemPosition handling
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        else
        {
            // no use of transect GPS track data
            Intent intent;
            intent = new Intent(WelcomeActivity.this, ListSectionActivity.class);
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
    }

    public void editMeta(View view)
    {
        startActivity(new Intent(this, EditMetaActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void viewSpecies(View view)
    {
        Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show();

        // Trick: Pause for 100 msec to show toast
        mHandler.postDelayed(() ->
            startActivity(new Intent(getApplicationContext(), ListSpeciesActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)), 100);
    }

    /************************************************
     * check if tracks exist and coords match section
     */
    private DataTrkpt checkSectionTrack()
    {
        double dist = 0.0;
        String tLat = "0.0";
        String tLon = "0.0";
        // read all TRACK_TABLE entries until dist matches distMax
        for (Track trackpt : trackPts)
        {
            tSecName = trackpt.tsection;
            if (trackpt.id == 1)
                tSectionMatch = tSecName; // initial value
            tLat = trackpt.tlat;
            tLon = trackpt.tlon;
            dist = sDistance(tLat, tLon, latitude, longitude);
            double distMax = 5.0; // 5 meters
            if (dist < distMax)
            {
                insideOfTrack = true;
                tSectionMatch = tSecName;
                break;
            }
            else
            {
                insideOfTrack = false;
                tSecName = tSectionMatch;
            }
            // check next trackpt
        }

        if (MyDebug.LOG)
        {
            Log.d(TAG, "778, checkSectionTrack, GPS Lat: " + latitude + ", GPS Lon: " + longitude);
            Log.d(TAG, "779, checkSectionTrack, Track-Lat: " + tLat + ", Track-Lon: " + tLon
                + ", dist: " + dist);
            Log.d(TAG, "781, checkSectionTrack, tSecName: " + tSecName + ", insideOfTrack: " + insideOfTrack);
        }
        return new DataTrkpt(tSecName, insideOfTrack);
    }

    // Allows to return a complex result by DataTrkpt
    public static class DataTrkpt
    {
        private final String tsection;
        private final boolean insideOfTrack;

        public DataTrkpt(String tsection, boolean insideOfTrack)
        {
            this.tsection = tsection;
            this.insideOfTrack = insideOfTrack;
        }
    }

    /**************************************************
     * Calculate distance between two coordinate points
     * Uses Pythagorean method as its base.
     * Distance in meters
     */
    private double sDistance(String latDB, String lonDB, Double latGPS, Double lonGPS)
    {
        double latiDB = Double.parseDouble(latDB);
        double loniDB = Double.parseDouble(lonDB);

        return FlatEarthDist.distance(latiDB, loniDB, latGPS, lonGPS);
    }

    private static class FlatEarthDist
    {
        public static double distance(double lat1, double lon1, double lat2, double lon2)
        {
            double a = (lat1 - lat2) * FlatEarthDist.distPerLat(lat1);
            double b = (lon1 - lon2) * FlatEarthDist.distPerLon(lat1);
            return Math.sqrt(a * a + b * b);
        }

        private static double distPerLat(double lat)
        {
            return -0.000000487305676 * Math.pow(lat, 4)
                - 0.0033668574 * Math.pow(lat, 3)
                + 0.4601181791 * lat * lat
                - 1.4558127346 * lat + 110579.25662316;
        }

        private static double distPerLon(double lat)
        {
            return 0.0003121092 * Math.pow(lat, 4)
                + 0.0101182384 * Math.pow(lat, 3)
                - 17.2385140059 * lat * lat
                + 5.5485277537 * lat + 111301.967182595;
        }
    }
    // end of checkSectionTrack() and sub-routines

    /********************************************************************************
     * The seven functions below are for exporting and importing of (database) files.
     * They've been put here because no database should be left open at this point.
     */
    // Exports DB to Documents/TransektCount/transektcount_yyyy-MM-dd_HHmmss.db
    //   supplemented with date and time in filename
    // outfile ->
    //   /storage/emulated/0/Documents/TransektCount/transektcount_yyyy-MM-dd_HHmmss.db
    @SuppressLint({"SdCardPath", "LongLogTag"})
    public void exportDb()
    {
        /*
         outfile -> /storage/emulated/0/Documents/TransektCount/transektcount_yyyy-MM-dd_HHmmss.db

         1. Solution for Android >= 10 (Q)
         path = new File(Environment.getExternalStorageDirectory() + "/Documents/TransektCount");

         2. Solution for Android < 10 (deprecated in Q)
         path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
           + "/TransektCount");
        */
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TransektCount");
        }
        else
        {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TransektCount");
        }
        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // Verify path

        // outfile -> /storage/emulated/0/Documents/TransektCount/transektcount_yyyy-MM-dd_HHmmss.db
        outfile = new File(path, "/transektcount_" + getcurDate() + ".db");

        // infile <- /data/data/com.wmstein.transektcount/databases/transektcount.db
        String inPath = getApplicationContext().getFilesDir().getPath();
        inPath = inPath.substring(0, inPath.lastIndexOf("/"))
            + "/databases/transektcount.db";
        infile = new File(inPath);

        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        }
        else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
        {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        }
        else
        {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        if ((!mExternalStorageAvailable) || (!mExternalStorageWriteable))
        {
            showSnackbarRed(getString(R.string.noCard));
        }
        else
        {
            // export the db
            try
            {
                copy(infile, outfile);
                showSnackbar(getString(R.string.saveDB));
            } catch (IOException e)
            {
                showSnackbarRed(getString(R.string.saveFail));
            }
        }
    }
    // end of exportDb()

    /********************************************************************************************/
    // Exports DB contents as transektcount_yyyy-MM-dd_HHmmss.csv-file to Documents/TransektCount/
    //   with purged data set
    // Spreadsheet programs can import this csv file with
    //   - Unicode UTF-8 filter,
    //   - comma delimiter and
    //   - "" for text recognition.
    @SuppressLint({"SdCardPath", "LongLogTag"})
    public void exportDb2CSV()
    {
        // outfile -> /storage/emulated/0/Documents/TransektCount/transektcount_yyyy-MM-dd_HHmmss.csv
        //
        // 1. Alternative for Android >= 10 (Q):
        //    path = new File(Environment.getExternalStorageDirectory() + "/Documents/TransektCount");
        //
        // 2. Alternative for Android < 10 (deprecated in Q):
        //    path = new File(Environment.getExternalStoragePublicDirectory
        //    (Environment.DIRECTORY_DOCUMENTS) + "/TransektCount");
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TransektCount");
        }
        else
        {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TransektCount");
        }

        path.mkdirs(); // just verify path, result ignored
        outfile = new File(path, "/transektcount_" + getcurDate() + ".csv");

        Section section;
        String sectName;
        String sectNotes;
        int sect_id;

        Meta meta;
        String transNo, inspecName;
        int temps, tempe, winds, winde, clouds, cloude;
        int summf = 0, summ = 0, sumf = 0, sump = 0, suml = 0, sumo = 0;
        int total, sumSpec;
        String date, start_tm, end_tm, kw;
        int yyyy, mm, dd;
        int Kw = 0; // calendar week

        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        }
        else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
        {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        }
        else
        {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        if ((!mExternalStorageAvailable) || (!mExternalStorageWriteable))
        {
            showSnackbarRed(getString(R.string.noCard));
        }
        else
        {
            // export purged db as csv
            dbHandler = new DbHelper(this);
            database = dbHandler.getWritableDatabase();
            headDataSource = new HeadDataSource(this);
            headDataSource.open();
            MetaDataSource metaDataSource = new MetaDataSource(this);
            metaDataSource.open();
            SectionDataSource sectionDataSource = new SectionDataSource(this);
            sectionDataSource.open();
            CountDataSource countDataSource = new CountDataSource(this);
            countDataSource.open();

            // get number of different species
            sumSpec = countDataSource.getDiffSpec();

            /*********************************/
            // start creating csv table output
            try
            {
                CSVWriter csvWrite = new CSVWriter(new FileWriter(outfile));

                // set header according to table representation in MS Excel
                String[] arrCol =
                    {
                        getString(R.string.transectnumber),
                        getString(R.string.inspector),
                        getString(R.string.date),
                        "",
                        "",
                        getString(R.string.timehead),
                        getString(R.string.temperature),
                        getString(R.string.wind),
                        getString(R.string.clouds),
                        "",
                        getString(R.string.kal_w),
                    };
                csvWrite.writeNext(arrCol); // write line to csv-file

                // open Head table for head info
                head = headDataSource.getHead();
                transNo = head.transect_no;
                inspecName = head.inspector_name;

                // open Meta table for meta info
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

                // Calculating the week of the year (ISO 8601)
                Calendar cal = Calendar.getInstance();

                assert date != null;
                if (!date.equals(""))
                {
                    String language = Locale.getDefault().toString().substring(0, 2);
                    if (language.equals("de"))
                    {
                        try
                        {
                            yyyy = Integer.parseInt(date.substring(6, 10));
                            mm = Integer.parseInt(date.substring(3, 5));
                            dd = Integer.parseInt(date.substring(0, 2));
                        } catch (Exception e)
                        {
                            // wrong date format (English DB in German), use
                            yyyy = Integer.parseInt(date.substring(0, 4));
                            mm = Integer.parseInt(date.substring(5, 7));
                            dd = Integer.parseInt(date.substring(8, 10));
                        }
                    }
                    else
                    {
                        try
                        {
                            yyyy = Integer.parseInt(date.substring(0, 4));
                            mm = Integer.parseInt(date.substring(5, 7));
                            dd = Integer.parseInt(date.substring(8, 10));
                        } catch (Exception e)
                        {
                            // wrong date format (German DB in English), use
                            yyyy = Integer.parseInt(date.substring(6, 10));
                            mm = Integer.parseInt(date.substring(3, 5));
                            dd = Integer.parseInt(date.substring(0, 2));
                        }
                    }

                    // cal.set(2017, 3, 9); // 09.04.2017
                    cal.set(yyyy, mm - 1, dd);
                    Kw = cal.get(Calendar.WEEK_OF_YEAR);
                }

                // main headline with
                //   transect no., inspector name, temperature, wind, clouds, date, start-time,
                //   end-time, calendar week
                kw = String.valueOf(Kw);
                String[] arrMeta =
                    {
                        transNo,
                        inspecName,
                        date,
                        "",
                        getString(R.string.from),
                        start_tm,
                        String.valueOf(temps),
                        String.valueOf(winds),
                        String.valueOf(clouds),
                        "",
                        kw,
                    };
                csvWrite.writeNext(arrMeta);

                String[] arrMeta1 =
                    {
                        "",
                        "",
                        "",
                        "",
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

                // species table headline with
                //   Species Name, Local Name, Code, Section, Section Note, Counts, Spec.-Note
                String[] arrCol1 =
                    {
                        getString(R.string.name_spec),
                        getString(R.string.name_spec_g),
                        getString(R.string.code_spec),
                        getString(R.string.name_sect),
                        getString(R.string.notes_sect),
                        getString(R.string.countImagomfHint),
                        getString(R.string.countImagomHint),
                        getString(R.string.countImagofHint),
                        getString(R.string.countPupaHint),
                        getString(R.string.countLarvaHint),
                        getString(R.string.countOvoHint),
                        getString(R.string.rem_spec)
                    };
                csvWrite.writeNext(arrCol1);

                /***********************************************************************/
                // build the internal species array according to the sorted species list
                Cursor curCSV;
                if ("codes".equals(sortPref))
                {
                    // cursor contains sorted by code list with all internal count entries > 0
                    curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                        + " order by " + DbHelper.C_CODE, null);
                }
                else
                {
                    // cursor contains sorted by name list with all internal count entries > 0
                    curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                        + " order by " + DbHelper.C_NAME, null);
                }

                // get the internal counts for the internal count area
                int countmf, countm, countf, countp, countl, counte;
                String strcountmf, strcountm, strcountf, strcountp, strcountl, strcounte;

                // get the internal counts for the internal count area
                curCSV.moveToFirst();
                while (!curCSV.isAfterLast())
                {
                    sect_id = curCSV.getInt(1);
                    section = sectionDataSource.getSection(sect_id);
                    sectName = section.name;
                    sectNotes = section.notes;

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

                    // show internal counts line only if there is a count
                    if (countmf + countm + countf + countp + countl + counte > 0)
                    {
                        String[] arrStr =
                            {
                                curCSV.getString(2),   //species name
                                curCSV.getString(17),  //species name_g
                                curCSV.getString(3),   //species code
                                sectName,              //section name
                                sectNotes,             //section note
                                strcountmf,            //count mf
                                strcountm,             //count m
                                strcountf,             //count f
                                strcountp,             //count p
                                strcountl,             //count l
                                strcounte,             //count e
                                curCSV.getString(16)   //notes
                            };
                        csvWrite.writeNext(arrStr);
                    }

                    summf = summf + curCSV.getInt(4);
                    summ = summ + curCSV.getInt(5);
                    sumf = sumf + curCSV.getInt(6);
                    sump = sump + curCSV.getInt(7);
                    suml = suml + curCSV.getInt(8);
                    sumo = sumo + curCSV.getInt(9);
                    curCSV.moveToNext();
                }
                curCSV.close();

                // empty row followed before external counts area
                csvWrite.writeNext(arrEmpt);
                csvWrite.close();
                showSnackbar(getString(R.string.savecsv));
            } catch (Exception e)
            {
                showSnackbarRed(getString(R.string.saveFail));
                if (MyDebug.LOG) Log.d(TAG, "1236, csv write internal failed");
            }

            /***********************************************************************/
            // build the external species array according to the sorted species list
            //   and append it to internal array by CSVWriter parameter 'true'
            try
            {
                CSVWriter csvWrite = new CSVWriter(new FileWriter(outfile, true));
                Cursor curCSVe;
                if ("codes".equals(sortPref))
                {
                    // cursor contains sorted by code list with all external count entries > 0
                    curCSVe = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE ("
                        + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                        + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                        + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                        + " order by " + DbHelper.C_CODE, null);
                }
                else
                {
                    // cursor contains sorted by name list with all external count entries > 0
                    curCSVe = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE ("
                        + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                        + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                        + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                        + " order by " + DbHelper.C_NAME, null);
                }

                // get the external counts for the external count area
                int countmfe, countme, countfe, countpe, countle, countee; // ext. counts
                String strcountmfe, strcountme, strcountfe, strcountpe, strcountle, strcountee;

                int totalmfe = 0, totalme = 0, totalfe = 0, totalpe = 0, totalle = 0, totalee = 0;
                String strtotalmfe, strtotalme, strtotalfe, strtotalpe, strtotalle, strtotalee;

                // get the external counts for the external count area
                int curCount;
                curCount = curCSVe.getCount();
                if (MyDebug.LOG) Log.d(TAG, "1277, curCSVe, curCount: " + curCount);

                if (curCount > 0) // build table only when there is any count at all
                {
                    curCSVe.moveToFirst();
                    String code;       // current species code
                    String code1 = ""; // initial species code
                    if (isNotBlank(curCSVe.getString(3)))
                        code1 = curCSVe.getString(3);
                    if (MyDebug.LOG) Log.d(TAG, "1286, curCSVe, code1: " + code1);

                    boolean cDiff;
                    boolean firstCnt = true; // needed to write the first counts of the external species
                    while (!curCSVe.isAfterLast())
                    {
                        // read code of current position
                        code = curCSVe.getString(3); //species code
                        if (MyDebug.LOG) Log.d(TAG, "1294, while curCSVe, code: " + code
                            + ", code1: " + code1);

                        countmfe = countDataSource.getMFEWithCode(code);
                        if (countmfe > 0)
                            strcountmfe = Integer.toString(countmfe);
                        else
                            strcountmfe = "";

                        countme = countDataSource.getMEWithCode(code);
                        if (countme > 0)
                            strcountme = Integer.toString(countme);
                        else
                            strcountme = "";

                        countfe = countDataSource.getFEWithCode(code);
                        if (countfe > 0)
                            strcountfe = Integer.toString(countfe);
                        else
                            strcountfe = "";

                        countpe = countDataSource.getPEWithCode(code);
                        if (countpe > 0)
                            strcountpe = Integer.toString(countpe);
                        else
                            strcountpe = "";

                        countle = countDataSource.getLEWithCode(code);
                        if (countle > 0)
                            strcountle = Integer.toString(countle);
                        else
                            strcountle = "";

                        countee = countDataSource.getEEWithCode(code);
                        if (countee > 0)
                            strcountee = Integer.toString(countee);
                        else
                            strcountee = "";

                        // check for writing the external count line
                        cDiff = !Objects.equals(code, code1);
                        if (MyDebug.LOG) Log.d(TAG, "1335, curCSVe, cDiff: " + cDiff
                            + ", code: " + code + ", code1: " + code1);

                        code1 = code;

                        // show external counts line only if there is a count
                        if ((cDiff || firstCnt) && (countmfe + countme + countfe + countpe + countle + countee > 0))
                        {
                            // show external counts section as "External"
                            String sectName1 = getString(R.string.external);
                            String[] arrStrExt =
                                {
                                    curCSVe.getString(2),   //species name
                                    curCSVe.getString(17),  //species name_g
                                    code,                  //species code
                                    sectName1,             //section name
                                    "",                    //section note
                                    strcountmfe,           //count mfe
                                    strcountme,            //count me
                                    strcountfe,            //count fe
                                    strcountpe,            //count pe
                                    strcountle,            //count le
                                    strcountee,            //count ee
                                    curCSVe.getString(16)   //notes
                                };
                            csvWrite.writeNext(arrStrExt);

                            totalmfe = totalmfe + countmfe;
                            totalme = totalme + countme;
                            totalfe = totalfe + countfe;
                            totalpe = totalpe + countpe;
                            totalle = totalle + countle;
                            totalee = totalee + countee;
                        }
                        firstCnt = false;
                        curCSVe.moveToNext();
                    }
                }
                curCSVe.close();
                if (MyDebug.LOG) Log.d(TAG, "1374, ext. totals (mf,m,f,p,l,e): "
                    + totalmfe + ", " + totalme + ", " + totalfe + ", "
                    + totalpe + ", " + totalle + ", " + totalee);

                /********************************/
                // Empty row followed by sum area
                String[] arrEmpt = {};
                csvWrite.writeNext(arrEmpt);

                // Counts, Total headline
                String[] arrCol2 =
                    {
                        "", "", "", "", "",
                        getString(R.string.countImagomfHint),
                        getString(R.string.countImagomHint),
                        getString(R.string.countImagofHint),
                        getString(R.string.countPupaHint),
                        getString(R.string.countLarvaHint),
                        getString(R.string.countOvoHint),
                        getString(R.string.hintTotal)
                    };
                csvWrite.writeNext(arrCol2);

                int totali = summf + summ + sumf + sump + suml + sumo;
                int totale = totalmfe + totalme + totalfe + totalpe + totalle + totalee;
                total = totali + totale;

                String strsummf, strsumm, strsumf, strsump, strsuml, strsumo;
                String strtotali, strtotale, strtotal;

                //internal sums
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

                // write internal total sum
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
                        strtotali
                    };
                csvWrite.writeNext(arrSumi);

                // external sums
                if (totalmfe > 0)
                    strtotalmfe = Integer.toString(totalmfe);
                else
                    strtotalmfe = "";

                if (totalme > 0)
                    strtotalme = Integer.toString(totalme);
                else
                    strtotalme = "";

                if (totalfe > 0)
                    strtotalfe = Integer.toString(totalfe);
                else
                    strtotalfe = "";

                if (totalpe > 0)
                    strtotalpe = Integer.toString(totalpe);
                else
                    strtotalpe = "";

                if (totalle > 0)
                    strtotalle = Integer.toString(totalle);
                else
                    strtotalle = "";

                if (totalee > 0)
                    strtotalee = Integer.toString(totalee);
                else
                    strtotalee = "";

                if (totale > 0)
                    strtotale = Integer.toString(totale);
                else
                    strtotale = "";

                // write external total sum
                String[] arrSume =
                    {
                        "", "", "", "",
                        getString(R.string.sume),
                        strtotalmfe,
                        strtotalme,
                        strtotalfe,
                        strtotalpe,
                        strtotalle,
                        strtotalee,
                        strtotale
                    };
                csvWrite.writeNext(arrSume);

                // totals
                String strtotalsmfe, strtotalsme, strtotalsfe, strtotalspe, strtotalsle, strtotalsee;
                int totalsmfe = totalmfe + summf;
                if (totalsmfe > 0)
                    strtotalsmfe = Integer.toString(totalsmfe);
                else
                    strtotalsmfe = "";

                int totalsme = totalme + summ;
                if (totalsme > 0)
                    strtotalsme = Integer.toString(totalsme);
                else
                    strtotalsme = "";

                int totalsfe = totalfe + sumf;
                if (totalsfe > 0)
                    strtotalsfe = Integer.toString(totalsfe);
                else
                    strtotalsfe = "";

                int totalspe = totalpe + sump;
                if (totalspe > 0)
                    strtotalspe = Integer.toString(totalspe);
                else
                    strtotalspe = "";

                int totalsle = totalle + suml;
                if (totalsle > 0)
                    strtotalsle = Integer.toString(totalsle);
                else
                    strtotalsle = "";

                int totalsee = totalee + sumo;
                if (totalsee > 0)
                    strtotalsee = Integer.toString(totalsee);
                else
                    strtotalsee = "";

                if (total > 0)
                    strtotal = Integer.toString(total);
                else
                    strtotal = "";

                // write total sum
                String[] arrTotal =
                    {
                        "", "", "", "",
                        getString(R.string.sum_total),
                        strtotalsmfe,
                        strtotalsme,
                        strtotalsfe,
                        strtotalspe,
                        strtotalsle,
                        strtotalsee,
                        strtotal
                    };
                csvWrite.writeNext(arrTotal);

                csvWrite.close();
                showSnackbar(getString(R.string.savecsv));
            } catch (Exception e)
            {
                showSnackbarRed(getString(R.string.saveFail));
                if (MyDebug.LOG) Log.d(TAG, "1571, csv write external failed");
            }
            headDataSource.close();
            metaDataSource.close();
            sectionDataSource.close();
            countDataSource.close();
            dbHandler.close();
        }
    }
    // end of exportDb2CSV()

    /**********************************************************************************************/
    // Exports Basis DB to Documents/TransektCount/transektcount0.db
    @SuppressLint({"SdCardPath", "LongLogTag"})
    public void exportBasisDb()
    {
        // infile <- /data/data/com.wmstein.transektcount/databases/transektcount.db
        String inPath = getApplicationContext().getFilesDir().getPath();
        inPath = inPath.substring(0, inPath.lastIndexOf("/")) + "/databases/transektcount.db";
        infile = new File(inPath);

        // tmpfile -> /data/data/com.wmstein.transektcount/files/transektcount_tmp.db
        String tmpPath = getApplicationContext().getFilesDir().getPath();
        tmpPath = tmpPath.substring(0, tmpPath.lastIndexOf("/")) + "/files/transektcount_tmp.db";
        File tmpfile = new File(tmpPath);

        // outfile -> /storage/emulated/0/Documents/TransektCount/transektcount0.db
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TransektCount");
        }
        else
        {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TransektCount");
        }

        path.mkdirs(); // just verify path, result ignored
        outfile = new File(path, "/transektcount0.db");

        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        }
        else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
        {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        }
        else
        {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        if ((!mExternalStorageAvailable) || (!mExternalStorageWriteable))
        {
            showSnackbarRed(getString(R.string.noCard));
        }
        else
        {
            // export the basic db
            try
            {
                // save current db as backup db tmpfile
                copy(infile, tmpfile);

                // clear DB values for basic DB
                clearDBValues();

                // write Basis DB
                copy(infile, outfile);

                // restore actual db from tmpfile
                copy(tmpfile, infile);

                // delete backup db
                boolean d0 = tmpfile.delete();
                if (d0)
                    showSnackbar(getString(R.string.saveBasisDB));
            } catch (IOException e)
            {
                showSnackbarRed(getString(R.string.saveFail));
            }
        }
    }

    /**********************************************************************************************/
    // Clear all relevant DB values, reset to basic DB
    public void resetToBasisDb()
    {
        // confirm dialogue before anything else takes place
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmResetDB);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.deleteButton, (dialog, id) ->
        {
            boolean r_ok = clearDBValues();
            if (r_ok)
                showSnackbar(getString(R.string.reset2basic));
        });
        builder.setNegativeButton(R.string.importCancelButton, (dialog, id) -> dialog.cancel());
        alert = builder.create();
        alert.show();
    }

    // clear DB values for basic DB
    @SuppressLint({"LongLogTag"})
    public boolean clearDBValues()
    {
        // clear values in DB
        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();
        boolean r_ok = true; // gets false when reset fails

        try
        {
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
                + DbHelper.M_END_TM + " = '';";
            database.execSQL(sql);

            sql = "DELETE FROM " + DbHelper.ALERT_TABLE;
            database.execSQL(sql);

            dbHandler.close();
        } catch (Exception e)
        {
            showSnackbarRed(getString(R.string.resetFail));
            r_ok = false;
        }
        return r_ok;
    }

    /**********************************************************************************************/
    @SuppressLint("SdCardPath")
    // Choose a db-file to load and set it to transektcount.db
    // based on android-file-chooser from Google Code Archive.
    // Created by wmstein
    public void importDBFile()
    {
        ArrayList<String> extensions = new ArrayList<>();
        extensions.add(".db");
        String filterFileName = "transektcount";
        infile = null;

        Intent intent;
        intent = new Intent(this, AdvFileChooser.class);
        intent.putStringArrayListExtra("filterFileExtension", extensions);
        intent.putExtra("filterFileName", filterFileName);
        myActivityResultLauncher.launch(intent);

        // outfile -> /data/data/com.wmstein.transektcount/databases/transektcount.db
        String destPath = getApplicationContext().getFilesDir().getPath();
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases/transektcount.db";
        outfile = new File(destPath);

        // confirm dialogue before importing
        // with short delay to get the file name before the dialog appears
        mHandler.postDelayed(() ->
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(R.string.confirmDBImport);
            builder.setCancelable(false).setPositiveButton(R.string.importButton, (dialog, id) ->
            {
                try
                {
                    copy(infile, outfile);

                    // make sure that import of a DB file sets sectionHasTrack correctly
                    try
                    {
                        trackDataSource = new TrackDataSource(this);
                        trackDataSource.open();
                        sectionHasTrack = trackDataSource.getHasTrack();
                        trackDataSource.close();
                        if (MyDebug.LOG)
                            Log.i(TAG, "1780, importDBFile, hasTrack: " + sectionHasTrack);

                        editor = prefs.edit();
                        editor.putBoolean("section_has_track", sectionHasTrack);
                        editor.commit();
                        showSnackbar(getString(R.string.importWin));
                    } catch (SQLiteException e)
                    {
                        trackDataSource.close();
                        showSnackbarRed(getString(R.string.corruptDb));
                    }

                    headDataSource = new HeadDataSource(getApplicationContext());
                    headDataSource.open();
                    head = headDataSource.getHead();
                    headDataSource.close();

                    // set transect number as title
                    try
                    {
                        Objects.requireNonNull(getSupportActionBar()).setTitle(head.transect_no);
                    } catch (NullPointerException e)
                    {
                        // nothing
                    }

                } catch (IOException e)
                {
                    showSnackbarRed(getString(R.string.importFail));
                }
            });
            builder.setNegativeButton(R.string.importCancelButton, (dialog, id) -> dialog.cancel());
            alert = builder.create();
            alert.show();
        }, 100);
    }

    // Function processes the result of AdvFileChooser
    final ActivityResultLauncher<Intent> myActivityResultLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<>()
        {
            @Override
            public void onActivityResult(ActivityResult result)
            {
                if (result.getResultCode() == Activity.RESULT_OK)
                {
                    Intent data = result.getData();
                    // Following the operation
                    assert data != null;
                    selectedFile = data.getStringExtra("fileSelected");
                    if (MyDebug.LOG)
                    {
                        showSnackbar("Selected file: " + selectedFile);
                    }
                    assert selectedFile != null;
                    infile = new File(selectedFile);
                }
            }
        });

    /**********************************************************************************************/
    @SuppressLint({"SdCardPath"})
    // Import of the basic DB, modified by wmstein
    public void importBasisDb()
    {
        // infile <- /storage/emulated/0/Documents/TransektCount/transektcount0.db
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TransektCount");
        }
        else
        {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TransektCount");
        }
        infile = new File(path, "/transektcount0.db");

        // outfile -> /data/data/com.wmstein.transektcount/databases/transektcount.db
        String destPath = getApplicationContext().getFilesDir().getPath();
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases/transektcount.db";
        outfile = new File(destPath);
        if (!(infile.exists()))
        {
            showSnackbar(getString(R.string.noDb));
            return;
        }

        // confirm dialogue before importing
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmBasisImport);
        builder.setCancelable(false).setPositiveButton(R.string.importButton, (dialog, id) ->
        {
            try
            {
                copy(infile, outfile);

                // make sure that import of the basic DB file sets sectionHasTrack correctly
                try
                {
                    trackDataSource = new TrackDataSource(this);
                    trackDataSource.open();
                    sectionHasTrack = trackDataSource.getHasTrack();
                    trackDataSource.close();
                    if (MyDebug.LOG)
                        Log.i(TAG, "1888, importBasisDb, hasTrack: " + sectionHasTrack);

                    editor = prefs.edit();
                    editor.putBoolean("section_has_track", sectionHasTrack);
                    editor.commit();
                    showSnackbar(getString(R.string.importWin));
                } catch (SQLiteException e)
                {
                    trackDataSource.close();
                    showSnackbarRed(getString(R.string.corruptDb));
                }

                headDataSource = new HeadDataSource(getApplicationContext());
                headDataSource.open();
                head = headDataSource.getHead();
                headDataSource.close();

                // set transect number as title
                try
                {
                    Objects.requireNonNull(getSupportActionBar()).setTitle(head.transect_no);
                } catch (NullPointerException e)
                {
                    // nothing
                }
            } catch (IOException e)
            {
                showSnackbarRed(getString(R.string.importFail));
            }
        }).setNegativeButton(R.string.importCancelButton, (dialog, id) -> dialog.cancel());
        alert = builder.create();
        alert.show();
    }

    /**********************************************************************************************/
    // copy file block-wise
    public static void copy(File src, File dst) throws IOException
    {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0)
        {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    /*******************************************************************
     * Select and import gpx-file to store track coords into TRACK_TABLE
     */
    public void importGPX()
    {
        ArrayList<String> extensions = new ArrayList<>();
        extensions.add(".gpx");
        String filterFileName = "transektcount";
        infile = null;
        selectedFile = null;

        Intent intent;
        intent = new Intent(this, AdvFileChooser.class);
        intent.putStringArrayListExtra("filterFileExtension", extensions);
        intent.putExtra("filterFileName", filterFileName);
        myActivityResultLauncher.launch(intent);

        // confirm dialogue before importing
        // with short delay to get the file name before the dialog appears
        mHandler.postDelayed(() ->
        {
            StringBuilder gpxsb = new StringBuilder();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(R.string.confirmGPXImport);
            builder.setCancelable(false).setPositiveButton(R.string.importButton, (dialog, id) ->
            {
                Toast.makeText(getApplicationContext(), getString(R.string.waitImport), Toast.LENGTH_SHORT).show();
                try
                {
                    FileInputStream fileIS;
                    String gpxLine;
                    fileIS = new FileInputStream(selectedFile);
                    BufferedReader xmlBR = new BufferedReader(new InputStreamReader(fileIS));

                    while ((gpxLine = xmlBR.readLine()) != null)
                    {
                        gpxsb.append(gpxLine).append('\n');
                    }
                    fileIS.close();
                } catch (IOException e)
                {
                    if (MyDebug.LOG)
                        Log.e(TAG, "1984, importGPX, Problem converting Stream to String: " + e);
                }

                gpxString = gpxsb.toString();

                // Parse gpxString to write fields into TRACK_TABLE
                trackDataSource = new TrackDataSource(getApplicationContext());
                trackDataSource.open();
                sectionDataSource = new SectionDataSource(getApplicationContext());
                sectionDataSource.open();
                if (MyDebug.LOG)
                    Log.i(TAG, "1995, importGPX, Datasources open");

                strStart = gpxString.indexOf("<trk>"); // start of 1. track

                if (gpxString.contains("<trk>")) // test for track data
                {
                    // reduce gpxString to all tracks <trk> ... </gpx>
                    gpxString = gpxString.substring(strStart); // string with all tracks
                    if (MyDebug.LOG)
                        Log.i(TAG, "2004, importGPX, gpxString total: " + gpxString);

                    // do in background
//                    new Thread(new Runnable()
//                    {
//                        @Override
//                        public void run()
//                        {
                    // For each track
                    do
                    {
                        // get data of indexed track
                        trkStart = gpxString.indexOf("<trk>");
                        trkEnd = gpxString.indexOf("</trk>");
                        gpxTrkString = gpxString.substring(trkStart, trkEnd + 6);
                        if (MyDebug.LOG)
                            Log.i(TAG, "2020, importGPX, gpxTrkString: " + gpxTrkString);

                        // get track name as section name
                        if (gpxTrkString.contains("<name>"))
                        {
                            section = sectionDataSource.getSection(trk);
                            tSecName = section.name; // name track same as section

                            // reduce gpxString to rest after </trk>
                            strEnd = gpxString.indexOf("</trk>");
                            // offset = length of "</trk>" = 6
                            gpxString = gpxString.substring(strEnd + 6);
                        }

                        if (gpxTrkString.contains("<trkseg>"))
                        {
                            // for each track point in trkseg
                            do
                            {
                                if (MyDebug.LOG)
                                    Log.i(TAG, "2040, importGPX, do trackpt");
                                int nextTp; // index for next track point (after />)
                                strStart = gpxTrkString.indexOf("lat=") + 5;
                                strEnd = gpxTrkString.indexOf("lat=") + 13;
                                String tlat = gpxTrkString.substring(strStart, strEnd);

                                strStart = gpxTrkString.indexOf("lon=") + 5;
                                strEnd = gpxTrkString.indexOf("lon=") + 14;
                                String tlon = gpxTrkString.substring(strStart, strEnd);
                                if (MyDebug.LOG)
                                    Log.i(TAG, "2050, importGPX, tSecName: "
                                        + tSecName + ", " + tlat + ", " + tlon);
                                trackDataSource.createTrackTp(tSecName, tlat, tlon);  // !!!

                                // increment gpxTrkString line
                                trkpt = trkpt + 1;

                                // reduce gpxTrkString to remainder after current trkpt line
                                nextTp = gpxTrkString.indexOf("/>");
                                gpxTrkString = gpxTrkString.substring(nextTp + 2);
                                if (MyDebug.LOG)
                                    Log.i(TAG, "2061, importGPX, trackpt " + trkpt);
                            } while (gpxTrkString.contains("<trkpt"));
                        }

                        // reduce gpxString for next track segment
                        trk = trk + 1;
                    } while (gpxString.contains("<trk>"));

//                        }
//                    }).start();
                    if (MyDebug.LOG)
                        Log.i(TAG, "2072, importGPX, gpxString finished: " + gpxString);

                    sectionHasTrack = true;
                    editor = prefs.edit();
                    editor.putBoolean("section_has_track", sectionHasTrack);
                    editor.apply();
                }
                trackDataSource.close();
                sectionDataSource.close();

                showSnackbar(getString(R.string.importGPX));
            }).setNegativeButton(R.string.importCancelButton, (dialog, id) -> dialog.cancel());

            alert = builder.create();
            alert.show();
        }, 100);
    }

    /****************************************/
    // Delete all track info from TRACK_TABLE
    public void deleteGPX()
    {
        // confirm dialogue before anything else takes place
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmGPXDelete);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.deleteButton, (dialog, id) ->
        {
            boolean r_ok = clearGPXValues();
            if (r_ok)
            {
                // switch autoSection off
                sectionHasTrack = false;
                autoSection = false;
                editor = prefs.edit();
                editor.putBoolean("pref_auto_section", autoSection);
                editor.putBoolean("section_has_track", sectionHasTrack);
                editor.apply();
                showSnackbar(getString(R.string.resetTracks));
            }
        });
        builder.setNegativeButton(R.string.importCancelButton, (dialog, id) -> dialog.cancel());
        alert = builder.create();
        alert.show();
    }

    // Clear track coordinates from TRACK_TABLE
    public boolean clearGPXValues()
    {
        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();
        boolean r_ok = true; // gets false when reset fails

        try
        {
            String sql = "DELETE FROM " + DbHelper.TRACK_TABLE;
            database.execSQL(sql);

            dbHandler.close();
        } catch (Exception e)
        {
            showSnackbarRed(getString(R.string.resetFail));
            r_ok = false;
        }
        return r_ok;
    }

    private void showSnackbar(String str) // green text
    {
        View view = findViewById(R.id.baseLayout);
        Snackbar sB = Snackbar.make(view, str, Snackbar.LENGTH_LONG);
        sB.setTextColor(Color.GREEN);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        sB.show();
    }

    private void showSnackbarRed(String str) // bold red text
    {
        View view = findViewById(R.id.baseLayout);
        Snackbar sB = Snackbar.make(view, str, Snackbar.LENGTH_LONG);
        sB.setTextColor(RED);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        sB.show();
    }

    /*******************************************************************************
     * Checks if a CharSequence is not empty (""), not null and not whitespace only.
     * <p>
     * isNotBlank(null)      = false
     * isNotBlank("")        = false
     * isNotBlank(" ")       = false
     * isNotBlank("bob")     = true
     * isNotBlank("  bob  ") = true
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is
     * not empty and not null and not whitespace
     */
    public static boolean isNotBlank(final CharSequence cs)
    {
        return !isBlank(cs);
    }

    private static boolean isBlank(final CharSequence cs)
    {
        int strLen = cs.length();
        if (strLen == 0)
            return true;
        for (int i = 0; i < strLen; i++)
        {
            if (!Character.isWhitespace(cs.charAt(i)))
                return false;
        }
        return true;
    }

}