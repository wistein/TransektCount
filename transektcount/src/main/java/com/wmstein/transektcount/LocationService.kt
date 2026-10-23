package com.wmstein.transektcount

/***************************************************************************************************
 * Copyright © 2025-2026, Wilhelm Stein, Bonn, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use any of its files except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************************************
 *
 * Gaining location data is based on LocationSrv created by anupamchugh on 2016-11-28, published on
 * https://github.com/journaldev/journaldev/tree/master/Android/GPSLocationTracking
 * Copyright © 2016 anupamchugh.
 *
 * Licensed under the MIT License (MIT):
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the “Software”), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ***************************************************************************************************
 *
 * LocationService does not rely on Google Play Services.
 * It provides periodic location data and determines the current section of a transect
 * with the help of corresponding section tracks.
 *
 * Partly taken from the LocationService.kt code of TourCount that was adopted from anupamchugh's
 * code by wmstein on 2018-07-26,
 * enhanced for TransektCount by wmstein on 2025-09-11,
 * last edited on 2026-10-21
 **************************************************************************************************/

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT

import androidx.core.app.ActivityCompat
import androidx.core.net.toUri

import com.wmstein.transektcount.TransektCountApplication.Companion.distMin
import com.wmstein.transektcount.TransektCountApplication.Companion.isFirstLoc
import com.wmstein.transektcount.TransektCountApplication.Companion.isSelectSectionActivityResumed
import com.wmstein.transektcount.TransektCountApplication.Companion.lat
import com.wmstein.transektcount.TransektCountApplication.Companion.lon
import com.wmstein.transektcount.TransektCountApplication.Companion.sectionIdGPS
import com.wmstein.transektcount.TransektCountApplication.Companion.sectionNameCurrent
import com.wmstein.transektcount.Utils.fromHtml
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource
import com.wmstein.transektcount.database.Track
import com.wmstein.transektcount.database.TrackDataSource

import java.text.DecimalFormat

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class LocationService : Service, LocationListener {
    var sContext: Context? = null
    private var audioAttributionContext: Context? = null
    private var locationAttributionContext: Context? = null
    private var locationManager: LocationManager? = null
    private var location: Location? = null
    private var checkGPS = false
    var canGetLocation = false
    private var sectionGPS: Section? = null
    private var sectionNameGPS = "" // current track section name
    private var isInsideTrack = true // inside of track (always true for manual section selection)
    private lateinit var trackPts: List<Track>

    // prefs
    private var selTimeInterval: Long = 3000 // Default time interval for updates
    private var maxDeviation = 5.0 // track width: default 5 meters
    private var minDistanceM: Long = 0 // No movement between updates necessary
    private var alertSoundPref = false
    private var alertSound = ""

    private var rToneA: MediaPlayer? = null

    // DB handling
    private var sectionDataSource: SectionDataSource? = null
    private var trackDataSource: TrackDataSource? = null
    private var isSelSectAct = false
    private var msg = ""

    // Default constructor is demanded for service declaration in AndroidManifest.xml
    constructor() // not to be removed!

    constructor(context: Context) {
        this.sContext = context // Gets ApplicationContext from call in WelcomeActivity
        audioAttributionContext =
            sContext!!.createAttributionContext("ringSound")
        locationAttributionContext =
            sContext!!.createAttributionContext("locationCheck")

        val prefs = TransektCountApplication.getPrefs()
        selTimeInterval = prefs.getString("pref_time_interval", "3000")!!.toLong()
        minDistanceM = prefs.getString("pref_distance", "0")!!.toLong()
        maxDeviation = prefs.getString("pref_deviation", "5")!!.toDouble()
        alertSoundPref = prefs.getBoolean("pref_alert_sound", false)
        alertSound = prefs.getString("alert_sound", "")!!

        sectionDataSource = SectionDataSource(sContext!!)
        trackDataSource = TrackDataSource(sContext!!)

        // Try to get list of trackpoints
        trackDataSource!!.open()
        trackPts = trackDataSource!!.getAllTrackPoints()
        trackDataSource!!.close()

        try {
            locationManager =
                locationAttributionContext!!.getSystemService(LOCATION_SERVICE) as LocationManager

            // Get GPS provider status
            checkGPS = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

            if (checkGPS) {
                this.canGetLocation = true
            } else {
                msg = sContext!!.getString(R.string.no_provider)
                Toast.makeText(
                    sContext!!,
                    fromHtml("<font color='red'><b>$msg</b></font>"),
                    LENGTH_SHORT
                ).show()
            }

            // if GPS is enabled get position using GPS service
            if (checkGPS && canGetLocation) {
                if (ActivityCompat.checkSelfPermission(
                        sContext!!,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationManager!!.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        selTimeInterval,
                        minDistanceM.toFloat(), this
                    )
                }
            }
        } catch (e: Exception) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "180, Error in getLocation: $e")
        }
    }

    /* Get current location and evaluate the location for a track of the current transect section:
     - Read tracks info,
     - compare position with track points,
     - identify section if its track contains the current position
     */
    override fun onLocationChanged(location: Location) {
        sectionNameGPS = checkSectionTrack()

        if (isFirstLoc && distMin != 0.0) {
            // Show message: GPS: Distance to track: distance m
            val dst = DecimalFormat("#.#").format(distMin)
            msg = sContext!!.getString(R.string.distanceToTrack) + " " + dst + " m"
            Toast.makeText( // bright green
                sContext!!,
                fromHtml("<bold><font color='#008000'>$msg</font></bold>"),
                LENGTH_SHORT
            ).show()
            isFirstLoc = false
        }

        if (sectionNameGPS != "") {
            sectionDataSource!!.open()
            sectionGPS = sectionDataSource!!.getSectionByName(sectionNameGPS)
//            sectionIdGPS = sectionGPS!!.id // todo?
            sectionDataSource!!.close()
            isInsideTrack = true
        } else {
            sectionGPS = null
//            sectionIdGPS = 1 // todo?
            isInsideTrack = false
        }

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "217, current SecName: $sectionNameCurrent")

        // Show message about new section if position isInsideTrack
        //  sectionNameCurrent is a global variable and is also set in SelectSectionAdapter
        if (isInsideTrack && sectionNameGPS != sectionNameCurrent) {
            // Beep and show message for mew recognized section
            soundAlert()

            // Pause 100 ms to play sound surely
            Handler(Looper.getMainLooper()).postDelayed({
                msg = sContext!!.getString(R.string.newSect) + " $sectionNameGPS"

                Toast.makeText( // bright green
                    sContext!!,
                    fromHtml("<bold><font color='#008000'>$msg</font></bold>"),
                    LENGTH_SHORT
                ).show()
            }, 100)

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "237, new SecName: $sectionNameGPS")

            sectionNameCurrent = sectionNameGPS
            sectionIdGPS = sectionGPS!!.id // highlight new GPS section in SelectSectionActivity

            // Start SelectSectionActivity if it called location
            isSelSectAct = isSelectSectionActivityResumed
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "245, isSelectSectionActivityRes: $isSelSectAct")

            // Reload SelectSectionActivity when it is active with new section marked blue
            if (isSelSectAct) {
                val intent = Intent(sContext, SelectSectionActivity::class.java)
                intent.flags = FLAG_ACTIVITY_NEW_TASK
                sContext!!.startActivity(intent)
            }

        } else if (isInsideTrack)
            sectionIdGPS = sectionGPS!!.id // highlight last GPS section in SelectSectionActivity
        else sectionIdGPS = 0 // don't highlight a section in SelectSectionActivity
    }

    /***********************************************************************
     * Get position and check if position is on track and matches a section,
     *   return the name of an identified section or an empty string
     */
    @SuppressLint("UseKtx", "DefaultLocale")
    private fun checkSectionTrack(): String {
        var tSName: String           // temporary GPS section name
        var dist: Double             // distance between location and nearest track point
        var distMinInit = 20000000.0 // initial min. distance bigger than expectable
        var tLat: String             // track point latitude
        var tLon: String             // track point longitude
        var tSecNam = ""             // section name from identified track

        if (ActivityCompat.checkSelfPermission(
                sContext!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (locationManager != null) {
                location = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (location != null) {
                    lat = location!!.latitude  // set global GPS lat variable
                    lon = location!!.longitude // set global GPS lon variable
                }
            }
        }

        // Read all TRACK_TABLE entries and find distMin for nearest track
        for (trackPt: Track in trackPts) {
            tSName = trackPt.tsection
            tLat = trackPt.tlat // Lat. from DB
            tLon = trackPt.tlon // Lon. from DB
            dist = sDistance(tLat, tLon, lat, lon)
            if (distMinInit > dist) {
                distMinInit = dist
                distMin = distMinInit
                tSecNam = tSName // tSecNam = name of nearest track
            }
        }

        // If smallest distance still > track width, then location is outside a section
        //   -> keep last known section marked
        if (distMin > maxDeviation) {
            if (sectionNameCurrent.isNotEmpty())
                tSecNam = ""
            else if (sectionNameCurrent.isEmpty())
                tSecNam = sectionNameCurrent
        }
        return tSecNam
    }

    /****************************************************************************
     * Calculate distance in meters between two points in latitude and longitude.
     * Based on method in stackoverflow.com/questions/3694380 by David George
     */
    private fun sDistance(latDB: String, lonDB: String, latGPS: Double, lonGPS: Double): Double {
        val eRad = 6371000 // Radius of the earth
        val lat1 = latDB.toDouble() // is correct for signed values
        val lon1 = lonDB.toDouble()

        val latDistance = Math.toRadians(latGPS - lat1)
        val lonDistance = Math.toRadians(lonGPS - lon1)
        val a = (sin(latDistance / 2) * sin(latDistance / 2)
                + cos(Math.toRadians(lat1)) * cos(Math.toRadians(latGPS))
                * sin(lonDistance / 2) * sin(lonDistance / 2)
                )
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        var distance = eRad * c

        distance = distance.pow(2.0)

        return sqrt(distance)
    }

    // Stop location listener
    fun stopListener() {
        try {
            if (locationManager != null) {
                locationManager!!.removeUpdates(this@LocationService)
                stopSelf()
                locationManager = null

                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "342, StopListener: Should stop GPS service.")
            }
        } catch (e: Exception) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "346, StopListener: $e")
        }

        if (alertSoundPref) {
            if (rToneA != null) {
                if (rToneA!!.isPlaying) {
                    rToneA!!.stop()
                }
                rToneA!!.release()
                rToneA = null
            }
        }
    }

    fun canGetLocation(): Boolean {
        return canGetLocation
    }

    override fun onProviderEnabled(s: String) {
        // do nothing
    }

    override fun onProviderDisabled(s: String) {
        // do nothing
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    // If the user has set the preference for an audible alert, then sound it here.
    private fun soundAlert() {
        if (alertSoundPref) {
            val uriA = if (isNotBlank(alertSound))
                alertSound.toUri()
            else
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            rToneA = MediaPlayer.create(audioAttributionContext, uriA)
            if (rToneA!!.isPlaying) {
                rToneA!!.stop()
                rToneA!!.release()
            }
            rToneA!!.start()
        }
    }

    // Release alert sound, called by WelcomeActivity
    fun releaseSoundA() {
        if (alertSoundPref && rToneA != null) {
            rToneA!!.reset()
            rToneA!!.release()
            rToneA = null
        }
    }

    // Stop alert sound, called by WelcomeActivity when denied in settings
    fun stopSoundA() {
        if (rToneA != null) {
            rToneA!!.reset()
            rToneA!!.release()
            rToneA = null
        }
    }

    override fun onDestroy() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "413, onDestroy")

        if (alertSoundPref && rToneA != null) {
            rToneA!!.reset()
            rToneA!!.release()
            rToneA = null
        }
        super.onDestroy()
    }

    fun isNotBlank(cs: CharSequence): Boolean {
        return !isBlank(cs)
    }

    fun isBlank(cs: CharSequence): Boolean {
        val strLen = cs.length
        if (strLen == 0) return true

        for (i in 0..strLen) {
            if (!Character.isWhitespace(cs[i])) return false
        }
        return true
    }

    companion object {
        private const val TAG = "LocationSrv"
    }

}
