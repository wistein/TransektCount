package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;
import com.wmstein.transektcount.database.Count;
import com.wmstein.transektcount.database.Section;

import java.lang.reflect.Field;

/*******************************************************
 * ListSpeciesWidget shows count info area for a species
 * ListSpeciesActivity shows the result page
 * Created for TransektCount by wmstein on 15.03.2016
 * and modifications till 07.01.2017
 */
public class ListSpeciesWidget extends RelativeLayout
{
    public static String TAG = "transektcountListSpeciesWidget";

    private TextView txtSectName;
    private TextView txtSectRem;
    private TextView txtSpecName;
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

    public int spec_sectionid;
    public int spec_countf1i;
    public int spec_countf2i;
    public int spec_countf3i;
    public int spec_countpi;
    public int spec_countli;
    public int spec_countei;
    public int spec_countf1e;
    public int spec_countf2e;
    public int spec_countf3e;
    public int spec_countpe;
    public int spec_countle;
    public int spec_countee;
    public Section section;

    public ListSpeciesWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_list_species, this, true);
        txtSectName = (TextView) findViewById(R.id.txtSectName);
        txtSectRem = (TextView) findViewById(R.id.txtSectRem);
        txtSpecName = (TextView) findViewById(R.id.txtSpecName);
        txtSpecRem = (TextView) findViewById(R.id.txtSpecRem);
        picSpecies = (ImageView) findViewById(R.id.picSpecies);
        specCountf1i = (TextView) findViewById(R.id.specCountf1i);
        specCountf2i = (TextView) findViewById(R.id.specCountf2i);
        specCountf3i = (TextView) findViewById(R.id.specCountf3i);
        specCountpi = (TextView) findViewById(R.id.specCountpi);
        specCountli = (TextView) findViewById(R.id.specCountli);
        specCountei = (TextView) findViewById(R.id.specCountei);
        specCountf1e = (TextView) findViewById(R.id.specCountf1e);
        specCountf2e = (TextView) findViewById(R.id.specCountf2e);
        specCountf3e = (TextView) findViewById(R.id.specCountf3e);
        specCountpe = (TextView) findViewById(R.id.specCountpe);
        specCountle = (TextView) findViewById(R.id.specCountle);
        specCountee = (TextView) findViewById(R.id.specCountee);
    }

    public void setCount(Count spec, Section section)
    {
        txtSectName.setText(section.name);
        txtSectName.setTextColor(0xffffffff); // white
        txtSectRem.setText(section.notes);
        txtSectRem.setTextColor(0xffffffff);
        txtSpecName.setText(spec.name);
        // get picSpecies
        setImage(spec);
        txtSpecRem.setText(spec.notes);
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
        spec_sectionid = spec.section_id;
        return spec_sectionid;
    }

    public int getSpec_countf1i(Count spec)
    {
        spec_countf1i = spec.count_f1i;
        return spec_countf1i;
    }

    public int getSpec_countf2i(Count spec)
    {
        spec_countf2i = spec.count_f2i;
        return spec_countf2i;
    }

    public int getSpec_countf3i(Count spec)
    {
        spec_countf3i = spec.count_f3i;
        return spec_countf3i;
    }

    public int getSpec_countpi(Count spec)
    {
        spec_countpi = spec.count_pi;
        return spec_countpi;
    }

    public int getSpec_countli(Count spec)
    {
        spec_countli = spec.count_li;
        return spec_countli;
    }

    public int getSpec_countei(Count spec)
    {
        spec_countei = spec.count_ei;
        return spec_countei;
    }

    public int getSpec_countf1e(Count spec)
    {
        spec_countf1e = spec.count_f1e;
        return spec_countf1e;
    }

    public int getSpec_countf2e(Count spec)
    {
        spec_countf2e = spec.count_f2e;
        return spec_countf2e;
    }

    public int getSpec_countf3e(Count spec)
    {
        spec_countf3e = spec.count_f3e;
        return spec_countf3e;
    }

    public int getSpec_countpe(Count spec)
    {
        spec_countpe = spec.count_pe;
        return spec_countpe;
    }

    public int getSpec_countle(Count spec)
    {
        spec_countle = spec.count_le;
        return spec_countle;
    }

    public int getSpec_countee(Count spec)
    {
        spec_countee = spec.count_ee;
        return spec_countee;
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
