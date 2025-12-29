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
 * last edited on 2025-12-29
 */
// Load the preferences from preferences.xml
class SettingsFragment : PreferenceFragmentCompat() {
    private var prefs: SharedPreferences? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Get preferences
        prefs = getPrefs()

        // Set proximity option enabled if sensor is available in device
        val prefProx: Boolean = prefs!!.getBoolean("enable_prox", false)
        val proxPref: ListPreference? = findPreference("pref_prox")
        proxPref?.isEnabled = prefProx

        // Set GPS option enabled if section tracks exist
        val transectHasTrack = prefs!!.getBoolean("transect_has_track", false)
        val hasTrackPref: SwitchPreferenceCompat? = findPreference("pref_auto_section")
        hasTrackPref?.isEnabled = transectHasTrack
    }

}
