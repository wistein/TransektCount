package com.wmstein.transektcount;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/***************************************************************
 * EditMetaActivity collects meta info for a transect inspection
 * Created by wmstein on 2016-03-31,
 * last edited on 2025-12-29
 */
public class EditMetaActivity extends AppCompatActivity {
    private final static String TAG = "EditMetaAct";

    // Data from DB tables
    private Head head;
    private Meta meta;

    private HeadDataSource headDataSource;
    private MetaDataSource metaDataSource;

    private Calendar pdate, ptime;

    // Preferences
    private final SharedPreferences prefs = TransektCountApplication.getPrefs();
    private boolean awakePref;

    private LinearLayout metaArea;
    private TextView sDate, sTime, eTime;

    private EditMetaHeadWidget ehw;
    private EditMetaWidget emw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "71, onCreate");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            EdgeToEdge.enable(this);
        }
        setContentView(R.layout.activity_edit_meta);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editHeadScreen),
                (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    mlp.topMargin = insets.top;
                    mlp.bottomMargin = insets.bottom;
                    mlp.leftMargin = insets.left;
                    mlp.rightMargin = insets.right;
                    v.setLayoutParams(mlp);
                    return WindowInsetsCompat.CONSUMED;
                });

        // Option for full bright screen
        boolean brightPref = prefs.getBoolean("pref_bright", true);
        awakePref = prefs.getBoolean("pref_awake", true);

        // Set full brightness of screen
        if (brightPref) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        if (awakePref)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        metaArea = findViewById(R.id.meta_area);

        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.editHeadTitle));

        headDataSource = new HeadDataSource(this);
        metaDataSource = new MetaDataSource(this);

        // New onBackPressed logic
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.d(TAG, "117, handleOnBackPressed");
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
    // End of onCreate()

    @Override
    protected void onResume() {
        super.onResume();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "130, onResume");

        // Build the Edit Meta Data screen
        // Clear existing view
        metaArea.removeAllViews();

        // Setup data sources
        headDataSource.open();
        metaDataSource.open();

        // Load head and meta data
        head = headDataSource.getHead();
        meta = metaDataSource.getMeta();

        // Display the editable head data
        ehw = new EditMetaHeadWidget(this, null);
        ehw.setWidgetNo(getString(R.string.transectnumber));
        ehw.setWidgetNo1(head.transect_no);
        ehw.setWidgetName(getString(R.string.inspector));
        ehw.setWidgetName1(head.inspector_name);
        metaArea.addView(ehw);

        // Display the editable meta data
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
        metaArea.addView(emw);

        // Check for focus
        String newTransectNo = head.transect_no;
        if (isNotEmpty(newTransectNo)) {
            emw.requestFocus();
        } else {
            ehw.requestFocus();
        }

        pdate = Calendar.getInstance();
        ptime = Calendar.getInstance();

        sDate = this.findViewById(R.id.widgetDate2);
        sTime = this.findViewById(R.id.widgetSTime2);
        eTime = this.findViewById(R.id.widgetETime2);

        // Get current date by click
        sDate.setOnClickListener(v -> {
            Date date = new Date();
            sDate.setText(getformDate(date));
        });

        // Get date picker result
        final DatePickerDialog.OnDateSetListener dpd = (view, year, monthOfYear, dayOfMonth) -> {
            pdate.set(Calendar.YEAR, year);
            pdate.set(Calendar.MONTH, monthOfYear);
            pdate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            Date date = pdate.getTime();
            sDate.setText(getformDate(date));
        };

        // Select date by long click
        sDate.setOnLongClickListener(v -> {
            new DatePickerDialog(EditMetaActivity.this, dpd,
                    pdate.get(Calendar.YEAR),
                    pdate.get(Calendar.MONTH),
                    pdate.get(Calendar.DAY_OF_MONTH)).show();
            return true;
        });

        // Get current start time
        sTime.setOnClickListener(v -> {
            Date date = new Date();
            sTime.setText(getformTime(date));
        });

        // Get start time picker result
        final TimePickerDialog.OnTimeSetListener stpd = (view, hourOfDay, minute) -> {
            ptime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            ptime.set(Calendar.MINUTE, minute);
            Date date = ptime.getTime();
            sTime.setText(getformTime(date));
        };

        // Select start time
        sTime.setOnLongClickListener(v -> {
            new TimePickerDialog(EditMetaActivity.this, stpd,
                    ptime.get(Calendar.HOUR_OF_DAY),
                    ptime.get(Calendar.MINUTE),
                    true).show();
            return true;
        });

        // Get current end time
        eTime.setOnClickListener(v -> {
            Date date = new Date();
            eTime.setText(getformTime(date));
        });

        // Get start time picker result
        final TimePickerDialog.OnTimeSetListener etpd = (view, hourOfDay, minute) -> {
            ptime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            ptime.set(Calendar.MINUTE, minute);
            Date date = ptime.getTime();
            eTime.setText(getformTime(date));
        };

        // Select end time
        eTime.setOnLongClickListener(v -> {
            new TimePickerDialog(EditMetaActivity.this, etpd,
                    ptime.get(Calendar.HOUR_OF_DAY),
                    ptime.get(Calendar.MINUTE),
                    true).show();
            return true;
        });
    }
    // End of onResume()

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_meta, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();
        if (id == android.R.id.home) // back button in actionBar
        {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "276, MenuItem home");
            finish();
            return true;
        }

        if (id == R.id.menuSaveExit) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "283, MenuItem saveExit");
            if (saveData())
                finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "296, onPause");

        headDataSource.close();
        metaDataSource.close();

        sDate.setOnClickListener(null);
        sDate.setOnLongClickListener(null);
        sTime.setOnClickListener(null);
        sTime.setOnLongClickListener(null);
        eTime.setOnClickListener(null);
        eTime.setOnLongClickListener(null);

        metaArea.clearFocus();
        metaArea.removeAllViews();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "317, onStop");

        if (awakePref) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        metaArea = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "331, onDestroy");
    }

    public boolean saveData() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "336, saveData");
        // Save head data
        head.transect_no = ehw.getWidgetNo1();
        head.inspector_name = ehw.getWidgetName1();

        headDataSource.saveHead(head);

        String mesg;

        // Save meta data with plausi
        meta.temps = emw.getWidgetTemps();
        meta.tempe = emw.getWidgetTempe();
        if (meta.temps > 50 || meta.temps < 0 || meta.tempe > 50 || meta.tempe < 0) {
            mesg = getString(R.string.valTemp);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
            return false;
        }

        meta.winds = emw.getWidgetWinds();
        meta.winde = emw.getWidgetWinde();
        if (meta.winds > 4 || meta.winds < 0 || meta.winde > 4 || meta.winde < 0) {
            mesg = getString(R.string.valWind);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
            return false;
        }

        meta.clouds = emw.getWidgetClouds();
        meta.cloude = emw.getWidgetCloude();
        if (meta.clouds > 100 || meta.clouds < 0 || meta.cloude > 100 || meta.cloude < 0) {
            mesg = getString(R.string.valClouds);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
            return false;
        }

        meta.date = emw.getWidgetDate();
        meta.start_tm = emw.getWidgetSTime();
        meta.end_tm = emw.getWidgetETime();
        meta.note = emw.getWidgetNote();

        metaDataSource.saveMeta(meta);
        return true;
    }

    // Formatted date
    public String getformDate(Date date) {
        DateFormat dform;
        String lng = Locale.getDefault().toString().substring(0, 2);

        if (lng.equals("de")) {
            dform = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
        } else {
            dform = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        }
        return dform.format(date);
    }

    // Date for start_tm and end_tm
    public String getformTime(Date date) {
        DateFormat dform = new SimpleDateFormat("HH:mm", Locale.US);
        return dform.format(date);
    }

    /**
     * Following functions are derived from the Apache commons-lang3-3.4 library
     * licensed under Apache License Version 2.0, January 2004
     * <p>
     * Checks if a CharSequence is not empty ("") and not null.
     * <p>
     * isNotEmpty(null)      = false
     * isNotEmpty("")        = false
     * isNotEmpty(" ")       = true
     * isNotEmpty("bob")     = true
     * isNotEmpty("  bob  ") = true
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }

    /**
     * Checks if a CharSequence is empty ("") or null.
     * <p>
     * isEmpty(null)      = true
     * isEmpty("")        = true
     * isEmpty(" ")       = false
     * isEmpty("bob")     = false
     * isEmpty("  bob  ") = false
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(final CharSequence cs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // API level 35
            return cs == null || cs.isEmpty();
        else
            return cs == null || cs.length() == 0; // needed for older Android versions
    }

}
