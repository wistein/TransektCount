package com.wmstein.transektcount;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.CursorIndexOutOfBoundsException;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.wmstein.transektcount.database.Alert;
import com.wmstein.transektcount.database.AlertDataSource;
import com.wmstein.transektcount.database.Count;
import com.wmstein.transektcount.database.CountDataSource;
import com.wmstein.transektcount.database.Section;
import com.wmstein.transektcount.database.SectionDataSource;
import com.wmstein.transektcount.widgets.CountingWidget;
import com.wmstein.transektcount.widgets.NotesWidget;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by milo on 05/05/2014.
 * Changed by wmstein on 18.02.2016
 */

public class CountingActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "transektcountCountingActivity";
    private AlertDialog.Builder row_alert;
    TransektCountApplication transektCount;
    SharedPreferences prefs;
    int section_id;
    LinearLayout count_area;
    LinearLayout notes_area;
    int last_count;
    // added for 2nd counter per species
    int last_counta;

    // preferences
    private boolean awakePref;
    private boolean fontPref;
    private boolean soundPref;
    private boolean buttonSoundPref;
    private boolean hasChanged = false;
    private String alertSound;
    private String buttonAlertSound;
    private PowerManager.WakeLock wl;

    // the actual data
    Section section;
    List<Count> counts;
    List<Alert> alerts;

    List<CountingWidget> countingWidgets;

    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;
    private AlertDataSource alertDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counting);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            section_id = extras.getInt("section_id");
        }

        sectionDataSource = new SectionDataSource(this);
        countDataSource = new CountDataSource(this);
        alertDataSource = new AlertDataSource(this);

        transektCount = (TransektCountApplication) getApplication();
        //section_id = transektCount.section_id;
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        getPrefs();

        ScrollView counting_screen = (ScrollView) findViewById(R.id.countingScreen);
        counting_screen.setBackground(transektCount.getBackground());

        count_area = (LinearLayout) findViewById(R.id.countCountLayout);
        notes_area = (LinearLayout) findViewById(R.id.countNotesLayout);
        last_count = 0;
        last_counta = 0;

        if (awakePref)
        {
            // As FULL_WAKE_LOCK is deprecated, next 2 lines changed to addFlags funtion
            //PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            //wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /*
     * So preferences can be loaded at the start, and also when a change is detected.
     */
    private void getPrefs()
    {
        awakePref = prefs.getBoolean("pref_awake", true);
        fontPref = prefs.getBoolean("pref_note_font", false);
        soundPref = prefs.getBoolean("pref_sound", false);
        alertSound = prefs.getString("alert_sound", null);
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false);
        buttonAlertSound = prefs.getString("alert_button_sound", null);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // clear any existing views
        count_area.removeAllViews();
        notes_area.removeAllViews();

        // setup the data sources
        sectionDataSource.open();
        countDataSource.open();
        alertDataSource.open();

        // load the data
        // sections
        Log.i(TAG, "Section ID: " + String.valueOf(section_id));
        try
        {
            section = sectionDataSource.getSection(section_id);
        } 
        catch (CursorIndexOutOfBoundsException e)
        {
            Log.e(TAG, "Problem loading section: " + e.toString());
            Toast.makeText(CountingActivity.this, getString(R.string.getHelp), Toast.LENGTH_LONG).show();
            finish();
        }

        //Log.i(TAG, "Got section: " + section.name);
        // Actionbar: Zeile wegen Holo auskommentiert, funktioniert trotzdem nicht
        getSupportActionBar().setTitle(section.name);
        
        List<String> extras = new ArrayList<>();

        // counts
        countingWidgets = new ArrayList<>();
        counts = countDataSource.getAllCountsForSection(section.id);

        // display all the counts by adding them to countCountLayout
        alerts = new ArrayList<>();
        for (Count count : counts)
        {
            CountingWidget widget = new CountingWidget(this, null);
            widget.setCount(count);
            countingWidgets.add(widget);
            count_area.addView(widget);

            // add a section note widget if there are any notes
            if (StringUtils.isNotBlank(count.notes))
            {
                NotesWidget count_notes = new NotesWidget(this, null);
                count_notes.setNotes(count.notes);
                count_notes.setFont(fontPref);
                count_area.addView(count_notes);
            }

            // get add all alerts for this section
            List<Alert> tmpAlerts = alertDataSource.getAllAlertsForCount(count.id);
            for (Alert a : tmpAlerts)
            {
                alerts.add(a);
                extras.add(String.format(getString(R.string.willAlert), count.name, a.alert));
            }
        }
    /*
     * A crash here is a mystery, and users should seek further assistance.
     */

        if (!extras.isEmpty())
        {
            NotesWidget extra_notes = new NotesWidget(this, null);
            extra_notes.setNotes(StringUtils.join(extras, "\n"));
            notes_area.addView(extra_notes);
        }

        // display section notes
        // moved to bottom so it doesn't look like a count note
        if (section.notes != null)
        {
            if (!section.notes.isEmpty())
            {
                NotesWidget section_notes = new NotesWidget(this, null);
                section_notes.setNotes(section.notes);
                section_notes.setFont(fontPref);
                notes_area.addView(section_notes);
            }
        }

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // save the data
        saveData();
        // save section id in case it is lost on pause
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("pref_section_id", section_id);
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

    /**************************************************
     * Zählerstände sichern und Zähldatum nach S_CREATED_AT schreiben
     */
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


    /***************/
    public void saveAndExit(View view)
    {
        saveData();
        super.finish();
    }

  /*
   * The functions below are triggered by the count buttons
   */
    public void countUp(View view)
    {
        //Log.i(TAG, "View clicked: " + view.toString());
        //Log.i(TAG, "View tag: " + view.getTag().toString());
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            last_count = 0;
            widget.countUp();
            checkAlert(widget.count.id, widget.count.count);
        }
        hasChanged = true;
    }

    public void countDown(View view)
    {
        //Log.i(TAG, "View clicked: " + view.toString());
        //Log.i(TAG, "View tag: " + view.getTag().toString());
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            last_count = 0;
            widget.countDown();
            checkAlert(widget.count.id, widget.count.count);
        }
        hasChanged = true;
    }

    public void countUpa(View view)
    {
        //Log.i(TAG, "View clicked: " + view.toString());
        //Log.i(TAG, "View tag: " + view.getTag().toString());
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            last_counta = 0;
            widget.countUpa();
        }
        hasChanged = true;
    }

    public void countDowna(View view)
    {
        //Log.i(TAG, "View clicked: " + view.toString());
        //Log.i(TAG, "View tag: " + view.getTag().toString());
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            last_counta = 0;
            widget.countDowna();
        }
        hasChanged = true;
    }

    public void edit(View view)
    {
        int count_id = Integer.valueOf(view.getTag().toString());
        Intent intent = new Intent(CountingActivity.this, CountOptionsActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("section_id", section_id);
        startActivity(intent);
    }

    /*
     * This is the lookup to get a counting widget (with references to the
     * associated count) from the list of widgets.
     */
    public CountingWidget getCountFromId(int id)
    {
        for (CountingWidget widget : countingWidgets)
        {
            if (widget.count.id == id)
            {
                return widget;
            }
        }
        return null;
    }

    //**************************************
  /*
   * alert checking...
   */
    public void checkAlert(int count_id, int count_value)
    {
        for (Alert a : alerts)
        {
            if (a.count_id == count_id && a.alert == count_value)
            {
                row_alert = new AlertDialog.Builder(this);
                row_alert.setTitle(getString(R.string.alertTitle));
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
    
    /*
     * If the user has set the preference for an audible alert, then sound it here.
     */
    public void soundAlert()
    {
        if (soundPref)
        {
            try
            {
                Uri notification;
                if (StringUtils.isNotBlank(alertSound) && alertSound != null)
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

    public void buttonSound()
    {
        if (buttonSoundPref)
        {
            try
            {
                Uri notification;
                if (StringUtils.isNotBlank(buttonAlertSound) && buttonAlertSound != null)
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

     
  /***************************************
   * Pop up various exciting messages if the user has not bothered to turn them off in the
   * settings...
   */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.counting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        }
        else if (id == R.id.menuEditSection)
        {
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
        else if (id == R.id.menuSaveExit)
        {
            saveData();
            super.finish();
            return true;
        }
        else if (id == R.id.action_share)
        {
            String section_notes = section.notes;
            String section_name = section.name;
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
        ScrollView counting_screen = (ScrollView) findViewById(R.id.countingScreen);
        counting_screen.setBackground(null);
        counting_screen.setBackground(transektCount.setBackground());
        getPrefs();
    }

    public void cloneSection()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String m_Text = input.getText().toString();
                Toast.makeText(CountingActivity.this, "Got text: " + m_Text, Toast.LENGTH_SHORT).show();
        /*
         * Now having to a title for the section it needs to be determined if this is a duplicate
         * of the existing name. If not, then the section can be created.
         */
                if (m_Text.isEmpty())
                {
                    Toast.makeText(CountingActivity.this, getString(R.string.newName), Toast.LENGTH_SHORT).show();
                    return;
                }
                // These are needed to link old and new counts and alerts
                HashMap<Integer, Integer> countMap = new HashMap<>();

                // Creating the new section
                Section newSection = sectionDataSource.createSection(m_Text); // might need to escape the name
                newSection.notes = section.notes;
                sectionDataSource.saveSection(newSection);
                for (Count c : countDataSource.getAllCountsForSection(section_id))
                {
                    Count newCount = countDataSource.createCount(newSection.id, c.name);
                    newCount.notes = c.notes;
                    countDataSource.saveCount(newCount);

                    // prepare alerts
                    for (Alert a : alertDataSource.getAllAlertsForCount(c.id))
                    {
                        Alert newAlert = alertDataSource.createAlert(newCount.id, a.alert, a.alert_text);
                    }

                    // save relationship between old and new counts
                    countMap.put(c.id, newCount.id); //old, new

                }

                // Exit this and go to the list of new sections
                Toast.makeText(CountingActivity.this, getString(R.string.newCopyCreated), Toast.LENGTH_SHORT).show();
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

}
