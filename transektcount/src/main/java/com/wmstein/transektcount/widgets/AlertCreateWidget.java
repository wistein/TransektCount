package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.wmstein.transektcount.R;

import java.io.Serializable;

/**
 * This is the widget for creating an alert in the CountOptionsActivity.
 * Created by milo on 02/06/2014.
 * Modified by wmstein on 18.02.2016
 */
public class AlertCreateWidget extends LinearLayout implements Serializable
{
    EditText alert_name;
    EditText alert_value;
    int alert_id;
    ImageButton deleteButton;

    public AlertCreateWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_alert_create, this, true);
        alert_name = (EditText) findViewById(R.id.alert_name);
        alert_value = (EditText) findViewById(R.id.alert_value);
        alert_id = 0;
        deleteButton = (ImageButton) findViewById(R.id.delete_button);
        deleteButton.setTag(Integer.valueOf(0));
    }

    public String getAlertName()
    {
        return alert_name.getText().toString();
    }

    // this is set to return 0 if it can't parse a value from the box in order
    // that transektcount doesn't crash
    public int getAlertValue()
    {
        String text = alert_value.getText().toString();
        if (isEmpty(text))
        {
            return Integer.valueOf(0);
        }
        else
        {
            return Integer.parseInt(text);
        }
    }

    public int getAlertId()
    {
        return alert_id;
    }

    public void setAlertName(String name)
    {
        alert_name.setText(name);
    }

    public void setAlertValue(int value)
    {
        alert_value.setText(String.valueOf(value));
    }

    public void setAlertId(int id)
    {
        alert_id = id;
        deleteButton.setTag(Integer.valueOf(id));
    }

    /**
     * Checks if a CharSequence is empty ("") or null.
     *
     * isEmpty(null)      = true
     * isEmpty("")        = true
     * isEmpty(" ")       = false
     * isEmpty("bob")     = false
     * isEmpty("  bob  ") = false
     *
     * @param cs  the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

}
