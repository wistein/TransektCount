package com.wmstein.transektcount;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.wmstein.transektcount.database.Count;
import com.wmstein.transektcount.database.CountDataSource;
import com.wmstein.transektcount.database.Section;
import com.wmstein.transektcount.database.SectionDataSource;
import com.wmstein.transektcount.widgets.ListSpeciesWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Show counting results without empty counts
 * Created by wmstein on 15.03.2016
 */

public class ListSpeciesActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "transektcountListSpeciesActivity";
    TransektCountApplication transektCount;
    SharedPreferences prefs;
    LinearLayout spec_area;
    
    public int spec_count;
    public int spec_counta;
    // preferences
    private boolean awakePref;
    private PowerManager.WakeLock wl;

    // the actual data
    private List<Count> specs;  //List of species

    private List<ListSpeciesWidget> listSpecWidgets;

    private CountDataSource countDataSource;
    private SectionDataSource sectionDataSource;
    
    private int sect_id;
    private int sect_idOld;
    private Section section;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listspecies);

        countDataSource = new CountDataSource(this);
        sectionDataSource = new SectionDataSource(this);

        transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        getPrefs();

        ScrollView listSpec_screen = (ScrollView) findViewById(R.id.listSpecScreen);
        listSpec_screen.setBackground(transektCount.getBackground());

        spec_area = (LinearLayout) findViewById(R.id.listSpecLayout);

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
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        loadData();

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
    

    // 
    public void loadData()
    {
        listSpecWidgets = new ArrayList<>();
        //ListSpeciesActivity.this.getSupportActionBar().setTitle(getString(R.string.viewSpecTitle));
        getSupportActionBar().setTitle(getString(R.string.viewSpecTitle));

        // setup the data sources
        countDataSource.open();
        sectionDataSource.open();

        // load the data
        specs = countDataSource.getAllSpecies();
        
        //ListSpeciesActivity.this.spec_area.removeAllViews();
        spec_area.removeAllViews();
        sect_idOld = 999999; // preset for No. of sections never reached
        // display all the counts by adding them to listSpecies layout
        for (Count spec : specs)
        {
            // set section ID from count table and prepare to get section name from section table
            sect_id = spec.section_id;
            Log.e(TAG, "sect_id "  + String.valueOf(sect_id));
            section = sectionDataSource.getSection(sect_id);

            ListSpeciesWidget widget = new ListSpeciesWidget(this, null);
            widget.setCount(spec, section);
            spec_count = widget.getSpec_count(spec);
            spec_counta = widget.getSpec_counta(spec);

            // fill widget only for counted species
            if (spec_counta > 0 || spec_count > 0)
            {
                if (sect_id == sect_idOld)
                {
                    widget.setCount1(spec, section);
                }
                
                listSpecWidgets.add(widget);
                //ListSpeciesActivity.this.spec_area.addView(widget);
                spec_area.addView(widget);
                sect_idOld = sect_id;
            }
        }
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();

        // close the data sources
        countDataSource.close();
        sectionDataSource.close();
        
        // N.B. a wakelock might not be held, e.g. if someone is using Cyanogenmod and
        // has denied wakelock permission to transektcount
        if (awakePref)
        {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }


    /***************/
    public void saveAndExit(View view)
    {
        super.finish();
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView listSpec_screen = (ScrollView) findViewById(R.id.listSpecScreen);
        listSpec_screen.setBackground(null);
        listSpec_screen.setBackground(transektCount.setBackground());
        getPrefs();
    }

}
