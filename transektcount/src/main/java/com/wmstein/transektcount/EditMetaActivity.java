package com.wmstein.transektcount;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.wmstein.transektcount.database.Head;
import com.wmstein.transektcount.database.HeadDataSource;
import com.wmstein.transektcount.database.Meta;
import com.wmstein.transektcount.database.MetaDataSource;
import com.wmstein.transektcount.widgets.EditHeadWidget;
import com.wmstein.transektcount.widgets.EditMetaWidget;

import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/*
 * Created by wmstein on 31.03.2016.
 */
public class EditMetaActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "transektcountEditMetaActivity";
    TransektCountApplication transektCount;

    Head head;
    Meta meta;
    
    private HeadDataSource headDataSource;
    private MetaDataSource metaDataSource;

    int head_id;
    int meta_id;
    LinearLayout head_area;
    
    EditHeadWidget ehw;
    EditHeadWidget eiw;
    EditMetaWidget etw;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_head);

        head_area = (LinearLayout) findViewById(R.id.edit_head);
        
        transektCount = (TransektCountApplication) getApplication();

        ScrollView editHead_screen = (ScrollView) findViewById(R.id.editHeadScreen);
        editHead_screen.setBackground(transektCount.getBackground());
        try
        {
            getSupportActionBar().setTitle(getString(R.string.editHeadTitle));
        }
        catch (NullPointerException e)
        {
            Log.i(TAG, "NullPointerException: No head title!");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        
        //clear existing view
        head_area.removeAllViews();
        
        //setup data sources
        headDataSource = new HeadDataSource(this);
        headDataSource.open();
        metaDataSource = new MetaDataSource(this);
        metaDataSource.open();
        
        //load head and meta data
        head = headDataSource.getHead();
        meta = metaDataSource.getMeta();
        
        // display the editable transect No.
        ehw = new EditHeadWidget(this, null);
        ehw.setWidgetHead(getString(R.string.transectnumber));
        ehw.setWidgetItem(head.transect_no);
        head_area.addView(ehw);

        // display the editable inspector name
        eiw = new EditHeadWidget(this, null);
        eiw.setWidgetHead(getString(R.string.inspector));
        eiw.setWidgetItem(head.inspector_name);
        head_area.addView(eiw);
        
        // display the editable meta data
        etw = new EditMetaWidget(this, null);
        etw.setWidgetMeta1(getString(R.string.temperature));
        etw.setWidgetItem1(meta.temp);
        etw.setWidgetMeta2(getString(R.string.wind));
        etw.setWidgetItem2(meta.wind);
        etw.setWidgetMeta3(getString(R.string.clouds));
        etw.setWidgetItem3(meta.clouds);
        etw.setWidgetTime1(getString(R.string.starttm));
        etw.setWidgetItem4(meta.start_tm);
        etw.setWidgetTime2(getString(R.string.endtm));
        etw.setWidgetItem5(meta.end_tm);
        head_area.addView(etw);

        // check for focus
        String newTransectNo = head.transect_no;
        if (StringUtils.isNotEmpty(newTransectNo))
        {
            etw.requestFocus();
        }
        else
        {
            ehw.requestFocus();
        }

    }

    // getSTime()
    public void getSTime(View view)
    {
        ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE))
            .toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        EditText sTime = (EditText) this.findViewById(R.id.widgetItem4);
        sTime.setText(getcurTime());
    }

    // getETime()
    public void getETime(View view)
    {
        ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE))
            .toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        EditText eTime = (EditText) this.findViewById(R.id.widgetItem5);
        eTime.setText(getcurTime());
    }

    // Date for start_tm and end_tm
    // by wmstein
    public String getcurTime()
    {
        Date date = new Date();
        DateFormat dform = new SimpleDateFormat("HH:mm");
        return dform.format(date);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // close the data sources
        headDataSource.close();
        metaDataSource.close();
    }

    /***************/
    public void saveAndExit(View view)
    {
        saveData();
        super.finish();
    }

    public boolean saveData()
    {
        // Save head data
        head.transect_no = ehw.getWidgetItem();
        head.inspector_name = eiw.getWidgetItem();

        headDataSource.saveHead(head);

        // Save meta data
        meta.temp = etw.getWidgetItem1();
        meta.wind = etw.getWidgetItem2();
        meta.clouds = etw.getWidgetItem3();
        meta.start_tm = etw.getWidgetItem4();
        meta.end_tm = etw.getWidgetItem5();

        metaDataSource.saveMeta(meta);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_meta, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.menuSaveExit)
        {
            if (saveData())
                super.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView editHead_screen = (ScrollView) findViewById(R.id.editHeadScreen);
        editHead_screen.setBackground(null);
        editHead_screen.setBackground(transektCount.setBackground());
    }

}