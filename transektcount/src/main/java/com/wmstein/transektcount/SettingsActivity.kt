package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

/**********************************************************
 * Set the Settings parameters for TransektCount
 * Based on SettingsActivity created by milo on 05/05/2014.
 * Adapted for TransektCount by wmstein on 18.02.2016
 * Last edited in Java on 2023-06-28,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2025-05-18
 */
class SettingsActivity : AppCompatActivity() {
    private var prefs = TransektCountApplication.getPrefs()
    private var editor: SharedPreferences.Editor? = null

    @SuppressLint("CommitPrefEdits", "SourceLockedOrientationActivity")
    public override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

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
//            startActivity(Intent(this, WelcomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
