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

import com.wmstein.transektcount.AutoFitText;
import com.wmstein.transektcount.CountingActivity;
import com.wmstein.transektcount.R;
import com.wmstein.transektcount.database.Count;

/**
 * Created by milo on 25/05/2014.
 * Changed by wmstein on 06.09.2016
 */
public class CountingWidgetLH extends RelativeLayout
{
    public static String TAG = "transektcountCountingWidgetLH";

    private TextView countName;
    private AutoFitText countCount;
    private AutoFitText countCounta;

    public Count count;

    public CountingWidgetLH(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_counting_lh, this, true);
        countCount = (AutoFitText) findViewById(R.id.countCountLH);
        countCounta = (AutoFitText) findViewById(R.id.countCountaLH);
        countName = (TextView) findViewById(R.id.countNameLH);
    }

    public void setCount(Count newcount)
    {
        count = newcount;
        countCount.setText(String.valueOf(count.count));
        countCounta.setText(String.valueOf(count.counta));
        countName.setText(count.name);
        ImageButton countUpButton = (ImageButton) findViewById(R.id.buttonUpLH);
        countUpButton.setTag(count.id);
        ImageButton countUpButtona = (ImageButton) findViewById(R.id.buttonUpaLH);
        countUpButtona.setTag(count.id);
        ImageButton countDownButton = (ImageButton) findViewById(R.id.buttonDownLH);
        countDownButton.setTag(count.id);
        ImageButton countDownButtona = (ImageButton) findViewById(R.id.buttonDownaLH);
        countDownButtona.setTag(count.id);
        ImageButton editButton = (ImageButton) findViewById(R.id.buttonEditLH);
        editButton.setTag(count.id);
    }

    public void countUpLH()
    {
        count.increase();
        countCount.setText(String.valueOf(count.count));
    }

    public void countDownLH()
    {
        count.safe_decrease();
        countCount.setText(String.valueOf(count.count));
    }

    public void countUpaLH()
    {
        count.increasea();
        countCounta.setText(String.valueOf(count.counta));
    }

    public void countDownaLH()
    {
        count.safe_decreasea();
        countCounta.setText(String.valueOf(count.counta));
    }

}
