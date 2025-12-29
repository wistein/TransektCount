package com.wmstein.transektcount.database

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date

/***************************************************************
 * Table Section interface with record of transect sections data
 * Based on Project.java by milo on 05/05/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016
 * last edited in Java on 2019-08-22,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2025-05-15
 */
class Section {
    @JvmField
    var id = 0
    @JvmField
    var created_at: Long = 0
    @JvmField
    var name: String? = null
    @JvmField
    var notes: String? = null

    //Get date and time from DB table sections field created_at
    val dateTime: String
        @SuppressLint("SimpleDateFormat")
        get() {
            val date = Date(created_at)
            val df = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss")
            return df.format(date)
        }

    fun datNum(): Long {
        return created_at
    }

}