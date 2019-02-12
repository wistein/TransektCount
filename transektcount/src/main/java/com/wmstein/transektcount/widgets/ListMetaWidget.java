package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;

import java.util.Objects;

/****************************************************
 * ListMetaWidget.java used by ListSpeciesActivity.java
 * Created by wmstein for TransektCount on 03.04.2016,
 * Last edited on 2019-02-12
 */
public class ListMetaWidget extends LinearLayout
{
    TextView widget_lmeta1; // temperature
    TextView widget_litem1;
    TextView widget_lmeta2; // wind
    TextView widget_litem2;
    TextView widget_lmeta3; // clouds
    TextView widget_litem3;
    TextView widget_ldate1; // date
    TextView widget_ldate2;
    TextView widget_ltime1; // start_tm
    TextView widget_litem4;
    TextView widget_ltime2; // end_tm
    TextView widget_litem5;

    public ListMetaWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_meta, this, true);
        widget_lmeta1 = findViewById(R.id.widgetLMeta1);
        widget_litem1 = findViewById(R.id.widgetLItem1);
        widget_lmeta2 = findViewById(R.id.widgetLMeta2);
        widget_litem2 = findViewById(R.id.widgetLItem2);
        widget_lmeta3 = findViewById(R.id.widgetLMeta3);
        widget_litem3 = findViewById(R.id.widgetLItem3);
        widget_ldate1 = findViewById(R.id.widgetLDate1);
        widget_ldate2 = findViewById(R.id.widgetLDate2);
        widget_ltime1 = findViewById(R.id.widgetLTime1);
        widget_litem4 = findViewById(R.id.widgetLItem4);
        widget_ltime2 = findViewById(R.id.widgetLTime2);
        widget_litem5 = findViewById(R.id.widgetLItem5);
    }

    // Following the SETS
    // temperature
    public void setWidgetLMeta1(String title)
    {
        widget_lmeta1.setText(title);
    }

    public void setWidgetLItem1(int name)
    {
        widget_litem1.setText(String.valueOf(name));
    }

    // wind
    public void setWidgetLMeta2(String title)
    {
        widget_lmeta2.setText(title);
    }

    public void setWidgetLItem2(int name)
    {
        widget_litem2.setText(String.valueOf(name));
    }

    // clouds
    public void setWidgetLMeta3(String title)
    {
        widget_lmeta3.setText(title);
    }

    public void setWidgetLItem3(int name)
    {
        widget_litem3.setText(String.valueOf(name));
    }

    // date
    public void setWidgetLDate1(String title)
    {
        widget_ldate1.setText(title);
    }

    public void setWidgetLDate2(String name)
    {
        widget_ldate2.setText(name);
    }

    // start_tm
    public void setWidgetLTime1(String title)
    {
        widget_ltime1.setText(title);
    }

    public void setWidgetLItem4(String name)
    {
        widget_litem4.setText(name);
    }

    // end_tm
    public void setWidgetLTime2(String title)
    {
        widget_ltime2.setText(title);
    }

    public void setWidgetLItem5(String name)
    {
        widget_litem5.setText(name);
    }

}
