/*
 * Copyright (c) 2016-2024, Wilhelm Stein, Bonn, Germany.
 */
package com.wmstein.transektcount

import android.content.Intent
import android.graphics.Color.TRANSPARENT
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/****************************************************************************************
 * Dummy activity to overcome Spinner deficiency
 * Re-initializes Spinner to work as expected when repeatedly used in CountingActivity(A)
 * Created by wmstein on 2016-12-28,
 * last edited in Java on 2022-04-30,
 * converted to Kotlin on 2023-07-01,
 * last edited on 2024-06-23
 */
class DummyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.LOG) Log.d(TAG, "25, Dummy")

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < 34)
            overridePendingTransition(0, 0)
        else
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0, TRANSPARENT)
        val intent = Intent(this@DummyActivity, CountingActivity::class.java)
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))

        exit()
    }

    private fun exit() {
        super.finish()
    }

    companion object {
        private const val TAG = "DummyAct"
    }

}
