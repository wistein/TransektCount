package com.wmstein.transektcount.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/******************************************************
 * Based on CountDataSource.java by milo on 05/05/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016
 */
public class CountDataSource
{
    // Database fields
    private SQLiteDatabase database;
    private DbHelper dbHandler;
    private String[] allColumns = {
        DbHelper.C_ID,
        DbHelper.C_SECTION_ID,
        DbHelper.C_COUNT,
        DbHelper.C_COUNTA,
        DbHelper.C_NAME,
        DbHelper.C_CODE,
        DbHelper.C_NOTES
    };

    public List<Count> count_list;

    public CountDataSource(Context context)
    {
        dbHandler = new DbHelper(context);
    }

    public void open() throws SQLException
    {
        database = dbHandler.getWritableDatabase();
    }

    public void close()
    {
        dbHandler.close();
    }

    // Used by EditSectionActivity and CountingActivity
    public Count createCount(int section_id, String name, String code)
    {
        ContentValues values = new ContentValues();
        values.put(DbHelper.C_SECTION_ID, section_id);
        values.put(DbHelper.C_COUNT, 0);
        values.put(DbHelper.C_COUNTA, 0);
        values.put(DbHelper.C_NAME, name);
        values.put(DbHelper.C_CODE, code);
        // notes should be default null and so is not created here

        int insertId = (int) database.insert(DbHelper.COUNT_TABLE, null, values);
        Cursor cursor = database.query(DbHelper.COUNT_TABLE,
            allColumns, DbHelper.C_ID + " = " + insertId, null, null, null, null);
        cursor.moveToFirst();
        Count newCount = cursorToCount(cursor);
        cursor.close();
        return newCount;
    }

    private Count cursorToCount(Cursor cursor)
    {
        Count newcount = new Count();
        newcount.id = cursor.getInt(cursor.getColumnIndex(DbHelper.C_ID));
        newcount.section_id = cursor.getInt(cursor.getColumnIndex(DbHelper.C_SECTION_ID));
        newcount.count = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT));
        newcount.counta = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNTA));
        newcount.name = cursor.getString(cursor.getColumnIndex(DbHelper.C_NAME));
        newcount.code = cursor.getString(cursor.getColumnIndex(DbHelper.C_CODE));
        newcount.notes = cursor.getString(cursor.getColumnIndex(DbHelper.C_NOTES));
        return newcount;
    }

    // Used by EditSectionActivity
    public void deleteCountById(int id)
    {
        System.out.println("Gelöscht: Zähler mit ID: " + id);
        database.delete(DbHelper.COUNT_TABLE, DbHelper.C_ID + " = " + id, null);

        // delete associated alerts
        database.delete(DbHelper.ALERT_TABLE, DbHelper.A_COUNT_ID + " = " + id, null);
    }

    // Used by CountOptionsActivity and CountingActivity
    public void saveCount(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_COUNT, count.count);
        dataToInsert.put(DbHelper.C_COUNTA, count.counta);
        dataToInsert.put(DbHelper.C_NAME, count.name);
        dataToInsert.put(DbHelper.C_CODE, count.code);
        dataToInsert.put(DbHelper.C_NOTES, count.notes);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // Used by EditSectionActivity
    public void updateCountName(int id, String name, String code)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_NAME, name);
        dataToInsert.put(DbHelper.C_CODE, code);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};
        database.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // Used by EditSectionActivity and CountingActivity
    public List<Count> getAllCountsForSection(int section_id)
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.query(DbHelper.COUNT_TABLE, allColumns,
            DbHelper.C_SECTION_ID + " = " + section_id, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Count count = cursorToCount(cursor);
            counts.add(count);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return counts;
    }

    // Used by CountingActivity
    public List<Count> getAllCountsForSectionSrtName(int section_id)
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
            + " WHERE " + DbHelper.C_SECTION_ID + " = " + section_id + " order by "
            + DbHelper.C_NAME, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Count count = cursorToCount(cursor);
            counts.add(count);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return counts;
    }

    // Used by CountingActivity
    public List<Count> getAllCountsForSectionSrtCode(int section_id)
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
            + " WHERE " + DbHelper.C_SECTION_ID + " = " + section_id + " order by "
            + DbHelper.C_CODE, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Count count = cursorToCount(cursor);
            counts.add(count);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return counts;
    }

    // Used by CountOptionsActivity
    public Count getCountById(int count_id)
    {
        Cursor cursor = database.query(DbHelper.COUNT_TABLE, allColumns,
            DbHelper.C_ID + " = " + count_id, null, null, null, null);

        cursor.moveToFirst();
        Count count = cursorToCount(cursor);
        cursor.close();
        return count;
    }

    // Used by ListSpeciesActivity
    public List<Count> getAllSpecsForSectionSrtName(int section_id)
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
            + " WHERE " + DbHelper.C_SECTION_ID + " = " + section_id + " and ("
            + DbHelper.C_COUNT + " > 0 or " + DbHelper.C_COUNTA + " > 0)"
            + " order by " + DbHelper.C_NAME, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Count count = cursorToCount(cursor);
            counts.add(count);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return counts;
    }

    // Used by ListSpeciesActivity
    public List<Count> getAllSpecsForSectionSrtCode(int section_id)
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
            + " WHERE " + DbHelper.C_SECTION_ID + " = " + section_id + " and ("
            + DbHelper.C_COUNT + " > 0 or " + DbHelper.C_COUNTA + " > 0)"
            + " order by " + DbHelper.C_CODE, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Count count = cursorToCount(cursor);
            counts.add(count);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return counts;
    }

    // Used by ListSpeciesActivity
    public List<Count> getAllSpecsForSection(int section_id)
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
            + " WHERE " + DbHelper.C_SECTION_ID + " = " + section_id + " and ("
            + DbHelper.C_COUNT + " > 0 or " + DbHelper.C_COUNTA + " > 0)"
            + " order by " + DbHelper.C_ID, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Count count = cursorToCount(cursor);
            counts.add(count);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return counts;
    }

}
