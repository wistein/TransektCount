package com.wmstein.transektcount.database

/***********************************
 * Created by wmstein on 2023-09-09,
 * last edited on 2023-10-01
 */
class Track {
    @JvmField
    var id = 0 // track point ID
    @JvmField
    var tsection: String? = null // track section name
    @JvmField
    var tlat: String? = null // track point latitude
    @JvmField
    var tlon: String? = null // track point longitude
}