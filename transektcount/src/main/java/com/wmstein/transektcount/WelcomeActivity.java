package com.wmstein.transektcount;

import static android.graphics.Color.RED;

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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import sheetrock.panda.changelog.ChangeLog;
import sheetrock.panda.changelog.ViewHelp;
import sheetrock.panda.changelog.ViewLicense;

/**********************************************************************
 * WelcomeActivity provides the starting page with menu and buttons for
 * import/export/help/info methods and starts
 * EditMetaActivity, ListSectionActivity and ListSpeciesActivity.
 * It uses further the PermissionDialogFragment.
 * <p>
 * Database handling is mainly done in WelcomeActivity as upgrade to current
 * DB version when importing an older DB file by importDBFile().
 * <p>
 * Based on BeeCount's WelcomeActivity.java by Milo Thurston from 2014-05-05.
 * Changes and additions for TransektCount by wmstein since 2016-02-18,
 * last edited on 2024-08-15.
 */
public class WelcomeActivity
    extends AppCompatActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "WelcomeAct";

    @SuppressLint("StaticFieldLeak")
    private static TransektCountApplication transektCount;

    private ChangeLog cl;
    private ViewHelp vh;
    private ViewLicense vl;

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
    private String outPref;

    // db handling
    private SQLiteDatabase database;
    private DbHelper dbHandler;
    private HeadDataSource headDataSource;
    private Head head;
    private SectionDataSource sectionDataSource;
    private MetaDataSource metaDataSource;
    private CountDataSource countDataSource;
    private AlertDataSource alertDataSource;

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
                showSnackbarRed(getString(R.string.perm_cancel));
        }

        selectedFile = "";
        infile = null;
        outfile = null;

        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);

        // setup the data sources
        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();

        headDataSource = TransektCountApplication.getHeadDS();
        sectionDataSource = TransektCountApplication.getSectionDS();
        metaDataSource = TransektCountApplication.getMetaDS();
        countDataSource = TransektCountApplication.getCountDS();
        alertDataSource = TransektCountApplication.getAlertDS();

        // check for DB integrity
        try
        {
            headDataSource.open();
            head = headDataSource.getHead();
            String tName = head.transect_no; // just dummy read for DB integrity test
            headDataSource.close();
        } catch (SQLiteException e)
        {
            headDataSource.close();
            showSnackbarRed(getString(R.string.corruptDb));
            finish();
        }

        dbHandler.close();

        if (MyDebug.LOG) Log.d(TAG, "174, onCreate, vor ChangeLog");
        // Show changelog for new version
        cl = new ChangeLog(this);
        vh = new ViewHelp(this);
        vl = new ViewLicense(this);
        if (cl.firstRun())
            cl.getLogDialog().show();

        // Test for existence of directory /storage/emulated/0/Documents/TransektCount/transektcount0.db
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
        if (!infile.exists())
            exportBasisDb(); // create directory and copy internal DB-data to initial Basis DB-file

        // new onBackPressed logic
        if (Build.VERSION.SDK_INT >= 33)
        {
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true)
                {
                    @Override
                    public void handleOnBackPressed()
                    {
                        if (doubleBackToExitPressedTwice)
                        {
                            finish();
                        }

                        doubleBackToExitPressedTwice = true;

                        Toast t = new Toast(getApplicationContext());
                        LayoutInflater inflater = getLayoutInflater();

                        @SuppressLint("InflateParams")
                        View toastView = inflater.inflate(R.layout.toast_view, null);
                        TextView textView = toastView.findViewById(R.id.toast);
                        textView.setText(R.string.back_twice);

                        t.setView(toastView);
                        t.setDuration(Toast.LENGTH_SHORT);
                        t.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
                        t.show();

                        mHandler.postDelayed(() ->
                            doubleBackToExitPressedTwice = false, 1500);
                    }
                }
            );
        }
    }
    // end of onCreate()

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onResume()
    {
        super.onResume();

        if (MyDebug.LOG) Log.d(TAG, "241, onResume");
        prefs = TransektCountApplication.getPrefs();
        outPref = prefs.getString("pref_csv_out", "species"); // sort mode csv-export

        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();
        headDataSource.open();
        sectionDataSource.open();
        metaDataSource.open();
        countDataSource.open();
        alertDataSource.open();

        head = headDataSource.getHead();

        // set transect name as title
        try
        {
            Objects.requireNonNull(getSupportActionBar()).setTitle(head.transect_no);
        } catch (NullPointerException e)
        {
            // nothing
        }
    }
    // end of onResume()

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
            startActivity(new Intent(this, SettingsActivity.class).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP));
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
                PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(),
                    PermissionsDialogFragment.class.getName());
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
        else if (id == R.id.resetDBMenu)
        {
            resetToBasisDb();
            return true;
        }
/*
        else if (id == R.id.importListMenu)
        {
            importTourCountList();
            return true;
        }
 */
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
        else if (id == R.id.viewLicense)
        {
            vl.getFullLogDialog().show();
            return true;
        }
        else if (id == R.id.startCounting)
        {
            // call CountingActivity
            Intent intent;
            intent = new Intent(WelcomeActivity.this, ListSectionActivity.class);
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
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
    }
    // end of onOptionsItemSelected

    // Save activity state
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        View baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(null);
        baseLayout.setBackground(transektCount.setBackground());
        outPref = prefs.getString("pref_csv_out", "species");
    }

    @Override
    public void onPause()
    {
        super.onPause();

        headDataSource.close();
        sectionDataSource.close();
        metaDataSource.close();
        countDataSource.close();
        alertDataSource.close();
        dbHandler.close();
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

        Toast t = new Toast(this);
        LayoutInflater inflater = getLayoutInflater();

        @SuppressLint("InflateParams")
        View toastView = inflater.inflate(R.layout.toast_view, null);
        TextView textView = toastView.findViewById(R.id.toast);
        textView.setText(R.string.back_twice);

        t.setView(toastView);
        t.setDuration(Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
        t.show();

        mHandler.postDelayed(() -> doubleBackToExitPressedTwice = false, 1500);
    }

    @Override
    public void onStop()
    {
        super.onStop();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        prefs.unregisterOnSharedPreferenceChangeListener(this);
        dbHandler.close();
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

    // Date for filename of Export-DB
    public static String getcurDate()
    {
        Date date = new Date();
        @SuppressLint("SimpleDateFormat")
        DateFormat dform = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        return dform.format(date);
    }

    // Start CountingActivity
    public void startCounting(View view)
    {
        // Start ListSectionActivity to select section to be used for counting
        Intent intent;
        intent = new Intent(WelcomeActivity.this, ListSectionActivity.class);
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
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

    /********************************************************************************
     * The seven functions below are for exporting and importing of (database) files.
     * They've been put here because no database should be left open at this point.
     * Exports DB to Documents/TransektCount/transektcount_yyyy-MM-dd_HHmmss.db
     *   supplemented with date and time in filename
     * outfile ->
     *   /storage/emulated/0/Documents/TransektCount/transektcount_yyyy-MM-dd_HHmmss.db
     */
    @SuppressLint({"SdCardPath", "LongLogTag"})
    public void exportDb()
    {
        /*
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

    /*********************************************************************************************
     // Exports DB contents as transektcount_yyyy-MM-dd_HHmmss.csv-file to Documents/TransektCount/
     //   with purged data set
     // Spreadsheet programs can import this csv file with
     //   - Unicode UTF-8 filter,
     //   - comma delimiter and
     //   - "" for text recognition.
     */
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
        String sectName;  // name shown in list
        int sect_id;

        Meta meta; // meta database instance
        String transNo, inspecName;
        int temps, tempe;   // temperature at start time and end time
        int winds, winde;   // wind
        int clouds, cloude; // clouds

        String date, start_tm, end_tm, kw, inspection_note; // kw = calendar week (String)
        int yyyy, mm, dd;
        int Kw = 0; // calendar week (Int)

        //  ♂,♀        ♂         ♀         pupa      larva     ovo
        int summf = 0, summ = 0, sumf = 0, sump = 0, suml = 0, sumo = 0;
        int summfe = 0, summe = 0, sumfe = 0, sumpe = 0, sumle = 0, sumoe = 0;

        int totali, totale;
        int total, sumSpec;

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
            headDataSource = TransektCountApplication.getHeadDS();
            headDataSource.open();
            metaDataSource.open();
            sectionDataSource.open();
            countDataSource.open();

            // get number of different species
            sumSpec = countDataSource.getDiffSpec();

            //********************************
            // start creating csv table output
            try
            {
                CSVWriter csvWrite = new CSVWriter(new FileWriter(outfile));

                // set header according to table representation in spreadsheet
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
                        "", "", "", "", "", "",
                        getString(R.string.note),
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
                inspection_note = meta.note;

                // Calculating the week of the year (ISO 8601)
                Calendar cal = Calendar.getInstance();

                assert date != null;
                if (!date.isEmpty())
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
                        "", "", "", "", "", "",
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
                String[] arrIE = {"", "", "", "", getString(R.string.internal), "", "", "", "", "", getString(R.string.external)};
                csvWrite.writeNext(arrIE);

                // headline of species table with
                if (outPref.equals("sections"))
                {
                    // Section, Species Name, Local Name, Code, Internal Counts, External Counts, Spec.-Notes
                    String[] arrCol1 =
                        {
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
                // build the internal species array according to the sorted species or sections
                Cursor curCSV;
                if (outPref.equals("sections"))
                {
                        // cursor contains list sorted by name with all internal count entries > 0
                        curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                            + " WHERE ("
                            + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                            + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                            + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                            + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                            + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                            + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                            + " order by " + DbHelper.C_SECTION_ID + ", " + DbHelper.C_NAME, null);
                }
                else
                {
                        // cursor contains list sorted by name with all internal count entries > 0
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

                // internal counts variables
                int countmf, countm, countf, countp, countl, counte;
                String strcountmf, strcountm, strcountf, strcountp, strcountl, strcounte;

                // external counts variables
                int countmfe = 0, countme = 0, countfe = 0, countpe = 0, countle = 0, countee = 0;
                String strcountmfe, strcountme, strcountfe, strcountpe, strcountle, strcountee;

                // sums and totals variables
                int totalsmfie = 0, totalsmie = 0, totalsfie = 0, totalspie = 0, totalslie = 0, totalseie = 0;
                String strsummf, strsumm, strsumf, strsump, strsuml, strsumo;
                String strsummfe, strsumme, strsumfe, strsumpe, strsumle, strsumoe;
                String strtotalsmfie, strtotalsmie, strtotalsfie, strtotalspie, strtotalslie, strtotalseie;

                String strtotali, strtotale, strtotal;

                curCSV.moveToFirst();
                while (!curCSV.isAfterLast())
                {
                    sect_id = curCSV.getInt(1);
                    section = sectionDataSource.getSection(sect_id);
                    sectName = section.name;
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

                    if (outPref.equals("sections"))
                    {
                        // build line in species table for species in section
                        String[] arrStr =
                            {
                                sectName,    // section name
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
                    } else
                    {
                        // build line in species table for species in section
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

                // Internal counts, External counts, Totals
                String[] arrCol2 =
                    {
                        "", "", "", "",
                        getString(R.string.countImagomfHint),
                        getString(R.string.countImagomHint),
                        getString(R.string.countImagofHint),
                        getString(R.string.countPupaHint),
                        getString(R.string.countLarvaHint),
                        getString(R.string.countOvoHint),
                        "", "", "", "", "", "",
                        getString(R.string.hintTotal)
                    };
                csvWrite.writeNext(arrCol2);

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

                // write internal total sums
                String[] arrSumi =
                    {
                        "",
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

                // external sums
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

                // write external total sums
                String[] arrSume =
                    {
                        "", "", "",
                        getString(R.string.sume),
                        strsummfe,
                        strsumme,
                        strsumfe,
                        strsumpe,
                        strsumle,
                        strsumoe,
                        "", "", "", "", "", "",
                        strtotale,
                    };
                csvWrite.writeNext(arrSume);

                // overall totals
                totalsmfie = summf + summfe;
                if (totalsmfie > 0)
                    strtotalsmfie = Integer.toString(totalsmfie);
                else
                    strtotalsmfie = "";

                totalsmie = summ + summe;
                if (totalsmie > 0)
                    strtotalsmie = Integer.toString(totalsmie);
                else
                    strtotalsmie = "";

                totalsfie = sumf + sumfe;
                if (totalsfie > 0)
                    strtotalsfie = Integer.toString(totalsfie);
                else
                    strtotalsfie = "";

                totalspie = sump + sumpe;
                if (totalspie > 0)
                    strtotalspie = Integer.toString(totalspie);
                else
                    strtotalspie = "";

                totalslie = suml + sumle;
                if (totalslie > 0)
                    strtotalslie = Integer.toString(totalslie);
                else
                    strtotalslie = "";

                totalseie = sumo + sumoe;
                if (totalseie > 0)
                    strtotalseie = Integer.toString(totalseie);
                else
                    strtotalseie = "";

                if (total > 0)
                    strtotal = Integer.toString(total);
                else
                    strtotal = "";

                // write overall total sum
                String[] arrTotal =
                    {
                        "", "", "",
                        getString(R.string.sum_total),
                        strtotalsmfie,
                        strtotalsmie,
                        strtotalsfie,
                        strtotalspie,
                        strtotalslie,
                        strtotalseie,
                        "", "", "", "", "", "",
                        strtotal
                    };
                csvWrite.writeNext(arrTotal);

                csvWrite.close();
                showSnackbar(getString(R.string.savecsv));

            } catch (Exception e)
            {
                showSnackbarRed(getString(R.string.saveFail));
                if (MyDebug.LOG) Log.e(TAG, "1178, csv write external failed");
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
        database = TransektCountApplication.getDatabase();

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

        } catch (Exception e)
        {
            showSnackbarRed(getString(R.string.resetFail));
            r_ok = false;
        }
        return r_ok;
    }

    /**********************************************************************************************/
    @SuppressLint("SdCardPath")
    // Choose a transektcount db-file to load and set it to transektcount.db
    public void importDBFile()
    {
        String fileExtension = ".db";
        String fileName = "transektcount";
        infile = null;

        Intent intent;
        intent = new Intent(this, AdvFileChooser.class);
        intent.putExtra("filterFileExtension", fileExtension);
        intent.putExtra("filterFileName", fileName);
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

                    showSnackbar(getString(R.string.importDB));

                    head = headDataSource.getHead();

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
                        showSnackbar("Selected file: " + selectedFile);
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

                showSnackbar(getString(R.string.importDB));

                head = headDataSource.getHead();

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

}
