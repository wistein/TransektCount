/*
 * Copyright (c) 2019. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.transektcount;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.wmstein.transektcount.database.Count;
import com.wmstein.transektcount.database.CountDataSource;
import com.wmstein.transektcount.widgets.SpeciesAddWidget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

/********************************************************************************
 * AddSpeciesLActivity lets you insert a new species into a section's species list
 * in landscape mode.
 * AddSpeciesActivity is called from EditSectionLActivity
 * Uses SpeciesAddWidget.java, widget_add_spec.xml.
 <p>
 * The sorting order of the species to add cannot be changed, as it is determined 
 * by 3 interdependent and correlated arrays in arrays.xml
 <p>
 * Created for TourCount by wmstein on 2022-04-29,
 * last edited on 2023-05-08
 */
public class AddSpeciesLActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "TransektCntAddSpecLAct";
    @SuppressLint("StaticFieldLeak")
    private static TransektCountApplication transektCount;

    private LinearLayout add_area;

    // the actual data
    private CountDataSource countDataSource;

    private int section_id;
    
    // Id list of missing species
    private String[] idArray;

    // complete ArrayLists of species
    ArrayList<String> namesCompleteArrayList, namesGCompleteArrayList, codesCompleteArrayList;

    private String specCode;

    private Bitmap bMap;
    private BitmapDrawable bg;

    private boolean brightPref;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        transektCount = (TransektCountApplication) getApplication();
        SharedPreferences prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        brightPref = prefs.getBoolean("pref_bright", true);

        setContentView(R.layout.activity_add_species);
        ScrollView add_screen = findViewById(R.id.addScreen);

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        bMap = transektCount.decodeBitmap(R.drawable.abackground, transektCount.width, transektCount.height);
        bg = new BitmapDrawable(add_screen.getResources(), bMap);
        add_screen.setBackground(bg);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            section_id = extras.getInt("section_id");
        }
        if (MyDebug.LOG)
            Log.e(TAG, "onCreate getIntent Section Id = " + section_id);

        add_area = findViewById(R.id.addSpecLayout);

        // Load complete species ArrayList from arrays.xml (lists are sorted by code)
        namesCompleteArrayList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.selSpecs)));
        namesGCompleteArrayList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.selSpecs_g)));
        codesCompleteArrayList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.selCodes)));
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        SharedPreferences prefs = TransektCountApplication.getPrefs();
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

        // clear any existing views
        add_area.removeAllViews();

        // setup the data sources
        countDataSource = new CountDataSource(this);
        countDataSource.open();

        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.addTitle);

        // list of only new species not already contained in the species counting list
        List<Count> counts;

        // code list of contained species
        ArrayList<String> specCodesContainedList = new ArrayList<>();
        
        // get species of the section counting list
        counts = countDataSource.getAllSpeciesForSectionSrtCode(section_id);

        // build code ArrayList of already contained species
        for (Count count : counts)
        {
            specCodesContainedList.add(count.code);
        }

        // build lists of missing species
        int specCodesContainedListSize = specCodesContainedList.size();
        int posSpec;

        // for already contained species reduce complete arraylists
        for (int i = 0; i < specCodesContainedListSize; i++)
        {
            if (codesCompleteArrayList.contains(specCodesContainedList.get(i)))
            {
                // Remove species with code x from missing species lists.
                // Prerequisites: exactly correlated arrays of selCodes, selSpecs and selSpecs_g
                //   for all localisations of arrays.xml
                specCode = specCodesContainedList.get(i);
                posSpec = codesCompleteArrayList.indexOf(specCode);

                namesCompleteArrayList.remove(posSpec);
                namesGCompleteArrayList.remove(posSpec);
                codesCompleteArrayList.remove(specCode);
            }
        }

        idArray = setIdsSelSpecs(codesCompleteArrayList); // create idArray from codeArray

        // load the species data into the widgets
        int i;
        for (i = 0; i < codesCompleteArrayList.size(); i++)
        {
            SpeciesAddWidget saw = new SpeciesAddWidget(this, null);

            saw.setSpecName(namesCompleteArrayList.get(i));
            saw.setSpecNameG(namesGCompleteArrayList.get(i));
            saw.setSpecCode(codesCompleteArrayList.get(i));
            saw.setPSpec(codesCompleteArrayList.get(i));
            saw.setSpecId(idArray[i]);
            add_area.addView(saw);
        }

    } // end of Resume


    // create idArray from codeArray
    private String[] setIdsSelSpecs(ArrayList<String> speccodesm)
    {
        int i;
        idArray = new String[speccodesm.size()];
        for (i = 0; i < speccodesm.size(); i++)
        {
            idArray[i] = String.valueOf(i + 1);
        }
        return idArray;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        outState.putInt("section_id", section_id);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        if (savedInstanceState.getInt("section_id") != 0)
            section_id = savedInstanceState.getInt("section_id");
        if (MyDebug.LOG)
            Log.e(TAG, "savedInstanceState Section Id = " + section_id);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // close the data sources
        countDataSource.close();
    }

    public void saveAndExit(View view)
    {
        if (saveData(view))
        {
            super.finish();
        }
    }

    private boolean saveData(View view)
    {
        // save added species to species list
        boolean retValue = true;

        int idToAdd = (Integer) view.getTag();
        SpeciesAddWidget saw1 = (SpeciesAddWidget) add_area.getChildAt(idToAdd);

        String specName = saw1.getSpecName();
        specCode = saw1.getSpecCode();
        String specNameG = saw1.getSpecNameG();

        try
        {
            countDataSource.createCount(section_id, specName, specCode, specNameG);
        } catch (Exception e)
        {
            retValue = false;
        }
        return retValue;
    }

    /*
     * Save the selected species to the species list
     */
    public void addCount(View view)
    {
        if (saveData(view))
        {
            super.finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_species, menu);
        return true;
    }

    @SuppressLint("ApplySharedPref")
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
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            NavUtils.navigateUpTo(this, intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onBackPressed()
    {
        //Intent intent = NavUtils.getParentActivityIntent(this);
        finish();
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView add_screen = findViewById(R.id.addScreen);
        bMap = transektCount.decodeBitmap(R.drawable.abackground, transektCount.width, transektCount.height);
        add_screen.setBackground(null);
        bg = new BitmapDrawable(add_screen.getResources(), bMap);
        add_screen.setBackground(bg);
    }
    
}
