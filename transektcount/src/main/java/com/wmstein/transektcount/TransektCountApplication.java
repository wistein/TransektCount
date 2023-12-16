package com.wmstein.transektcount;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.util.Objects;

import androidx.preference.PreferenceManager;

/**********************************************************
 * Handle background image, prefs and get image ids
 <p>
 * Partly based on BeeCountApplication.java by milo on 14/05/2014.
 * Adopted by wmstein on 18.02.2016,
 * last edit on 2023-12-15
 */
public class TransektCountApplication extends Application
{
    private static final String TAG = "TransektCountAppl";
    private static SharedPreferences prefs;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    public BitmapDrawable bMapDraw;
    private Bitmap bMap;
    int width;
    int height;
    int resID;

    @Override
    public void onCreate()
    {
        super.onCreate();
        TransektCountApplication.context = getApplicationContext();
        bMapDraw = null;
        bMap = null;
        try
        {
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
        } catch (Exception e)
        {
            if (MyDebug.LOG) Log.e(TAG, "51, " + e);
        }
    }

    // Provide access to Application Context
    public static Context getAppContext()
    {
        return TransektCountApplication.context;
    }

    // The idea here is to keep bMapDraw around as a pre-prepared bitmap, only setting it up
    // when the user's settings change or when the application starts up.
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
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
//        if (MyDebug.LOG) Log.d(TAG, "width = " + width + ", height = " + height);

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
                // portrait
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
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth)
            {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public Bitmap decodeBitmap(int resId, int reqWidth, int reqHeight)
    {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(getResources(), resId, options);
    }

    public static SharedPreferences getPrefs()
    {
        return prefs;
    }


    // Get resource ID from resource name
    @SuppressLint("DiscouragedApi")
    public int getResID(String rName) // non-static method
    {
        try
        {
            resID = getAppContext().getResources().getIdentifier(rName, "drawable",
                getAppContext().getPackageName());
            return resID;
        } catch (Exception e)
        {
            return 0;
        }
    }

}
