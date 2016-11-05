package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;

/*****************************************************************
 * EditHeadWidget.java used by EditMetaActivity.java
 * Created by wmstein for TransektCount on 31.03.2016.
 */
public class EditHeadWidget extends LinearLayout
{
    TextView widget_no; // used for transect_no title
    EditText widget_no1; // used for transect_no
    TextView widget_name; // used for inspector_name title
    EditText widget_name1; // used for inspector_name

    public EditHeadWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_edit_head, this, true);
        widget_no = (TextView) findViewById(R.id.widgetNo);
        widget_no1 = (EditText) findViewById(R.id.widgetNo1);
        widget_name = (TextView) findViewById(R.id.widgetName);
        widget_name1 = (EditText) findViewById(R.id.widgetName1);
    }

    public void setWidgetNo(String title)
    {
        widget_no.setText(title);
    }

    public void setWidgetNo1(String name)
    {
        widget_no1.setText(name);
    }

    public void setWidgetName(String title)
    {
        widget_name.setText(title);
    }

    public void setWidgetName1(String name)
    {
        widget_name1.setText(name);
    }

    public String getWidgetNo1()
    {
        return widget_no1.getText().toString();
    }

    public String getWidgetName1()
    {
        return widget_name1.getText().toString();
    }

    public void setHint(String hint)
    {
        widget_no.setHint(hint);
    }

}
