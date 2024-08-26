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
 * last edited on 2024-05-28
 */
public class EditMetaWidget extends LinearLayout
{
    final TextView widget_temp1;        // start temperature
    final EditText widget_starttemp2;
    final EditText widget_endtemp2;     // end temperature

    final TextView widget_wind1;        // start wind
    final EditText widget_startwind2;
    final EditText widget_endwind2;     // end wind

    final TextView widget_clouds1;      // start clouds
    final EditText widget_startclouds2;
    final EditText widget_endclouds2;   // end clouds

    final TextView widget_date1;        // date
    final TextView widget_date2;
    final TextView widget_stime1;       // start-time
    final TextView widget_stime2;
    final TextView widget_etime1;       // end-time
    final TextView widget_etime2;
    final TextView widget_note1;
    final TextView widget_note2;

    final String regEx = "^[0-9]*$"; // plausi for numeric input

    public EditMetaWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_meta, this, true);
        widget_temp1 = findViewById(R.id.widgetTemp1);
        widget_starttemp2 = findViewById(R.id.widgetStartTemp2);
        widget_endtemp2 = findViewById(R.id.widgetEndTemp2);
        widget_wind1 = findViewById(R.id.widgetWind1);
        widget_startwind2 = findViewById(R.id.widgetStartWind2);
        widget_endwind2 = findViewById(R.id.widgetEndWind2);
        widget_clouds1 = findViewById(R.id.widgetClouds1);
        widget_startclouds2 = findViewById(R.id.widgetStartClouds2);
        widget_endclouds2 = findViewById(R.id.widgetEndClouds2);
        widget_date1 = findViewById(R.id.widgetDate1);
        widget_date2 = findViewById(R.id.widgetDate2);
        widget_stime1 = findViewById(R.id.widgetSTime1);
        widget_stime2 = findViewById(R.id.widgetSTime2);
        widget_etime1 = findViewById(R.id.widgetETime1);
        widget_etime2 = findViewById(R.id.widgetETime2);
        widget_note1 = findViewById(R.id.widgetNote1);
        widget_note2 = findViewById(R.id.widgetNote2);
    }

    // Following the SETS
    // temperature
    public void setWidgetTemp1(String title)
    {
        widget_temp1.setText(title);
    }
    public void setWidgetStartTemp2(int name)
    {
        if (name == 0)
            widget_starttemp2.setText("");
        else
            widget_starttemp2.setText(String.valueOf(name));
    }
    public void setWidgetEndTemp2(int name)
    {
        if (name == 0)
            widget_endtemp2.setText("");
        else
            widget_endtemp2.setText(String.valueOf(name));
    }

    // wind
    public void setWidgetWind1(String title)
    {
        widget_wind1.setText(title);
    }
    public void setWidgetStartWind2(int name)
    {
        if (name == 0)
            widget_startwind2.setText("");
        else
            widget_startwind2.setText(String.valueOf(name));
    }
    public void setWidgetEndWind2(int name)
    {
        if (name == 0)
            widget_endwind2.setText("");
        else
            widget_endwind2.setText(String.valueOf(name));
    }

    // clouds
    public void setWidgetClouds1(String title)
    {
        widget_clouds1.setText(title);
    }
    public void setWidgetStartClouds2(int name)
    {
        if (name == 0)
            widget_startclouds2.setText("");
        else
            widget_startclouds2.setText(String.valueOf(name));
    }
    public void setWidgetEndClouds2(int name)
    {
        if (name == 0)
            widget_endclouds2.setText("");
        else
            widget_endclouds2.setText(String.valueOf(name));
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

    // note
    public void setWidgetNote1(String title)
    {
        widget_note1.setText(title);
    }
    public void setWidgetNote2(String name)
    {
        widget_note2.setText(name);
    }

    // following the GETS
    // get temperature with plausi
    public int getWidgetTemps()
    {
        String text = widget_starttemp2.getText().toString();
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

    public int getWidgetTempe()
    {
        String text = widget_endtemp2.getText().toString();
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
    public int getWidgetWinds()
    {
        String text = widget_startwind2.getText().toString();
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

    public int getWidgetWinde()
    {
        String text = widget_endwind2.getText().toString();
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
        String text = widget_startclouds2.getText().toString();
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

    public int getWidgetCloude()
    {
        String text = widget_endclouds2.getText().toString();
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

    // get stop time
    public String getWidgetNote()
    {
        return widget_note2.getText().toString();
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
