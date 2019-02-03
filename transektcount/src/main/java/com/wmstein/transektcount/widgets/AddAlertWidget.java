package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;

/****************************************************
 * Created by milo on 01/06/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016
 * Last edited on 2019-02-02
 */
public class AddAlertWidget extends LinearLayout
{
    private TextView textView;

    public AddAlertWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_add_alert, this, true);
        textView = findViewById(R.id.add_alert_text);
    }

}
