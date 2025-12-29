package com.wmstein.transektcount.database

/*************************************************************
 * Table Head interface with record of transect meta head data
 * Created by wmstein on 31.03.2016.
 * converted to Kotlin on 2023-06-26
 */
class Head {
    @JvmField
    var id = 0
    @JvmField
    var transect_no: String? = null
    @JvmField
    var inspector_name: String? = null
}