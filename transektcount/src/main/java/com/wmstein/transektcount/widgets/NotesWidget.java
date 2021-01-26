package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.transektcount.MyDebug;
import com.wmstein.transektcount.R;

import java.util.Objects;

/****************************************************
 * Created by milo on 26/05/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016
 * Last edited on 2021-01-26
 */
public class NotesWidget extends LinearLayout
{
    private static final String TAG = "TransektCntNotesWidget";
    public String section_notes;
    private final TextView textView;

    public NotesWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_notes, this, true);
        textView = findViewById(R.id.notes_text);
    }

    public void setNotes(String notes)
    {
        section_notes = notes;
        textView.setText(section_notes);
    }

    public void setFont(Boolean large)
    {
        if (large)
        {
            if (MyDebug.LOG)
                Log.i(TAG, "Setzt große Schrift.");
            textView.setTextSize(15);
        }
        else
        {
            if (MyDebug.LOG)
                Log.i(TAG, "Setzt kleine Schrift.");
            textView.setTextSize(12);
        }
    }

}

