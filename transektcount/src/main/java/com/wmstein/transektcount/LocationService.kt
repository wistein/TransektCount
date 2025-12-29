package com.wmstein.transektcount

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import com.wmstein.transektcount.TransektCountApplication.Companion.distMin
import com.wmstein.transektcount.TransektCountApplication.Companion.isFirstLoc
import com.wmstein.transektcount.TransektCountApplication.Companion.lat
import com.wmstein.transektcount.TransektCountApplication.Companion.locServiceOn
import com.wmstein.transektcount.TransektCountApplication.Companion.lon
import com.wmstein.transektcount.TransektCountApplication.Companion.sectionIdGPS
import com.wmstein.transektcount.TransektCountApplication.Companion.sectionNameCurrent
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

/**************************************************************************************************
 * LocationService provides periodic location data and determines the current section of a transect
 * with the help of corresponding section tracks.
 *
 * Gaining location data is based on LocationSrv created by anupamchugh on 2016-11-28, published on
 * https://github.com/journaldev/journaldev/tree/master/Android/GPSLocationTracking
 * under MIT License.
 *
 * That code was adopted for TourCount and converted to kotlin by wmstein on 2023-08-16,
 * adapted and enhanced for TransektCount by wmstein on 2025-09-11,
 * last edited on 2025-12-29
 */
class LocationService : Service, LocationListener {
    var mContext: Context? = null
    protected var locationManager: LocationManager? = null
    private var location: Location? = null
    var checkGPS = false
    var canGetLocation = false
    private var sectionGPS: Section? = null
    private var sectionNameGPS: String = "" // current track section name
    private var isInsideTrack = true // inside of track (always true for manual section selection)
    private var distMax = 0.0 // track width: default 5 meters
    private lateinit var trackPts: List<Track>

    // prefs
    private var prefs: SharedPreferences? = null
    private var selTimeInterval: Long = 3000 // Default time interval for updates
    private var deviationAllowed: Long = 5 // Default min. distance for updates
    private val minDistanceM: Long = 0 // No movement between updates necessary
    private var alertSoundPref: Boolean = false
    private var alertSound: String = ""

    private var audioAttributionContext: Context? = null
    private var locationAttributionContext: Context? = null
    private var rToneA: MediaPlayer? = null

    // DB handling
    private var sectionDataSource: SectionDataSource? = null
    private var trackDataSource: TrackDataSource? = null

    // Default constructor is demanded for service declaration in AndroidManifest.xml
    constructor() {}

    constructor(mContext: Context?) {
        this.mContext = mContext // Gets ApplicationContext from call in WelcomActivity
        getLocation()
    }

    @SuppressLint("UseKtx")
    private fun getLocation() {
        audioAttributionContext =
            if (Build.VERSION.SDK_INT >= 30)
                mContext!!.createAttributionContext("ringSound")
            else this
        locationAttributionContext =
            if (Build.VERSION.SDK_INT >= 30)
                mContext!!.createAttributionContext("locationCheck")
            else this

        prefs = TransektCountApplication.getPrefs()
        selTimeInterval = prefs!!.getString("pref_time_interval", "3000")!!.toLong()
        deviationAllowed = prefs!!.getString("pref_distance", "5")!!.toLong()
        distMax = prefs!!.getString("pref_deviation", "5")!!.toDouble()
        alertSoundPref = prefs!!.getBoolean("pref_alert_sound", false)
        alertSound = prefs!!.getString("alert_sound", "")!!

        sectionDataSource = SectionDataSource(mContext!!)
        trackDataSource = TrackDataSource(mContext!!)

        // Prepare alert sound
        if (alertSoundPref) {
            val uriA = if (isNotBlank(alertSound))
                alertSound.toUri()
            else
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            rToneA = MediaPlayer.create(audioAttributionContext, uriA)
        }

        // Try to get list of trackpoints
        trackDataSource!!.open()
        trackPts = trackDataSource!!.getAllTrackPoints()
        trackDataSource!!.close()

        try {
            locationManager =
                locationAttributionContext!!.getSystemService(LOCATION_SERVICE) as LocationManager
            assert(locationManager != null)

            // Get GPS provider status
            checkGPS = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

            if (checkGPS) {
                this.canGetLocation = true
            } else {
                val mesg = mContext!!.getString(R.string.no_provider)
                Toast.makeText(
                    mContext!!,
                    HtmlCompat.fromHtml(
                        "<font color='red'><b>$mesg</b></font>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }

            // if GPS is enabled get position using GPS service
            if (checkGPS && canGetLocation) {
                if (ActivityCompat.checkSelfPermission(
                        mContext!!,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED
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
                Log.e(TAG, "165, StopListener: $e")
        }
    }

    /* Get current location and evaluate the location for track of the current transect section:
     - Read tracks info,
     - compare position with tracks,
     - identify section if its track contains the current position
     */
    fun getGPSSection() {
        sectionNameGPS = checkSectionTrack()

        if (isFirstLoc && distMin != 0.0) {
            // Show message: GPS: Distance to track: distance m
            val dst = DecimalFormat("#.#").format(distMin)
            val mesg = mContext!!.getString(R.string.distanceToTrack) + " " + dst + " m"
            Toast.makeText(
                mContext!!,
                HtmlCompat.fromHtml(
                    "<bold><font color='#008000'>$mesg</font></bold>",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ),
                Toast.LENGTH_SHORT
            ).show()
            isFirstLoc = false
        }

        if (sectionNameGPS != "") {
            sectionDataSource!!.open()
            sectionGPS = sectionDataSource!!.getSectionByName(sectionNameGPS)
            sectionDataSource!!.close()
            isInsideTrack = true
        } else {
            sectionGPS = null
            isInsideTrack = false
        }

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "203, current SecName: $sectionNameCurrent")

        // Show message about new section if position isInsideTrack
        //  sectionNameCurrent is a global variable and is also set in SelectSectionAdapter
        if (isInsideTrack && sectionNameGPS != sectionNameCurrent && locServiceOn) {
            // Beep and show message for mew recognized section
            soundAlert()

            // Pause 100 ms to play sound surely
            Handler(Looper.getMainLooper()).postDelayed({
                val mesg = mContext!!.getString(R.string.newSect) + " $sectionNameGPS"
                Toast.makeText(
                    mContext!!,
                    HtmlCompat.fromHtml(
                        "<bold><font color='#008000'>$mesg</font></bold>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }, 100)

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) {
                Log.d(TAG, "225, is InsideTrack: $isInsideTrack")
                Log.d(TAG, "226, new SecName: $sectionNameGPS")
            }

            sectionNameCurrent = sectionNameGPS
            sectionIdGPS = sectionGPS!!.id // highlight new GPS section in SelectSectionActivity
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
                mContext!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (locationManager != null) {
                location = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (location != null) {
                    lat = location!!.latitude  // set global GPS lat variable
                    lon = location!!.longitude // set global GPS lon variable
                }
            }
        }

        // Read all TRACK_TABLE entries and find distMin
        for (trackpt: Track in trackPts) {
            tSName = trackpt.tsection.toString()
            tLat = trackpt.tlat.toString() // Lat. from DB
            tLon = trackpt.tlon.toString() // Lon. from DB
            dist = sDistance(tLat, tLon, lat, lon)
            if (distMinInit > dist) {
                distMinInit = dist
                distMin = distMinInit
                tSecNam = tSName // tSecNam = name of nearest track
            }
        }

        // If smallest distance still > track width, than location is outside a section
        //   -> keep last known section marked
        if (distMin > distMax) {
            if (!sectionNameCurrent.isEmpty())
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
                locationManager = null

                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "319, StopListener: Should stop GPS service.")
            }
        } catch (e: Exception) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "323, StopListener: $e")
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

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onLocationChanged(location: Location) {
        getGPSSection()
    }

    override fun onProviderEnabled(s: String) {
        // do nothing
    }

    override fun onProviderDisabled(s: String) {
        // do nothing
    }

    // If the user has set the preference for an audible alert, then sound it here.
    private fun soundAlert() {
        if (alertSoundPref) {
            if (rToneA!!.isPlaying) {
                rToneA!!.stop()
                rToneA!!.release()
            }
            rToneA!!.start()
        }
    }

    companion object {
        private const val TAG = "LocationSrv"

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
    }

}
