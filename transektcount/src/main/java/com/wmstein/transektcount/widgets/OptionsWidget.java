package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by milo on 27/05/2014.
 * Changed by wmstein on 18.02.2016
 * Supplemented with functions for transect externel counter
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

    public void setSize(int i)
    {
        instructions.setTextSize(i);
    }

    public void setInstructionsa(String i)
    {
        instructionsa.setText(i);
    }

    public void setParameterValuea(int i)
    {
        numbera.setText(String.valueOf(i));
    }

    public void setSizea(int i)
    {
        instructionsa.setTextSize(i);
    }

    // this is set to return 0 if it can't parse a value from the box in order
    // that transektcount doesn't crash
    // For transect internal counter
    public int getParameterValue()
    {
        String text = number.getText().toString();
        if (StringUtils.isEmpty(text))
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
        if (StringUtils.isEmpty(text))
        {
            return Integer.valueOf(0);
        }
        else
        {
            return Integer.parseInt(text);
        }
    }
}
