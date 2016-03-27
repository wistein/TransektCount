package com.wmstein.transektcount;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.wmstein.transektcount.database.CSVWriter;
import com.wmstein.transektcount.database.Count;
import com.wmstein.transektcount.database.CountDataSource;
import com.wmstein.transektcount.database.DbHelper;
import com.wmstein.transektcount.database.Section;
import com.wmstein.transektcount.database.SectionDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sheetrock.panda.changelog.ChangeLog;
import sheetrock.panda.changelog.ViewHelp;

/**
 * Created by milo on 05/05/2014.
 * Changes and additions by wmstein on 18.02.2016
 */

public class WelcomeActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "TransektCountWelcomeActivity";
    TransektCountApplication transektCount;
    SharedPreferences prefs;
    ChangeLog cl;

    ViewHelp vh; // added by wmstein

    // import/export stuff
    File infile;
    File outfile;
    File tmpfile;
    boolean mExternalStorageAvailable = false;
    boolean mExternalStorageWriteable = false;
    String state = Environment.getExternalStorageState();
    AlertDialog alert;

    // following stuff for purging export db added by wmstein
    private SQLiteDatabase database;
    private DbHelper dbHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);

        //LinearLayout baseLayout = (LinearLayout) findViewById(R.id.baseLayout);
        ScrollView baseLayout = (ScrollView) findViewById(R.id.baseLayout);
        baseLayout.setBackground(transektCount.getBackground());
        
        //Progressbar in activity_welcome invisible

        // a title isn't necessary on this welcome screen as it appears below
        getSupportActionBar().setTitle("");

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

    @Override
    // supplemented with exportBasicMenu, inportBasicMenu, viewSpecies and viewHelp by wmstein
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

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
        else if (id == R.id.viewSpecies)
        {
            Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, ListSpeciesActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void viewSections(View view)
    {
        startActivity(new Intent(this, ListSectionActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void viewSpecies(View view)
    {
        Toast.makeText(getApplicationContext(),getString(R.string.wait), Toast.LENGTH_SHORT).show();

        startActivity(new Intent(this, ListSpeciesActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        //LinearLayout baseLayout = (LinearLayout) findViewById(R.id.baseLayout);
        ScrollView baseLayout = (ScrollView) findViewById(R.id.baseLayout);
        baseLayout.setBackground(null);
        baseLayout.setBackground(transektCount.setBackground());
    }

  /**************************************************************************
   * The six activities below are for exporting and importing the database. 
   * They've been put here because no database should be open at this point.
   */

    /***********************************************************************/
    // Exports DB to SdCard/transektcount_yyyy-MM-dd_HHmmss.db
    // supplemented with date and time in filename by wmstein
    // and purged export
    @SuppressLint("SdCardPath")
    public void exportDb()
    {
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;
        String state = Environment.getExternalStorageState();
        File tmpfile = new File("/data/data/com.wmstein.transektcount/files/transektcount_tmp.db");
        File outfile = new File(Environment.getExternalStorageDirectory() + "/transektcount_" + getcurDate() + ".db");
        String destPath = "/data/data/com.wmstein.transektcount/files";
        
        try
        {
            destPath = getFilesDir().getPath();
        }
        catch (Exception e)
        {
            Log.e(TAG, "destPath error: " + e.toString());
        }
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases";
        File infile = new File(destPath + "/transektcount.db");

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
            // purge count table and export the db
            try
            {
                // save current db as backup db tmpfile
                copy(infile, tmpfile);

                // purge transektcount.db count table from empty rows
                // Delete all empty counts (and sections)
                // by wmstein
                dbHandler = new DbHelper(this);
                database = dbHandler.getWritableDatabase();

                String sql = "DELETE FROM " + DbHelper.COUNT_TABLE + " WHERE (" + DbHelper.C_COUNT + " = 0 AND " + DbHelper.C_COUNTA + " = 0);";
                database.execSQL(sql);

                // Not necessary to purge the section table, but the code would do
                //sql = "DELETE FROM " + DbHelper.SECTION_TABLE + " WHERE (" + DbHelper.S_CREATED_AT + " IS NULL OR " + DbHelper.S_CREATED_AT + " = '');";
                //database.execSQL(sql);

                dbHandler.close();

                // export purged db
                copy(infile, outfile);

                // restore current db from tmpfile
                copy(tmpfile, infile);

                // delete backup db
                boolean d0 = tmpfile.delete(); // delete backup DB
                if (d0)
                {
                    Toast.makeText(this, getString(R.string.saveWin), Toast.LENGTH_SHORT).show();
                }
            }
            catch (IOException e)
            {
                Log.e(TAG, "Failed to copy database");
                Toast.makeText(this, getString(R.string.saveFail), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /***********************************************************************/
    // Exports DB to SdCard/transektcount_yyyy-MM-dd_HHmmss.csv
    // supplemented with date and time in filename by wmstein
    // and purged export in csv-format
    @SuppressLint("SdCardPath")
    public void exportDb2CSV()
    {
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;
        String state = Environment.getExternalStorageState();
        File tmpfile = new File("/data/data/com.wmstein.transektcount/files/transektcount_tmp.db");
        File outfile = new File(Environment.getExternalStorageDirectory() + "/transektcount_" + getcurDate() + ".csv");
        String destPath = "/data/data/com.wmstein.transektcount/files";
        SectionDataSource sectionDataSource;
        Section section;
        String sectName;
        String sectNotes;
        String specNotes;
        long sect_id;
        
        try
        {
            destPath = getFilesDir().getPath();
        }
        catch (Exception e)
        {
            Log.e(TAG, "destPath error: " + e.toString());
        }
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases";
        File infile = new File(destPath + "/transektcount.db");

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
            // export the count table to csv
            try
            {
                // save current db as backup db tmpfile
                copy(infile, tmpfile);

                // purge transektcount.db count table from empty rows 
                dbHandler = new DbHelper(this);
                database = dbHandler.getWritableDatabase();

                String sql = "DELETE FROM " + DbHelper.COUNT_TABLE + " WHERE (" + DbHelper.C_COUNT + " = 0 AND " + DbHelper.C_COUNTA + " = 0);";
                database.execSQL(sql);
                sectionDataSource = new SectionDataSource(this);
                sectionDataSource.open();

                // export purged db as csv
                CSVWriter csvWrite = new CSVWriter(new FileWriter(outfile));
                Cursor curCSV = database.rawQuery("select * from " + DbHelper.COUNT_TABLE,null);
                
                // set header according to table representation in MS Excel
                // Section, Section Notes, Species, Internal, External, Notes
                String arrCol[] =
                    {
                        getString(R.string.col1), getString(R.string.col2), getString(R.string.col3), getString(R.string.col4), getString(R.string.col5), getString(R.string.col6)
                    };
                csvWrite.writeNext(arrCol);
                
                // build the table array
                while(curCSV.moveToNext())
                {
                    sect_id = curCSV.getLong(1);
                    section = sectionDataSource.getSection(sect_id);
                    sectName = section.name;
                    sectNotes = section.notes;
                    
                    specNotes = curCSV.getString(5);
                    // Byte code translation from UTF-8 to ISO-8859-1 makes representation in MS Excel even worse.
                    // Excel can import csv files with UTF-8 filter 
                    //byte[] utf8 = specNotes.getBytes("UTF-8");
                    //specNotes = new String(utf8, "ISO-8859-1");

                    String arrStr[] = 
                    {
                        sectName,              //section name
                        sectNotes,             //section notes
                        curCSV.getString(4),   //species name
                        curCSV.getString(2),   //count
                        curCSV.getString(3),   //counta
                        specNotes              //notes
                    };
                    csvWrite.writeNext(arrStr);
                }

                csvWrite.close();
                curCSV.close();
                dbHandler.close();
                
                // restore current db from tmpfile
                copy(tmpfile, infile);

                // delete backup db
                boolean d0 = tmpfile.delete();
                if (d0)
                {
                    Toast.makeText(this, getString(R.string.saveWin), Toast.LENGTH_SHORT).show();
                }
            }
            catch (IOException e)
            {
                Log.e(TAG, "Failed to copy database");
                Toast.makeText(this, getString(R.string.saveFail), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**************************************************************************************************/
    @SuppressLint("SdCardPath")
    // modified by wmstein
    public void exportBasisDb()
    {
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;
        String state = Environment.getExternalStorageState();
        File outfile = new File(Environment.getExternalStorageDirectory() + "/transektcount0.db");
        String destPath = "/data/data/com.wmstein.transektcount/files";
        try
        {
            destPath = getFilesDir().getPath();
        } 
        catch (Exception e)
        {
            Log.e(TAG, "destPath error: " + e.toString());
        }
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases";
        File infile = new File(destPath + "/transektcount.db");

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
                copy(infile, outfile);
                Toast.makeText(this, getString(R.string.saveWin), Toast.LENGTH_SHORT).show();
            }
            catch (IOException e)
            {
                Log.e(TAG, "Failed to copy database");
                Toast.makeText(this, getString(R.string.saveFail), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**************************************************************************************************/
    @SuppressLint("SdCardPath")
    // modified by wmstein
    public void importBasisDb()
    {
        infile = new File(Environment.getExternalStorageDirectory() + "/transektcount0.db");
        //outfile = new File("/data/data/com.wmstein.transektcount/databases/transektcount.db");
        String destPath = "/data/data/com.wmstein.transektcount/files";
        try
        {
            destPath = getFilesDir().getPath();
        }
        catch (Exception e)
        {
            Log.e(TAG, "destPath error: " + e.toString());
        }
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases";
        //File infile = new File(destPath + "/transektcount.db");
        //outfile = new File(this.getFilesDir().getPath() + "/data/com.wmstein.transektcount/databases/transektcount.db");
        outfile = new File(destPath + "/transektcount.db");
        if (!(infile.exists()))
        {
            Toast.makeText(this, getString(R.string.noDb), Toast.LENGTH_LONG).show();
            return;
        }

        // a confirm dialogue before anything else takes place
        // http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
        // could make the dialog central in the popup - to do later
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmBasisImport).setCancelable(false).setPositiveButton(R.string.importButton, new DialogInterface.OnClickListener()
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
