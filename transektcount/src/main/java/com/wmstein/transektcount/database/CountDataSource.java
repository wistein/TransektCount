package com.wmstein.transektcount.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.wmstein.transektcount.TransektCountApplication;

import java.util.ArrayList;
import java.util.List;

import static com.wmstein.transektcount.database.DbHelper.COUNT_TABLE;
import static com.wmstein.transektcount.database.DbHelper.C_CODE;
import static com.wmstein.transektcount.database.DbHelper.C_COUNT_EE;
import static com.wmstein.transektcount.database.DbHelper.C_COUNT_EI;
import static com.wmstein.transektcount.database.DbHelper.C_COUNT_F1E;
import static com.wmstein.transektcount.database.DbHelper.C_COUNT_F1I;
import static com.wmstein.transektcount.database.DbHelper.C_COUNT_F2E;
import static com.wmstein.transektcount.database.DbHelper.C_COUNT_F2I;
import static com.wmstein.transektcount.database.DbHelper.C_COUNT_F3E;
import static com.wmstein.transektcount.database.DbHelper.C_COUNT_F3I;
import static com.wmstein.transektcount.database.DbHelper.C_COUNT_LE;
import static com.wmstein.transektcount.database.DbHelper.C_COUNT_LI;
import static com.wmstein.transektcount.database.DbHelper.C_COUNT_PE;
import static com.wmstein.transektcount.database.DbHelper.C_COUNT_PI;
import static com.wmstein.transektcount.database.DbHelper.C_ID;
import static com.wmstein.transektcount.database.DbHelper.C_NAME;
import static com.wmstein.transektcount.database.DbHelper.C_NAME_G;
import static com.wmstein.transektcount.database.DbHelper.C_NOTES;
import static com.wmstein.transektcount.database.DbHelper.C_SECTION_ID;

/******************************************************
 * Based on CountDataSource.java by milo on 05/05/2014.
 * Adopted for TransektCount by wmstein on 2016-02-18,
 * last edited on 2022-04-26.
 */
public class CountDataSource
{
    // Database fields
    private SQLiteDatabase database;
    private final DbHelper dbHandler;
    private final String[] allColumns = {
        C_ID,
        C_SECTION_ID,
        C_NAME,
        C_CODE,
        C_COUNT_F1I,
        C_COUNT_F2I,
        C_COUNT_F3I,
        C_COUNT_PI,
        C_COUNT_LI,
        C_COUNT_EI,
        C_COUNT_F1E,
        C_COUNT_F2E,
        C_COUNT_F3E,
        C_COUNT_PE,
        C_COUNT_LE,
        C_COUNT_EE,
        C_NOTES,
        C_NAME_G
    };

    private final TransektCountApplication transektCountApp = new TransektCountApplication();

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
    public Count createCount(int section_id, String name, String code, String name_g)
    {
        if (database.isOpen())
        {
            ContentValues values = new ContentValues();
            values.put(C_SECTION_ID, section_id);
            values.put(C_NAME, name);
            values.put(C_CODE, code);
            values.put(C_COUNT_F1I, 0);
            values.put(C_COUNT_F2I, 0);
            values.put(C_COUNT_F3I, 0);
            values.put(C_COUNT_PI, 0);
            values.put(C_COUNT_LI, 0);
            values.put(C_COUNT_EI, 0);
            values.put(C_COUNT_F1E, 0);
            values.put(C_COUNT_F2E, 0);
            values.put(C_COUNT_F3E, 0);
            values.put(C_COUNT_PE, 0);
            values.put(C_COUNT_LE, 0);
            values.put(C_COUNT_EE, 0);
            values.put(C_NOTES, "");
            values.put(C_NAME_G, name_g);

            int insertId = (int) database.insert(COUNT_TABLE, null, values);
            Cursor cursor = database.query(COUNT_TABLE,
                allColumns, C_ID + " = " + insertId, null, null, null, null);
            cursor.moveToFirst();
            Count newCount = cursorToCount(cursor);
            cursor.close();
            return newCount;
        }
        else
            return null;
    }
    
    @SuppressLint("Range")
    private Count cursorToCount(Cursor cursor)
    {
        Count newcount = new Count();
        newcount.id = cursor.getInt(cursor.getColumnIndex(C_ID));
        newcount.section_id = cursor.getInt(cursor.getColumnIndex(C_SECTION_ID));
        newcount.name = cursor.getString(cursor.getColumnIndex(C_NAME));
        newcount.code = cursor.getString(cursor.getColumnIndex(C_CODE));
        newcount.count_f1i = cursor.getInt(cursor.getColumnIndex(C_COUNT_F1I));
        newcount.count_f2i = cursor.getInt(cursor.getColumnIndex(C_COUNT_F2I));
        newcount.count_f3i = cursor.getInt(cursor.getColumnIndex(C_COUNT_F3I));
        newcount.count_pi = cursor.getInt(cursor.getColumnIndex(C_COUNT_PI));
        newcount.count_li = cursor.getInt(cursor.getColumnIndex(C_COUNT_LI));
        newcount.count_ei = cursor.getInt(cursor.getColumnIndex(C_COUNT_EI));
        newcount.count_f1e = cursor.getInt(cursor.getColumnIndex(C_COUNT_F1E));
        newcount.count_f2e = cursor.getInt(cursor.getColumnIndex(C_COUNT_F2E));
        newcount.count_f3e = cursor.getInt(cursor.getColumnIndex(C_COUNT_F3E));
        newcount.count_pe = cursor.getInt(cursor.getColumnIndex(C_COUNT_PE));
        newcount.count_le = cursor.getInt(cursor.getColumnIndex(C_COUNT_LE));
        newcount.count_ee = cursor.getInt(cursor.getColumnIndex(C_COUNT_EE));
        newcount.notes = cursor.getString(cursor.getColumnIndex(C_NOTES));
        newcount.name_g = cursor.getString(cursor.getColumnIndex(C_NAME_G));
        return newcount;
    }

    // Used by EditSectionActivity
    public void deleteCountById(int id)
    {
        System.out.println("Gelöscht: Zähler mit ID: " + id);
        database.delete(COUNT_TABLE, C_ID + " = " + id, null);

        // delete associated alerts
        database.delete(DbHelper.ALERT_TABLE, DbHelper.A_COUNT_ID + " = " + id, null);
    }

    // Used by CountOptionsActivity and CountingActivity
    public void saveCount(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_NAME, count.name);
        dataToInsert.put(C_CODE, count.code);
        dataToInsert.put(C_COUNT_F1I, count.count_f1i);
        dataToInsert.put(C_COUNT_F2I, count.count_f2i);
        dataToInsert.put(C_COUNT_F3I, count.count_f3i);
        dataToInsert.put(C_COUNT_PI, count.count_pi);
        dataToInsert.put(C_COUNT_LI, count.count_li);
        dataToInsert.put(C_COUNT_EI, count.count_ei);
        dataToInsert.put(C_COUNT_F1E, count.count_f1e);
        dataToInsert.put(C_COUNT_F2E, count.count_f2e);
        dataToInsert.put(C_COUNT_F3E, count.count_f3e);
        dataToInsert.put(C_COUNT_PE, count.count_pe);
        dataToInsert.put(C_COUNT_LE, count.count_le);
        dataToInsert.put(C_COUNT_EE, count.count_ee);
        dataToInsert.put(C_NOTES, count.notes);
        dataToInsert.put(C_NAME_G, count.name_g);
        String where = C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_f1i
    public void saveCountf1i(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_F1I, count.count_f1i);
        String where = C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_f2i
    public void saveCountf2i(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_F2I, count.count_f2i);
        String where = C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_f3i
    public void saveCountf3i(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_F3I, count.count_f3i);
        String where = C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_pi
    public void saveCountpi(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_PI, count.count_pi);
        String where = C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_li
    public void saveCountli(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_LI, count.count_li);
        String where = C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_ei
    public void saveCountei(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_EI, count.count_ei);
        String where = C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_f1e
    public void saveCountf1e(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_F1E, count.count_f1e);
        String where = C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_f2e
    public void saveCountf2e(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_F2E, count.count_f2e);
        String where = C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_f3e
    public void saveCountf3e(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_F3E, count.count_f3e);
        String where = C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_pe
    public void saveCountpe(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_PE, count.count_pe);
        String where = C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_le
    public void saveCountle(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_LE, count.count_le);
        String where = C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_ee
    public void saveCountee(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_EE, count.count_ee);
        String where = C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // Used by EditSectionActivity
    public void updateCountName(int id, String name, String code, String name_g)
    {
        if (database.isOpen())
        {
            ContentValues dataToInsert = new ContentValues();
            dataToInsert.put(C_NAME, name);
            dataToInsert.put(C_CODE, code);
            dataToInsert.put(C_NAME_G, name_g);
            String where = C_ID + " = ?";
            String[] whereArgs = {String.valueOf(id)};
            database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
        }
    }

    // Used by EditSectionActivity and CountingActivity
    public List<Count> getAllCountsForSection(int section_id)
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            C_SECTION_ID + " = " + section_id, null, null, null, null);

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

    // Used by EditSectionActivity
    public List<Count> getAllSpeciesForSectionSrtCode(int section_id)
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + " ("
            + C_SECTION_ID + " = " + section_id
            + ") order by " + C_CODE, null);

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

    // Used by EditSectionActivity
    public List<Count> getAllSpeciesForSectionSrtName(int section_id)
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + " ("
            + C_SECTION_ID + " = " + section_id
            + ") order by " + C_NAME, null);

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
    public List<Count> getAllCountsForSrtName()
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + " ("
            + C_COUNT_F1I + " > 0 or " + C_COUNT_F2I + " > 0 or "
            + C_COUNT_F3I + " > 0 or " + C_COUNT_PI + " > 0 or "
            + C_COUNT_LI + " > 0 or " + C_COUNT_EI + " > 0 or "
            + C_COUNT_F1E + " > 0 or " + C_COUNT_F2E + " > 0 or "
            + C_COUNT_F3E + " > 0 or " + C_COUNT_PE + " > 0 or "
            + C_COUNT_LE + " > 0 or " + C_COUNT_EE + " > 0)"
            + " order by " + C_NAME + ", " + C_SECTION_ID, null);

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

    // Prepared for future use by ListSpeciesActivity
    public List<Count> getAllCountsForSrtNameG()
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + " ("
            + C_COUNT_F1I + " > 0 or " + C_COUNT_F2I + " > 0 or "
            + C_COUNT_F3I + " > 0 or " + C_COUNT_PI + " > 0 or "
            + C_COUNT_LI + " > 0 or " + C_COUNT_EI + " > 0 or "
            + C_COUNT_F1E + " > 0 or " + C_COUNT_F2E + " > 0 or "
            + C_COUNT_F3E + " > 0 or " + C_COUNT_PE + " > 0 or "
            + C_COUNT_LE + " > 0 or " + C_COUNT_EE + " > 0)"
            + " order by " + C_NAME_G + ", " + C_SECTION_ID, null);

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
    public List<Count> getAllCountsForSrtCode()
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + " ("
            + C_COUNT_F1I + " > 0 or " + C_COUNT_F2I + " > 0 or "
            + C_COUNT_F3I + " > 0 or " + C_COUNT_PI + " > 0 or "
            + C_COUNT_LI + " > 0 or " + C_COUNT_EI + " > 0 or "
            + C_COUNT_F1E + " > 0 or " + C_COUNT_F2E + " > 0 or "
            + C_COUNT_F3E + " > 0 or " + C_COUNT_PE + " > 0 or "
            + C_COUNT_LE + " > 0 or " + C_COUNT_EE + " > 0)"
            + " order by " + C_CODE + ", " + C_SECTION_ID, null);

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

    // Used by ListSpeciesActivity and WelcomeActivity
    public List<Count> getAllCounts()
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + " ("
            + C_COUNT_F1I + " > 0 or " + C_COUNT_F2I + " > 0 or "
            + C_COUNT_F3I + " > 0 or " + C_COUNT_PI + " > 0 or "
            + C_COUNT_LI + " > 0 or " + C_COUNT_EI + " > 0 or "
            + C_COUNT_F1E + " > 0 or " + C_COUNT_F2E + " > 0 or "
            + C_COUNT_F3E + " > 0 or " + C_COUNT_PE + " > 0 or "
            + C_COUNT_LE + " > 0 or " + C_COUNT_EE + " > 0)"
            + " order by " + C_SECTION_ID, null);

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
    public int getDiffSpec()
    {
        int cntSpec = 0;
        Cursor cursor = database.rawQuery("select DISTINCT " + C_CODE + " from " + COUNT_TABLE
            + " WHERE " + " ("
            + C_COUNT_F1I + " > 0 or " + C_COUNT_F2I + " > 0 or "
            + C_COUNT_F3I + " > 0 or " + C_COUNT_PI + " > 0 or "
            + C_COUNT_LI + " > 0 or " + C_COUNT_EI + " > 0 or "
            + C_COUNT_F1E + " > 0 or " + C_COUNT_F2E + " > 0 or "
            + C_COUNT_F3E + " > 0 or " + C_COUNT_PE + " > 0 or "
            + C_COUNT_LE + " > 0 or " + C_COUNT_EE + " > 0)", null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            cntSpec++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return cntSpec;
    }

    // Used by CountingActivity
    public String[] getAllIdsForSection(int section_id)
    {
        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            C_SECTION_ID + " = " + section_id, null, null, null, null);

        String[] idArray = new String[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            int uid = cursor.getInt(0);
            idArray[i] = Integer.toString(uid);
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return idArray;
    }

    // Used by CountingActivity
    public String[] getAllIdsForSectionSrtName(int section_id)
    {
        Cursor cursor = database.rawQuery("select ROWID from " + COUNT_TABLE
            + " WHERE " + C_SECTION_ID + " = " + section_id + " order by "
            + C_NAME, null);

        String[] idArray = new String[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            int uid = cursor.getInt(0);
            idArray[i] = Integer.toString(uid);
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return idArray;
    }

    // Prepared for future use by ListSpeciesActivity
    public String[] getAllIdsForSectionSrtNameG(int section_id)
    {
        Cursor cursor = database.rawQuery("select ROWID from " + COUNT_TABLE
            + " WHERE " + C_SECTION_ID + " = " + section_id + " order by "
            + C_NAME_G, null);

        String[] idArray = new String[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            int uid = cursor.getInt(0);
            idArray[i] = Integer.toString(uid);
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return idArray;
    }

    // Used by CountingActivity
    public String[] getAllIdsForSectionSrtCode(int section_id)
    {
        Cursor cursor = database.rawQuery("select ROWID from " + COUNT_TABLE
            + " WHERE " + C_SECTION_ID + " = " + section_id + " order by "
            + C_CODE, null);

        String[] idArray = new String[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            int uid = cursor.getInt(0);
            idArray[i] = Integer.toString(uid);
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return idArray;
    }

    // Used by CountingActivity
    public String[] getAllStringsForSection(int section_id, String sname)
    {
        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            C_SECTION_ID + " = " + section_id, null, null, null, null);

        String[] uArray = new String[cursor.getCount()];
        int i = 0;
        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            @SuppressLint("Range") String uname = cursor.getString(cursor.getColumnIndex(sname));
            uArray[i] = uname;
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return uArray;
    }

    // Used by CountingActivity
    public String[] getAllStringsForSectionSrtName(int section_id, String sname)
    {

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + C_SECTION_ID + " = " + section_id + " order by "
            + C_NAME, null);

        String[] uArray = new String[cursor.getCount()];

        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            @SuppressLint("Range") String uname = cursor.getString(cursor.getColumnIndex(sname));
            uArray[i] = uname;
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return uArray;
    }

    // Prepared for future use by ListSpeciesActivity
    public String[] getAllStringsForSectionSrtNameG(int section_id, String sname)
    {

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + C_SECTION_ID + " = " + section_id + " order by "
            + C_NAME_G, null);

        String[] uArray = new String[cursor.getCount()];

        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            @SuppressLint("Range") String uname = cursor.getString(cursor.getColumnIndex(sname));
            uArray[i] = uname;
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return uArray;
    }

    // Used by CountingActivity
    public String[] getAllStringsForSectionSrtCode(int section_id, String sname)
    {

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + C_SECTION_ID + " = " + section_id + " order by "
            + C_CODE, null);

        String[] uArray = new String[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            @SuppressLint("Range") String uname = cursor.getString(cursor.getColumnIndex(sname));
            uArray[i] = uname;
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return uArray;
    }

    // Used by CountingActivity
    public Integer[] getAllImagesForSection(int section_id)
    {
        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            C_SECTION_ID + " = " + section_id, null, null, null, null);

        Integer[] imageArray = new Integer[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            @SuppressLint("Range") String ucode = cursor.getString(cursor.getColumnIndex("code"));

            String rname = "p" + ucode; // species picture resource name
            int resId = transektCountApp.getResID(rname);
            int resId0 = transektCountApp.getResID("p00000");
            
            if (resId != 0)
            {
                imageArray[i] = resId;
            }
            else
            {
                imageArray[i] = resId0;
            }
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return imageArray;
    }

    // Used by CountingActivity
    public Integer[] getAllImagesForSectionSrtName(int section_id)
    {
        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + C_SECTION_ID + " = " + section_id + " order by "
            + C_NAME, null);

        Integer[] imageArray = new Integer[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            @SuppressLint("Range") String ucode = cursor.getString(cursor.getColumnIndex("code"));

            String rname = "p" + ucode; // species picture resource name
            int resId = transektCountApp.getResID(rname);
            int resId0 = transektCountApp.getResID("p00000");

            if (resId != 0)
            {
                imageArray[i] = resId;
            }
            else
            {
                imageArray[i] = resId0;
            }
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return imageArray;
    }

    // Prepared for future use by ListSpeciesActivity
    public Integer[] getAllImagesForSectionSrtNameG(int section_id)
    {
        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + C_SECTION_ID + " = " + section_id + " order by "
            + C_NAME_G, null);

        Integer[] imageArray = new Integer[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            @SuppressLint("Range") String ucode = cursor.getString(cursor.getColumnIndex("code"));

            String rname = "p" + ucode; // species picture resource name
            int resId = transektCountApp.getResID(rname);
            int resId0 = transektCountApp.getResID("p00000");

            if (resId != 0)
            {
                imageArray[i] = resId;
            }
            else
            {
                imageArray[i] = resId0;
            }
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return imageArray;
    }

    // Used by CountingActivity
    public Integer[] getAllImagesForSectionSrtCode(int section_id)
    {
        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + C_SECTION_ID + " = " + section_id + " order by "
            + C_CODE, null);

        Integer[] imageArray = new Integer[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            @SuppressLint("Range") String ucode = cursor.getString(cursor.getColumnIndex("code"));

            String rname = "p" + ucode; // species picture resource name
            int resId = transektCountApp.getResID(rname);
            int resId0 = transektCountApp.getResID("p00000");

            if (resId != 0)
            {
                imageArray[i] = resId;
            }
            else
            {
                imageArray[i] = resId0;
            }
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return imageArray;
    }

    // Used by CountingActivity and CountOptionsActivity
    public Count getCountById(int count_id)
    {
        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            C_ID + " = " + count_id, null, null, null, null);

        cursor.moveToFirst();
        Count count = cursorToCount(cursor);
        cursor.close();
        return count;
    }

}
