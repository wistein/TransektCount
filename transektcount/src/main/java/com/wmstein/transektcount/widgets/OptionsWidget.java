package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;

/***********************************************************************
 * Edit options for species
 * used by CountOptionsActivity in conjunction with widget_options.xml
 * Created by milo on 27/05/2014.
 * Adopted and supplemented with functions for transect external counter
 * by wmstein on 18.02.2016
 */
public class OptionsWidget extends LinearLayout
{
    private TextView instructions;
    private TextView number;
    private TextView instructionsa;
    private TextView numbera;

    public OptionsWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_options, this, true);
        //For transect internal counter
        instructions = (TextView) findViewById(R.id.help_text);
        number = (EditText) findViewById(R.id.count_parameter_edit);
        //For transect external counter
        instructionsa = (TextView) findViewById(R.id.help_texta);
        numbera = (EditText) findViewById(R.id.counta_parameter_edit);
    }

    public void setInstructions(String i)
    {
        instructions.setText(i);
    }

    public void setParameterValue(int i)
    {
        number.setText(String.valueOf(i));
    }

    public void setInstructionsa(String i)
    {
        instructionsa.setText(i);
    }

    public void setParameterValuea(int i)
    {
        numbera.setText(String.valueOf(i));
    }

    // this is set to return 0 if it can't parse a value from the box in order
    // that transektcount doesn't crash
    // For transect internal counter
    public int getParameterValue()
    {
        String text = number.getText().toString();
        if (isEmpty(text))
        {
            return Integer.valueOf(0);
        }
        else
        {
            return Integer.parseInt(text);
        }
    }

    // For transect external counter
    public int getParameterValuea()
    {
        String text = numbera.getText().toString();
        if (isEmpty(text))
        {
            return Integer.valueOf(0);
        }
        else
        {
            return Integer.parseInt(text);
        }
    }

    /**
     * Following function is taken from the Apache commons-lang3-3.4 library
     * licensed under Apache License Version 2.0, January 2004
     *
     * Checks if a CharSequence is empty ("") or null.
     *
     * isEmpty(null)      = true
     * isEmpty("")        = true
     * isEmpty(" ")       = false
     * isEmpty("bob")     = false
     * isEmpty("  bob  ") = false
     *
     * @param cs  the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

}
