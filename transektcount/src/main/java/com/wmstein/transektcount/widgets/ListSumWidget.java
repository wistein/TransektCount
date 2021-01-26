package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wmstein.transektcount.R;

import java.util.Objects;

/**********************************************************
 * ListSumWidget shows count totals area in the result page 
 * created by ListSpeciesActivity
 * Created for TransektCount by wmstein on 15.03.2016
 * Last edited on 2021-01-26
 */
public class ListSumWidget extends LinearLayout
{
    private final TextView sumCountf1i;
    private final TextView sumCountf2i;
    private final TextView sumCountf3i;
    private final TextView sumCountpi;
    private final TextView sumCountli;
    private final TextView sumCountei;
    private final TextView sumCountf1e;
    private final TextView sumCountf2e;
    private final TextView sumCountf3e;
    private final TextView sumCountpe;
    private final TextView sumCountle;
    private final TextView sumCountee;
    private final TextView sumIndInt;
    private final TextView sumIndExt;
    private final TextView sumDiffInd;
    
    public ListSumWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_sum_species, this, true);
        sumCountf1i = findViewById(R.id.sumCountf1i);
        sumCountf2i = findViewById(R.id.sumCountf2i);
        sumCountf3i = findViewById(R.id.sumCountf3i);
        sumCountpi = findViewById(R.id.sumCountpi);
        sumCountli = findViewById(R.id.sumCountli);
        sumCountei = findViewById(R.id.sumCountei);
        sumCountf1e = findViewById(R.id.sumCountf1e);
        sumCountf2e = findViewById(R.id.sumCountf2e);
        sumCountf3e = findViewById(R.id.sumCountf3e);
        sumCountpe = findViewById(R.id.sumCountpe);
        sumCountle = findViewById(R.id.sumCountle);
        sumCountee = findViewById(R.id.sumCountee);
        sumIndInt = findViewById(R.id.sumIndInt);
        sumIndExt = findViewById(R.id.sumIndExt);
        sumDiffInd = findViewById(R.id.sumDiffInd);
    }

    public void setSum(int summf, int summ, int sumf, int sump, int suml, int sumo, 
                       int summfe, int summe, int sumfe, int sumpe, int sumle, int sumoe,
                       int sumInt, int sumExt, int sumDiff)
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
        sumIndInt.setText(String.valueOf(sumInt));
        sumIndExt.setText(String.valueOf(sumExt));
        sumDiffInd.setText(String.valueOf(sumDiff));
    }
    
}
