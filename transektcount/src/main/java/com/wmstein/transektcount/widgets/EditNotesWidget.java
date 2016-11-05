package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;

/*******************************************************
 * Used by EditSectionActivity and widget_edit_notes.xml
 * Created by wmstein on 23.10.2016
 */
public class EditNotesWidget extends LinearLayout
{
    TextView widget_notes;
    EditText section_notes;

    public EditNotesWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_edit_notes, this, true);
        widget_notes = (TextView) findViewById(R.id.widgetNotes);
        section_notes = (EditText) findViewById(R.id.sectionNotes);
    }

    public void setWidgetNotes(String title)
    {
        widget_notes.setText(title);
    }

    public void setSectionNotes(String name)
    {
        section_notes.setText(name);
    }

    public String getSectionNotes()
    {
        return section_notes.getText().toString();
    }

    public void setHint(String hint)
    {
        section_notes.setHint(hint);
    }

}
