package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.wmstein.transektcount.R;

import java.util.Objects;

/****************************************************
 * Created by milo on 01/06/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016
 * Last edited on 2019-02-12
 */
public class AddAlertWidget extends LinearLayout
{

    public AddAlertWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_add_alert, this, true);
    }

}
