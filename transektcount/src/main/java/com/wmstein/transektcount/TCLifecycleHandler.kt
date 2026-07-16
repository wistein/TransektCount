package com.wmstein.transektcount

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log

/*********************************************************************************************
 * TCLifecycleHandler controls the state of all activities and check if your application
 * is in foreground or background.
 * Needed to stop location service when app is finished and
 * takes care of the special activity handling of the permit dialog of MANAGE_EXTERNAL_STORAGE.
 * 
 * Based on [...](https://stackoverflow.com/questions/3667022/)
 * checking-if-an-android-application-is-running-in-the-background/13809991#13809991
 * 
 * Adopted for TransektCount by wmstein on 2025-08-14,
 * converted to Kotlin on 2026-07-16,
 * Last edited on 2026-07-16
 */
class TCLifecycleHandler : ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) Log.i(
            "TCLifecycleHandler ","24, Activity created: $activity $started"
        )
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
        // Reduce activity counts for startActivity(stoIntent) from requestAllFilesAccessPermission
        // in WelcomeActivity
        val activ: String = activity.toString()
        var isWelcomeActivity = false
        if (activ.contains("WelcomeActivity"))
            isWelcomeActivity = true

        if(TransektCountApplication.isStorPermReq && isWelcomeActivity) {
            stopped++
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) Log.i(
                "TCLifecycleHandler ","42, Activity resumed, stopped: $activ $stopped"
            )
            TransektCountApplication.isStorPermReq = false
        }
    }

    override fun onActivityPaused(activity: Activity) {
        // Increase activity counts for startActivity(stoIntent) from requestAllFilesAccessPermission
        // in WelcomeActivity
        val activ: String = activity.toString()
        var isWelcomeActivity = false
        if (activ.contains("WelcomeActivity"))
            isWelcomeActivity = true

        if(TransektCountApplication.isStorPermReq && isWelcomeActivity) {
            started++
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) Log.i(
                "TCLifecycleHandler ","59, Activity paused, started: $activ $started"
            )
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityStarted(activity: Activity) {
        started++
    }

    override fun onActivityStopped(activity: Activity) {
        stopped++
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) {
            Log.i("TCLifecycleHandler ","74, Activity stopped: $activity $stopped")
            Log.i("TCLifecycleHandler ","75, Application is visible: " + (started > stopped))
        }
    }

    companion object {
        // Increment/decrement the variables 'started' and 'stopped' by all activities
        private var started = 0
        private var stopped = 0

        @JvmStatic
        val isApplicationVisible: Boolean
            // Static function to check if the application is in foreground or background
            get() = started > stopped
    }
}
