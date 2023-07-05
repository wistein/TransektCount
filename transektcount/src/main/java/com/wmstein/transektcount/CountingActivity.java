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
import com.wmstein.transektcount.widgets.CountingWidgetLH_e;
import com.wmstein.transektcount.widgets.CountingWidgetLH_i;
import com.wmstein.transektcount.widgets.CountingWidget_e;
import com.wmstein.transektcount.widgets.CountingWidget_head1;
import com.wmstein.transektcount.widgets.CountingWidget_head2;
import com.wmstein.transektcount.widgets.CountingWidget_head3;
import com.wmstein.transektcount.widgets.CountingWidget_i;
import com.wmstein.transektcount.widgets.NotesWidget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/***************************************************************************************************
 * CountingActivity does the actual counting on portrait layout with 12 counters, 
 * checks for alerts, calls CountOptionsActivity, EditSectionActivity and DummyActivity, clones a section,
 * switches screen off when device is pocketed and lets you send a message.
 <p>
 * Inspired by milo's CountingActivity.java of BeeCount from 05/05/2014.
 * Changes and additions for TransektCount by wmstein since 18.02.2016
 * Last edit on 2023-07-01
 */
public class CountingActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "transektcountCntAct";

    private SharedPreferences prefs;

    private int section_id;
    private int iid = 1;
    private LinearLayout notes_area1;
    private LinearLayout head_area2;
    private LinearLayout count_area_i;
    private LinearLayout head_area3;
    private LinearLayout count_area_e;
    private LinearLayout notes_area2;
    private LinearLayout notes_area3;

    // Proximity sensor handling for screen on/off
    private PowerManager.WakeLock mProximityWakeLock;

    // preferences
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

    // the actual data
    private Count count;
    private Section section;
    private List<Alert> alerts;
    private List<CountingWidget_i> countingWidgets_i;
    private List<CountingWidget_e> countingWidgets_e;
    private List<CountingWidgetLH_i> countingWidgetsLH_i;
    private List<CountingWidgetLH_e> countingWidgetsLH_e;
    private Spinner spinner;
    private int itemPosition = 0;
    private int oldCount;

    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;
    private AlertDataSource alertDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Context context = this.getApplicationContext();

        TransektCountApplication transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        getPrefs();

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            section_id = extras.getInt("section_id");
        }

        sectionDataSource = new SectionDataSource(this);
        countDataSource = new CountDataSource(this);
        alertDataSource = new AlertDataSource(this);

        if (lhandPref) // if left-handed counting page
        {
            setContentView(R.layout.activity_counting_lh);
            LinearLayout counting_screen = findViewById(R.id.countingScreenLH);
            counting_screen.setBackground(transektCount.getBackground());
            notes_area1 = findViewById(R.id.sectionNotesLayoutLH);
            head_area2 = findViewById(R.id.countHead2LayoutLH);
            count_area_i = findViewById(R.id.countCountiLayoutLH);
            head_area3 = findViewById(R.id.countHead3LayoutLH);
            count_area_e = findViewById(R.id.countCounteLayoutLH);
            notes_area2 = findViewById(R.id.countNotesLayoutLH);
            notes_area3 = findViewById(R.id.alertNotesLayoutLH);
        }
        else
        {
            setContentView(R.layout.activity_counting);
            LinearLayout counting_screen = findViewById(R.id.countingScreen);
            counting_screen.setBackground(transektCount.getBackground());
            notes_area1 = findViewById(R.id.sectionNotesLayout);
            head_area2 = findViewById(R.id.countHead2Layout);
            count_area_i = findViewById(R.id.countCountiLayout);
            head_area3 = findViewById(R.id.countHead3Layout);
            count_area_e = findViewById(R.id.countCounteLayout);
            notes_area2 = findViewById(R.id.countNotesLayout);
            notes_area3 = findViewById(R.id.alertNotesLayout);
        }

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        if (savedInstanceState != null)
        {
            spinner.setSelection(savedInstanceState.getInt("itemPosition", 0));
            iid = savedInstanceState.getInt("count_id");
        }

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        PowerManager mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        try
        {
            assert mPowerManager != null;
            if (mPowerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK))
            {
                mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "TransektCount:WAKELOCK");
            }
            enableProximitySensor();
        } catch (NullPointerException e)
        {
            // do nothing
        }
    } // End of onCreate

    // Used to load preferences at the start, and also when a change is detected.
    private void getPrefs()
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
    }

    @SuppressLint("LongLogTag")
    @Override
    protected void onResume()
    {
        super.onResume();

        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        getPrefs();

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            section_id = extras.getInt("section_id");
            iid = extras.getInt("count_id");
        }

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
        // clear any existing views
        notes_area1.removeAllViews();
        head_area2.removeAllViews();
        count_area_i.removeAllViews();
        head_area3.removeAllViews();
        count_area_e.removeAllViews();
        notes_area2.removeAllViews();
        notes_area3.removeAllViews();

        // setup the data sources
        sectionDataSource.open();
        countDataSource.open();
        alertDataSource.open();

        // load and show the data
        if (MyDebug.LOG)
            Log.d(TAG, "Section ID: " + section_id);

        try
        {
            section = sectionDataSource.getSection(section_id);
        } catch (CursorIndexOutOfBoundsException e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "Problem loading section: " + e);
            showSnackbarRed(getString(R.string.getHelp));
            finish();
        }

        try
        {
            Objects.requireNonNull(getSupportActionBar()).setTitle(section.name);
        } catch (NullPointerException e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "Problem setting title bar: " + e);
        }

        String[] idArray;
        String[] nameArray;
        String[] nameArrayG;
        String[] codeArray;
        Integer[] imageArray;

        switch (sortPref)
        {
            case "names_alpha" ->
            {
                idArray = countDataSource.getAllIdsForSectionSrtName(section.id);
                nameArray = countDataSource.getAllStringsForSectionSrtName(section.id, "name");
                codeArray = countDataSource.getAllStringsForSectionSrtName(section.id, "code");
                nameArrayG = countDataSource.getAllStringsForSectionSrtName(section.id, "name_g");
                imageArray = countDataSource.getAllImagesForSectionSrtName(section.id);
            }
            case "codes" ->
            {
                idArray = countDataSource.getAllIdsForSectionSrtCode(section.id);
                nameArray = countDataSource.getAllStringsForSectionSrtCode(section.id, "name");
                codeArray = countDataSource.getAllStringsForSectionSrtCode(section.id, "code");
                nameArrayG = countDataSource.getAllStringsForSectionSrtCode(section.id, "name_g");
                imageArray = countDataSource.getAllImagesForSectionSrtCode(section.id);
            }
            default ->
            {
                idArray = countDataSource.getAllIdsForSection(section.id);
                nameArray = countDataSource.getAllStringsForSection(section.id, "name");
                codeArray = countDataSource.getAllStringsForSection(section.id, "code");
                nameArrayG = countDataSource.getAllStringsForSection(section.id, "name_g");
                imageArray = countDataSource.getAllImagesForSection(section.id);
            }
        }

        countingWidgets_i = new ArrayList<>();
        countingWidgets_e = new ArrayList<>();
        countingWidgetsLH_i = new ArrayList<>();
        countingWidgetsLH_e = new ArrayList<>();

        // 1. section
        // display section notes if there are any
        if (section.notes != null)
        {
            if (!section.notes.isEmpty())
            {
                NotesWidget section_notes = new NotesWidget(this, null);
                section_notes.setNotes(section.notes);
                section_notes.setFont(fontPref);
                notes_area1.addView(section_notes);
            }
        }

        // 2. Head1, species selection spinner
        if (lhandPref) // if left-handed counting page
        {
            spinner = findViewById(R.id.countHead1SpinnerLH);
        }
        else
        {
            spinner = findViewById(R.id.countHead1Spinner);
        }

        CountingWidget_head1 adapter = new CountingWidget_head1(this,
            R.layout.widget_counting_head1, idArray, nameArray, nameArrayG, codeArray, imageArray);
        spinner.setAdapter(adapter);
        spinner.setSelection(itemPosition);
        spinnerListener();

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    } // End of onResume

    private void showSnackbarRed(String str)
    {
        View view;
        if (lhandPref) // if left-handed counting page
        {
            view = findViewById(R.id.countingScreenLH);
        }
        else
        {
            view = findViewById(R.id.countingScreen);
        }
        Snackbar sB = Snackbar.make(view, str, Snackbar.LENGTH_LONG);
        sB.setTextColor(Color.RED);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        sB.show();
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
                    head_area2.removeAllViews();
                    count_area_i.removeAllViews();
                    head_area3.removeAllViews();
                    count_area_e.removeAllViews();
                    notes_area2.removeAllViews();
                    notes_area3.removeAllViews();

                    String sid = ((TextView) view.findViewById(R.id.countId)).getText().toString();
                    iid = Integer.parseInt(sid);
                    itemPosition = position;

                    count = countDataSource.getCountById(iid);
                    countingScreen(count);
                    if (MyDebug.LOG)
                        Toast.makeText(CountingActivity.this, "1. " + count.name, Toast.LENGTH_SHORT).show();
                } catch (Exception e)
                {
                    // Exception may occur when permissions are changed while activity is paused
                    //  or when spinner is rapidly repeatedly pressed
                    if (MyDebug.LOG)
                        Log.e(TAG, "SpinnerListener: " + e);
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
        // 2. Head2 with edit button
        CountingWidget_head2 head2 = new CountingWidget_head2(this, null);
        head2.setCountHead2(count);
        head_area2.addView(head2);

        // 3. counts internal
        if (lhandPref) // if left-handed counting page
        {
            CountingWidgetLH_i widgeti = new CountingWidgetLH_i(this, null);
            widgeti.setCountLHi(count);
            countingWidgetsLH_i.add(widgeti);
            count_area_i.addView(widgeti);
        }
        else
        {
            CountingWidget_i widgeti = new CountingWidget_i(this, null);
            widgeti.setCounti(count);
            countingWidgets_i.add(widgeti);
            count_area_i.addView(widgeti);
        }

        // 4. Head3
        CountingWidget_head3 head3 = new CountingWidget_head3(this, null);

        head3.setCountHead3();
        head_area3.addView(head3);

        // 5. counts external
        if (lhandPref) // if left-handed counting page
        {
            CountingWidgetLH_e widgete = new CountingWidgetLH_e(this, null);
            widgete.setCountLHe(count);
            countingWidgetsLH_e.add(widgete);
            count_area_e.addView(widgete);
        }
        else
        {
            CountingWidget_e widgete = new CountingWidget_e(this, null);
            widgete.setCounte(count);
            countingWidgets_e.add(widgete);
            count_area_e.addView(widgete);
        }

        // 6. species note widget if there are any notes
        if (isNotBlank(count.notes))
        {
            NotesWidget count_notes = new NotesWidget(this, null);
            count_notes.setNotes(count.notes);
            count_notes.setFont(fontPref);
            notes_area2.addView(count_notes);
        }

        // 7. species alerts note widget if there are any alert notes
        List<String> extras = new ArrayList<>();
        alerts = new ArrayList<>();
        List<Alert> tmpAlerts = alertDataSource.getAllAlertsForCount(count.id);

        for (Alert a : tmpAlerts)
        {
            alerts.add(a);
            extras.add(String.format(getString(R.string.willAlert), count.name, a.alert));
        }

        if (!extras.isEmpty())
        {
            NotesWidget extra_notes = new NotesWidget(this, null);
            extra_notes.setNotes(join(extras, "\n"));
            extra_notes.setFont(fontPref);
            notes_area3.addView(extra_notes);
        }
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onPause()
    {
        super.onPause();

        disableProximitySensor();

        // save section id in case it is lost on pause
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("section_id", section_id);
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

    /****************************
     * The following functions get a referenced counting widget from the list
     */

    // countingWidgets_i (internal, right-handed)
    private CountingWidget_i getCountFromId_i(int id)
    {
        for (CountingWidget_i widget : countingWidgets_i)
        {
            assert widget.count != null;
            if (widget.count.id == id)
            {
                return widget;
            }
        }
        return null;
    }

    // countingWidgetsLH_i (internal, left-handed)
    private CountingWidgetLH_i getCountFromIdLH_i(int id)
    {
        for (CountingWidgetLH_i widget : countingWidgetsLH_i)
        {
            assert widget.count != null;
            if (widget.count.id == id)
            {
                return widget;
            }
        }
        return null;
    }

    // countingWidgets_e (external, right-handed)
    private CountingWidget_e getCountFromId_e(int id)
    {
        for (CountingWidget_e widget : countingWidgets_e)
        {
            assert widget.count != null;
            if (widget.count.id == id)
            {
                return widget;
            }
        }
        return null;
    }

    // countingWidgetsLH_e (external, left-handed)
    private CountingWidgetLH_e getCountFromIdLH_e(int id)
    {
        for (CountingWidgetLH_e widget : countingWidgetsLH_e)
        {
            assert widget.count != null;
            if (widget.count.id == id)
            {
                return widget;
            }
        }
        return null;
    }

    /************************
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

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            // Desperate workaround for spinner's 2. misbehaviour: 
            // When returning from species that got no count to previous selected species: 
            // 1st count button press is ignored,
            // so use button sound only for 2nd press when actually counted
            // ToDo: instead of workaround complete fix by spinner replacement
            oldCount = count.count_f1i;
            widget.countUpf1i(); // count up and set value on screen
            if (count.count_f1i > oldCount) // has actually counted up
            {
                soundButtonSound();
                buttonVib();
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);

                // save the data
                countDataSource.saveCountf1i(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHf1i(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_f1i;
            widget.countUpLHf1i();
            if (count.count_f1i > oldCount) // has actually counted up
            {
                soundButtonSound();
                buttonVib();
                assert widget.count != null;
                checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);

                // save the data
                countDataSource.saveCountf1i(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countDownf1i(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_f1i;
            widget.countDownf1i();
            if (count.count_f1i < oldCount || count.count_f1i == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf1i(count);
            }
        }
    }

    public void countDownLHf1i(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_f1i;
            widget.countDownLHf1i();
            if (count.count_f1i < oldCount || count.count_f1i == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf1i(count);
            }
        }
    }

    public void countUpf2i(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_f2i;
            widget.countUpf2i();
            if (count.count_f2i > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_f2i;
            widget.countUpLHf2i();
            if (count.count_f2i > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_f2i;
            widget.countDownf2i();
            if (count.count_f2i < oldCount || count.count_f2i == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf2i(count);
            }
        }
    }

    public void countDownLHf2i(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_f2i;
            widget.countDownLHf2i();
            if (count.count_f2i < oldCount || count.count_f2i == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf2i(count);
            }
        }
    }

    public void countUpf3i(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_f3i;
            widget.countUpf3i();
            if (count.count_f3i > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_f3i;
            widget.countUpLHf3i();
            if (count.count_f3i > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_f3i;
            widget.countDownf3i();
            if (count.count_f3i < oldCount || count.count_f3i == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf3i(count);
            }
        }
    }

    public void countDownLHf3i(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_f3i;
            widget.countDownLHf3i();
            if (count.count_f3i < oldCount || count.count_f3i == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf3i(count);
            }
        }
    }

    public void countUppi(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_pi;
            widget.countUppi();
            if (count.count_pi > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_pi;
            widget.countUpLHpi();
            if (count.count_pi > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_pi;
            widget.countDownpi();
            if (count.count_pi < oldCount || count.count_pi == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountpi(count);
            }
        }
    }

    public void countDownLHpi(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_pi;
            widget.countDownLHpi();
            if (count.count_pi < oldCount || count.count_pi == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountpi(count);
            }
        }
    }

    public void countUpli(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_li;
            widget.countUpli();
            if (count.count_li > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_li;
            widget.countUpLHli();
            if (count.count_li > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_li;
            widget.countDownli();
            if (count.count_li < oldCount || count.count_li == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountli(count);
            }
        }
    }

    public void countDownLHli(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_li;
            widget.countDownLHli();
            if (count.count_li < oldCount || count.count_li == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountli(count);
            }
        }
    }

    public void countUpei(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_ei;
            widget.countUpei();
            if (count.count_ei > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_ei;
            widget.countUpLHei();
            if (count.count_ei > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_ei;
            widget.countDownei();
            if (count.count_ei < oldCount || count.count_ei == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountei(count);
            }
        }
    }

    public void countDownLHei(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            oldCount = count.count_ei;
            widget.countDownLHei();
            if (count.count_ei < oldCount || count.count_ei == 0)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_f1e;
            widget.countUpf1e();
            if (count.count_f1e > oldCount)
            {
                soundButtonSound();
                buttonVib();
                countDataSource.saveCountf1e(count);
                sectionDataSource.saveDateSection(section);
            }
        }
    }

    public void countUpLHf1e(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_f1e;
            widget.countUpLHf1e();
            if (count.count_f1e > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_f1e;
            widget.countDownf1e();
            if (count.count_f1e < oldCount || count.count_f1e == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf1e(count);
            }
        }
    }

    public void countDownLHf1e(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_f1e;
            widget.countDownLHf1e();
            if (count.count_f1e < oldCount || count.count_f1e == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf1e(count);
            }
        }
    }

    public void countUpf2e(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_f2e;
            widget.countUpf2e();
            if (count.count_f2e > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_f2e;
            widget.countUpLHf2e();
            if (count.count_f2e > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_f2e;
            widget.countDownf2e();
            if (count.count_f2e < oldCount || count.count_f2e == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf2e(count);
            }
        }
    }

    public void countDownLHf2e(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_f2e;
            widget.countDownLHf2e();
            if (count.count_f2e < oldCount || count.count_f2e == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf2e(count);
            }
        }
    }

    public void countUpf3e(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_f3e;
            widget.countUpf3e();
            if (count.count_f3e > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_f3e;
            widget.countUpLHf3e();
            if (count.count_f3e > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_f3e;
            widget.countDownf3e();
            if (count.count_f3e < oldCount || count.count_f3e == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf3e(count);
            }
        }
    }

    public void countDownLHf3e(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_f3e;
            widget.countDownLHf3e();
            if (count.count_f3e < oldCount || count.count_f3e == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountf3e(count);
            }
        }
    }

    public void countUppe(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_pe;
            widget.countUppe();
            if (count.count_pe > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_pe;
            widget.countUpLHpe();
            if (count.count_pe > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_pe;
            widget.countDownpe();
            if (count.count_pe < oldCount || count.count_pe == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountpe(count);
            }
        }
    }

    public void countDownLHpe(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_pe;
            widget.countDownLHpe();
            if (count.count_pe < oldCount || count.count_pe == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountpe(count);
            }
        }
    }

    public void countUple(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_le;
            widget.countUple();
            if (count.count_le > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_le;
            widget.countUpLHle();
            if (count.count_le > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_le;
            widget.countDownle();
            if (count.count_le < oldCount || count.count_le == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountle(count);
            }
        }
    }

    public void countDownLHle(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_le;
            widget.countDownLHle();
            if (count.count_le < oldCount || count.count_le == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountle(count);
            }
        }
    }

    public void countUpee(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_ee;
            widget.countUpee();
            if (count.count_ee > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_ee;
            widget.countUpLHee();
            if (count.count_ee > oldCount)
            {
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
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_ee;
            widget.countDownee();
            if (count.count_ee < oldCount || count.count_ee == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountee(count);
            }
        }
    }

    public void countDownLHee(View view)
    {
        dummy();
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            oldCount = count.count_ee;
            widget.countDownLHee();
            if (count.count_ee < oldCount || count.count_ee == 0)
            {
                soundButtonSoundMinus();
                buttonVibLong();
                countDataSource.saveCountee(count);
            }
        }
    }

    // Call CountOptionsActivity with count_id, section_id and itemposition
    public void edit(View view)
    {
        Intent intent = new Intent(CountingActivity.this, CountOptionsActivity.class);
        intent.putExtra("count_id", iid);
        intent.putExtra("section_id", section_id);
        intent.putExtra("itemposition", spinner.getSelectedItemPosition());
        startActivity(intent);
    }

    // Call DummyActivity to overcome Spinner deficiency for repeated item
    public void dummy()
    {
        Intent intent = new Intent(CountingActivity.this, DummyActivity.class);
        startActivity(intent);
    }

    // Save activity state for getting back to CountingActivity
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putInt("count_id", iid);
        savedInstanceState.putInt("itemPosition", spinner.getSelectedItemPosition());
    }

    // alert checking...
    private void checkAlert(int count_id, int count_value)
    {
        for (Alert a : alerts)
        {
            if (a.count_id == count_id && a.alert == count_value)
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
                {
                    notification = Uri.parse(alertSound);
                }
                else
                {
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
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
                {
                    notification = Uri.parse(buttonSound);
                }
                else
                {
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
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
                {
                    notification = Uri.parse(buttonSoundMinus);
                }
                else
                {
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
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
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= 26)
                {
                    vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
                }
                else
                {
                    vibrator.vibrate(150);
                }
            } catch (Exception e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "could not vibrate.", e);
            }
        }
    }

    private void buttonVibLong()
    {
        if (buttonVibPref)
        {
            try
            {
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= 26)
                {
                    vibrator.vibrate(VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE));
                }
                else
                {
                    vibrator.vibrate(450);
                }
            } catch (Exception e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "could not vibrate.", e);
            }
        }
    }

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

        if (id == R.id.menuEditSection)
        {
            disableProximitySensor();

            // store section_id into SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("section_id", section_id);
            editor.commit();
            if (MyDebug.LOG)
                Log.e(TAG, "Sect Id = " + section_id);

            Intent intent = new Intent(CountingActivity.this, EditSectionActivity.class);
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
            // store section_id into SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("section_id", section_id);
            editor.commit();
            if (MyDebug.LOG)
                Log.e(TAG, "Sect Id = " + section_id);

            cloneSection();
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
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        getPrefs();
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
                if (MyDebug.LOG)
                    showSnackbarRed("getNumEntries failed");
            }

            try
            {
                maxId = sectionDataSource.getMaxId();
            } catch (Exception e)
            {
                if (MyDebug.LOG)
                    showSnackbarRed("getMaxId failed");
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
            for (Count c : countDataSource.getAllCountsForSection(section_id))
            {
                Count newCount = countDataSource.createCount(newSection.id, c.name, c.code, c.name_g);
                if (newCount != null)
                {
                    newCount.notes = c.notes;
                    countDataSource.saveCount(newCount);
                }
            }

            sectionDataSource.close();
            countDataSource.close();
            alertDataSource.close();

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
            if (MyDebug.LOG)
                Log.d(TAG, "sname = " + sname);
            if (newname.equals(sname))
            {
                isDblName = true;
                if (MyDebug.LOG)
                    Log.d(TAG, "Double name = " + sname);
                break;
            }
        }
        return isDblName;
    }

    private void enableProximitySensor()
    {
        if (mProximityWakeLock == null)
        {
            return;
        }

        if (!mProximityWakeLock.isHeld())
        {
            mProximityWakeLock.acquire(30 * 60 * 1000L /*30 minutes*/);
        }
    }

    // Check for API-Level 21 or above is done previously
    @SuppressLint("NewApi")
    private void disableProximitySensor()
    {
        if (mProximityWakeLock == null)
        {
            return;
        }
        if (mProximityWakeLock.isHeld())
        {
            int flags = PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY;
            mProximityWakeLock.release(flags);
        }
    }

    /**
     * Following functions are taken from the Apache commons-lang3-3.4 library
     * licensed under Apache License Version 2.0, January 2004
     * <p>
     * Checks if a CharSequence is whitespace, empty ("") or null
     * <p>
     * isBlank(null)      = true
     * isBlank("")        = true
     * isBlank(" ")       = true
     * isBlank("bob")     = false
     * isBlank("  bob  ") = false
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace
     */
    public static boolean isBlank(final CharSequence cs)
    {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0)
        {
            return true;
        }
        for (int i = 0; i < strLen; i++)
        {
            if (!Character.isWhitespace(cs.charAt(i)))
            {
                return false;
            }
        }
        return true;
    }

    /**
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

    public static String join(Iterator<?> iterator, String separator)
    {
        if (iterator == null)
        {
            return null;
        }
        else if (!iterator.hasNext())
        {
            return "";
        }
        else
        {
            Object first = iterator.next();
            if (!iterator.hasNext())
            {
                return toString(first);
            }
            else
            {
                StringBuilder buf = new StringBuilder(256);
                if (first != null)
                {
                    buf.append(first);
                }

                while (iterator.hasNext())
                {
                    if (separator != null)
                    {
                        buf.append(separator);
                    }

                    Object obj = iterator.next();
                    if (obj != null)
                    {
                        buf.append(obj);
                    }
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
