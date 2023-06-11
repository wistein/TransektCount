package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;

import java.util.Objects;

/*****************************************************************
 * EditHeadWidget.java used by EditMetaActivity.java
 * Created by wmstein for TransektCount on 31.03.2016.
 * Last edited on 2023-05-09
 */
public class EditHeadWidget extends LinearLayout
{
    final TextView widget_no; // used for transect_no title
    final EditText widget_no1; // used for transect_no
    final TextView widget_name; // used for inspector_name title
    final EditText widget_name1; // used for inspector_name

    public EditHeadWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_head, this, true);
        widget_no = findViewById(R.id.widgetNo);
        widget_no1 = findViewById(R.id.widgetNo1);
        widget_name = findViewById(R.id.widgetName);
        widget_name1 = findViewById(R.id.widgetName1);
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
