package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;

import java.util.Objects;

/*****************************************************
 * EditMetaWidget.java used by EditMetaActivity.java
 * Created by wmstein for TransektCount on 2016-04-02
 * last edited on 2023-05-09
 */
public class EditMetaWidget extends LinearLayout
{
    final TextView widget_temp1; // temperature
    final EditText widget_temp2;
    final TextView widget_wind1; // wind
    final EditText widget_wind2;
    final TextView widget_clouds1; // clouds
    final EditText widget_clouds2;
    final TextView widget_date1; // date
    final TextView widget_date2;
    final TextView widget_stime1; // start-time
    final TextView widget_stime2;
    final TextView widget_etime1; // end-time
    final TextView widget_etime2;

    final String regEx = "^[0-9]*$"; // plausi for numeric input

    public EditMetaWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_meta, this, true);
        widget_temp1 = findViewById(R.id.widgetTemp1);
        widget_temp2 = findViewById(R.id.widgetTemp2);
        widget_wind1 = findViewById(R.id.widgetWind1);
        widget_wind2 = findViewById(R.id.widgetWind2);
        widget_clouds1 = findViewById(R.id.widgetClouds1);
        widget_clouds2 = findViewById(R.id.widgetClouds2);
        widget_date1 = findViewById(R.id.widgetDate1);
        widget_date2 = findViewById(R.id.widgetDate2);
        widget_stime1 = findViewById(R.id.widgetSTime1);
        widget_stime2 = findViewById(R.id.widgetSTime2);
        widget_etime1 = findViewById(R.id.widgetETime1);
        widget_etime2 = findViewById(R.id.widgetETime2);
    }

    // Following the SETS
    // temperature
    public void setWidgetTemp1(String title)
    {
        widget_temp1.setText(title);
    }

    public void setWidgetTemp2(int name)
    {
        widget_temp2.setText(String.valueOf(name));
    }

    // wind
    public void setWidgetWind1(String title)
    {
        widget_wind1.setText(title);
    }

    public void setWidgetWind2(int name)
    {
        widget_wind2.setText(String.valueOf(name));
    }

    // clouds
    public void setWidgetClouds1(String title)
    {
        widget_clouds1.setText(title);
    }

    public void setWidgetClouds2(int name)
    {
        widget_clouds2.setText(String.valueOf(name));
    }

    // date
    public void setWidgetDate1(String title)
    {
        widget_date1.setText(title);
    }

    public void setWidgetDate2(String name)
    {
        widget_date2.setText(name);
    }

    // start_tm
    public void setWidgetSTime1(String title)
    {
        widget_stime1.setText(title);
    }

    public void setWidgetSTime2(String name)
    {
        widget_stime2.setText(name);
    }

    // end_tm
    public void setWidgetETime1(String title)
    {
        widget_etime1.setText(title);
    }

    public void setWidgetETime2(String name)
    {
        widget_etime2.setText(name);
    }

    // following the GETS
    // get temperature with plausi
    public int getWidgetTemp()
    {
        String text = widget_temp2.getText().toString();
        if (isEmpty(text))
            return 0;
        else if (!text.trim().matches(regEx))
            return 100;
        else
            try
            {
                return Integer.parseInt(text.replaceAll("\\D",""));
            } catch (NumberFormatException e)
            {
                return 100;
            }
    }

    // get wind with plausi
    public int getWidgetWind()
    {
        String text = widget_wind2.getText().toString();
        if (isEmpty(text))
            return 0;
        else if (!text.trim().matches(regEx))
            return 100;
        else
            try
            {
                return Integer.parseInt(text.replaceAll("\\D",""));
            } catch (NumberFormatException e)
            {
                return 100;
            }
    }

    // get clouds with plausi
    public int getWidgetClouds()
    {
        String text = widget_clouds2.getText().toString();
        if (isEmpty(text))
            return 0;
        else if (!text.trim().matches(regEx))
            return 200;
        else
            try
            {
                return Integer.parseInt(text.replaceAll("\\D",""));
            } catch (NumberFormatException e)
            {
                return 200;
            }
    }

    // get get date
    public String getWidgetDate()
    {
        return widget_date2.getText().toString();
    }

    // get start time
    public String getWidgetSTime()
    {
        return widget_stime2.getText().toString();
    }

    // get stop time
    public String getWidgetETime()
    {
        return widget_etime2.getText().toString();
    }

    // set the hint
    public void setHint(String hint)
    {
        widget_temp1.setHint(hint);
    }

    /**
     * Checks if a CharSequence is empty ("") or null.
     * <p>
     * isEmpty(null)      = true
     * isEmpty("")        = true
     * isEmpty(" ")       = false
     * isEmpty("bob")     = false
     * isEmpty("  bob  ") = false
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(final CharSequence cs)
    {
        return cs == null || cs.length() == 0;
    }

}
