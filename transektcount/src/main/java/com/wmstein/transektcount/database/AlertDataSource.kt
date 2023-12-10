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
 * last edited in Java on 2022-04-26,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2023-09-23
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
        dbHandler = DbHelper(context!!)
    }

    @Throws(SQLException::class)
    fun open() {
        database = dbHandler.writableDatabase
    }

    fun close() {
        dbHandler.close()
    }

    fun createAlert(countId: Int, alertValue: Int, alertText: String?) {
        val values = ContentValues()
        values.put(DbHelper.A_COUNT_ID, countId)
        values.put(DbHelper.A_ALERT, alertValue)
        values.put(DbHelper.A_ALERT_TEXT, alertText)
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

    fun saveAlert(alertId: Int, alertValue: Int, alertText: String?) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.A_ALERT, alertValue)
        dataToInsert.put(DbHelper.A_ALERT_TEXT, alertText)
        val where = DbHelper.A_ID + " = ?"
        val whereArgs = arrayOf(alertId.toString())
        database!!.update(DbHelper.ALERT_TABLE, dataToInsert, where, whereArgs)
    }

    fun getAllAlertsForCount(countId: Int): List<Alert> {
        val alerts: MutableList<Alert> = ArrayList()
        val cursor = database!!.query(
            DbHelper.ALERT_TABLE, allColumns,
            DbHelper.A_COUNT_ID + " = " + countId, null, null, null, null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val alert = cursorToAlert(cursor)
            alerts.add(alert)
            cursor.moveToNext()
        }
        cursor.close()
        return alerts
    }
}