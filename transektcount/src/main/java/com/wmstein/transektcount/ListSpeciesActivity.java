package com.wmstein.transektcount;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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
import com.wmstein.transektcount.widgets.ListSumWidget;

import java.util.List;

/****************************************************
 * ListSpeciesActivity shows list of counting results
 * Created by wmstein on 2016-03-15,
 * last edited on 2019-02-12
 */
public class ListSpeciesActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "transektcountListSpecAct"; // for future use
    TransektCountApplication transektCount;
    SharedPreferences prefs;

    LinearLayout spec_area;

    Head head;
    Meta meta;

    // preferences
    private boolean awakePref;
    private String sortPref;
    private boolean screenOrientL; // option for screen orientation

    // the actual data
    private CountDataSource countDataSource;
    private SectionDataSource sectionDataSource;
    private HeadDataSource headDataSource;
    private MetaDataSource metaDataSource;

    ListHeadWidget lhw;
    ListMetaWidget lmw;
    ListSumWidget lsw;

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
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        ScrollView listSpec_screen = findViewById(R.id.listSpecScreen);
        listSpec_screen.setBackground(transektCount.getBackground());

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(getString(R.string.viewSpecTitle));

        spec_area = findViewById(R.id.listSpecLayout);

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
        int summf = 0, summ = 0, sumf = 0, sump = 0, suml = 0, sumo = 0;
        int summfe = 0, summe = 0, sumfe = 0, sumpe = 0, sumle = 0, sumoe = 0;
        int sumInt, sumExt;

        headDataSource.open();
        metaDataSource.open();

        //load head and meta data
        head = headDataSource.getHead();
        meta = metaDataSource.getMeta();

        // display the editable transect No.
        lhw = new ListHeadWidget(this, null);
        lhw.setWidgetLNo(getString(R.string.transectnumber));
        lhw.setWidgetLNo1(head.transect_no);
        lhw.setWidgetLName(getString(R.string.inspector));
        lhw.setWidgetLName1(head.inspector_name);
        spec_area.addView(lhw);

        // display the editable meta data
        lmw = new ListMetaWidget(this, null);
        lmw.setWidgetLMeta1(getString(R.string.temperature));
        lmw.setWidgetLItem1(meta.tempe);
        lmw.setWidgetLMeta2(getString(R.string.wind));
        lmw.setWidgetLItem2(meta.wind);
        lmw.setWidgetLMeta3(getString(R.string.clouds));
        lmw.setWidgetLItem3(meta.clouds);
        lmw.setWidgetLDate1(getString(R.string.date));
        lmw.setWidgetLDate2(meta.date);
        lmw.setWidgetLTime1(getString(R.string.starttm));
        lmw.setWidgetLItem4(meta.start_tm);
        lmw.setWidgetLTime2(getString(R.string.endtm));
        lmw.setWidgetLItem5(meta.end_tm);
        spec_area.addView(lmw);

        // display all the sorted counts by adding them to listSpecies layout
        List<Count> specs; // List of species

        int sect_id;
        Section section;
        countDataSource.open();
        sectionDataSource.open();

        switch (sortPref)
        {
        case "names_alpha":
            specs = countDataSource.getAllCountsForSrtName();
            break;
        case "codes":
            specs = countDataSource.getAllCountsForSrtCode();
            break;
        default:
            specs = countDataSource.getAllCounts();
            break;
        }

        int spec_countf1i;
        int spec_countf2i;
        int spec_countf3i;
        int spec_countpi;
        int spec_countli;
        int spec_countei;
        int spec_countf1e;
        int spec_countf2e;
        int spec_countf3e;
        int spec_countpe;
        int spec_countle;
        int spec_countee;

        for (Count spec : specs)
        {
            ListSpeciesWidget widget = new ListSpeciesWidget(this, null);
            sect_id = widget.getSpec_sectionid(spec);
            section = sectionDataSource.getSection(sect_id);
            widget.setCount(spec, section);

            spec_countf1i = widget.getSpec_countf1i(spec);
            spec_countf2i = widget.getSpec_countf2i(spec);
            spec_countf3i = widget.getSpec_countf3i(spec);
            spec_countpi = widget.getSpec_countpi(spec);
            spec_countli = widget.getSpec_countli(spec);
            spec_countei = widget.getSpec_countei(spec);
            spec_countf1e = widget.getSpec_countf1e(spec);
            spec_countf2e = widget.getSpec_countf2e(spec);
            spec_countf3e = widget.getSpec_countf3e(spec);
            spec_countpe = widget.getSpec_countpe(spec);
            spec_countle = widget.getSpec_countle(spec);
            spec_countee = widget.getSpec_countee(spec);

            summf = summf + spec_countf1i;
            summ = summ + spec_countf2i;
            sumf = sumf + spec_countf3i;
            sump = sump + spec_countpi;
            suml = suml + spec_countli;
            sumo = sumo + spec_countei;
            summfe = summfe + spec_countf1e;
            summe = summe + spec_countf2e;
            sumfe = sumfe + spec_countf3e;
            sumpe = sumpe + spec_countpe;
            sumle = sumle + spec_countle;
            sumoe = sumoe + spec_countee;
        }

        sumInt = summf + summ + sumf + sump + suml + sumo;
        sumExt = summfe + summe + sumfe + sumpe + sumle + sumoe;

        // display the totals
        lsw = new ListSumWidget(this, null);
        lsw.setSum(summf, summ, sumf, sump, suml, sumo, summfe, summe, sumfe, sumpe, sumle, sumoe, sumInt, sumExt);
        spec_area.addView(lsw);

        for (Count spec : specs)
        {
            ListSpeciesWidget widget = new ListSpeciesWidget(this, null);
            sect_id = widget.getSpec_sectionid(spec);
            section = sectionDataSource.getSection(sect_id);
            widget.setCount(spec, section);
            spec_area.addView(widget);
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

    @Override
    protected void onStop()
    {
        super.onStop();

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
        ScrollView listSpec_screen = findViewById(R.id.listSpecScreen);
        listSpec_screen.setBackground(null);
        listSpec_screen.setBackground(transektCount.setBackground());
        awakePref = prefs.getBoolean("pref_awake", true);
        sortPref = prefs.getString("pref_sort_sp", "none"); // sorted species list
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
    }

}
