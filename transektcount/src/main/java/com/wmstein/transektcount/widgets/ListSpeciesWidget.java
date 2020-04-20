package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;
import com.wmstein.transektcount.database.Count;
import com.wmstein.transektcount.database.Section;

import java.lang.reflect.Field;
import java.util.Objects;

/*******************************************************
 * ListSpeciesWidget shows count info area for a species.
 * ListSpeciesActivity shows the result page.
 * Created for TransektCount by wmstein on 15.03.2016
 * Last edited on 2020-04-17
 */
public class ListSpeciesWidget extends RelativeLayout
{
    private TextView txtSectName;
    private TextView txtSectRem;
    private TextView txtSpecName;
    private TextView txtSpecNameG;
    private ImageView picSpecies;
    private TextView specCountf1i;
    private TextView specCountf2i;
    private TextView specCountf3i;
    private TextView specCountpi;
    private TextView specCountli;
    private TextView specCountei;
    private TextView txtSpecRem;
    private TextView specCountf1e;
    private TextView specCountf2e;
    private TextView specCountf3e;
    private TextView specCountpe;
    private TextView specCountle;
    private TextView specCountee;

    public Section section;

    public ListSpeciesWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_species, this, true);
        txtSectName = findViewById(R.id.txtSectName);
        txtSectRem = findViewById(R.id.txtSectRem);
        txtSpecName = findViewById(R.id.txtSpecName);
        txtSpecNameG = findViewById(R.id.txtSpecNameG);
        txtSpecRem = findViewById(R.id.txtSpecRem);
        picSpecies = findViewById(R.id.picSpecies);
        specCountf1i = findViewById(R.id.specCountf1i);
        specCountf2i = findViewById(R.id.specCountf2i);
        specCountf3i = findViewById(R.id.specCountf3i);
        specCountpi = findViewById(R.id.specCountpi);
        specCountli = findViewById(R.id.specCountli);
        specCountei = findViewById(R.id.specCountei);
        specCountf1e = findViewById(R.id.specCountf1e);
        specCountf2e = findViewById(R.id.specCountf2e);
        specCountf3e = findViewById(R.id.specCountf3e);
        specCountpe = findViewById(R.id.specCountpe);
        specCountle = findViewById(R.id.specCountle);
        specCountee = findViewById(R.id.specCountee);
    }

    public void setCount(Count spec, Section section)
    {
        txtSectName.setText(section.name);
        txtSpecName.setText(spec.name);
        if (spec.name_g != null)
        {
            if (!spec.name_g.isEmpty())
            {
                txtSpecNameG.setText(spec.name_g);
            }
            else
            {
                txtSpecNameG.setText("");
            }
        }
        
        setImage(spec); // get picSpecies

        if (section.notes != null)
        {
            if (!section.notes.isEmpty())
            {
                txtSectRem.setText(section.notes);
                txtSectRem.setVisibility(View.VISIBLE);
            }
            else if (spec.notes != null)
            {
                if (!spec.notes.isEmpty())
                {
                    txtSectRem.setVisibility(View.INVISIBLE);
                }
                else
                {
                    txtSectRem.setVisibility(View.GONE);
                }
            }
            else
            {
                txtSectRem.setVisibility(View.GONE);
            }
        }

        if (spec.notes != null)
        {
            if (!spec.notes.isEmpty())
            {
                txtSpecRem.setText(spec.notes);
                txtSpecRem.setVisibility(View.VISIBLE);
            }
            else
            {
                txtSpecRem.setVisibility(View.GONE);
            }
        }

        if (spec.count_f1i > 0)
            specCountf1i.setText(String.valueOf(spec.count_f1i));
        if (spec.count_f2i > 0)
            specCountf2i.setText(String.valueOf(spec.count_f2i));
        if (spec.count_f3i > 0)
            specCountf3i.setText(String.valueOf(spec.count_f3i));
        if (spec.count_pi > 0)
            specCountpi.setText(String.valueOf(spec.count_pi));
        if (spec.count_li > 0)
            specCountli.setText(String.valueOf(spec.count_li));
        if (spec.count_ei > 0)
            specCountei.setText(String.valueOf(spec.count_ei));
        if (spec.count_f1e > 0)
            specCountf1e.setText(String.valueOf(spec.count_f1e));
        if (spec.count_f2e > 0)
            specCountf2e.setText(String.valueOf(spec.count_f2e));
        if (spec.count_f3e > 0)
            specCountf3e.setText(String.valueOf(spec.count_f3e));
        if (spec.count_pe > 0)
            specCountpe.setText(String.valueOf(spec.count_pe));
        if (spec.count_le > 0)
            specCountle.setText(String.valueOf(spec.count_le));
        if (spec.count_ee > 0)
            specCountee.setText(String.valueOf(spec.count_ee));
    }

    //Parameters spec_* for use in ListSpeciesActivity
    public int getSpec_sectionid(Count spec)
    {
        return spec.section_id;
    }

    public int getSpec_countf1i(Count spec)
    {
        return spec.count_f1i;
    }

    public int getSpec_countf2i(Count spec)
    {
        return spec.count_f2i;
    }

    public int getSpec_countf3i(Count spec)
    {
        return spec.count_f3i;
    }

    public int getSpec_countpi(Count spec)
    {
        return spec.count_pi;
    }

    public int getSpec_countli(Count spec)
    {
        return spec.count_li;
    }

    public int getSpec_countei(Count spec)
    {
        return spec.count_ei;
    }

    public int getSpec_countf1e(Count spec)
    {
        return spec.count_f1e;
    }

    public int getSpec_countf2e(Count spec)
    {
        return spec.count_f2e;
    }

    public int getSpec_countf3e(Count spec)
    {
        return spec.count_f3e;
    }

    public int getSpec_countpe(Count spec)
    {
        return spec.count_pe;
    }

    public int getSpec_countle(Count spec)
    {
        return spec.count_le;
    }

    public int getSpec_countee(Count spec)
    {
        return spec.count_ee;
    }

    public void setImage(Count newcount)
    {
        String rname = "p" + newcount.code; // species picture resource name

        int resId = getResId(rname);
        if (resId != 0)
        {
            picSpecies.setImageResource(resId);
        }
    }

    // Get resource ID from resource name
    public int getResId(String rName)
    {
        try
        {
            Class res = R.drawable.class;
            Field idField = res.getField(rName);
            return idField.getInt(null);
        } catch (Exception e)
        {
            return 0;
        }
    }

}
