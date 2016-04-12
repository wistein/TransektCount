package com.wmstein.transektcount;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.wmstein.transektcount.database.Count;
import com.wmstein.transektcount.database.CountDataSource;
import com.wmstein.transektcount.database.Section;
import com.wmstein.transektcount.database.SectionDataSource;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

/*********************
 * Create a new transect section list (NewCount) with name
 * Created by milo on 05/05/2014.
 * Changed by wmstein on 18.02.2016
 */

/**********************************************************************************************************************/
public class NewSectionActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "TransektCountNewSectionActivity";
    TransektCountApplication transektCount;
    SharedPreferences prefs;

    private Bitmap bMap;
    private BitmapDrawable bg;

    int newBox;
    private boolean dupPref;
    ViewGroup layout;
    private ArrayList<NewCount> myTexts;
    private ArrayList<String> countNames;
    EditText newsectName;
    SectionDataSource sectionDataSource;
    CountDataSource countDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_section);

        transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        dupPref = prefs.getBoolean("pref_duplicate", true);

        LinearLayout baseLayout = (LinearLayout) findViewById(R.id.newsectScreen);
        bMap = transektCount.decodeBitmap(R.drawable.kbackground, transektCount.width, transektCount.height);
        bg = new BitmapDrawable(baseLayout.getResources(), bMap);
        baseLayout.setBackground(bg);
        //baseLayout.setBackground(transektCount.getBackground());

        // data access using CrowTrack method
        sectionDataSource = new SectionDataSource(this);
        countDataSource = new CountDataSource(this);

        // setup from previous version
        newBox = 1;
        layout = (ViewGroup) findViewById(R.id.newCountLayout);
        myTexts = new ArrayList<>();
        newsectName = (EditText) findViewById(R.id.newsectName);
        newsectName.setTextColor(Color.WHITE);
        newsectName.setHintTextColor(Color.argb(255, 170, 170, 170));
        countNames = new ArrayList<>();

        if (savedInstanceState != null)
        {
            if (savedInstanceState.getSerializable("savedTexts") != null)
            {
                myTexts = (ArrayList<NewCount>) savedInstanceState.getSerializable("savedTexts");
                for (NewCount c : myTexts)
                {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(5, 5, 5, 5);
                    c.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                    c.setHint(this.getString(R.string.boxFill) + " " + newBox);
                    c.setBackgroundResource(R.drawable.rounded_corner);
                    c.setPadding(5, 5, 5, 5);
                    c.setTextSize(18);
                    c.setTextColor(Color.WHITE);
                    c.setHintTextColor(Color.argb(255, 170, 170, 170));
                    layout.addView(c, params);
                    newBox++;
                }
            }
        }
    }

    // the required pause and resume stuff
    @Override
    protected void onResume()
    {
        sectionDataSource.open();
        countDataSource.open();
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
    /*
     * Before these widgets can be serialised they must be removed from their parent, or else
     * trying to add them to a new parent causes a crash because they've already got one.
     */
        super.onSaveInstanceState(outState);
        for (NewCount c : myTexts)
        {
            ((ViewGroup) c.getParent()).removeView(c);
        }
        outState.putSerializable("savedTexts", myTexts);
    }

    @Override
    protected void onPause()
    {
        sectionDataSource.close();
        countDataSource.close();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_section, menu);
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
        else if (id == R.id.menuSaveExit)
        {
            saveSection(layout);
        }
        return super.onOptionsItemSelected(item);
    }

    public void newCount(View view)
    {
        // attempt to add a new EditText to an array thereof
        //Log.i(TAG,"Adding new count!");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(5, 5, 5, 5);
        NewCount c = new NewCount(this);
        c.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        c.setHint(this.getString(R.string.boxFill) + " " + newBox);
        c.setBackgroundResource(R.drawable.rounded_corner);
        c.setPadding(5, 5, 5, 5);
        c.setTextSize(18);
        c.setTextColor(Color.WHITE);
        c.setHintTextColor(Color.argb(255, 170, 170, 170));

        layout.addView(c, params);
        c.requestFocus();
        myTexts.add(c);
        newBox++;
    }

    public void clearCount(View view)
    {
        if (myTexts.isEmpty())
            return;
        int count_number = myTexts.size();
        ViewGroup layout = (ViewGroup) findViewById(R.id.newCountLayout);
        layout.removeView(myTexts.get(count_number - 1));
        myTexts.remove(count_number - 1);
        newBox--;
    }

    public void saveSection(View view)
    {
        // first, the section name
        String sect_name = newsectName.getText().toString();
        String count_name;

        if (myTexts.isEmpty())
        {
            Toast.makeText(this, getString(R.string.noCounts), Toast.LENGTH_SHORT).show();
            return;
        }

        // check that the boxes are filled in
        int carryon = 1;
        if (StringUtils.isBlank(sect_name))
        {
            carryon = 0;
        }
        for (NewCount c : myTexts)
        {
            count_name = c.getText().toString();
            if (StringUtils.isBlank(count_name))
            {
                carryon = 0;
                break;
            }
        }
        if (carryon == 0)
        {
            Toast.makeText(this, getString(R.string.emptyBox), Toast.LENGTH_SHORT).show();
            return;
        }

        // check for unique names
        if (dupPref)
        {
            countNames.clear();
            for (NewCount c : myTexts)
            {
                count_name = c.getText().toString();
                if (countNames.contains(count_name))
                {
                    Toast.makeText(this, getString(R.string.duplicate), Toast.LENGTH_SHORT).show();
                    return;
                }
                else
                {
                    countNames.add(count_name);
                }
            }
        }

    /*
     * Commence saving the section and its associated counts.
     */

        Section newSection = sectionDataSource.createSection(sect_name); // might need to escape the name
        for (NewCount c : myTexts)
        {
            count_name = c.getText().toString();
            Count newCount = countDataSource.createCount(newSection.id, count_name);
        }

        // Huzzah!
        Toast.makeText(this, getString(R.string.sectionSaved), Toast.LENGTH_SHORT).show();

        // Instead of returning to the welcome screen, show the new section.
        //super.finish();
        Intent intent = new Intent(NewSectionActivity.this, ListSectionActivity.class);
        //intent.putExtra("section_id",newSection.id);
        startActivity(intent);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        LinearLayout baseLayout = (LinearLayout) findViewById(R.id.newsectScreen);
        baseLayout.setBackground(null);
        bMap = transektCount.decodeBitmap(R.drawable.kbackground, transektCount.width, transektCount.height);
        bg = new BitmapDrawable(baseLayout.getResources(), bMap);
        baseLayout.setBackground(bg);
        //baseLayout.setBackground(transektCount.setBackground());

        dupPref = prefs.getBoolean("duplicate_counts", true);
    }
}
