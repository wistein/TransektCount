/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;

/*******************************************************
 * Used by EditSectionActivity and widget_edit_title.xml
 * Created by by milo on 05/05/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016
 */
public class EditTitleWidget extends LinearLayout
{
    TextView widget_title;
    EditText section_name;

    public EditTitleWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_edit_title, this, true);
        widget_title = (TextView) findViewById(R.id.widgeteditTitle);
        section_name = (EditText) findViewById(R.id.editsectionName);
    }

    public void setWidgetTitle(String title)
    {
        widget_title.setText(title);
    }

    public void setSectionName(String name)
    {
        section_name.setText(name);
    }

    public String getSectionName()
    {
        return section_name.getText().toString();
    }

    public void setHint(String hint)
    {
        section_name.setHint(hint);
    }

}
