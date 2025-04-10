package com.example.lbsapp.tracking.modes

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.example.lbsapp.tracking.base.BaseTracker
import com.google.android.gms.location.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Implementierung des Trackings mit dem Fused Location Provider
 * Bietet eine bessere Energieeffizienz und kombiniert verschiedene Standortquellen
 */
class FusedLocationTracker(context: Context) : BaseTracker(context) {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var lastLocation: Location? = null

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    init {
        createLocationRequest()
        createLocationCallback()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 Sekunden zwischen Updates
            fastestInterval = 5000 // Schnellste Update-Rate
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY // Priorität für hohe Genauigkeit
        }
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    lastLocation = location
                    _locationData.postValue(location)
                }
            }
        }
    }

    override fun startTracking() {
        if (!hasPermissions()) {
            _error.postValue("Keine Standortberechtigungen")
            return
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            _isTracking.postValue(true)
        } catch (e: SecurityException) {
            _error.postValue("Keine Berechtigung für Standortdaten: ${e.message}")
        } catch (e: Exception) {
            _error.postValue("Fehler beim Starten des Fused Location Trackings: ${e.message}")
        }
    }

    override fun stopTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            _isTracking.postValue(false)
        } catch (e: Exception) {
            _error.postValue("Fehler beim Stoppen des Fused Location Trackings: ${e.message}")
        }
    }

    override fun getLastLocation(): Location? {
        if (!hasPermissions()) {
            _error.postValue("Keine Standortberechtigungen")
            return null
        }

        try {
            // Wenn wir noch keinen Standort haben, versuchen wir, den letzten bekannten abzurufen
            if (lastLocation == null) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        lastLocation = location
                        _locationData.postValue(location)
                    }
                }
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
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun getModeName(): String {
        return "Fused Location Provider"
    }

    /**
     * Ändert die Aktualisierungsrate für die Standortabfragen
     */
    fun setUpdateInterval(intervalMs: Long, fastestIntervalMs: Long) {
        locationRequest.interval = intervalMs
        locationRequest.fastestInterval = fastestIntervalMs

        // Wenn Tracking aktiv ist, Updates mit neuen Einstellungen neu starten
        if (_isTracking.value == true) {
            stopTracking()
            startTracking()
        }
    }

    /**
     * Ändert die Priorität der Standortabfragen
     */
    fun setPriority(priority: Int) {
        locationRequest.priority = priority

        // Wenn Tracking aktiv ist, Updates mit neuen Einstellungen neu starten
        if (_isTracking.value == true) {
            stopTracking()
            startTracking()
        }
    }
}