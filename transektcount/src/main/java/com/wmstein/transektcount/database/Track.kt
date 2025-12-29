package com.wmstein.transektcount.database

/************************************************************************
 * Table Track interface with records of transect sections GPS track data
 * Created by wmstein on 2023-09-09,
 * last edited on 2025-07-02
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
