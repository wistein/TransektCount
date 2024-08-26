package com.wmstein.transektcount;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
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
import com.wmstein.transektcount.widgets.EditMetaHeadWidget;
import com.wmstein.transektcount.widgets.EditMetaWidget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;

/***************************************************************
 * EditMetaActivity collects meta info for a transect inspection
 * Created by wmstein on 2016-03-31,
 * last edited on 2024-06-30
 */
public class EditMetaActivity extends AppCompatActivity
{

    private Head head;
    private Meta meta;

    private Calendar pdate, ptime;

    private HeadDataSource headDataSource;
    private MetaDataSource metaDataSource;

    // preferences
    private final SharedPreferences prefs = TransektCountApplication.getPrefs();
    private boolean brightPref;
    
    private LinearLayout head_area;
    private TextView sDate, sTime, eTime;

    private EditMetaHeadWidget ehw;
    private EditMetaWidget emw;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_head);

        head_area = findViewById(R.id.edit_head);

        //    private static final String TAG = "EditMetaAct";  // potentially for debugging
        TransektCountApplication transektCount = (TransektCountApplication) getApplication();
        brightPref = prefs.getBoolean("pref_bright", true);

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        Bitmap bMap = transektCount.decodeBitmap(R.drawable.edbackground,
            transektCount.width, transektCount.height);
        ScrollView editHead_screen = findViewById(R.id.editHeadScreen);
        BitmapDrawable bg = new BitmapDrawable(editHead_screen.getResources(), bMap);
        editHead_screen.setBackground(bg);

        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.editHeadTitle));

        headDataSource = TransektCountApplication.getHeadDS();
        metaDataSource = TransektCountApplication.getMetaDS();
    }
    
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onResume()
    {
        super.onResume();

        brightPref = prefs.getBoolean("pref_bright", true);

        // build the Edit Meta Data screen
        // clear existing view
        head_area.removeAllViews();

        //setup data sources
        headDataSource.open();
        metaDataSource.open();

        // load head and meta data
        head = headDataSource.getHead();
        meta = metaDataSource.getMeta();

        // display the editable head data
        ehw = new EditMetaHeadWidget(this, null);
        ehw.setWidgetNo(getString(R.string.transectnumber));
        ehw.setWidgetNo1(head.transect_no);
        ehw.setWidgetName(getString(R.string.inspector));
        ehw.setWidgetName1(head.inspector_name);
        head_area.addView(ehw);

        // display the editable meta data
        emw = new EditMetaWidget(this, null);
        emw.setWidgetTemp1(getString(R.string.temperature));
        emw.setWidgetWind1(getString(R.string.wind));
        emw.setWidgetClouds1(getString(R.string.clouds));

        emw.setWidgetStartTemp2(meta.temps);
        emw.setWidgetEndTemp2(meta.tempe);
        emw.setWidgetStartWind2(meta.winds);
        emw.setWidgetEndWind2(meta.winde);
        emw.setWidgetStartClouds2(meta.clouds);
        emw.setWidgetEndClouds2(meta.cloude);

        emw.setWidgetDate1(getString(R.string.date));
        emw.setWidgetDate2(meta.date);
        emw.setWidgetSTime1(getString(R.string.starttm));
        emw.setWidgetSTime2(meta.start_tm);
        emw.setWidgetETime1(getString(R.string.endtm));
        emw.setWidgetETime2(meta.end_tm);
        emw.setWidgetNote1(getString(R.string.note));
        emw.setWidgetNote2(meta.note);
        head_area.addView(emw);

        // check for focus
        String newTransectNo = head.transect_no;
        if (isNotEmpty(newTransectNo))
        {
            emw.requestFocus();
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
    // end of onResume()

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

        // Save meta data with plausi
        meta.temps = emw.getWidgetTemps();
        meta.tempe = emw.getWidgetTempe();
        if (meta.temps > 50 || meta.temps < 0 || meta.tempe > 50 || meta.tempe < 0)
        {
            Snackbar sB = Snackbar.make(emw, getString(R.string.valTemp), Snackbar.LENGTH_LONG);
            TextView tv = sB.getView().findViewById(R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
            sB.show();
            return false;
        }
        meta.winds = emw.getWidgetWinds();
        meta.winde = emw.getWidgetWinde();
        if (meta.winds > 4 || meta.winds < 0 || meta.winde > 4 || meta.winde < 0)
        {
            Snackbar sB = Snackbar.make(emw, getString(R.string.valWind), Snackbar.LENGTH_LONG);
            TextView tv = sB.getView().findViewById(R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
            sB.show();
            return false;
        }
        meta.clouds = emw.getWidgetClouds();
        meta.cloude = emw.getWidgetCloude();
        if (meta.clouds > 100 || meta.clouds < 0 || meta.cloude > 100 || meta.cloude < 0)
        {
            Snackbar sB = Snackbar.make(emw, getString(R.string.valClouds), Snackbar.LENGTH_LONG);
            TextView tv = sB.getView().findViewById(R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
            sB.show();
            return false;
        }

        meta.date = emw.getWidgetDate();
        meta.start_tm = emw.getWidgetSTime();
        meta.end_tm = emw.getWidgetETime();
        meta.note = emw.getWidgetNote();

        metaDataSource.saveMeta(meta);
        return true;
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

    /**
     * Following functions are taken from the Apache commons-lang3-3.4 library
     * licensed under Apache License Version 2.0, January 2004
     <p>
     * Checks if a CharSequence is not empty ("") and not null.
     <p>
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
     <p>
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