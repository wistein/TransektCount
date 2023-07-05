/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */
package com.wmstein.transektcount

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/*****************************************************************************************
 * Dummy to overcome Spinner deficiency
 * Re-initializes Spinner to work as exspected when repeatedly used in CountingActivity
 * Created by wmstein on 2016-12-28,
 * last edited in Java on 2022-04-30
 * converted to Kotlin on 2023-07-01
 */
class DummyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exit()
    }

    private fun exit() {
        super.finish()
    }
}
