package com.wmstein.transektcount

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.wmstein.transektcount.TransektCountApplication.Companion.getPrefs

/**
 * SettingsFragment
 * Created by wmstein on 2020-04-17,
 * last edited in Java on 2020-04-17,
 * converted to Kotlin on 2023-06-28,
 * last edited on 2026-01-03
 */
// Load the preferences from preferences.xml
class SettingsFragment : PreferenceFragmentCompat() {
    private var prefs: SharedPreferences? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Get preferences
        prefs = getPrefs()

        // Set proximity option visible if sensor is available in device
        val prefProx: Boolean = prefs!!.getBoolean("enable_prox", false)
        val proxPref: ListPreference? = findPreference("pref_prox")
        proxPref?.isEnabled = prefProx
        if (prefProx)
            proxPref?.setIcon(R.drawable.ic_speaker_phone_black_48dp)
        else
            proxPref?.setIcon(R.drawable.ic_speaker_phone_gray_48dp)

        // Set vibrator option visible if available in device
        val prefVib: Boolean = prefs!!.getBoolean("enable_vib", false)
        val vibPref: SwitchPreferenceCompat? = findPreference("pref_button_vib")
        vibPref?.isEnabled = prefVib
        if (prefVib)
            vibPref?.setIcon(R.drawable.outline_vibration_48)
        else
            vibPref?.setIcon(R.drawable.outline_vibration_gray_48)

        // Set GPS option visible if section tracks exist
        val transectHasTrack = prefs!!.getBoolean("transect_has_track", false)
        val hasTrackPref: SwitchPreferenceCompat? = findPreference("pref_auto_section")
        hasTrackPref?.isEnabled = transectHasTrack
        if (transectHasTrack)
            hasTrackPref?.setIcon(R.drawable.baseline_room_48)
        else
            hasTrackPref?.setIcon(R.drawable.baseline_room_gray_48)
    }

}
