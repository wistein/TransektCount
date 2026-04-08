package com.wmstein.transektcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.wmstein.transektcount.BuildConfig
import com.wmstein.transektcount.IsRunningOnEmulator
import com.wmstein.transektcount.R
import java.util.Locale

/************************************************************************************
 * DbHelper.kt is the database helper class for SQLite functionality of TransektCount
 * onUpgrade is called with 1. call of dbHelper.getWritableDatabase()
 *   if newVersion != oldVersion
 *
 * Basic structure of DbHelper.java by milo on 05/05/2014.
 * Adopted for TransektCount by wmstein on 2016-02-18.
 * last edited in Java on 2023-06-11,
 * converted to Kotlin on 2023-06-26,
 * updated to version 7 on 2026-03-07,
 * last edited on 2026-04-07
 *
 * ************************************************************************
 * ATTENTION!
 * Current DATABASE_VERSION must be set under 'companion object' at the end
 * ************************************************************************
 */
class DbHelper
    (private val mContext: Context?) :
    SQLiteOpenHelper(mContext, DATABASE_NAME, null, DATABASE_VERSION) {
    // initDataLanguage = current system locale
    val initDataLanguage = Locale.getDefault().toString().substring(0, 2)

    // Called on initial database creation and on DB version upgrades
    override fun onCreate(db: SQLiteDatabase) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "41, onCreate, Creating database: $DATABASE_NAME")

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

        sql = ("create table " + HEAD_TABLE + " ("
                + H_ID + " integer primary key, "
                + H_TRANSECT_NO + " text, "
                + H_INSPECTOR_NAME + " text, "
                + H_DATA_LANGUAGE + " text)")
        db.execSQL(sql)

        sql = ("create table " + META_TABLE + " ("
                + M_ID + " integer primary key, "
                + M_TEMPS + " int, "
                + M_TEMPE + " int, "
                + M_WINDS + " int, "
                + M_WINDE + " int, "
                + M_CLOUDS + " int, "
                + M_CLOUDE + " int, "
                + M_DATE + " text, "
                + M_START_TM + " text, "
                + M_END_TM + " text, "
                + M_NOTE + " text)")
        db.execSQL(sql)

        sql = ("create table " + TRACK_TABLE + " ("
                + T_ID + " integer primary key, "
                + T_SECTION + " text, "
                + T_LAT + " text, "
                + T_LON + " text)")
        db.execSQL(sql)

        // Create single row for HEAD_TABLE
        val values1 = ContentValues()
        values1.put(H_ID, 1)
        values1.put(H_TRANSECT_NO, "")
        values1.put(H_INSPECTOR_NAME, "")
        values1.put(H_DATA_LANGUAGE, "")
        db.insert(HEAD_TABLE, null, values1)

        // Create empty row for META_TABLE
        val values2 = ContentValues()
        values2.put(M_ID, 1)
        values2.put(M_TEMPS, 0)
        values2.put(M_TEMPE, 0)
        values2.put(M_WINDS, 0)
        values2.put(M_WINDE, 0)
        values2.put(M_CLOUDS, 0)
        values2.put(M_CLOUDE, 0)
        values2.put(M_DATE, "")
        values2.put(M_START_TM, "")
        values2.put(M_END_TM, "")
        values2.put(M_NOTE, "")
        db.insert(META_TABLE, null, values2)

        // Create initial data for SECTION_TABLE
        initialSection(db)

        // Create initial data for HEAD_TABLE
        initialHead(db)

        // Create initial data for COUNT_TABLE
        initialCount(db)
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "131, onCreate, Success!")
    }
    // End of onCreate

    private fun initialSection(db: SQLiteDatabase) {
        val values3 = ContentValues()
        values3.put(S_ID, 1)
        values3.put(S_CREATED_AT, 0)
        values3.put(S_NAME, mContext!!.resources.getString(R.string.sect01))
        values3.put(S_NOTES, "")
        db.insert(SECTION_TABLE, null, values3)
    }

    private fun initialHead(db: SQLiteDatabase) {
        // Enter current system language as initial data language
        val sql = "UPDATE $HEAD_TABLE SET $H_DATA_LANGUAGE = '$initDataLanguage'"
        db.execSQL(sql)
    }

    // Initial data for section 1 in COUNT_TABLE
    private fun initialCount(db: SQLiteDatabase) {
        val specs: Array<String> = mContext!!.resources.getStringArray(R.array.initSpecs)
        val codes: Array<String> = mContext.resources.getStringArray(R.array.initCodes)
        // Initial local species name entries comprise initial species in the current system language
        val specsL: Array<String> = when (initDataLanguage) {
            "en" -> mContext.resources.getStringArray(R.array.initSpecs_en)
            "fr" -> mContext.resources.getStringArray(R.array.initSpecs_fr)
            "it" -> mContext.resources.getStringArray(R.array.initSpecs_it)
            "es" -> mContext.resources.getStringArray(R.array.initSpecs_es)
            else -> mContext.resources.getStringArray(R.array.initSpecs_de)
        }

        for (i in 1 until codes.size) {
            val values4 = ContentValues()
            values4.put(C_ID, i)
            values4.put(C_SECTION_ID, 1) // sect01
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
            values4.put(C_NAME_G, specsL[i])
            db.insert(COUNT_TABLE, null, values4)
        }
    }

    // *********************************************************************************
    // Called with 1. call of dbHelper.getWritableDatabase() if newVersion >= oldVersion
    //   and if a database already exists on disk with the same DATABASE_NAME.
    // see https://guides.codepath.org/android/local-databases-with-sqliteopenhelper
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "193, onUpgrade DB")

        if (oldVersion == 7) {
            version8(db)
        }
        if (oldVersion == 6) {
            version7(db)
            version8(db)
        }
        if (oldVersion == 5) {
            version6(db)
            version7(db)
            version8(db)
        }
        if (oldVersion == 4) {
            version5(db)
            version6(db)
            version7(db)
            version8(db)
        }
        if (oldVersion == 3) {
            version4(db)
            version5(db)
            version6(db)
            version7(db)
            version8(db)
        }
        if (oldVersion == 2) {
            version3(db)
            version4(db)
            version5(db)
            version6(db)
            version7(db)
            version8(db)
        }
        if (oldVersion == 1) {
            version2(db)
            version3(db)
            version4(db)
            version5(db)
            version6(db)
            version7(db)
            version8(db)
        }
    }

    /*** V2 ***/
    //  DATABASE_VERSION 2: New count columns added to COUNT_TABLE for sexes and stadiums
    private fun version2(db: SQLiteDatabase) {
        // Adds new count columns to TABLE:COUNT
        var sql: String
        var colExist = false

        // Add new extra columns to table counts without count_f1i and count_f1e as these are
        //  still represented by count and counta
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_F2I int"
            db.execSQL(sql)
        } catch (_: Exception) {
            colExist = true
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_F3I int"
            db.execSQL(sql)
        } catch (_: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_PI int"
            db.execSQL(sql)
        } catch (_: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_LI int"
            db.execSQL(sql)
        } catch (_: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_EI int"
            db.execSQL(sql)
        } catch (_: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_F2E int"
            db.execSQL(sql)
        } catch (_: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_F3E int"
            db.execSQL(sql)
        } catch (_: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_PE int"
            db.execSQL(sql)
        } catch (_: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_LE int"
            db.execSQL(sql)
        } catch (_: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_EE int"
            db.execSQL(sql)
        } catch (_: Exception) {
            //
        }
        if (!colExist) {
            // rename table counts to counts_backup
            sql = "alter table 'counts' rename to 'counts_backup'"
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

            // insert the whole old data into counts
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
                    + C_NOTES + " FROM 'counts_backup'")
            db.execSQL(sql)
            sql = "DROP TABLE 'counts_backup'"
            db.execSQL(sql)
        }
    }

    /*** V3 ***/
    // DATABASE_VERSION 3: Column temp in table META_TABLE changed to tempe as 'temp' seems to have
    //   a reserved term conflict (but can't help it for compatibility with previous versions)
    private fun version3(db: SQLiteDatabase) {
        // Changes column temp to tempe as 'temp' seems to have a reserved term conflict
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
    }

    /*** V4 ***/
    // DATABASE_VERSION 4: Column C_NAME_G added to COUNT_TABLE for local butterfly names
    private fun version4(db: SQLiteDatabase) {
        // Add column C_NAME_G
        val sql = "alter table $COUNT_TABLE add column $C_NAME_G text"
        db.execSQL(sql)
    }

    /*** V5 ***/
    // DATABASE_VERSION 5: New table TRACK_TABLE for GPS supported control of transect sections
    private fun version5(db: SQLiteDatabase) {
        // Adds new TABLE TRACK_TABLE
        var sql = ("create table " + TRACK_TABLE + " ("
                + T_ID + " integer primary key, "
                + T_SECTION + " text, "
                + T_LAT + " text, "
                + T_LON + " text)")
        db.execSQL(sql)

        // reset section data and make SECTION_TABLE contiguous
        sql = ("UPDATE " + SECTION_TABLE + " SET "
                + S_CREATED_AT + " = 0, "
                + S_NOTES + " = ''")
        db.execSQL(sql)
        sql = "alter table $SECTION_TABLE rename to section_backup"
        db.execSQL(sql)
        sql = ("create table " + SECTION_TABLE + " ("
                + S_ID + " integer primary key, "
                + S_CREATED_AT + " int, "
                + S_NAME + " text, "
                + S_NOTES + " text)")
        db.execSQL(sql)
        sql = ("INSERT INTO " + SECTION_TABLE
                + " (created_at, name, notes) SELECT "
                + S_CREATED_AT + ", "
                + S_NAME + ", "
                + S_NOTES + " FROM section_backup order by " + S_ID)
        db.execSQL(sql)
        sql = "DROP TABLE section_backup"
        db.execSQL(sql)
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "433, SECTION_TABLE resetted")

        // reset metadata
        sql = ("UPDATE " + META_TABLE + " SET "
                + M_TEMPE + " = 0, "
                + M_WIND + " = 0, "
                + M_CLOUDS + " = 0, "
                + M_DATE + " = '', "
                + M_START_TM + " = '', "
                + M_END_TM + " = ''")
        db.execSQL(sql)
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "445, META_TABLE resetted")

        // Unify and reset species in section lists of COUNT_TABLE
        //   (For automatic switching between sections all lists
        //   must contain the same species in the same order and with
        //   contiguous numbering of section ids)

        // create new empty counts1 table
        sql = ("create table " + COUNT_TABLE1 + " ("
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
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "473, new empty counts1 table created")

        val specs: List<String> = getAllSpeciesDataSrtCode(db).specs
        val codes: List<String> = getAllSpeciesDataSrtCode(db).codes
        val specsL: List<String> = getAllSpeciesDataSrtCode(db).specsL
        val specNum: Int = codes.size

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "482, Anzahl Spez.: $specNum")

        var cnti = 1  // count index for new track table
        var speci: Int // species index in initSpecs-array
        var sectIncr = 0
        val sectionList: List<Section> = getAllSects(db)
        // fill table for all previous sections with initSpecs data
        for (secti in 0 until sectionList.size) {
            speci = 0
            // for all species of section 1
            while (speci < specNum) {
                val values4 = ContentValues()
                values4.put(C_ID, cnti) // id from count index
                values4.put(C_SECTION_ID, secti + 1)
                values4.put(C_NAME, specs[speci]) // name from species index
                values4.put(C_CODE, codes[speci])
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
                values4.put(C_NAME_G, specsL[speci])
                db.insert(COUNT_TABLE1, null, values4)
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.d(TAG, "514, species cnti: " + cnti + ", " + specs[speci])
                speci++
                cnti++
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.d(TAG, "518, species cnti: $cnti, index speci: $speci")
            }
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "521, last species-index: $cnti")

            sectIncr++
            cnti = (sectIncr * speci) + 1
        }
        sql = "DROP TABLE $COUNT_TABLE"
        db.execSQL(sql)

        sql = "ALTER TABLE $COUNT_TABLE1 RENAME TO $COUNT_TABLE"
        db.execSQL(sql)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "533, Upgraded database to version 5")
    }

    /*** V6 ***/
    // DATABASE_VERSION 6: Modified table META_TABLE for start and end values
    private fun version6(db: SQLiteDatabase) {

        // update meta table for start and end values of temp., wind and clouds
        var sql = "alter table $META_TABLE rename to meta_backup"
        db.execSQL(sql)

        // Create new meta table
        sql = ("create table " + META_TABLE + " ("
                + M_ID + " integer primary key, "
                + M_TEMPS + " int, "
                + M_TEMPE + " int, "
                + M_WINDS + " int, "
                + M_WINDE + " int, "
                + M_CLOUDS + " int, "
                + M_CLOUDE + " int, "
                + M_DATE + " text, "
                + M_START_TM + " text, "
                + M_END_TM + " text, "
                + M_NOTE + " text)")
        db.execSQL(sql)

        // Insert the old data into meta
        sql = ("INSERT INTO " + META_TABLE + " SELECT "
                + M_ID + ", "
                + M_TEMPE + ", "
                + "0, "
                + M_WIND + ", "
                + "0, "
                + M_CLOUDS + ", "
                + "0, "
                + M_DATE + ", "
                + M_START_TM + ", "
                + M_END_TM + ", "
                + "''" + " FROM meta_backup")
        db.execSQL(sql)

        sql = "DROP TABLE meta_backup"
        db.execSQL(sql)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "578, META_TABLE initialized")
    }

    /*** V7 ***/
    // DATABASE_VERSION 7: Drop table ALERT_TABLE and add field H_DATA_LANGUAGE
    private fun version7(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $ALERT_TABLE")

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "587, HEAD_TABLE upgraded")
    }

    /*** V8 ***/
    // DATABASE_VERSION 8: Add field H_DATA_LANGUAGE
    private fun version8(db: SQLiteDatabase) {
        // Add extra column data_language to table head if not exists
        var sql: String
        try {
            sql = "alter table head add column data_language text"
            db.execSQL(sql)
        } catch (_: SQLiteException) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "600, Column already exists.")
        }

        // Enter empty data_language
        sql = "UPDATE head SET data_language = ''"
        db.execSQL(sql)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "608, HEAD_TABLE upgraded")
    }

    data class SpcCdsSpL(val specs: List<String>, val codes: List<String>, val specsL: List<String>)

    @SuppressLint("Range")
    private fun getAllSpeciesDataSrtCode(db: SQLiteDatabase): SpcCdsSpL {
        val sNames: MutableList<String> = ArrayList()
        val sCodes: MutableList<String> = ArrayList()
        val sNamesL: MutableList<String> = ArrayList()
        val cursor = db.rawQuery(
            "select * from " + COUNT_TABLE
                    + " WHERE (" + C_SECTION_ID + " = 1) order by " + C_CODE, null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            sNames.add(cursor.getString(cursor.getColumnIndex(C_NAME)))
            sCodes.add(cursor.getString(cursor.getColumnIndex(C_CODE)))
            sNamesL.add(cursor.getString(cursor.getColumnIndex(C_NAME_G)))
            cursor.moveToNext()
        }
        cursor.close()
        return SpcCdsSpL(sNames, sCodes, sNamesL)
    }

    private fun getAllSects(db: SQLiteDatabase): List<Section> {
        val sections: MutableList<Section> = ArrayList()
        val cursor = db.rawQuery(
            "select * from " + SECTION_TABLE
                    + " order by '" + S_ID + "'", null
        )
        cursor.moveToFirst()
        var i = 1
        while (!cursor.isAfterLast) {
            val section = Section()
            section.id = i
            sections.add(section)
            i++
            cursor.moveToNext()
        }
        cursor.close()
        return sections
    }

    companion object {
        private const val DATABASE_VERSION = 8
        //DATABASE_VERSION 8: Add field H_DATA_LANGUAGE to HEAD_TABLE if not exists
        //DATABASE_VERSION 7: Drop table ALERT_TABLE
        //DATABASE_VERSION 6: Modified table META_TABLE for start and end values
        //DATABASE_VERSION 5: New table TRACK_TABLE for GPS supported control of transect sections
        //DATABASE_VERSION 4: Column C_NAME_G added to COUNT_TABLE for local butterfly names
        //DATABASE_VERSION 3: Column temp in table META_TABLE changed to tempe as 'temp' seems to have
        //DATABASE_VERSION 2: New count columns added to COUNT_TABLE for sexes and stadiums

        private const val TAG = "DBHelper"
        private const val DATABASE_NAME = "transektcount.db"

        // tables
        const val SECTION_TABLE = "sections"
        const val COUNT_TABLE = "counts"
        const val HEAD_TABLE = "head"
        const val META_TABLE = "meta"
        const val TRACK_TABLE = "tracks"
        const val COUNT_TABLE1 = "counts1" // temporary table for update to version 5
        const val ALERT_TABLE = "alerts" // needed to remove obsolet table alerts for update to version 7

        // fields of table sections
        const val S_ID = "_id"
        const val S_CREATED_AT = "created_at"
        const val S_NAME = "name"
        const val S_NOTES = "notes"

        // fields of table counts
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

        // fields of old counts table version 1
        private const val C_COUNT = "count" //deprecated in database version 2
        private const val C_COUNTA = "counta" //deprecated in database version 2

        // fields of table head
        const val H_ID = "_id"
        const val H_TRANSECT_NO = "transect_no"
        const val H_INSPECTOR_NAME = "inspector_name"
        const val H_DATA_LANGUAGE = "data_language"

        // fields of table meta
        const val M_ID = "_id"
        const val M_TEMPS = "temps"
        const val M_TEMPE = "tempe"
        const val M_WINDS = "winds"
        const val M_WINDE = "winde"
        const val M_CLOUDS = "clouds"
        const val M_CLOUDE = "cloude"
        const val M_DATE = "date"
        const val M_START_TM = "start_tm"
        const val M_END_TM = "end_tm"
        const val M_NOTE = "note"

        // fields of old meta table version 4 and 5 deprecated in version 6
        private const val M_TEMP = "temp"
        private const val M_WIND = "wind"

        // fields of table tracks
        const val T_ID = "_id"
        const val T_SECTION = "tsection"
        const val T_LAT = "tlat"
        const val T_LON = "tlon"
    }

}
