package com.wmstein.transektcount.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.wmstein.transektcount.MyDebug;
import com.wmstein.transektcount.R;


/***********************************************
 * Based on DbHelper.java by milo on 05/05/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016
 * Last edited on 2019-02-02
 */
public class DbHelper extends SQLiteOpenHelper
{
    private static final String TAG = "TransektCount DBHelper";
    private static final String DATABASE_NAME = "transektcount.db";
    private static final int DATABASE_VERSION = 3;
    private Context mContext;

    // tables
    public static final String SECTION_TABLE = "sections";
    public static final String COUNT_TABLE = "counts";
    public static final String ALERT_TABLE = "alerts";
    public static final String HEAD_TABLE = "head";
    public static final String META_TABLE = "meta";

    // fields
    public static final String S_ID = "_id";
    public static final String S_CREATED_AT = "created_at";
    public static final String S_NAME = "name";
    public static final String S_NOTES = "notes";

    public static final String C_ID = "_id";
    public static final String C_SECTION_ID = "section_id";
    public static final String C_NAME = "name";
    public static final String C_CODE = "code";
    public static final String C_COUNT_F1I = "count_f1i";
    public static final String C_COUNT_F2I = "count_f2i";
    public static final String C_COUNT_F3I = "count_f3i";
    public static final String C_COUNT_PI = "count_pi";
    public static final String C_COUNT_LI = "count_li";
    public static final String C_COUNT_EI = "count_ei";
    public static final String C_COUNT_F1E = "count_f1e";
    public static final String C_COUNT_F2E = "count_f2e";
    public static final String C_COUNT_F3E = "count_f3e";
    public static final String C_COUNT_PE = "count_pe";
    public static final String C_COUNT_LE = "count_le";
    public static final String C_COUNT_EE = "count_ee";
    public static final String C_NOTES = "notes";

    private static final String C_COUNT = "count"; //deprecated in version 2
    private static final String C_COUNTA = "counta"; //deprecated in version 2

    public static final String A_ID = "_id";
    public static final String A_COUNT_ID = "count_id";
    public static final String A_ALERT = "alert";
    public static final String A_ALERT_TEXT = "alert_text";

    public static final String H_ID = "_id";
    public static final String H_TRANSECT_NO = "transect_no";
    public static final String H_INSPECTOR_NAME = "inspector_name";

    public static final String M_ID = "_id";
    public static final String M_TEMPE = "tempe";
    public static final String M_WIND = "wind";
    public static final String M_CLOUDS = "clouds";
    public static final String M_DATE = "date";
    public static final String M_START_TM = "start_tm";
    public static final String M_END_TM = "end_tm";

    private static final String M_TEMP = "temp";

    // constructor
    public DbHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
    }

    // called once on database creation
    @Override
    public void onCreate(SQLiteDatabase db)
    {
        if (MyDebug.LOG)
            Log.d(TAG, "Creating database: " + DATABASE_NAME);
        String sql = "create table " + SECTION_TABLE + " ("
            + S_ID + " integer primary key, "
            + S_CREATED_AT + " int, "
            + S_NAME + " text, "
            + S_NOTES + " text)";
        db.execSQL(sql);
        sql = "create table " + COUNT_TABLE + " ("
            + C_ID + " integer primary key, "
            + C_SECTION_ID + " int, "
            + C_NAME + " text, "
            + C_CODE + " text, "
            + C_COUNT_F1I + " int, "
            + C_COUNT_F2I + " int, "
            + C_COUNT_F3I + " int, "
            + C_COUNT_PI + " int, "
            + C_COUNT_LI + " int, "
            + C_COUNT_EI + " int, "
            + C_COUNT_F1E + " int, "
            + C_COUNT_F2E + " int, "
            + C_COUNT_F3E + " int, "
            + C_COUNT_PE + " int, "
            + C_COUNT_LE + " int, "
            + C_COUNT_EE + " int, "
            + C_NOTES + " text)";
        db.execSQL(sql);
        sql = "create table " + ALERT_TABLE + " ("
            + A_ID + " integer primary key, "
            + A_COUNT_ID + " int, "
            + A_ALERT + " int, "
            + A_ALERT_TEXT + " text)";
        db.execSQL(sql);
        sql = "create table " + HEAD_TABLE + " ("
            + H_ID + " integer primary key, "
            + H_TRANSECT_NO + " text, "
            + H_INSPECTOR_NAME + " text)";
        db.execSQL(sql);
        sql = "create table " + META_TABLE + " ("
            + M_ID + " integer primary key, "
            + M_TEMPE + " int, "
            + M_WIND + " int, "
            + M_CLOUDS + " int, "
            + M_DATE + " text, "
            + M_START_TM + " text, "
            + M_END_TM + " text)";
        db.execSQL(sql);

        //create empty row for HEAD_TABLE
        ContentValues values1 = new ContentValues();
        values1.put(H_ID, 1);
        values1.put(H_TRANSECT_NO, "");
        values1.put(H_INSPECTOR_NAME, "");
        db.insert(HEAD_TABLE, null, values1);

        //create empty row for META_TABLE
        ContentValues values2 = new ContentValues();
        values2.put(M_ID, 1);
        values2.put(M_TEMPE, 0);
        values2.put(M_WIND, 0);
        values2.put(M_CLOUDS, 0);
        values2.put(M_DATE, "");
        values2.put(M_START_TM, "");
        values2.put(M_END_TM, "");
        db.insert(META_TABLE, null, values2);

        //create initial data for SECTION_TABLE
        initialSection(db);

        //create initial data for COUNT_TABLE
        initialCounts(db);

        if (MyDebug.LOG)
            Log.d(TAG, "Success!");
    }

    private void initialSection(SQLiteDatabase db)
    {
        ContentValues values3;
        values3 = new ContentValues();
        values3.put(S_ID, 1);
        values3.put(S_CREATED_AT, 0);
        values3.put(S_NAME, mContext.getResources().getString(R.string.sect01));
        values3.put(S_NOTES, "");
        db.insert(SECTION_TABLE, null, values3);

    }

    // initial data for COUNT_TABLE
    private void initialCounts(SQLiteDatabase db)
    {
        String[] specs, codes;
        specs = mContext.getResources().getStringArray(R.array.initSpecs);
        codes = mContext.getResources().getStringArray(R.array.initCodes);

        for (int i = 1; i < specs.length; i++)
        {
            ContentValues values4 = new ContentValues();
            values4.put(C_ID, i);
            values4.put(C_SECTION_ID, 1);
            values4.put(C_NAME, specs[i]);
            values4.put(C_CODE, codes[i]);
            values4.put(C_COUNT_F1I, 0);
            values4.put(C_COUNT_F2I, 0);
            values4.put(C_COUNT_F3I, 0);
            values4.put(C_COUNT_PI, 0);
            values4.put(C_COUNT_LI, 0);
            values4.put(C_COUNT_EI, 0);
            values4.put(C_COUNT_F1E, 0);
            values4.put(C_COUNT_F2E, 0);
            values4.put(C_COUNT_F3E, 0);
            values4.put(C_COUNT_PE, 0);
            values4.put(C_COUNT_LE, 0);
            values4.put(C_COUNT_EE, 0);
            values4.put(C_NOTES, "");
            db.insert(COUNT_TABLE, null, values4);
        }
    }

    // ******************************************************************************************
    // called if newVersion != oldVersion
    // see https://www.androidpit.de/forum/472061/sqliteopenhelper-mit-upgrade-beispielen-und-zentraler-instanz
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        if (oldVersion == 2)
        {
            version_3(db);
        }
        if (oldVersion == 1)
        {
            version_2(db);
            version_3(db);
        }
    }

    // Adds new count columns to TABLE:COUNT
    private void version_2(SQLiteDatabase db)
    {
        String sql;
        boolean colExist = false;

        // add new extra columns to table counts without count_f1i and count_f1e as these are
        //  still represented by count and counta
        try
        {
            sql = "alter table " + COUNT_TABLE + " add column " + C_COUNT_F2I + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing count_f2i column added to counts!");
        } catch (Exception e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "Column already present: " + e.toString());
            colExist = true;
        }
        try
        {
            sql = "alter table " + COUNT_TABLE + " add column " + C_COUNT_F3I + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing count_f3i column added to counts!");
        } catch (Exception e)
        {
            //
        }
        try
        {
            sql = "alter table " + COUNT_TABLE + " add column " + C_COUNT_PI + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing count_pi column added to counts!");
        } catch (Exception e)
        {
            //
        }
        try
        {
            sql = "alter table " + COUNT_TABLE + " add column " + C_COUNT_LI + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing count_li column added to counts!");
        } catch (Exception e)
        {
            //
        }
        try
        {
            sql = "alter table " + COUNT_TABLE + " add column " + C_COUNT_EI + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing count_ei column added to counts!");
        } catch (Exception e)
        {
            //
        }
        try
        {
            sql = "alter table " + COUNT_TABLE + " add column " + C_COUNT_F2E + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing count_f2e column added to counts!");
        } catch (Exception e)
        {
            //
        }
        try
        {
            sql = "alter table " + COUNT_TABLE + " add column " + C_COUNT_F3E + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing count_f3e column added to counts!");
        } catch (Exception e)
        {
            //
        }
        try
        {
            sql = "alter table " + COUNT_TABLE + " add column " + C_COUNT_PE + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing count_pe column added to counts!");
        } catch (Exception e)
        {
            //
        }
        try
        {
            sql = "alter table " + COUNT_TABLE + " add column " + C_COUNT_LE + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing count_le column added to counts!");
        } catch (Exception e)
        {
            //
        }
        try
        {
            sql = "alter table " + COUNT_TABLE + " add column " + C_COUNT_EE + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing count_ee column added to counts!");
        } catch (Exception e)
        {
            //
        }

        if (!colExist)
        {
            // rename table counts to counts_backup
            sql = "alter table " + COUNT_TABLE + " rename to counts_backup";
            db.execSQL(sql);

            // create new counts table
            sql = "create table " + COUNT_TABLE + "("
                + C_ID + " integer primary key, "
                + C_SECTION_ID + " int, "
                + C_NAME + " text, "
                + C_CODE + " text, "
                + C_COUNT_F1I + " int, "
                + C_COUNT_F2I + " int, "
                + C_COUNT_F3I + " int, "
                + C_COUNT_PI + " int, "
                + C_COUNT_LI + " int, "
                + C_COUNT_EI + " int, "
                + C_COUNT_F1E + " int, "
                + C_COUNT_F2E + " int, "
                + C_COUNT_F3E + " int, "
                + C_COUNT_PE + " int, "
                + C_COUNT_LE + " int, "
                + C_COUNT_EE + " int, "
                + C_NOTES + " text)";
            db.execSQL(sql);

            // insert the old data into counts
            sql = "INSERT INTO " + COUNT_TABLE + " SELECT "
                + C_ID + ","
                + C_SECTION_ID + ","
                + C_NAME + ","
                + C_CODE + ","
                + C_COUNT + ","
                + C_COUNT_F2I + ","
                + C_COUNT_F3I + ","
                + C_COUNT_PI + ","
                + C_COUNT_LI + ","
                + C_COUNT_EI + ","
                + C_COUNTA + ","
                + C_COUNT_F2E + ","
                + C_COUNT_F3E + ","
                + C_COUNT_PE + ","
                + C_COUNT_LE + ","
                + C_COUNT_EE + ","
                + C_NOTES + " FROM counts_backup";
            db.execSQL(sql);

            sql = "DROP TABLE counts_backup";
            db.execSQL(sql);

            if (MyDebug.LOG)
                Log.d(TAG, "Upgraded database to version 2");
        }
    }
    
    // Changes column temp to tempe as 'temp' seems to have a reserved term conflict  
    private void version_3(SQLiteDatabase db)
    {
        String sql;
        sql = "alter table " + META_TABLE + " rename to meta_backup";
        db.execSQL(sql);

        // create new meta table
        sql = "create table " + META_TABLE + " ("
            + M_ID + " integer primary key, "
            + M_TEMPE + " int, "
            + M_WIND + " int, "
            + M_CLOUDS + " int, "
            + M_DATE + " text, "
            + M_START_TM + " text, "
            + M_END_TM + " text)";
        db.execSQL(sql);

        // insert the old data into meta
        sql = "INSERT INTO " + META_TABLE + " SELECT "
            + M_ID + ", "
            + M_TEMP + ", "
            + M_WIND + ", "
            + M_CLOUDS + ", "
            + M_DATE + ", "
            + M_START_TM + ", "
            + M_END_TM + " FROM meta_backup";
        db.execSQL(sql);

        sql = "DROP TABLE meta_backup";
        db.execSQL(sql);

        if (MyDebug.LOG)
            Log.d(TAG, "Upgraded database to version 3");
    }

}
