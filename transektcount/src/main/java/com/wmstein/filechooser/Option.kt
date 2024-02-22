package com.wmstein.filechooser

import java.util.Locale

/**
 * Option is part of filechooser.
 * It will be called within AdvFileChooser.
 * Based on android-file-chooser, 2011, Google Code Archiv, GNU GPL v3.
 * Adopted by wmstein on 2016-06-18,
 * last change in Java on 2021-01-26,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2023-12-15.
 */
class Option(val name: String?, val data: String, val path: String, val isBack: Boolean) :
    Comparable<Option> {

    override fun compareTo(other: Option): Int {
        return name?.lowercase(Locale.getDefault())?.compareTo(other.name!!
            .lowercase(Locale.getDefault()))?: throw IllegalArgumentException()
    }
}
