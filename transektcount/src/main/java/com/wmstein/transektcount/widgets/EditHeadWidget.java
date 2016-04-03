package com.wmstein.transektcount.widgets;

/*
 * EditHeadWidget.java used by EditMetaActivity.java
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;

import static java.lang.Integer.valueOf;

/*
 * Created by wmstein for com.wmstein.transektcount on 31.03.2016.
 */
public class EditHeadWidget extends LinearLayout
{
    TextView widget_head; // used for transect_no and inspector_name
    EditText widget_item;
    public int widget_no;

    public EditHeadWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_edit_head, this, true);
        widget_head = (TextView) findViewById(R.id.widgetHead);
        widget_item = (EditText) findViewById(R.id.widgetItem);
    }

    public void setWidgetHead(String title)
    {
        widget_head.setText(title);
    }

    public void setWidgetItem(String name)
    {
        widget_item.setText(name);
    }

    public String getWidgetItem()
    {
        return widget_item.getText().toString();
    }

    public void setWidgetNo(int no)
    {
        widget_no = no;
    }

    public int getWidgetNo()
    {
        widget_no = valueOf(String.valueOf(widget_item));
        return widget_no;
    }

    public void setHint(String hint)
    {
        widget_head.setHint(hint);
    }

}
