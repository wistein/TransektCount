package com.wmstein.transektcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import java.util.Date
import java.util.Objects

/********************************************************
 * Based on ProjectDataSource.java by milo on 05/05/2014.
 * Adopted for TransektCount by wmstein on 2016-02-18,
 * last edited in Java on 2023-06-23,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2024-12-07
 */
class SectionDataSource(context: Context) {
    // Database fields
    private var database: SQLiteDatabase? = null
    private val dbHandler: DbHelper
    private val allColumns = arrayOf(
        DbHelper.S_ID,
        DbHelper.S_CREATED_AT,
        DbHelper.S_NAME,
        DbHelper.S_NOTES
    )

    init {
        dbHandler = DbHelper(context)
    }

    @Throws(SQLException::class)
    fun open() {
        database = dbHandler.writableDatabase
    }

    fun close() {
        dbHandler.close()
    }

    @SuppressLint("Range")
    fun createSection(name: String?): Section {
        val values = ContentValues()
        values.put(DbHelper.S_NAME, name)
        val cursor: Cursor
        val insertId = database!!.insert(DbHelper.SECTION_TABLE, null, values).toInt()
        cursor = database!!.query(
            DbHelper.SECTION_TABLE,
            allColumns, DbHelper.S_ID + " = " + insertId, null,
            null, null, null
        )
        cursor.moveToFirst()
        val newSection = cursorToSection(cursor)
        cursor.close()
        return newSection
    }

    @SuppressLint("Range")
    private fun cursorToSection(cursor: Cursor): Section {
        val section = Section()
        section.id = cursor.getInt(cursor.getColumnIndex(DbHelper.S_ID))
        section.created_at = cursor.getLong(cursor.getColumnIndex(DbHelper.S_CREATED_AT))
        section.name = cursor.getString(cursor.getColumnIndex(DbHelper.S_NAME))
        section.notes = cursor.getString(cursor.getColumnIndex(DbHelper.S_NOTES))
        return section
    }

    // get number of table entries 
    val numEntries: Int
        get() = DatabaseUtils.queryNumEntries(database, DbHelper.SECTION_TABLE).toInt()

    // get highest ID-number in table sections
    @get:SuppressLint("Range")
    val maxId: Int
        get() {
            val sql =
                "SELECT * FROM " + DbHelper.SECTION_TABLE + " ORDER BY " + DbHelper.S_ID + " DESC LIMIT 1"
            val cursor = database!!.rawQuery(sql, null)
            cursor.moveToFirst()
            val maxId: Int = cursor.getInt(cursor.getColumnIndex(DbHelper.S_ID))
            cursor.close()
            return maxId
        }

    fun deleteSection(section: Section) {
        val id = section.id
        //val sname = section.name
        database!!.delete(DbHelper.SECTION_TABLE, DbHelper.S_ID + " = " + id, null)

        /*
         Delete associated links and counts
         Get the id of all associated counts here; alerts are the only things which can't
         be removed directly as the section_id is not stored in them. A join is therefore required.
        */
        val sql = ("DELETE FROM " + DbHelper.ALERT_TABLE + " WHERE " + DbHelper.A_COUNT_ID + " IN "
                + "(SELECT " + DbHelper.C_ID + " FROM " + DbHelper.COUNT_TABLE + " WHERE "
                + DbHelper.C_SECTION_ID + " = " + id + ")")
        database!!.execSQL(sql)
        database!!.delete(DbHelper.COUNT_TABLE, DbHelper.C_SECTION_ID + " = " + id, null)
    }

    fun saveSection(section: Section) {
        if (database!!.isOpen) {
            val dataToInsert = ContentValues()
            dataToInsert.put(DbHelper.S_NAME, section.name)
            val where = DbHelper.S_ID + " = ?"
            val whereArgs = arrayOf(section.id.toString())
            database!!.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs)
        }
    }

    // save initial date and time to section
    fun saveDateSection(section: Section) {
        if (section.created_at == 0L) {
            val date = Date()
            val timeMsec = date.time
            val values = ContentValues()
            values.put(DbHelper.S_CREATED_AT, timeMsec)
            val where = DbHelper.S_ID + " = ?"
            val whereArgs = arrayOf(section.id.toString())
            database!!.update(DbHelper.SECTION_TABLE, values, where, whereArgs)
        }
    }

    fun getAllSections(prefs: SharedPreferences): List<Section> {
        val sections: MutableList<Section> = ArrayList()
        val orderBy: String
        val sortString = prefs.getString("pref_sort_sect", "name_asc")
        orderBy = when (Objects.requireNonNull(sortString)) {
            "name_desc" -> DbHelper.S_NAME + " DESC"
            "name_asc" -> DbHelper.S_NAME + " ASC"
            else -> ""
        }
        val cursor = database!!.query(
            DbHelper.SECTION_TABLE, allColumns,
            null, null, null, null, orderBy
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val section = cursorToSection(cursor)
            sections.add(section)
            cursor.moveToNext()
        }
        cursor.close()
        return sections
    }

    // Called from NewSectionActivity and EditSectionListActivity
    val allSectionNames: List<Section>
        get() {
            val sections: MutableList<Section> = ArrayList()
            val cursor = database!!.query(
                DbHelper.SECTION_TABLE, allColumns,
                null, null, null, null, null
            )
            try {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val section = cursorToSect(cursor)
                    sections.add(section)
                    cursor.moveToNext()
                }
            } catch (_: Exception) {
                //
            }

            cursor.close()
            return sections
        }

    // called by List<Section> getAllSectionNames()
    @SuppressLint("Range")
    private fun cursorToSect(cursor: Cursor): Section {
        val section = Section()
        section.id = cursor.getInt(cursor.getColumnIndex(DbHelper.S_ID))
        section.name = cursor.getString(cursor.getColumnIndex(DbHelper.S_NAME))
        return section
    }

    // called from WelcomeActivity, NewSectionActivity, CountingActivity and EditSectionListActivity
    fun getSection(sectionId: Int): Section {
        val section: Section
        val cursor = database!!.query(
            DbHelper.SECTION_TABLE, allColumns,
            DbHelper.S_ID + " = ?", arrayOf(sectionId.toString()), null, null, null
        )
        cursor.moveToFirst()
        section = cursorToSection(cursor)
        cursor.close()
        return section
    }

}
