package com.example.lbsapp.tracking.modes

import com.example.lbsapp.database.GeofenceEntity
import android.Manifest
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.lbsapp.tracking.base.BaseTracker
import com.example.lbsapp.tracking.receivers.GeofenceBroadcastReceiver
import com.google.android.gms.location.*
import java.util.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.lbsapp.geofence.GeofenceRepository

/**
 * Implementierung des Trackings mit Geofencing
 * Erlaubt das Überwachen von bestimmten geografischen Bereichen
 */
class GeofencingTracker(context: Context) : BaseTracker(context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private var lastLocation: Location? = null
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // Standardmäßig ein Geofence um den Benutzerstandort mit 100m Radius
    private var geofenceRadius = 100f
    private var geofenceList = mutableListOf<Geofence>()
    private lateinit var geofencePendingIntent: PendingIntent

    init {
        // PendingIntent initialisieren
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    override fun startTracking() {
        if (!hasPermissions()) {
            _error.postValue("Keine Standortberechtigungen")
            return
        }

        // Aktuellen Standort ermitteln und Geofence erstellen
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lastLocation = location
                    createGeofence(location.latitude, location.longitude)
                    addGeofences()
                } else {
                    _error.postValue("Konnte aktuellen Standort nicht ermitteln")
                }
            }
        } catch (e: SecurityException) {
            _error.postValue("Keine Berechtigung für Standortdaten: ${e.message}")
        } catch (e: Exception) {
            _error.postValue("Fehler beim Starten des Geofencing: ${e.message}")
        }
    }

    private fun createGeofence(lat: Double, lng: Double) {
        // Bestehende Geofences löschen
        geofenceList.clear()

        // Neues Geofence erstellen
        val geofence = Geofence.Builder()
            .setRequestId(UUID.randomUUID().toString()) // Eindeutige ID
            .setCircularRegion(lat, lng, geofenceRadius) // Kreisförmige Region
            .setExpirationDuration(Geofence.NEVER_EXPIRE) // Unbegrenzte Dauer
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT) // Eintreten und Verlassen überwachen
            .build()

        geofenceList.add(geofence)
    }

    private fun addGeofences() {
        if (!hasPermissions()) {
            _error.postValue("Keine Standortberechtigungen")
            return
        }

        try {
            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofenceList)
                .build()

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    _isTracking.postValue(true)
                }
                .addOnFailureListener { e ->
                    _error.postValue("Fehler beim Hinzufügen des Geofence: ${e.message}")
                }
        } catch (e: SecurityException) {
            _error.postValue("Keine Berechtigung für Standortdaten: ${e.message}")
        }
    }

    override fun stopTracking() {
        try {
            geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnSuccessListener {
                    _isTracking.postValue(false)
                }
                .addOnFailureListener { e ->
                    _error.postValue("Fehler beim Entfernen des Geofence: ${e.message}")
                }
        } catch (e: Exception) {
            _error.postValue("Fehler beim Stoppen des Geofencings: ${e.message}")
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
        // Für Geofencing wird zusätzlich BACKGROUND_LOCATION benötigt (ab Android 10)
        val fineLocationPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Vor Android 10 wird diese Berechtigung nicht benötigt
        }

        return fineLocationPermission && backgroundPermission
    }

    override fun getModeName(): String {
        return "Geofencing"
    }

    /**
     * Setzt den Radius für Geofences
     */
    fun setGeofenceRadius(radius: Float) {
        geofenceRadius = radius

        // Wenn wir bereits tracken, Geofence mit neuem Radius aktualisieren
        if (_isTracking.value == true && lastLocation != null) {
            stopTracking()
            startTracking()
        }
    }

    /**
     * Erstellt ein neues Geofence an der angegebenen Position
     */
    fun createGeofenceAtLocation(lat: Double, lng: Double) {
        createGeofence(lat, lng)

        if (_isTracking.value == true) {
            stopTracking()
            addGeofences()
        }
    }

    private val repository: GeofenceRepository by lazy{
        GeofenceRepository((context.applicationContext as Application))
            }
    suspend fun addCustomGeofence(name: String, latitude: Double, longitude: Double, radius: Float): Long {
        val geofenceId = repository.addGeofence(name, latitude, longitude, radius)

        // Erstelle das tatsächliche Geofence für die API
        val geofence = Geofence.Builder()
            .setRequestId(geofenceId.toString()) // Verwende die DB-ID als requestId
            .setCircularRegion(latitude, longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        geofenceList.add(geofence)

        // Wenn Tracking aktiv ist, aktualisiere die Geofences
        if (_isTracking.value == true) {
            removeAllGeofences()
            addGeofences()
        }

        return geofenceId
    }

    // Methode zum Entfernen eines benutzerdefinierten Geofences
    suspend fun removeCustomGeofence(geofence: GeofenceEntity) {
        repository.deleteGeofence(geofence)

        // Entferne aus der lokalen Liste
        geofenceList.removeAll { it.requestId == geofence.id.toString() }

        // Wenn Tracking aktiv ist, aktualisiere die Geofences
        if (_isTracking.value == true) {
            removeAllGeofences()
            addGeofences()
        }
    }

    private fun removeAllGeofences() {
        try {
            geofencingClient.removeGeofences(geofencePendingIntent)
        } catch (e: SecurityException) {
            _error.postValue("Keine Berechtigung für Standortdaten: ${e.message}")
        }
    }

    // Methode zum Laden aller gespeicherten Geofences beim Start
    suspend fun loadSavedGeofences() {
        geofenceList.clear()

        // Lade alle Geofences aus der Datenbank
        val geofences = repository.allGeofences.value ?: return

        for (geofence in geofences) {
            val gf = Geofence.Builder()
                .setRequestId(geofence.id.toString())
                .setCircularRegion(geofence.latitude, geofence.longitude, geofence.radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

            geofenceList.add(gf)
        }
    }

}