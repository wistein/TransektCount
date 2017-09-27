/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.transektcount;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/************************************************************
 * Dummy to overcome Spinner deficiency
 * Created by wmstein on 28.12.2016
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
