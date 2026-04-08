package com.wmstein.transektcount.database

/***********************************
 * Definitions for table Head
 *
 * Created by wmstein on 31.03.2016,
 * last edited in Java on 2022-03-23,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2026-03-17
 */
class Head {
    @JvmField
    var id = 0
    @JvmField
    var transect_no: String? = null
    @JvmField
    var inspector_name: String? = null

    @JvmField
    var data_language: String? = null
}