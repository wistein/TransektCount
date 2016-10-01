package com.wmstein.transektcount;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.wmstein.transektcount.database.Section;
import com.wmstein.transektcount.database.SectionDataSource;

import java.util.List;

/*
 * Based on ListProjectActivity.java by milo on 05/05/2014.
 * Modified by wmstein on 08.04.2016
 */
public class ListSectionActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "TransektCountListSectionActivity";
    TransektCountApplication transektCount;
    SharedPreferences prefs;

    // preferences
    private boolean brightPref;

    private SectionDataSource sectionDataSource;
    
    List<Section> sections;
    ListView list;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_section);

        transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
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

        LinearLayout list_view = (LinearLayout) findViewById(R.id.list_view);
        list_view.setBackground(transektCount.getBackground());
        list = (ListView) findViewById(android.R.id.list);
    }

    public void deleteSection(Section sct)
    {
        sectionDataSource.deleteSection(sct);
        showData();
        list.invalidate(); //force list to draw
    }

    @Override
    protected void onResume()
    {
        super.onResume();
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
        ListView lv = (ListView) findViewById(android.R.id.list);
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
        LinearLayout list_view = (LinearLayout) findViewById(R.id.list_view);
        list_view.setBackground(null);
        list_view.setBackground(transektCount.setBackground());
        TransektCountApplication.getPrefs();
    }
}
