package com.wmstein.transektcount

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.wmstein.transektcount.TransektCountApplication.Companion.isActivityResumed

/********************************************************************************************
 * ActivityLifecycleObserver
 * Used to restart SelectSectionActivity to show the name of a GPS-recognized section in blue
 *
 * Created for TransektCount on 2026-01-08,
 * last edited on 2026-01-15.
 */
// Check if SelectSectionActivity is currently active (resumed)
open class ActivityLifecycleObserver() : DefaultLifecycleObserver {
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

        isActivityResumed = true // Activity is now in foreground
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i("ActLifecycleObserver","22, onResume, isActivityResumed: true")
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)

        isActivityResumed = false // Activity is now in background
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i("ActLifecycleObserver","30, onPause, isActivityResumed: false")
    }

}
