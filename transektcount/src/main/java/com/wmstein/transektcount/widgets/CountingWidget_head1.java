/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wmstein.transektcount.R;
import com.wmstein.transektcount.database.Count;

import java.lang.reflect.Field;

/****************************************************
 * Interface for widget_counting_head1.xml
 * Created by wmstein 18.12.2016
 */
public class CountingWidget_head1 extends ArrayAdapter<String>
{
    public static String TAG = "transektcountCountingWidget_head1";

    private Context context;
    private String[] idArray;
    private String[] contentArray1;
    private String[] contentArray2;
    private Integer[] imageArray;

    private TextView countId;
    private TextView countName;
    private TextView countCode;
    private ImageView pSpecies;

    public Count count;
    LayoutInflater inflater;

    public CountingWidget_head1(Context context, int resource, String[] idArray, String[] nameArray, String[] codeArray, Integer[] imageArray)
    {
        super(context, R.layout.widget_counting_head1, R.id.countName, nameArray);
        this.context = context;
        this.idArray = idArray;
        this.contentArray1 = nameArray;
        this.contentArray2 = codeArray;
        this.imageArray = imageArray;

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

/*        inflater.inflate(R.layout.widget_counting_head1, this, true);
        countName = (TextView) findViewById(R.id.countName);
        countCode = (TextView) findViewById(R.id.countCode);
        pSpecies = (ImageView) findViewById(R.id.pSpecies);
*/
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent)
    {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent)
    {
        View row = inflater.inflate(R.layout.widget_counting_head1, parent, false);

        countId = (TextView) row.findViewById(R.id.countId);
        countId.setText(idArray[position]);

        countName = (TextView) row.findViewById(R.id.countName);
        countName.setText(contentArray1[position]);

        countCode = (TextView) row.findViewById(R.id.countCode);
        countCode.setText(contentArray2[position]);

        pSpecies = (ImageView) row.findViewById(R.id.pSpecies);
        pSpecies.setImageResource(imageArray[position]);

        return row;
    }


    public void setCountHead1(Count newcount)
    {
        count = newcount;

        countName.setText(count.name);
        countCode.setText(count.code);

        String rname = "p" + count.code; // species picture resource name

        int resId = getResId(rname);
        //Log.i(TAG, "rname = " + rname);
        //Log.i(TAG, "resId = " + String.valueOf(resId));
        if (resId != 0)
        {
            pSpecies.setImageResource(resId);
        }

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
