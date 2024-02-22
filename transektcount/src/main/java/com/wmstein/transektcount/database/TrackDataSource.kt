package com.wmstein.transektcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase

/**********************************
 * Created by wmstein on 2023-09-06
 * last edited on 2023-12-18
 */
class TrackDataSource(context: Context?) {
    // Database fields
    private var database: SQLiteDatabase? = null
    private val dbHandler: DbHelper
    private val allColumns = arrayOf(
        DbHelper.T_ID,      // track point ID
        DbHelper.T_SECTION, // Track name = section name
        DbHelper.T_LAT,     // track point latitude
        DbHelper.T_LON      // track point longitude
    )

    init {
        dbHandler = context?.let { DbHelper(it) }!!
    }

    @Throws(SQLException::class)
    fun open() {
        database = dbHandler.writableDatabase
    }

    fun close() {
        dbHandler.close()
    }

    fun createTrackTp(tsection: String, tlat: String, tlon: String) {
        val values = ContentValues()
        values.put(DbHelper.T_SECTION, tsection)
        values.put(DbHelper.T_LAT, tlat)
        values.put(DbHelper.T_LON, tlon)
        val cursor: Cursor
        val insertId = database!!.insert(DbHelper.TRACK_TABLE, null, values).toInt()
        cursor = database!!.query(
            DbHelper.TRACK_TABLE,
            allColumns, DbHelper.T_ID + " = " + insertId, null,
            null, null, null
        )
        cursor.moveToFirst()
//        val newTrack = cursorToTrack(cursor)
        cursorToTrack(cursor)
        cursor.close()
//        return newTrack
    }

    @SuppressLint("Range")
    private fun cursorToTrack(cursor: Cursor): Track {
        val newtrack = Track()
        newtrack.id = cursor.getInt(cursor.getColumnIndex(DbHelper.T_ID)) // !!!
        newtrack.tsection = cursor.getString(cursor.getColumnIndex(DbHelper.T_SECTION))
        newtrack.tlat = cursor.getString(cursor.getColumnIndex(DbHelper.T_LAT))
        newtrack.tlon = cursor.getString(cursor.getColumnIndex(DbHelper.T_LON))
        return newtrack
    }

    // Used by WelcomeActivity
    fun getAllTrackPoints(): List<Track> {
    val trackpoints: MutableList<Track> = ArrayList()
            val cursor = database!!.rawQuery(
                "select * from " + DbHelper.TRACK_TABLE
                        + " WHERE " + " (" + DbHelper.T_ID + " > 0)"
                        + " order by " + DbHelper.T_ID, null
            )
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val count = cursorToTrack(cursor)
                trackpoints.add(count)
                cursor.moveToNext()
            }
            cursor.close()
            return trackpoints
        }

    // Used by WelcomeActivity
    val diffTrks: Int
        get() {
            var cntTrk = 0
            val cursor = database!!.rawQuery(
                "select DISTINCT " + DbHelper.T_SECTION + " from " + DbHelper.TRACK_TABLE, null
            )
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                cntTrk++
                cursor.moveToNext()
            }
            cursor.close()
            return cntTrk
        }

    // Used by EditSectionActivity
    fun saveTrackName(newName: String?, oldName: String?) {
        if (database!!.isOpen) {
            val dataToInsert = ContentValues()
            dataToInsert.put(DbHelper.T_SECTION, newName)
            val where = DbHelper.T_SECTION + " = ?"
            val whereArgs = arrayOf(oldName.toString())
            database!!.update(DbHelper.TRACK_TABLE, dataToInsert, where, whereArgs)
        }
    }

    // Used by WelcomeActivity
    val hasTrack: Boolean
        get() {
            var hasTrack: Boolean = true
            val cursor = database!!.rawQuery(
                "select exists (select 1 from tracks)", null)

            if (cursor != null) {
                cursor.moveToFirst()
                if (cursor.getInt(0) == 0) {
                    hasTrack = false
                }
            }
            cursor.close()
            return hasTrack
        }

}
