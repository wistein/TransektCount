package com.wmstein.transektcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase

/***********************************
 * Created by wmstein on 2016-03-31,
 * last edited on 2022-04-26,
 * converted to Kotlin on 2023-06-26
 * last edited on 2024-03-09
 */
class MetaDataSource(context: Context?) {
    // Database fields
    private var database: SQLiteDatabase? = null
    private val dbHandler: DbHelper
    private val allColumns = arrayOf(
        DbHelper.M_ID,
        DbHelper.M_TEMPS,
        DbHelper.M_TEMPE,
        DbHelper.M_WINDS,
        DbHelper.M_WINDE,
        DbHelper.M_CLOUDS,
        DbHelper.M_CLOUDE,
        DbHelper.M_DATE,
        DbHelper.M_START_TM,
        DbHelper.M_END_TM
    )

    init {
        dbHandler = DbHelper(context!!)
    }

    @Throws(SQLException::class)
    fun open() {
        database = dbHandler.writableDatabase
    }

    fun close() {
        dbHandler.close()
    }

    fun saveMeta(meta: Meta) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.M_ID, meta.id)
        dataToInsert.put(DbHelper.M_TEMPS, meta.temps)
        dataToInsert.put(DbHelper.M_TEMPE, meta.tempe)
        dataToInsert.put(DbHelper.M_WINDS, meta.winds)
        dataToInsert.put(DbHelper.M_WINDE, meta.winde)
        dataToInsert.put(DbHelper.M_CLOUDS, meta.clouds)
        dataToInsert.put(DbHelper.M_CLOUDE, meta.cloude)
        dataToInsert.put(DbHelper.M_DATE, meta.date)
        dataToInsert.put(DbHelper.M_START_TM, meta.start_tm)
        dataToInsert.put(DbHelper.M_END_TM, meta.end_tm)
        database!!.update(DbHelper.META_TABLE, dataToInsert, null, null)
    }

    @SuppressLint("Range")
    private fun cursorToMeta(cursor: Cursor): Meta {
        val meta = Meta()
        meta.id = cursor.getInt(cursor.getColumnIndex(DbHelper.M_ID))
        meta.temps = cursor.getInt(cursor.getColumnIndex(DbHelper.M_TEMPS))
        meta.tempe = cursor.getInt(cursor.getColumnIndex(DbHelper.M_TEMPE))
        meta.winds = cursor.getInt(cursor.getColumnIndex(DbHelper.M_WINDS))
        meta.winde = cursor.getInt(cursor.getColumnIndex(DbHelper.M_WINDE))
        meta.clouds = cursor.getInt(cursor.getColumnIndex(DbHelper.M_CLOUDS))
        meta.cloude = cursor.getInt(cursor.getColumnIndex(DbHelper.M_CLOUDE))
        meta.date = cursor.getString(cursor.getColumnIndex(DbHelper.M_DATE))
        meta.start_tm = cursor.getString(cursor.getColumnIndex(DbHelper.M_START_TM))
        meta.end_tm = cursor.getString(cursor.getColumnIndex(DbHelper.M_END_TM))
        return meta
    }

    val meta: Meta
        get() {
            val meta: Meta
            val cursor = database!!.query(
                DbHelper.META_TABLE, allColumns, 1.toString(),
                null, null, null, null
            )
            cursor.moveToFirst()
            meta = cursorToMeta(cursor)
            cursor.close()
            return meta
        }
}