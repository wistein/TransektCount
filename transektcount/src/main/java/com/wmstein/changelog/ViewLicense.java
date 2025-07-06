package com.wmstein.changelog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.webkit.WebView;

import com.wmstein.transektcount.MyDebug;
import com.wmstein.transektcount.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

/************************************************************************
 * Parts taken from ChangeLog.java,
 * copyright (C) 2011-2013, Karsten Priegnitz
 <p>
 * Permission to use, copy, modify, and distribute this piece of software
 * for any purpose with or without fee is hereby granted, provided that
 * the above copyright notice and this permission notice appear in the
 * source code of all copies.
 <p>
 * It would be appreciated if you mention the author in your change log,
 * contributors list or the like.
 <p>
 * Author: Karsten Priegnitz
 * See: <a href="https://code.google.com/p/android-change-log/">...</a>
 <p>
 * Adaptation for ViewLicense by wmstein on 2024-07-16,
 * last edited on 2025-07-01
 */
public class ViewLicense
{
    private static final String TAG = "ViewLicense";
    private final Context context;
    private String thisVersion;
    private static final String NO_VERSION = "";
    private Listmode listMode = Listmode.NONE;
    private StringBuffer sb = null;

    public ViewLicense(Context context)
    {
        this.context = context;

        // get version name
        try
        {
            thisVersion = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e)
        {
            thisVersion = NO_VERSION;
            if (MyDebug.DLOG)
                Log.e(TAG, "60, Could not get version name from manifest!", e);
        }
    }

    /********************************************************
     * Return an AlertDialog with the license text displayed
     */
    public AlertDialog getDialog()
    {
        WebView wl = new WebView(this.context);

        wl.setBackgroundColor(Color.BLACK);
        wl.loadDataWithBaseURL(null, this.getLog(), "text/html", "UTF-8",
            null);

        AlertDialog.Builder builder = new AlertDialog.Builder(
            new ContextThemeWrapper(this.context, android.R.style.Theme_Material_Dialog));
        builder
            .setView(wl)
            .setTitle(context.getResources().getString(R.string.viewlicense_full_title) +
                    " " + thisVersion + ")\n")
            .setCancelable(false)
            // Just an OK button
            .setPositiveButton(
                context.getResources().getString(R.string.ok_button),
                (dialog, which) -> {});
        return builder.create();
    }

    private String getLog()
    {
        // Read viewlicense.txt file
        sb = new StringBuffer();
        try
        {
            String language = Locale.getDefault().toString().substring(0, 2);
            InputStream ins;
            if (language.equals("de"))
                ins = context.getResources().openRawResource(R.raw.viewlicense_de);
            else
                ins = context.getResources().openRawResource(R.raw.viewlicense);

            BufferedReader br = new BufferedReader(new InputStreamReader(ins));
            String line;
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                char marker = !line.isEmpty() ? line.charAt(0) : 0;
                switch (marker)
                {
                    case '%' ->
                    {
                        // line contains version title
                        this.closeList();
                        sb.append("<div class='title'>");
                        sb.append(line.substring(1).trim());
                        sb.append("</div>\n");
                    }
                    case '_' ->
                    {
                        // line contains version subtitle
                        this.closeList();
                        sb.append("<div class='subtitle'>");
                        sb.append(line.substring(1).trim());
                        sb.append("</div>\n");
                    }
                    case '!' ->
                    {
                        // line contains free text
                        this.closeList();
                        sb.append("<div class='freetext'>");
                        sb.append(line.substring(1).trim());
                        sb.append("</div>\n");
                    }
                    case '&' ->
                    {
                        // line contains bold text
                        this.closeList();
                        sb.append("<div class='boldtext'>");
                        sb.append(line.substring(1).trim());
                        sb.append("</div>\n");
                    }
                    case '*' ->
                    {
                        // line contains bullet list item
                        this.openList();
                        sb.append("<li>");
                        sb.append(line.substring(1).trim());
                        sb.append("</li>\n");
                    }
                    default ->
                    {
                        // no special character: just use line as is
                        this.closeList();
                        sb.append(line);
                        sb.append("\n");
                    }
                }
            }
            this.closeList();
            br.close();
            ins.close();
        } catch (IOException e)
        {
            if (MyDebug.DLOG)
                Log.e(TAG, "165, could not read license text.", e);
        }
        return sb.toString();
    }

    private void openList()
    {
        if (this.listMode != Listmode.UNORDERED)
        {
            closeList();
            sb.append("<div class='list'><ul>\n");
            this.listMode = Listmode.UNORDERED;
        }
    }

    private void closeList()
    {
        if (this.listMode == Listmode.UNORDERED)
            sb.append("</ul></div>\n");
        this.listMode = Listmode.NONE;
    }

    /**
     * Modes for HTML-Lists (none, bullet)
     */
    private enum Listmode
    {
        NONE, UNORDERED,
    }

}
