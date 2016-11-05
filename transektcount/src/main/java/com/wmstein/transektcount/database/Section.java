package com.wmstein.transektcount.database;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/****************************************************
 * Based on Project.java by milo on 05/05/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016
 */
public class Section
{
    public int id;
    public long created_at;
    public String name;
    public String notes;

    //Get Date from DB table sections field created_at
    public String getDate()
    {
        Date date = new Date(created_at);
        DateFormat df = SimpleDateFormat.getDateInstance();
        return df.format(date);
    }

    public Long DatNum()
    {
        long datnum = created_at;
        return datnum;
    }

    //setting S_SECTION_ID *******************
    public void setS_ID(int S_sectid)
    {
        this.id = S_sectid;
    }

    //setting S_CREATED_AT
    public void setS_CREATED_AT(long S_Created)
    {
        this.created_at = S_Created;
    }

    //setting S_NAME
    public void setS_NAME(String S_name)
    {
        this.name = S_name;
    }

    //setting S_NOTES
    public void setS_NOTES(String S_notes)
    {
        this.notes = S_notes;
    }

}
