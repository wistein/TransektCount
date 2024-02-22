package com.wmstein.transektcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.wmstein.transektcount.MyDebug
import com.wmstein.transektcount.R
import com.wmstein.transektcount.TransektCountApplication.getAppContext

/***********************************************
 * Based on DbHelper.java by milo on 05/05/2014.
 * Adopted for TransektCount by wmstein on 2016-02-18
 * last edited in Java on 2023-06-11
 * converted to Kotlin on 2023-06-26
 * last edited on 2024-02-22
 */
class DbHelper   // constructor
    (private val mContext: Context) :
    SQLiteOpenHelper(mContext, DATABASE_NAME, null, DATABASE_VERSION) {

    // called once on database creation
    override fun onCreate(db: SQLiteDatabase) {
        if (MyDebug.LOG) Log.d(TAG, "22, Creating database: $DATABASE_NAME")
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
        sql = ("create table " + TRACK_TABLE + " ("
                + T_ID + " integer primary key, "
                + T_SECTION + " text, "
                + T_LAT + " text, "
                + T_LON + " text)")
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
        initialCount(db)
        if (MyDebug.LOG) Log.d(TAG, "103, onCreate, Success!")
    }
    // end of onCreate

    private fun initialSection(db: SQLiteDatabase) {
        val values3 = ContentValues()
        values3.put(S_ID, 1)
        values3.put(S_CREATED_AT, 0)
        values3.put(S_NAME, mContext.resources.getString(R.string.sect01))
        values3.put(S_NOTES, "")
        db.insert(SECTION_TABLE, null, values3)
    }

    // initial data for COUNT_TABLE
    private fun initialCount(db: SQLiteDatabase) {
        // entries for sect 1 comprise initial selected species
        val specs: Array<String> = mContext.resources.getStringArray(R.array.initSpecs)
        val codes: Array<String> = mContext.resources.getStringArray(R.array.initCodes)
        val specsL: Array<String> = mContext.resources.getStringArray(R.array.initSpecs_l)
        for (i in 1 until codes.size) {
            val values4 = ContentValues()
            values4.put(C_ID, 1)
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

    // ******************************************************************************************
    // called if newVersion != oldVersion
    // see https://www.androidpit.de/forum/472061/sqliteopenhelper-mit-upgrade-beispielen-und-zentraler-instanz
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

        if (oldVersion == 4) {
            version5(db)
        }
        if (oldVersion == 3) {
            version4(db)
            version5(db)
        }
        if (oldVersion == 2) {
            version3(db)
            version4(db)
            version5(db)
        }
        if (oldVersion == 1) {
            version2(db)
            version3(db)
            version4(db)
            version5(db)
        }
    }

    /*** V2 ***/
    //  DATABASE_VERSION 2: New count columns added to COUNT_TABLE for sexes and stadiums
    private fun version2(db: SQLiteDatabase) {
        // Adds new count columns to TABLE:COUNT
        var sql: String
        var colExist = false

        // add new extra columns to table counts without count_f1i and count_f1e as these are
        //  still represented by count and counta
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_F2I int"
            db.execSQL(sql)
        } catch (e: Exception) {
            colExist = true
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_F3I int"
            db.execSQL(sql)
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_PI int"
            db.execSQL(sql)
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_LI int"
            db.execSQL(sql)
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_EI int"
            db.execSQL(sql)
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_F2E int"
            db.execSQL(sql)
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_F3E int"
            db.execSQL(sql)
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_PE int"
            db.execSQL(sql)
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_LE int"
            db.execSQL(sql)
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_EE int"
            db.execSQL(sql)
        } catch (e: Exception) {
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

        // reset alert data
        sql = "DROP TABLE $ALERT_TABLE"
        db.execSQL(sql)
        sql = ("create table " + ALERT_TABLE + " ("
                + A_ID + " integer primary key, "
                + A_COUNT_ID + " int, "
                + A_ALERT + " int, "
                + A_ALERT_TEXT + " text)")
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
        if (MyDebug.LOG) Log.d(TAG, "374, SECTION_TABLE resetted")

        // reset meta data
        sql = ("UPDATE " + META_TABLE + " SET "
                + M_TEMPE + " = 0, "
                + M_WIND + " = 0, "
                + M_CLOUDS + " = 0, "
                + M_DATE + " = '', "
                + M_START_TM + " = '', "
                + M_END_TM + " = ''")
        db.execSQL(sql)
        if (MyDebug.LOG) Log.d(TAG, "385, META_TABLE resetted")

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
        if (MyDebug.LOG) Log.d(TAG, "413, new empty counts1 table created")

        val specs: List<String> = getAllSpeciesDataSrtCode(db).specs
        val codes: List<String> = getAllSpeciesDataSrtCode(db).codes
        val specsL: List<String> = getAllSpeciesDataSrtCode(db).specsL
        val specNum: Int = codes.size

        if (MyDebug.LOG) Log.d(TAG, "420, Anzahl Spez.: $specNum")

        // fill table for all previous sections with initSpecs data
        var cnti = 1  // count index for new track table
        var speci: Int // species index in initSpecs-array
        var secti = 1 // for new contiguous section index
        var sectIncr = 0
        val sectionList: List<Section> = getAllSects(db)
        for (section in sectionList)
        {
            // for all species of section 1
            speci = 0
            if (MyDebug.LOG) Log.d(TAG, "432, TRACK_TABLE filled for " + section.id)
            while (speci < specNum)
            {
                val values4 = ContentValues()
                values4.put(C_ID, cnti) // id from count index
                values4.put(C_SECTION_ID, secti)
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
                if (MyDebug.LOG) Log.d(TAG, "455, species cnti: " + cnti + ", " + specs[speci])
                speci++
                cnti++
                if (MyDebug.LOG) Log.d(TAG, "458, species cnti: $cnti, index speci: $speci")
            }
            if (MyDebug.LOG) Log.d(TAG, "460, last species-index: $cnti")

            secti++
            sectIncr++
            cnti = (sectIncr * speci) + 1
        }
        sql = "DROP TABLE $COUNT_TABLE"
        db.execSQL(sql)

        sql = "ALTER TABLE $COUNT_TABLE1 RENAME TO $COUNT_TABLE"
        db.execSQL(sql)

        if (MyDebug.LOG) Log.d(TAG, "472, Upgraded database to version 5")
    }

    data class SpcCdsSpL(val specs: List<String>, val codes: List<String>, val specsL: List<String>)

    @SuppressLint("Range")
    private fun getAllSpeciesDataSrtCode(db: SQLiteDatabase): SpcCdsSpL {
        val sNames: MutableList<String> = ArrayList()
        val sCodes: MutableList<String> = ArrayList()
        val sNamesL: MutableList<String> = ArrayList()
        val cursor = db.rawQuery("select * from " + COUNT_TABLE
                    + " WHERE " + " (" + C_SECTION_ID + " = 1) order by " + C_CODE, null)
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
        val cursor = db.rawQuery("select * from " + SECTION_TABLE
                + " order by '" + S_ID + "'", null)
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
        private const val TAG = "TransektCount DBHelper"
        private const val DATABASE_NAME = "transektcount.db"
        private const val DATABASE_VERSION = 5

        // tables
        const val SECTION_TABLE = "sections"
        const val COUNT_TABLE = "counts"
        const val ALERT_TABLE = "alerts"
        const val HEAD_TABLE = "head"
        const val META_TABLE = "meta"
        const val TRACK_TABLE = "tracks"
        const val COUNT_TABLE1 = "counts1" // temporary table for update to version 5

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
        private const val C_COUNT = "count" //deprecated in database version 2
        private const val C_COUNTA = "counta" //deprecated in database version 2

        // fields of table alerts
        const val A_ID = "_id"
        const val A_COUNT_ID = "count_id"
        const val A_ALERT = "alert"
        const val A_ALERT_TEXT = "alert_text"

        // fields of table head
        const val H_ID = "_id"
        const val H_TRANSECT_NO = "transect_no"
        const val H_INSPECTOR_NAME = "inspector_name"

        // fields of table meta
        const val M_ID = "_id"
        const val M_TEMPE = "tempe"
        const val M_WIND = "wind"
        const val M_CLOUDS = "clouds"
        const val M_DATE = "date"
        const val M_START_TM = "start_tm"
        const val M_END_TM = "end_tm"
        private const val M_TEMP = "temp"

        // fields of table tracks
        const val T_ID = "_id"
        const val T_SECTION = "tsection"
        const val T_LAT = "tlat"
        const val T_LON = "tlon"
    }

}
