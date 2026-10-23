package com.wmstein.transektcount

/***************************************************************************************************
 * Copyright © 2016-2026, Wilhelm Stein, Bonn, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use any of its files except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************************************
 *
 * Set the Settings parameters for TransektCount
 *
 * Parts based on SettingsActivity.java of BeeCount created by Milo Thurston (knirirr)
 * on 2014-05-05, licensed under the Apache License, Version 2.0.
 *
 * Adapted for TransektCount by wmstein on 18.02.2016.
 * Last edited in Java on 2023-06-28,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2026-10-21
 **************************************************************************************************/

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.ViewGroup.MarginLayoutParams
import android.view.Window

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

import com.wmstein.transektcount.database.Head
import com.wmstein.transektcount.database.HeadDataSource
import com.wmstein.transektcount.TransektCountApplication.Companion.getPrefs

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var prefs = getPrefs()
    private var editor = prefs.edit()
    private var headDataSource: HeadDataSource? = null
    private var locationService: LocationService? = null
    private lateinit var locIntent: Intent

    @SuppressLint("CommitPrefEdits", "SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "66, onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        setContentView(R.layout.settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_container))
        { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. You can also update the view padding
            // if that's more appropriate.
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }

            // Return CONSUMED if you don't want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            setStatusBarColor(window, "#303030".toColorInt()) // DarkerGray
        }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        prefs.registerOnSharedPreferenceChangeListener(this)

        //add Preferences From Resource (R.xml.preferences);
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
        editor = prefs.edit() // will be committed on pause
    }
	// End of onCreate()

    fun setStatusBarColor(window: Window, color: Int) {
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            view.setBackgroundColor(color)
            insets
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        // Handle the GPS preference change
        if (key == "pref_auto_section") {
            // Start/stop location service when activated/deactivated
            val transectHasTrack = prefs.getBoolean("transect_has_track", false)
            val autoSection = prefs.getBoolean("pref_auto_section", false)
            locationService = LocationService(applicationContext)
            locIntent = Intent(applicationContext, LocationService::class.java)

            if (transectHasTrack && autoSection) {
                // Start location service
                if (locationService == null) {
                    startService(locIntent)
                }
            } else {
                if (locationService != null) {
                    stopService(locIntent)

                    TransektCountApplication.sectionIdGPS = 0
                    TransektCountApplication.lat = 0.0
                    TransektCountApplication.lon = 0.0
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "155, onPause")

        prefs = getPrefs()
        prefs.edit(commit = true) {
            val dataLanguage = prefs.getString("pref_sel_data_lang", "").toString()

            // Save dataLanguage to the Head data source
            headDataSource = HeadDataSource(applicationContext)
            headDataSource!!.open()
            val head: Head = headDataSource!!.head
            head.data_language = dataLanguage
            headDataSource!!.saveHead(head)
            headDataSource!!.close()

            // Set the sounds
            var ringtone: String

            val alertSoundUri =
                ("android.resource://com.wmstein.transektcount/" + R.raw.alert).toUri()
            ringtone = alertSoundUri.toString()
            putString("alert_sound", ringtone)

            val buttonSoundUri =
                ("android.resource://com.wmstein.transektcount/" + R.raw.button).toUri()
            ringtone = buttonSoundUri.toString()
            putString("button_sound", ringtone)

            val buttonSoundUriM =
                ("android.resource://com.wmstein.transektcount/" + R.raw.button_minus).toUri()
            ringtone = buttonSoundUriM.toString()
            putString("button_sound_minus", ringtone)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    companion object {
        private const val TAG = "SettingsAct"
    }

}
