/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.transektcount;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/*****************************************************************************************
 * Dummy to overcome Spinner deficiency
 * Re-initializes Spinner to work as exspected when repeatedly used in Counting(L)Activity
 * Created by wmstein on 2016-12-28,
 * last edited on 2022-04-30
 */
public class DummyActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        exit();
    }

    public void exit()
    {
        super.finish();
    }
    
}
