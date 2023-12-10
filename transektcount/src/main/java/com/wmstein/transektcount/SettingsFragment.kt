package com.wmstein.transektcount

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

/**
 * SettingsFragment
 * Created by wmstein on 2020-04-17,
 * last edited in Java on 2020-04-17,
 * converted to Kotlin on 2023-06-28,
 * last edited on 2023-11-06.
 */
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from preferences.xml
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

}
