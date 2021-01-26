package com.wmstein.filechooser;

import androidx.annotation.NonNull;

/**
 * Option is part of filechooser.
 * It will be called within AdvFileChooser.
 * Based on android-file-chooser, 2011, Google Code Archiv, GNU GPL v3.
 * Adopted by wmstein on 2016-06-18, 
 * last change on 2021-01-26
 */
public class Option implements Comparable<Option>
{
    private final String name;
    private final String data;
    private final String path;
    private final boolean back;

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

    String getPath()
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
    
    boolean isBack()
    {
        return back;
    }

}
