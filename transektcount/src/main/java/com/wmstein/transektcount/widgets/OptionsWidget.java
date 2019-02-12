package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;

import java.util.Objects;

/***********************************************************************
 * Edit options for species
 * used by CountOptionsActivity in conjunction with widget_options.xml
 * Created by wmstein on 2016-02-16
 * Last edited on 2019-02-12
 */
public class OptionsWidget extends LinearLayout
{
    private TextView instructionsf1i;
    private EditText numberf1i;
    private TextView instructionsf2i;
    private EditText numberf2i;
    private TextView instructionsf3i;
    private EditText numberf3i;
    private TextView instructionspi;
    private EditText numberpi;
    private TextView instructionsli;
    private EditText numberli;
    private TextView instructionsei;
    private EditText numberei;

    private TextView instructionsf1e;
    private EditText numberf1e;
    private TextView instructionsf2e;
    private EditText numberf2e;
    private TextView instructionsf3e;
    private EditText numberf3e;
    private TextView instructionspe;
    private EditText numberpe;
    private TextView instructionsle;
    private EditText numberle;
    private TextView instructionsee;
    private EditText numberee;

    public OptionsWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_options, this, true);
        
        //For transect internal counters
        instructionsf1i = findViewById(R.id.help_textf1i);
        numberf1i = findViewById(R.id.count_parameter_editf1i);
        instructionsf2i = findViewById(R.id.help_textf2i);
        numberf2i = findViewById(R.id.count_parameter_editf2i);
        instructionsf3i = findViewById(R.id.help_textf3i);
        numberf3i = findViewById(R.id.count_parameter_editf3i);
        instructionspi = findViewById(R.id.help_textpi);
        numberpi = findViewById(R.id.count_parameter_editpi);
        instructionsli = findViewById(R.id.help_textli);
        numberli = findViewById(R.id.count_parameter_editli);
        instructionsei = findViewById(R.id.help_textei);
        numberei = findViewById(R.id.count_parameter_editei);
        
        //For transect external counters
        instructionsf1e = findViewById(R.id.help_textf1e);
        numberf1e = findViewById(R.id.counta_parameter_editf1e);
        instructionsf2e = findViewById(R.id.help_textf2e);
        numberf2e = findViewById(R.id.counta_parameter_editf2e);
        instructionsf3e = findViewById(R.id.help_textf3e);
        numberf3e = findViewById(R.id.counta_parameter_editf3e);
        instructionspe = findViewById(R.id.help_textpe);
        numberpe = findViewById(R.id.counta_parameter_editpe);
        instructionsle = findViewById(R.id.help_textle);
        numberle = findViewById(R.id.counta_parameter_editle);
        instructionsee = findViewById(R.id.help_textee);
        numberee = findViewById(R.id.counta_parameter_editee);
    }

    public void setInstructionsf1i(String i)
    {
        instructionsf1i.setText(i);
    }

    public void setParameterValuef1i(int i)
    {
        numberf1i.setText(String.valueOf(i));
    }

    public void setInstructionsf2i(String i)
    {
        instructionsf2i.setText(i);
    }

    public void setParameterValuef2i(int i)
    {
        numberf2i.setText(String.valueOf(i));
    }

    public void setInstructionsf3i(String i)
    {
        instructionsf3i.setText(i);
    }

    public void setParameterValuef3i(int i)
    {
        numberf3i.setText(String.valueOf(i));
    }

    public void setInstructionspi(String i)
    {
        instructionspi.setText(i);
    }

    public void setParameterValuepi(int i)
    {
        numberpi.setText(String.valueOf(i));
    }

    public void setInstructionsli(String i)
    {
        instructionsli.setText(i);
    }

    public void setParameterValueli(int i)
    {
        numberli.setText(String.valueOf(i));
    }

    public void setInstructionsei(String i)
    {
        instructionsei.setText(i);
    }

    public void setParameterValueei(int i)
    {
        numberei.setText(String.valueOf(i));
    }

    public void setInstructionsf1e(String i)
    {
        instructionsf1e.setText(i);
    }

    public void setParameterValuef1e(int i)
    {
        numberf1e.setText(String.valueOf(i));
    }

    public void setInstructionsf2e(String i)
    {
        instructionsf2e.setText(i);
    }

    public void setParameterValuef2e(int i)
    {
        numberf2e.setText(String.valueOf(i));
    }

    public void setInstructionsf3e(String i)
    {
        instructionsf3e.setText(i);
    }

    public void setParameterValuef3e(int i)
    {
        numberf3e.setText(String.valueOf(i));
    }

    public void setInstructionspe(String i)
    {
        instructionspe.setText(i);
    }

    public void setParameterValuepe(int i)
    {
        numberpe.setText(String.valueOf(i));
    }

    public void setInstructionsle(String i)
    {
        instructionsle.setText(i);
    }

    public void setParameterValuele(int i)
    {
        numberle.setText(String.valueOf(i));
    }

    public void setInstructionsee(String i)
    {
        instructionsee.setText(i);
    }

    public void setParameterValueee(int i)
    {
        numberee.setText(String.valueOf(i));
    }

    // this is set to return 0 if it can't parse a value from the box in order
    // that transektcount doesn't crash
    // For transect internal counters
    public int getParameterValuef1i()
    {
        String text = numberf1i.getText().toString();
        if (isEmpty(text))
        {
            return 0;
        }
        else
        {
            try
            {
                return Integer.parseInt(text.replaceAll("[\\D]",""));
            } catch (NumberFormatException e)
            {
                return 0;
            }
        }
    }

    public int getParameterValuef2i()
    {
        String text = numberf2i.getText().toString();
        if (isEmpty(text))
        {
            return 0;
        }
        else
        {
            try
            {
                return Integer.parseInt(text.replaceAll("[\\D]",""));
            } catch (NumberFormatException e)
            {
                return 0;
            }
        }
    }

    public int getParameterValuef3i()
    {
        String text = numberf3i.getText().toString();
        if (isEmpty(text))
        {
            return 0;
        }
        else
        {
            try
            {
                return Integer.parseInt(text.replaceAll("[\\D]",""));
            } catch (NumberFormatException e)
            {
                return 0;
            }
        }
    }

    public int getParameterValuepi()
    {
        String text = numberpi.getText().toString();
        if (isEmpty(text))
        {
            return 0;
        }
        else
        {
            try
            {
                return Integer.parseInt(text.replaceAll("[\\D]",""));
            } catch (NumberFormatException e)
            {
                return 0;
            }
        }
    }

    public int getParameterValueli()
    {
        String text = numberli.getText().toString();
        if (isEmpty(text))
        {
            return 0;
        }
        else
        {
            try
            {
                return Integer.parseInt(text.replaceAll("[\\D]",""));
            } catch (NumberFormatException e)
            {
                return 0;
            }
        }
    }

    public int getParameterValueei()
    {
        String text = numberei.getText().toString();
        if (isEmpty(text))
        {
            return 0;
        }
        else
        {
            try
            {
                return Integer.parseInt(text.replaceAll("[\\D]",""));
            } catch (NumberFormatException e)
            {
                return 0;
            }
        }
    }

    // For transect external counters
    public int getParameterValuef1e()
    {
        String text = numberf1e.getText().toString();
        if (isEmpty(text))
        {
            return 0;
        }
        else
        {
            try
            {
                return Integer.parseInt(text.replaceAll("[\\D]",""));
            } catch (NumberFormatException e)
            {
                return 0;
            }
        }
    }

    public int getParameterValuef2e()
    {
        String text = numberf2e.getText().toString();
        if (isEmpty(text))
        {
            return 0;
        }
        else
        {
            try
            {
                return Integer.parseInt(text.replaceAll("[\\D]",""));
            } catch (NumberFormatException e)
            {
                return 0;
            }
        }
    }

    public int getParameterValuef3e()
    {
        String text = numberf3e.getText().toString();
        if (isEmpty(text))
        {
            return 0;
        }
        else
        {
            try
            {
                return Integer.parseInt(text.replaceAll("[\\D]",""));
            } catch (NumberFormatException e)
            {
                return 0;
            }
        }
    }

    public int getParameterValuepe()
    {
        String text = numberpe.getText().toString();
        if (isEmpty(text))
        {
            return 0;
        }
        else
        {
            try
            {
                return Integer.parseInt(text.replaceAll("[\\D]",""));
            } catch (NumberFormatException e)
            {
                return 0;
            }
        }
    }

    public int getParameterValuele()
    {
        String text = numberle.getText().toString();
        if (isEmpty(text))
        {
            return 0;
        }
        else
        {
            try
            {
                return Integer.parseInt(text.replaceAll("[\\D]",""));
            } catch (NumberFormatException e)
            {
                return 0;
            }
        }
    }

    public int getParameterValueee()
    {
        String text = numberee.getText().toString();
        if (isEmpty(text))
        {
            return 0;
        }
        else
        {
            try
            {
                return Integer.parseInt(text.replaceAll("[\\D]",""));
            } catch (NumberFormatException e)
            {
                return 0;
            }
        }
    }

    /**
     * Following function is taken from the Apache commons-lang3-3.4 library
     * licensed under Apache License Version 2.0, January 2004
     * <p>
     * Checks if a CharSequence is empty ("") or null.
     * <p>
     * isEmpty(null)      = true
     * isEmpty("")        = true
     * isEmpty(" ")       = false
     * isEmpty("bob")     = false
     * isEmpty("  bob  ") = false
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(final CharSequence cs)
    {
        return cs == null || cs.length() == 0;
    }

}
