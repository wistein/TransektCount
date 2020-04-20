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

import java.util.Objects;

/****************************************************
 * Interface for widget_counting_head3.xml
 * Created by wmstein 18.12.2016
 * Last edited on 2020-04-17
 */
public class CountingWidget_head3 extends RelativeLayout
{
    private TextView countHead3;

    public CountingWidget_head3(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_head3, this, true);
        countHead3 = findViewById(R.id.countHead3);
    }

    public void setCountHead3()
    {
        countHead3.setText(getContext().getString(R.string.countExternalHint));
    }

}
