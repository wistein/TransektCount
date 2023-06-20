package com.wmstein.transektcount;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.wmstein.transektcount.database.Section;
import com.wmstein.transektcount.database.SectionDataSource;

import java.util.ArrayList;
import java.util.List;

/*********************************************************
 * Create a new empty transect section list (NewCount)
 * uses activity_new_section.xml
 * NewSectionLActivity is called from ListSectionLActivity.
 * Based on NewProjectActivity.java by milo on 05/05/2014,
 * changed by wmstein since 2016-02-16,
 * last edited on 2022-06-09
 */
public class NewSectionLActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "TransektCountNewSectAct";
    @SuppressLint("StaticFieldLeak")
    private static TransektCountApplication transektCount;
    SharedPreferences prefs;

    private Bitmap bMap;
    private BitmapDrawable bg;

    Section section;
    Section newSection;
    
//    ViewGroup layout;
    EditText newsectName;
    private SectionDataSource sectionDataSource;
    List<Section> sections = new ArrayList<>();

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
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    @Override
    // Inflate the menu; this adds items to the action bar if it is present.
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.new_section_l, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will automatically handle clicks on 
        int id = item.getItemId();
        if (id == R.id.menuSaveExit)
        {
            saveSection();
        }
        return super.onOptionsItemSelected(item);
    }

    // Save section with plausi-check for empty or duplicate section name
    @SuppressLint("ApplySharedPref")
    public void saveSection()
    {
        // first, edit the section name
        String sect_name = newsectName.getText().toString();
        sections = sectionDataSource.getAllSections(prefs);

        // check for empty section name
        if (sect_name.isEmpty())
        {
            showSnackbarRed(getString(R.string.newName));
            return;
        }

        // check if this is not a duplicate of an existing name
        if (compSectionNames(sect_name))
        {
            showSnackbarRed(sect_name + " " + getString(R.string.isdouble));
            return;
        }

        // check if section is contiguous
        int entries = -1, maxId = 0;
        try
        {
            entries = sectionDataSource.getNumEntries();
        } catch (Exception e)
        {
            //do nothing
        }

        try
        {
            maxId = sectionDataSource.getMaxId();
        } catch (Exception e)
        {
            //do nothing
        }

        if (entries != maxId)
        {
            showSnackbarRed(getString(R.string.notContiguous));
            return;
        }

        newSection = sectionDataSource.createSection(sect_name);
        sectionDataSource.saveSection(newSection);

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
        Intent intent = new Intent(NewSectionLActivity.this, EditSectionLActivity.class);
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
        Snackbar sB = Snackbar.make(view, str, Snackbar.LENGTH_LONG);
        sB.setTextColor(Color.RED);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        sB.show();
    }
    
}
