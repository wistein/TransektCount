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
 * Inspired by https://stackoverflow.com/questions/3667022/
 * checking-if-an-android-application-is-running-in-the-background/13809991#13809991
 * 
 * Adopted for TransektCount by wmstein on 2025-08-14,
 * converted to Kotlin on 2026-07-16,
 * Last edited on 2026-07-18
 */
class TCLifecycleHandler : ActivityLifecycleCallbacks {
    var activityName: String = ""
    var isWelcomeActivity = false
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    // onActivityStarted counts up
    override fun onActivityStarted(activity: Activity) {
        started++ // counts any activity

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i("TCLifecycleHandler ","32, Activities started: $started times")
    }

    // After requestAllFilesAccessPermission in WelcomeActivity, WelcomeActivity resumes,
    //   so here corrects activity counts (adds additional stopped count)
    override fun onActivityResumed(activity: Activity) {
        if (activity.toString().contains("WelcomeActivity")) {
            activityName = "WelcomeActivity"
            isWelcomeActivity = true
        } else {
            activityName = "other"
            isWelcomeActivity = false
        }

        if (TransektCountApplication.isStorPermReq && isWelcomeActivity) {
            stopped++ // counts only for isStorPermReq in WelcomeActivity

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i("TCLifecycleHandler ","51, $activityName resumed, activities stopped: $stopped times")

            TransektCountApplication.isStorPermReq = false
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    // requestAllFilesAccessPermission in WelcomeActivity pauses WelcomeActivity,
    //   so here corrects activity counts (adds additional started count)
    override fun onActivityPaused(activity: Activity) {
        if (activity.toString().contains("WelcomeActivity")) {
            activityName = "WelcomeActivity"
            isWelcomeActivity = true
        } else {
            activityName = "other"
            isWelcomeActivity = false
        }

        if (TransektCountApplication.isStorPermReq && isWelcomeActivity) {
            started++ // counts only for isStorPermReq in WelcomeActivity

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i("TCLifecycleHandler ","74, $activityName paused, activities started: $started times")
        }
    }

    // onActivityStopped counts down
    override fun onActivityStopped(activity: Activity) {
        stopped++ // counts any activity

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) {
            Log.i("TCLifecycleHandler ","83, Activity stopped: $stopped times")
            Log.i("TCLifecycleHandler ","84, Application is visible: $isApplicationVisible")
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
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
