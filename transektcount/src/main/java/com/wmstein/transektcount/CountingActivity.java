package com.wmstein.transektcount;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.wmstein.transektcount.database.Alert;
import com.wmstein.transektcount.database.AlertDataSource;
import com.wmstein.transektcount.database.Count;
import com.wmstein.transektcount.database.CountDataSource;
import com.wmstein.transektcount.database.DbHelper;
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

/***************************************************************************************************
 * CountingActivity does the actual counting with 12 counters, checks for alerts,
 * calls CountOptionsActivity, calls EditSectionActivity, clones a section,
 * switches screen off when pocketed and lets you send a message.
 * Inspired by milo's CountingActivity.java from 05/05/2014.
 * Modified by wmstein on 18.12.2016
 */
public class CountingActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener 
{
    private static final String TAG = "transektcountCountingActivity";
    private AlertDialog.Builder row_alert;

    private TransektCountApplication transektCount;
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
    private boolean soundPref;
    private boolean buttonSoundPref;
    private String alertSound;
    private String buttonAlertSound;

    private boolean hasChanged = false; // Criteria for storing S_AT_CREATED

    // the actual data
    private Count count;
    private Section section;
    private List<Count> counts;
    private List<Alert> alerts;
    private List<CountingWidget_i> countingWidgets_i;
    private List<CountingWidget_e> countingWidgets_e;
    private List<CountingWidgetLH_i> countingWidgetsLH_i;
    private List<CountingWidgetLH_e> countingWidgetsLH_e;
    //private SpinnerPlus spinnerPlus;
    private Spinner spinner;
    private int itemPosition = 0;

    private String[] idArray;
    private String[] nameArray;
    private String[] codeArray;
    private Integer[] imageArray;

    private SQLiteDatabase database;
    private DbHelper dbHandler;

    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;
    private AlertDataSource alertDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Context context = this.getApplicationContext();

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            section_id = extras.getInt("section_id");
        }

        if (savedInstanceState != null)
        {
            spinner.setSelection(savedInstanceState.getInt("itemPosition", 0));
            iid = savedInstanceState.getInt("count_id");
        }

        sectionDataSource = new SectionDataSource(this);
        countDataSource = new CountDataSource(this);
        alertDataSource = new AlertDataSource(this);

        transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        getPrefs();

        if (lhandPref) // if left-handed counting page
        {
            setContentView(R.layout.activity_counting_lh);
        }
        else
        {
            setContentView(R.layout.activity_counting);
        }

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        if (lhandPref) // if left-handed counting page
        {
            LinearLayout counting_screen = (LinearLayout) findViewById(R.id.countingScreenLH);
            counting_screen.setBackground(transektCount.getBackground());
            notes_area1 = (LinearLayout) findViewById(R.id.sectionNotesLayoutLH);
            head_area2 = (LinearLayout) findViewById(R.id.countHead2LayoutLH);
            count_area_i = (LinearLayout) findViewById(R.id.countCountiLayoutLH);
            head_area3 = (LinearLayout) findViewById(R.id.countHead3LayoutLH);
            count_area_e = (LinearLayout) findViewById(R.id.countCounteLayoutLH);
            notes_area2 = (LinearLayout) findViewById(R.id.countNotesLayoutLH);
            notes_area3 = (LinearLayout) findViewById(R.id.alertNotesLayoutLH);
        }
        else
        {
            LinearLayout counting_screen = (LinearLayout) findViewById(R.id.countingScreen);
            counting_screen.setBackground(transektCount.getBackground());
            notes_area1 = (LinearLayout) findViewById(R.id.sectionNotesLayout);
            head_area2 = (LinearLayout) findViewById(R.id.countHead2Layout);
            count_area_i = (LinearLayout) findViewById(R.id.countCountiLayout);
            head_area3 = (LinearLayout) findViewById(R.id.countHead3Layout);
            count_area_e = (LinearLayout) findViewById(R.id.countCounteLayout);
            notes_area2 = (LinearLayout) findViewById(R.id.countNotesLayout);
            notes_area3 = (LinearLayout) findViewById(R.id.alertNotesLayout);
        }

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        PowerManager mPowerManager;
        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (mPowerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK))
            {
                mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "WAKE LOCK");
            }
            enableProximitySensor();
        }
    }

    /*
     * So preferences can be loaded at the start, and also when a change is detected.
     */
    private void getPrefs()
    {
        awakePref = prefs.getBoolean("pref_awake", true);
        brightPref = prefs.getBoolean("pref_bright", true);
        sortPref = prefs.getString("pref_sort_sp", "none"); // sorted species list on counting page
        fontPref = prefs.getBoolean("pref_note_font", false);
        lhandPref = prefs.getBoolean("pref_left_hand", false); // left-handed counting page
        soundPref = prefs.getBoolean("pref_sound", false);
        alertSound = prefs.getString("alert_sound", null);
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false);
        buttonAlertSound = prefs.getString("alert_button_sound", null);
    }

    @SuppressLint("LongLogTag")
    @Override
    protected void onResume()
    {
        super.onResume();

        Context context = this.getApplicationContext();

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            enableProximitySensor();
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
        Log.i(TAG, "Section ID: " + String.valueOf(section_id));
        try
        {
            section = sectionDataSource.getSection(section_id);
        } catch (CursorIndexOutOfBoundsException e)
        {
            Log.e(TAG, "Problem loading section: " + e.toString());
            Toast.makeText(CountingActivity.this, getString(R.string.getHelp), Toast.LENGTH_LONG).show();
            finish();
        }

        getSupportActionBar().setTitle(section.name);

        switch (sortPref)
        {
        case "names_alpha":
            counts = countDataSource.getAllCountsForSectionSrtName(section.id);
            idArray = countDataSource.getAllIdsForSectionSrtName(section.id);
            nameArray = countDataSource.getAllStringsForSectionSrtName(section.id, "name");
            codeArray = countDataSource.getAllStringsForSectionSrtName(section.id, "code");
            imageArray = countDataSource.getAllImagesForSectionSrtName(section.id);
            break;
        case "codes":
            counts = countDataSource.getAllCountsForSectionSrtCode(section.id);
            idArray = countDataSource.getAllIdsForSectionSrtCode(section.id);
            nameArray = countDataSource.getAllStringsForSectionSrtCode(section.id, "name");
            codeArray = countDataSource.getAllStringsForSectionSrtCode(section.id, "code");
            imageArray = countDataSource.getAllImagesForSectionSrtCode(section.id);
            break;
        default:
            counts = countDataSource.getAllCountsForSection(section.id);
            idArray = countDataSource.getAllIdsForSection(section.id);
            nameArray = countDataSource.getAllStringsForSection(section.id, "name");
            codeArray = countDataSource.getAllStringsForSection(section.id, "code");
            imageArray = countDataSource.getAllImagesForSection(section.id);
            break;
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
            spinner = (Spinner) findViewById(R.id.countHead1SpinnerLH);
        }
        else
        {
            spinner = (Spinner) findViewById(R.id.countHead1Spinner);
        }

        CountingWidget_head1 adapter = new CountingWidget_head1(this,
            R.layout.widget_counting_head1, idArray, nameArray, codeArray, imageArray);
        spinner.setAdapter(adapter);
        spinner.setSelection(itemPosition);

        //spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        spinnerListener();
        
        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    // Spinner listener
    private void spinnerListener()
    {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long aid)
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
                //Toast.makeText(CountingActivity.this, "1. " + count.name, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                // stub
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

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

        // save the data
        //saveData();

        // save section id in case it is lost on pause
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("section_id", section_id);
        editor.putInt("count_id", iid);
        editor.commit();

        // close the data sources
        sectionDataSource.close();
        countDataSource.close();
        alertDataSource.close();

        // N.B. a wakelock might not be held, e.g. if someone is using Cyanogenmod and
        // has denied wakelock permission to transektcount
        if (awakePref)
        {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

    }

    // Save counters to C_COUNT and counting date to S_CREATED_AT
    private void saveData()
    {

        Toast.makeText(CountingActivity.this, getString(R.string.sectSaving) + " " + section.name + "!", Toast.LENGTH_SHORT).show();
        for (Count count : counts)
        {
            countDataSource.saveCount(count);
        }
        if (hasChanged)
        {
            sectionDataSource.saveDateSection(section);
            hasChanged = false;
        }
    }

    public void saveAndExit(View view)
    {
        //saveData();
        if (hasChanged)
        {
            sectionDataSource.saveDateSection(section);
            hasChanged = false;
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

        super.finish();
    }

    /****************************
     * The following functions get a referenced counting widget from the list
     */

    // countingWidgets_i
    private CountingWidget_i getCountFromId_i(int id)
    {
        for (CountingWidget_i widget : countingWidgets_i)
        {
            if (widget.count.id == id)
            {
                return widget;
            }
        }
        return null;
    }

    // countingWidgetsLH_i
    private CountingWidgetLH_i getCountFromIdLH_i(int id)
    {
        for (CountingWidgetLH_i widget : countingWidgetsLH_i)
        {
            if (widget.count.id == id)
            {
                return widget;
            }
        }
        return null;
    }

    // countingWidgets_e
    private CountingWidget_e getCountFromId_e(int id)
    {
        for (CountingWidget_e widget : countingWidgets_e)
        {
            if (widget.count.id == id)
            {
                return widget;
            }
        }
        return null;
    }

    // countingWidgetsLH_e
    private CountingWidgetLH_e getCountFromIdLH_e(int id)
    {
        for (CountingWidgetLH_e widget : countingWidgetsLH_e)
        {
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
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            widget.countUpf1i(); // count up and set value on screen
            //Toast.makeText(CountingActivity.this, "CountUp " + count.count_f1i, Toast.LENGTH_SHORT).show();
            checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);

            // save the data
            countDataSource.saveCountf1i(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countUpLHf1i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            widget.countUpLHf1i();
            checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);

            // save the data
            countDataSource.saveCountf1i(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countDownf1i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            widget.countDownf1i();
            countDataSource.saveCountf1i(count);
        }

        hasChanged = widget.count.count_f1i != 0;
        dummy();
    }

    public void countDownLHf1i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            widget.countDownLHf1i();
            countDataSource.saveCountf1i(count);
        }

        hasChanged = widget.count.count_f1i != 0;
        dummy();
    }

    public void countUpf2i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());

        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            widget.countUpf2i();
            checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);
            countDataSource.saveCountf2i(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countUpLHf2i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            widget.countUpLHf2i();
            checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);
            countDataSource.saveCountf2i(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countDownf2i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            widget.countDownf2i();
            countDataSource.saveCountf2i(count);
        }
        hasChanged = widget.count.count_f2i != 0;
        dummy();
    }

    public void countDownLHf2i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            widget.countDownLHf2i();
            countDataSource.saveCountf2i(count);
        }
        hasChanged = widget.count.count_f2i != 0;
        dummy();
    }

    public void countUpf3i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());

        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            widget.countUpf3i();
            checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);
            countDataSource.saveCountf3i(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countUpLHf3i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            widget.countUpLHf3i();
            checkAlert(widget.count.id, widget.count.count_f1i + widget.count.count_f2i + widget.count.count_f3i);
            countDataSource.saveCountf3i(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countDownf3i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            widget.countDownf3i();
            countDataSource.saveCountf3i(count);
        }
        hasChanged = widget.count.count_f3i != 0;
        dummy();
    }

    public void countDownLHf3i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            widget.countDownLHf3i();
            countDataSource.saveCountf3i(count);
        }
        hasChanged = widget.count.count_f3i != 0;
        dummy();
    }

    public void countUppi(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());

        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            widget.countUppi();
            countDataSource.saveCountpi(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countUpLHpi(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            widget.countUpLHpi();
            countDataSource.saveCountpi(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countDownpi(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            widget.countDownpi();
            countDataSource.saveCountpi(count);
        }
        hasChanged = widget.count.count_pi != 0;
        dummy();
    }

    public void countDownLHpi(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            widget.countDownLHpi();
            countDataSource.saveCountpi(count);
        }
        hasChanged = widget.count.count_pi != 0;
        dummy();
    }

    public void countUpli(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());

        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            widget.countUpli();
            countDataSource.saveCountli(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countUpLHli(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            widget.countUpLHli();
            countDataSource.saveCountli(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countDownli(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            widget.countDownli();
            countDataSource.saveCountli(count);
        }
        hasChanged = widget.count.count_li != 0;
        dummy();
    }

    public void countDownLHli(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            widget.countDownLHli();
            countDataSource.saveCountli(count);
        }
        hasChanged = widget.count.count_li != 0;
        dummy();
    }

    public void countUpei(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());

        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            widget.countUpei();
            countDataSource.saveCountei(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countUpLHei(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            widget.countUpLHei();
            countDataSource.saveCountei(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countDownei(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget_i widget = getCountFromId_i(count_id);
        if (widget != null)
        {
            widget.countDownei();
            countDataSource.saveCountei(count);
        }
        hasChanged = widget.count.count_ei != 0;
        dummy();
    }

    public void countDownLHei(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_i widget = getCountFromIdLH_i(count_id);
        if (widget != null)
        {
            widget.countDownLHei();
            countDataSource.saveCountei(count);
        }
        hasChanged = widget.count.count_ei != 0;
        dummy();
    }


    // count functions for external counters
    public void countUpf1e(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            widget.countUpf1e();
            countDataSource.saveCountf1e(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countUpLHf1e(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            widget.countUpLHf1e();
            countDataSource.saveCountf1e(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countDownf1e(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            widget.countDownf1e();
            countDataSource.saveCountf1e(count);
        }
        hasChanged = widget.count.count_f1e != 0;
        dummy();
    }

    public void countDownLHf1e(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            widget.countDownLHf1e();
            countDataSource.saveCountf1e(count);
        }
        hasChanged = widget.count.count_f1e != 0;
        dummy();
    }

    public void countUpf2e(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());

        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            widget.countUpf2e();
            countDataSource.saveCountf2e(count);
            sectionDataSource.saveDateSection(section);
        }
        dummy();
        hasChanged = true;
    }

    public void countUpLHf2e(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            widget.countUpLHf2e();
            countDataSource.saveCountf2e(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countDownf2e(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            widget.countDownf2e();
            countDataSource.saveCountf2e(count);
        }
        hasChanged = widget.count.count_f2e != 0;
        dummy();
    }

    public void countDownLHf2e(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            widget.countDownLHf2e();
            countDataSource.saveCountf2e(count);
        }
        hasChanged = widget.count.count_f2e != 0;
        dummy();
    }

    public void countUpf3e(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());

        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            widget.countUpf3e();
            countDataSource.saveCountf3e(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countUpLHf3e(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            widget.countUpLHf3e();
            countDataSource.saveCountf3e(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countDownf3e(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            widget.countDownf3e();
            countDataSource.saveCountf3e(count);
        }

        hasChanged = widget.count.count_f3e != 0;
        dummy();
    }

    public void countDownLHf3e(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            widget.countDownLHf3e();
            countDataSource.saveCountf3e(count);
        }

        hasChanged = widget.count.count_f3e != 0;
        dummy();
    }

    public void countUppe(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());

        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            widget.countUppe();
            countDataSource.saveCountpe(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countUpLHpe(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            widget.countUpLHpe();
            countDataSource.saveCountpe(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countDownpe(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            widget.countDownpe();
            countDataSource.saveCountpe(count);
        }

        hasChanged = widget.count.count_pe != 0;
        dummy();
    }

    public void countDownLHpe(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            widget.countDownLHpe();
            countDataSource.saveCountpe(count);
        }

        hasChanged = widget.count.count_pe != 0;
        dummy();
    }

    public void countUple(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());

        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            widget.countUple();
            countDataSource.saveCountle(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countUpLHle(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            widget.countUpLHle();
            countDataSource.saveCountle(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countDownle(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            widget.countDownle();
            countDataSource.saveCountle(count);
        }

        hasChanged = widget.count.count_le != 0;
        dummy();
    }

    public void countDownLHle(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            widget.countDownLHle();
            countDataSource.saveCountle(count);
        }

        hasChanged = widget.count.count_le != 0;
        dummy();
    }

    public void countUpee(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());

        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            widget.countUpee();
            countDataSource.saveCountee(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countUpLHee(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            widget.countUpLHee();
            countDataSource.saveCountee(count);
            sectionDataSource.saveDateSection(section);
        }
        hasChanged = true;
        dummy();
    }

    public void countDownee(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget_e widget = getCountFromId_e(count_id);
        if (widget != null)
        {
            widget.countDownee();
            countDataSource.saveCountee(count);
        }

        hasChanged = widget.count.count_ee != 0;
        dummy();
    }

    public void countDownLHee(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH_e widget = getCountFromIdLH_e(count_id);
        if (widget != null)
        {
            widget.countDownLHee();
            countDataSource.saveCountee(count);
        }

        hasChanged = widget.count.count_ee != 0;
        dummy();
    }

    // Call CountOptionsActivity with count_id and section_id
    public void edit(View view)
    {
        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

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
        //intent.putExtra("count_id", iid);
        //intent.putExtra("section_id", section_id);
        //intent.putExtra("section_name", section.name);
        //intent.putExtra("itemposition", spinner.getSelectedItemPosition());
        startActivity(intent);
    }
    
    // Save activity state for getting back to CountingActivity
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
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
                row_alert = new AlertDialog.Builder(this);
//                row_alert.setTitle(getString(R.string.alertTitle));
                row_alert.setTitle(String.format(getString(R.string.alertTitle), count_value));
                row_alert.setMessage(a.alert_text);
                row_alert.setNegativeButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        // Cancelled.
                    }
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
        if (soundPref)
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

    private void buttonSound()
    {
        if (buttonSoundPref)
        {
            try
            {
                Uri notification;
                if (isNotBlank(buttonAlertSound) && buttonAlertSound != null)
                {
                    notification = Uri.parse(buttonAlertSound);
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

    // Inflate the menu; this adds items to the action bar if it is present.
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.counting, menu);
        return true;
    }

    // Handle menu selections
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will automatically handle clicks 
        // on the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.menuEditSection)
        {
            // check for API-Level >= 21
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                disableProximitySensor(true);
            }

            Intent intent = new Intent(CountingActivity.this, EditSectionActivity.class);
            intent.putExtra("section_id", section_id);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.menuClone)
        {
            cloneSection();
            return true;
        }
        else if (id == R.id.action_share)
        {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, section.notes);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, section.name);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        getPrefs();
    }

    // cloneSection() with check for double names
    // modified by wmstein on 10.04.2016
    private void cloneSection()
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dpSectTitle));

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
/*        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            input.setShowSoftInputOnFocus(true);
        }
*/
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // enter new section title
                String m_Text = input.getText().toString();

                //check if this is not empty 
                if (m_Text.isEmpty())
                {
                    Toast.makeText(CountingActivity.this, getString(R.string.newName), Toast.LENGTH_SHORT).show();
                    return;
                }

                //check if this is not a duplicate of an existing name
                if (compSectionNames(m_Text))
                {
                    Toast.makeText(CountingActivity.this, m_Text + " " + getString(R.string.isdouble), Toast.LENGTH_SHORT).show();
                    return;
                }

                // Creating the new section
                Section newSection = sectionDataSource.createSection(m_Text);
                newSection.notes = section.notes;
                sectionDataSource.saveSection(newSection);
                for (Count c : countDataSource.getAllCountsForSection(section_id))
                {
                    Count newCount = countDataSource.createCount(newSection.id, c.name, c.code);
                    newCount.notes = c.notes;
                    countDataSource.saveCount(newCount);
                }

                // Exit this and go to the list of new sections
                Toast.makeText(CountingActivity.this, m_Text + " " + getString(R.string.newCopyCreated), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(CountingActivity.this, ListSectionActivity.class);
                startActivity(intent);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        builder.show();
    }

    // Compare section names for duplicates and return state of duplicate found
    // created by wmstein on 10.04.2016
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
            //Log.i(TAG, "sname = " + sname);
            if (newname.equals(sname))
            {
                isDblName = true;
                //Log.i(TAG, "Double name = " + sname);
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
            mProximityWakeLock.acquire();
        }
    }

    // Check for API-Level 21 or above is done previously
    @SuppressLint("NewApi")
    private void disableProximitySensor(boolean waitForFarState)
    {
        if (mProximityWakeLock == null)
        {
            return;
        }
        if (mProximityWakeLock.isHeld())
        {
            int flags = waitForFarState ? PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY : 0;
            mProximityWakeLock.release(flags);
        }
    }

    /**
     * Following functions are taken from the Apache commons-lang3-3.4 library
     * licensed under Apache License Version 2.0, January 2004
     * <p>
     * Checks if a CharSequence is whitespace, empty ("") or null
     * <p/>
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
     * <p/>
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
