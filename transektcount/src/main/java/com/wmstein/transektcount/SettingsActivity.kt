package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup.MarginLayoutParams
import android.view.Window
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**********************************************************
 * Set the Settings parameters for TransektCount
 * Based on SettingsActivity created by milo on 05/05/2014.
 * Adapted for TransektCount by wmstein on 18.02.2016
 * Last edited in Java on 2023-06-28,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2025-12-29
 */
class SettingsActivity : AppCompatActivity() {
    private var prefs = TransektCountApplication.getPrefs()
    private var editor: SharedPreferences.Editor? = null

    @SuppressLint("CommitPrefEdits", "SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

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
            setStatusBarColor(window, ContextCompat.getColor(applicationContext,
                R.color.DarkerGray))
        }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        //add Preferences From Resource (R.xml.preferences);
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
        editor = prefs.edit() // will be committed on pause
    }
	// End of onCreate()

    @RequiresApi(Build.VERSION_CODES.R)
    fun setStatusBarColor(window: Window, color: Int) {
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            view.setBackgroundColor(color)
            insets
        }
    }

    override fun onPause() {
        super.onPause()

        var ringtone: String

        val alertSoundUri =
            ("android.resource://com.wmstein.transektcount/" + R.raw.alert).toUri()
        ringtone = alertSoundUri.toString()
        editor!!.putString("alert_sound", ringtone)

        val buttonSoundUri =
            ("android.resource://com.wmstein.transektcount/" + R.raw.button).toUri()
        ringtone = buttonSoundUri.toString()
        editor!!.putString("button_sound", ringtone)

        val buttonSoundUriM =
            ("android.resource://com.wmstein.transektcount/" + R.raw.button_minus).toUri()
        ringtone = buttonSoundUriM.toString()
        editor!!.putString("button_sound_minus", ringtone)
        editor!!.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
