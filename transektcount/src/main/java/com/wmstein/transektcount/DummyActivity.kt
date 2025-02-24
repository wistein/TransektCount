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

/*****************************************************************************************
 * Dummy activity
 *  - to overcome Spinner deficiency by
 *    re-initializing Spinner to work as expected when repeatedly used in CountingActivity
 *  - to re-enter AddSpeciesActivity, DelSpeciesActivity or EditSectionListActivity
 *    to rebuild their views after restricted species selection
 *
 * Created by wmstein on 2016-12-28,
 * last edited in Java on 2022-04-30,
 * converted to Kotlin on 2023-07-01,
 * last edited on 2024-11-12
 */
class DummyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.DLOG) Log.d(TAG, "29, Dummy")

        var initChars = ""
        var isFlag = ""
        var sectId = 1

        // get value from calling Activity respective getInitialChars()
        val extras = intent.extras
        if (extras != null) {
            initChars = extras.getString("init_Chars").toString()
            isFlag = extras.getString("is_Flag").toString()
            sectId = extras.getInt("section_id")
        }

        // Set smooth transition to the called activity
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < 34)
            overridePendingTransition(0, 0)
        else
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0, TRANSPARENT)

        when (isFlag) {
            "isCount" -> intent = Intent(this@DummyActivity, CountingActivity::class.java)
            "isAdd" -> intent = Intent(this@DummyActivity, AddSpeciesActivity::class.java)
            "isDel" -> intent = Intent(this@DummyActivity, DelSpeciesActivity::class.java)
            "isEdit" -> intent = Intent(this@DummyActivity, EditSectionListActivity::class.java)
            else -> exit()
        }

        intent.putExtra("section_id", sectId)
        intent.putExtra("init_Chars", initChars)
        intent.putExtra("is_Flag", "")
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
