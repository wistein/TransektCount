package com.wmstein.transektcount.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/***********************************
 * Created by wmstein on 2016-03-31,
 * last edited on 2022-04-26.
 */
public class MetaDataSource
{
    // Database fields
    private SQLiteDatabase database;
    private final DbHelper dbHandler;
    private final String[] allColumns = {
        DbHelper.M_ID,
        DbHelper.M_TEMPE,
        DbHelper.M_WIND,
        DbHelper.M_CLOUDS,
        DbHelper.M_DATE,
        DbHelper.M_START_TM,
        DbHelper.M_END_TM
    };

    public MetaDataSource(Context context)
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

    public void saveMeta(Meta meta)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.M_ID, meta.id);
        dataToInsert.put(DbHelper.M_TEMPE, meta.tempe);
        dataToInsert.put(DbHelper.M_WIND, meta.wind);
        dataToInsert.put(DbHelper.M_CLOUDS, meta.clouds);
        dataToInsert.put(DbHelper.M_DATE, meta.date);
        dataToInsert.put(DbHelper.M_START_TM, meta.start_tm);
        dataToInsert.put(DbHelper.M_END_TM, meta.end_tm);
        database.update(DbHelper.META_TABLE, dataToInsert, null, null);
    }

    @SuppressLint("Range")
    private Meta cursorToMeta(Cursor cursor)
    {
        Meta meta = new Meta();
        meta.id = cursor.getInt(cursor.getColumnIndex(DbHelper.M_ID));
        meta.tempe = cursor.getInt(cursor.getColumnIndex(DbHelper.M_TEMPE));
        meta.wind = cursor.getInt(cursor.getColumnIndex(DbHelper.M_WIND));
        meta.clouds = cursor.getInt(cursor.getColumnIndex(DbHelper.M_CLOUDS));
        meta.date = cursor.getString(cursor.getColumnIndex(DbHelper.M_DATE));
        meta.start_tm = cursor.getString(cursor.getColumnIndex(DbHelper.M_START_TM));
        meta.end_tm = cursor.getString(cursor.getColumnIndex(DbHelper.M_END_TM));
        return meta;
    }

    public Meta getMeta()
    {
        Meta meta;
        Cursor cursor = database.query(DbHelper.META_TABLE, allColumns, String.valueOf(1), 
            null, null, null, null);
        cursor.moveToFirst();
        meta = cursorToMeta(cursor);
        // Make sure to close the cursor
        cursor.close();
        return meta;
    }

}
