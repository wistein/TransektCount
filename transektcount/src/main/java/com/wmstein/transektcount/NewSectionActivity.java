package com.wmstein.transektcount;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
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

import com.wmstein.transektcount.database.Section;
import com.wmstein.transektcount.database.SectionDataSource;

import java.util.List;

/*********************************************************
 * Create a new empty transect section list (NewCount)
 * uses activity_new_section
 * NewSectionActivity is called from ListSectionActivity.
 * Based on NewProjectActivity.java by milo on 05/05/2014,
 * changed by wmstein since 2016-02-16,
 * last edited on 2018-08-04
 */
public class NewSectionActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "TransektCountNewSectionActivity";
    TransektCountApplication transektCount;
    SharedPreferences prefs;

    private boolean screenOrientL; // option for screen orientation
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
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        ScrollView baseLayout = findViewById(R.id.newsectScreen); //in activity_new_section.xml

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        bMap = transektCount.decodeBitmap(R.drawable.kbackground, transektCount.width, transektCount.height);
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
//                Toast.makeText(NewSectionActivity.this, sect_name + " " + getString(R.string.isdouble), Toast.LENGTH_SHORT).show();
                showSnackbarRed(sect_name + " " + getString(R.string.isdouble));
                return;
            }
            else
            {
                sectionDataSource.createSection(sect_name); // might need to escape the name
            }
        }
        else
        {
//            Toast.makeText(NewSectionActivity.this, sect_name + " " + getString(R.string.isempty), Toast.LENGTH_SHORT).show();
            showSnackbarRed(sect_name + " " + getString(R.string.isempty));
            return;
        }

        // Toast, as snackbar doesn't show up
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

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView baseLayout = findViewById(R.id.newsectScreen);

        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        baseLayout.setBackground(null);
        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
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
