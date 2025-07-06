package com.wmstein.transektcount

import android.app.Application
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.WindowManager
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.preference.PreferenceManager

/********************************************************************
 * Handle background image and prefs
 * Partly derived from BeeCountApplication.java by milo on 14/05/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016,
 * converted to Kotlin on 2024-12-09,
 * last edit on 2025-06-27
 */
class TransektCountApplication : Application() {
    var bMapDraw: BitmapDrawable? = null
    var width: Int = 0
    var height: Int = 0

    override fun onCreate() {
        super.onCreate()

        // Support to debug "A resource failed to call ..." (close, dispose or similar)
        if (MyDebug.DLOG) {
            Log.i(TAG, "42, onCreate, StrictMode.setVmPolicy")
            StrictMode.setVmPolicy(
                VmPolicy.Builder(StrictMode.getVmPolicy())
                    .detectLeakedClosableObjects()
                    .build()
            )
        }

        try {
            prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        } catch (e: Exception) {
            if (MyDebug.DLOG) Log.e(TAG, "53, $e")
        }
    }
    // End of onCreate()

    // bMapDraw is a pre-prepared bitmap read by WelcomeActivity, SelectSectionActivity
    //   and CountingActivity
    fun setBackgr(): BitmapDrawable {
        bMapDraw = null

        val backgroundPref: String = prefs!!.getString("pref_backgr", "default")!!
        if (MyDebug.DLOG) Log.i(TAG, "64, Backgr.: $backgroundPref")

        val wm = checkNotNull(this.getSystemService(WINDOW_SERVICE) as WindowManager)
        if (Build.VERSION.SDK_INT >= 30) {
            val metrics = wm.currentWindowMetrics
            width = metrics.bounds.right + metrics.bounds.left
            height = metrics.bounds.top + metrics.bounds.bottom
        } else {
            @Suppress("DEPRECATION")
            val display = wm.defaultDisplay // deprecated in 30
            val size = Point()
            @Suppress("DEPRECATION")
            display.getSize(size) // deprecated in 30
            width = size.x
            height = size.y
        }
        if (MyDebug.DLOG) Log.d(TAG, "80, width = $width, height = $height")

        var bMap: Bitmap?
        if (backgroundPref == "none") {
            // black screen
            bMap = createBitmap(width, height, Bitmap.Config.RGB_565)
            bMap.eraseColor(Color.BLACK)
        } else if (backgroundPref == "grey") {
            bMap = createBitmap(width, height, Bitmap.Config.RGB_565)
            bMap.eraseColor(-0xddddde) // dark grey
        } else {
            if (height.toDouble() / width < 1.8) {
                // normal screen
                bMap = decodeBitmap(R.drawable.transektcount_picture_pn, width, height)
            } else {
                // long screen
                bMap = decodeBitmap(R.drawable.transektcount_picture_pl, width, height)
            }
        }

        bMapDraw = bMap?.toDrawable(this.resources)
        return bMapDraw!!
    }

    fun decodeBitmap(resId: Int, reqWidth: Int, reqHeight: Int): Bitmap? {
        // First decode with inJustDecodeBounds = true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(resources, resId, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        try {
            return BitmapFactory.decodeResource(resources, resId, options)
        } catch (_: OutOfMemoryError) {
            return null
        }
    }

    companion object {
        private const val TAG = "TransektCntAppl"
        private var prefs: SharedPreferences? = null

        // Scale background bitmap
        private fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int,
        ): Int {
            // Raw height and width of image
            val height1 = options.outHeight
            val width1 = options.outWidth
            var inSampleSize = 1

            if (height1 > reqHeight || width1 > reqWidth) {
                val halfHeight = height1 / 2
                val halfWidth = width1 / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                //   height1 and width1 larger than the requested height and width.
                while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth
                ) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }

        @JvmStatic
        fun getPrefs(): SharedPreferences {
            return prefs!!
        }
    }

}
