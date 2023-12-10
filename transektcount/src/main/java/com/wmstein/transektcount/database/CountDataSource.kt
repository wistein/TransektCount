package com.wmstein.transektcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import com.wmstein.transektcount.TransektCountApplication

/******************************************************
 * Based on CountDataSource.java by milo on 05/05/2014.
 * Adopted for TransektCount by wmstein on 2016-02-18,
 * last edited in Java on 2022-04-26,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2023-12-08
 */
class CountDataSource(context: Context?) {
    // Database fields
    private val transektCountApp = TransektCountApplication()
    private var database: SQLiteDatabase? = null
    private val dbHandler: DbHelper
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

    // Used by EditSectionActivity and CountingActivity
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

    // Used by EditSectionActivity
    fun deleteAllCountsWithCode(code: String?) {
        println("116, CntDataSrc, deleteAllCountsWithCode, Löschen: Count mit Code: $code")
        val allCtsWithCode: List<Count> = getAllCountsWithCode(code)
        for (count in allCtsWithCode) {
            println("119, deleteAllCountsWithCode, cntId: $count.id")
            database!!.delete(DbHelper.COUNT_TABLE, DbHelper.C_ID + " = " + count.id, null)
        }

        // delete associated alerts
        // get array of all IDs by code
        for (count in allCtsWithCode) {
            database!!.delete(DbHelper.ALERT_TABLE, DbHelper.A_COUNT_ID + " = " + count.id, null)
        }
    }

    private fun getAllCountsWithCode(code: String?): List<Count>
        {
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

    // Used by WelcomeActivity
    // get number of counts of mfe (f1e) for code
    fun getMFEWithCode(code: String?): Int
    {
        var numCode = 0
        val cursor = database!!.rawQuery(
            "select * from " + DbHelper.COUNT_TABLE
                    + " WHERE " + " ("
                    + DbHelper.C_CODE + " = '" + code + "' AND "
                    + DbHelper.C_COUNT_F1E + " > 0)", null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val count = cursorToCount(cursor)
            numCode += count.count_f1e
            cursor.moveToNext()
        }
        cursor.close()
        return numCode
    }

    fun getMEWithCode(code: String?): Int
    {
        var numCode = 0
        val cursor = database!!.rawQuery(
            "select * from " + DbHelper.COUNT_TABLE
                    + " WHERE " + " ("
                    + DbHelper.C_CODE + " = '" + code + "' AND "
                    + DbHelper.C_COUNT_F2E + " > 0)", null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val count = cursorToCount(cursor)
            numCode += count.count_f2e
            cursor.moveToNext()
        }
        cursor.close()
        return numCode
    }

    fun getFEWithCode(code: String?): Int
    {
        var numCode = 0
        val cursor = database!!.rawQuery(
            "select * from " + DbHelper.COUNT_TABLE
                    + " WHERE " + " ("
                    + DbHelper.C_CODE + " = '" + code + "' AND "
                    + DbHelper.C_COUNT_F3E + " > 0)", null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val count = cursorToCount(cursor)
            numCode += count.count_f3e
            cursor.moveToNext()
        }
        cursor.close()
        return numCode
    }

    fun getLEWithCode(code: String?): Int
    {
        var numCode = 0
        val cursor = database!!.rawQuery(
            "select * from " + DbHelper.COUNT_TABLE
                    + " WHERE " + " ("
                    + DbHelper.C_CODE + " = '" + code + "' AND "
                    + DbHelper.C_COUNT_LE + " > 0)", null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val count = cursorToCount(cursor)
            numCode += count.count_le
            cursor.moveToNext()
        }
        cursor.close()
        return numCode
    }

    fun getPEWithCode(code: String?): Int
    {
        var numCode = 0
        val cursor = database!!.rawQuery(
            "select * from " + DbHelper.COUNT_TABLE
                    + " WHERE " + " ("
                    + DbHelper.C_CODE + " = '" + code + "' AND "
                    + DbHelper.C_COUNT_PE + " > 0)", null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val count = cursorToCount(cursor)
            numCode += count.count_pe
            cursor.moveToNext()
        }
        cursor.close()
        return numCode
    }

    fun getEEWithCode(code: String?): Int
    {
        var numCode = 0
        val cursor = database!!.rawQuery(
            "select * from " + DbHelper.COUNT_TABLE
                    + " WHERE " + " ("
                    + DbHelper.C_CODE + " = '" + code + "' AND "
                    + DbHelper.C_COUNT_EE + " > 0)", null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val count = cursorToCount(cursor)
            numCode += count.count_ee
            cursor.moveToNext()
        }
        cursor.close()
        return numCode
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

    // Used by EditSectionActivity
    fun updateCountName(id: Int, name: String?, code: String?, nameG: String?) {
        if (database!!.isOpen) {
            val dataToInsert = ContentValues()
            dataToInsert.put(DbHelper.C_NAME, name)
            dataToInsert.put(DbHelper.C_CODE, code)
            dataToInsert.put(DbHelper.C_NAME_G, nameG)
            val where = DbHelper.C_ID + " = ?"
            val whereArgs = arrayOf(id.toString())
            database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
        }
    }

    // Used by EditSectionActivity and CountingActivity
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

    // Used by EditSectionActivity
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

    // Used by EditSectionActivity
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

    // Used by ListSpeciesActivity
    val allCountsForSrtName: List<Count>
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

    // Used by ListSpeciesActivity
    val allCountsForSrtCode: List<Count>
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
                        + " order by " + DbHelper.C_CODE + ", " + DbHelper.C_SECTION_ID, null
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

    // Used by ListSpeciesActivity and WelcomeActivity
    val allCounts: List<Count>
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
                        + " order by " + DbHelper.C_SECTION_ID, null
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

    // Used by ListSpeciesActivity (results page)
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

    // Used by CountingActivity
    fun getAllImagesForSection(sectionId: Int): Array<Int?> {
        val cursor = database!!.query(
            DbHelper.COUNT_TABLE, allColumns,
            DbHelper.C_SECTION_ID + " = " + sectionId, null, null, null, null
        )
        val imageArray = arrayOfNulls<Int>(cursor.count)
        cursor.moveToFirst()
        var i = 0
        while (!cursor.isAfterLast) {
            @SuppressLint("Range")
            val ucode = cursor.getString(cursor.getColumnIndex("code"))
            val rname = "p$ucode" // species picture resource name
            val resId = transektCountApp.getResID(rname)
            val resId0 = transektCountApp.getResID("p00000")
            if (resId != 0) {
                imageArray[i] = resId
            } else {
                imageArray[i] = resId0
            }
            i++
            cursor.moveToNext()
        }
        cursor.close()
        return imageArray
    }

    // Used by CountingActivity
    fun getAllImagesForSectionSrtName(sectionId: Int): Array<Int?> {
        val cursor = database!!.rawQuery(
            "select * from " + DbHelper.COUNT_TABLE
                    + " WHERE " + DbHelper.C_SECTION_ID + " = " + sectionId + " order by "
                    + DbHelper.C_NAME, null
        )
        val imageArray = arrayOfNulls<Int>(cursor.count)
        cursor.moveToFirst()
        var i = 0
        while (!cursor.isAfterLast) {
            @SuppressLint("Range")
            val ucode = cursor.getString(cursor.getColumnIndex("code"))
            val rname = "p$ucode" // species picture resource name
            val resId = transektCountApp.getResID(rname)
            val resId0 = transektCountApp.getResID("p00000")
            if (resId != 0) {
                imageArray[i] = resId
            } else {
                imageArray[i] = resId0
            }
            i++
            cursor.moveToNext()
        }
        cursor.close()
        return imageArray
    }

    // Used by CountingActivity
    fun getAllImagesForSectionSrtCode(sectionId: Int): Array<Int?> {
        val cursor = database!!.rawQuery(
            "select * from " + DbHelper.COUNT_TABLE
                    + " WHERE " + DbHelper.C_SECTION_ID + " = " + sectionId + " order by "
                    + DbHelper.C_CODE, null
        )
        val imageArray = arrayOfNulls<Int>(cursor.count)
        cursor.moveToFirst()
        var i = 0
        while (!cursor.isAfterLast) {
            @SuppressLint("Range") val ucode = cursor.getString(cursor.getColumnIndex("code"))
            val rname = "p$ucode" // species picture resource name
            val resId = transektCountApp.getResID(rname)
            val resId0 = transektCountApp.getResID("p00000")
            if (resId != 0) {
                imageArray[i] = resId
            } else {
                imageArray[i] = resId0
            }
            i++
            cursor.moveToNext()
        }
        cursor.close()
        return imageArray
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

    // Used by CountingActivity
    fun getCodeById(countId: Int): String? {
        val cursor = database!!.query(
            DbHelper.COUNT_TABLE, allColumns,
            DbHelper.C_ID + " = " + countId,
            null, null, null, null
        )
        cursor.moveToFirst()
        val count = cursorToCount(cursor)
        cursor.close()
        return count.code
    }

    // Used by CountingActivityA
    fun getIdBySectionAndCode(sectionId: Int, countCode: String): Int {
        val cursor = database!!.rawQuery("select * from "
           + DbHelper.COUNT_TABLE + " where "
           + DbHelper.C_SECTION_ID + " = " + sectionId
           + " AND " + DbHelper.C_CODE + " = '" + countCode + "'",
            null
        )
        cursor.moveToFirst()
        val count = cursorToCount(cursor)
        cursor.close()
        return count.id
    }

}
