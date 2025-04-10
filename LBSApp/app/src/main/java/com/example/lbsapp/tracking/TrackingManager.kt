package com.example.lbsapp.tracking

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.example.lbsapp.tracking.base.BaseTracker
import com.example.lbsapp.tracking.modes.FusedLocationTracker
import com.example.lbsapp.tracking.modes.GeofencingTracker
import com.example.lbsapp.tracking.modes.GpsTracker
import com.example.lbsapp.tracking.models.TrackingMode

/**
 * Manager-Klasse, die alle Tracking-Modi verwaltet und koordiniert
 */
class TrackingManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: TrackingManager? = null

        fun getInstance(context: Context): TrackingManager {
            return instance ?: synchronized(this) {
                instance ?: TrackingManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // Die verfügbaren Tracker
    private val gpsTracker = GpsTracker(context)
    private val fusedLocationTracker = FusedLocationTracker(context)
    private val geofencingTracker = GeofencingTracker(context)

    // Der aktuelle aktive Tracker
    private var currentTracker: BaseTracker? = null

    // LiveData für aktuelle Tracking-Modi
    private val _currentMode = MutableLiveData<TrackingMode>(TrackingMode.GPS)
    val currentMode: LiveData<TrackingMode> = _currentMode

    // LiveData für Tracking-Status
    private val _isTracking = MutableLiveData<Boolean>(false)
    val isTracking: LiveData<Boolean> = _isTracking

    // LiveData für Fehler-Benachrichtigungen, vereint Fehler aller Tracker
    private val _error = MediatorLiveData<String>()
    val error: LiveData<String> = _error

    // LiveData für Standortdaten, gibt den Standort des aktiven Trackers weiter
    private val _locationData = MediatorLiveData<android.location.Location>()
    val locationData: LiveData<android.location.Location> = _locationData

    init {
        // Fehler und Standortupdates von allen Trackern beobachten
        setupLiveDataObservers()
    }

    private fun setupLiveDataObservers() {
        // GPS Tracker
        _error.addSource(gpsTracker.error) { _error.value = it }
        _locationData.addSource(gpsTracker.locationData) { if (currentTracker == gpsTracker) _locationData.value = it }

        // Fused Location Tracker
        _error.addSource(fusedLocationTracker.error) { _error.value = it }
        _locationData.addSource(fusedLocationTracker.locationData) { if (currentTracker == fusedLocationTracker) _locationData.value = it }

        // Geofencing Tracker
        _error.addSource(geofencingTracker.error) { _error.value = it }
        _locationData.addSource(geofencingTracker.locationData) { if (currentTracker == geofencingTracker) _locationData.value = it }
    }

    /**
     * Startet das Tracking mit dem aktuell ausgewählten Modus
     */
    fun startTracking() {
        if (_isTracking.value == true) {
            stopTracking() // Stoppe zuerst, falls bereits aktiv
        }

        currentTracker = when (_currentMode.value) {
            TrackingMode.GPS -> gpsTracker
            TrackingMode.FUSED_LOCATION -> fusedLocationTracker
            TrackingMode.GEOFENCING -> geofencingTracker
            else -> gpsTracker // Fallback auf GPS
        }

        currentTracker?.startTracking()
        _isTracking.value = true
    }

    /**
     * Stoppt das aktive Tracking
     */
    fun stopTracking() {
        currentTracker?.stopTracking()
        _isTracking.value = false
    }

    /**
     * Wechselt den Tracking-Modus und startet ihn neu, falls aktiv
     */
    fun setTrackingMode(mode: TrackingMode) {
        if (_currentMode.value == mode) {
            return // Modus ist bereits gesetzt
        }

        val wasTracking = _isTracking.value == true

        // Stoppe aktuellen Tracker
        if (wasTracking) {
            stopTracking()
        }

        // Setze neuen Modus
        _currentMode.value = mode

        // Starte neuen Tracker, falls vorher aktiv
        if (wasTracking) {
            startTracking()
        }
    }

    /**
     * Gibt den letzten bekannten Standort zurück
     */
    fun getLastLocation(): android.location.Location? {
        return currentTracker?.getLastLocation()
    }

    /**
     * Prüft, ob der aktuelle Tracker die nötigen Berechtigungen hat
     */
    fun hasPermissions(): Boolean {
        return when (_currentMode.value) {
            TrackingMode.GPS -> gpsTracker.hasPermissions()
            TrackingMode.FUSED_LOCATION -> fusedLocationTracker.hasPermissions()
            TrackingMode.GEOFENCING -> geofencingTracker.hasPermissions()
            else -> false
        }
    }

    /**
     * Gibt Zugriff auf den GPS-Tracker für spezifische Konfigurationen
     */
    fun getGpsTracker(): GpsTracker {
        return gpsTracker
    }

    /**
     * Gibt Zugriff auf den Fused Location Tracker für spezifische Konfigurationen
     */
    fun getFusedLocationTracker(): FusedLocationTracker {
        return fusedLocationTracker
    }

    /**
     * Gibt Zugriff auf den Geofencing Tracker für spezifische Konfigurationen
     */
    fun getGeofencingTracker(): GeofencingTracker {
        return geofencingTracker
    }
}