package com.wmstein.transektcount;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

/***************************************************************************************
 * TCLifecycleHandler controls the state of all activities and check if your application
 * is in foreground or background.
 * Needed to stop location service when app is finished.
 * <p>
 * Based on <a href="https://stackoverflow.com/questions/3667022/">...</a>
 *   checking-if-an-android-application-is-running-in-the-background/13809991#13809991
 * <p>
 * Adopted for TransektCount by wmstein on 2025-08-14,
 * Last edited on 2025-12-29
 */
public class TCLifecycleHandler implements Application.ActivityLifecycleCallbacks {
    // Increment/decrement the variables 'started' and 'stopped' by all activities
    private static int started;
    private static int stopped;

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        ++started;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        ++stopped;
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d("TransektCount", " TCLifecycleHandler: Application is visible: " + (started > stopped));
    }

    // Static function to check if the application is in foreground or background
    public static boolean isApplicationVisible() {
        return started > stopped;
    }

}
