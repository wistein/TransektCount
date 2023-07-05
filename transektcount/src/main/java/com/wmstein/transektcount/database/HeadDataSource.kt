package com.wmstein.transektcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase

/***********************************
 * Created by wmstein on 31.03.2016.
 * Last edited in Java on 2022-04-26,
 * converted to Kotlin on 2023-06-26
 */
class HeadDataSource(context: Context?) {
    // Database fields
    private var database: SQLiteDatabase? = null
    private val dbHandler: DbHelper
    private val allColumns = arrayOf(
        DbHelper.H_ID,
        DbHelper.H_TRANSECT_NO,
        DbHelper.H_INSPECTOR_NAME
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

    fun saveHead(head: Head) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.H_ID, head.id)
        dataToInsert.put(DbHelper.H_TRANSECT_NO, head.transect_no)
        dataToInsert.put(DbHelper.H_INSPECTOR_NAME, head.inspector_name)
        database!!.update(DbHelper.HEAD_TABLE, dataToInsert, null, null)
    }

    @SuppressLint("Range")
    private fun cursorToHead(cursor: Cursor): Head {
        val head = Head()
        head.id = cursor.getInt(cursor.getColumnIndex(DbHelper.H_ID))
        head.transect_no = cursor.getString(cursor.getColumnIndex(DbHelper.H_TRANSECT_NO))
        head.inspector_name = cursor.getString(cursor.getColumnIndex(DbHelper.H_INSPECTOR_NAME))
        return head
    }

    // Make sure to close the cursor
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
            // Make sure to close the cursor
            cursor.close()
            return head
        }
}