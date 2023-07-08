package sheetrock.panda.changelog;

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
 * Based on ChangeLog.java
 * Copyright (C) 2011-2013, Karsten Priegnitz
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
 * See: https://code.google.com/p/android-change-log/
 <p>
 * Adaptation for ViewHelp:
 * Last edited by wmstein on 2023-07-07
 */
public class ViewHelp
{
    private static final String TAG = "ViewHelp";

    private final Context context;
    private String thisVersion;
    private static final String NO_VERSION = "";
    private Listmode listMode = Listmode.NONE;
    private StringBuffer sb = null;

    public ViewHelp(Context context)
    {
        this.context = context;

        // get version numbers
        try
        {
            this.thisVersion = context.getPackageManager().getPackageInfo(
                context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e)
        {
            this.thisVersion = NO_VERSION;
            if (MyDebug.LOG)
                Log.e(TAG, "could not get version name from manifest!", e);
        }
    }

    /*********************************************************
     * @return an AlertDialog with a full change log displayed
     */
    public AlertDialog getFullLogDialog()
    {
        return this.getDialog();
    }

    private AlertDialog getDialog()
    {
        WebView wv = new WebView(this.context);

        wv.setBackgroundColor(Color.BLACK);
        wv.loadDataWithBaseURL(null, this.getLog(), "text/html", "UTF-8", null);

        AlertDialog.Builder builder = new AlertDialog.Builder(
            new ContextThemeWrapper(this.context, android.R.style.Theme_Holo_Dialog));
        builder.setTitle(context.getResources().getString(
                R.string.viewhelp_full_title) + " " + thisVersion + ")")
            .setView(wv)
            .setCancelable(false)
            // OK button
            .setPositiveButton(
                context.getResources().getString(
                    R.string.viewhelp_ok_button),
                (dialog, which) -> {
                });

        return builder.create();
    }

    /**
     * modes for HTML-Lists (bullet, numbered)
     */
    private enum Listmode
    {
        NONE, ORDERED, UNORDERED,
    }

    private String getLog()
    {
        // read viewhelp.txt file
        sb = new StringBuffer();
        try
        {
            String language = Locale.getDefault().toString().substring(0, 2);
            InputStream ins;
            if (language.equals("de"))
            {
                ins = context.getResources().openRawResource(R.raw.viewhelp_de);
            }
            else
            {
                ins = context.getResources().openRawResource(R.raw.viewhelp);
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(ins));
            String line;
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                char marker = line.length() > 0 ? line.charAt(0) : 0;
                if (marker == '$')
                {
                    // begin of a version section
                    this.closeList();
                }
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
                    case '&' ->
                    {
                        // line contains bold red text
                        this.closeList();
                        sb.append("<div class='boldredtext'>");
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
                    case ')' ->
                    {
                        // line contains small text
                        this.closeList();
                        sb.append("<div class='smalltext'>").append(line.substring(1).trim()).append("</div>\n");
                    }
                    case '#' ->
                    {
                        // line contains numbered list item
                        this.openList(Listmode.ORDERED);
                        sb.append("<li>");
                        sb.append(line.substring(1).trim());
                        sb.append("</li>\n");
                    }
                    case '*' ->
                    {
                        // line contains bullet list item
                        this.openList(Listmode.UNORDERED);
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
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private void openList(Listmode listMode)
    {
        if (this.listMode != listMode)
        {
            closeList();
            if (listMode == Listmode.ORDERED)
            {
                sb.append("<div class='list'><ol>\n");
            }
            else if (listMode == Listmode.UNORDERED)
            {
                sb.append("<div class='list'><ul>\n");
            }
            this.listMode = listMode;
        }
    }

    private void closeList()
    {
        if (this.listMode == Listmode.ORDERED)
        {
            sb.append("</ol></div>\n");
        }
        else if (this.listMode == Listmode.UNORDERED)
        {
            sb.append("</ul></div>\n");
        }
        this.listMode = Listmode.NONE;
    }
    
}
