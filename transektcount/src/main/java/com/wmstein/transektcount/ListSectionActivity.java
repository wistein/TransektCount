package com.wmstein.transektcount;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;

import com.wmstein.transektcount.database.Section;
import com.wmstein.transektcount.database.SectionDataSource;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

/***************************************************************************************************
 * Shows the list of selectable sections
 * Based on ListProjectActivity.java by milo on 05/05/2014.
 * Changes and additions for TransektCount by wmstein since 2016-02-16,
 * last edited on 2021-01-26
 */
public class ListSectionActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    //private static final String TAG = "TransektCountListSectAct";
    @SuppressLint("StaticFieldLeak")
    private static TransektCountApplication transektCount;

    // preferences
    private SharedPreferences prefs;
    private boolean screenOrientL; // option for screen orientation

    private SectionDataSource sectionDataSource;
    List<Section> sections;
    ListView list;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list_section);

        boolean brightPref;
        transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        brightPref = prefs.getBoolean("pref_bright", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        LinearLayout list_view = findViewById(R.id.list_view);
        list_view.setBackground(transektCount.getBackground());
        list = findViewById(android.R.id.list);
    }

    public void deleteSection(Section sct)
    {
        sectionDataSource.deleteSection(sct);
        showData();
        list.invalidate(); //force list to draw
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onResume()
    {
        super.onResume();

        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        ScrollView baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(null);
        baseLayout.setBackground(transektCount.setBackground());

        sectionDataSource = new SectionDataSource(this);
        sectionDataSource.open();
        showData();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        sectionDataSource.close();
    }

    // modified for ListView lv by wmstein
    public void showData()
    {
        sections = sectionDataSource.getAllSections(prefs);
        SectionListAdapter adapter = new SectionListAdapter(this, R.layout.listview_section_row, sections);
        ListView lv = findViewById(android.R.id.list);
        lv.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.list_section, menu);
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
        else if (id == R.id.newSect)
        {
            startActivity(new Intent(this, NewSectionActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        LinearLayout list_view = findViewById(R.id.list_view);
        list_view.setBackground(null);
        list_view.setBackground(transektCount.setBackground());
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
    }
}
