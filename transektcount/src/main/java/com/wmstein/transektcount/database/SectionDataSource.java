package com.wmstein.transektcount.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.wmstein.transektcount.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/********************************************************
 * Based on ProjectDataSource.java by milo on 05/05/2014.
 * Adopted for TransektCount by wmstein on 2016-02-18,
 * last edited on 2021-01-26
 */
public class SectionDataSource
{
    // Database fields
    private SQLiteDatabase database;
    private final DbHelper dbHandler;
    private final String[] allColumns = {
        DbHelper.S_ID,
        DbHelper.S_CREATED_AT,
        DbHelper.S_NAME,
        DbHelper.S_NOTES
    };

    public SectionDataSource(Context context)
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

    public Section createSection(String name)
    {
        ContentValues values = new ContentValues();
        values.put(DbHelper.S_NAME, name);
        int insertId = (int) database.insert(DbHelper.SECTION_TABLE, null, values);
        Cursor cursor = database.query(DbHelper.SECTION_TABLE,
            allColumns, DbHelper.S_ID + " = " + insertId, null,
            null, null, null);
        cursor.moveToFirst();
        Section newSection = cursorToSection(cursor);
        cursor.close();
        return newSection;
    }

    private Section cursorToSection(Cursor cursor)
    {
        Section section = new Section();
        section.id = cursor.getInt(cursor.getColumnIndex(DbHelper.S_ID));
        section.created_at = cursor.getLong(cursor.getColumnIndex(DbHelper.S_CREATED_AT));
        section.name = cursor.getString(cursor.getColumnIndex(DbHelper.S_NAME));
        section.notes = cursor.getString(cursor.getColumnIndex(DbHelper.S_NOTES));
        return section;
    }

    public void deleteSection(Section section)
    {
        int id = section.id;
        System.out.println((R.string.deletedList) + id);
        database.delete(DbHelper.SECTION_TABLE, DbHelper.S_ID + " = " + id, null);

    /*
    Get the id of all associated counts here; alerts are the only things which can't
    be removed directly as the section_id is not stored in them. A join is therefore required.
     */
        // delete associated links and counts
        String sql = "DELETE FROM " + DbHelper.ALERT_TABLE + " WHERE " + DbHelper.A_COUNT_ID + " IN "
            + "(SELECT " + DbHelper.C_ID + " FROM " + DbHelper.COUNT_TABLE + " WHERE "
            + DbHelper.C_SECTION_ID + " = " + id + ")";
        database.execSQL(sql);
        database.delete(DbHelper.COUNT_TABLE, DbHelper.C_SECTION_ID + " = " + id, null);
    }

    public void saveSection(Section section)
    {
        if (database.isOpen())
        {
            ContentValues dataToInsert = new ContentValues();
            dataToInsert.put(DbHelper.S_NAME, section.name);
            dataToInsert.put(DbHelper.S_NOTES, section.notes);
            String where = DbHelper.S_ID + " = ?";
            String[] whereArgs = {String.valueOf(section.id)};
            database.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs);
        }
    }

    public void saveSectionNotes(Section section)
    {
        if (database.isOpen())
        {
            ContentValues dataToInsert = new ContentValues();
            dataToInsert.put(DbHelper.S_NOTES, section.notes);
            String where = DbHelper.S_ID + " = ?";
            String[] whereArgs = {String.valueOf(section.id)};
            database.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs);
        }
    }

    /******************/
    public void saveDateSection(Section section)
    {
        if (section.created_at == 0)
        {
            Date date = new Date();
            long timeMsec = date.getTime();

            ContentValues values = new ContentValues();
            values.put(DbHelper.S_CREATED_AT, timeMsec);
            String where = DbHelper.S_ID + " = ?";
            String[] whereArgs = {String.valueOf(section.id)};
            database.update(DbHelper.SECTION_TABLE, values, where, whereArgs);
        }
    }

    public List<Section> getAllSections(SharedPreferences prefs)
    {
        List<Section> sections = new ArrayList<>();

        String orderBy;
        String sortString = prefs.getString("pref_sort", "name_asc");
        switch (sortString)
        {
        case "name_desc":
            orderBy = DbHelper.S_NAME + " DESC";
            break;
        case "name_asc":
            orderBy = DbHelper.S_NAME + " ASC";
            break;
        default:
            orderBy = "";
            break;
        }
        Cursor cursor = database.query(DbHelper.SECTION_TABLE, allColumns,
            null, null, null, null, orderBy);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Section section = cursorToSection(cursor);
            sections.add(section);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return sections;
    }

    public List<Section> getAllSectionNames()
    {
        List<Section> sections = new ArrayList<>();

        Cursor cursor = database.query(DbHelper.SECTION_TABLE, allColumns,
            null, null, null, null, null);

        try
        {
            cursor.moveToFirst();

            while (!cursor.isAfterLast())
            {
                Section section = cursorToSect(cursor);
                sections.add(section);
                cursor.moveToNext();
            }
        }catch (Exception e)
        {
            //
        }
        
        // Make sure to close the cursor
        cursor.close();
        return sections;
    }

    // called by List<Section> getAllSectionNames()
    private Section cursorToSect(Cursor cursor)
    {
        Section section = new Section();
        section.id = cursor.getInt(cursor.getColumnIndex(DbHelper.S_ID));
        section.name = cursor.getString(cursor.getColumnIndex(DbHelper.S_NAME));
        return section;
    }

    // called from NewSectionActivity, CountingActivity and EditSectionActivity
    public Section getSection(int section_id)
    {
        Section section;
        Cursor cursor = database.query(DbHelper.SECTION_TABLE, allColumns,
            DbHelper.S_ID + " = ?", new String[]{String.valueOf(section_id)}, null, null, null);
        cursor.moveToFirst();
        section = cursorToSection(cursor);
        // Make sure to close the cursor
        cursor.close();
        return section;
    }

    // called from NewSectionActivity
    public Section getSectionByName(String name)
    {
        Section section;
        Cursor cursor = database.query(DbHelper.SECTION_TABLE, allColumns,
            DbHelper.S_NAME + " = ?", new String[]{name}, null, null, null);
        cursor.moveToFirst();
        section = cursorToSection(cursor);
        // Make sure to close the cursor
        cursor.close();
        return section;
    }


}
