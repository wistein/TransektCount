package com.wmstein.transektcount.database;

import java.util.ArrayList;
import java.util.List;

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
    static final int DATABASE_VERSION = 11;
    public static final String SECTION_TABLE = "sections";
    public static final String COUNT_TABLE = "counts";
    static final String ALERT_TABLE = "alerts";
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
    
    private Context mContext;

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
            " int, " + C_COUNT + " int, " + C_COUNTA + " int, " + C_NAME + " text, " + C_NOTES + " text default NULL)";
        db.execSQL(sql);
        sql = "create table " + ALERT_TABLE + " (" + A_ID + " integer primary key, " + A_COUNT_ID +
            " int, " + A_ALERT + " int, " + A_ALERT_TEXT + " text)";
        db.execSQL(sql);
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
