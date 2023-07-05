package com.wmstein.transektcount.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.wmstein.transektcount.MyDebug
import com.wmstein.transektcount.R

/***********************************************
 * Based on DbHelper.java by milo on 05/05/2014.
 * Adopted for TransektCount by wmstein on 2016-02-18
 * last edited in Java on 2023-06-11
 * converted to Kotlin on 2023-06-26
 */
class DbHelper    // constructor
    (private val mContext: Context) :
    SQLiteOpenHelper(mContext, DATABASE_NAME, null, DATABASE_VERSION) {
    // called once on database creation
    override fun onCreate(db: SQLiteDatabase) {
        if (MyDebug.LOG) Log.d(TAG, "Creating database: $DATABASE_NAME")
        var sql = ("create table " + SECTION_TABLE + " ("
                + S_ID + " integer primary key, "
                + S_CREATED_AT + " int, "
                + S_NAME + " text, "
                + S_NOTES + " text)")
        db.execSQL(sql)
        sql = ("create table " + COUNT_TABLE + " ("
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
                + C_NOTES + " text, "
                + C_NAME_G + " text)")
        db.execSQL(sql)
        sql = ("create table " + ALERT_TABLE + " ("
                + A_ID + " integer primary key, "
                + A_COUNT_ID + " int, "
                + A_ALERT + " int, "
                + A_ALERT_TEXT + " text)")
        db.execSQL(sql)
        sql = ("create table " + HEAD_TABLE + " ("
                + H_ID + " integer primary key, "
                + H_TRANSECT_NO + " text, "
                + H_INSPECTOR_NAME + " text)")
        db.execSQL(sql)
        sql = ("create table " + META_TABLE + " ("
                + M_ID + " integer primary key, "
                + M_TEMPE + " int, "
                + M_WIND + " int, "
                + M_CLOUDS + " int, "
                + M_DATE + " text, "
                + M_START_TM + " text, "
                + M_END_TM + " text)")
        db.execSQL(sql)

        //create empty row for HEAD_TABLE
        val values1 = ContentValues()
        values1.put(H_ID, 1)
        values1.put(H_TRANSECT_NO, "")
        values1.put(H_INSPECTOR_NAME, "")
        db.insert(HEAD_TABLE, null, values1)

        //create empty row for META_TABLE
        val values2 = ContentValues()
        values2.put(M_ID, 1)
        values2.put(M_TEMPE, 0)
        values2.put(M_WIND, 0)
        values2.put(M_CLOUDS, 0)
        values2.put(M_DATE, "")
        values2.put(M_START_TM, "")
        values2.put(M_END_TM, "")
        db.insert(META_TABLE, null, values2)

        //create initial data for SECTION_TABLE
        initialSection(db)

        //create initial data for COUNT_TABLE
        initialCounts(db)
        if (MyDebug.LOG) Log.d(TAG, "Success!")
    }

    private fun initialSection(db: SQLiteDatabase) {
        val values3 = ContentValues()
        values3.put(S_ID, 1)
        values3.put(S_CREATED_AT, 0)
        values3.put(S_NAME, mContext.resources.getString(R.string.sect01))
        values3.put(S_NOTES, "")
        db.insert(SECTION_TABLE, null, values3)
    }

    // initial data for COUNT_TABLE
    private fun initialCounts(db: SQLiteDatabase) {
        val specs: Array<String> = mContext.resources.getStringArray(R.array.initSpecs)
        val codes: Array<String> = mContext.resources.getStringArray(R.array.initCodes)
        val specs_g: Array<String> = mContext.resources.getStringArray(R.array.initSpecs_g)
        for (i in 1 until specs.size) {
            val values4 = ContentValues()
            values4.put(C_ID, i)
            values4.put(C_SECTION_ID, 1)
            values4.put(C_NAME, specs[i])
            values4.put(C_CODE, codes[i])
            values4.put(C_COUNT_F1I, 0)
            values4.put(C_COUNT_F2I, 0)
            values4.put(C_COUNT_F3I, 0)
            values4.put(C_COUNT_PI, 0)
            values4.put(C_COUNT_LI, 0)
            values4.put(C_COUNT_EI, 0)
            values4.put(C_COUNT_F1E, 0)
            values4.put(C_COUNT_F2E, 0)
            values4.put(C_COUNT_F3E, 0)
            values4.put(C_COUNT_PE, 0)
            values4.put(C_COUNT_LE, 0)
            values4.put(C_COUNT_EE, 0)
            values4.put(C_NOTES, "")
            values4.put(C_NAME_G, specs_g[i])
            db.insert(COUNT_TABLE, null, values4)
        }
    }

    // ******************************************************************************************
    // called if newVersion != oldVersion
    // see https://www.androidpit.de/forum/472061/sqliteopenhelper-mit-upgrade-beispielen-und-zentraler-instanz
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 3) {
            version_4(db)
        }
        if (oldVersion == 2) {
            version_3(db)
            version_4(db)
        }
        if (oldVersion == 1) {
            version_2(db)
            version_3(db)
            version_4(db)
        }
    }

    // Adds new count columns to TABLE:COUNT
    private fun version_2(db: SQLiteDatabase) {
        var sql: String
        var colExist = false

        // add new extra columns to table counts without count_f1i and count_f1e as these are
        //  still represented by count and counta
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_F2I int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing count_f2i column added to counts!")
        } catch (e: Exception) {
            if (MyDebug.LOG) Log.e(TAG, "Column already present: $e")
            colExist = true
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_F3I int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing count_f3i column added to counts!")
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_PI int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing count_pi column added to counts!")
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_LI int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing count_li column added to counts!")
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_EI int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing count_ei column added to counts!")
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_F2E int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing count_f2e column added to counts!")
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_F3E int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing count_f3e column added to counts!")
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_PE int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing count_pe column added to counts!")
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_LE int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing count_le column added to counts!")
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_EE int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing count_ee column added to counts!")
        } catch (e: Exception) {
            //
        }
        if (!colExist) {
            // rename table counts to counts_backup
            sql = "alter table $COUNT_TABLE rename to counts_backup"
            db.execSQL(sql)

            // create new counts table
            sql = ("create table " + COUNT_TABLE + "("
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
                    + C_NOTES + " text)")
            db.execSQL(sql)

            // insert the old data into counts
            sql = ("INSERT INTO " + COUNT_TABLE + " SELECT "
                    + C_ID + ", "
                    + C_SECTION_ID + ", "
                    + C_NAME + ", "
                    + C_CODE + ", "
                    + C_COUNT + ", "
                    + C_COUNT_F2I + ", "
                    + C_COUNT_F3I + ", "
                    + C_COUNT_PI + ", "
                    + C_COUNT_LI + ", "
                    + C_COUNT_EI + ", "
                    + C_COUNTA + ", "
                    + C_COUNT_F2E + ", "
                    + C_COUNT_F3E + ", "
                    + C_COUNT_PE + ", "
                    + C_COUNT_LE + ", "
                    + C_COUNT_EE + ", "
                    + C_NOTES + " FROM counts_backup")
            db.execSQL(sql)
            sql = "DROP TABLE counts_backup"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Upgraded database to version 2")
        }
    }

    // Changes column temp to tempe as 'temp' seems to have a reserved term conflict  
    private fun version_3(db: SQLiteDatabase) {
        var sql = "alter table $META_TABLE rename to meta_backup"
        db.execSQL(sql)

        // create new meta table
        sql = ("create table " + META_TABLE + " ("
                + M_ID + " integer primary key, "
                + M_TEMPE + " int, "
                + M_WIND + " int, "
                + M_CLOUDS + " int, "
                + M_DATE + " text, "
                + M_START_TM + " text, "
                + M_END_TM + " text)")
        db.execSQL(sql)

        // insert the old data into meta
        sql = ("INSERT INTO " + META_TABLE + " SELECT "
                + M_ID + ", "
                + M_TEMP + ", "
                + M_WIND + ", "
                + M_CLOUDS + ", "
                + M_DATE + ", "
                + M_START_TM + ", "
                + M_END_TM + " FROM meta_backup")
        db.execSQL(sql)
        sql = "DROP TABLE meta_backup"
        db.execSQL(sql)
        if (MyDebug.LOG) Log.d(TAG, "Upgraded database to version 3")
    }

    // Add column C_NAME_G
    private fun version_4(db: SQLiteDatabase) {
        val sql = "alter table $COUNT_TABLE add column $C_NAME_G text"
        db.execSQL(sql)
        if (MyDebug.LOG) Log.d(TAG, "Upgraded database to version 4")
    }

    companion object {
        private const val TAG = "TransektCount DBHelper"
        private const val DATABASE_NAME = "transektcount.db"

        //DATABASE_VERSION 2: New count columns added to COUNT_TABLE for sexes and stadiums
        //DATABASE_VERSION 3: Column temp in table META_TABLE changed to tempe as 'temp' seems to have a reserved term conflict
        //DATABASE_VERSION 4: Column C_NAME_G added to COUNT_TABLE for local butterfly names 
        private const val DATABASE_VERSION = 4

        // tables
        const val SECTION_TABLE = "sections"
        const val COUNT_TABLE = "counts"
        const val ALERT_TABLE = "alerts"
        const val HEAD_TABLE = "head"
        const val META_TABLE = "meta"

        // fields
        const val S_ID = "_id"
        const val S_CREATED_AT = "created_at"
        const val S_NAME = "name"
        const val S_NOTES = "notes"
        const val C_ID = "_id"
        const val C_SECTION_ID = "section_id"
        const val C_NAME = "name"
        const val C_CODE = "code"
        const val C_COUNT_F1I = "count_f1i"
        const val C_COUNT_F2I = "count_f2i"
        const val C_COUNT_F3I = "count_f3i"
        const val C_COUNT_PI = "count_pi"
        const val C_COUNT_LI = "count_li"
        const val C_COUNT_EI = "count_ei"
        const val C_COUNT_F1E = "count_f1e"
        const val C_COUNT_F2E = "count_f2e"
        const val C_COUNT_F3E = "count_f3e"
        const val C_COUNT_PE = "count_pe"
        const val C_COUNT_LE = "count_le"
        const val C_COUNT_EE = "count_ee"
        const val C_NOTES = "notes"
        const val C_NAME_G = "name_g"
        private const val C_COUNT = "count" //deprecated in database version 2
        private const val C_COUNTA = "counta" //deprecated in database version 2
        const val A_ID = "_id"
        const val A_COUNT_ID = "count_id"
        const val A_ALERT = "alert"
        const val A_ALERT_TEXT = "alert_text"
        const val H_ID = "_id"
        const val H_TRANSECT_NO = "transect_no"
        const val H_INSPECTOR_NAME = "inspector_name"
        const val M_ID = "_id"
        const val M_TEMPE = "tempe"
        const val M_WIND = "wind"
        const val M_CLOUDS = "clouds"
        const val M_DATE = "date"
        const val M_START_TM = "start_tm"
        const val M_END_TM = "end_tm"
        private const val M_TEMP = "temp"
    }
}