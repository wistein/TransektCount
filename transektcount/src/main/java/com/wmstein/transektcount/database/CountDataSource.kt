package com.wmstein.transektcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase

/******************************************************
 * Based on CountDataSource.java by milo on 05/05/2014.
 * Adopted for TransektCount by wmstein on 2016-02-18,
 * last edited in Java on 2022-04-26,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2025-11-01
 */
class CountDataSource(context: Context) {
    // Database fields
    private var database: SQLiteDatabase? = null
    private val dbHelper: DbHelper = DbHelper(context)
    private val allColumns = arrayOf(
        DbHelper.C_ID,
        DbHelper.C_SECTION_ID,
        DbHelper.C_NAME,
        DbHelper.C_CODE,
        DbHelper.C_COUNT_F1I,
        DbHelper.C_COUNT_F2I,
        DbHelper.C_COUNT_F3I,
        DbHelper.C_COUNT_PI,
        DbHelper.C_COUNT_LI,
        DbHelper.C_COUNT_EI,
        DbHelper.C_COUNT_F1E,
        DbHelper.C_COUNT_F2E,
        DbHelper.C_COUNT_F3E,
        DbHelper.C_COUNT_PE,
        DbHelper.C_COUNT_LE,
        DbHelper.C_COUNT_EE,
        DbHelper.C_NOTES,
        DbHelper.C_NAME_G
    )

    @Throws(SQLException::class)
    fun open() {
        database = dbHelper.writableDatabase
    }

    fun close() {
        dbHelper.close()
    }

    // Used by AddSpeciesActivity and CountingActivity
    fun createCount(sectionId: Int, name: String?, code: String?, nameG: String?): Count? {
        return if (database!!.isOpen) {
            val values = ContentValues()
            values.put(DbHelper.C_SECTION_ID, sectionId)
            values.put(DbHelper.C_NAME, name)
            values.put(DbHelper.C_CODE, code)
            values.put(DbHelper.C_COUNT_F1I, 0)
            values.put(DbHelper.C_COUNT_F2I, 0)
            values.put(DbHelper.C_COUNT_F3I, 0)
            values.put(DbHelper.C_COUNT_PI, 0)
            values.put(DbHelper.C_COUNT_LI, 0)
            values.put(DbHelper.C_COUNT_EI, 0)
            values.put(DbHelper.C_COUNT_F1E, 0)
            values.put(DbHelper.C_COUNT_F2E, 0)
            values.put(DbHelper.C_COUNT_F3E, 0)
            values.put(DbHelper.C_COUNT_PE, 0)
            values.put(DbHelper.C_COUNT_LE, 0)
            values.put(DbHelper.C_COUNT_EE, 0)
            values.put(DbHelper.C_NOTES, "")
            values.put(DbHelper.C_NAME_G, nameG)
            val insertId = database!!.insert(DbHelper.COUNT_TABLE, null, values).toInt()
            val cursor = database!!.query(
                DbHelper.COUNT_TABLE,
                allColumns, DbHelper.C_ID + " = " + insertId, null, null,
                null, null
            )
            cursor.moveToFirst()
            val newCount = cursorToCount(cursor)
            cursor.close()
            newCount
        } else null
    }

    @SuppressLint("Range")
    private fun cursorToCount(cursor: Cursor): Count {
        val newcount = Count()
        newcount.id = cursor.getInt(cursor.getColumnIndex(DbHelper.C_ID))
        newcount.section_id = cursor.getInt(cursor.getColumnIndex(DbHelper.C_SECTION_ID))
        newcount.name = cursor.getString(cursor.getColumnIndex(DbHelper.C_NAME))
        newcount.code = cursor.getString(cursor.getColumnIndex(DbHelper.C_CODE))
        newcount.count_f1i = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_F1I))
        newcount.count_f2i = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_F2I))
        newcount.count_f3i = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_F3I))
        newcount.count_pi = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_PI))
        newcount.count_li = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_LI))
        newcount.count_ei = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_EI))
        newcount.count_f1e = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_F1E))
        newcount.count_f2e = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_F2E))
        newcount.count_f3e = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_F3E))
        newcount.count_pe = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_PE))
        newcount.count_le = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_LE))
        newcount.count_ee = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_EE))
        newcount.notes = cursor.getString(cursor.getColumnIndex(DbHelper.C_NOTES))
        newcount.name_g = cursor.getString(cursor.getColumnIndex(DbHelper.C_NAME_G))
        return newcount
    }

    // Used by EditSectionListActivity
    fun deleteAllCountsWithCode(code: String?) {
        val allCtsWithCode: List<Count> = getAllCountsWithCode(code)
        for (count in allCtsWithCode) {
            database!!.delete(DbHelper.COUNT_TABLE,
                DbHelper.C_ID + " = " + count.id, null)
        }

        // delete associated alerts
        for (count in allCtsWithCode) {
            database!!.delete(DbHelper.ALERT_TABLE,
                DbHelper.A_COUNT_ID + " = " + count.id, null)
        }
    }

    private fun getAllCountsWithCode(code: String?): List<Count> {
        val counts: MutableList<Count> = ArrayList()
        val cursor = database!!.rawQuery(
            "select * from " + DbHelper.COUNT_TABLE
                    + " WHERE " + " ("
                    + DbHelper.C_CODE + " = '" + code + "')", null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val count = cursorToCount(cursor)
            counts.add(count)
            cursor.moveToNext()
        }
        cursor.close()
        return counts
    }

    // Used by CountOptionsActivity and CountingActivity
    fun saveCount(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_NAME, count.name)
        dataToInsert.put(DbHelper.C_CODE, count.code)
        dataToInsert.put(DbHelper.C_COUNT_F1I, count.count_f1i)
        dataToInsert.put(DbHelper.C_COUNT_F2I, count.count_f2i)
        dataToInsert.put(DbHelper.C_COUNT_F3I, count.count_f3i)
        dataToInsert.put(DbHelper.C_COUNT_PI, count.count_pi)
        dataToInsert.put(DbHelper.C_COUNT_LI, count.count_li)
        dataToInsert.put(DbHelper.C_COUNT_EI, count.count_ei)
        dataToInsert.put(DbHelper.C_COUNT_F1E, count.count_f1e)
        dataToInsert.put(DbHelper.C_COUNT_F2E, count.count_f2e)
        dataToInsert.put(DbHelper.C_COUNT_F3E, count.count_f3e)
        dataToInsert.put(DbHelper.C_COUNT_PE, count.count_pe)
        dataToInsert.put(DbHelper.C_COUNT_LE, count.count_le)
        dataToInsert.put(DbHelper.C_COUNT_EE, count.count_ee)
        dataToInsert.put(DbHelper.C_NOTES, count.notes)
        dataToInsert.put(DbHelper.C_NAME_G, count.name_g)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_f1i
    fun saveCountf1i(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_F1I, count.count_f1i)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_f2i
    fun saveCountf2i(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_F2I, count.count_f2i)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_f3i
    fun saveCountf3i(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_F3I, count.count_f3i)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_pi
    fun saveCountpi(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_PI, count.count_pi)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_li
    fun saveCountli(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_LI, count.count_li)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_ei
    fun saveCountei(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_EI, count.count_ei)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_f1e
    fun saveCountf1e(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_F1E, count.count_f1e)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_f2e
    fun saveCountf2e(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_F2E, count.count_f2e)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_f3e
    fun saveCountf3e(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_F3E, count.count_f3e)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_pe
    fun saveCountpe(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_PE, count.count_pe)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_le
    fun saveCountle(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_LE, count.count_le)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_ee
    fun saveCountee(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_EE, count.count_ee)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // Used by EditSectionListActivity
    fun updateCountForAllSections(
        sectid: Int,
        countid: Int,
        name: String?,
        code: String?,
        nameG: String?,
    ) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_SECTION_ID, sectid)
        dataToInsert.put(DbHelper.C_NAME, name)
        dataToInsert.put(DbHelper.C_CODE, code)
        dataToInsert.put(DbHelper.C_NAME_G, nameG)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(countid.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // Used by WelcomeActivity
    fun writeCountItem(id: String?, secId: String?, code: String?, name: String?, nameG: String?) {
        if (database!!.isOpen) {
            val values = ContentValues()
            values.put(DbHelper.C_ID, id)
            values.put(DbHelper.C_SECTION_ID, secId)
            values.put(DbHelper.C_NAME, name)
            values.put(DbHelper.C_CODE, code)
            values.put(DbHelper.C_COUNT_F1I, 0)
            values.put(DbHelper.C_COUNT_F2I, 0)
            values.put(DbHelper.C_COUNT_F3I, 0)
            values.put(DbHelper.C_COUNT_PI, 0)
            values.put(DbHelper.C_COUNT_LI, 0)
            values.put(DbHelper.C_COUNT_EI, 0)
            values.put(DbHelper.C_COUNT_F1E, 0)
            values.put(DbHelper.C_COUNT_F2E, 0)
            values.put(DbHelper.C_COUNT_F3E, 0)
            values.put(DbHelper.C_COUNT_PE, 0)
            values.put(DbHelper.C_COUNT_LE, 0)
            values.put(DbHelper.C_COUNT_EE, 0)
            values.put(DbHelper.C_NOTES, "")
            values.put(DbHelper.C_NAME_G, nameG)
            database!!.insert(DbHelper.COUNT_TABLE, null, values)
        }
    }

    // Used by EditSectionListActivity and CountingActivity
    fun getAllCountsForSection(sectionId: Int): List<Count> {
        val counts: MutableList<Count> = ArrayList()
        val cursor = database!!.query(
            DbHelper.COUNT_TABLE, allColumns,
            DbHelper.C_SECTION_ID + " = " + sectionId, null, null, null, null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val count = cursorToCount(cursor)
            counts.add(count)
            cursor.moveToNext()
        }
        cursor.close()
        return counts
    }

    // Used by EditSectionListActivity
    fun getAllSpeciesForSectionSrtCode(sectionId: Int): List<Count> {
        val counts: MutableList<Count> = ArrayList()
        val cursor = database!!.rawQuery(
            "select * from " + DbHelper.COUNT_TABLE
                    + " WHERE " + " ("
                    + DbHelper.C_SECTION_ID + " = " + sectionId
                    + ") order by " + DbHelper.C_CODE, null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val count = cursorToCount(cursor)
            counts.add(count)
            cursor.moveToNext()
        }
        cursor.close()
        return counts
    }

    // Used by EditSectionListActivity
    fun getAllSpeciesForSectionSrtName(sectionId: Int): List<Count> {
        val counts: MutableList<Count> = ArrayList()
        val cursor = database!!.rawQuery(
            "select * from " + DbHelper.COUNT_TABLE
                    + " WHERE " + " ("
                    + DbHelper.C_SECTION_ID + " = " + sectionId
                    + ") order by " + DbHelper.C_NAME, null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val count = cursorToCount(cursor)
            counts.add(count)
            cursor.moveToNext()
        }
        cursor.close()
        return counts
    }

    // Used by ShowResultsActivity
    val allCountsForSrtSectionName: List<Count>
        get() {
            val counts: MutableList<Count> = ArrayList()
            val cursor = database!!.rawQuery(
                "select * from " + DbHelper.COUNT_TABLE
                        + " WHERE " + " ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                        + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                        + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                        + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                        + " order by " + DbHelper.C_SECTION_ID + ", " + DbHelper.C_NAME, null
            )
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val count = cursorToCount(cursor)
                counts.add(count)
                cursor.moveToNext()
            }
            cursor.close()
            return counts
        }

    // Used by ShowResultsActivity
    val allCountsForSrtNameSection: List<Count>
        get() {
            val counts: MutableList<Count> = ArrayList()
            val cursor = database!!.rawQuery(
                "select * from " + DbHelper.COUNT_TABLE
                        + " WHERE " + " ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                        + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                        + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                        + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
                        + " order by " + DbHelper.C_NAME + ", " + DbHelper.C_SECTION_ID, null
            )
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val count = cursorToCount(cursor)
                counts.add(count)
                cursor.moveToNext()
            }
            cursor.close()
            return counts
        }

    // Used by ShowResultsActivity (results page)
    val diffSpec: Int
        get() {
            var cntSpec = 0
            val cursor = database!!.rawQuery(
                "select DISTINCT " + DbHelper.C_CODE + " from " + DbHelper.COUNT_TABLE
                        + " WHERE " + " ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
                        + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
                        + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
                        + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)", null
            )
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                cntSpec++
                cursor.moveToNext()
            }
            cursor.close()
            return cntSpec
        }

    // Used by CountingActivity
    fun getAllIdsForSection(sectionId: Int): Array<String?> {
        val cursor = database!!.query(
            DbHelper.COUNT_TABLE, allColumns,
            DbHelper.C_SECTION_ID + " = " + sectionId, null, null, null, null
        )
        val idArray = arrayOfNulls<String>(cursor.count)
        cursor.moveToFirst()
        var i = 0
        while (!cursor.isAfterLast) {
            val uid = cursor.getInt(0)
            idArray[i] = uid.toString()
            i++
            cursor.moveToNext()
        }
        cursor.close()
        return idArray
    }

    // Used by CountingActivity
    fun getAllIdsForSectionSrtName(sectionId: Int): Array<String?> {
        val cursor = database!!.rawQuery(
            "select ROWID from " + DbHelper.COUNT_TABLE
                    + " WHERE " + DbHelper.C_SECTION_ID + " = " + sectionId + " order by "
                    + DbHelper.C_NAME, null
        )
        val idArray = arrayOfNulls<String>(cursor.count)
        cursor.moveToFirst()
        var i = 0
        while (!cursor.isAfterLast) {
            val uid = cursor.getInt(0)
            idArray[i] = uid.toString()
            i++
            cursor.moveToNext()
        }
        cursor.close()
        return idArray
    }

    // Used by CountingActivity
    fun getAllIdsForSectionSrtCode(sectionId: Int): Array<String?> {
        val cursor = database!!.rawQuery(
            "select ROWID from " + DbHelper.COUNT_TABLE
                    + " WHERE " + DbHelper.C_SECTION_ID + " = " + sectionId + " order by "
                    + DbHelper.C_CODE, null
        )
        val idArray = arrayOfNulls<String>(cursor.count)
        cursor.moveToFirst()
        var i = 0
        while (!cursor.isAfterLast) {
            val uid = cursor.getInt(0)
            idArray[i] = uid.toString()
            i++
            cursor.moveToNext()
        }
        cursor.close()
        return idArray
    }

    // Used by CountingActivity
    fun getAllStringsForSection(sectionId: Int, sname: String?): Array<String?> {
        val cursor = database!!.query(
            DbHelper.COUNT_TABLE, allColumns,
            DbHelper.C_SECTION_ID + " = " + sectionId, null, null, null, null
        )
        val uArray = arrayOfNulls<String>(cursor.count)
        var i = 0
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            @SuppressLint("Range") val uname = cursor.getString(cursor.getColumnIndex(sname))
            uArray[i] = uname
            i++
            cursor.moveToNext()
        }
        cursor.close()
        return uArray
    }

    // Used by CountingActivity
    fun getAllStringsForSectionSrtName(sectionId: Int, sname: String?): Array<String?> {
        val cursor = database!!.rawQuery(
            "select * from " + DbHelper.COUNT_TABLE
                    + " WHERE " + DbHelper.C_SECTION_ID + " = " + sectionId + " order by "
                    + DbHelper.C_NAME, null
        )
        val uArray = arrayOfNulls<String>(cursor.count)
        cursor.moveToFirst()
        var i = 0
        while (!cursor.isAfterLast) {
            @SuppressLint("Range") val uname = cursor.getString(cursor.getColumnIndex(sname))
            uArray[i] = uname
            i++
            cursor.moveToNext()
        }
        cursor.close()
        return uArray
    }

    // Used by CountingActivity
    fun getAllStringsForSectionSrtCode(sectionId: Int, sname: String?): Array<String?> {
        val cursor = database!!.rawQuery(
            "select * from " + DbHelper.COUNT_TABLE
                    + " WHERE " + DbHelper.C_SECTION_ID + " = " + sectionId + " order by "
                    + DbHelper.C_CODE, null
        )
        val uArray = arrayOfNulls<String>(cursor.count)
        cursor.moveToFirst()
        var i = 0
        while (!cursor.isAfterLast) {
            @SuppressLint("Range")
            val uname = cursor.getString(cursor.getColumnIndex(sname))
            uArray[i] = uname
            i++
            cursor.moveToNext()
        }
        cursor.close()
        return uArray
    }

    // Used by CountingActivity and CountOptionsActivity
    fun getCountById(countId: Int): Count {
        val cursor = database!!.query(
            DbHelper.COUNT_TABLE, allColumns,
            DbHelper.C_ID + " = " + countId,
            null, null, null, null
        )
        cursor.moveToFirst()
        val count = cursorToCount(cursor)
        cursor.close()
        return count
    }

    // Sorts COUNT_TABLE for C_SECTION_ID, C_CODE and contiguous index
    fun sortCounts() {
        var sql = "alter table 'counts' rename to 'counts_backup'"
        database!!.execSQL(sql)

        // create new counts table
        sql = ("create table counts("
                + DbHelper.C_ID + " integer primary key, "
                + DbHelper.C_SECTION_ID + " int, "
                + DbHelper.C_NAME + " text, "
                + DbHelper.C_CODE + " text, "
                + DbHelper.C_COUNT_F1I + " int, "
                + DbHelper.C_COUNT_F2I + " int, "
                + DbHelper.C_COUNT_F3I + " int, "
                + DbHelper.C_COUNT_PI + " int, "
                + DbHelper.C_COUNT_LI + " int, "
                + DbHelper.C_COUNT_EI + " int, "
                + DbHelper.C_COUNT_F1E + " int, "
                + DbHelper.C_COUNT_F2E + " int, "
                + DbHelper.C_COUNT_F3E + " int, "
                + DbHelper.C_COUNT_PE + " int, "
                + DbHelper.C_COUNT_LE + " int, "
                + DbHelper.C_COUNT_EE + " int, "
                + DbHelper.C_NOTES + " text, "
                + DbHelper.C_NAME_G + " text)")
        database!!.execSQL(sql)

        // insert the whole COUNT_TABLE data sorted into counts
        sql = ("INSERT INTO 'counts' ('section_id', 'name', 'code', " +
                "'count_f1i', 'count_f2i', 'count_f3i', 'count_pi', 'count_li', 'count_ei', " +
                "'count_f1e', 'count_f2e', 'count_f3e', 'count_pe', 'count_le', 'count_ee', " +
                "'notes', 'name_g') " +
                "SELECT section_id, name, code, " +
                "count_f1i, count_f2i, count_f3i, count_pi, count_li, count_ei, " +
                "count_f1e, count_f2e, count_f3e, count_pe, count_le, count_ee, " +
                "notes, name_g " +
                "from 'counts_backup' order by section_id, code")
        database!!.execSQL(sql)

        sql = "DROP TABLE 'counts_backup'"
        database!!.execSQL(sql)
    }

}
