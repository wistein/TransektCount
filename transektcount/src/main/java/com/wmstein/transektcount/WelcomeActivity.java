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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.wmstein.filechooser.AdvFileChooser;
import com.wmstein.filechooser.AdvFileChooserL;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import sheetrock.panda.changelog.ChangeLog;
import sheetrock.panda.changelog.ViewHelp;

/**********************************************************************
 * WelcomeActivity provides the starting page with menu and buttons for
 * import/export/help/info methods and starts
 * EditMetaActivity, ListSectionActivity and ListSpeciesActivity.
 * It uses further PermissionDialogFragment.
 * <p>
 * Based on BeeCount's WelcomeActivity.java by milo on 05/05/2014.
 * Changes and additions for TransektCount by wmstein since 2016-02-18,
 * last edited on 2023-06-10
 */
public class WelcomeActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "TransektCountWelcomeAct";
    @SuppressLint("StaticFieldLeak")
    private static TransektCountApplication transektCount;

    ChangeLog cl;
    ViewHelp vh;

    private final Handler mHandler = new Handler();

    public boolean doubleBackToExitPressedOnce;

    // import/export stuff
    private File infile;
    private File outfile;
    boolean mExternalStorageAvailable = false;
    boolean mExternalStorageWriteable = false;
    private final String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private boolean granted;

    final String state = Environment.getExternalStorageState();
    AlertDialog alert;

    // preferences
    SharedPreferences prefs;
    private String sortPref;
    private boolean screenOrientL; // option for screen orientation

    // db handling
    private SQLiteDatabase database;
    private DbHelper dbHandler;
    private HeadDataSource headDataSource;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        sortPref = prefs.getString("pref_sort_sp", "none"); // sort mode species list
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.activity_welcome);

        ScrollView baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(transektCount.getBackground());

        // set transect number as title
        Head head;
        String tname;
        try
        {
            headDataSource = new HeadDataSource(this);
            headDataSource.open();
            head = headDataSource.getHead();
            tname = head.transect_no;
            headDataSource.close();
        } catch (SQLiteException e)
        {
            tname = getString(R.string.errorDb);
            showSnackbarRed(getString(R.string.corruptDb));
        }

        try
        {
            Objects.requireNonNull(getSupportActionBar()).setTitle(tname);
        } catch (NullPointerException e)
        {
            // nothing
        }

        cl = new ChangeLog(this);
        vh = new ViewHelp(this); // by wmstein
        if (cl.firstRun())
            cl.getLogDialog().show();

        // test for existence of directory /storage/emulated/0/Android/data/com.wmstein.transektcount/files/transektcount0.db
        infile = new File(getApplicationContext().getExternalFilesDir(null) + "/transektcount0.db");
        if (!infile.exists())
            exportBasisDb(); // create directory and initial Basis DB (getExternalFilesDir, getExternalDir)

        // Initial test for write permission to external storage
        granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;

    } // end of onCreate

    // Request missing permission
    private final ActivityResultLauncher<String> permissionLauncherSingle = registerForActivityResult(
        new ActivityResultContracts.RequestPermission(),
        new ActivityResultCallback<Boolean>()
        {
            @Override
            public void onActivityResult(Boolean isGranted)
            {
                //here we will check if permission is granted from permission request dialog
                Log.d(TAG, "onActivityResult: isGranted: " + isGranted);

                if (isGranted)
                {
                    //Permission granted now do the required task here or call the function for that
                    granted = true;
                }
                else
                {
                    //Permission was denied so can't do the task that requires that permission
                    Log.d(TAG, "onActivityResult: Permission denied...");
                    showSnackbarRed(getString(R.string.perm_cancel));
                }
            }
        }
                                                                                                     );

    // Date for filename of Export-DB
    public static String getcurDate()
    {
        Date date = new Date();
        @SuppressLint("SimpleDateFormat")
        DateFormat dform = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        return dform.format(date);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.welcome, menu);
        return true;
    }

    // Handle action bar item clicks here. The action bar will automatically handle clicks on 
    // the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.
    // Supplemented with exportCSVMenu, exportBasicMenu, importBasicMenu, importFileMenu, resetDBMenu,
    // editMeta, viewSpecies and viewHelp by wmstein
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            if (screenOrientL)
            {
                startActivity(new Intent(this, SettingsLActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
            else
            {
                startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
            return true;
        }
        else if (id == R.id.exportMenu)
        {
            exportDb();
            return true;
        }
        else if (id == R.id.exportCSVMenu)
        {
            if (granted)
            {
                exportDb2CSV();
            }
            else
            {
                permissionLauncherSingle.launch(permission);
                if (granted)
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
            importFile();
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
        else if (id == R.id.viewSections)
        {
            if (screenOrientL)
            {
                startActivity(new Intent(this, ListSectionLActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
            else
            {
                startActivity(new Intent(this, ListSectionActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
            return true;
        }
        else if (id == R.id.editMeta)
        {
            if (screenOrientL)
            {
                startActivity(new Intent(this, EditMetaLActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
            else
            {
                startActivity(new Intent(this, EditMetaActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
            return true;
        }
        else if (id == R.id.viewSpecies)
        {
            Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show();

            // Trick: Pause for 100 msec to show toast
            if (screenOrientL)
            {
                mHandler.postDelayed(() ->
                    startActivity(new Intent(getApplicationContext(), ListSpeciesLActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)), 100);
            }
            else
            {
                mHandler.postDelayed(() ->
                    startActivity(new Intent(getApplicationContext(), ListSpeciesActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)), 100);
            }

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void viewSections(View view)
    {
        if (screenOrientL)
        {
            startActivity(new Intent(this, ListSectionLActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        else
        {
            startActivity(new Intent(this, ListSectionActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
    }

    public void editMeta(View view)
    {
        if (screenOrientL)
        {
            startActivity(new Intent(this, EditMetaLActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        else
        {
            startActivity(new Intent(this, EditMetaActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
    }

    public void viewSpecies(View view)
    {
        Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show();

        // Trick: Pause for 100 msec to show toast
        if (screenOrientL)
        {
            mHandler.postDelayed(() ->
                startActivity(new Intent(getApplicationContext(), ListSpeciesLActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)), 100);
        }
        else
        {
            mHandler.postDelayed(() ->
                startActivity(new Intent(getApplicationContext(), ListSpeciesActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)), 100);
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onResume()
    {
        super.onResume();

        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        ScrollView baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(null);
        baseLayout.setBackground(transektCount.setBackground());

        Head head;
        headDataSource = new HeadDataSource(this);
        headDataSource.open();
        head = headDataSource.getHead();

        // set transect number as title
        try
        {
            Objects.requireNonNull(getSupportActionBar()).setTitle(head.transect_no);
        } catch (NullPointerException e)
        {
            // nothing
        }

        headDataSource.close();
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(null);
        baseLayout.setBackground(transektCount.setBackground());
        sortPref = prefs.getString("pref_sort_sp", "none");
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
    }

    @Override
    public void onBackPressed()
    {
        if (doubleBackToExitPressedOnce)
        {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.back_twice, Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    public void onPause()
    {
        super.onPause();
    }

    public void onStop()
    {
        super.onStop();
    }

    /******************************************************************************
     * The seven functions below are for exporting and importing of database files.
     * They've been put here because no database should be open at this point.
     ******************************************************************************/
    // Exports DB to SdCard/transektcount_yyyy-MM-dd_HHmmss.db
    // supplemented with date and time in filename by wmstein
    @SuppressLint({"SdCardPath", "LongLogTag"})
    public void exportDb()
    {
        // outfile -> /storage/emulated/0/Android/data/com.wmstein.transektcount/files/transektcount_yyyy-MM-dd_HHmmss.db
        outfile = new File(getApplicationContext().getExternalFilesDir(null) + "/transektcount_" + getcurDate() + ".db");

        // infile <- /data/data/com.wmstein.transektcount/databases/transektcount.db
        String inPath = getApplicationContext().getFilesDir().getPath();
        inPath = inPath.substring(0, inPath.lastIndexOf("/")) + "/databases/transektcount.db";
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
            if (MyDebug.LOG)
                Log.d(TAG, "No sdcard access");
            showSnackbarRed(getString(R.string.noCard));
        }
        else
        {
            // export the db
            try
            {
                // export db
                copy(infile, outfile);
                showSnackbar(getString(R.string.saveWin));
            } catch (IOException e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "Failed to copy database");
                showSnackbarRed(getString(R.string.saveFail));
            }
        }
    }

    /**********************************************************************************************/
    // Exports DB to SdCard/transektcount_yyyy-MM-dd_HHmmss.csv
    // supplemented with date and time in filename by wmstein
    // and purged export in csv-format
    @SuppressLint({"SdCardPath", "LongLogTag"})
    public void exportDb2CSV()
    {
        // outfile -> /storage/emulated/0/Android/data/com.wmstein.transektcount/files/transektcount_yyyy-MM-dd_HHmmss.csv
        //outfile = new File(getApplicationContext().getExternalFilesDir(null) + "/transektcount_" + getcurDate() + ".csv");

        File path;
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        path = new File(path + "/TransektCount");
        //noinspection ResultOfMethodCallIgnored
        path.mkdirs();
        outfile = new File(path, "/transektcount_" + getcurDate() + ".csv");

        Section section;
        String sectName;
        String sectNotes;
        int sect_id;

        Head head;
        Meta meta;
        String transNo, inspecName;
        int tempe, wind, clouds;
        int summf = 0, summ = 0, sumf = 0, sump = 0, suml = 0, sumo = 0;
        int summfe = 0, summe = 0, sumfe = 0, sumpe = 0, sumle = 0, sumoe = 0;
        int total, sumSpec;
        String date, start_tm, end_tm, kw;
        int yyyy, mm, dd;
        int Kw = 0; // calendar week

        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();

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
            if (MyDebug.LOG)
                Log.d(TAG, "No sdcard access");
            showSnackbarRed(getString(R.string.noCard));

        }
        else
        {
            // export purged db as csv
            try
            {
                CSVWriter csvWrite = new CSVWriter(new FileWriter(outfile));

                // set header according to table representation in MS Excel
                String[] arrCol =
                    {
                        getString(R.string.transectnumber),
                        getString(R.string.inspector),
                        getString(R.string.temperature),
                        getString(R.string.wind),
                        getString(R.string.clouds),
                        getString(R.string.date),
                        getString(R.string.starttm),
                        getString(R.string.endtm),
                        getString(R.string.kal_w),
                    };
                csvWrite.writeNext(arrCol); // write line to csv-file

                // open Head table for head info
                headDataSource = new HeadDataSource(this);
                headDataSource.open();
                head = headDataSource.getHead();
                headDataSource.close();
                transNo = head.transect_no;
                inspecName = head.inspector_name;

                // open Meta table for meta info
                MetaDataSource metaDataSource = new MetaDataSource(this);
                metaDataSource.open();
                meta = metaDataSource.getMeta();
                metaDataSource.close();
                tempe = meta.tempe;
                wind = meta.wind;
                clouds = meta.clouds;
                date = meta.date;
                start_tm = meta.start_tm;
                end_tm = meta.end_tm;

                // Calculating the week of the year (ISO 8601)
                Calendar cal = Calendar.getInstance();

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

                kw = String.valueOf(Kw);
                String[] arrMeta =
                    {
                        transNo,
                        inspecName,
                        String.valueOf(tempe),
                        String.valueOf(wind),
                        String.valueOf(clouds),
                        date,
                        start_tm,
                        end_tm,
                        kw,
                    };
                csvWrite.writeNext(arrMeta);

                // Empty row
                String[] arrEmpt = {};
                csvWrite.writeNext(arrEmpt);

                // Intern, extern
                String[] arrIE = {"", "", "", "", "", getString(R.string.internal), "", "", "", "", "", getString(R.string.external)};
                csvWrite.writeNext(arrIE);

                // Species Name, Local Name, Code, Section, Section Note, Internal Counts, External Counts, Spec.-Note
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
                        getString(R.string.countImagomfHint),
                        getString(R.string.countImagomHint),
                        getString(R.string.countImagofHint),
                        getString(R.string.countPupaHint),
                        getString(R.string.countLarvaHint),
                        getString(R.string.countOvoHint),
                        getString(R.string.rem_spec)
                    };
                csvWrite.writeNext(arrCol1);

                CountDataSource countDataSource = new CountDataSource(this);
                countDataSource.open();
                sumSpec = countDataSource.getDiffSpec(); // get number of different species
                countDataSource.close();

                Cursor curCSV;

                // build the species table array according to sort mode for species list
                if ("codes".equals(sortPref))
                {
                    curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                        + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                        + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                        + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                        + " order by " + DbHelper.C_CODE, null);
                }
                else
                {
                    curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                        + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                        + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                        + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                        + " order by " + DbHelper.C_NAME, null);
                }

                int countmf, countm, countf, countp, countl, counte;
                int countmfe, countme, countfe, countpe, countle, countee;
                String strcountmf, strcountm, strcountf, strcountp, strcountl, strcounte;
                String strcountmfe, strcountme, strcountfe, strcountpe, strcountle, strcountee;

                // open Section table for section name and notes
                SectionDataSource sectionDataSource = new SectionDataSource(this);
                sectionDataSource.open();

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

                    String[] arrStr =
                        {
                            curCSV.getString(2),   //species name
                            curCSV.getString(17),   //species name_g
                            curCSV.getString(3),   //species code
                            sectName,              //section name
                            sectNotes,             //section note
                            strcountmf,            //count mf
                            strcountm,             //count m
                            strcountf,             //count f
                            strcountp,             //count p
                            strcountl,             //count l
                            strcounte,             //count e
                            strcountmfe,           //count mfe
                            strcountme,            //count me
                            strcountfe,            //count fe
                            strcountpe,            //count pe
                            strcountle,            //count le
                            strcountee,            //count ee
                            curCSV.getString(16)   //notes
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
                sectionDataSource.close();

                total = summf + summ + sumf + sump + suml + sumo +
                    summfe + summe + sumfe + sumpe + sumle + sumoe;

                // Empty row
                String[] arrEmpt2 = {};
                csvWrite.writeNext(arrEmpt2);

                // Internal, external
                String[] arrIEsum = {"", "", "", "", "", getString(R.string.internal), "", "", "", "", "", getString(R.string.external)};
                csvWrite.writeNext(arrIEsum);

                // Internal counts, External counts, Total
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

                String strsummf, strsumm, strsumf, strsump, strsuml, strsumo,
                    strsummfe, strsumme, strsumfe, strsumpe, strsumle, strsumoe, strtotal;

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

                if (total > 0)
                    strtotal = Integer.toString(total);
                else
                    strtotal = "";

                // write total sum
                String[] arrSum =
                    {
                        "", "", getString(R.string.sumSpec), Integer.toString(sumSpec),
                        getString(R.string.sum),
                        strsummf,
                        strsumm,
                        strsumf,
                        strsump,
                        strsuml,
                        strsumo,
                        strsummfe,
                        strsumme,
                        strsumfe,
                        strsumpe,
                        strsumle,
                        strsumoe,
                        strtotal
                    };
                csvWrite.writeNext(arrSum);

                csvWrite.close();
                dbHandler.close();

                showSnackbar(getString(R.string.savecsv));

            } catch (Exception e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "Failed to export csv file");
                showSnackbarRed(getString(R.string.saveFail));
            }
        }
    }

    /**********************************************************************************************/
    @SuppressLint({"SdCardPath", "LongLogTag"})
    public void exportBasisDb()
    {
        // tmpfile -> /data/data/com.wmstein.transektcount/files/transektcount_tmp.db
        String tmpPath = getApplicationContext().getFilesDir().getPath();
        tmpPath = tmpPath.substring(0, tmpPath.lastIndexOf("/")) + "/files/transektcount_tmp.db";
        File tmpfile = new File(tmpPath);

        // outfile -> /storage/emulated/0/Android/data/com.wmstein.transektcount/files/transektcount0.db
        outfile = new File(getApplicationContext().getExternalFilesDir(null) + "/transektcount0.db");

        // infile <- /data/data/com.wmstein.transektcount/databases/transektcount.db
        String inPath = getApplicationContext().getFilesDir().getPath();
        inPath = inPath.substring(0, inPath.lastIndexOf("/")) + "/databases/transektcount.db";
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
            if (MyDebug.LOG)
                Log.d(TAG, "No sdcard access");
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
                {
                    showSnackbar(getString(R.string.saveWin));
                }
            } catch (IOException e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "Failed to export Basic DB");
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
                + DbHelper.M_TEMPE + " = 0, "
                + DbHelper.M_WIND + " = 0, "
                + DbHelper.M_CLOUDS + " = 0, "
                + DbHelper.M_DATE + " = '', "
                + DbHelper.M_START_TM + " = '', "
                + DbHelper.M_END_TM + " = '';";
            database.execSQL(sql);

            sql = "DELETE FROM " + DbHelper.ALERT_TABLE;
            database.execSQL(sql);

            dbHandler.close();
        } catch (Exception e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "Failed to reset DB");
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
    public void importFile()
    {
        ArrayList<String> extensions = new ArrayList<>();
        extensions.add(".db");
        String filterFileName = "transektcount";

        Intent intent;
        if (screenOrientL)
        {
            intent = new Intent(this, AdvFileChooserL.class);
        }
        else
        {
            intent = new Intent(this, AdvFileChooser.class);
        }
        intent.putStringArrayListExtra("filterFileExtension", extensions);
        intent.putExtra("filterFileName", filterFileName);
        myActivityResultLauncher.launch(intent);

        // outfile -> /data/data/com.wmstein.transektcount/databases/transektcount.db
        String destPath = getApplicationContext().getFilesDir().getPath();
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases/transektcount.db";
        outfile = new File(destPath);

        // confirm dialogue before anything else takes place
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmDBImport)
            .setCancelable(false).setPositiveButton(R.string.importButton, (dialog, id) ->
            {
                try
                {
                    copy(infile, outfile);
                    showSnackbar(getString(R.string.importWin));

                    Head head;
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
                    if (MyDebug.LOG)
                        Log.e(TAG, "Failed to import database");
                    showSnackbarRed(getString(R.string.importFail));
                }
                // END
            }).setNegativeButton(R.string.importCancelButton, (dialog, id) -> dialog.cancel());
        alert = builder.create();
        alert.show();
    }

    // Function is part of importFile() and processes the result of AdvFileChooser
    final ActivityResultLauncher<Intent> myActivityResultLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>()
        {
            @Override
            public void onActivityResult(ActivityResult result)
            {
                String selectedFile;
                if (result.getResultCode() == Activity.RESULT_OK)
                {
                    Intent data = result.getData();
                    // Following the operation
                    assert data != null;
                    selectedFile = data.getStringExtra("fileSelected");
                    if (MyDebug.LOG)
                    {
                        Log.e(TAG, "File selected: " + selectedFile);
                        showSnackbarRed("Selected file: " + selectedFile);
                    }
                    infile = new File(selectedFile);
                }
            }
        });

    /**********************************************************************************************/
    @SuppressLint({"SdCardPath", "LongLogTag"})
    // Import of the basic DB, modified by wmstein
    public void importBasisDb()
    {
        // infile <- /storage/emulated/0/Android/data/com.wmstein.transektcount/files/transektcount0.db
        infile = new File(getApplicationContext().getExternalFilesDir(null) + "/transektcount0.db");

        // outfile -> /data/data/com.wmstein.transektcount//databases/transektcount.db
        String destPath = getApplicationContext().getFilesDir().getPath();
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases/transektcount.db";
        outfile = new File(destPath);
        if (!(infile.exists()))
        {
            showSnackbar(getString(R.string.noDb));
            return;
        }

        // confirm dialogue before anything else takes place
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmBasisImport)
            .setCancelable(false).setPositiveButton(R.string.importButton, (dialog, id) ->
            {
                try
                {
                    copy(infile, outfile);
                    showSnackbar(getString(R.string.importWin));

                    Head head;
                    headDataSource = new HeadDataSource(getApplicationContext());
                    headDataSource.open();
                    head = headDataSource.getHead();

                    // set transect number as title
                    try
                    {
                        Objects.requireNonNull(getSupportActionBar()).setTitle(head.transect_no);
                    } catch (NullPointerException e)
                    {
                        // nothing
                    }

                    headDataSource.close();
                } catch (IOException e)
                {
                    if (MyDebug.LOG)
                        Log.e(TAG, "Failed to import database");
                    showSnackbarRed(getString(R.string.importFail));
                }
                // END
            }).setNegativeButton(R.string.importCancelButton, (dialog, id) -> dialog.cancel());
        alert = builder.create();
        alert.show();
    }

    /**********************************************************************************************/
    // copy file block-wise
    // http://stackoverflow.com/questions/9292954/how-to-make-a-copy-of-a-file-in-android
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
        sB.setActionTextColor(Color.GREEN);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        sB.show();
    }

    private void showSnackbarRed(String str) // bold red text
    {
        View view = findViewById(R.id.baseLayout);
        Snackbar sB = Snackbar.make(view, str, Snackbar.LENGTH_LONG);
        sB.setActionTextColor(Color.RED);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        sB.show();
    }

}
