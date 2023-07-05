package com.wmstein.transektcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase

/**********************************
 * Created by milo on 2014-05-05
 * changed by wmstein on 2016-02-18
 * last edited in Java on 2022-04-26
 * converted to Kotlin on 2023-06-26
 */
class AlertDataSource(context: Context?) {
    // Database fields
    private var database: SQLiteDatabase? = null
    private val dbHandler: DbHelper
    private val allColumns = arrayOf(
        DbHelper.A_ID,
        DbHelper.A_COUNT_ID,
        DbHelper.A_ALERT,
        DbHelper.A_ALERT_TEXT
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

    fun createAlert(count_id: Int, alert_value: Int, alert_text: String?) {
        val values = ContentValues()
        values.put(DbHelper.A_COUNT_ID, count_id)
        values.put(DbHelper.A_ALERT, alert_value)
        values.put(DbHelper.A_ALERT_TEXT, alert_text)
        val insertId = database!!.insert(DbHelper.ALERT_TABLE, null, values).toInt()
        val cursor = database!!.query(
            DbHelper.ALERT_TABLE,
            allColumns, DbHelper.A_ID + " = " + insertId, null,
            null, null, null
        )
        cursor.close()
    }

    @SuppressLint("Range")
    private fun cursorToAlert(cursor: Cursor): Alert {
        val newalert = Alert()
        newalert.id = cursor.getInt(cursor.getColumnIndex(DbHelper.A_ID))
        newalert.count_id = cursor.getInt(cursor.getColumnIndex(DbHelper.A_COUNT_ID))
        newalert.alert = cursor.getInt(cursor.getColumnIndex(DbHelper.A_ALERT))
        newalert.alert_text = cursor.getString(cursor.getColumnIndex(DbHelper.A_ALERT_TEXT))
        return newalert
    }

    fun deleteAlertById(id: Int) {
        println("Alert deleted with id: $id")
        database!!.delete(DbHelper.ALERT_TABLE, DbHelper.A_ID + " = " + id, null)
    }

    fun saveAlert(alert_id: Int, alert_value: Int, alert_text: String?) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.A_ALERT, alert_value)
        dataToInsert.put(DbHelper.A_ALERT_TEXT, alert_text)
        val where = DbHelper.A_ID + " = ?"
        val whereArgs = arrayOf(alert_id.toString())
        database!!.update(DbHelper.ALERT_TABLE, dataToInsert, where, whereArgs)
    }

    fun getAllAlertsForCount(count_id: Int): List<Alert> {
        val alerts: MutableList<Alert> = ArrayList()
        val cursor = database!!.query(
            DbHelper.ALERT_TABLE, allColumns,
            DbHelper.A_COUNT_ID + " = " + count_id, null, null, null, null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val alert = cursorToAlert(cursor)
            alerts.add(alert)
            cursor.moveToNext()
        }
        // Make sure to close the cursor
        cursor.close()
        return alerts
    }
}