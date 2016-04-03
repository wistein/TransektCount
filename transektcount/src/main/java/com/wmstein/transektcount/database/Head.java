package com.wmstein.transektcount.database;

/*
 * Created by wmstein on 31.03.2016.
 */
public class Head
{
    public int id;
    public String transect_no;
    public String inspector_name;

    //setting H_ID *******************
    public void setH_ID (int H_id)
    {
        this.id = H_id;
    }

    //setting H_TRANSECT_NO
    public void setH_TRANSECT_NO(String H_transect_no)
    {
        this.transect_no = H_transect_no;
    }

    //setting H_INSPECTOR_NAME
    public void setH_INSPECTOR_NAME(String H_inspector_name)
    {
        this.inspector_name = H_inspector_name;
    }
    
}
