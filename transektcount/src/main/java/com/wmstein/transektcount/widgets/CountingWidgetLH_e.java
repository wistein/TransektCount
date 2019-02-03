/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.transektcount.AutoFitText;
import com.wmstein.transektcount.R;
import com.wmstein.transektcount.database.Count;

/**********************************
 * Created by wmstein on 06.12.2016
 * Last edited on 2019-02-02
 */
public class CountingWidgetLH_e extends RelativeLayout
{
    public static String TAG = "transektcountCountingWidgetLH_e";

    private TextView namef1e;
    private TextView namef2e;
    private TextView namef3e;
    private TextView namepe;
    private TextView namele;
    private TextView nameee;
    private AutoFitText countCountf1e; // external counters
    private AutoFitText countCountf2e;
    private AutoFitText countCountf3e;
    private AutoFitText countCountpe;
    private AutoFitText countCountle;
    private AutoFitText countCountee;

    public Count count;

    public CountingWidgetLH_e(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_counting_lhe, this, true);
        namef1e = findViewById(R.id.f1eNameLH);
        namef2e = findViewById(R.id.f2eNameLH);
        namef3e = findViewById(R.id.f3eNameLH);
        namepe = findViewById(R.id.peNameLH);
        namele = findViewById(R.id.leNameLH);
        nameee = findViewById(R.id.eeNameLH);
        countCountf1e = findViewById(R.id.countCountLHf1e);
        countCountf2e = findViewById(R.id.countCountLHf2e);
        countCountf3e = findViewById(R.id.countCountLHf3e);
        countCountpe = findViewById(R.id.countCountLHpe);
        countCountle = findViewById(R.id.countCountLHle);
        countCountee = findViewById(R.id.countCountLHee);
    }

    public void setCountLHe(Count newcount)
    {
        count = newcount;

        namef1e.setText(getContext().getString(R.string.countImagomfHint));
        namef2e.setText(getContext().getString(R.string.countImagomHint));
        namef3e.setText(getContext().getString(R.string.countImagofHint));
        namepe.setText(getContext().getString(R.string.countPupaHint));
        namele.setText(getContext().getString(R.string.countLarvaHint));
        nameee.setText(getContext().getString(R.string.countOvoHint));
        countCountf1e.setText(String.valueOf(count.count_f1e));
        countCountf2e.setText(String.valueOf(count.count_f2e));
        countCountf3e.setText(String.valueOf(count.count_f3e));
        countCountpe.setText(String.valueOf(count.count_pe));
        countCountle.setText(String.valueOf(count.count_le));
        countCountee.setText(String.valueOf(count.count_ee));
        ImageButton countUpf1eButton = findViewById(R.id.buttonUpLHf1e);
        countUpf1eButton.setTag(count.id);
        ImageButton countUpf2eButton = findViewById(R.id.buttonUpLHf2e);
        countUpf2eButton.setTag(count.id);
        ImageButton countUpf3eButton = findViewById(R.id.buttonUpLHf3e);
        countUpf3eButton.setTag(count.id);
        ImageButton countUppeButton = findViewById(R.id.buttonUpLHpe);
        countUppeButton.setTag(count.id);
        ImageButton countUpleButton = findViewById(R.id.buttonUpLHle);
        countUpleButton.setTag(count.id);
        ImageButton countUpeeButton = findViewById(R.id.buttonUpLHee);
        countUpeeButton.setTag(count.id);
        ImageButton countDownf1eButton = findViewById(R.id.buttonDownLHf1e);
        countDownf1eButton.setTag(count.id);
        ImageButton countDownf2eButton = findViewById(R.id.buttonDownLHf2e);
        countDownf2eButton.setTag(count.id);
        ImageButton countDownf3eButton = findViewById(R.id.buttonDownLHf3e);
        countDownf3eButton.setTag(count.id);
        ImageButton countDownpeButton = findViewById(R.id.buttonDownLHpe);
        countDownpeButton.setTag(count.id);
        ImageButton countDownleButton = findViewById(R.id.buttonDownLHle);
        countDownleButton.setTag(count.id);
        ImageButton countDowneeButton = findViewById(R.id.buttonDownLHee);
        countDowneeButton.setTag(count.id);
    }

    // Count up/down and set value on lefthanded screen
    public void countUpLHf1e()
    {
        countCountf1e.setText(String.valueOf(count.increase_f1e()));
    }

    public void countDownLHf1e()
    {
        countCountf1e.setText(String.valueOf(count.safe_decrease_f1e()));
    }

    public void countUpLHf2e()
    {
        countCountf2e.setText(String.valueOf(count.increase_f2e()));
    }

    public void countDownLHf2e()
    {
        countCountf2e.setText(String.valueOf(count.safe_decrease_f2e()));
    }

    public void countUpLHf3e()
    {
        countCountf3e.setText(String.valueOf(count.increase_f3e()));
    }

    public void countDownLHf3e()
    {
        countCountf3e.setText(String.valueOf(count.safe_decrease_f3e()));
    }

    public void countUpLHpe()
    {
        countCountpe.setText(String.valueOf(count.increase_pe()));
    }

    public void countDownLHpe()
    {
        countCountpe.setText(String.valueOf(count.safe_decrease_pe()));
    }

    public void countUpLHle()
    {
        countCountle.setText(String.valueOf(count.increase_le()));
    }

    public void countDownLHle()
    {
        countCountle.setText(String.valueOf(count.safe_decrease_le()));
    }

    public void countUpLHee()
    {
        countCountee.setText(String.valueOf(count.increase_ee()));
    }

    public void countDownLHee()
    {
        countCountee.setText(String.valueOf(count.safe_decrease_ee()));
    }

}
