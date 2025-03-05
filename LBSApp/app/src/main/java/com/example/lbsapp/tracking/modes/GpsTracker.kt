package com.example.lbsapp.tracking.modes

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.example.lbsapp.tracking.base.BaseTracker
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Implementierung des Trackings mit dem GPS-Anbieter
 */
class GpsTracker(context: Context) : BaseTracker(context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var lastLocation: Location? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastLocation = location
            _locationData.postValue(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            // Nicht mehr verwendet in neueren Android-Versionen, aber zur Kompatibilität implementiert
        }

        override fun onProviderEnabled(provider: String) {
            // Provider wurde aktiviert
        }

        override fun onProviderDisabled(provider: String) {
            _error.postValue("GPS wurde deaktiviert")
        }
    }

    override fun startTracking() {
        if (!hasPermissions()) {
            _error.postValue("Keine Standortberechtigungen")
            return
        }

        try {
            // Prüfen, ob GPS aktiviert ist
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                _error.postValue("GPS ist nicht aktiviert")
                return
            }

            // Starte GPS-Updates
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000, // Minimale Zeit zwischen Updates (ms)
                10f,  // Minimale Distanz zwischen Updates (m)
                locationListener,
                Looper.getMainLooper()
            )

            _isTracking.postValue(true)
        } catch (e: SecurityException) {
            _error.postValue("Keine Berechtigung für Standortdaten: ${e.message}")
        } catch (e: Exception) {
            _error.postValue("Fehler beim Starten des GPS-Trackings: ${e.message}")
        }
    }

    override fun stopTracking() {
        try {
            locationManager.removeUpdates(locationListener)
            _isTracking.postValue(false)
        } catch (e: Exception) {
            _error.postValue("Fehler beim Stoppen des GPS-Trackings: ${e.message}")
        }
    }

    override fun getLastLocation(): Location? {
        if (!hasPermissions()) {
            _error.postValue("Keine Standortberechtigungen")
            return null
        }

        try {
            // Versuche, den letzten bekannten Standort zu bekommen, falls wir noch keinen haben
            if (lastLocation == null) {
                lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            return lastLocation
        } catch (e: SecurityException) {
            _error.postValue("Keine Berechtigung für Standortdaten: ${e.message}")
        } catch (e: Exception) {
            _error.postValue("Fehler beim Abrufen des letzten Standorts: ${e.message}")
        }
        return null
    }

    override fun hasPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun getModeName(): String {
        return "GPS Tracking"
    }
}