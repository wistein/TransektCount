package com.wmstein.transektcount;

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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.core.text.HtmlCompat;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**********************************************************************
 * WelcomeActivity provides the starting page with menu and buttons for
 * import/export/help/info methods and lets you call
 * EditMetaActivity, SelectSectionActivity and ShowResultsActivity.
 * It uses further the PermissionDialogFragment.
 * <p>
 * Database handling is mainly done in WelcomeActivity as upgrade to current
 * DB version when importing an older DB file by importDBFile().
 * <p>
 * Based on BeeCount's WelcomeActivity.java by Milo Thurston from 2014-05-05.
 * Changes and additions for TransektCount by wmstein since 2016-02-18,
 * last edited on 2025-07-12
 */
public class WelcomeActivity
        extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "WelcomeAct";

    private TransektCountApplication transektCount;

    private ChangeLog cl;
    public boolean doubleBackToExitPressedTwice = false;

    // Import/export stuff
    private File inFile = null;
    private File outFile = null;
    boolean mExternalStorageWriteable = false;
    private final String sState = Environment.getExternalStorageState();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private AlertDialog alert;
    private String transNo = "";

    // Preferences
    private SharedPreferences prefs;
    private String outPref;

    // Permissions
    private boolean storagePermGranted = false; // initial storage permission state

    // DB handling
    private SQLiteDatabase database;
    private DbHelper dbHandler;
    private HeadDataSource headDataSource;
    private Head head;
    private SectionDataSource sectionDataSource;
    private MetaDataSource metaDataSource;
    private CountDataSource countDataSource;
    private AlertDataSource alertDataSource;

    private View baseLayout;
    private String mesg;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (MyDebug.DLOG) Log.d(TAG, "133, onCreate");

        transektCount = (TransektCountApplication) getApplication();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Get preferences
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);

        // Proximity sensor handling in preferences menu
        SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        // Grey out preferences menu item pref_prox when max. proximity sensitivity = null
        boolean prefProx = mProximity != null;

        // Set pref_prox enabler, used in SettingsFragment
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("enable_prox", prefProx);
        editor.apply();

        // Set DarkMode when system is in BrightMode
        int nightModeFlags = Configuration.UI_MODE_NIGHT_MASK;
        int confUi = getResources().getConfiguration().uiMode;
        if ((nightModeFlags & confUi) == Configuration.UI_MODE_NIGHT_NO)
        {
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

        // Check initial storage permission state and provide dialog
        isStoragePermGranted();
        if (!storagePermGranted) // in self permission
        {
            PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                    PermissionsStorageDialogFragment.class.getName());

            mesg = getString(R.string.storage_perm_denied);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
        }
        if (MyDebug.DLOG) Log.d(TAG, "195, onCreate, storagePermGranted: " + storagePermGranted);

        // setup the data sources
        headDataSource = new HeadDataSource(this);
        sectionDataSource = new SectionDataSource(this);
        metaDataSource = new MetaDataSource(this);
        countDataSource = new CountDataSource(this);
        alertDataSource = new AlertDataSource(this);

        // Get transect No. and check for DB integrity
        try {
            headDataSource.open();
            head = headDataSource.getHead();
            transNo = head.transect_no; // read and test for DB integrity
            headDataSource.close();
        } catch (SQLiteException e) {
            headDataSource.close();
            mesg = getString(R.string.corruptDb);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
            mHandler.postDelayed(this::finishAndRemoveTask, 2000);
        }

        cl = new ChangeLog(this, prefs);

        // Show changelog for new version
        if (cl.firstRun())
            cl.getLogDialog().show();

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
            exportBasisDb(); // create directory and copy internal DB-data to initial Basis DB-file

        // New onBackPressed logic
        // Different Navigation Bar modes and layouts:
        // - Classic three-button navigation: NavBarMode = 0
        // - Two-button navigation (Android P): NavBarMode = 1
        // - Full screen gesture mode (Android Q): NavBarMode = 2
        // Use onBackPressed logic only if NavBarMode = 0 or 1
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

        // iMode = 0: 3-button, = 1: 2-button, = 2: gesture
        int iMode = resourceId > 0 ? resources.getInteger(resourceId) : 0;
        if (MyDebug.DLOG) Log.i(TAG, "265, NavBarMode = " + iMode);
        return iMode;
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
                    finish();
                    remove();
                } else {
                    doubleBackToExitPressedTwice = true;
                    mesg = getString(R.string.back_twice);
                    Toast.makeText(getApplicationContext(),
                            HtmlCompat.fromHtml("<font color='blue'>" + mesg + "</font>",
                                    HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
                    m1Handler.postDelayed(r1, 1500);
                }
            }
        };
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onResume() {
        super.onResume();

        if (MyDebug.DLOG) Log.d(TAG, "299, onResume");

        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        outPref = prefs.getString("pref_csv_out", "species"); // sort mode csv-export

        isStoragePermGranted(); // set storagePermGranted from self permission
        if (MyDebug.DLOG) Log.d(TAG, "306, onResume, storagePermGranted: " + storagePermGranted);

        headDataSource.open();
        sectionDataSource.open();
        metaDataSource.open();
        countDataSource.open();
        alertDataSource.open();

        baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(transektCount.setBackgr());

        // Set transect name as title
        head = headDataSource.getHead();
        transNo = head.transect_no; // read and test for DB integrity
        try {
            Objects.requireNonNull(getSupportActionBar()).setTitle(transNo);
        } catch (NullPointerException e) {
            // nothing
        }
    }
    // End of onResume()

    // Check external storage self permission
    private void isStoragePermGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) // Android >= 11
        {
            // check permission MANAGE_EXTERNAL_STORAGE for Android >= 11
            storagePermGranted = Environment.isExternalStorageManager();
        } else {
            storagePermGranted = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.welcome, menu);
        MenuCompat.setGroupDividerEnabled(menu, true); // Show dividers in menu
        return true;
    }

    // Handle action bar item clicks here. The action bar will automatically handle clicks on
    // the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
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
                    mesg = getString(R.string.storage_perm_denied);
                    Toast.makeText(this,
                            HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                    HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
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
                    mesg = getString(R.string.storage_perm_denied);
                    Toast.makeText(this,
                            HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                    HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                }
            }
            return true;
        } else if (id == R.id.exportBasisMenu) {
            if (storagePermGranted) {
                exportBasisDb();
            } else {
                PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                        PermissionsStorageDialogFragment.class.getName());
                if (storagePermGranted) {
                    exportBasisDb();
                } else {
                    mesg = getString(R.string.storage_perm_denied);
                    Toast.makeText(this,
                            HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                    HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
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
                    mesg = getString(R.string.storage_perm_denied);
                    Toast.makeText(this,
                            HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                    HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                }
            }
            return true;
        } else if (id == R.id.importBasisMenu) {
            headDataSource.close();
            sectionDataSource.close();
            metaDataSource.close();
            countDataSource.close();
            alertDataSource.close();

            importBasisDb();
            return true;
        } else if (id == R.id.importFileMenu) {
            headDataSource.close();
            sectionDataSource.close();
            metaDataSource.close();
            countDataSource.close();
            alertDataSource.close();

            inFile = null;
            importDBFile();
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
        } else if (id == R.id.selectSection) {
            // Call SelectSectionActivity to select section for counting
            startActivity(new Intent(this, SelectSectionActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        } else if (id == R.id.editMeta) {
            // Call EditMetaActivity
            startActivity(new Intent(this, EditMetaActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        } else if (id == R.id.viewSpecies) {
            mesg = getString(R.string.wait);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();

            // Call ShowResultsActivity with trick: Pause for 100 msec to show toast
            mHandler.postDelayed(() ->
                    startActivity(new Intent(getApplicationContext(), ShowResultsActivity
                            .class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)), 100);
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
        storagePermGranted = prefs.getBoolean("permStor_Given", false);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (MyDebug.DLOG) Log.d(TAG, "498, onPause");

        headDataSource.close();
        sectionDataSource.close();
        metaDataSource.close();
        countDataSource.close();
        alertDataSource.close();

        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (MyDebug.DLOG) Log.d(TAG, "513, onStop");
        baseLayout.invalidate();
    }


    public void onDestroy() {
        super.onDestroy();

        if (MyDebug.DLOG) Log.d(TAG, "521, onDestroy");
    }

    // Start CountingActivity
    public void selectSection(View view) {
        // Start SelectSectionActivity to select section to be used for counting
        Intent intent;
        intent = new Intent(WelcomeActivity.this, SelectSectionActivity.class);
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void editMeta(View view) {
        startActivity(new Intent(this, EditMetaActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void viewSpecies(View view) {
        mesg = getString(R.string.wait);
        Toast.makeText(this,
                HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();

        // Trick: Pause for 100 msec to show toast
        mHandler.postDelayed(() ->
                startActivity(new Intent(getApplicationContext(), ShowResultsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)), 100);
    }

    // Date for filename of exported data
    private static String getcurDate() {
        Date date = new Date();
        @SuppressLint("SimpleDateFormat")
        DateFormat dform = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return dform.format(date);
    }

    /*********************************************************************************
     * The next three functions below are for importing data files.
     * They've been put here because no database should be left open at this point.
     */
    // Import the basic DB
    private void importBasisDb() {
        if (MyDebug.DLOG) Log.d(TAG, "563, importBasicDBFile");

        String fileExtension = ".db";
        String fileNameStart = "transektcount0";
        String fileHd = getString(R.string.fileHeadlineBasicDB);

        Intent intent;
        intent = new Intent(this, AdvFileChooser.class);
        intent.putExtra("filterFileExtension", fileExtension);
        intent.putExtra("filterFileNameStart", fileNameStart);
        intent.putExtra("fileHd", fileHd);
        myActivityResultLauncher.launch(intent);
    }
    // End of importBasisDb()

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
        myActivityResultLauncher.launch(intent);
    }

    // ActivityResultLauncher is part of importDBFile()
    // and processes the result of AdvFileChooser
    final ActivityResultLauncher<Intent> myActivityResultLauncher = registerForActivityResult(
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
                            if (MyDebug.DLOG)
                                Log.i(TAG, "608, Selected file: " + selectedFile);

                            if (selectedFile != null)
                                inFile = new File(selectedFile);
                            else
                                inFile = null;
                        }
                    } else if ((result.getResultCode() == Activity.RESULT_FIRST_USER)) {
                        mesg = getString(R.string.noFile);
                        Toast.makeText(getApplicationContext(),
                                HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                        HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                    }
                    if (inFile != null) {
                        // outFile -> /data/data/com.wmstein.transektcount/databases/transektcount.db
                        String destPath = getApplicationContext().getFilesDir().getPath();
                        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases/transektcount.db";
                        outFile = new File(destPath);

                        AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.setMessage(R.string.confirmDBImport);
                        builder.setCancelable(false);
                        builder.setPositiveButton(R.string.importButton, (dialog, id) ->
                        {
                            try {
                                copy(inFile, outFile);

                                // List transNo as title
                                headDataSource.open();
                                head = headDataSource.getHead();
                                transNo = head.transect_no;
                                sectionDataSource.close();
                                Objects.requireNonNull(getSupportActionBar()).setTitle(transNo);

                                mesg = getString(R.string.importDB);
                                Toast.makeText(getApplicationContext(),
                                        HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                mesg = getString(R.string.importFail);
                                Toast.makeText(getApplicationContext(),
                                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                            }
                        });
                        builder.setNegativeButton(R.string.importCancelButton, (dialog, id) -> dialog.cancel());
                        alert = builder.create();
                        alert.show();
                    }
                }
            });
    // End of importDBFile()

    /**********************************************************************************************/
    // Import species list from TourCount file species_YYYY-MM-DD_hhmmss.csv
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
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<>() {
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
                                HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                        HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
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
                            readCSV(inFile);
                        });
                        builder.setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
                        alert = builder.create();
                        alert.show();
                    }
                }
            });

    // Clear DB for import of external species list
    private void clearDBforImport() {
        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();

        String sql = "DELETE FROM " + DbHelper.COUNT_TABLE;
        database.execSQL(sql);

        sql = "DELETE FROM " + DbHelper.ALERT_TABLE;
        database.execSQL(sql);

        sql = "UPDATE " + DbHelper.SECTION_TABLE + " SET "
                + DbHelper.S_CREATED_AT + " = '', "
                + DbHelper.S_NOTES + " = '';";
        database.execSQL(sql);

        dbHandler.close();

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("item_Position", 0);
        editor.putInt("section_id", 1);
        editor.apply();
    }

    private void readCSV(File inFile) {
        try {
            // Read exported species list and write items to table counts
            mesg = getString(R.string.waitImport);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
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
                    HtmlCompat.fromHtml("<font color='green'>" + mesg + "</font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            mesg = getString(R.string.importListFail);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
        }
    }
    // End of importSpeciesList()

    /*********************************************************************************
     * The next four functions below are for exporting data files.
     */
    // Exports Basis DB to Documents/TransektCount/transektcount0.db
    private void exportBasisDb() {
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
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
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
                if (d0) {
                    mesg = getString(R.string.saveBasisDB);
                    Toast.makeText(this,
                            HtmlCompat.fromHtml("<font color='green'>" + mesg + "</font>",
                                    HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                mesg = getString(R.string.saveFail);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
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
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
        } else {
            // Export the db
            try {
                copy(inFile, outFile);
                mesg = getString(R.string.saveDB);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                mesg = getString(R.string.saveFail);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
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
        // outFile -> /storage/emulated/0/Documents/TransektCount/Transekt_Tr-No_yyyyMMdd_HHmmss.csv
        //
        // 1. Alternative for Android >= 10 (Q):
        //    path = new File(Environment.getExternalStorageDirectory() + "/Documents/TransektCount");
        //
        // 2. Alternative for Android < 10 (deprecated in Q):
        //    path = new File(Environment.getExternalStoragePublicDirectory
        //    (Environment.DIRECTORY_DOCUMENTS) + "/TransektCount");
        //
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
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
        } else {
            // Export purged db as csv
            dbHandler = new DbHelper(this);
            database = dbHandler.getWritableDatabase();

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
                if (outPref.equals("sections")) {
                    // Section, Species Name, Local Name, Code, Internal Counts, External Counts, Spec.-Notes
                    String[] arrCol1 =
                            {
                                    getString(R.string.time_sect),
                                    getString(R.string.name_sect),
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
                    csvWrite.writeNext(arrCol1);
                } else {
                    // Species Name, Local Name, Code, Section, Internal Counts, External Counts, Spec.-Note
                    String[] arrCol1 =
                            {
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
                    csvWrite.writeNext(arrCol1);
                }

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
                        sectTimeValue = section.DatNum(); // get Long created_at value
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

                    if (outPref.equals("sections")) {
                        // Build line in species table for species in section
                        String[] arrStr =
                                {
                                        sectTime,    // time of 1. count in section
                                        sectName2,   // section name
                                        name_s,      // species name
                                        name_l,      // species local name
                                        code,        // species code
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
                        csvWrite.writeNext(arrStr);
                    } else {
                        // Build line in species table for species in section
                        String[] arrStr =
                                {
                                        name_s,      // species name
                                        name_l,      // species local name
                                        code,        // species code
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
                        csvWrite.writeNext(arrStr);
                    }

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
                        HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                mesg = getString(R.string.saveFail);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                if (MyDebug.DLOG) Log.e(TAG, "1546, csv write external failed");
            }
            dbHandler.close();
        }
    }
    // End of exportDb2CSV()

    /**
     ********************************************************************************************/
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
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
        } else {
            // Export species list into species_yyyy-MM-dd_HHmmss.csv
            dbHandler = new DbHelper(this);
            database = dbHandler.getWritableDatabase();

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
                            HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                    HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
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
                        HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                mesg = getString(R.string.saveFailList);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
            }
            dbHandler.close();
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
                        HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.importCancelButton, (dialog, id) -> dialog.cancel());
        alert = builder.create();
        alert.show();
    }

    // Clear DB values for basic DB
    @SuppressLint({"LongLogTag"})
    private boolean clearDBValues() {
        // Clear values in DB
        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();

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
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
            r_ok = false;
        }
        dbHandler.close();
        return r_ok;
    }
    // End of resetToBasisDb()

}
