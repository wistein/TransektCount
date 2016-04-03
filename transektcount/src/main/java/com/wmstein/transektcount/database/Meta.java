package com.wmstein.transektcount.database;

/*
 * Created by wmstein on 31.03.2016.
 */
public class Meta
{
    public int id;
    public int temp;
    public int wind;
    public int clouds;
    public String start_tm;
    public String end_tm;

    //setting M_ID *******************
    public void setM_ID (int M_id)
    {
        this.id = M_id;
    }
    
    //setting M_TEMP
    public void setM_TEMP(int M_temp)
    {
        this.temp = M_temp;
    }
    
    //setting M_WIND
    public void setM_WIND(int M_wind)
    {
        this.wind = M_wind;
    }

    //setting M_CLOUDS
    public void setM_CLOUDS(int M_clouds)
    {
        this.clouds = M_clouds;
    }

    //setting M_START_TM
    public void setM_START_TM(String M_start_tm)
    {
        this.start_tm = M_start_tm;
    }

    //setting M_END_TM
    public void setM_END_TM(String M_end_tm)
    {
        this.end_tm = M_end_tm;
    }

}
