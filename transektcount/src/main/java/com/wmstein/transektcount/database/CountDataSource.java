package com.wmstein.transektcount.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.wmstein.transektcount.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.wmstein.transektcount.database.DbHelper.COUNT_TABLE;

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
        DbHelper.C_NOTES
    };

//    public List<Count> count_list;

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
        values.put(DbHelper.C_NAME, name);
        values.put(DbHelper.C_CODE, code);
        values.put(DbHelper.C_COUNT_F1I, 0);
        values.put(DbHelper.C_COUNT_F2I, 0);
        values.put(DbHelper.C_COUNT_F3I, 0);
        values.put(DbHelper.C_COUNT_PI, 0);
        values.put(DbHelper.C_COUNT_LI, 0);
        values.put(DbHelper.C_COUNT_EI, 0);
        values.put(DbHelper.C_COUNT_F1E, 0);
        values.put(DbHelper.C_COUNT_F2E, 0);
        values.put(DbHelper.C_COUNT_F3E, 0);
        values.put(DbHelper.C_COUNT_PE, 0);
        values.put(DbHelper.C_COUNT_LE, 0);
        values.put(DbHelper.C_COUNT_EE, 0);
        // notes should be default null and so is not created here

        int insertId = (int) database.insert(COUNT_TABLE, null, values);
        Cursor cursor = database.query(COUNT_TABLE,
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
        newcount.name = cursor.getString(cursor.getColumnIndex(DbHelper.C_NAME));
        newcount.code = cursor.getString(cursor.getColumnIndex(DbHelper.C_CODE));
        newcount.count_f1i = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_F1I));
        newcount.count_f2i = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_F2I));
        newcount.count_f3i = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_F3I));
        newcount.count_pi = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_PI));
        newcount.count_li = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_LI));
        newcount.count_ei = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_EI));
        newcount.count_f1e = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_F1E));
        newcount.count_f2e = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_F2E));
        newcount.count_f3e = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_F3E));
        newcount.count_pe = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_PE));
        newcount.count_le = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_LE));
        newcount.count_ee = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_EE));
        newcount.notes = cursor.getString(cursor.getColumnIndex(DbHelper.C_NOTES));
        return newcount;
    }

    // Used by EditSectionActivity
    public void deleteCountById(int id)
    {
        System.out.println("Gelöscht: Zähler mit ID: " + id);
        database.delete(COUNT_TABLE, DbHelper.C_ID + " = " + id, null);

        // delete associated alerts
        database.delete(DbHelper.ALERT_TABLE, DbHelper.A_COUNT_ID + " = " + id, null);
    }

    // Used by CountOptionsActivity and CountingActivity
    public void saveCount(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_NAME, count.name);
        dataToInsert.put(DbHelper.C_CODE, count.code);
        dataToInsert.put(DbHelper.C_COUNT_F1I, count.count_f1i);
        dataToInsert.put(DbHelper.C_COUNT_F2I, count.count_f2i);
        dataToInsert.put(DbHelper.C_COUNT_F3I, count.count_f3i);
        dataToInsert.put(DbHelper.C_COUNT_PI, count.count_pi);
        dataToInsert.put(DbHelper.C_COUNT_LI, count.count_li);
        dataToInsert.put(DbHelper.C_COUNT_EI, count.count_ei);
        dataToInsert.put(DbHelper.C_COUNT_F1E, count.count_f1e);
        dataToInsert.put(DbHelper.C_COUNT_F2E, count.count_f2e);
        dataToInsert.put(DbHelper.C_COUNT_F3E, count.count_f3e);
        dataToInsert.put(DbHelper.C_COUNT_PE, count.count_pe);
        dataToInsert.put(DbHelper.C_COUNT_LE, count.count_le);
        dataToInsert.put(DbHelper.C_COUNT_EE, count.count_ee);
        dataToInsert.put(DbHelper.C_NOTES, count.notes);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_f1i
    public void saveCountf1i(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_COUNT_F1I, count.count_f1i);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_f2i
    public void saveCountf2i(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_COUNT_F2I, count.count_f2i);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_f3i
    public void saveCountf3i(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_COUNT_F3I, count.count_f3i);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_pi
    public void saveCountpi(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_COUNT_PI, count.count_pi);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_li
    public void saveCountli(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_COUNT_LI, count.count_li);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_ei
    public void saveCountei(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_COUNT_EI, count.count_ei);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_f1e
    public void saveCountf1e(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_COUNT_F1E, count.count_f1e);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_f2e
    public void saveCountf2e(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_COUNT_F2E, count.count_f2e);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_f3e
    public void saveCountf3e(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_COUNT_F3E, count.count_f3e);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_pe
    public void saveCountpe(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_COUNT_PE, count.count_pe);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_le
    public void saveCountle(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_COUNT_LE, count.count_le);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_ee
    public void saveCountee(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_COUNT_EE, count.count_ee);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // Used by EditSectionActivity
    public void updateCountName(int id, String name, String code)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_NAME, name);
        dataToInsert.put(DbHelper.C_CODE, code);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // Used by EditSectionActivity and CountingActivity
    public List<Count> getAllCountsForSection(int section_id)
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.query(COUNT_TABLE, allColumns,
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

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
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

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
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

    // Used by CountingActivity
    public String[] getAllIdsForSection(int section_id)
    {
        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            DbHelper.C_SECTION_ID + " = " + section_id, null, null, null, null);

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
            + " WHERE " + DbHelper.C_SECTION_ID + " = " + section_id + " order by "
            + DbHelper.C_NAME, null);

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
            + " WHERE " + DbHelper.C_SECTION_ID + " = " + section_id + " order by "
            + DbHelper.C_CODE, null);

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
            DbHelper.C_SECTION_ID + " = " + section_id, null, null, null, null);

        String[] uArray = new String[cursor.getCount()];
        int i = 0;
        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            String uname = cursor.getString(cursor.getColumnIndex(sname));
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
            + " WHERE " + DbHelper.C_SECTION_ID + " = " + section_id + " order by "
            + DbHelper.C_NAME, null);

        String[] uArray = new String[cursor.getCount()];

        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            String uname = cursor.getString(cursor.getColumnIndex(sname));
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
            + " WHERE " + DbHelper.C_SECTION_ID + " = " + section_id + " order by "
            + DbHelper.C_CODE, null);

        String[] uArray = new String[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            String uname = cursor.getString(cursor.getColumnIndex(sname));
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
            DbHelper.C_SECTION_ID + " = " + section_id, null, null, null, null);

        Integer[] imageArray = new Integer[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            String ucode = cursor.getString(cursor.getColumnIndex("code"));

            String rname = "p" + ucode; // species picture resource name
            int resId = getResId(rname);
            imageArray[i] = resId;
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
            + " WHERE " + DbHelper.C_SECTION_ID + " = " + section_id + " order by "
            + DbHelper.C_NAME, null);

        Integer[] imageArray = new Integer[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            String ucode = cursor.getString(cursor.getColumnIndex("code"));

            String rname = "p" + ucode; // species picture resource name
            int resId = getResId(rname);
            imageArray[i] = resId;
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
            + " WHERE " + DbHelper.C_SECTION_ID + " = " + section_id + " order by "
            + DbHelper.C_CODE, null);

        Integer[] imageArray = new Integer[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            String ucode = cursor.getString(cursor.getColumnIndex("code"));

            String rname = "p" + ucode; // species picture resource name
            int resId = getResId(rname);
            imageArray[i] = resId;
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

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + DbHelper.C_SECTION_ID + " = " + section_id + " and ("
            + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
            + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
            + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
            + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
            + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
            + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
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

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + DbHelper.C_SECTION_ID + " = " + section_id + " and ("
            + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
            + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
            + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
            + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
            + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
            + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
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

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + DbHelper.C_SECTION_ID + " = " + section_id + " and ("
            + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
            + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
            + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0 or "
            + DbHelper.C_COUNT_F1E + " > 0 or " + DbHelper.C_COUNT_F2E + " > 0 or "
            + DbHelper.C_COUNT_F3E + " > 0 or " + DbHelper.C_COUNT_PE + " > 0 or "
            + DbHelper.C_COUNT_LE + " > 0 or " + DbHelper.C_COUNT_EE + " > 0)"
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

    // Get resource ID from resource name
    public int getResId(String rName)
    {
        try
        {
            Class res = R.drawable.class;
            Field idField = res.getField(rName);
            return idField.getInt(null);
        } catch (Exception e)
        {
            return 0;
        }
    }

}
