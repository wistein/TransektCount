package com.wmstein.transektcount;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.wmstein.transektcount.database.Alert;
import com.wmstein.transektcount.database.AlertDataSource;
import com.wmstein.transektcount.database.Count;
import com.wmstein.transektcount.database.CountDataSource;
import com.wmstein.transektcount.widgets.AddAlertWidget;
import com.wmstein.transektcount.widgets.AlertCreateWidget;
import com.wmstein.transektcount.widgets.EditNotesWidget;
import com.wmstein.transektcount.widgets.OptionsWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

/************************************************************
 * Edit options for counting species
 * uses optionsWidget.java and widget_options.xml
 * Supplemented with functions for transect external counter
 * Based on CountOptionsActivity.java by milo on 05/05/2014.
 * Adapted and changed by wmstein since 2016-02-18,
 * last edited on 2023-05-08
 */
public class CountOptionsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "transektcountCntOptAct";
    @SuppressLint("StaticFieldLeak")
    private static TransektCountApplication transektCount;
    
    SharedPreferences prefs;

    private Count count;
    private int count_id;
    private CountDataSource countDataSource;
    private AlertDataSource alertDataSource;
    private View markedForDelete;
    private int deleteAnAlert;
    private int section_id;

    private Bitmap bMap;
    private BitmapDrawable bg;

    LinearLayout static_widget_area;
    LinearLayout dynamic_widget_area;
    OptionsWidget curr_val_widget;
    EditNotesWidget enw;
    AddAlertWidget aa_widget;
    private boolean brightPref;

    ArrayList<AlertCreateWidget> savedAlerts;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_count_options);

        transektCount = (TransektCountApplication) getApplication();
        prefs = com.wmstein.transektcount.TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        brightPref = prefs.getBoolean("pref_bright", true);

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        ScrollView counting_screen = findViewById(R.id.count_options);
        bMap = transektCount.decodeBitmap(R.drawable.kbackground, transektCount.width, transektCount.height);
        bg = new BitmapDrawable(counting_screen.getResources(), bMap);
        counting_screen.setBackground(bg);

        static_widget_area = findViewById(R.id.static_widget_area);
        dynamic_widget_area = findViewById(R.id.dynamic_widget_area);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            count_id = extras.getInt("count_id");
            section_id = extras.getInt("section_id");
            //spinnerPosition = extras.getInt("itemposition");
        }

        savedAlerts = new ArrayList<>();
        if (savedInstanceState != null)
        {
            if (savedInstanceState.getSerializable("savedAlerts") != null)
            {
                //noinspection unchecked
                savedAlerts = (ArrayList<AlertCreateWidget>) savedInstanceState.getSerializable("savedAlerts");
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // build the count options screen
        // clear any existing views
        static_widget_area.removeAllViews();
        dynamic_widget_area.removeAllViews();

        // get the data sources
        countDataSource = new CountDataSource(this);
        countDataSource.open();
        alertDataSource = new AlertDataSource(this);
        alertDataSource.open();

        count = countDataSource.getCountById(count_id);

        try
        {
            Objects.requireNonNull(getSupportActionBar()).setTitle(count.name);
        } catch (NullPointerException e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "Problem setting title bar: " + e);
        }

        List<Alert> alerts = alertDataSource.getAllAlertsForCount(count_id);

        // setup the static widgets in the following order
        // 1. Current count value (internal counter)
        // 2. Current counta value (external counter)
        // 3. Alert add/remove
        curr_val_widget = new OptionsWidget(this, null);
        curr_val_widget.setInstructionsf1i(String.format(getString(R.string.editCountValuef1i), count.count_f1i));
        curr_val_widget.setInstructionsf2i(String.format(getString(R.string.editCountValuef2i), count.count_f2i));
        curr_val_widget.setInstructionsf3i(String.format(getString(R.string.editCountValuef3i), count.count_f3i));
        curr_val_widget.setInstructionspi(String.format(getString(R.string.editCountValuepi), count.count_pi));
        curr_val_widget.setInstructionsli(String.format(getString(R.string.editCountValueli), count.count_li));
        curr_val_widget.setInstructionsei(String.format(getString(R.string.editCountValueei), count.count_ei));
        curr_val_widget.setInstructionsf1e(String.format(getString(R.string.editCountValuef1e), count.count_f1e));
        curr_val_widget.setInstructionsf2e(String.format(getString(R.string.editCountValuef2e), count.count_f2e));
        curr_val_widget.setInstructionsf3e(String.format(getString(R.string.editCountValuef3e), count.count_f3e));
        curr_val_widget.setInstructionspe(String.format(getString(R.string.editCountValuepe), count.count_pe));
        curr_val_widget.setInstructionsle(String.format(getString(R.string.editCountValuele), count.count_le));
        curr_val_widget.setInstructionsee(String.format(getString(R.string.editCountValueee), count.count_ee));
        
        curr_val_widget.setParameterValuef1i(count.count_f1i);
        curr_val_widget.setParameterValuef2i(count.count_f2i);
        curr_val_widget.setParameterValuef3i(count.count_f3i);
        curr_val_widget.setParameterValuepi(count.count_pi);
        curr_val_widget.setParameterValueli(count.count_li);
        curr_val_widget.setParameterValueei(count.count_ei);
        curr_val_widget.setParameterValuef1e(count.count_f1e);
        curr_val_widget.setParameterValuef2e(count.count_f2e);
        curr_val_widget.setParameterValuef3e(count.count_f3e);
        curr_val_widget.setParameterValuepe(count.count_pe);
        curr_val_widget.setParameterValuele(count.count_le);
        curr_val_widget.setParameterValueee(count.count_ee);
        
        static_widget_area.addView(curr_val_widget);

        enw = new EditNotesWidget(this, null);
        enw.setSectionNotes(count.notes);
        enw.setWidgetNotes(getString(R.string.notesSpecies));
        enw.setHint(getString(R.string.notesHint));

        static_widget_area.addView(enw);

        aa_widget = new AddAlertWidget(this, null);
        static_widget_area.addView(aa_widget);

    /*
     * There should be a method to add all counts in order to re-draw when one is deleted.
     */
        for (Alert alert : alerts)
        {
            AlertCreateWidget acw = new AlertCreateWidget(this, null);
            acw.setAlertName(alert.alert_text);
            acw.setAlertValue(alert.alert);
            acw.setAlertId(alert.id);
            dynamic_widget_area.addView(acw);
        }

    /*
     * Add saved alert create widgets
     */
        for (AlertCreateWidget acw : savedAlerts)
        {
            dynamic_widget_area.addView(acw);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
    /*
     * Before these widgets can be serialised they must be removed from their parent, or else
     * trying to add them to a new parent causes a crash because they've already got one.
     */
        super.onSaveInstanceState(outState);
        for (AlertCreateWidget acw : savedAlerts)
        {
            ((ViewGroup) acw.getParent()).removeView(acw);
        }
        outState.putSerializable("savedAlerts", savedAlerts);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // finally, close the database
        countDataSource.close();
        alertDataSource.close();

    }

    public void saveAndExit(View view)
    {
        saveData();
        savedAlerts.clear();
        super.finish();
    }

    @SuppressLint("LongLogTag")
    public void saveData()
    {
        // don't crash if the user hasn't filled things in...
        // Toast here, as snackbar doesn't show up
        Toast.makeText(CountOptionsActivity.this, getString(R.string.sectSaving) + " " + count.name + "!", Toast.LENGTH_SHORT).show();
        count.count_f1i = curr_val_widget.getParameterValuef1i();
        count.count_f2i = curr_val_widget.getParameterValuef2i();
        count.count_f3i = curr_val_widget.getParameterValuef3i();
        count.count_pi = curr_val_widget.getParameterValuepi();
        count.count_li = curr_val_widget.getParameterValueli();
        count.count_ei = curr_val_widget.getParameterValueei();
        count.count_f1e = curr_val_widget.getParameterValuef1e();
        count.count_f2e = curr_val_widget.getParameterValuef2e();
        count.count_f3e = curr_val_widget.getParameterValuef3e();
        count.count_pe = curr_val_widget.getParameterValuepe();
        count.count_le = curr_val_widget.getParameterValuele();
        count.count_ee = curr_val_widget.getParameterValueee();
        count.notes = enw.getSectionNotes();

        countDataSource.saveCount(count);

    /*
     * Get all the alerts from the dynamic_widget_area and save each one.
     * If it has an id value set to anything higher than 0 then it should be an update, if it is 0
     * then it's a new alert and should be created instead.
     */
        int childcount = dynamic_widget_area.getChildCount();
        for (int i = 0; i < childcount; i++)
        {
            AlertCreateWidget acw = (AlertCreateWidget) dynamic_widget_area.getChildAt(i);
            if (isNotEmpty(acw.getAlertName()))
            {
                // save or create
                if (acw.getAlertId() == 0)
                {
                    alertDataSource.createAlert(count_id, acw.getAlertValue(), acw.getAlertName());
                }
                else
                {
                    alertDataSource.saveAlert(acw.getAlertId(), acw.getAlertValue(), acw.getAlertName());
                }
            }
            else
            {
                if (MyDebug.LOG)
                    Log.d(TAG, "Failed to save alert: " + acw.getAlertId());
            }
        }
    }

    // Scroll to end of view, by wmstein
    public void ScrollToEndOfView(View scrlV)
    {
        int scroll_amount = scrlV.getBottom();
        int scrollY = scroll_amount;
        boolean pageend = false;
        while (!pageend)
        {
            scrlV.scrollTo(0, scroll_amount);           //scroll
            scroll_amount = scroll_amount + scroll_amount; //increase scroll_amount
            scrollY = scrollY + scrlV.getScrollY();        //scroll position of 1. row
            if (scroll_amount > scrollY)
            {
                pageend = true;
            }
        }
    }

    // Add alert to species counter
    public void addAnAlert(View view)
    {
        AlertCreateWidget acw = new AlertCreateWidget(this, null);
        savedAlerts.add(acw);
        // Scroll to end of view
        View scrollV = findViewById(R.id.count_options);
        ScrollToEndOfView(scrollV);
        acw.requestFocus();
        dynamic_widget_area.addView(acw);
    }

    // Delete alert from species counter and its widget from the view
    public void deleteWidget(View view)
    {
    /*
     * These global variables keep a track of the view containing an alert to be deleted and also the id
     * of the alert itself, to make sure that they're available inside the code for the alert dialog by
     * which they will be deleted.
     */
        AlertDialog.Builder areYouSure;
        markedForDelete = view;
        deleteAnAlert = (Integer) view.getTag();
        if (deleteAnAlert == 0)
        {
            //Log.i(TAG, "(1) View tag was " + String.valueOf(deleteAnAlert));
            // the actual AlertCreateWidget is two levels up from the button in which it is embedded
            dynamic_widget_area.removeView((AlertCreateWidget) view.getParent().getParent());
        }
        else
        {
            //Log.i(TAG, "(2) View tag was " + String.valueOf(deleteAnAlert));
            // before removing this widget it is necessary to do the following:
            // (1) Check the user is sure they want to delete it and, if so...
            // (2) Delete the associated alert from the database.
            areYouSure = new AlertDialog.Builder(this);
            areYouSure.setTitle(getString(R.string.deleteAlert));
            areYouSure.setMessage(getString(R.string.reallyDeleteAlert));
            areYouSure.setPositiveButton(R.string.yesDeleteIt, (dialog, whichButton) -> {
                // go ahead for the delete
                try
                {
                    alertDataSource.deleteAlertById(deleteAnAlert);
                    dynamic_widget_area.removeView((AlertCreateWidget) markedForDelete.getParent().getParent());
                } catch (Exception e)
                {
                    if (MyDebug.LOG)
                        Log.e(TAG, "Failed to delete a widget: " + e);
                }
            });
            areYouSure.setNegativeButton(R.string.cancel, (dialog, whichButton) -> {
                // Cancelled.
            });
            areYouSure.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.count_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home)
        {
            Intent intent = NavUtils.getParentActivityIntent(this);
            assert intent != null;
            intent.putExtra("section_id", section_id);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            NavUtils.navigateUpTo(this, intent);
        }
        else if (id == R.id.menuSaveExit)
        {
            saveData();
            super.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView counting_screen = findViewById(R.id.count_options);
        counting_screen.setBackground(null);
        brightPref = prefs.getBoolean("pref_bright", true);

        bMap = transektCount.decodeBitmap(R.drawable.kbackground, transektCount.width, transektCount.height);
        bg = new BitmapDrawable(counting_screen.getResources(), bMap);
        counting_screen.setBackground(bg);
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
