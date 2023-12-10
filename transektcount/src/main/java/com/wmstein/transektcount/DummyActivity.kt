/*
 * Copyright (c) 2016 - 2023. Wilhelm Stein, Bonn, Germany.
 */
package com.wmstein.transektcount

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/*************************************************************************************
 * Dummy activity to overcome Spinner deficiency
 * Re-initializes Spinner to work as expected when repeatedly used in CountingActivity
 * Created by wmstein on 2016-12-28,
 * last edited in Java on 2022-04-30,
 * converted to Kotlin on 2023-07-01,
 * last edited on 2023-12-08
 */
class DummyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // get values from calling activity
        var autoSection = false
        val extras = intent.extras
        if (extras != null)
            autoSection = extras.getBoolean("auto_section")

        if (MyDebug.LOG)
            Log.d(TAG, "30, Dummy")

        // recall CountingActivity(A) with variable autoSection from Extras
        val intent: Intent = if (autoSection)
            Intent(this@DummyActivity, CountingActivityA::class.java)
        else
            Intent(this@DummyActivity, CountingActivity::class.java)
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
