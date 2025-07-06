package com.wmstein.changelog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.webkit.WebView;

import androidx.preference.PreferenceManager;

import com.wmstein.transektcount.MyDebug;
import com.wmstein.transektcount.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

/**********************************************************************
 Based on ChangeLog.java, copyright (C) 2011-2013, Karsten Priegnitz

 Permission to use, copy, modify, and distribute this piece of software
 for any purpose with or without fee is hereby granted, provided that
 the above copyright notice and this permission notice appear in the
 source code of all copies.

 It would be appreciated if you mention the author in your change log,
 contributors list or the like.

 Author: Karsten Priegnitz
 See: <a href="https://code.google.com/p/android-change-log/">...</a>

 Newly installed: Shows the history of TransektCount.
 Updated: Shows the last changes of TransektCount.

 Therefore retrieves the version names and stores the new version name in SharedPreferences

 Adopted for TransektCount by wm.stein on 2016-02-12,
 last change by wmstein on 2025-07-01
 */
public class ChangeLog
{
    private static final String TAG = "ChangeLog";
    private final Context context;
    private final String lastVersion;
    private String thisVersion;

    // key for storing the version name in SharedPreferences
    private static final String VERSION_KEY = "PREFS_VERSION_KEY";
    private static final String NO_VERSION = "";
    private Listmode listMode = Listmode.NONE;
    private StringBuffer sb = null;
    private static final String EOCL = "END_OF_CHANGE_LOG";

public ChangeLog(Context context, SharedPreferences prefs)
    {
        this.context = context;

        // Get version numbers of last Version and this Version to compare
        this.lastVersion = prefs.getString(VERSION_KEY, NO_VERSION);
        if (MyDebug.DLOG)
            Log.d(TAG, "66, lastVersion: " + lastVersion);

        try
        {
            thisVersion = context.getPackageManager().getPackageInfo(
                context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e)
        {
            thisVersion = NO_VERSION;
            if (MyDebug.DLOG)
                Log.e(TAG, "76, Could not get version name from manifest!", e);
        }
        if (MyDebug.DLOG)
            Log.d(TAG, "79, appVersion: " + this.thisVersion);
    }

    /**
     * Return true if this version of your app is started the first time
     */
    public boolean firstRun()
    {
        return !this.lastVersion.equals(this.thisVersion);
    }

    /**
     * Return true if your app including ChangeLog is started the first time ever.
     * Return also true if your app was deinstalled and reinstalled again.
     */
    private boolean firstRunEver()
    {
        return NO_VERSION.equals(this.lastVersion);
    }

    /**
     * Return an AlertDialog displaying the changes since the previous installed
     * version of your app (what's new). But when this is the first run of your app
     * including ChangeLog then the full log dialog is show.
     */
    public AlertDialog getLogDialog()
    {
        return this.getDialog(this.firstRunEver());
    }

    /**
     * Return an AlertDialog with a full change log displayed
     */
    public AlertDialog getFullLogDialog()
    {
        return this.getDialog(true);
    }

    private AlertDialog getDialog(boolean full)
    {
        WebView wv = new WebView(this.context);

        wv.setBackgroundColor(Color.BLACK);
        wv.loadDataWithBaseURL(null, this.getLog(full), "text/html",
            "UTF-8", null);

        String fullTitle = context.getResources().getString(R.string.changelog_full_title) +
            " " + thisVersion + ")\n";
        String changeTitle = "Ver. " + thisVersion + ": "
            + context.getResources().getString(R.string.changelog_title);

        AlertDialog.Builder builder = new AlertDialog.Builder(
            new ContextThemeWrapper(this.context, android.R.style.Theme_Material_Dialog));
        builder
            .setView(wv)
            .setTitle(full ? fullTitle : changeTitle)
            .setCancelable(false)
            // OK button
            .setPositiveButton(context.getResources().getString(
                    R.string.ok_button),
                    (dialog, which) -> updateVersionInPreferences());

        if (!full)
        {
            // "more ..." button
            builder.setNegativeButton(R.string.changelog_show_full, (dialog, id) -> getFullLogDialog().show());
        }
        return builder.create();
    }

    private void updateVersionInPreferences()
    {
        // save new version number to preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(VERSION_KEY, thisVersion);
        editor.apply();
    }

    /**
     * Return HTML displaying the changes since the previous installed version
     *   of TransektCount (what's new)
     */
    public String getLog()
    {
        return this.getLog(false);
    }

    private String getLog(boolean full)
    {
        // read changelog.txt file
        sb = new StringBuffer();
        try
        {
            String language = Locale.getDefault().toString().substring(0, 2);
            InputStream ins;
            if (language.equals("de"))
                ins = context.getResources().openRawResource(R.raw.changelog_de);
            else
                ins = context.getResources().openRawResource(R.raw.changelog);
            BufferedReader br = new BufferedReader(new InputStreamReader(ins));
            boolean advanceToEOVS = false; // if true: ignore further version sections
            String line;
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                char marker = !line.isEmpty() ? line.charAt(0) : 0;

                // begin of a version section
                if (marker == '$')
                {
                    this.closeList();
                    String version = line.substring(1).trim();
                    // stop output?
                    if (!full)
                    {
                        if (this.lastVersion.equals(version))
                            advanceToEOVS = true;
                        else if (version.equals(EOCL))
                            advanceToEOVS = false;
                    }
                }
                // other text
                else if (!advanceToEOVS)
                {
                    switch (marker)
                    {
                        // line contains version title
                        case '%' ->
                        {
                            this.closeList();
                            sb.append("<div class='title'>");
                            sb.append(line.substring(1).trim());
                            sb.append("</div>\n");
                        }
                        // line contains version subtitle
                        case '_' ->
                        {
                            this.closeList();
                            sb.append("<div class='subtitle'>");
                            sb.append(line.substring(1).trim());
                            sb.append("</div>\n");
                        }
                        // line contains free text
                        case '!' ->
                        {
                            this.closeList();
                            sb.append("<div class='freetext'>");
                            sb.append(line.substring(1).trim());
                            sb.append("</div>\n");
                        }
                        // line contains bold red text
                        case '&' ->
                        {
                            this.closeList();
                            sb.append("<div class='boldtext'>");
                            sb.append(line.substring(1).trim());
                            sb.append("</div>\n");
                        }
                        // line contains numbered list item
                        case '#' ->
                        {
                            this.openList(Listmode.ORDERED);
                            sb.append("<li>");
                            sb.append(line.substring(1).trim());
                            sb.append("</li>\n");
                        }
                        // line contains bullet list item
                        case '*' ->
                        {
                            this.openList(Listmode.UNORDERED);
                            sb.append("<li>");
                            sb.append(line.substring(1).trim());
                            sb.append("</li>\n");
                        }
                        // just use line as is
                        default ->
                        {
                            this.closeList();
                            sb.append(line);
                            sb.append("\n");
                        }
                    }
                }
            }
            this.closeList();
            br.close();
            ins.close();
        } catch (IOException e)
        {
            if (MyDebug.DLOG) Log.e(TAG, "269, could not read changelog text.", e);
        }

        return sb.toString();
    }

    private void openList(Listmode listMode)
    {
        if (this.listMode != listMode)
        {
            closeList();
            if (listMode == Listmode.ORDERED)
                sb.append("<div class='list'><ol>\n");
            else if (listMode == Listmode.UNORDERED)
                sb.append("<div class='list'><ul>\n");
            this.listMode = listMode;
        }
    }

    private void closeList()
    {
        if (this.listMode == Listmode.ORDERED)
            sb.append("</ol></div>\n");
        else if (this.listMode == Listmode.UNORDERED)
            sb.append("</ul></div>\n");
        this.listMode = Listmode.NONE;
    }

    /**
     * modes for HTML-Lists (none, numbered, bullet)
     */
    private enum Listmode
    {
        NONE, ORDERED, UNORDERED,
    }

}
