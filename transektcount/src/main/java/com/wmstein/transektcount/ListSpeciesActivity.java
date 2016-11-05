package com.wmstein.transektcount;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.wmstein.transektcount.database.Count;
import com.wmstein.transektcount.database.CountDataSource;
import com.wmstein.transektcount.database.Head;
import com.wmstein.transektcount.database.HeadDataSource;
import com.wmstein.transektcount.database.Meta;
import com.wmstein.transektcount.database.MetaDataSource;
import com.wmstein.transektcount.database.Section;
import com.wmstein.transektcount.database.SectionDataSource;
import com.wmstein.transektcount.widgets.ListHeadWidget;
import com.wmstein.transektcount.widgets.ListMetaWidget;
import com.wmstein.transektcount.widgets.ListSpeciesWidget;

import java.util.List;

/****************************************************
 * ListSpeciesActivity shows list of counting results
 * Created by wmstein on 15.03.2016
 */
public class ListSpeciesActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "transektcountListSpeciesActivity";
    TransektCountApplication transektCount;
    SharedPreferences prefs;
    
    LinearLayout spec_area;

    Head head;
    Meta meta;

    public int spec_count;
    public int spec_counta;

    // preferences
    private boolean awakePref;
    private String sortPref;

    // the actual data
    private CountDataSource countDataSource;
    private SectionDataSource sectionDataSource;
    private HeadDataSource headDataSource;
    private MetaDataSource metaDataSource;

    ListHeadWidget ehw;
    ListMetaWidget etw;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_species);

        countDataSource = new CountDataSource(this);
        sectionDataSource = new SectionDataSource(this);
        headDataSource = new HeadDataSource(this);
        metaDataSource = new MetaDataSource(this);

        transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        awakePref = prefs.getBoolean("pref_awake", true);
        sortPref = prefs.getString("pref_sort_sp", "none"); // sorted species list

        ScrollView listSpec_screen = (ScrollView) findViewById(R.id.listSpecScreen);
        listSpec_screen.setBackground(transektCount.getBackground());
        getSupportActionBar().setTitle(getString(R.string.viewSpecTitle));

        spec_area = (LinearLayout) findViewById(R.id.listSpecLayout);

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // build Show Results screen
        spec_area.removeAllViews();
        loadData();
    }

    // fill ListSpeciesWidget with relevant counts and sections data
    public void loadData()
    {
        headDataSource.open();
        metaDataSource.open();

        //load head and meta data
        head = headDataSource.getHead();
        meta = metaDataSource.getMeta();

        // display the editable transect No.
        ehw = new ListHeadWidget(this, null);
        ehw.setWidgetLNo(getString(R.string.transectnumber));
        ehw.setWidgetLNo1(head.transect_no);
        ehw.setWidgetLName(getString(R.string.inspector));
        ehw.setWidgetLName1(head.inspector_name);
        spec_area.addView(ehw);

        // display the editable meta data
        etw = new ListMetaWidget(this, null);
        etw.setWidgetLMeta1(getString(R.string.temperature));
        etw.setWidgetLItem1(meta.temp);
        etw.setWidgetLMeta2(getString(R.string.wind));
        etw.setWidgetLItem2(meta.wind);
        etw.setWidgetLMeta3(getString(R.string.clouds));
        etw.setWidgetLItem3(meta.clouds);
        etw.setWidgetLDate1(getString(R.string.date));
        etw.setWidgetLDate2(meta.date);
        etw.setWidgetLTime1(getString(R.string.starttm));
        etw.setWidgetLItem4(meta.start_tm);
        etw.setWidgetLTime2(getString(R.string.endtm));
        etw.setWidgetLItem5(meta.end_tm);
        spec_area.addView(etw);
        
        List<Section> sort_sections; // List of sorted sections
        List<Count> specs; // List of species

        int sect_id;
        // preset for unused id of section as starting criteria in if-clause of for-loop
        int sect_idOld = 0;
        Section section;

        // setup the data sources
        countDataSource.open();
        sectionDataSource.open();

        // load the data
        sort_sections = sectionDataSource.getAllSections(prefs);

        // display all the sorted counts by adding them to listSpecies layout
        for (Section sort_sect : sort_sections)
        {
            sect_id = sort_sect.id;

            switch (sortPref)
            {
            case "names_alpha":
                specs = countDataSource.getAllSpecsForSectionSrtName(sect_id);
                break;
            case "codes":
                specs = countDataSource.getAllSpecsForSectionSrtCode(sect_id);
                break;
            default:
                specs = countDataSource.getAllSpecsForSection(sect_id);
                break;
            }
            
            for (Count spec : specs)
            {
                // set section ID from count table and prepare to get section name from section table
                //sect_id = spec.section_id;
                //Log.e(TAG, "sect_id "  + String.valueOf(sect_id));
                section = sectionDataSource.getSection(sect_id);

                ListSpeciesWidget widget = new ListSpeciesWidget(this, null);
                widget.setCount(spec, section);
                spec_count = widget.getSpec_count(spec);
                spec_counta = widget.getSpec_counta(spec);

                // Show 2nd and ff. section names and remarks dark grey
                if (sect_id == sect_idOld)
                {
                    widget.setCount1();
                }

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
        headDataSource.close();
        metaDataSource.close();
        countDataSource.close();
        sectionDataSource.close();

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
        awakePref = prefs.getBoolean("pref_awake", true);
        sortPref = prefs.getString("pref_sort_sp", "none");
    }

}
