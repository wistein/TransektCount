package com.wmstein.transektcount.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.wmstein.transektcount.WelcomeActivity;


/**
 * Created by milo on 05/05/2014.
 * Changed by wmstein on 18.02.2016
 */
public class DbHelper extends SQLiteOpenHelper
{
    static final String TAG = "TransektCount DB";
    public static final String DATABASE_NAME = "transektcount.db";
    static final int DATABASE_VERSION = 1;
    public static final String SECTION_TABLE = "sections";
    public static final String COUNT_TABLE = "counts";
    public static final String ALERT_TABLE = "alerts";
    public static final String HEAD_TABLE = "head";
    public static final String META_TABLE = "meta";
    public static final String S_ID = "_id";
    public static final String S_CREATED_AT = "created_at";
    public static final String S_NAME = "name";
    public static final String S_NOTES = "notes";
    public static final String C_ID = "_id";
    public static final String C_SECTION_ID = "section_id";
    public static final String C_COUNT = "count";
    public static final String C_COUNTA = "counta";
    public static final String C_NAME = "name";
    public static final String C_NOTES = "notes";
    public static final String A_ID = "_id";
    public static final String A_COUNT_ID = "count_id";
    public static final String A_ALERT = "alert";
    public static final String A_ALERT_TEXT = "alert_text";
    public static final String H_ID = "_id";
    public static final String H_TRANSECT_NO = "transect_no";
    public static final String H_INSPECTOR_NAME = "inspector_name";
    public static final String M_ID = "_id";
    public static final String M_TEMP = "temp";
    public static final String M_WIND = "wind";
    public static final String M_CLOUDS = "clouds";
    public static final String M_DATE = "date";
    public static final String M_START_TM = "start_tm";
    public static final String M_END_TM = "end_tm";
    private Context mContext;

    private SQLiteDatabase db;
    
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
        //Log.i(TAG, "Creating database: " + DATABASE_NAME);
        String sql = "create table " + SECTION_TABLE + " (" + S_ID + " integer primary key, " +
            S_CREATED_AT + " int, " + S_NAME + " text, " + S_NOTES + " text)";
        db.execSQL(sql);
        sql = "create table " + COUNT_TABLE + " (" + C_ID + " integer primary key, " + C_SECTION_ID +
            " int, " + C_COUNT + " int, " + C_COUNTA + " int, " + C_NAME + " text, " + C_NOTES + 
            " text default NULL)";
        db.execSQL(sql);
        sql = "create table " + ALERT_TABLE + " (" + A_ID + " integer primary key, " + A_COUNT_ID +
            " int, " + A_ALERT + " int, " + A_ALERT_TEXT + " text)";
        db.execSQL(sql);
        sql = "create table " + HEAD_TABLE + " (" + H_ID + " integer primary key, " + H_TRANSECT_NO 
            + " text, " + H_INSPECTOR_NAME + " text)";
        db.execSQL(sql);
        sql = "create table " + META_TABLE + " (" + M_ID + " integer primary key, " + M_TEMP + 
            " int, " + M_WIND + " int, " + M_CLOUDS + " int, " + M_DATE + " text, " + M_START_TM + 
            " text, " + M_END_TM + " text)";
        db.execSQL(sql);
        
        //create empty row for HEAD_TABLE and META_TABLE
        ContentValues values1 = new ContentValues();
        values1.put(H_ID, 1);
        values1.put(H_TRANSECT_NO, "");
        values1.put(H_INSPECTOR_NAME, "");
        db.insert(HEAD_TABLE, null, values1);

        ContentValues values2 = new ContentValues();
        values2.put(M_ID, 1);
        values2.put(M_TEMP, 0);
        values2.put(M_WIND, 0);
        values2.put(M_CLOUDS, 0);
        values2.put(M_DATE, "");
        values2.put(M_START_TM, "");
        values2.put(M_END_TM, "");
        db.insert(META_TABLE, null, values2);
        
        //Log.i(TAG, "Success!");
    }

    // ******************************************************************************************
    // called if newVersion != oldVersion
    // nur weil es die class erfordert, siehe beeCount oder 
    // https://www.androidpit.de/forum/472061/sqliteopenhelper-mit-upgrade-beispielen-und-zentraler-instanz
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        //nothing to upgrade
    }

}
