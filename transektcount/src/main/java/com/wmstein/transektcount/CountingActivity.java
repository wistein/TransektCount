package com.wmstein.transektcount;

import static android.os.Build.VERSION.SDK_INT;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.CursorIndexOutOfBoundsException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wmstein.transektcount.database.Alert;
import com.wmstein.transektcount.database.AlertDataSource;
import com.wmstein.transektcount.database.Count;
import com.wmstein.transektcount.database.CountDataSource;
import com.wmstein.transektcount.database.Head;
import com.wmstein.transektcount.database.HeadDataSource;
import com.wmstein.transektcount.database.Section;
import com.wmstein.transektcount.database.SectionDataSource;
import com.wmstein.transektcount.widgets.CountingWidgetExt;
import com.wmstein.transektcount.widgets.CountingWidgetHead1;
import com.wmstein.transektcount.widgets.CountingWidgetHead2;
import com.wmstein.transektcount.widgets.CountingWidgetHead3;
import com.wmstein.transektcount.widgets.CountingWidgetInt;
import com.wmstein.transektcount.widgets.CountingWidgetLhExt;
import com.wmstein.transektcount.widgets.CountingWidgetLhInt;
import com.wmstein.transektcount.widgets.NotesWidget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/******************************************************************************
 * CountingActivity is the central activity of TransektCount and is called from
 *   SelectSectionActivity for the selected section.
 *   It does the actual counting with 12 counters,
 *   checks for alerts,
 *   calls AddSpeciesActivity, DelSpeciesActivity, EditSectionListActivity,
 *   CountOptionsActivity and DummyActivity,
 *   clones a section,
 *   switches screen off when device is pocketed
 *   and allows taking pictures and sending notes.
 *   <p>
 *   CountingActivity uses CountingWidget*.kt, NotesWidget.kt and activity_counting*.xml
 * <p>
 * Basic counting functions created by milo for BeeCount on 2014-05-05.
 * Adopted, modified and enhanced for TransektCount by wmstein since 2016-02-18,
 * last edited on 2025-06-28
 */
public class CountingActivity
        extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener, SensorEventListener {
    private static final String TAG = "CountAct";
    private int sectionId;  // section ID
    private int iid = 1;    // count ID
    private int itemPosition = 0; // 0 = 1. position of selected species in array for Spinner

    private LinearLayout countsFieldHeadArea1; // headline internal/external
    private LinearLayout countsFieldArea1;     // internal counts field
    private LinearLayout countsFieldHeadArea2; // headline external
    private LinearLayout countsFieldArea2;     // external counts field
    private LinearLayout speciesNotesArea;     // species notes line
    private LinearLayout alertNotesArea;       // alert notes line

    // Proximity sensor handling for screen on/off
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mProximityWakeLock;
    private SensorManager mSensorManager;
    private Sensor mProximity;

    // Preferences
    private SharedPreferences prefs;
    private boolean awakePref;
    private boolean brightPref;
    private String sortPref;
    private boolean fontPref;
    private boolean lhandPref; // true for lefthand mode of counting screen
    private String alertSound;
    private boolean alertSoundPref;
    private String buttonSound;
    private String buttonSoundMinus;
    private boolean buttonSoundPref;
    private boolean buttonVibPref;
    private String specCode = "";
    private String proxSensorPref;
    private double sensorSensitivity = 0.0;

    // Data
    private Count count;     // record in SQLite counts table for a counted species
    private Section section; // record in SQLite sections table
    private List<Alert> alerts;
    private Spinner spinner; // species selector
    private int oldCounter;
    private int newCounter;

    // CountingWidgets
    private List<CountingWidgetInt> countingWidget_i;
    private List<CountingWidgetExt> countingWidget_e;
    private List<CountingWidgetLhInt> countingWidgetLH_i;
    private List<CountingWidgetLhExt> countingWidgetLH_e;

    // Data sources
    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;
    private AlertDataSource alertDataSource;
    private HeadDataSource headDataSource;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private Ringtone r;
    private Vibrator vibrator;
    private String mesg = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (MyDebug.DLOG) Log.d(TAG, "158, onCreate");

        TransektCountApplication transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        setPrefVariables(); // set all stored preferences into their variables

        // get values from calling activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            sectionId = extras.getInt("section_id");
            itemPosition = extras.getInt("item_position");
        } else {
            sectionId = 1;
            itemPosition = 0;
        }

        sectionDataSource = new SectionDataSource(this);
        countDataSource = new CountDataSource(this);
        alertDataSource = new AlertDataSource(this);
        headDataSource = new HeadDataSource(this);

        // Set full brightness of screen
        if (brightPref) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
            EdgeToEdge.enable(this);

        // Distinguish between left-/ right-handed counting page layout
        if (lhandPref) {
            setContentView(R.layout.activity_counting_lh);
            LinearLayout counting_screen = findViewById(R.id.countingScreenLH);
            counting_screen.setBackground(transektCount.setBackgr());
            countsFieldHeadArea1 = findViewById(R.id.countsFieldHead1LH);
            countsFieldArea1 = findViewById(R.id.countsField1LH);
            countsFieldHeadArea2 = findViewById(R.id.countsFieldHead2LH);
            countsFieldArea2 = findViewById(R.id.countsField2LH);
            speciesNotesArea = findViewById(R.id.speciesNotesLH);
            alertNotesArea = findViewById(R.id.alertRemarkLH);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.countingScreenLH),
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
        } else {
            setContentView(R.layout.activity_counting);
            LinearLayout counting_screen = findViewById(R.id.countingScreen);
            counting_screen.setBackground(transektCount.setBackgr());
            countsFieldHeadArea1 = findViewById(R.id.countsFieldHead1RH);
            countsFieldArea1 = findViewById(R.id.countsField1RH);
            countsFieldHeadArea2 = findViewById(R.id.countsFieldHead2RH);
            countsFieldArea2 = findViewById(R.id.countsField2RH);
            speciesNotesArea = findViewById(R.id.speciesNotesRH);
            alertNotesArea = findViewById(R.id.alertRemarkRH);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.countingScreen),
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
        }

        // Proximity sensor handling screen on/off
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mPowerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if (mPowerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK))
            mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "TransektCount:WAKELOCK");
        else
            mProximityWakeLock = null;

        // Get max. proximity sensitivity
        double sensorSensitivityMax;
        if (mProximity != null)
            sensorSensitivityMax = mProximity.getMaximumRange();
        else {
            sensorSensitivityMax = 0;
        }

        // Get proximity sensitivity selection from preferences
        if (sensorSensitivityMax != 0) {
            // Set sensorSensitivity proportional to value for max. sensitivity
            if (Objects.equals(proxSensorPref, "Off"))
                sensorSensitivity = 0;
            else if (Objects.equals(proxSensorPref, "Medium")) {
                sensorSensitivity = sensorSensitivityMax / 2;
            } else if (Objects.equals(proxSensorPref, "High")) {
                sensorSensitivity = sensorSensitivityMax - 0.1;
            }
        }

        // new onBackPressed logic
        // Different Navigation Bar modes and layouts:
        // - Classic three-button navigation: NavBarMode = 0
        // - Two-button navigation (Android P): NavBarMode = 1
        // - Full screen gesture mode (Android Q): NavBarMode = 2
        // Use only if NavBarMode = 0 or 1.
        if (getNavBarMode() == 0 || getNavBarMode() == 1) {
            OnBackPressedCallback callback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    disableProximitySensor();

                    Intent intent = new Intent(CountingActivity.this, SelectSectionActivity.class);
                    startActivity(intent);
                }
            };
            getOnBackPressedDispatcher().addCallback(this, callback);
        }
    }
    // End of onCreate()

    // Check for Navigation bar 1-, 2- or 3-button mode
    public int getNavBarMode() {
        Resources resources = this.getResources();
        @SuppressLint("DiscouragedApi")
        int resourceId = resources.getIdentifier("config_navBarInteractionMode",
                "integer", "android");
        return resourceId > 0 ? resources.getInteger(resourceId) : 0;
    }

    // Load preferences at start, and also when a change is detected
    private void setPrefVariables() {
        awakePref = prefs.getBoolean("pref_awake", true);
        brightPref = prefs.getBoolean("pref_bright", true);
        sortPref = prefs.getString("pref_sort_sp", "none"); // sorted species list on counting page
        fontPref = prefs.getBoolean("pref_note_font", false);
        lhandPref = prefs.getBoolean("pref_left_hand", false); // left-handed counting page
        alertSoundPref = prefs.getBoolean("pref_alert_sound", false);
        alertSound = prefs.getString("alert_sound", null);
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false);
        buttonSound = prefs.getString("button_sound", null);
        buttonSoundMinus = prefs.getString("button_sound_minus", null); //use deeper button sound
        buttonVibPref = prefs.getBoolean("pref_button_vib", false);
        proxSensorPref = prefs.getString("pref_prox", "Off");
    }

    @SuppressLint("DiscouragedApi")
    @Override
    protected void onResume() {
        super.onResume();

        if (MyDebug.DLOG) Log.d(TAG, "316, onResume");

        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);

        // Prepare vibrator service
        vibrator = getApplicationContext().getSystemService(Vibrator.class);

        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        setPrefVariables(); // set prefs into their variables

        if (awakePref)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Build the counting screen
        //   Clear any existing views
        countsFieldHeadArea1.removeAllViews(); // headline internal/external
        countsFieldArea1.removeAllViews();     // internal counts
        countsFieldHeadArea2.removeAllViews(); // headline for external counts
        countsFieldArea2.removeAllViews();     // external counts
        speciesNotesArea.removeAllViews();     // species remarks
        alertNotesArea.removeAllViews();       // alert remarks

        // Setup the data sources
        sectionDataSource.open();
        countDataSource.open();
        alertDataSource.open();

        try {
            section = sectionDataSource.getSection(sectionId);
        } catch (CursorIndexOutOfBoundsException e) {
            mesg = getString(R.string.getHelp);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
            finish();
        }

        // Load and show the data, set title in ActionBar
        try {
            Objects.requireNonNull(getSupportActionBar()).setTitle(section.name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e) {
            // nothing
        }

        String[] idArray;
        String[] nameArray;
        String[] nameArrayL;
        String[] codeArray;

        switch (sortPref) {
            case "names_alpha" -> {
                idArray = countDataSource.getAllIdsForSectionSrtName(section.id);
                nameArray = countDataSource.getAllStringsForSectionSrtName(section.id, "name");
                codeArray = countDataSource.getAllStringsForSectionSrtName(section.id, "code");
                nameArrayL = countDataSource.getAllStringsForSectionSrtName(section.id, "name_g");
            }
            case "codes" -> {
                idArray = countDataSource.getAllIdsForSectionSrtCode(section.id);
                nameArray = countDataSource.getAllStringsForSectionSrtCode(section.id, "name");
                codeArray = countDataSource.getAllStringsForSectionSrtCode(section.id, "code");
                nameArrayL = countDataSource.getAllStringsForSectionSrtCode(section.id, "name_g");
            }
            default -> {
                idArray = countDataSource.getAllIdsForSection(section.id);
                nameArray = countDataSource.getAllStringsForSection(section.id, "name");
                codeArray = countDataSource.getAllStringsForSection(section.id, "code");
                nameArrayL = countDataSource.getAllStringsForSection(section.id, "name_g");
            }
        }

        String rName;
        int resId, resId0;
        int iPic = 0;
        resId0 = getResources().getIdentifier("p00000", "drawable",
                getPackageName());

        Integer[] imageArray = new Integer[codeArray.length];
        for (String code : codeArray) {
            rName = "p" + code;
            resId = getResources().getIdentifier(rName, "drawable",
                    getPackageName());
            if (resId != 0)
                imageArray[iPic] = resId;
            else
                imageArray[iPic] = resId0;
            iPic++;
        }

        countingWidget_i = new ArrayList<>();
        countingWidget_e = new ArrayList<>();
        countingWidgetLH_i = new ArrayList<>();
        countingWidgetLH_e = new ArrayList<>();

        // Show head2: Species with spinner to select
        if (lhandPref) // if left-handed counting page
            spinner = findViewById(R.id.countHead1SpinnerLH);
        else
            spinner = findViewById(R.id.countHead1Spinner);

        // Get itemPosition of added species by specCode from sharedPreference
        if (!Objects.equals(prefs.getString("new_spec_code", ""), "")) {
            specCode = prefs.getString("new_spec_code", "");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("new_spec_code", ""); // clear prefs value after use
            editor.apply();
        }

        if (!Objects.equals(specCode, "")) {
            int i = 0;
            while (i <= codeArray.length) {
                assert specCode != null;
                if (specCode.equals(codeArray[i])) {
                    itemPosition = i;
                    break;
                }
                i++;
            }
            specCode = "";
        }

        // Set part of counting screen
        CountingWidgetHead1 adapter = new CountingWidgetHead1(this,
               idArray, nameArray, nameArrayL, codeArray, imageArray);
        spinner.setAdapter(adapter);
        spinner.setSelection(itemPosition); // from savedInstanceState
        spinnerListener();
    }
    // End of onResume()

    // Proximity sensor handling
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (MyDebug.DLOG)
                Log.d(TAG, "449 Value0: " + event.values[0] + ", " + "Sensitivity: "
                        + (sensorSensitivity));

            // if ([0|5] >= [-0|-2.5|-4.9] && [0|5] < [0|2.5|4.9])
            if (event.values[0] >= -sensorSensitivity && event.values[0] < sensorSensitivity) {
                // near
                if (mProximityWakeLock == null)
                    mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                            "TransektCount:WAKELOCK");

                if (!mProximityWakeLock.isHeld())
                    mProximityWakeLock.acquire(30 * 60 * 1000L); // 30 minutes
            } else {
                // far
                disableProximitySensor();
            }
        }
    }

    // Necessary for SensorEventListener
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void disableProximitySensor() // far
    {
        if (mProximityWakeLock == null)
            return;
        if (mProximityWakeLock.isHeld()) {
            int flags = PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY;
            mProximityWakeLock.release(flags);
            mProximityWakeLock = null;
        }
    }

    // Inflate the menu; this adds items to the action bar if it is present.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.counting, menu);
        return true;
    }

    // Handle menu selections
    @SuppressLint("QueryPermissionsNeeded")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) // back button in actionBar
        {
            disableProximitySensor();

            Intent intent = new Intent(CountingActivity.this, SelectSectionActivity.class);
            startActivity(intent);
            return true;
        }

        else if (id == R.id.menuAddSpecies) {
            disableProximitySensor();

            mesg = getString(R.string.wait);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();

            // Trick: Pause for 100 msec to show toast
            Intent intent = new Intent(CountingActivity.this, AddSpeciesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("section_id", sectionId);
            mHandler.postDelayed(() ->
                    startActivity(intent), 100);
            return true;
        }

        else if (id == R.id.menuDelSpecies) {
            disableProximitySensor();

            mesg = getString(R.string.wait);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(CountingActivity.this, DelSpeciesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("section_id", sectionId);
            mHandler.postDelayed(() ->
                    startActivity(intent), 100);
            return true;
        }

        else if (id == R.id.menuEditSection) {
            disableProximitySensor();

            mesg = getString(R.string.wait);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(CountingActivity.this, EditSectionListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("section_id", sectionId);
            mHandler.postDelayed(() ->
                    startActivity(intent), 100);
            return true;
        }

        else if (id == R.id.menuTakePhoto) {
            Intent camIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);

            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(camIntent,
                    PackageManager.MATCH_DEFAULT_ONLY);

            // Select from available camera apps
            boolean isIntentSafe = !activities.isEmpty();
            if (isIntentSafe) {
                String title = getResources().getString(R.string.chooserTitle);
                Intent chooser = Intent.createChooser(camIntent, title);
                if (camIntent.resolveActivity(getPackageManager()) != null) {
                    try {
                        startActivity(chooser);
                    } catch (Exception e) {
                        mesg = getString(R.string.noPhotoPermit);
                        Toast.makeText(this,
                                HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                        HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                // Only default camera available
                startActivity(camIntent);
            }
            return true;
        }

        else if (id == R.id.menuClone) {
            cloneSection();
            return true;
        }

        else if (id == R.id.action_share) {
            headDataSource.open();
            Head head = headDataSource.getHead();
            headDataSource.close();
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "TransektCount " + head.transect_no);
            sendIntent.putExtra(Intent.EXTRA_TITLE, "Message of TransektCount");
            sendIntent.putExtra(Intent.EXTRA_TEXT, section.name + ": ");
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    // End of onOptionsItemSelected()

    // Call CountOptionsActivity with parameters by button in widget_counting_head2.xml
    public void editOptions(View view) {
        disableProximitySensor();

        Intent intent = new Intent(CountingActivity.this, CountOptionsActivity.class);
        intent.putExtra("count_id", iid);
        intent.putExtra("section_id", sectionId);
        intent.putExtra("section_name", section.name);
        startActivity(intent);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        setPrefVariables();
    }

    // Save activity state for getting back to CountingActivity
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putInt("section_id", sectionId);
        savedInstanceState.putInt("count_id", iid);
        savedInstanceState.putInt("item_position", itemPosition);
        savedInstanceState.putString("new_spec_code", "");
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        sectionId = savedInstanceState.getInt("section_id");
        iid = savedInstanceState.getInt("count_id");
        itemPosition = savedInstanceState.getInt("item_position");
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (MyDebug.DLOG) Log.d(TAG, "636, onPause");

        disableProximitySensor();

        // close the data sources
        sectionDataSource.close();
        countDataSource.close();
        alertDataSource.close();

        // N.B. a wakelock might not be held, e.g. if someone is using LineageOS and
        //   has denied wakelock permission to TransektCount
        if (awakePref) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        prefs.unregisterOnSharedPreferenceChangeListener(this);
        mSensorManager.unregisterListener(this);
    }
    // End of onPause()

    @Override
    public void onStop() {
        super.onStop();

        if (MyDebug.DLOG) Log.d(TAG, "660, onStop");

        if (r != null)
            r.stop(); // stop media player
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (MyDebug.DLOG) Log.d(TAG, "670, onDestroy");
    }

    // Spinner listener
    private void spinnerListener() {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long aid) {
                try {
                    countsFieldHeadArea1.removeAllViews(); // headline internal
                    countsFieldArea1.removeAllViews();    // internal counts
                    countsFieldHeadArea2.removeAllViews(); // headline for external counts
                    countsFieldArea2.removeAllViews();    // external counts
                    speciesNotesArea.removeAllViews();   // species remark
                    alertNotesArea.removeAllViews();     // alert remark

                    String sid = ((TextView) view.findViewById(R.id.countId)).getText().toString();
                    iid = Integer.parseInt(sid);
                    itemPosition = position; // position of new selected item in Spinner

                    count = countDataSource.getCountById(iid);
                    countingScreen(count);
                    if (MyDebug.DLOG)
                        Log.d(TAG, "693, SpinnerListener, count id: " + count.id
                                + ", code: " + count.code);
                } catch (Exception e) {
                    // Exception may occur when permissions are changed while activity is paused
                    //  or when spinner is rapidly repeatedly pressed
                    if (MyDebug.DLOG)
                        Log.e(TAG, "699, SpinnerListener, catch: " + e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Stub, necessary to make Spinner work correctly when repeatedly used
            }
        });
    }

    // Show rest of widgets for counting screen
    private void countingScreen(Count count) {
        if (MyDebug.DLOG) Log.d(TAG, "712, countingScreen");

        // 1. Species line is set by CountingWidgetHead1 in onResume, Spinner
        // 2. Headline Counting Area 1 (internal)
        CountingWidgetHead2 head2 = new CountingWidgetHead2(this, null);
        head2.setCountHead2(count);
        countsFieldHeadArea1.addView(head2);

        // 3. Counts internal
        if (lhandPref) // if left-handed counting page
        {
            CountingWidgetLhInt widgeti = new CountingWidgetLhInt(this, null);
            widgeti.setCountLHi(count);
            countingWidgetLH_i.add(widgeti);
            countsFieldArea1.addView(widgeti);
        } else {
            CountingWidgetInt widgeti = new CountingWidgetInt(this, null);
            widgeti.setCounti(count);
            countingWidget_i.add(widgeti);
            countsFieldArea1.addView(widgeti);
        }

        // 4. Headline Counting Area 2 (external)
        CountingWidgetHead3 head3 = new CountingWidgetHead3(this, null);
        head3.setCountHead3();
        countsFieldHeadArea2.addView(head3);

        // 5. Counts external
        if (lhandPref) // if left-handed counting page
        {
            CountingWidgetLhExt widgete = new CountingWidgetLhExt(this, null);
            widgete.setCountLHe(count);
            countingWidgetLH_e.add(widgete);
            countsFieldArea2.addView(widgete);
        } else {
            CountingWidgetExt widgete = new CountingWidgetExt(this, null);
            widgete.setCounte(count);
            countingWidget_e.add(widgete);
            countsFieldArea2.addView(widgete);
        }

        // 6. Species note widget if there are any notes
        if (isNotBlank(count.notes)) {
            NotesWidget count_notes = new NotesWidget(this, null);
            count_notes.setNotes(count.notes);
            count_notes.setFont(fontPref);
            speciesNotesArea.addView(count_notes);
        }

        // 7. Species alerts note widget if there are any alert notes to show
        List<String> alertExtras = new ArrayList<>();
        alerts = new ArrayList<>();
        List<Alert> tmpAlerts = alertDataSource.getAllAlertsForCount(count.id);

        for (Alert a : tmpAlerts) {
            alerts.add(a);
            alertExtras.add(String.format(getString(R.string.willAlert), count.name, a.alert));
        }

        if (!alertExtras.isEmpty()) {
            NotesWidget alertNotes = new NotesWidget(this, null);
            alertNotes.setNotes(join(alertExtras, "\n"));
            alertNotes.setFont(fontPref);
            alertNotesArea.addView(alertNotes);
        }
    }
    // End of countingScreen

    // Get the referenced counting widgets
    // CountingWidget_i (internal, right-handed)
    private CountingWidgetInt getCountFromId_i(int id) {
        for (CountingWidgetInt widget : countingWidget_i) {
            assert widget.count != null;
            if (widget.count.id == id)
                return widget;
        }
        return null;
    }

    // CountingWidgetLH_i (internal, left-handed)
    private CountingWidgetLhInt getCountFromIdLH_i(int id) {
        for (CountingWidgetLhInt widget : countingWidgetLH_i) {
            assert widget.count != null;
            if (widget.count.id == id)
                return widget;
        }
        return null;
    }

    // CountingWidget_e (external, right-handed)
    private CountingWidgetExt getCountFromId_e(int id) {
        for (CountingWidgetExt widget : countingWidget_e) {
            assert widget.count != null;
            if (widget.count.id == id)
                return widget;
        }
        return null;
    }

    // CountingWidgetLH_e (external, left-handed)
    private CountingWidgetLhExt getCountFromIdLH_e(int id) {
        for (CountingWidgetLhExt widget : countingWidgetLH_e) {
            assert widget.count != null;
            if (widget.count.id == id)
                return widget;
        }
        return null;
    }

    /*****************************************************************
     * The functions below are triggered by the count buttons
     * on the righthand/lefthand (LH) views
     * <p>
     * countUpf1i is triggered by buttonUpf1i in widget_counting_i.xml
     */
    public void countUpf1i(View view) {
        int tempCountId = Integer.parseInt(view.getTag().toString());
        if (MyDebug.DLOG)
            Log.d(TAG, "830, countUpf1i, section Id: " + sectionId + ", count Id: " + tempCountId);

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null) {
            // Desperate workaround for spinner's 2. misbehaviour: 
            // When returning from species that got no count to previous selected species: 
            //   1st count button press is ignored,
            //   so use button sound only for 2nd press when actually counted
            // ToDo: instead of this workaround fix by spinner replacement
            oldCounter = count.count_f1i;
            widget.countUpf1i(); // count up and set value on screen
            assert widget.count != null;
            newCounter = widget.count.count_f1i;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f1i = newCounter;
                soundButtonSound();
                buttonVib(200);
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i
                        + widget.count.count_f2i + widget.count.count_f3i);

                // Save the data
                countDataSource.saveCountf1i(count);
                sectionDataSource.saveDateSection(section);
            }
            // Run dummy activity to reenter CountinActivity and so fix spinner's 1. misbehaviour:
            //  no action by 1st click when previous species selected again
            dummy();
        }
    }

    public void countUpLHf1i(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f1i;
            widget.countUpLHf1i();
            assert widget.count != null;
            newCounter = widget.count.count_f1i;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f1i = newCounter;
                soundButtonSound();
                buttonVib(200);
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);

                countDataSource.saveCountf1i(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownf1i(View view) {
        int tempCountId = Integer.parseInt(view.getTag().toString());
        if (MyDebug.DLOG)
            Log.d(TAG, "889, countDownf1i, section Id: " + sectionId + ", tempCountId: " + tempCountId);

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f1i;
            widget.countDownf1i();
            assert widget.count != null;
            newCounter = widget.count.count_f1i;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_f1i = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountf1i(count);
            }
            dummy();
        }
    }

    public void countDownLHf1i(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f1i;
            widget.countDownLHf1i();
            assert widget.count != null;
            newCounter = widget.count.count_f1i;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_f1i = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountf1i(count);
            }
        }
    }

    public void countUpf2i(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f2i;
            widget.countUpf2i();
            assert widget.count != null;
            newCounter = widget.count.count_f2i;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f2i = newCounter;
                soundButtonSound();
                buttonVib(200);
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);
                countDataSource.saveCountf2i(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHf2i(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f2i;
            widget.countUpLHf2i();
            assert widget.count != null;
            newCounter = widget.count.count_f2i;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f2i = newCounter;
                soundButtonSound();
                buttonVib(200);
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);
                countDataSource.saveCountf2i(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownf2i(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f2i;
            widget.countDownf2i();
            assert widget.count != null;
            newCounter = widget.count.count_f2i;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_f2i = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountf2i(count);
            }
        }
    }

    public void countDownLHf2i(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f2i;
            widget.countDownLHf2i();
            assert widget.count != null;
            newCounter = widget.count.count_f2i;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_f2i = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountf2i(count);
            }
        }
    }

    public void countUpf3i(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f3i;
            widget.countUpf3i();
            assert widget.count != null;
            newCounter = widget.count.count_f3i;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f3i = newCounter;
                soundButtonSound();
                buttonVib(200);
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);
                countDataSource.saveCountf3i(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHf3i(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f3i;
            widget.countUpLHf3i();
            assert widget.count != null;
            newCounter = widget.count.count_f3i;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f3i = newCounter;
                soundButtonSound();
                buttonVib(200);
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);
                countDataSource.saveCountf3i(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownf3i(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f3i;
            widget.countDownf3i();
            assert widget.count != null;
            newCounter = widget.count.count_f3i;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_f3i = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountf3i(count);
            }
        }
    }

    public void countDownLHf3i(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f3i;
            widget.countDownLHf3i();
            assert widget.count != null;
            newCounter = widget.count.count_f3i;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_f3i = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountf3i(count);
            }
        }
    }

    public void countUppi(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_pi;
            widget.countUppi();
            assert widget.count != null;
            newCounter = widget.count.count_pi;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_pi = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountpi(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHpi(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_pi;
            widget.countUpLHpi();
            assert widget.count != null;
            newCounter = widget.count.count_pi;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_pi = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountpi(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownpi(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_pi;
            widget.countDownpi();
            assert widget.count != null;
            newCounter = widget.count.count_pi;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_pi = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountpi(count);
            }
        }
    }

    public void countDownLHpi(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_pi;
            widget.countDownLHpi();
            assert widget.count != null;
            newCounter = widget.count.count_pi;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_pi = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountpi(count);
            }
        }
    }

    public void countUpli(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_li;
            widget.countUpli();
            assert widget.count != null;
            newCounter = widget.count.count_li;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_li = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountli(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHli(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_li;
            widget.countUpLHli();
            assert widget.count != null;
            newCounter = widget.count.count_li;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_li = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountli(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownli(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_li;
            widget.countDownli();
            assert widget.count != null;
            newCounter = widget.count.count_li;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_li = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountli(count);
            }
        }
    }

    public void countDownLHli(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_li;
            widget.countDownLHli();
            assert widget.count != null;
            newCounter = widget.count.count_li;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_li = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountli(count);
            }
        }
    }

    public void countUpei(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_ei;
            widget.countUpei();
            assert widget.count != null;
            newCounter = widget.count.count_ei;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_ei = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountei(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHei(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_ei;
            widget.countUpLHei();
            assert widget.count != null;
            newCounter = widget.count.count_ei;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_ei = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountei(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownei(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_ei;
            widget.countDownei();
            assert widget.count != null;
            newCounter = widget.count.count_ei;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_ei = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountei(count);
            }
        }
    }

    public void countDownLHei(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null) {
            oldCounter = count.count_ei;
            widget.countDownLHei();
            assert widget.count != null;
            newCounter = widget.count.count_ei;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_ei = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountei(count);
            }
        }
    }

    // count functions for external counters
    public void countUpf1e(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        if (MyDebug.DLOG)
            Log.d(TAG, "1340, countUpf1e, section Id: " + sectionId
                    + ", tempCountId: " + tempCountId);

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f1e;
            widget.countUpf1e();
            assert widget.count != null;
            newCounter = widget.count.count_f1e;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f1e = newCounter;
                soundButtonSound();
                buttonVib(200);

                // save the data
                countDataSource.saveCountf1e(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHf1e(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f1e;
            widget.countUpLHf1e();
            assert widget.count != null;
            newCounter = widget.count.count_f1e;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f1e = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountf1e(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownf1e(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        if (MyDebug.DLOG)
            Log.d(TAG, "1388 countDownf1e, section Id: " + sectionId
                    + ", tempCountId: " + tempCountId);

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f1e;
            widget.countDownf1e();
            assert widget.count != null;
            newCounter = widget.count.count_f1e;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_f1e = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountf1e(count);
            }
        }
    }

    public void countDownLHf1e(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f1e;
            widget.countDownLHf1e();
            assert widget.count != null;
            newCounter = widget.count.count_f1e;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_f1e = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountf1e(count);
            }
        }
    }

    public void countUpf2e(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f2e;
            widget.countUpf2e();
            assert widget.count != null;
            newCounter = widget.count.count_f2e;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f2e = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountf2e(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHf2e(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f2e;
            widget.countUpLHf2e();
            assert widget.count != null;
            newCounter = widget.count.count_f2e;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f2e = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountf2e(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownf2e(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f2e;
            widget.countDownf2e();
            assert widget.count != null;
            newCounter = widget.count.count_f2e;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_f2e = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountf2e(count);
            }
        }
    }

    public void countDownLHf2e(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f2e;
            widget.countDownLHf2e();
            assert widget.count != null;
            newCounter = widget.count.count_f2e;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_f2e = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountf2e(count);
            }
        }
    }

    public void countUpf3e(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f3e;
            widget.countUpf3e();
            assert widget.count != null;
            newCounter = widget.count.count_f3e;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f3e = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountf3e(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHf3e(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f3e;
            widget.countUpLHf3e();
            assert widget.count != null;
            newCounter = widget.count.count_f3e;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f3e = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountf3e(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownf3e(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f3e;
            widget.countDownf3e();
            assert widget.count != null;
            newCounter = widget.count.count_f3e;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_f3e = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountf3e(count);
            }
        }
    }

    public void countDownLHf3e(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_f3e;
            widget.countDownLHf3e();
            assert widget.count != null;
            newCounter = widget.count.count_f3e;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_f3e = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountf3e(count);
            }
        }
    }

    public void countUppe(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_pe;
            widget.countUppe();
            assert widget.count != null;
            newCounter = widget.count.count_pe;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_pe = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountpe(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHpe(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_pe;
            widget.countUpLHpe();
            assert widget.count != null;
            newCounter = widget.count.count_pe;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_pe = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountpe(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownpe(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_pe;
            widget.countDownpe();
            assert widget.count != null;
            newCounter = widget.count.count_pe;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_pe = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountpe(count);
            }
        }
    }

    public void countDownLHpe(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_pe;
            widget.countDownLHpe();
            assert widget.count != null;
            newCounter = widget.count.count_pe;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_pe = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountpe(count);
            }
        }
    }

    public void countUple(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_le;
            widget.countUple();
            assert widget.count != null;
            newCounter = widget.count.count_le;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_le = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountle(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHle(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_le;
            widget.countUpLHle();
            assert widget.count != null;
            newCounter = widget.count.count_le;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_le = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountle(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownle(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_le;
            widget.countDownle();
            assert widget.count != null;
            newCounter = widget.count.count_le;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_le = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountle(count);
            }
        }
    }

    public void countDownLHle(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_le;
            widget.countDownLHle();
            assert widget.count != null;
            newCounter = widget.count.count_le;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_le = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountle(count);
            }
        }
    }

    public void countUpee(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_ee;
            widget.countUpee();
            assert widget.count != null;
            newCounter = widget.count.count_ee;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_ee = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountee(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHee(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_ee;
            widget.countUpLHee();
            assert widget.count != null;
            newCounter = widget.count.count_ee;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_ee = newCounter;
                soundButtonSound();
                buttonVib(200);
                countDataSource.saveCountee(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownee(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_ee;
            widget.countDownee();
            assert widget.count != null;
            newCounter = widget.count.count_ee;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_ee = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountee(count);
            }
        }
    }

    public void countDownLHee(View view) {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null) {
            oldCounter = count.count_ee;
            widget.countDownLHee();
            assert widget.count != null;
            newCounter = widget.count.count_ee;
            if (newCounter < oldCounter || newCounter == 0) {
                count.count_ee = newCounter;
                soundButtonSoundMinus();
                buttonVib(450);
                countDataSource.saveCountee(count);
            }
        }
    }
    // End of counters

    /*****************/
    // Call DummyActivity to overcome Spinner deficiency for repeated item
    private void dummy() {
        Intent intent = new Intent(CountingActivity.this, DummyActivity.class);
        intent.putExtra("section_id", sectionId);
        intent.putExtra("init_Chars", "");
        intent.putExtra("is_Flag", "isCount");
        startActivity(intent);
    }

    // Clone section with check for double names
    private void cloneSection() {
        final AlertDialog.Builder aDialog = new AlertDialog.Builder(this);

        aDialog.setTitle(getString(R.string.dpSectTitle));

        // Set up the input an specify the type of input expected
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        // Set up the buttons
        aDialog.setView(input);
        aDialog.setPositiveButton("OK", (dialog, which) ->
        {
            // Enter a new section name
            String sect_name = input.getText().toString();

            // Check for empty section name
            if (sect_name.isEmpty()) {
                mesg = getString(R.string.attention) + " " + getString(R.string.newSectName);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                return;
            }

            // Check if this is not a duplicate of an existing name
            if (compSectionNames(sect_name)) {
                mesg = getString(R.string.attention) + " " + sect_name + " " + getString(R.string.isdouble);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                return;
            }

            // Check if section is contiguous
            int entries = -1, maxId = 0;
            try {
                entries = sectionDataSource.getNumEntries();
            } catch (Exception e) {
                if (MyDebug.DLOG) Log.d(TAG, "1873 getNumEntries failed");
            }

            try {
                maxId = sectionDataSource.getMaxId();
            } catch (Exception e) {
                if (MyDebug.DLOG) Log.d(TAG, "1879 getMaxId failed");
            }

            if (entries != maxId) {
                mesg = getString(R.string.notContiguous);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                if (MyDebug.DLOG)
                    Log.d(TAG, "1885 maxId: " + maxId + ", entries: " + entries);
                return;
            }

            // Creating the new section
            Section newSection = sectionDataSource.createSection(sect_name);
            sectionDataSource.saveSection(newSection);
            for (Count c : countDataSource.getAllCountsForSection(sectionId)) {
                Count newCount = countDataSource.createCount(newSection.id, c.name, c.code, c.name_g);
                if (newCount != null) {
                    countDataSource.saveCount(newCount);
                }
            }

            // Exit this and go to the list of new sections
            mesg = sect_name + " " + getString(R.string.newCopyCreated);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(CountingActivity.this, SelectSectionActivity.class);
            startActivity(intent);
        });

        aDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        aDialog.show();
    }

    // Compare section names for duplicates and return state of duplicate found
    private boolean compSectionNames(String newname) {
        boolean isDblName = false;
        String sname;

        List<Section> sectionList = sectionDataSource.getAllSections(prefs);

        int childcount = sectionList.size() + 1;
        // For all sections
        for (int i = 1; i < childcount; i++) {
            section = sectionDataSource.getSection(i);
            sname = section.name;
            if (MyDebug.DLOG) Log.d(TAG, "1921, compSectionNames, sname = " + sname);

            if (newname.equals(sname)) {
                isDblName = true;
                if (MyDebug.DLOG) Log.d(TAG, "1925, compSectionNames, Double name = " + sname);
                break;
            }
        }
        return isDblName;
    }

    // Alert checking...
    private void checkAlert(int countId, int count_value) {
        for (Alert a : alerts) {
            if (a.count_id == countId && a.alert == count_value) {
                AlertDialog.Builder row_alert = new AlertDialog.Builder(this);
                row_alert.setTitle(String.format(getString(R.string.alertTitle), count_value));
                row_alert.setMessage(a.alert_text);
                row_alert.setNegativeButton("OK", (dialog, whichButton) ->
                {
                    // Cancelled.
                });
                row_alert.show();
                soundAlert();
                break;
            }
        }
    }

    // If the user has set the preference for an audible alert, then sound it here.
    private void soundAlert() {
        if (alertSoundPref) {
            try {
                Uri notification;
                if (isNotBlank(alertSound) && alertSound != null)
                    notification = Uri.parse(alertSound);
                else
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                mHandler.postDelayed(r::stop, 300); // Stop sound after 0.3 sec
            } catch (Exception e) {
                // Do nothing
            }
        }
    }

    // If the user has set the preference for button sound, then sound it here.
    private void soundButtonSound() {
        if (buttonSoundPref) {
            try {
                Uri notification;
                if (isNotBlank(buttonSound) && buttonSound != null)
                    notification = Uri.parse(buttonSound);
                else
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                mHandler.postDelayed(r::stop, 400);
            } catch (Exception e) {
                // Do nothing
            }
        }
    }

    private void soundButtonSoundMinus() {
        if (buttonSoundPref) {
            try {
                Uri notification;
                if (isNotBlank(buttonSoundMinus) && buttonSoundMinus != null)
                    notification = Uri.parse(buttonSoundMinus);
                else
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                mHandler.postDelayed(r::stop, 400);
            } catch (Exception e) {
                // Do nothing
            }
        }
    }

    private void buttonVib(long dur) {
        if (buttonVibPref && vibrator.hasVibrator()) {
            if (SDK_INT >= 31) { // S, Android 12
                if (MyDebug.DLOG) Log.d(TAG, "2008, Vibrator >= SDK 31");

                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            } else {
                if (SDK_INT >= 26) // Oreo Android 8
                {
                    if (MyDebug.DLOG) Log.d(TAG, "2014 Vibrator >= SDK 26");
                    vibrator.vibrate(VibrationEffect.createOneShot(dur,
                            VibrationEffect.DEFAULT_AMPLITUDE));
                    vibrator.cancel();
                }
            }
        }
    }

    /*******************************************************************************
     * Checks if a CharSequence is not empty (""), not null and not whitespace only.
     * <p>
     * isNotBlank(null)      = false
     * isNotBlank("")        = false
     * isNotBlank(" ")       = false
     * isNotBlank("bob")     = true
     * isNotBlank("  bob  ") = true
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is
     * not empty and not null and not whitespace
     */
    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    public static boolean isBlank(final CharSequence cs) {
        int strLen;

        if (cs == null || (strLen = cs.length()) == 0)
            return true;

        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i)))
                return false;
        }
        return true;
    }

    public static String join(Iterator<?> iterator, String separator) {
        if (iterator == null)
            return null;
        else if (!iterator.hasNext())
            return "";
        else {
            Object first = iterator.next();
            if (!iterator.hasNext())
                return toString(first);
            else {
                StringBuilder buf = new StringBuilder(256);
                if (first != null)
                    buf.append(first);

                while (iterator.hasNext()) {
                    if (separator != null)
                        buf.append(separator);

                    Object obj = iterator.next();
                    if (obj != null)
                        buf.append(obj);
                }

                return buf.toString();
            }
        }
    }

    public static String join(Iterable<?> iterable, String separator) {
        return iterable == null ? null : join(iterable.iterator(), separator);
    }

    public static String toString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

}
