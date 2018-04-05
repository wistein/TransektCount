package com.wmstein.filechooser;

import android.support.annotation.NonNull;

/**
 * Option is part of filechooser.
 * It will be called within AdvFileChooser.
 * Based on android-file-chooser, 2011, Google Code Archiv, GNU GPL v3.
 * Adopted by wmstein on 2016-06-18, 
 * last change on 2018-04-05
 */

public class Option implements Comparable<Option>
{
    private String name;
    private String data;
    private String path;
    private boolean back;

    public Option(String n, String d, String p, boolean back)
    {
        name = n;
        data = d;
        path = p;
        this.back = back;
    }

    public String getName()
    {
        return name;
    }

    public String getData()
    {
        return data;
    }

    public String getPath()
    {
        return path;
    }

    @Override
    public int compareTo(@NonNull Option o)
    {
        if (this.name != null)
            return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
        else
            throw new IllegalArgumentException();
    }
    
    public boolean isBack()
    {
        return back;
    }

}
