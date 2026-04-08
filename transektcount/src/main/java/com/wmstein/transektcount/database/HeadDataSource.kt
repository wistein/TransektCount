package com.wmstein.transektcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

/******************************************************
 * Class HeadDataSource provides methods for table Head
 *
 * Created by wmstein on 31.03.2016,
 * last edited in Java on 2022-04-26,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2026-04-07
 */
class HeadDataSource(context: Context) {
    // Database fields
    private var database: SQLiteDatabase? = null
    private val dbHelper: DbHelper = DbHelper(context)
    private val allColumns = arrayOf(
        DbHelper.H_ID,
        DbHelper.H_TRANSECT_NO,
        DbHelper.H_INSPECTOR_NAME,
        DbHelper.H_DATA_LANGUAGE
    )

    fun open() {
        database = dbHelper.writableDatabase
    }

    fun close() {
        dbHelper.close()
    }

    fun saveHead(head: Head) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.H_ID, head.id)
        dataToInsert.put(DbHelper.H_TRANSECT_NO, head.transect_no)
        dataToInsert.put(DbHelper.H_INSPECTOR_NAME, head.inspector_name)
        dataToInsert.put(DbHelper.H_DATA_LANGUAGE, head.data_language)
        database!!.update(DbHelper.HEAD_TABLE, dataToInsert, null, null)
    }

    val head: Head
        get() {
            val head: Head
            val cursor = database!!.query(
                DbHelper.HEAD_TABLE,
                allColumns,
                1.toString(),
                null,
                null,
                null,
                null
            )
            cursor.moveToFirst()
            head = cursorToHead(cursor)
            cursor.close()
            return head
        }

    @SuppressLint("Range")
    private fun cursorToHead(cursor: Cursor): Head {
        val head = Head()
        head.id = cursor.getInt(cursor.getColumnIndex(DbHelper.H_ID))
        head.transect_no = cursor.getString(cursor.getColumnIndex(DbHelper.H_TRANSECT_NO))
        head.inspector_name = cursor.getString(cursor.getColumnIndex(DbHelper.H_INSPECTOR_NAME))
        head.data_language = cursor.getString(cursor.getColumnIndex(DbHelper.H_DATA_LANGUAGE))
        return head
    }

}