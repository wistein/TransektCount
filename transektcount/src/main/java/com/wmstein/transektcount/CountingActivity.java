package com.wmstein.transektcount;

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
import android.media.MediaPlayer;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
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

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;

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
 * It does the actual counting with 12 counters,
 *   checks for alerts,
 *   calls AddSpeciesActivity, DelSpeciesActivity, EditSectionListActivity,
 *   and CountOptionsActivity,
 *   clones a section,
 *   switches screen off when device is pocketed
 *   and allows taking pictures and sending notes.
 * <p>
 * CountingActivity uses CountingWidget*.kt, NotesWidget.kt and activity_counting*.xml
 * <p>
 * Basic counting functions created by milo for BeeCount on 2014-05-05.
 * Adopted, modified and enhanced for TransektCount by wmstein since 2016-02-18,
 * last edited on 2025-12-29
 */
public class CountingActivity
        extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener, SensorEventListener {
    private static final String TAG = "CountAct";
    private int sectionId;  // section ID
    private int iid = 1;    // count ID
    private int itemPosition = 0; // 0 = 1. position of selected species in array for Spinner

    private View counting_screen;

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

    private Context audioAttributionContext;
    private MediaPlayer rToneP, rToneM, rToneA;
    private boolean setNoButtonSound = false;
    private Vibrator vibrator;
    private String mesg = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "161, onCreate");

        audioAttributionContext = (Build.VERSION.SDK_INT >= 30) ?
                createAttributionContext("ringSound") : this;

        TransektCountApplication transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        setPrefVariables(); // set all stored preferences into their variables

        // Get values from calling activity
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
            EdgeToEdge.enable(this);

        // Distinguish between left-/ right-handed counting page layout
        if (lhandPref) {
            setContentView(R.layout.activity_counting_lh);
            counting_screen = findViewById(R.id.countingScreenLH);
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
            counting_screen = findViewById(R.id.countingScreen);
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
                    Intent intent;
                    intent = new Intent(CountingActivity.this, SelectSectionActivity.class);
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

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "316, onResume");

        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);

        // Prepare vibrator service
        vibrator = getApplicationContext().getSystemService(Vibrator.class);

        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        setPrefVariables(); // set prefs into their variables

        // Set full brightness of screen
        if (brightPref) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        if (awakePref)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Prepare button sounds
        if (alertSoundPref) {
            Uri uriA;
            if (isNotBlank(alertSound) && alertSound != null)
                uriA = Uri.parse(alertSound);
            else
                uriA = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (rToneA == null)
                rToneA = MediaPlayer.create(audioAttributionContext, uriA);
        }

        if (buttonSoundPref) {
            Uri uriP;
            if (isNotBlank(buttonSound) && buttonSound != null) {
                uriP = Uri.parse(buttonSound);
            } else
                uriP = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (rToneP == null)
                rToneP = MediaPlayer.create(audioAttributionContext, uriP);

            Uri uriM;
            if (isNotBlank(buttonSoundMinus) && buttonSoundMinus != null)
                uriM = Uri.parse(buttonSoundMinus);
            else
                uriM = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (rToneM == null)
                rToneM = MediaPlayer.create(audioAttributionContext, uriM);
        }

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
            disableProximitySensor();
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
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "489 Value0: " + event.values[0] + ", " + "Sensitivity: "
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
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        } else if (id == R.id.menuAddSpecies) {
            disableProximitySensor();

            Intent intent = new Intent(CountingActivity.this, AddSpeciesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("section_id", sectionId);
            startActivity(intent);
            return true;
        } else if (id == R.id.menuDelSpecies) {
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
        } else if (id == R.id.menuEditSection) {
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
        } else if (id == R.id.menuTakePhoto) {
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
        } else if (id == R.id.action_share) {
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

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "667, onPause");

        disableProximitySensor();

        // Close the data sources
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
    protected void onStop() {
        super.onStop();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "692, onStop");

        counting_screen.invalidate();

        // Stop media player
        if (buttonSoundPref) {
            if (rToneP != null) {
                if (rToneP.isPlaying()) {
                    rToneP.stop();
                }
                rToneP.release();
                rToneP = null;
            }
            if (rToneM != null) {
                if (rToneM.isPlaying()) {
                    rToneM.stop();
                }
                rToneM.release();
                rToneM = null;
            }
        }

        if (alertSoundPref) {
            if (rToneA != null) {
                if (rToneA.isPlaying()) {
                    rToneA.stop();
                }
                rToneA.release();
                rToneA = null;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "730, onDestroy");
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
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.d(TAG, "753, SpinnerListener, count id: " + count.id
                                + ", code: " + count.code);
                } catch (Exception e) {
                    // Exception may occur when permissions are changed while activity is paused
                    //  or when spinner is rapidly repeatedly pressed
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.e(TAG, "759, SpinnerListener, catch: " + e);
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
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "773, countingScreen");

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

            // Set notes with light blue introducer
            count_notes.setNotesC(getString(R.string.species_notes), count.notes);
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
     * and widget_counting_lhi.xml
     */
    public void countUpf1i(View view) {
        // Run reenterActivity to re-enter CountingActivity and so fix spinner's 1. misbehaviour:
        //  no action by 1st click when previous species selected again
        int tempCountId = Integer.parseInt(view.getTag().toString());
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "894, countUpf1i, section Id: " + sectionId + ", count Id: " + tempCountId);

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null) {
            // Desperate workaround for spinner's 2. misbehaviour: 
            // When returning from species that got no count to previous selected species: 
            //   1st count button press is ignored,
            //   so use button sound only for 2nd press when actually counted
            oldCounter = count.count_f1i;
            widget.countUpf1i(); // count up and set value on screen
            assert widget.count != null;
            newCounter = widget.count.count_f1i;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f1i = newCounter;
                buttonVib(200);
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i
                        + widget.count.count_f2i + widget.count.count_f3i);
                soundButtonSound();

                // Save the data
                countDataSource.saveCountf1i(count);
                sectionDataSource.saveDateSection(section);
            }
            reenterActivity();
        }
    }

    public void countUpLHf1i(View view) {
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
                buttonVib(200);
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i
                        + widget.count.count_f2i + widget.count.count_f3i);
                soundButtonSound();

                countDataSource.saveCountf1i(count);
                sectionDataSource.saveDateSection(section);
            }
            reenterActivity();
        }
    }

    public void countDownf1i(View view) {
        int tempCountId = Integer.parseInt(view.getTag().toString());
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "951, countDownf1i, section Id: " + sectionId + ", tempCountId: " + tempCountId);

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
            reenterActivity();
        }
    }

    public void countDownLHf1i(View view) {
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
            reenterActivity();
        }
    }

    public void countUpf2i(View view) {
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
                buttonVib(200);
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i
                        + widget.count.count_f2i + widget.count.count_f3i);
                soundButtonSound();
                countDataSource.saveCountf2i(count);
                sectionDataSource.saveDateSection(section);
            }
            reenterActivity();
        }
    }

    public void countUpLHf2i(View view) {
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
                buttonVib(200);
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i
                        + widget.count.count_f2i + widget.count.count_f3i);
                soundButtonSound();
                countDataSource.saveCountf2i(count);
                sectionDataSource.saveDateSection(section);
            }
            reenterActivity();
        }
    }

    public void countDownf2i(View view) {
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
            reenterActivity();
        }
    }

    public void countDownLHf2i(View view) {
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
            reenterActivity();
        }
    }

    public void countUpf3i(View view) {
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
                buttonVib(200);
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i
                        + widget.count.count_f2i + widget.count.count_f3i);
                soundButtonSound();
                countDataSource.saveCountf3i(count);
                sectionDataSource.saveDateSection(section);
            }
            reenterActivity();
        }
    }

    public void countUpLHf3i(View view) {
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
                buttonVib(200);
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i
                        + widget.count.count_f2i + widget.count.count_f3i);
                soundButtonSound();
                countDataSource.saveCountf3i(count);
                sectionDataSource.saveDateSection(section);
            }
            reenterActivity();
        }
    }

    public void countDownf3i(View view) {
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
            reenterActivity();
        }
    }

    public void countDownLHf3i(View view) {
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
            reenterActivity();
        }
    }

    public void countUppi(View view) {
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
            reenterActivity();
        }
    }

    public void countUpLHpi(View view) {
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
            reenterActivity();
        }
    }

    public void countDownpi(View view) {
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
            reenterActivity();
        }
    }

    public void countDownLHpi(View view) {
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
            reenterActivity();
        }
    }

    public void countUpli(View view) {
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
            reenterActivity();
        }
    }

    public void countUpLHli(View view) {
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
            reenterActivity();
        }
    }

    public void countDownli(View view) {
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
            reenterActivity();
        }
    }

    public void countDownLHli(View view) {
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
            reenterActivity();
        }
    }

    public void countUpei(View view) {
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
            reenterActivity();
        }
    }

    public void countUpLHei(View view) {
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
            reenterActivity();
        }
    }

    public void countDownei(View view) {
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
            reenterActivity();
        }
    }

    public void countDownLHei(View view) {
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
            reenterActivity();
        }
    }

    // count functions for external counters
    public void countUpf1e(View view) {
        int tempCountId = Integer.parseInt(view.getTag().toString());

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "1405, countUpf1e, section Id: " + sectionId
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
            reenterActivity();
        }
    }

    public void countUpLHf1e(View view) {
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
            reenterActivity();
        }
    }

    public void countDownf1e(View view) {
        int tempCountId = Integer.parseInt(view.getTag().toString());

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "1453 countDownf1e, section Id: " + sectionId
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
            reenterActivity();
        }
    }

    public void countDownLHf1e(View view) {
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
            reenterActivity();
        }
    }

    public void countUpf2e(View view) {
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
            reenterActivity();
        }
    }

    public void countUpLHf2e(View view) {
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
            reenterActivity();
        }
    }

    public void countDownf2e(View view) {
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
            reenterActivity();
        }
    }

    public void countDownLHf2e(View view) {
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
            reenterActivity();
        }
    }

    public void countUpf3e(View view) {
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
            reenterActivity();
        }
    }

    public void countUpLHf3e(View view) {
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
            reenterActivity();
        }
    }

    public void countDownf3e(View view) {
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
            reenterActivity();
        }
    }

    public void countDownLHf3e(View view) {
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
            reenterActivity();
        }
    }

    public void countUppe(View view) {
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
            reenterActivity();
        }
    }

    public void countUpLHpe(View view) {
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
            reenterActivity();
        }
    }

    public void countDownpe(View view) {
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
            reenterActivity();
        }
    }

    public void countDownLHpe(View view) {
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
            reenterActivity();
        }
    }

    public void countUple(View view) {
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
            reenterActivity();
        }
    }

    public void countUpLHle(View view) {
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
            reenterActivity();
        }
    }

    public void countDownle(View view) {
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
            reenterActivity();
        }
    }

    public void countDownLHle(View view) {
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
            reenterActivity();
        }
    }

    public void countUpee(View view) {
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
            reenterActivity();
        }
    }

    public void countUpLHee(View view) {
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
            reenterActivity();
        }
    }

    public void countDownee(View view) {
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
            reenterActivity();
        }
    }

    public void countDownLHee(View view) {
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
            reenterActivity();
        }
    }
    // End of counters

    /*****************/

    // Re-enter CountingActivity to overcome Spinner deficiency for repeated item
    private void reenterActivity() {
        Intent intent = new Intent(CountingActivity.this, CountingActivity.class);
        intent.putExtra("section_id", sectionId);
        intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
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
                setNoButtonSound = true; // don't soundButtonSound() when soundAlert()
                soundAlert();
                row_alert.show();
                break;
            }
        }
    }

    // If the user has set the preference for an audible alert, then sound it here.
    private void soundAlert() {
        if (alertSoundPref) {
            if (rToneA.isPlaying()) {
                rToneA.stop();
                rToneA.release();
            }
            rToneA.start();
        }
    }

    // If the user has set the preference for button sound, then sound it here.
    private void soundButtonSound() {
        if (buttonSoundPref) {
            // don't soundButtonSound() when soundAlert()
            if (setNoButtonSound) {
                setNoButtonSound = false;
            } else {
                if (rToneP.isPlaying()) {
                    rToneP.stop();
                    rToneP.release();
                }
                rToneP.start();
            }
        }
    }

    private void soundButtonSoundMinus() {
        if (buttonSoundPref) {
            if (rToneM.isPlaying()) {
                rToneM.stop();
                rToneM.release();
            }
            rToneM.start();
        }
    }

    private void buttonVib(long dur) {
        if (buttonVibPref && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= 31) { // S, Android 12
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.d(TAG, "1962, Vibrator >= SDK 31");

                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            } else {
                if (Build.VERSION.SDK_INT >= 26) // Oreo Android 8
                {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.d(TAG, "1969, Vibrator >= SDK 26");
                    vibrator.vibrate(VibrationEffect.createOneShot(dur,
                            VibrationEffect.DEFAULT_AMPLITUDE));
                    vibrator.cancel();
                }
            }
        }
    }

    /**************************************************************************************
     * Following functions are derived from StringUtils of the Apache commons-lang3 library
     * licensed under Apache License Version 2.0, January 2004
     <p>
     * Checks if a CharSequence is not empty (""), not null and not whitespace only.
     * <p>
     * isNotBlank(null)      = false
     * isNotBlank("")        = false
     * isNotBlank(" ")       = false
     * isNotBlank("bob")     = true
     * isNotBlank("  bob  ") = true
     *
     * @param cs the CharSequence to check, may be null
     * @return true if the CharSequence is not empty and not null and not whitespace
     */
    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    public static boolean isBlank(final CharSequence cs) {
        final int strLen = cs.length();
        if (strLen == 0)
            return true;

        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i)))
                return false;
        }
        return true;
    }

    public static String toString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    /**
     * Joins the elements of the provided Iterator into a single String containing the provided elements.
     * <p>
     * No delimiter is added before or after the list. A null separator is the same as an empty String ("").
     * <p>
     *
     * @param iterator  the Iterator of values to join together, may be null.
     * @param separator the separator character to use, null treated as "".
     * @return the joined String, null if null iterator input.
     * <p>
     * Derived from package org.apache.commons.lang3/StringUtils
     */
    public static String join(final Iterator<?> iterator, final String separator) {
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

    /**
     * Joins the elements of the provided Iterable into a single String containing the provided elements.
     * <p>
     * No delimiter is added before or after the list. A null separator is the same as an empty String ("").
     * <p>
     *
     * @param iterable  Iterable providing the values to join together, may be null.
     * @param separator the separator character to use, null treated as "".
     * @return the joined String, null if null iterator input.
     * <p>
     * Derived from package org.apache.commons.lang3/StringUtils
     */
    public static String join(final Iterable<?> iterable, final String separator) {
        return iterable == null ? null : join(iterable.iterator(), separator);
    }

}
