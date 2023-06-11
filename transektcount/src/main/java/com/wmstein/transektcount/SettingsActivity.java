package com.wmstein.transektcount;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.util.Objects;

/**********************************************************
 * Set the Settings parameters for TransektCount
 * Based on SettingsActivity created by milo on 05/05/2014.
 * Adapted for TransektCount by wmstein on 18.02.2016
 * Last edited on 2023-06-10
 */
public class SettingsActivity extends AppCompatActivity
{
    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    
    @Override
    @SuppressLint({"CommitPrefEdits", "SourceLockedOrientationActivity"})
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.settings);

        //add Preferences From Resource (R.xml.preferences);
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settings_container, new SettingsFragment())
            .commit();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit(); // will be committed on pause
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        String ringtone;
        boolean alertSoundPref = prefs.getBoolean("pref_alert_sound", false);
        boolean buttonSoundPref = prefs.getBoolean("pref_button_sound", false);
        if (alertSoundPref)
        {
            Uri alert_sound_uri = Uri.parse("android.resource://com.wmstein.transektcount/" + R.raw.alert);
            ringtone = alert_sound_uri.toString();
            editor.putString("alert_sound", ringtone);
        }

        if (buttonSoundPref)
        {
            Uri button_sound_uri = Uri.parse("android.resource://com.wmstein.transektcount/" + R.raw.button);
            ringtone = button_sound_uri.toString();
            editor.putString("button_sound", ringtone);
            Uri button_sound_uri_m = Uri.parse("android.resource://com.wmstein.transektcount/" + R.raw.button_minus);
            ringtone = button_sound_uri_m.toString();
            editor.putString("button_sound_minus", ringtone);
        }
        
        editor.commit();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            startActivity(new Intent(this, super.getClass()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        else
        {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
    
}
