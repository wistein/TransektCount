/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;
import com.wmstein.transektcount.database.Count;

/****************************************************
 * Interface for widget_counting_head1.xml
 * Created by wmstein 18.12.2016
 */
public class CountingWidget_head2 extends RelativeLayout
{
    public static String TAG = "transektcountCountingWidget_head1";

    private TextView countHead2;

    public Count count;

    public CountingWidget_head2(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_counting_head2, this, true);
        countHead2 = (TextView) findViewById(R.id.countHead2);
    }


    public void setCountHead2(Count count)
    {
        // set TextView countHead2
        countHead2.setText(getContext().getString(R.string.countInternalHint));
        // set ImageButton Edit
        ImageButton editButton = (ImageButton) findViewById(R.id.buttonEdit);
        editButton.setTag(count.id);
    }

}
