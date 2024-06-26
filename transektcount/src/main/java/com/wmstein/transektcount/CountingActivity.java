package com.wmstein.transektcount;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.wmstein.transektcount.database.Alert;
import com.wmstein.transektcount.database.AlertDataSource;
import com.wmstein.transektcount.database.Count;
import com.wmstein.transektcount.database.CountDataSource;
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

/**********************************************************************************************
 * CountingActivity
 * does the actual counting on portrait layout for a manually selected section with 12 counters,
 * checks for alerts,
 * calls CountOptionsActivity, EditSectionActivity and DummyActivity,
 * clones a section,
 * switches screen off when device is pocketed
 * and lets you send a message.
 * <p>
 * Basic counting functions inspired by milo's CountingActivity.java of BeeCount from 2014-05-05.
 * Changes and additions for TransektCount by wmstein since 2016-02-18,
 * last edit on 2023-12-15.
 */
public class CountingActivity
    extends AppCompatActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "CountAct";

    private int sectionId;  // section ID
    private int iid = 1;    // count ID

    private LinearLayout sectionNotesArea1;     // section notes line
    private LinearLayout countsFieldHeadArea3;  // headline internal/external
    private LinearLayout countsField1Area4;     // internal counts field
    private LinearLayout countsFieldHeadArea5;  // headline external
    private LinearLayout countsField2Area6;     // external counts field
    private LinearLayout speciesRemarkArea7;    // species remark line
    private LinearLayout alertRemarkArea8;      // alert remark line

    // Proximity sensor handling for screen on/off
    private PowerManager.WakeLock mProximityWakeLock;

    // preferences
    private SharedPreferences prefs;
    private boolean awakePref;
    private boolean brightPref;
    private String sortPref;
    private boolean fontPref;
    private boolean lhandPref; // true for lefthand mode of counting screen
    private boolean alertSoundPref;
    private boolean buttonSoundPref;
    private boolean buttonVibPref;
    private String alertSound;
    private String buttonSound;
    private String buttonSoundMinus;
    private int itemPosition = 0; // 0 = 1. position of selected species in array for Spinner
    private String specCode = "";
    private boolean sectionHasTrack;

    // the actual data
    private Count count;     // line in SQLite counts table for a counted species with all fields
    private Section section; // line in SQLite sections table
    private List<Alert> alerts;
    private Spinner spinner; // species selector
    private int oldCounter;
    private int newCounter;

    // CountingWidgets
    private List<CountingWidgetInt> countingWidget_i;
    private List<CountingWidgetExt> countingWidget_e;
    private List<CountingWidgetLhInt> countingWidgetLH_i;
    private List<CountingWidgetLhExt> countingWidgetLH_e;

    // data sources
    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;
    private AlertDataSource alertDataSource;

    private Ringtone r;
    private VibratorManager vibratorManager;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Context context = this.getApplicationContext();

        TransektCountApplication transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        setPrefVariables(); // set all prefs into their variables

        // get values from calling activity
        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            if (extras.getBoolean("welcome_act")) // called from ListSectionAdapter
            {
                // store initial itemPosition = 0 for Spinner into SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("item_position", 0);
                editor.commit();
            }
            sectionId = extras.getInt("section_id");
        }

        sectionDataSource = new SectionDataSource(this);
        countDataSource = new CountDataSource(this);
        alertDataSource = new AlertDataSource(this);

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        // distinguish between left-/ right-handed counting page layout
        if (lhandPref)
        {
            setContentView(R.layout.activity_counting_lh);
            LinearLayout counting_screen = findViewById(R.id.countingScreenLH);
            counting_screen.setBackground(transektCount.getBackground());
            sectionNotesArea1 = findViewById(R.id.sectionNotesLH);
            countsFieldHeadArea3 = findViewById(R.id.countsFieldHead1LH);
            countsField1Area4 = findViewById(R.id.countsField1LH);
            countsFieldHeadArea5 = findViewById(R.id.countsFieldHead2LH);
            countsField2Area6 = findViewById(R.id.countsField2LH);
            speciesRemarkArea7 = findViewById(R.id.speciesRemarkLH);
            alertRemarkArea8 = findViewById(R.id.alertRemarkLH);
        }
        else
        {
            setContentView(R.layout.activity_counting);
            LinearLayout counting_screen = findViewById(R.id.countingScreen);
            counting_screen.setBackground(transektCount.getBackground());
            sectionNotesArea1 = findViewById(R.id.sectionNotesRH);
            countsFieldHeadArea3 = findViewById(R.id.countsFieldHead1RH);
            countsField1Area4 = findViewById(R.id.countsField1RH);
            countsFieldHeadArea5 = findViewById(R.id.countsFieldHead2RH);
            countsField2Area6 = findViewById(R.id.countsField2RH);
            speciesRemarkArea7 = findViewById(R.id.speciesRemarkRH);
            alertRemarkArea8 = findViewById(R.id.alertRemarkRH);
        }

        // set visibility in case of invisible mode left over from CountingActivityA
        countsField1Area4.setVisibility(View.VISIBLE);
        countsFieldHeadArea5.setVisibility(View.VISIBLE);
        alertRemarkArea8.setVisibility(View.VISIBLE);

        if (awakePref)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PowerManager mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        try
        {
            assert mPowerManager != null;
            if (mPowerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK))
                mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "TransektCount:WAKELOCK");
            enableProximitySensor();
        } catch (NullPointerException e)
        {
            // do nothing
        }
    }
    // End of onCreate

    // Load preferences at start, and also when a change is detected
    private void setPrefVariables()
    {
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
        sectionHasTrack = prefs.getBoolean("section_has_track", false);
    }

    @SuppressLint("LongLogTag")
    @Override
    protected void onResume()
    {
        super.onResume();

        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        setPrefVariables(); // set all prefs into their variables
        itemPosition = prefs.getInt("item_position", 0);

        enableProximitySensor();

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        // build the counting screen
        //   clear any existing views
        sectionNotesArea1.removeAllViews();    // section notes
        countsFieldHeadArea3.removeAllViews(); // headline internal/external
        countsField1Area4.removeAllViews();    // internal counts
        countsFieldHeadArea5.removeAllViews(); // headline for external counts
        countsField2Area6.removeAllViews();    // external counts
        speciesRemarkArea7.removeAllViews();   // species remarks
        alertRemarkArea8.removeAllViews();     // alert remarks

        // setup the data sources
        sectionDataSource.open();
        countDataSource.open();
        alertDataSource.open();

        try
        {
            section = sectionDataSource.getSection(sectionId);
        } catch (CursorIndexOutOfBoundsException e)
        {
            showSnackbarRed(getString(R.string.getHelp));
            finish();
        }

        // Load and show the data, set title in ActionBar
        try
        {
            Objects.requireNonNull(getSupportActionBar()).setTitle(section.name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e)
        {
            // nothing
        }

        String[] idArray;
        String[] nameArray;
        String[] nameArrayL;
        String[] codeArray;
        Integer[] imageArray;

        switch (sortPref)
        {
            case "names_alpha" ->
            {
                idArray = countDataSource.getAllIdsForSectionSrtName(section.id);
                nameArray = countDataSource.getAllStringsForSectionSrtName(section.id, "name");
                codeArray = countDataSource.getAllStringsForSectionSrtName(section.id, "code");
                nameArrayL = countDataSource.getAllStringsForSectionSrtName(section.id, "name_g");
                imageArray = countDataSource.getAllImagesForSectionSrtName(section.id);
            }
            case "codes" ->
            {
                idArray = countDataSource.getAllIdsForSectionSrtCode(section.id);
                nameArray = countDataSource.getAllStringsForSectionSrtCode(section.id, "name");
                codeArray = countDataSource.getAllStringsForSectionSrtCode(section.id, "code");
                nameArrayL = countDataSource.getAllStringsForSectionSrtCode(section.id, "name_g");
                imageArray = countDataSource.getAllImagesForSectionSrtCode(section.id);
            }
            default ->
            {
                idArray = countDataSource.getAllIdsForSection(section.id);
                nameArray = countDataSource.getAllStringsForSection(section.id, "name");
                codeArray = countDataSource.getAllStringsForSection(section.id, "code");
                nameArrayL = countDataSource.getAllStringsForSection(section.id, "name_g");
                imageArray = countDataSource.getAllImagesForSection(section.id);
            }
        }

        countingWidget_i = new ArrayList<>();
        countingWidget_e = new ArrayList<>();
        countingWidgetLH_i = new ArrayList<>();
        countingWidgetLH_e = new ArrayList<>();

        // 1. show section notes if there are any
        if (section.notes != null)
        {
            if (!section.notes.isEmpty())
            {
                NotesWidget section_notes = new NotesWidget(this, null);
                section_notes.setNotes(section.notes);
                section_notes.setFont(fontPref);
                sectionNotesArea1.addView(section_notes);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            vibratorManager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
        else
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // 2. show Head2: species with selection spinner
        if (lhandPref) // if left-handed counting page
            spinner = findViewById(R.id.countHead1SpinnerLH);
        else
            spinner = findViewById(R.id.countHead1Spinner);

        //   get itemPosition of added species by specCode from sharedPreference
        if (!Objects.equals(prefs.getString("new_spec_code", ""), ""))
        {
            specCode = prefs.getString("new_spec_code", "");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("new_spec_code", ""); // clear prefs value after use
            editor.apply();

        }

        if (!Objects.equals(specCode, ""))
        {
            int i = 0;
            while (i <= codeArray.length)
            {
                assert specCode != null;
                if (specCode.equals(codeArray[i]))
                {
                    itemPosition = i;
                    break;
                }
                i++;
            }
            specCode = "";
        }

        CountingWidgetHead1 adapter = new CountingWidgetHead1(this,
            R.layout.widget_counting_head1, idArray, nameArray, nameArrayL, codeArray, imageArray);
        spinner.setAdapter(adapter);
        spinner.setSelection(itemPosition); // from savedInstanceState
        spinnerListener();

        if (awakePref)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    // End of onResume

    // Inflate the menu; this adds items to the action bar if it is present.
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.counting, menu);
        return true;
    }

    // Handle menu selections
    @SuppressLint("QueryPermissionsNeeded")
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will automatically handle clicks
        // on the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) // back button in actionBar
        {
            Intent intent = new Intent(CountingActivity.this, ListSectionActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.menuEditSection)
        {
            disableProximitySensor();

            // store sectionId into SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("section_id", sectionId);
            editor.commit();

            Intent intent = new Intent(CountingActivity.this, EditSectionActivity.class);
            intent.putExtra("inside_of_track", true);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.menuTakePhoto)
        {
            Intent camIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);

            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(camIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
            boolean isIntentSafe = activities.size() > 0;

            if (isIntentSafe)
            {
                String title = getResources().getString(R.string.chooserTitle);
                Intent chooser = Intent.createChooser(camIntent, title);
                if (camIntent.resolveActivity(getPackageManager()) != null)
                {
                    try
                    {
                        startActivity(chooser);
                    } catch (Exception e)
                    {
                        showSnackbarRed(getString(R.string.noPhotoPermit));
                    }
                }
            }
            return true;
        }
        else if (id == R.id.menuClone)
        {
            if (sectionHasTrack)
                showSnackbarRed(getString(R.string.hasTrack));
            else
            {
                // store section_id into SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("section_id", sectionId);
                editor.commit();

                cloneSection();
            }
            return true;
        }
        else if (id == R.id.action_share)
        {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Transekt " + section.name);
            sendIntent.putExtra(Intent.EXTRA_TITLE, "Message by Transekt");
            sendIntent.putExtra(Intent.EXTRA_TEXT, section.notes);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    } // end of onOptionsItemSelected

    // Call CountOptionsActivity with parameters by button in widget_counting_head2.xml
    public void editOptions(View view)
    {
        Intent intent = new Intent(CountingActivity.this, CountOptionsActivity.class);
        intent.putExtra("count_id", iid);
        intent.putExtra("section_id", sectionId);
        intent.putExtra("section_name", section.name);
        intent.putExtra("item_position", itemPosition);
        intent.putExtra("inside_of_track", true);
        startActivity(intent);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        setPrefVariables();
    }

    // Save activity state for getting back to CountingActivity
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putInt("count_id", iid);
        savedInstanceState.putInt("item_position", itemPosition);
        savedInstanceState.putInt("section_id", sectionId);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        disableProximitySensor();

        // save section id in case it is lost on pause
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("section_id", sectionId);
        editor.putInt("count_id", iid);
        editor.apply();

        // close the data sources
        sectionDataSource.close();
        countDataSource.close();
        alertDataSource.close();

        // N.B. a wakelock might not be held, e.g. if someone is using LineageOS and
        //   has denied wakelock permission to TransektCount
        if (awakePref)
        {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
    // end of onPause()

    @Override
    public void onStop()
    {
        super.onStop();
        if (r != null)
            r.stop();
    }

    // Spinner listener
    private void spinnerListener()
    {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long aid)
            {
                try
                {
                    countsFieldHeadArea3.removeAllViews(); // headline internal/external
                    countsField1Area4.removeAllViews();    // internal counts
                    countsFieldHeadArea5.removeAllViews(); // headline for external counts
                    countsField2Area6.removeAllViews();    // external counts
                    speciesRemarkArea7.removeAllViews();   // species remark
                    alertRemarkArea8.removeAllViews();     // alert remark

                    String sid = ((TextView) view.findViewById(R.id.countId)).getText().toString();
                    iid = Integer.parseInt(sid);
                    itemPosition = position; // position of new selected item in Spinner

                    // store new itemPosition into SharedPreferences
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("item_position", itemPosition);
                    editor.commit();

                    count = countDataSource.getCountById(iid);
                    countingScreen(count);
                    if (MyDebug.LOG)
                        Log.d(TAG, "582, SpinnerListener, count id: " + count.id
                            + ", code: " + count.code);
                } catch (Exception e)
                {
                    // Exception may occur when permissions are changed while activity is paused
                    //  or when spinner is rapidly repeatedly pressed
                    if (MyDebug.LOG)
                        Log.e(TAG, "589, SpinnerListener, catch: " + e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                // stub, necessary to make Spinner work correctly when repeatedly used
            }
        });
    }

    // Show rest of widgets for counting screen
    private void countingScreen(Count count)
    {
        // 1. Section remark is set by sectionNotesArea1 in onResume
        // 2. Species line is set by CountingWidgetHead1 in onResume, Spinner

        // 3. Headline Counting Area 1 (internal)
        CountingWidgetHead2 head2 = new CountingWidgetHead2(this, null, true);
        head2.setCountHead2(count);
        countsFieldHeadArea3.addView(head2);

        // 4. counts internal
        if (lhandPref) // if left-handed counting page
        {
            CountingWidgetLhInt widgeti = new CountingWidgetLhInt(this, null);
            widgeti.setCountLHi(count);
            countingWidgetLH_i.add(widgeti);
            countsField1Area4.addView(widgeti);
        }
        else
        {
            CountingWidgetInt widgeti = new CountingWidgetInt(this, null);
            widgeti.setCounti(count);
            countingWidget_i.add(widgeti);
            countsField1Area4.addView(widgeti);
        }

        // 5. Headline Counting Area 2 (external)
        CountingWidgetHead3 head3 = new CountingWidgetHead3(this, null);
        head3.setCountHead3();
        countsFieldHeadArea5.addView(head3);

        // 6. counts external
        if (lhandPref) // if left-handed counting page
        {
            CountingWidgetLhExt widgete = new CountingWidgetLhExt(this, null);
            widgete.setCountLHe(count);
            countingWidgetLH_e.add(widgete);
            countsField2Area6.addView(widgete);
        }
        else
        {
            CountingWidgetExt widgete = new CountingWidgetExt(this, null);
            widgete.setCounte(count);
            countingWidget_e.add(widgete);
            countsField2Area6.addView(widgete);
        }

        // 7. species note widget if there are any notes
        if (isNotBlank(count.notes))
        {
            NotesWidget count_notes = new NotesWidget(this, null);
            count_notes.setNotes(count.notes);
            count_notes.setFont(fontPref);
            speciesRemarkArea7.addView(count_notes);
        }

        // 8. species alerts note widget if there are any alert notes to show
        List<String> alertExtras = new ArrayList<>();
        alerts = new ArrayList<>();
        List<Alert> tmpAlerts = alertDataSource.getAllAlertsForCount(count.id);

        for (Alert a : tmpAlerts)
        {
            alerts.add(a);
            alertExtras.add(String.format(getString(R.string.willAlert), count.name, a.alert));
        }

        if (!alertExtras.isEmpty())
        {
            NotesWidget alertNotes = new NotesWidget(this, null);
            alertNotes.setNotes(join(alertExtras, "\n"));
            alertNotes.setFont(fontPref);
            alertRemarkArea8.addView(alertNotes);
        }
    }
    // end of countingScreen

    /************************************************************
     * The following 4 functions get a referenced counting widget
     */
    // countingWidget_i (internal, right-handed)
    private CountingWidgetInt getCountFromId_i(int id)
    {
        for (CountingWidgetInt widget : countingWidget_i)
        {
            assert widget.count != null;
            if (widget.count.id == id)
                return widget;
        }
        return null;
    }

    // countingWidgetLH_i (internal, left-handed)
    private CountingWidgetLhInt getCountFromIdLH_i(int id)
    {
        for (CountingWidgetLhInt widget : countingWidgetLH_i)
        {
            assert widget.count != null;
            if (widget.count.id == id)
                return widget;
        }
        return null;
    }

    // countingWidget_e (external, right-handed)
    private CountingWidgetExt getCountFromId_e(int id)
    {
        for (CountingWidgetExt widget : countingWidget_e)
        {
            assert widget.count != null;
            if (widget.count.id == id)
                return widget;
        }
        return null;
    }

    // countingWidgetLH_e (external, left-handed)
    private CountingWidgetLhExt getCountFromIdLH_e(int id)
    {
        for (CountingWidgetLhExt widget : countingWidgetLH_e)
        {
            assert widget.count != null;
            if (widget.count.id == id)
                return widget;
        }
        return null;
    }

    /*****************************************************************
     * The functions below are triggered by the count buttons
     * and righthand/lefthand (LH) views
     * <p>
     * countUpf1i is triggered by buttonUpf1i in widget_counting_i.xml
     */
    public void countUpf1i(View view)
    {
        // run dummy activity to fix spinner's 1. misbehaviour:
        //  no action by 1st click when previous species selected again
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());
        if (MyDebug.LOG)
            Log.d(TAG, "743, countUpf1i, section Id: " + sectionId + ", count Id: " + tempCountId);

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null)
        {
            // Desperate workaround for spinner's 2. misbehaviour: 
            // When returning from species that got no count to previous selected species: 
            //   1st count button press is ignored,
            //   so use button sound only for 2nd press when actually counted
            // ToDo: instead of workaround complete fix by spinner replacement
            oldCounter = count.count_f1i;
            widget.countUpf1i(); // count up and set value on screen
            assert widget.count != null;
            newCounter = widget.count.count_f1i;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f1i = newCounter;
                soundButtonSound();
                buttonVib();
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i
                    + widget.count.count_f2i + widget.count.count_f3i);

                // save the data
                countDataSource.saveCountf1i(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHf1i(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f1i;
            widget.countUpLHf1i();
            assert widget.count != null;
            newCounter = widget.count.count_f1i;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f1i = newCounter;
                soundButtonSound();
                buttonVib();
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);

                countDataSource.saveCountf1i(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownf1i(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());
        if (MyDebug.LOG)
            Log.d(TAG, "804, countDownf1i, section Id: " + sectionId + ", tempCountId: " + tempCountId);

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f1i;
            widget.countDownf1i();
            assert widget.count != null;
            newCounter = widget.count.count_f1i;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_f1i = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf1i(count);
            }
        }
    }

    public void countDownLHf1i(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f1i;
            widget.countDownLHf1i();
            assert widget.count != null;
            newCounter = widget.count.count_f1i;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_f1i = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf1i(count);
            }
        }
    }

    public void countUpf2i(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f2i;
            widget.countUpf2i();
            assert widget.count != null;
            newCounter = widget.count.count_f2i;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f2i = newCounter;
                soundButtonSound();
                buttonVib();
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);
                countDataSource.saveCountf2i(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHf2i(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f2i;
            widget.countUpLHf2i();
            assert widget.count != null;
            newCounter = widget.count.count_f2i;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f2i = newCounter;
                soundButtonSound();
                buttonVib();
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);
                countDataSource.saveCountf2i(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownf2i(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f2i;
            widget.countDownf2i();
            assert widget.count != null;
            newCounter = widget.count.count_f2i;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_f2i = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf2i(count);
            }
        }
    }

    public void countDownLHf2i(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f2i;
            widget.countDownLHf2i();
            assert widget.count != null;
            newCounter = widget.count.count_f2i;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_f2i = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf2i(count);
            }
        }
    }

    public void countUpf3i(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f3i;
            widget.countUpf3i();
            assert widget.count != null;
            newCounter = widget.count.count_f3i;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f3i = newCounter;
                soundButtonSound();
                buttonVib();
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);
                countDataSource.saveCountf3i(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHf3i(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f3i;
            widget.countUpLHf3i();
            assert widget.count != null;
            newCounter = widget.count.count_f3i;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f3i = newCounter;
                soundButtonSound();
                buttonVib();
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);
                countDataSource.saveCountf3i(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownf3i(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f3i;
            widget.countDownf3i();
            assert widget.count != null;
            newCounter = widget.count.count_f3i;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_f3i = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf3i(count);
            }
        }
    }

    public void countDownLHf3i(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f3i;
            widget.countDownLHf3i();
            assert widget.count != null;
            newCounter = widget.count.count_f3i;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_f3i = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf3i(count);
            }
        }
    }

    public void countUppi(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_pi;
            widget.countUppi();
            assert widget.count != null;
            newCounter = widget.count.count_pi;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_pi = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountpi(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHpi(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_pi;
            widget.countUpLHpi();
            assert widget.count != null;
            newCounter = widget.count.count_pi;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_pi = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountpi(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownpi(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_pi;
            widget.countDownpi();
            assert widget.count != null;
            newCounter = widget.count.count_pi;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_pi = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountpi(count);
            }
        }
    }

    public void countDownLHpi(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_pi;
            widget.countDownLHpi();
            assert widget.count != null;
            newCounter = widget.count.count_pi;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_pi = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountpi(count);
            }
        }
    }

    public void countUpli(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_li;
            widget.countUpli();
            assert widget.count != null;
            newCounter = widget.count.count_li;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_li = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountli(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHli(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_li;
            widget.countUpLHli();
            assert widget.count != null;
            newCounter = widget.count.count_li;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_li = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountli(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownli(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_li;
            widget.countDownli();
            assert widget.count != null;
            newCounter = widget.count.count_li;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_li = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountli(count);
            }
        }
    }

    public void countDownLHli(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_li;
            widget.countDownLHli();
            assert widget.count != null;
            newCounter = widget.count.count_li;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_li = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountli(count);
            }
        }
    }

    public void countUpei(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_ei;
            widget.countUpei();
            assert widget.count != null;
            newCounter = widget.count.count_ei;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_ei = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountei(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHei(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_ei;
            widget.countUpLHei();
            assert widget.count != null;
            newCounter = widget.count.count_ei;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_ei = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountei(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownei(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetInt widget = getCountFromId_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_ei;
            widget.countDownei();
            assert widget.count != null;
            newCounter = widget.count.count_ei;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_ei = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountei(count);
            }
        }
    }

    public void countDownLHei(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhInt widget = getCountFromIdLH_i(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_ei;
            widget.countDownLHei();
            assert widget.count != null;
            newCounter = widget.count.count_ei;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_ei = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountei(count);
            }
        }
    }

    // count functions for external counters
    public void countUpf1e(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        if (MyDebug.LOG)
            Log.d(TAG, "1310, countUpf1e, section Id: " + sectionId + ", tempCountId: " + tempCountId);

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f1e;
            widget.countUpf1e();
            assert widget.count != null;
            newCounter = widget.count.count_f1e;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f1e = newCounter;
                soundButtonSound();
                buttonVib();

                // save the data
                countDataSource.saveCountf1e(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHf1e(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f1e;
            widget.countUpLHf1e();
            assert widget.count != null;
            newCounter = widget.count.count_f1e;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f1e = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountf1e(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownf1e(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        if (MyDebug.LOG)
            Log.d(TAG, "1361, countDownf1e, section Id: " + sectionId + ", tempCountId: " + tempCountId);

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f1e;
            widget.countDownf1e();
            assert widget.count != null;
            newCounter = widget.count.count_f1e;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_f1e = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf1e(count);
            }
        }
    }

    public void countDownLHf1e(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f1e;
            widget.countDownLHf1e();
            assert widget.count != null;
            newCounter = widget.count.count_f1e;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_f1e = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf1e(count);
            }
        }
    }

    public void countUpf2e(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f2e;
            widget.countUpf2e();
            assert widget.count != null;
            newCounter = widget.count.count_f2e;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f2e = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountf2e(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHf2e(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f2e;
            widget.countUpLHf2e();
            assert widget.count != null;
            newCounter = widget.count.count_f2e;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f2e = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountf2e(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownf2e(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f2e;
            widget.countDownf2e();
            assert widget.count != null;
            newCounter = widget.count.count_f2e;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_f2e = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf2e(count);
            }
        }
    }

    public void countDownLHf2e(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f2e;
            widget.countDownLHf2e();
            assert widget.count != null;
            newCounter = widget.count.count_f2e;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_f2e = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf2e(count);
            }
        }
    }

    public void countUpf3e(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f3e;
            widget.countUpf3e();
            assert widget.count != null;
            newCounter = widget.count.count_f3e;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f3e = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountf3e(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHf3e(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f3e;
            widget.countUpLHf3e();
            assert widget.count != null;
            newCounter = widget.count.count_f3e;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_f3e = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountf3e(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownf3e(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f3e;
            widget.countDownf3e();
            assert widget.count != null;
            newCounter = widget.count.count_f3e;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_f3e = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf3e(count);
            }
        }
    }

    public void countDownLHf3e(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_f3e;
            widget.countDownLHf3e();
            assert widget.count != null;
            newCounter = widget.count.count_f3e;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_f3e = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf3e(count);
            }
        }
    }

    public void countUppe(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_pe;
            widget.countUppe();
            assert widget.count != null;
            newCounter = widget.count.count_pe;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_pe = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountpe(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHpe(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_pe;
            widget.countUpLHpe();
            assert widget.count != null;
            newCounter = widget.count.count_pe;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_pe = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountpe(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownpe(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_pe;
            widget.countDownpe();
            assert widget.count != null;
            newCounter = widget.count.count_pe;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_pe = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountpe(count);
            }
        }
    }

    public void countDownLHpe(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_pe;
            widget.countDownLHpe();
            assert widget.count != null;
            newCounter = widget.count.count_pe;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_pe = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountpe(count);
            }
        }
    }

    public void countUple(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_le;
            widget.countUple();
            assert widget.count != null;
            newCounter = widget.count.count_le;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_le = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountle(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHle(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_le;
            widget.countUpLHle();
            assert widget.count != null;
            newCounter = widget.count.count_le;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_le = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountle(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownle(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_le;
            widget.countDownle();
            assert widget.count != null;
            newCounter = widget.count.count_le;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_le = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountle(count);
            }
        }
    }

    public void countDownLHle(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_le;
            widget.countDownLHle();
            assert widget.count != null;
            newCounter = widget.count.count_le;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_le = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountle(count);
            }
        }
    }

    public void countUpee(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_ee;
            widget.countUpee();
            assert widget.count != null;
            newCounter = widget.count.count_ee;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_ee = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountee(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHee(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_ee;
            widget.countUpLHee();
            assert widget.count != null;
            newCounter = widget.count.count_ee;
            if (newCounter > oldCounter) // has actually counted up
            {
                count.count_ee = newCounter;
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountee(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownee(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetExt widget = getCountFromId_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_ee;
            widget.countDownee();
            assert widget.count != null;
            newCounter = widget.count.count_ee;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_ee = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountee(count);
            }
        }
    }

    public void countDownLHee(View view)
    {
        dummy();
        int tempCountId = Integer.parseInt(view.getTag().toString());

        CountingWidgetLhExt widget = getCountFromIdLH_e(tempCountId);
        if (widget != null)
        {
            oldCounter = count.count_ee;
            widget.countDownLHee();
            assert widget.count != null;
            newCounter = widget.count.count_ee;
            if (newCounter < oldCounter || newCounter == 0)
            {
                count.count_ee = newCounter;
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountee(count);
            }
        }
    }
    // end of counters
    /*****************/

    // Call DummyActivity to overcome Spinner deficiency for repeated item
    public void dummy()
    {
        Intent intent = new Intent(CountingActivity.this, DummyActivity.class);
        intent.putExtra("auto_section", false);
        startActivity(intent);
    }

    // alert checking...
    private void checkAlert(int countId, int count_value)
    {
        for (Alert a : alerts)
        {
            if (a.count_id == countId && a.alert == count_value)
            {
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
    private void soundAlert()
    {
        if (alertSoundPref)
        {
            try
            {
                Uri notification;
                if (isNotBlank(alertSound) && alertSound != null)
                    notification = Uri.parse(alertSound);
                else
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
//                new Handler().postDelayed(r::stop, 1000);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    // If the user has set the preference for button sound, then sound it here.
    private void soundButtonSound()
    {
        if (buttonSoundPref)
        {
            try
            {
                Uri notification;
                if (isNotBlank(buttonSound) && buttonSound != null)
                    notification = Uri.parse(buttonSound);
                else
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
//                new Handler().postDelayed(r::stop, 300);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private void soundButtonSoundMinus()
    {
        if (buttonSoundPref)
        {
            try
            {
                Uri notification;
                if (isNotBlank(buttonSoundMinus) && buttonSoundMinus != null)
                    notification = Uri.parse(buttonSoundMinus);
                else
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
//                new Handler().postDelayed(r::stop, 400);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private void buttonVib()
    {
        if (buttonVibPref)
        {
            try
            {
                if (Build.VERSION.SDK_INT >= 31)
                {
                    vibratorManager.getDefaultVibrator();
                    vibratorManager.cancel();
                }
                else
                {
                    if (Build.VERSION.SDK_INT >= 26)
                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                    else
                        vibrator.vibrate(100);
                    vibrator.cancel();
                }
            } catch (Exception e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "1970, buttonVib, catch, could not vibrate.", e);
            }
        }
    }

    private void buttonVibLong()
    {
        if (buttonVibPref)
        {
            try
            {
                if (Build.VERSION.SDK_INT >= 31)
                {
                    vibratorManager.getDefaultVibrator();
                    vibratorManager.cancel();
                }
                else
                {
                    if (Build.VERSION.SDK_INT >= 26)
                        vibrator.vibrate(VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE));
                    else
                        vibrator.vibrate(450);
                    vibrator.cancel();
                }
            } catch (Exception e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "1997, buttonVib, catch, could not vibrate.", e);
            }
        }
    }

    // cloneSection() with check for double names
    private void cloneSection()
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dpSectTitle));

        // Set up the input
        final EditText input = new EditText(this);

        // Specify the type of input expected
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) ->
        {
            // enter new section title
            String sect_name = input.getText().toString();

            // check for empty section name
            if (sect_name.isEmpty())
            {
                showSnackbarRed(getString(R.string.newName));
                return;
            }

            // check if this is not a duplicate of an existing name
            if (compSectionNames(sect_name))
            {
                showSnackbarRed(sect_name + " " + getString(R.string.isdouble));
                return;
            }

            // check if section is contiguous
            int entries = -1, maxId = 0;
            try
            {
                entries = sectionDataSource.getNumEntries();
            } catch (Exception e)
            {
                if (MyDebug.LOG) showSnackbarRed("getNumEntries failed");
            }

            try
            {
                maxId = sectionDataSource.getMaxId();
            } catch (Exception e)
            {
                if (MyDebug.LOG) showSnackbarRed("getMaxId failed");
            }

            if (entries != maxId)
            {
                showSnackbarRed(getString(R.string.notContiguous));
                if (MyDebug.LOG)
                    showSnackbarRed("maxId: " + maxId + ", entries: " + entries);
                return;
            }

            // Creating the new section
            Section newSection = sectionDataSource.createSection(sect_name);
            newSection.notes = section.notes;
            sectionDataSource.saveSection(newSection);
            for (Count c : countDataSource.getAllCountsForSection(sectionId))
            {
                Count newCount = countDataSource.createCount(newSection.id, c.name, c.code, c.name_g);
                if (newCount != null)
                {
                    newCount.notes = c.notes;
                    countDataSource.saveCount(newCount);
                }
            }

            // Exit this and go to the list of new sections
            Toast.makeText(CountingActivity.this, sect_name + " " + getString(R.string.newCopyCreated), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(CountingActivity.this, ListSectionActivity.class);
            startActivity(intent);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Compare section names for duplicates and return state of duplicate found
    private boolean compSectionNames(String newname)
    {
        boolean isDblName = false;
        String sname;

        List<Section> sectionList = sectionDataSource.getAllSections(prefs);

        int childcount = sectionList.size() + 1;
        // for all Sections
        for (int i = 1; i < childcount; i++)
        {
            section = sectionDataSource.getSection(i);
            sname = section.name;
            if (MyDebug.LOG) Log.d(TAG, "2100, compSectionNames, sname = " + sname);

            if (newname.equals(sname))
            {
                isDblName = true;
                if (MyDebug.LOG) Log.d(TAG, "2105, compSectionNames, Double name = " + sname);
                break;
            }
        }
        return isDblName;
    }

    private void enableProximitySensor()
    {
        if (mProximityWakeLock == null)
            return;

        if (!mProximityWakeLock.isHeld())
            mProximityWakeLock.acquire(30 * 60 * 1000L /*30 minutes*/);
    }

    // Check for API-Level 21 or above is done previously
    private void disableProximitySensor()
    {
        if (mProximityWakeLock == null)
            return;
        if (mProximityWakeLock.isHeld())
        {
            int flags = PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY;
            mProximityWakeLock.release(flags);
        }
    }

    private void showSnackbarRed(String str)
    {
        View view;
        if (lhandPref) // if left-handed counting page
            view = findViewById(R.id.countingScreenLH);
        else
            view = findViewById(R.id.countingScreen);
        Snackbar sB = Snackbar.make(view, str, Snackbar.LENGTH_LONG);
        sB.setTextColor(Color.RED);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        sB.show();
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
    public static boolean isNotBlank(final CharSequence cs)
    {
        return !isBlank(cs);
    }

    public static boolean isBlank(final CharSequence cs)
    {
        int strLen = cs.length();
        if (cs == null || strLen == 0)
            return true;
        for (int i = 0; i < strLen; i++)
        {
            if (!Character.isWhitespace(cs.charAt(i)))
                return false;
        }
        return true;
    }

    public static String join(Iterator<?> iterator, String separator)
    {
        if (iterator == null)
            return null;
        else if (!iterator.hasNext())
            return "";
        else
        {
            Object first = iterator.next();
            if (!iterator.hasNext())
                return toString(first);
            else
            {
                StringBuilder buf = new StringBuilder(256);
                if (first != null)
                    buf.append(first);

                while (iterator.hasNext())
                {
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

    public static String join(Iterable<?> iterable, String separator)
    {
        return iterable == null ? null : join(iterable.iterator(), separator);
    }

    public static String toString(Object obj)
    {
        return obj == null ? "" : obj.toString();
    }

}
