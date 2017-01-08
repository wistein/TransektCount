/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;
import com.wmstein.transektcount.database.Count;

/****************************************************
 * Interface for widget_counting_head1.xml
 * Created by wmstein 18.12.2016
 */
public class CountingWidget_notes extends RelativeLayout
{
    public static String TAG = "transektcountCountingWidget_notes";

    private TextView countNotes;

    public Count count;

    public CountingWidget_notes(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_counting_notes, this, true);
        countNotes = (TextView) findViewById(R.id.countNotes);
    }


    public void setCountNotes(Count newcount)
    {
        count = newcount;

        countNotes.setText(count.notes);
    }

}
