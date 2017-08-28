package com.wmstein.transektcount;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.wmstein.transektcount.database.Section;
import com.wmstein.transektcount.database.SectionDataSource;

import java.util.List;

/*********************************************************
 * Create a new empty transect section list (NewCount)
 * uses activity_new_section
 * NewSectionActivity is called from ListSectionActivity.
 * Based on NewProjectActivity.java by milo on 05/05/2014,
 * changed by wmstein on 18.02.2016
 */
public class NewSectionActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "TransektCountNewSectionActivity";
    TransektCountApplication transektCount;
    SharedPreferences prefs;

    private boolean screenOrientL; // option for screen orientation
    private boolean dupPref;
    private Bitmap bMap;
    private BitmapDrawable bg;

    Section section;
    ViewGroup layout;
    EditText newsectName;
    private SectionDataSource sectionDataSource;
    List<Section> sections;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_new_section);

        transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        dupPref = prefs.getBoolean("pref_duplicate", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        LinearLayout baseLayout = (LinearLayout) findViewById(R.id.newsectScreen); //in activity_new_section.xml

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            bMap = transektCount.decodeBitmap(R.drawable.kbackgroundl, transektCount.width, transektCount.height);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            bMap = transektCount.decodeBitmap(R.drawable.kbackground, transektCount.width, transektCount.height);
        }

        bg = new BitmapDrawable(baseLayout.getResources(), bMap);
        baseLayout.setBackground(bg);

        sectionDataSource = new SectionDataSource(this);

        newsectName = (EditText) findViewById(R.id.newsectName); //in activity_new_section.xml
        newsectName.setTextColor(Color.WHITE);
        newsectName.setHintTextColor(Color.argb(255, 170, 170, 170));
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        sectionDataSource.open();
        // To show the keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
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
                Toast.makeText(NewSectionActivity.this, sect_name + " " + getString(R.string.isdouble), Toast.LENGTH_SHORT).show();
                return;
            }
            else
            {
                sectionDataSource.createSection(sect_name); // might need to escape the name
            }
        }
        else
        {
            Toast.makeText(NewSectionActivity.this, sect_name + " " + getString(R.string.isempty), Toast.LENGTH_SHORT).show();
            return;
        }

        // Huzzah!
        Toast.makeText(this, getString(R.string.sectionSaved), Toast.LENGTH_SHORT).show();

        // Edit the new section.
        int section_id;
        section = sectionDataSource.getSectionByName(sect_name);
        section_id = section.id;
        Intent intent = new Intent(NewSectionActivity.this, EditSectionActivity.class);
        intent.putExtra("section_id", section_id);
        startActivity(intent);

        // Show the new section.
        //Intent intent = new Intent(NewSectionActivity.this, ListSectionActivity.class);
        //startActivity(intent);
    }

    // Compare section names for duplicates and return TRUE when duplicate found
    // created by wmstein on 10.04.2016
    public boolean compSectionNames(String newname)
    {
        boolean isDblName = false;
        String sname;

        List<Section> sectionList = sectionDataSource.getAllSectionNames();

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

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        LinearLayout baseLayout = (LinearLayout) findViewById(R.id.newsectScreen);

        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        assert baseLayout != null;
        baseLayout.setBackground(null);
        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            bMap = transektCount.decodeBitmap(R.drawable.kbackgroundl, transektCount.width, transektCount.height);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            bMap = transektCount.decodeBitmap(R.drawable.kbackground, transektCount.width, transektCount.height);
        }
        bg = new BitmapDrawable(baseLayout.getResources(), bMap);
        baseLayout.setBackground(bg);

        dupPref = prefs.getBoolean("pref_duplicate", true);
    }

    /**
     * Following functions are taken from the Apache commons-lang3-3.4 library
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
    public static boolean isNotEmpty(final CharSequence cs)
    {
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
    public static boolean isEmpty(final CharSequence cs)
    {
        return cs == null || cs.length() == 0;
    }

}
