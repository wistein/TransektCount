package com.wmstein.transektcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

/**********************************
 * Created by wmstein on 2023-09-06
 * last edited on 2025-12-29
 */
class TrackDataSource(context: Context) {
    // Database fields
    private var database: SQLiteDatabase? = null
    private val dbHelper: DbHelper = DbHelper(context)
    private val allColumns = arrayOf(
        DbHelper.T_ID,      // track point ID
        DbHelper.T_SECTION, // Track name = section name
        DbHelper.T_LAT,     // track point latitude
        DbHelper.T_LON      // track point longitude
    )

    fun open() {
        database = dbHelper.writableDatabase
    }

    fun close() {
        dbHelper.close()
    }

    fun createTrackTp(tsection: String, tlat: String, tlon: String) {
        val values = ContentValues()
        values.put(DbHelper.T_SECTION, tsection)
        values.put(DbHelper.T_LAT, tlat)
        values.put(DbHelper.T_LON, tlon)
        val insertId = database!!.insert(DbHelper.TRACK_TABLE, null, values).toInt()
        val cursor: Cursor = database!!.query(
            DbHelper.TRACK_TABLE,
            allColumns, DbHelper.T_ID + " = " + insertId, null,
            null, null, null
        )
        cursor.close()
    }

    @SuppressLint("Range")
    private fun cursorToTrack(cursor: Cursor): Track {
        val newTrack = Track()
        newTrack.id = cursor.getInt(cursor.getColumnIndex(DbHelper.T_ID)) // ! must be primary key
        newTrack.tsection = cursor.getString(cursor.getColumnIndex(DbHelper.T_SECTION))
        newTrack.tlat = cursor.getString(cursor.getColumnIndex(DbHelper.T_LAT))
        newTrack.tlon = cursor.getString(cursor.getColumnIndex(DbHelper.T_LON))
        return newTrack
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
            val trkPt = cursorToTrack(cursor)
            trackpoints.add(trkPt)
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

    // Delete track points for last section to be deleted
    fun deleteAllTrkPointsOfSection(sectName: String?) {
        val allTrkPtsOfSect: List<Track> = getAllTrkPtsOfSection(sectName)
        for (trkPt in allTrkPtsOfSect) {
            database!!.delete(
                DbHelper.TRACK_TABLE,
                DbHelper.T_ID + " = " + trkPt.id, null
            )
        }
    }

    private fun getAllTrkPtsOfSection(sectName: String?): List<Track> {
        val trkPts: MutableList<Track> = ArrayList()
        val cursor = database!!.rawQuery(
            "select * from " + DbHelper.TRACK_TABLE
                    + " WHERE " + " (" + DbHelper.T_SECTION + " = '" + sectName + "')", null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val trkPt = cursorToTrack(cursor)
            trkPts.add(trkPt)
            cursor.moveToNext()
        }
        cursor.close()
        return trkPts
    }

    // Used by EditSectionListActivity
    fun saveTrackName(newName: String?, oldName: String?) {
        if (database!!.isOpen) {
            val dataToInsert = ContentValues()
            dataToInsert.put(DbHelper.T_SECTION, newName)
            val where = DbHelper.T_SECTION + " = ?"
            val whereArgs = arrayOf(oldName.toString())
            database!!.update(DbHelper.TRACK_TABLE, dataToInsert, where, whereArgs)
        }
    }

}
