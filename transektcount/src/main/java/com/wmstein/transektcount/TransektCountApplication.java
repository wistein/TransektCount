package com.wmstein.transektcount;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.preference.PreferenceManager;

import java.util.Objects;

/********************************************************************
 * Handle background image, prefs and get image ids
 <p>
 * Partly derived from BeeCountApplication.java by milo on 14/05/2014.
 * Adopted by wmstein on 18.02.2016,
 * last edit on 2024-12-05
 */
public class TransektCountApplication extends Application
{
    private static final String TAG = "TransektCntAppl";
    private static SharedPreferences prefs;
    public BitmapDrawable bMapDraw;
    private Bitmap bMap;
    int width;
    int height;

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Support to debug "A resource failed to call ..." (close, dispose or similar)
        if (MyDebug.dLOG)
        {
            Log.i(TAG, "46, onCreate, StrictMode.setVmPolicy");
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build());
        }

        bMapDraw = null;
        bMap = null;
        try
        {
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
        } catch (Exception e)
        {
            if (MyDebug.dLOG) Log.e(TAG, "59, " + e);
        }
    }
    // End of onCreate()

    // bMapDraw is a pre-prepared bitmap set when the application starts up
    // or the settings are changed
    public BitmapDrawable getBackground()
    {
        if (bMapDraw == null)
        {
            return setBackground();
        }
        else
        {
            return bMapDraw;
        }
    }

    public BitmapDrawable setBackground()
    {
        bMapDraw = null;

        String backgroundPref = prefs.getString("pref_backgr", "default");
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        assert wm != null;

        if (Build.VERSION.SDK_INT >= 30) {
            final WindowMetrics metrics = wm.getCurrentWindowMetrics();
            width = metrics.getBounds().right + metrics.getBounds().left;
            height = metrics.getBounds().top +metrics.getBounds().bottom;
        }
        else
        {
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            width = size.x;
            height = size.y;
        }

        if (MyDebug.dLOG) Log.d(TAG, "100, width = " + width + ", height = " + height);

        switch (Objects.requireNonNull(backgroundPref))
        {
            case "none" ->
            {
                // black screen
                bMap = null;
                bMap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bMap.eraseColor(Color.BLACK);
            }
            case "default" ->
            {
                if ((double) height / width < 1.8)
                {
                    // normal screen
                    bMap = decodeBitmap(R.drawable.transektcount_picture_pn, width, height);
                }
                else
                {
                    // long screen
                    bMap = decodeBitmap(R.drawable.transektcount_picture_pl, width, height);
                }
            }
        }

        bMapDraw = new BitmapDrawable(this.getResources(), bMap);
        bMap = null;
        return bMapDraw;
    }

    public Bitmap decodeBitmap(int resId, int reqWidth, int reqHeight)
    {
        // First decode with inJustDecodeBounds = true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        try
        {
            return BitmapFactory.decodeResource(getResources(), resId, options);
        } catch (OutOfMemoryError e)
        {
            return null;
        }
    }

    // Scale background bitmap
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        // Raw height and width of image
        final int height1 = options.outHeight;
        final int width1 = options.outWidth;
        int inSampleSize = 1;

        if (height1 > reqHeight || width1 > reqWidth)
        {

            final int halfHeight = height1 / 2;
            final int halfWidth = width1 / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            //   height1 and width1 larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                && (halfWidth / inSampleSize) >= reqWidth)
            {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static SharedPreferences getPrefs()
    {
        return prefs;
    }

}
