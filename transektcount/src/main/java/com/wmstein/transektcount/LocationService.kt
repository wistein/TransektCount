package com.wmstein.transektcount

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat

/******************************************************************
 * LocationService provides the location data: latitude, longitude, uncertainty.
 * Its data is used to detect the current section of the transect.
 *
 * Based on LocationSrv created by anupamchugh on 28/11/16, published under
 * https://github.com/journaldev/journaldev/tree/master/Android/GPSLocationTracking
 * licensed under the MIT License.
 *
 * Adopted for TransektCount by wmstein since 2023-08-16,
 * last edited on 2023-09-17
 */
open class LocationService : Service, LocationListener {
    private var mContext: Context? = null
    private var checkGPS = false
    private var checkNetwork = false
    var canGetLocation = false
    var location: Location? = null
    private var lat = 0.0
    private var lon = 0.0
    private var locationManager: LocationManager? = null
    private var exactLocation = false

    // Default constructor is demanded for service declaration in AndroidManifest.xml
    constructor()

    constructor(mContext: Context?) {
        this.mContext = mContext
        getLocation()
    }

    private fun getLocation() {
        try {
            locationManager = mContext!!.getSystemService(LOCATION_SERVICE) as LocationManager
            assert(locationManager != null)
            checkGPS = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

            // get network provider status
            checkNetwork = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (checkGPS || checkNetwork) {
                canGetLocation = true
            } else {
                Toast.makeText(mContext, getString(R.string.no_provider), Toast.LENGTH_SHORT).show()
            }

            // if GPS is enabled get position using GPS Service
            if (checkGPS && canGetLocation) {
                if (ActivityCompat.checkSelfPermission(
                        mContext!!,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) //                    || ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED)
                {
                    locationManager!!.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_FOR_UPDATES.toFloat(), this
                    )
                    if (locationManager != null) {
                        location =
                            locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (location != null) {
                            lat = location!!.latitude
                            lon = location!!.longitude
                            exactLocation = true
                        }
                    }
                }
            }
            if (!exactLocation) {
                // if Network is enabled and still no GPS fix achieved
                if (checkNetwork && canGetLocation) {
                    if (ActivityCompat.checkSelfPermission(
                            mContext!!,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) //                        || ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    {
                        locationManager!!.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_FOR_UPDATES.toFloat(), this
                        )
                        if (locationManager != null) {
                            location =
                                locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            if (location != null) {
                                lat = location!!.latitude
                                lon = location!!.longitude
                                exactLocation = false
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Stop location service
    fun stopListener() {
        try {
            if (locationManager != null) {
                if (ActivityCompat.checkSelfPermission(
                        mContext!!,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(
                        mContext!!,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_GPS);
                    return
                }
                locationManager!!.removeUpdates(this@LocationService)
                locationManager = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "StopListener: $e")
        }
    }

    open fun getLongitude(): Double {
        if (location != null) {
            lon = location!!.longitude
        }
        return lon
    }

    open fun getLatitude(): Double {
        if (location != null) {
            lat = location!!.latitude
        }
        return lat
    }

    fun canGetLocation(): Boolean {
        return canGetLocation
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onLocationChanged(location: Location) {
        // do nothing
    }

    override fun onProviderEnabled(s: String) {
        // do nothing
    }

    override fun onProviderDisabled(s: String) {
        // do nothing
    }

    companion object {
        private const val TAG = "TransektCountLocationSrv"
        private const val MIN_DISTANCE_FOR_UPDATES: Long = 1 // (m)
        private const val MIN_TIME_BW_UPDATES: Long = 1000 // (msec)
    }
}