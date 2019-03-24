package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.wmstein.transektcount.R;

import java.io.Serializable;
import java.util.Objects;

/****************************************************
 * CountEditWidget is used by EditSectionActivity
 * Adopted for TransektCount by wmstein on 18.02.2016
 * Last edited on 2019-03-23
 */
public class CountEditWidget extends LinearLayout implements Serializable
{
    private transient EditText countName;
    private transient EditText countNameG;
    private transient EditText countCode;
    private ImageButton deleteButton;
    public int countId;

    public CountEditWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_count, this, true);
        countName = findViewById(R.id.countName);
        countNameG = findViewById(R.id.countNameG);
        countCode = findViewById(R.id.countCode);
        deleteButton = findViewById(R.id.deleteCount);
        deleteButton.setTag(0);
    }

    public String getCountName()
    {
        return countName.getText().toString();
    }

    public void setCountName(String name)
    {
        countName.setText(name);
    }

    public String getCountNameG()
    {
        return countNameG.getText().toString();
    }

    public void setCountNameG(String name)
    {
        countNameG.setText(name);
    }

    public String getCountCode()
    {
        return countCode.getText().toString();
    }

    public void setCountCode(String name)
    {
        countCode.setText(name);
    }

    public void setCountId(int id)
    {
        countId = id;
        deleteButton.setTag(id);
    }

}
