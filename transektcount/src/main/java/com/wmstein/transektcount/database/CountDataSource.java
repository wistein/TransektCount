package com.wmstein.transektcount.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by milo on 05/05/2014.
 * Changed by wmstein on 18.02.2016
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

    public Count createCount(long section_id, String name)
    {
        ContentValues values = new ContentValues();
        values.put(DbHelper.C_NAME, name);
        values.put(DbHelper.C_SECTION_ID, section_id);
        values.put(DbHelper.C_COUNT, 0);
        values.put(DbHelper.C_COUNTA, 0);
        // notes should be default null and so isn't created here

        long insertId = database.insert(DbHelper.COUNT_TABLE, null, values);
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
        newcount.id = cursor.getLong(cursor.getColumnIndex(DbHelper.C_ID));
        newcount.name = cursor.getString(cursor.getColumnIndex(DbHelper.C_NAME));
        newcount.section_id = cursor.getLong(cursor.getColumnIndex(DbHelper.C_SECTION_ID));
        newcount.count = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT));
        newcount.counta = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNTA));
        newcount.notes = cursor.getString(cursor.getColumnIndex(DbHelper.C_NOTES));
        return newcount;
    }

    public void deleteCount(Count count)
    {
        long id = count.id;
        database.delete(DbHelper.COUNT_TABLE, DbHelper.C_ID + " = " + id, null);

        // delete associated alerts
        database.delete(DbHelper.ALERT_TABLE, DbHelper.A_COUNT_ID + " = " + id, null);
    }

    public void deleteCountById(long id)
    {
        System.out.println("Gelöscht: Zähler mit ID: " + id);
        database.delete(DbHelper.COUNT_TABLE, DbHelper.C_ID + " = " + id, null);

        // delete associated alerts
        database.delete(DbHelper.ALERT_TABLE, DbHelper.A_COUNT_ID + " = " + id, null);
    }

    public void saveCount(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_COUNT, count.count);
        dataToInsert.put(DbHelper.C_COUNTA, count.counta);
        dataToInsert.put(DbHelper.C_NAME, count.name);
        dataToInsert.put(DbHelper.C_NOTES, count.notes);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    public void updateCountName(long id, String name)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_NAME, name);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};
        database.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    public List<Count> getAllCountsForSection(long section_id)
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

    public Count getCountById(long count_id)
    {
        Cursor cursor = database.query(DbHelper.COUNT_TABLE, allColumns,
            DbHelper.C_ID + " = " + count_id, null, null, null, null);

        cursor.moveToFirst();
        Count count = cursorToCount(cursor);
        cursor.close();
        return count;
    }
    
    // Used by ListSpeciesActivity
    public List<Count> getAllSpecies()
    {
        List<Count> species = new ArrayList<>();

        Cursor cursor = database.query(DbHelper.COUNT_TABLE,
            allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Count count = cursorToCount(cursor);
            
            species.add(count);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return species;

    }

    // Used by WelcomeActivity
    public void purgeCounts()
    {
        
    }
    
    // Getting All Counts
    public List<Count> get_Counts()
    {
        count_list.clear();

        // Select All Query
        String selectQuery = "SELECT  * FROM " + DbHelper.COUNT_TABLE +
            " where (" + DbHelper.C_COUNT + " > 0 or " + DbHelper.C_COUNTA + " > 0);";

        Cursor cursor = database.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst())
        {
            do
            {
                Count count = new Count();
                count.setC_ID(Integer.parseInt(cursor.getString(0)));
                count.setC_SECTION_ID(Integer.parseInt(cursor.getString(1)));
                count.setC_COUNT(Integer.parseInt(cursor.getString(2)));
                count.setC_COUNTA(Integer.parseInt(cursor.getString(3)));
                count.setC_NAME(cursor.getString(4));
                count.setC_NOTES(cursor.getString(5));
                // Adding count to list
                count_list.add(count);
            }
            while (cursor.moveToNext());
        }

        // return count list
        cursor.close();
        return count_list;
    }

}
