package com.wmstein.transektcount

/***************************************************************************************************
 * Copyright © 2016-2026, Wilhelm Stein, Bonn, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use any of its files except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************************************
 *
 * ActivityLifecycleObserver
 * Check if an Activity is currently active (resumed)
 * Used to restart SelectSectionActivity to show the name of a GPS-recognized section in blue
 *
 * Created for TransektCount on 2026-01-08,
 * last edited on 2026-10-21.
 **************************************************************************************************/

import android.util.Log

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

import com.wmstein.transektcount.TransektCountApplication.Companion.isSelectSectionActivityResumed

open class ActivityLifecycleObserver : DefaultLifecycleObserver {
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

        // Check if SelectSectionActivity is currently active (resumed)
        isSelectSectionActivityResumed = true // Activity is now in foreground
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i("ActLifecycleObserver","25, onResume, isSelectSectionActivityResumed: true")
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)

        isSelectSectionActivityResumed = false // Activity is now in background
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i("ActLifecycleObserver","33, onPause, isSelectSectionActivityResumed: false")
    }

}
