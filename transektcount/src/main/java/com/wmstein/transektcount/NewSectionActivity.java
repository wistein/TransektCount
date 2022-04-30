package com.wmstein.transektcount;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.wmstein.transektcount.database.Section;
import com.wmstein.transektcount.database.SectionDataSource;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

/*********************************************************
 * Create a new empty transect section list (NewCount)
 * uses activity_new_section.xml
 * NewSection(P)Activity is called from ListSection(P)Activity.
 * Based on NewProjectActivity.java by milo on 05/05/2014,
 * changed by wmstein since 2016-02-16,
 * last edited on 2022-04-30
 */
public class NewSectionActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "TransektCountNewSectAct";
    @SuppressLint("StaticFieldLeak")
    private static TransektCountApplication transektCount;
    SharedPreferences prefs;

    private Bitmap bMap;
    private BitmapDrawable bg;

    Section section;
    ViewGroup layout;
    EditText newsectName;
    private SectionDataSource sectionDataSource;
    List<Section> sections = new ArrayList<>();

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_new_section);

        transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);

        ScrollView baseLayout = findViewById(R.id.newsectScreen); //in activity_new_section.xml
        bMap = transektCount.decodeBitmap(R.drawable.kbackground, transektCount.width, transektCount.height);
        bg = new BitmapDrawable(baseLayout.getResources(), bMap);
        baseLayout.setBackground(bg);

        sectionDataSource = new SectionDataSource(this);

        newsectName = findViewById(R.id.newsectName); //in activity_new_section.xml
        newsectName.setTextColor(Color.WHITE);
        newsectName.setHintTextColor(Color.argb(255, 170, 170, 170));
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        sectionDataSource.open();
        // Show the keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // close the data sources
        sectionDataSource.close();
    }

    @Override
    // Inflate the menu; this adds items to the action bar if it is present.
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.new_section, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will automatically handle clicks on 
        // the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.menuSaveExit)
        {
            saveSection(layout);
        }
        return super.onOptionsItemSelected(item);
    }

    // Save section with plausi-check for empty or duplicate section name
    @SuppressLint("ApplySharedPref")
    public void saveSection(View view)
    {
        // first, the section name
        String sect_name = newsectName.getText().toString();
        sections = sectionDataSource.getAllSections(prefs);

        // check for empty section name
        if (isNotEmpty(sect_name))
        {
            //check if this is not a duplicate of an existing name
            if (compSectionNames(sect_name))
            {
                showSnackbarRed(sect_name + " " + getString(R.string.isdouble));
                return;
            }
            else
            {
                sectionDataSource.createSection(sect_name); // might need to escape the name
                if (MyDebug.LOG)
                    Log.d(TAG, "sect_name = " + sect_name);
            }
        }
        else
        {
            showSnackbarRed(sect_name + " " + getString(R.string.isempty));
            return;
        }

        // Toast here, as snackbar doesn't show up
        Toast.makeText(this, getString(R.string.sectionSaved), Toast.LENGTH_SHORT).show();

        // Edit the new section.
        int section_id;
        section = sectionDataSource.getSectionByName(sect_name);
        section_id = section.id;

        // Store section_id into SharedPreferences.
        // That makes sure that the current selected section can be retrieved 
        // by EditSectionActivity
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("section_id", section_id);
        editor.commit();
        
        if (MyDebug.LOG)
            Log.d(TAG, "sect_id = " + section_id);
        Intent intent = new Intent(NewSectionActivity.this, EditSectionActivity.class);
        intent.putExtra("section_id", section_id);
        startActivity(intent);
    }

    // Compare section names for duplicates and return TRUE when duplicate found
    // created by wmstein on 10.04.2016
    public boolean compSectionNames(String newname)
    {
        boolean isDblName = false;
        String sname;

        List<Section> sectionList = sectionDataSource.getAllSectionNames();

        // int childcount = sectionList.size() + 1;  erzeugte Indexfehler
        int childcount = sectionList.size();
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

    @SuppressLint("SourceLockedOrientationActivity")
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView baseLayout = findViewById(R.id.newsectScreen);
        baseLayout.setBackground(null);
        bMap = transektCount.decodeBitmap(R.drawable.kbackground, transektCount.width, transektCount.height);
        bg = new BitmapDrawable(baseLayout.getResources(), bMap);
        baseLayout.setBackground(bg);
    }

    private void showSnackbarRed(String str) // bold red text
    {
        View view = findViewById(R.id.newsectScreen);
        Snackbar sB = Snackbar.make(view, Html.fromHtml("<font color=\"#ff0000\"><b>" + str + "</font></b>"), Snackbar.LENGTH_LONG);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        sB.show();
    }
    
    /**
     * Following functions are taken from the Apache commons-lang3-3.4 library
     * licensed under Apache License Version 2.0, January 2004
     * 
     * Checks if a CharSequence is not empty ("") and not null.
     * 
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
     * 
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
