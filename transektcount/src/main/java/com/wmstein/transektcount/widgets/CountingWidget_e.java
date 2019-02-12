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

import java.util.Objects;

/****************************************************
 * Interface for widget_counting_e.xml
 * Created by wmstein on 18.12.2016
 * Last edited on 2019-02-12
 */
public class CountingWidget_e extends RelativeLayout
{
    public static String TAG = "transektcountCountingWidget_e";

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

    public CountingWidget_e(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_e, this, true);
        namef1e = findViewById(R.id.f1eName);
        namef2e = findViewById(R.id.f2eName);
        namef3e = findViewById(R.id.f3eName);
        namepe = findViewById(R.id.peName);
        namele = findViewById(R.id.leName);
        nameee = findViewById(R.id.eeName);
        countCountf1e = findViewById(R.id.countCountf1e);
        countCountf2e = findViewById(R.id.countCountf2e);
        countCountf3e = findViewById(R.id.countCountf3e);
        countCountpe = findViewById(R.id.countCountpe);
        countCountle = findViewById(R.id.countCountle);
        countCountee = findViewById(R.id.countCountee);
    }

    public void setCounte(Count newcount)
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
        ImageButton countUpf1eButton = findViewById(R.id.buttonUpf1e);
        countUpf1eButton.setTag(count.id);
        ImageButton countUpf2eButton = findViewById(R.id.buttonUpf2e);
        countUpf2eButton.setTag(count.id);
        ImageButton countUpf3eButton = findViewById(R.id.buttonUpf3e);
        countUpf3eButton.setTag(count.id);
        ImageButton countUppeButton = findViewById(R.id.buttonUppe);
        countUppeButton.setTag(count.id);
        ImageButton countUpleButton = findViewById(R.id.buttonUple);
        countUpleButton.setTag(count.id);
        ImageButton countUpeeButton = findViewById(R.id.buttonUpee);
        countUpeeButton.setTag(count.id);
        ImageButton countDownf1eButton = findViewById(R.id.buttonDownf1e);
        countDownf1eButton.setTag(count.id);
        ImageButton countDownf2eButton = findViewById(R.id.buttonDownf2e);
        countDownf2eButton.setTag(count.id);
        ImageButton countDownf3eButton = findViewById(R.id.buttonDownf3e);
        countDownf3eButton.setTag(count.id);
        ImageButton countDownpeButton = findViewById(R.id.buttonDownpe);
        countDownpeButton.setTag(count.id);
        ImageButton countDownleButton = findViewById(R.id.buttonDownle);
        countDownleButton.setTag(count.id);
        ImageButton countDowneeButton = findViewById(R.id.buttonDownee);
        countDowneeButton.setTag(count.id);
    }

    // Count up/down and set value on screen
    public void countUpf1e()
    {
        countCountf1e.setText(String.valueOf(count.increase_f1e()));
    }

    public void countDownf1e()
    {
        countCountf1e.setText(String.valueOf(count.safe_decrease_f1e()));
    }

    public void countUpf2e()
    {
        countCountf2e.setText(String.valueOf(count.increase_f2e()));
    }

    public void countDownf2e()
    {
        countCountf2e.setText(String.valueOf(count.safe_decrease_f2e()));
    }

    public void countUpf3e()
    {
        countCountf3e.setText(String.valueOf(count.increase_f3e()));
    }

    public void countDownf3e()
    {
        countCountf3e.setText(String.valueOf(count.safe_decrease_f3e()));
    }

    public void countUppe()
    {
        countCountpe.setText(String.valueOf(count.increase_pe()));
    }

    public void countDownpe()
    {
        countCountpe.setText(String.valueOf(count.safe_decrease_pe()));
    }

    public void countUple()
    {
        countCountle.setText(String.valueOf(count.increase_le()));
    }

    public void countDownle()
    {
        countCountle.setText(String.valueOf(count.safe_decrease_le()));
    }

    public void countUpee()
    {
        countCountee.setText(String.valueOf(count.increase_ee()));
    }

    public void countDownee()
    {
        countCountee.setText(String.valueOf(count.safe_decrease_ee()));
    }

}
