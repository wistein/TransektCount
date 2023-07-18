package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity

/**********************************************************
 * Set the Settings parameters for TransektCount
 * Based on SettingsActivity created by milo on 05/05/2014.
 * Adapted for TransektCount by wmstein on 18.02.2016
 * Last edited in Java on 2023-06-28,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2023-07-17
 */
class SettingsActivity : AppCompatActivity() {
    private var prefs = TransektCountApplication.getPrefs()
    private var editor: SharedPreferences.Editor? = null

    @SuppressLint("CommitPrefEdits", "SourceLockedOrientationActivity")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar!!.hide()
        setContentView(R.layout.settings)

        //add Preferences From Resource (R.xml.preferences);
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
        editor = prefs.edit() // will be committed on pause
    }

    override fun onPause() {
        super.onPause()

        var ringtone: String
        val alertSoundPref = prefs!!.getBoolean("pref_alert_sound", false)
        val buttonSoundPref = prefs!!.getBoolean("pref_button_sound", false)
        if (alertSoundPref) {
            val alert_sound_uri =
                Uri.parse("android.resource://com.wmstein.transektcount/" + R.raw.alert)
            ringtone = alert_sound_uri.toString()
            editor!!.putString("alert_sound", ringtone)
        }
        if (buttonSoundPref) {
            val button_sound_uri =
                Uri.parse("android.resource://com.wmstein.transektcount/" + R.raw.button)
            ringtone = button_sound_uri.toString()
            editor!!.putString("button_sound", ringtone)
            val button_sound_uri_m =
                Uri.parse("android.resource://com.wmstein.transektcount/" + R.raw.button_minus)
            ringtone = button_sound_uri_m.toString()
            editor!!.putString("button_sound_minus", ringtone)
        }
        editor!!.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            startActivity(
                Intent(
                    this,
                    WelcomeActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

}
