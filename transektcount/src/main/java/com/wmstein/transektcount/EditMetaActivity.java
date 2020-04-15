package com.wmstein.transektcount;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.wmstein.transektcount.database.Head;
import com.wmstein.transektcount.database.HeadDataSource;
import com.wmstein.transektcount.database.Meta;
import com.wmstein.transektcount.database.MetaDataSource;
import com.wmstein.transektcount.widgets.EditHeadWidget;
import com.wmstein.transektcount.widgets.EditMetaWidget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/***************************************************************
 * EditMetaActivity collects meta info for a transect inspection
 * Created by wmstein on 2016-03-31,
 * last edited on 2020-04-09
 */
public class EditMetaActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
//    private static final String TAG = "transektcountEditMetaAct";
    private static TransektCountApplication transektCount;
    SharedPreferences prefs;

    private Head head;
    private Meta meta;
    private Bitmap bMap;
    private BitmapDrawable bg;

    private Calendar pdate, ptime;

    private HeadDataSource headDataSource;
    private MetaDataSource metaDataSource;

    // preferences
    private boolean screenOrientL; // option for screen orientation

    private LinearLayout head_area;
    private TextView sDate, sTime, eTime;

    private EditHeadWidget ehw;
    private EditMetaWidget etw;

    @Override
    @SuppressLint({"LongLogTag", "SourceLockedOrientationActivity"})
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_head);

        head_area = findViewById(R.id.edit_head);

        transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        boolean brightPref = prefs.getBoolean("pref_bright", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        bMap = transektCount.decodeBitmap(R.drawable.kbackground, transektCount.width, transektCount.height);
        ScrollView editHead_screen = findViewById(R.id.editHeadScreen);
        bg = new BitmapDrawable(editHead_screen.getResources(), bMap);
        editHead_screen.setBackground(bg);

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(getString(R.string.editHeadTitle));
    }
    
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onResume()
    {
        super.onResume();

        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        // build the Edit Meta Data screen
        // clear existing view
        head_area.removeAllViews();

        //setup data sources
        headDataSource = new HeadDataSource(this);
        headDataSource.open();
        metaDataSource = new MetaDataSource(this);
        metaDataSource.open();

        // load head and meta data
        head = headDataSource.getHead();
        meta = metaDataSource.getMeta();

        // display the editable head data
        ehw = new EditHeadWidget(this, null);
        ehw.setWidgetNo(getString(R.string.transectnumber));
        ehw.setWidgetNo1(head.transect_no);
        ehw.setWidgetName(getString(R.string.inspector));
        ehw.setWidgetName1(head.inspector_name);
        head_area.addView(ehw);

        // display the editable meta data
        etw = new EditMetaWidget(this, null);
        etw.setWidgetTemp1(getString(R.string.temperature));
        etw.setWidgetTemp2(meta.tempe);
        etw.setWidgetWind1(getString(R.string.wind));
        etw.setWidgetWind2(meta.wind);
        etw.setWidgetClouds1(getString(R.string.clouds));
        etw.setWidgetClouds2(meta.clouds);
        etw.setWidgetDate1(getString(R.string.date));
        etw.setWidgetDate2(meta.date);
        etw.setWidgetSTime1(getString(R.string.starttm));
        etw.setWidgetSTime2(meta.start_tm);
        etw.setWidgetETime1(getString(R.string.endtm));
        etw.setWidgetETime2(meta.end_tm);
        head_area.addView(etw);

        // check for focus
        String newTransectNo = head.transect_no;
        if (isNotEmpty(newTransectNo))
        {
            etw.requestFocus();
        }
        else
        {
            ehw.requestFocus();
        }

        pdate = Calendar.getInstance();
        ptime = Calendar.getInstance();

        sDate = this.findViewById(R.id.widgetDate2);
        sTime = this.findViewById(R.id.widgetSTime2);
        eTime = this.findViewById(R.id.widgetETime2);

        // get current date by click
        sDate.setOnClickListener(v -> {
            Date date = new Date();
            sDate.setText(getformDate(date));
        });

        // get date picker result
        final DatePickerDialog.OnDateSetListener dpd = (view, year, monthOfYear, dayOfMonth) -> {
            pdate.set(Calendar.YEAR, year);
            pdate.set(Calendar.MONTH, monthOfYear);
            pdate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            Date date = pdate.getTime();
            sDate.setText(getformDate(date));
        };

        // select date by long click
        sDate.setOnLongClickListener(v -> {
            new DatePickerDialog(EditMetaActivity.this, dpd,
                pdate.get(Calendar.YEAR),
                pdate.get(Calendar.MONTH),
                pdate.get(Calendar.DAY_OF_MONTH)).show();
            return true;
        });

        // get current start time
        sTime.setOnClickListener(v -> {
            Date date = new Date();
            sTime.setText(getformTime(date));
        });

        // get start time picker result
        final TimePickerDialog.OnTimeSetListener stpd = (view, hourOfDay, minute) -> {
            ptime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            ptime.set(Calendar.MINUTE, minute);
            Date date = ptime.getTime();
            sTime.setText(getformTime(date));
        };

        // select start time
        sTime.setOnLongClickListener(v -> {
            new TimePickerDialog(EditMetaActivity.this, stpd,
                ptime.get(Calendar.HOUR_OF_DAY),
                ptime.get(Calendar.MINUTE),
                true).show();
            return true;
        });

        // get current end time
        eTime.setOnClickListener(v -> {
            Date date = new Date();
            eTime.setText(getformTime(date));
        });

        // get start time picker result
        final TimePickerDialog.OnTimeSetListener etpd = (view, hourOfDay, minute) -> {
            ptime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            ptime.set(Calendar.MINUTE, minute);
            Date date = ptime.getTime();
            eTime.setText(getformTime(date));
        };

        // select end time
        eTime.setOnLongClickListener(v -> {
            new TimePickerDialog(EditMetaActivity.this, etpd,
                ptime.get(Calendar.HOUR_OF_DAY),
                ptime.get(Calendar.MINUTE),
                true).show();
            return true;
        });
    }

    // formatted date
    public static String getformDate(Date date)
    {
        DateFormat dform;
        String lng = Locale.getDefault().toString().substring(0, 2);

        if (lng.equals("de"))
        {
            dform = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
        }
        else
        {
            dform = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        }
        return dform.format(date);
    }

    // date for start_tm and end_tm
    public static String getformTime(Date date)
    {
        DateFormat dform = new SimpleDateFormat("HH:mm", Locale.US);
        return dform.format(date);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // close the data sources
        headDataSource.close();
        metaDataSource.close();
    }

    /***************/
    public void saveAndExit(View view)
    {
        if (saveData())
            super.finish();
    }

    public boolean saveData()
    {
        // Save head data
        head.transect_no = ehw.getWidgetNo1();
        head.inspector_name = ehw.getWidgetName1();

        headDataSource.saveHead(head);

        // Save meta data
        meta.tempe = etw.getWidgetTemp();
        if (meta.tempe > 50 || meta.tempe < 0)
        {
            Snackbar sB = Snackbar.make(etw, Html.fromHtml("<font color=\"#ff0000\"><b>" +  getString(R.string.valTemp) + "</font></b>"), Snackbar.LENGTH_LONG);
            TextView tv = sB.getView().findViewById(R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            sB.show();
            return false;
        }
        meta.wind = etw.getWidgetWind();
        if (meta.wind > 4 || meta.wind < 0)
        {
            Snackbar sB = Snackbar.make(etw, Html.fromHtml("<font color=\"#ff0000\"><b>" +  getString(R.string.valWind) + "</font></b>"), Snackbar.LENGTH_LONG);
            TextView tv = sB.getView().findViewById(R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            sB.show();
            return false;
        }
        meta.clouds = etw.getWidgetClouds();
        if (meta.clouds > 100 || meta.clouds < 0)
        {
            Snackbar sB = Snackbar.make(etw, Html.fromHtml("<font color=\"#ff0000\"><b>" +  getString(R.string.valClouds) + "</font></b>"), Snackbar.LENGTH_LONG);
            TextView tv = sB.getView().findViewById(R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            sB.show();
            return false;
        }
        meta.date = etw.getWidgetDate();
        meta.start_tm = etw.getWidgetSTime();
        meta.end_tm = etw.getWidgetETime();

        metaDataSource.saveMeta(meta);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_meta, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.menuSaveExit)
        {
            if (saveData())
                super.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView editHead_screen = findViewById(R.id.editHeadScreen);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        bMap = transektCount.decodeBitmap(R.drawable.kbackground, transektCount.width, transektCount.height);
        editHead_screen.setBackground(null);
        bg = new BitmapDrawable(editHead_screen.getResources(), bMap);
        editHead_screen.setBackground(bg);
    }

    /**
     * Following functions are taken from the Apache commons-lang3-3.4 library
     * licensed under Apache License Version 2.0, January 2004
     * 
     * Checks if a CharSequence is not empty ("") and not null.
     * 
     * isNotEmpty(null)      = false
     * isNotEmpty("")        = false
     * isNotEmpty(" ")       = true
     * isNotEmpty("bob")     = true
     * isNotEmpty("  bob  ") = true
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null
     */
    public static boolean isNotEmpty(final CharSequence cs)
    {
        return !isEmpty(cs);
    }

    /**
     * Checks if a CharSequence is empty ("") or null.
     * 
     * isEmpty(null)      = true
     * isEmpty("")        = true
     * isEmpty(" ")       = false
     * isEmpty("bob")     = false
     * isEmpty("  bob  ") = false
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(final CharSequence cs)
    {
        return cs == null || cs.length() == 0;
    }

}