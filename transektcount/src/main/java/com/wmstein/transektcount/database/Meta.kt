package com.wmstein.transektcount.database

/********************************************************
 * Table Meta interface with record of transect metadata
 * Created by wmstein on 31.03.2016.
 * Converted to Kotlin on 2024-03-09,
 * last edited on 2026-02-21.
 */
class Meta {
    @JvmField
    var id = 0
    @JvmField
    var temps = 0
    @JvmField
    var tempe = 0
    @JvmField
    var winds = 0
    @JvmField
    var winde = 0
    @JvmField
    var clouds = 0
    @JvmField
    var cloude = 0
    @JvmField
    var date: String? = null     // Formet: EN: yyyy-mm-dd, DE: dd.mm.yyyy
    @JvmField
    var start_tm: String? = null // Format: hh:mm
    @JvmField
    var end_tm: String? = null   // Format: hh:mm
    @JvmField
    var note: String? = null
}