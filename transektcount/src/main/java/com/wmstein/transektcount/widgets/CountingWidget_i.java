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
 * Interface for widget_counting_i.xml
 * Created by wmstein 18.12.2016
 * Last edited on 2021-01-26
 */
public class CountingWidget_i extends RelativeLayout
{
    private final TextView namef1i;
    private final TextView namef2i;
    private final TextView namef3i;
    private final TextView namepi;
    private final TextView nameli;
    private final TextView nameei;
    private final AutoFitText countCountf1i; // section internal counters
    private final AutoFitText countCountf2i;
    private final AutoFitText countCountf3i;
    private final AutoFitText countCountpi;
    private final AutoFitText countCountli;
    private final AutoFitText countCountei;

    public Count count;

    public CountingWidget_i(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_i, this, true);
        namef1i = findViewById(R.id.f1iName);
        namef2i = findViewById(R.id.f2iName);
        namef3i = findViewById(R.id.f3iName);
        namepi = findViewById(R.id.piName);
        nameli = findViewById(R.id.liName);
        nameei = findViewById(R.id.eiName);
        countCountf1i = findViewById(R.id.countCountf1i);
        countCountf2i = findViewById(R.id.countCountf2i);
        countCountf3i = findViewById(R.id.countCountf3i);
        countCountpi = findViewById(R.id.countCountpi);
        countCountli = findViewById(R.id.countCountli);
        countCountei = findViewById(R.id.countCountei);
    }

    public void setCounti(Count newcount)
    {
        count = newcount;

        namef1i.setText(getContext().getString(R.string.countImagomfHint));
        namef2i.setText(getContext().getString(R.string.countImagomHint));
        namef3i.setText(getContext().getString(R.string.countImagofHint));
        namepi.setText(getContext().getString(R.string.countPupaHint));
        nameli.setText(getContext().getString(R.string.countLarvaHint));
        nameei.setText(getContext().getString(R.string.countOvoHint));
        countCountf1i.setText(String.valueOf(count.count_f1i));
        countCountf2i.setText(String.valueOf(count.count_f2i));
        countCountf3i.setText(String.valueOf(count.count_f3i));
        countCountpi.setText(String.valueOf(count.count_pi));
        countCountli.setText(String.valueOf(count.count_li));
        countCountei.setText(String.valueOf(count.count_ei));
        ImageButton countUpf1iButton = findViewById(R.id.buttonUpf1i);
        countUpf1iButton.setTag(count.id);
        ImageButton countUpf2iButton = findViewById(R.id.buttonUpf2i);
        countUpf2iButton.setTag(count.id);
        ImageButton countUpf3iButton = findViewById(R.id.buttonUpf3i);
        countUpf3iButton.setTag(count.id);
        ImageButton countUppiButton = findViewById(R.id.buttonUppi);
        countUppiButton.setTag(count.id);
        ImageButton countUpliButton = findViewById(R.id.buttonUpli);
        countUpliButton.setTag(count.id);
        ImageButton countUpeiButton = findViewById(R.id.buttonUpei);
        countUpeiButton.setTag(count.id);
        ImageButton countDownf1iButton = findViewById(R.id.buttonDownf1i);
        countDownf1iButton.setTag(count.id);
        ImageButton countDownf2iButton = findViewById(R.id.buttonDownf2i);
        countDownf2iButton.setTag(count.id);
        ImageButton countDownf3iButton = findViewById(R.id.buttonDownf3i);
        countDownf3iButton.setTag(count.id);
        ImageButton countDownpiButton = findViewById(R.id.buttonDownpi);
        countDownpiButton.setTag(count.id);
        ImageButton countDownliButton = findViewById(R.id.buttonDownli);
        countDownliButton.setTag(count.id);
        ImageButton countDowneiButton = findViewById(R.id.buttonDownei);
        countDowneiButton.setTag(count.id);
    }

    // Count up/down and set value on screen
    public void countUpf1i()
    {
        // increase count_f1i
        countCountf1i.setText(String.valueOf(count.increase_f1i()));
    }

    public void countDownf1i()
    {
        countCountf1i.setText(String.valueOf(count.safe_decrease_f1i()));
    }

    public void countUpf2i()
    {
        countCountf2i.setText(String.valueOf(count.increase_f2i()));
    }

    public void countDownf2i()
    {
        countCountf2i.setText(String.valueOf(count.safe_decrease_f2i()));
    }

    public void countUpf3i()
    {
        countCountf3i.setText(String.valueOf(count.increase_f3i()));
    }

    public void countDownf3i()
    {
        countCountf3i.setText(String.valueOf(count.safe_decrease_f3i()));
    }

    public void countUppi()
    {
        countCountpi.setText(String.valueOf(count.increase_pi()));
    }

    public void countDownpi()
    {
        countCountpi.setText(String.valueOf(count.safe_decrease_pi()));
    }

    public void countUpli()
    {
        countCountli.setText(String.valueOf(count.increase_li()));
    }

    public void countDownli()
    {
        countCountli.setText(String.valueOf(count.safe_decrease_li()));
    }

    public void countUpei()
    {
        countCountei.setText(String.valueOf(count.increase_ei()));
    }

    public void countDownei()
    {
        countCountei.setText(String.valueOf(count.safe_decrease_ei()));
    }

}
