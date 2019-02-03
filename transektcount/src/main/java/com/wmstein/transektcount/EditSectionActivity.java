package com.wmstein.transektcount;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.wmstein.transektcount.database.AlertDataSource;
import com.wmstein.transektcount.database.Count;
import com.wmstein.transektcount.database.CountDataSource;
import com.wmstein.transektcount.database.Section;
import com.wmstein.transektcount.database.SectionDataSource;
import com.wmstein.transektcount.widgets.CountEditWidget;
import com.wmstein.transektcount.widgets.EditNotesWidget;
import com.wmstein.transektcount.widgets.EditTitleWidget;

import java.util.ArrayList;
import java.util.List;

/*************************************************************************
 * Edit the current section list (change, delete) and insert new species
 * EditSectionActivity is called from ListSectionActivity or CountingActivity.
 * Uses CountEditWidget.java, EditTitleWidget.java, EditNotesWidget.java,
 * activity_edit_section.xml, widget_edit_title.xml, widget_edit_notes.xml.
 * Based on EditProjectActivity.java by milo on 05/05/2014.
 * Changed by wmstein since 2016-02-16,
 * last edited on 2019-02-02
 */
public class EditSectionActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    TransektCountApplication transektCount;
    SharedPreferences prefs;
    public static String TAG = "TransektCountEditSectAct";

    // the actual data
    Section section;
    List<Count> counts;

    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;
    private AlertDataSource alertDataSource;

    int section_id;
    LinearLayout counts_area;
    LinearLayout notes_area2;
    EditTitleWidget etw;
    EditNotesWidget enw;
    private View markedForDelete;
    private int idToDelete;
    AlertDialog.Builder areYouSure;

    public ArrayList<String> countNames;
    public ArrayList<String> countCodes;
    public ArrayList<String> cmpCountNames;
    public ArrayList<String> cmpCountCodes;
    public ArrayList<Integer> countIds;
    public ArrayList<CountEditWidget> savedCounts;

    private Bitmap bMap;
    private BitmapDrawable bg;

    private boolean dupPref;
    private boolean screenOrientL; // option for screen orientation

    String new_count_name = "";
    String oldname;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_section);

        countNames = new ArrayList<>();
        countCodes = new ArrayList<>();
        countIds = new ArrayList<>();
        savedCounts = new ArrayList<>();

        notes_area2 = findViewById(R.id.editingNotesLayout);
        counts_area = findViewById(R.id.editingCountsLayout);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            section_id = extras.getInt("section_id");
        }

        /*
         * Restore any edit widgets the user has added previously
         */
        if (savedInstanceState != null)
        {
            if (savedInstanceState.getSerializable("savedCounts") != null)
            {
                //noinspection unchecked
                savedCounts = (ArrayList<CountEditWidget>) savedInstanceState.getSerializable("savedCounts");
            }
        }

        // Load preferences
        transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);

        boolean brightPref = prefs.getBoolean("pref_bright", true);
        dupPref = prefs.getBoolean("pref_duplicate", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        ScrollView counting_screen = findViewById(R.id.editingScreen);

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        bMap = transektCount.decodeBitmap(R.drawable.kbackground, transektCount.width, transektCount.height);
        bg = new BitmapDrawable(counting_screen.getResources(), bMap);
        counting_screen.setBackground(bg);
    }

    @SuppressLint("LongLogTag")
    @Override
    protected void onResume()
    {
        super.onResume();

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            section_id = extras.getInt("section_id");
        }

        // Load preferences
        transektCount = (TransektCountApplication) getApplication();
        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);

        boolean brightPref = prefs.getBoolean("pref_bright", true);
        dupPref = prefs.getBoolean("pref_duplicate", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        // build the Edit Section screen
        // clear any existing views
        counts_area.removeAllViews();
        notes_area2.removeAllViews();

        // setup the data sources
        sectionDataSource = new SectionDataSource(this);
        sectionDataSource.open();
        countDataSource = new CountDataSource(this);
        countDataSource.open();
        alertDataSource = new AlertDataSource(this);
        alertDataSource.open();

        // load the sections data
        section = sectionDataSource.getSection(section_id);
        oldname = section.name;
        try
        {
            getSupportActionBar().setTitle(oldname);
        } catch (NullPointerException e)
        {
            Log.i(TAG, "NullPointerException: No section name!");
        }

        // display the section title
        etw = new EditTitleWidget(this, null);
        etw.setSectionName(oldname);
        etw.setWidgetTitle(getString(R.string.titleEdit));
        notes_area2.addView(etw);

        // display editable section notes; the same class
        // is being used for both due to being lazy
        enw = new EditNotesWidget(this, null);
        enw.setSectionNotes(section.notes);
        enw.setWidgetNotes(getString(R.string.notesHere));
        enw.setHint(getString(R.string.notesHint));
        enw.requestFocus();
        notes_area2.addView(enw);

        // load the counts data
        counts = countDataSource.getAllCountsForSection(section.id);

        // display all the counts by adding them to CountEditWidget
        for (Count count : counts)
        {
            // widget
            CountEditWidget cew = new CountEditWidget(this, null);
            cew.setCountName(count.name);
            cew.setCountCode(count.code);
            cew.setCountId(count.id);
            counts_area.addView(cew);
        }
        for (CountEditWidget cew : savedCounts)
        {
            counts_area.addView(cew);
        }
        getCountNames();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
    /*
     * Before these widgets can be serialised they must be removed from their parent, or else
     * trying to add them to a new parent causes a crash because they've already got one.
     */
        super.onSaveInstanceState(outState);
        for (CountEditWidget cew : savedCounts)
        {
            ((ViewGroup) cew.getParent()).removeView(cew);
        }
        outState.putSerializable("savedCounts", savedCounts);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // close the data sources
        sectionDataSource.close();
        countDataSource.close();
        alertDataSource.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_section, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.home)
        {
            Intent intent = NavUtils.getParentActivityIntent(this);
            intent.putExtra("section_id", section_id);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            NavUtils.navigateUpTo(this, intent);
        }
        else if (id == R.id.menuSaveExit)
        {
            if (saveData())
            {
                savedCounts.clear();
                super.finish();
            }
        }
        else if (id == R.id.newCount)
        {
            newCount(findViewById(R.id.editingCountsLayout));
        }
        return super.onOptionsItemSelected(item);
    }

    public void getCountNames()
    {
    /*
     * The plan here is that names, codes and ids arrays contain the entries in the same
     * order, so I can link count and code name to its id by knowing the index.
     */
        countNames.clear();
        countCodes.clear();
        countIds.clear();
        int childcount = counts_area.getChildCount();
        for (int i = 0; i < childcount; i++)
        {
            CountEditWidget cew = (CountEditWidget) counts_area.getChildAt(i);
            String name = cew.getCountName();
            String code = cew.getCountCode();
            // ignore count widgets where the user has filled nothing in. 
            // Id will be 0 if this is a new count.
            if (isNotEmpty(name))
            {
                countNames.add(name);
                countCodes.add(code);
                countIds.add(cew.countId);
            }
        }
    }

    // Compare count names for duplicates and returns name of 1. duplicate found
    public String compCountNames()
    {
        String name;
        String isDblName = "";
        cmpCountNames = new ArrayList<>();

        int childcount = counts_area.getChildCount();
        // for all CountEditWidgets
        for (int i = 0; i < childcount; i++)
        {
            CountEditWidget cew = (CountEditWidget) counts_area.getChildAt(i);
            name = cew.getCountName();

            if (cmpCountNames.contains(name))
            {
                isDblName = name;
                //Log.i(TAG, "Double name = " + isDblName);
                break;
            }
            cmpCountNames.add(name);
        }
        return isDblName;
    }

    // Compare count codes for duplicates and returns name of 1. duplicate found
    public String compCountCodes()
    {
        String code;
        String isDblCode = "";
        cmpCountCodes = new ArrayList<>();

        int childcount = counts_area.getChildCount();
        // for all CountEditWidgets
        for (int i = 0; i < childcount; i++)
        {
            CountEditWidget cew = (CountEditWidget) counts_area.getChildAt(i);
            code = cew.getCountCode();

            if (cmpCountCodes.contains(code))
            {
                isDblCode = code;
                //Log.i(TAG, "Double name = " + isDblName);
                break;
            }
            cmpCountCodes.add(code);
        }
        return isDblCode;
    }

    public void saveAndExit(View view)
    {
        if (saveData())
        {
            savedCounts.clear();
            super.finish();
        }
    }

    public boolean saveData()
    {
        // save section notes only if they have changed
        boolean savesection;

        String newtitle = etw.getSectionName();

        if (isNotEmpty(newtitle))
        {
            //check if this is not a duplicate of an existing name
            if (compSectionNames(newtitle))
            {
//                Toast.makeText(EditSectionActivity.this, newtitle + " " + getString(R.string.isdouble), Toast.LENGTH_SHORT).show();
                showSnackbarRed(newtitle + " " + getString(R.string.isdouble));
                savesection = false;
            }
            else
            {
                section.name = newtitle;
                savesection = true;
            }
        }
        else
        {
//            Toast.makeText(EditSectionActivity.this, newtitle + " " + getString(R.string.isempty), Toast.LENGTH_SHORT).show();
            showSnackbarRed(newtitle + " " + getString(R.string.isempty));
            savesection = false;
        }

        // Always add notes if the user has written some...
        String newnotes = enw.getSectionNotes();
        if (isNotEmpty(newnotes) && savesection)
        {
            section.notes = newnotes;
        }
        //...if they haven't, only save if the current notes have a value
        else
        {
            if (isNotEmpty(section.notes))
            {
                section.notes = newnotes;
            }
        }

        boolean retValue = false;
        if (savesection)
        {
            sectionDataSource.saveSection(section);

            // save counts (species list)
            String isDblName;
            String isDblCode;
            int childcount; //No. of species in list
            childcount = counts_area.getChildCount();
            if (MyDebug.LOG)
                Log.d(TAG, "childcount: " + String.valueOf(childcount));

            // check for unique species names
            if (dupPref)
            {
                isDblName = compCountNames();
                isDblCode = compCountCodes();
                if (isDblName.equals("") && isDblCode.equals(""))
                {
                    // do for all species 
                    for (int i = 0; i < childcount; i++)
                    {
                        CountEditWidget cew = (CountEditWidget) counts_area.getChildAt(i);
                        if (isNotEmpty(cew.getCountName()) && isNotEmpty(cew.getCountCode()))
                        {
                            if (MyDebug.LOG)
                                Log.d(TAG, "cew: " + String.valueOf(cew.countId) + ", " + cew.getCountName());
                            // create or update
                            if (cew.countId == 0)
                            {
                                if (MyDebug.LOG)
                                    Log.d(TAG, "Creating!");
                                // creates new species entry
                                countDataSource.createCount(section_id, cew.getCountName(), cew.getCountCode());
                            }
                            else
                            {
                                if (MyDebug.LOG)
                                    Log.d(TAG, "Updating!");
                                // updates species name and code
                                countDataSource.updateCountName(cew.countId, cew.getCountName(), cew.getCountCode());
                            }
                            retValue = true;
                        }
                        else
                        {
//                            Toast.makeText(this, getString(R.string.isempt), Toast.LENGTH_SHORT).show();
                            showSnackbarRed(getString(R.string.isempt));
                            retValue = false;
                        }
                    }
                }
                else
                {
//                    Toast.makeText(this, getString(R.string.spname) + " " + isDblName + " " + getString(R.string.orcode) + " " 
//                      + isDblCode + " " + getString(R.string.isdouble), Toast.LENGTH_SHORT).show();
                    showSnackbarRed(getString(R.string.spname) + " " + isDblName + " " + getString(R.string.orcode) + " " + isDblCode + " "
                        + getString(R.string.isdouble));
                    retValue = false;
                }
            }

            if (retValue)
            {
                // Snackbar doesn't appear, so Toast is used
                Toast.makeText(EditSectionActivity.this, getString(R.string.sectSaving) + " " 
                    + section.name + "!", Toast.LENGTH_SHORT).show();
            }
        }
        return retValue;
    }

    // Compare section names for duplicates and return TRUE when duplicate found
    // created by wmstein on 10.04.2016
    public boolean compSectionNames(String newname)
    {
        boolean isDblName = false;
        String sname;

        if (newname.equals(oldname))
        {
            return false; // name has not changed
        }

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

    // Scroll to end of view, by wmstein
    public void ScrollToEndOfView(View scrlV)
    {
        int scroll_amount = scrlV.getBottom();
        int scrollY = scroll_amount;
        boolean pageend = false;
        while (!pageend)
        {
            scrlV.scrollTo(0, scroll_amount);              //scroll
            scroll_amount = scroll_amount + scroll_amount; //increase scroll_amount 
            scrollY = scrollY + scrlV.getScrollY();        //scroll position 1. row
            if (scroll_amount > scrollY)
            {
                pageend = true;
            }
        }
    }

    // add new CountEditWidget to view
    public void newCount(View view)
    {
        CountEditWidget cew = new CountEditWidget(this, null);
        counts_area.addView(cew); // adds a child view cew
        // Scroll to end of view, added by wmstein
        View scrollV = findViewById(R.id.editingScreen);
        ScrollToEndOfView(scrollV);
        // set focus to cew, added by wmstein
        cew.requestFocus();
        savedCounts.add(cew);
    }

    // purging counts (with associated alerts)
    public void deleteCount(View view)
    {
    /*
     * These global variables keep a track of the view containing an alert to be deleted and also the id
     * of the alert itself, to make sure that they're available inside the code for the alert dialog by
     * which they will be deleted.
     */
        markedForDelete = view;
        idToDelete = (Integer) view.getTag();
        if (idToDelete == 0)
        {
            // the actual CountEditWidget is two levels up from the button in which it is embedded
            counts_area.removeView((CountEditWidget) view.getParent().getParent());
        }
        else
        {
            // Before removing this widget it is necessary to do the following:
            //   (1) Check the user is sure they want to delete it and, if so...
            //   (2) Delete the associated alert from the database.
            areYouSure = new AlertDialog.Builder(this);
            areYouSure.setTitle(getString(R.string.deleteCount));
            areYouSure.setMessage(getString(R.string.reallyDeleteCount));
            areYouSure.setPositiveButton(R.string.yesDeleteIt, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    // go ahead for the delete
                    countDataSource.deleteCountById(idToDelete);
                    counts_area.removeView((CountEditWidget) markedForDelete.getParent().getParent());
                    new_count_name = "";
                }
            });
            areYouSure.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    // Cancelled.
                }
            });
            areYouSure.show();
        }
        getCountNames();
    }

    private void showSnackbarRed(String str) // bold red text
    {
        View view = findViewById(R.id.editingScreen);
        Snackbar sB = Snackbar.make(view, Html.fromHtml("<font color=\"#ff0000\"><b>" +  str + "</font></b>"), Snackbar.LENGTH_LONG);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        sB.show();
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView counting_screen = findViewById(R.id.editingScreen);
        dupPref = prefs.getBoolean("pref_duplicate", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        bMap = transektCount.decodeBitmap(R.drawable.kbackground, transektCount.width, transektCount.height);
        counting_screen.setBackground(null);
        bg = new BitmapDrawable(counting_screen.getResources(), bMap);
        counting_screen.setBackground(bg);
    }

    /**
     * Following functions are taken from the Apache commons-lang3-3.4 library
     * licensed under Apache License Version 2.0, January 2004
     * 
     * Checks if a CharSequence is not empty ("") and not null.
     * 
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
     * 
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
