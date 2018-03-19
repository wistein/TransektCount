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
 * Interface for widget_counting_lhi.xml
 * Created by wmstein on 06.09.2016
 */
public class CountingWidgetLH_i extends RelativeLayout
{
    public static String TAG = "transektcountCountingWidgetLH_i";

    private TextView namef1i;
    private TextView namef2i;
    private TextView namef3i;
    private TextView namepi;
    private TextView nameli;
    private TextView nameei;
    private AutoFitText countCountf1i; // section internal counters
    private AutoFitText countCountf2i;
    private AutoFitText countCountf3i;
    private AutoFitText countCountpi;
    private AutoFitText countCountli;
    private AutoFitText countCountei;

    public Count count;

    public CountingWidgetLH_i(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_counting_lhi, this, true);
        namef1i = (TextView) findViewById(R.id.f1iNameLH);
        namef2i = (TextView) findViewById(R.id.f2iNameLH);
        namef3i = (TextView) findViewById(R.id.f3iNameLH);
        namepi = (TextView) findViewById(R.id.piNameLH);
        nameli = (TextView) findViewById(R.id.liNameLH);
        nameei = (TextView) findViewById(R.id.eiNameLH);
        countCountf1i = (AutoFitText) findViewById(R.id.countCountLHf1i);
        countCountf2i = (AutoFitText) findViewById(R.id.countCountLHf2i);
        countCountf3i = (AutoFitText) findViewById(R.id.countCountLHf3i);
        countCountpi = (AutoFitText) findViewById(R.id.countCountLHpi);
        countCountli = (AutoFitText) findViewById(R.id.countCountLHli);
        countCountei = (AutoFitText) findViewById(R.id.countCountLHei);
    }

    public void setCountLHi(Count newcount)
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
        ImageButton countUpf1eButton = (ImageButton) findViewById(R.id.buttonUpLHf1i);
        countUpf1eButton.setTag(count.id);
        ImageButton countUpf2eButton = (ImageButton) findViewById(R.id.buttonUpLHf2i);
        countUpf2eButton.setTag(count.id);
        ImageButton countUpf3eButton = (ImageButton) findViewById(R.id.buttonUpLHf3i);
        countUpf3eButton.setTag(count.id);
        ImageButton countUppeButton = (ImageButton) findViewById(R.id.buttonUpLHpi);
        countUppeButton.setTag(count.id);
        ImageButton countUpleButton = (ImageButton) findViewById(R.id.buttonUpLHli);
        countUpleButton.setTag(count.id);
        ImageButton countUpeeButton = (ImageButton) findViewById(R.id.buttonUpLHei);
        countUpeeButton.setTag(count.id);
        ImageButton countDownf1eButton = (ImageButton) findViewById(R.id.buttonDownLHf1i);
        countDownf1eButton.setTag(count.id);
        ImageButton countDownf2eButton = (ImageButton) findViewById(R.id.buttonDownLHf2i);
        countDownf2eButton.setTag(count.id);
        ImageButton countDownf3eButton = (ImageButton) findViewById(R.id.buttonDownLHf3i);
        countDownf3eButton.setTag(count.id);
        ImageButton countDownpeButton = (ImageButton) findViewById(R.id.buttonDownLHpi);
        countDownpeButton.setTag(count.id);
        ImageButton countDownleButton = (ImageButton) findViewById(R.id.buttonDownLHli);
        countDownleButton.setTag(count.id);
        ImageButton countDowneeButton = (ImageButton) findViewById(R.id.buttonDownLHei);
        countDowneeButton.setTag(count.id);
    }

    // Count up/down and set value on lefthanded screen
    public void countUpLHf1i()
    {
        // increase count_f1i
        countCountf1i.setText(String.valueOf(count.increase_f1i()));
    }

    public void countDownLHf1i()
    {
        countCountf1i.setText(String.valueOf(count.safe_decrease_f1i()));
    }

    public void countUpLHf2i()
    {
        countCountf2i.setText(String.valueOf(count.increase_f2i()));
    }

    public void countDownLHf2i()
    {
        countCountf2i.setText(String.valueOf(count.safe_decrease_f2i()));
    }

    public void countUpLHf3i()
    {
        countCountf3i.setText(String.valueOf(count.increase_f3i()));
    }

    public void countDownLHf3i()
    {
        countCountf3i.setText(String.valueOf(count.safe_decrease_f3i()));
    }

    public void countUpLHpi()
    {
        countCountpi.setText(String.valueOf(count.increase_pi()));
    }

    public void countDownLHpi()
    {
        countCountpi.setText(String.valueOf(count.safe_decrease_pi()));
    }

    public void countUpLHli()
    {
        countCountli.setText(String.valueOf(count.increase_li()));
    }

    public void countDownLHli()
    {
        countCountli.setText(String.valueOf(count.safe_decrease_li()));
    }

    public void countUpLHei()
    {
        countCountei.setText(String.valueOf(count.increase_ei()));
    }

    public void countDownLHei()
    {
        countCountei.setText(String.valueOf(count.safe_decrease_ei()));
    }

}
