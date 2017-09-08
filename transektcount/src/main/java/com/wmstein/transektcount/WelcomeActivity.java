package com.wmstein.transektcount;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Toast;

import com.wmstein.filechooser.AdvFileChooser;
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

import sheetrock.panda.changelog.ChangeLog;
import sheetrock.panda.changelog.ViewHelp;

/**********************************************************************
 * WelcomeActivity provides the starting page with menu and buttons for
 * import/export/help/info methods and
 * EditMetaActivity, ListSectionActivity and ListSpeciesActivity.
 * <p/>
 * Based on BeeCount's WelcomeActivity.java by milo on 05/05/2014.
 * Changes and additions for TransektCount by wmstein since 18.02.2016
 */
public class WelcomeActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "TransektCountWelcomeActivity";
    TransektCountApplication transektCount;
    SharedPreferences prefs;

    ChangeLog cl;
    ViewHelp vh;
    private static final int FILE_CHOOSER = 11;
    private Handler mHandler = new Handler();

    public boolean doubleBackToExitPressedOnce;

    // import/export stuff
    File infile;
    File outfile;
    File tmpfile;
    boolean mExternalStorageAvailable = false;
    boolean mExternalStorageWriteable = false;
    String state = Environment.getExternalStorageState();
    AlertDialog alert;

    // preferences
    private String sortPref;
    private boolean screenOrientL; // option for screen orientation

    // following stuff for purging export db added by wmstein
    private SQLiteDatabase database;
    private DbHelper dbHandler;

    SectionDataSource sectionDataSource;
    CountDataSource countDataSource;
    HeadDataSource headDataSource;
    MetaDataSource metaDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

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

        ScrollView baseLayout = (ScrollView) findViewById(R.id.baseLayout);

        assert baseLayout != null;
        baseLayout.setBackground(transektCount.getBackground());

        Head head;
        headDataSource = new HeadDataSource(this);
        headDataSource.open();
        head = headDataSource.getHead();

        // set transect number as title
        try
        {
            getSupportActionBar().setTitle(head.transect_no);
        } catch (NullPointerException e)
        {
            // nothing
        }

        headDataSource.close();

        // if API level > 23 permission request is necessary
        int REQUEST_CODE_STORAGE = 123; // Identifier for permission request Android 6.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int hasWriteExtStoragePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWriteExtStoragePermission != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
            }
        }

        cl = new ChangeLog(this);
        vh = new ViewHelp(this); // by wmstein
        if (cl.firstRun())
            cl.getLogDialog().show();
    }


    // Date for filename of Export-DB
    // by wmstein
    public String getcurDate()
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
    // Supplemented with exportCSVMenu, exportBasicMenu, importBasicMenu, loadFileMenu, resetDBMenu,
    // editMeta, viewSpecies and viewHelp by wmstein
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
            exportDb2CSV();
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
        else if (id == R.id.loadFileMenu)
        {
            loadFile();
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
            startActivity(new Intent(this, ListSectionActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
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

            // pause for 100 msec to show toast
            mHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    startActivity(new Intent(getApplicationContext(), ListSpeciesActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                }
            }, 100);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void viewSections(View view)
    {
        startActivity(new Intent(this, ListSectionActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void editMeta(View view)
    {
        startActivity(new Intent(this, EditMetaActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void viewSpecies(View view)
    {
        Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show();

        // pause for 100 msec to show toast
        mHandler.postDelayed(new Runnable()
        {
            public void run()
            {
                startActivity(new Intent(getApplicationContext(), ListSpeciesActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        }, 100);

    }

    @Override
    protected void onResume()
    {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
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

        Head head;
        headDataSource = new HeadDataSource(this);
        headDataSource.open();
        head = headDataSource.getHead();

        // set transect number as title
        try
        {
            getSupportActionBar().setTitle(head.transect_no);
        } catch (NullPointerException e)
        {
            // nothing
        }

        headDataSource.close();
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        //LinearLayout baseLayout = (LinearLayout) findViewById(R.id.baseLayout);
        ScrollView baseLayout = (ScrollView) findViewById(R.id.baseLayout);
        assert baseLayout != null;
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

        new Handler().postDelayed(new Runnable()
        {

            @Override
            public void run()
            {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    public void onPause()
    {
        super.onPause();
    }

    public void onStop()
    {
        super.onStop();
    }

    /**************************************************************************
     * The seven functions below are for exporting and importing the database.
     * They've been put here because no database should be open at this point.
     ***********************************************************************/
    // Exports DB to SdCard/transektcount_yyyy-MM-dd_HHmmss.db
    // supplemented with date and time in filename by wmstein
    @SuppressLint({"SdCardPath", "LongLogTag"})
    public void exportDb()
    {
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;
        String state = Environment.getExternalStorageState();
        outfile = new File(Environment.getExternalStorageDirectory() + "/transektcount_" + getcurDate() + ".db");
        String destPath = "/data/data/com.wmstein.transektcount/files";

        try
        {
            destPath = getFilesDir().getPath();
        } catch (Exception e)
        {
            Log.e(TAG, "destPath error: " + e.toString());
        }
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases";
        infile = new File(destPath + "/transektcount.db");

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
            Log.e(TAG, "No sdcard access");
            Toast.makeText(this, getString(R.string.noCard), Toast.LENGTH_LONG).show();
        }
        else
        {
            // export the db
            try
            {
                // export db
                copy(infile, outfile);
                Toast.makeText(this, getString(R.string.saveWin), Toast.LENGTH_SHORT).show();
            } catch (IOException e)
            {
                Log.e(TAG, "Failed to copy database");
                Toast.makeText(this, getString(R.string.saveFail), Toast.LENGTH_LONG).show();
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
        outfile = new File(Environment.getExternalStorageDirectory() + "/transektcount_" + getcurDate() + ".csv");

        Section section;
        String sectName;
        String sectNotes;
        int sect_id;

        Head head;
        Meta meta;
        String transNo, inspecName;
        int temp, wind, clouds;
        int summf = 0, summ = 0, sumf = 0, sump = 0, suml = 0, sumo = 0;
        int summfe = 0, summe = 0, sumfe = 0, sumpe = 0, sumle = 0, sumoe = 0;
        int total = 0;
        String date, start_tm, end_tm, kw;
        int yyyy, mm, dd;
        int Kw = 0; // calendar week

        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();

        // open Head and Meta table for head and meta info
        headDataSource = new HeadDataSource(this);
        headDataSource.open();
        metaDataSource = new MetaDataSource(this);
        metaDataSource.open();

        // open Section table for section name and notes
        sectionDataSource = new SectionDataSource(this);
        sectionDataSource.open();

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
            Log.e(TAG, "No sdcard access");
            Toast.makeText(this, getString(R.string.noCard), Toast.LENGTH_LONG).show();
        }
        else
        {
            // export purged db as csv
            try
            {
                CSVWriter csvWrite = new CSVWriter(new FileWriter(outfile));

                // set header according to table representation in MS Excel
                String arrCol[] =
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

                head = headDataSource.getHead();
                transNo = head.transect_no;
                inspecName = head.inspector_name;
                meta = metaDataSource.getMeta();
                temp = meta.temp;
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
                            yyyy = Integer.valueOf(date.substring(6, 10));
                            mm = Integer.valueOf(date.substring(3, 5));
                            dd = Integer.valueOf(date.substring(0, 2));
                        } catch (Exception e)
                        {
                            // wrong date format (English DB in German), use
                            yyyy = Integer.valueOf(date.substring(0, 4));
                            mm = Integer.valueOf(date.substring(5, 7));
                            dd = Integer.valueOf(date.substring(8, 10));
                        }
                    }
                    else
                    {
                        try
                        {
                            yyyy = Integer.valueOf(date.substring(0, 4));
                            mm = Integer.valueOf(date.substring(5, 7));
                            dd = Integer.valueOf(date.substring(8, 10));
                        } catch (Exception e)
                        {
                            // wrong date format (German DB in English), use
                            yyyy = Integer.valueOf(date.substring(6, 10));
                            mm = Integer.valueOf(date.substring(3, 5));
                            dd = Integer.valueOf(date.substring(0, 2));
                        }
                    }

                    // cal.set(2017, 3, 9); // 09.04.2017
                    cal.set(yyyy, mm - 1, dd);
                    Kw = cal.get(Calendar.WEEK_OF_YEAR);
                }

                kw = String.valueOf(Kw);
                String arrMeta[] =
                    {
                        transNo,
                        inspecName,
                        String.valueOf(temp),
                        String.valueOf(wind),
                        String.valueOf(clouds),
                        date,
                        start_tm,
                        end_tm,
                        kw,
                    };
                csvWrite.writeNext(arrMeta);

                // Empty row
                String arrEmpt[] = {};
                csvWrite.writeNext(arrEmpt);

                // Intern, extern
                String arrIE[] = {"", "", "", "", getString(R.string.internal), "", "", "", "", "", getString(R.string.external)};
                csvWrite.writeNext(arrIE);

                // Species, Codes, Section, Section Notes, Internal counts, External counts, Notes
                String arrCol1[] =
                    {
                        getString(R.string.name_spec),
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

                Cursor curCSV;
/*
                // Test integrity of database
                try
                {
                    curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE (" + DbHelper.C_COUNT_F1I + " > 0)", null);
                } catch (Exception e)
                {
                    Toast.makeText(WelcomeActivity.this, getString(R.string.getHelp), Toast.LENGTH_LONG).show();
                    curCSV.close();
                    finish();
                }
*/
                // build the species table array
                switch (sortPref) // sort mode species list
                {
                case "names_alpha":
                    curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                        + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                        + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                        + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                        + " order by " + DbHelper.C_NAME, null);
                    break;
                case "codes":
                    curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                        + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                        + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                        + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                        + " order by " + DbHelper.C_CODE, null);
                    break;
                default:
                    curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                        + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                        + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                        + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                        + " order by " + DbHelper.C_NAME, null);
                    break;
                }

                int countmf, countm, countf, countp, countl, counte;
                int countmfe, countme, countfe, countpe, countle, countee;
                String strcountmf, strcountm, strcountf, strcountp, strcountl, strcounte;
                String strcountmfe, strcountme, strcountfe, strcountpe, strcountle, strcountee;

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

                    String arrStr[] =
                        {
                            curCSV.getString(2),   //species name
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

                total = summf + summ + sumf + sump + suml + sumo +
                    summfe + summe + sumfe + sumpe + sumle + sumoe;

                // Empty row
                String arrEmpt2[] = {};
                csvWrite.writeNext(arrEmpt2);

                // Intern, extern
                String arrIEsum[] = {"", "", "", "", getString(R.string.internal), "", "", "", "", "", getString(R.string.external)};
                csvWrite.writeNext(arrIEsum);

                // Internal counts, External counts, Total
                String arrCol2[] =
                    {
                        "",
                        "",
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
                    };
                csvWrite.writeNext(arrCol2);

                // write total sum
                String arrSum[] =
                    {
                        "", "", "",
                        getString(R.string.sum),
                        Integer.toString(summf),
                        Integer.toString(summ),
                        Integer.toString(sumf),
                        Integer.toString(sump),
                        Integer.toString(suml),
                        Integer.toString(sumo),
                        Integer.toString(summfe),
                        Integer.toString(summe),
                        Integer.toString(sumfe),
                        Integer.toString(sumpe),
                        Integer.toString(sumle),
                        Integer.toString(sumoe),
                        Integer.toString(total)
                    };
                csvWrite.writeNext(arrSum);

                csvWrite.close();
                dbHandler.close();
                headDataSource.close();
                metaDataSource.close();
                sectionDataSource.close();

                Toast.makeText(this, getString(R.string.saveWin), Toast.LENGTH_SHORT).show();
            } catch (Exception e)
            {
                Log.e(TAG, "Failed to export csv file");
                Toast.makeText(this, getString(R.string.saveFail), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**********************************************************************************************/
    @SuppressLint({"SdCardPath", "LongLogTag"})
    // modified by wmstein
    public void exportBasisDb()
    {
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;
        String state = Environment.getExternalStorageState();
        tmpfile = new File("/data/data/com.wmstein.transektcount/files/transektcount_tmp.db");
        outfile = new File(Environment.getExternalStorageDirectory() + "/transektcount0.db");
        String destPath = "/data/data/com.wmstein.transektcount/files";

        try
        {
            destPath = getFilesDir().getPath();
        } catch (Exception e)
        {
            Log.e(TAG, "destPath error: " + e.toString());
        }
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases";
        infile = new File(destPath + "/transektcount.db");

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
            Log.e(TAG, "No sdcard access");
            Toast.makeText(this, getString(R.string.noCard), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(this, getString(R.string.saveWin), Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e)
            {
                Log.e(TAG, "Failed to export Basic DB");
                Toast.makeText(this, getString(R.string.saveFail), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**********************************************************************************************/
    // Clear all relevant DB values, reset to basic DB 
    // created by wmstein
    public void resetToBasisDb()
    {
        // a confirm dialogue before anything else takes place
        // http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmResetDB).setCancelable(false).setPositiveButton(R.string.deleteButton, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                clearDBValues();
            }
        }).setNegativeButton(R.string.importCancelButton, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dialog.cancel();
            }
        });
        alert = builder.create();
        alert.show();
    }

    // clear DB values for basic DB
    @SuppressLint({"LongLogTag"})
    public void clearDBValues()
    {
        // clear values in DB
        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();

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
                + DbHelper.M_TEMP + " = 0, "
                + DbHelper.M_WIND + " = 0, "
                + DbHelper.M_CLOUDS + " = 0, "
                + DbHelper.M_DATE + " = '', "
                + DbHelper.M_START_TM + " = '', "
                + DbHelper.M_END_TM + " = '';";
            database.execSQL(sql);

            sql = "DELETE FROM " + DbHelper.ALERT_TABLE;
            database.execSQL(sql);

            dbHandler.close();
            Toast.makeText(this, getString(R.string.reset2basic), Toast.LENGTH_SHORT).show();
        } catch (Exception e)
        {
            Log.e(TAG, "Failed to reset DB");
            Toast.makeText(this, getString(R.string.resetFail), Toast.LENGTH_LONG).show();
        }
    }

    /**********************************************************************************************/
    @SuppressLint("SdCardPath")
    // Choose a file to load and set it to transektcount.db
    // based on android-file-chooser from Google Code Archive.
    // Created by wmstein
    public void loadFile()
    {
        Intent intent = new Intent(this, AdvFileChooser.class);
        ArrayList<String> extensions = new ArrayList<>();
        extensions.add(".db");
        String filterFileName = "transektcount";
        intent.putStringArrayListExtra("filterFileExtension", extensions);
        intent.putExtra("filterFileName", filterFileName);
        startActivityForResult(intent, FILE_CHOOSER);
    }

    @SuppressLint("LongLogTag")
    @Override
    // Function is part of loadFile() and processes the result of AdvFileChooser
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        String fileSelected = "";
        if ((requestCode == FILE_CHOOSER) && (resultCode == -1))
        {
            fileSelected = data.getStringExtra("fileSelected");
            //Toast.makeText(this, fileSelected, Toast.LENGTH_SHORT).show();
        }

        //infile = selected File
        if (!fileSelected.equals(""))
        {
            infile = new File(fileSelected);
            // destPath = "/data/data/com.wmstein.transektcount/files"
            String destPath = this.getFilesDir().getPath();
            try
            {
                destPath = getFilesDir().getPath();
            } catch (Exception e)
            {
                Log.e(TAG, "destPath error: " + e.toString());
            }
            destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases";
            //outfile = "/data/data/com.wmstein.transektcount/databases/transektcount.db"
            outfile = new File(destPath + "/transektcount.db");

            // a confirm dialogue before anything else takes place
            // http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
            // could make the dialog central in the popup - to do later
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(R.string.confirmDBImport)
                .setCancelable(false).setPositiveButton(R.string.importButton, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    // START
                    // replace this with another function rather than this lazy c&p
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
                        Log.e(TAG, "No sdcard access");
                        Toast.makeText(getApplicationContext(), getString(R.string.noCard), Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        try
                        {
                            copy(infile, outfile);
                            Toast.makeText(getApplicationContext(), getString(R.string.importWin), Toast.LENGTH_SHORT).show();

                            Head head;
                            headDataSource = new HeadDataSource(getApplicationContext());
                            headDataSource.open();
                            head = headDataSource.getHead();

                            // set transect number as title
                            try
                            {
                                getSupportActionBar().setTitle(head.transect_no);
                            } catch (NullPointerException e)
                            {
                                // nothing
                            }

                            headDataSource.close();
                        } catch (IOException e)
                        {
                            Log.e(TAG, "Failed to import database");
                            Toast.makeText(getApplicationContext(), getString(R.string.importFail), Toast.LENGTH_LONG).show();
                        }
                    }
                    // END
                }
            }).setNegativeButton(R.string.importCancelButton, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            });
            alert = builder.create();
            alert.show();
        }
    }

    /**********************************************************************************************/
    @SuppressLint({"SdCardPath", "LongLogTag"})
    // Import of the basic DB, modified by wmstein
    public void importBasisDb()
    {
        //infile = new File("/data/data/com.wmstein.transektcount/databases/transektcount0.db");
        infile = new File(Environment.getExternalStorageDirectory() + "/transektcount0.db");
        String destPath = "/data/data/com.wmstein.transektcount/files";
        try
        {
            destPath = getFilesDir().getPath();
        } catch (Exception e)
        {
            Log.e(TAG, "destPath error: " + e.toString());
        }
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases";
        //outfile = new File("/data/data/com.wmstein.transektcount/databases/transektcount.db");
        outfile = new File(destPath + "/transektcount.db");
        if (!(infile.exists()))
        {
            Toast.makeText(this, getString(R.string.noDb), Toast.LENGTH_LONG).show();
            return;
        }

        // a confirm dialogue before anything else takes place
        // http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmBasisImport)
            .setCancelable(false).setPositiveButton(R.string.importButton, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                // START
                // replace this with another function rather than this lazy c&p
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
                    Log.e(TAG, "No sdcard access");
                    Toast.makeText(getApplicationContext(), getString(R.string.noCard), Toast.LENGTH_LONG).show();
                }
                else
                {
                    try
                    {
                        copy(infile, outfile);
                        Toast.makeText(getApplicationContext(), getString(R.string.importWin), Toast.LENGTH_SHORT).show();

                        Head head;
                        headDataSource = new HeadDataSource(getApplicationContext());
                        headDataSource.open();
                        head = headDataSource.getHead();

                        // set transect number as title
                        try
                        {
                            getSupportActionBar().setTitle(head.transect_no);
                        } catch (NullPointerException e)
                        {
                            // nothing
                        }

                        headDataSource.close();
                    } catch (IOException e)
                    {
                        Log.e(TAG, "Failed to import database");
                        Toast.makeText(getApplicationContext(), getString(R.string.importFail), Toast.LENGTH_LONG).show();
                    }
                }
                // END
            }
        }).setNegativeButton(R.string.importCancelButton, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dialog.cancel();
            }
        });
        alert = builder.create();
        alert.show();
    }

    /**********************************************************************************************/
    // copy file block-wise
    // http://stackoverflow.com/questions/9292954/how-to-make-a-copy-of-a-file-in-android
    public void copy(File src, File dst) throws IOException
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

}
