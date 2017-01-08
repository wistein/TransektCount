package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;

/****************************************************
 * ListSumWidget shows count totals area
 * ListSpeciesActivity shows the result page
 * Created for TransektCount by wmstein on 15.03.2016
 * and modifications till 07.01.2017
 */
public class ListSumWidget extends LinearLayout
{
    public static String TAG = "transektcountListSumWidget";

    private TextView sumCountf1i;
    private TextView sumCountf2i;
    private TextView sumCountf3i;
    private TextView sumCountpi;
    private TextView sumCountli;
    private TextView sumCountei;
    private TextView sumCountf1e;
    private TextView sumCountf2e;
    private TextView sumCountf3e;
    private TextView sumCountpe;
    private TextView sumCountle;
    private TextView sumCountee;
    
    public ListSumWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_sum_species, this, true);
        sumCountf1i = (TextView) findViewById(R.id.sumCountf1i);
        sumCountf2i = (TextView) findViewById(R.id.sumCountf2i);
        sumCountf3i = (TextView) findViewById(R.id.sumCountf3i);
        sumCountpi = (TextView) findViewById(R.id.sumCountpi);
        sumCountli = (TextView) findViewById(R.id.sumCountli);
        sumCountei = (TextView) findViewById(R.id.sumCountei);
        sumCountf1e = (TextView) findViewById(R.id.sumCountf1e);
        sumCountf2e = (TextView) findViewById(R.id.sumCountf2e);
        sumCountf3e = (TextView) findViewById(R.id.sumCountf3e);
        sumCountpe = (TextView) findViewById(R.id.sumCountpe);
        sumCountle = (TextView) findViewById(R.id.sumCountle);
        sumCountee = (TextView) findViewById(R.id.sumCountee);
    }

    public void setSum(int summf, int summ, int sumf, int sump, int suml, int sumo, 
                       int summfe, int summe, int sumfe, int sumpe, int sumle, int sumoe)
    {
        sumCountf1i.setText(String.valueOf(summf));
        sumCountf2i.setText(String.valueOf(summ));
        sumCountf3i.setText(String.valueOf(sumf));
        sumCountpi.setText(String.valueOf(sump));
        sumCountli.setText(String.valueOf(suml));
        sumCountei.setText(String.valueOf(sumo));
        sumCountf1e.setText(String.valueOf(summfe));
        sumCountf2e.setText(String.valueOf(summe));
        sumCountf3e.setText(String.valueOf(sumfe));
        sumCountpe.setText(String.valueOf(sumpe));
        sumCountle.setText(String.valueOf(sumle));
        sumCountee.setText(String.valueOf(sumoe));
    }
    
}
