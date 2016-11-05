package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;
import com.wmstein.transektcount.database.Count;
import com.wmstein.transektcount.database.Section;


/****************************************************
 * Created for TransektCount by wmstein on 15.03.2016
 */
public class ListSpeciesWidget extends RelativeLayout
{
    public static String TAG = "transektcountListSpeciesWidget";

    private TextView txtSectName;
    private TextView txtSectRem;
    private TextView txtSpecName;
    private TextView specCount;
    private TextView txtSpecRem;
    private TextView specCounta;

    public int spec_count;
    public int spec_counta;
    public Count spec;
    public Section section;

    public ListSpeciesWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_list_species, this, true);
        txtSectName = (TextView) findViewById(R.id.txtSectName);
        txtSectRem = (TextView) findViewById(R.id.txtSectRem);
        txtSpecName = (TextView) findViewById(R.id.txtSpecName);
        specCount = (TextView) findViewById(R.id.specCount);
        txtSpecRem = (TextView) findViewById(R.id.txtSpecRem);
        specCounta = (TextView) findViewById(R.id.specCounta);
    }

    public void setCount(Count spec, Section section)
    {
        txtSectName.setText(section.name);
        txtSectName.setTextColor(0xffffffff); // white
        txtSectRem.setText(section.notes);
        txtSectRem.setTextColor(0xffffffff);
        txtSpecName.setText(spec.name);
        specCount.setText(String.valueOf(spec.count));
        txtSpecRem.setText(spec.notes);
        specCounta.setText(String.valueOf(spec.counta));
    }

    public void setCount1()
    {
        txtSectName.setTextColor(0xff444444); // dark grey
        txtSectRem.setTextColor(0xff444444);
    }

    //Parameter spec_count for use in ListSpeciesActivity
    public int getSpec_count(Count newcount)
    {
        spec = newcount;
        spec_count = spec.count;
        return spec_count;
    }

    public int getSpec_counta(Count newcount)
    {
        spec = newcount;
        spec_counta = spec.counta;
        return spec_counta;
    }

}
