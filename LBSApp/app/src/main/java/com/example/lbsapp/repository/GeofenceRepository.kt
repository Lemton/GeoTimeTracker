package com.example.lbsapp.repository

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.lbsapp.database.AppDatabase
import com.example.lbsapp.database.GeofenceEntity
import com.example.lbsapp.database.VisitEntity

class GeofenceRepository(application: Application) {
    private val TAG = "GeofenceRepository"
    private val database = AppDatabase.getDatabase(application)
    private val geofenceDao = database.geofenceDao()
    private val visitDao = database.visitDao()

    val allGeofences = geofenceDao.getAllGeofences()

    suspend fun addGeofence(name: String, latitude: Double, longitude: Double, radius: Float): Long {
        Log.d(TAG, "Füge Geofence hinzu: $name an Position $latitude, $longitude mit Radius $radius")
        val geofence = GeofenceEntity(name = name, latitude = latitude, longitude = longitude, radius = radius)
        val id = geofenceDao.insert(geofence)
        Log.d(TAG, "Geofence erfolgreich hinzugefügt mit ID: $id")
        return id
    }

    suspend fun deleteGeofence(geofence: GeofenceEntity) {
        Log.d(TAG, "Lösche Geofence mit ID: ${geofence.id} und Name: ${geofence.name}")
        geofenceDao.delete(geofence)
        Log.d(TAG, "Geofence erfolgreich gelöscht")
    }

    suspend fun recordEntry(geofenceId: Long): Long {
        Log.d(TAG, "Zeichne Eintritt für Geofence $geofenceId auf")
        val visit = VisitEntity(geofenceId = geofenceId, enterTime = System.currentTimeMillis())
        val id = visitDao.insert(visit)
        Log.d(TAG, "Eintritt erfolgreich aufgezeichnet mit Besuchs-ID: $id")
        return id
    }

    suspend fun recordExit(geofenceId: Long): Boolean {
        Log.d(TAG, "Zeichne Austritt für Geofence $geofenceId auf")
        val activeVisits = visitDao.getActiveVisitsForGeofence(geofenceId)
        Log.d(TAG, "Aktive Besuche gefunden: ${activeVisits.size}")

        if (activeVisits.isEmpty()) {
            Log.d(TAG, "Kein aktiver Besuch gefunden für Geofence $geofenceId")
            return false
        }

        // Nimm den neuesten aktiven Besuch
        val currentVisit = activeVisits[0]
        Log.d(TAG, "Aktualisiere Besuch mit ID: ${currentVisit.id}")

        val exitTime = System.currentTimeMillis()
        val duration = exitTime - currentVisit.enterTime

        val updatedVisit = currentVisit.copy(
            exitTime = exitTime,
            totalDuration = duration
        )

        visitDao.update(updatedVisit)
        Log.d(TAG, "Austritt erfolgreich aufgezeichnet. Dauer: ${formatDuration(duration)}")
        return true
    }

    fun getVisitsForGeofence(geofenceId: Long): LiveData<List<VisitEntity>> {
        Log.d(TAG, "Rufe Besuche für Geofence $geofenceId ab")
        return visitDao.getVisitsForGeofence(geofenceId)
    }

    suspend fun getTotalTimeInGeofence(geofenceId: Long): Long {
        Log.d(TAG, "Berechne Gesamtzeit für Geofence $geofenceId")
        val totalTime = visitDao.getTotalDurationForGeofence(geofenceId) ?: 0
        Log.d(TAG, "Gesamtzeit für Geofence $geofenceId: ${formatDuration(totalTime)}")
        return totalTime
    }

    suspend fun getActiveVisits(geofenceId: Long): List<VisitEntity> {
        Log.d(TAG, "Rufe aktive Besuche für Geofence $geofenceId ab")
        val visits = visitDao.getActiveVisitsForGeofence(geofenceId)
        Log.d(TAG, "Aktive Besuche für Geofence $geofenceId: ${visits.size}")
        return visits
    }

    suspend fun getGeofenceName(geofenceId: Long): String? {
        Log.d(TAG, "Rufe Namen für Geofence $geofenceId ab")
        val geofence = geofenceDao.getGeofenceById(geofenceId)
        Log.d(TAG, "Geofence $geofenceId Name: ${geofence?.name}")
        return geofence?.name
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> "$hours h ${minutes % 60} min"
            minutes > 0 -> "$minutes min ${seconds % 60} s"
            else -> "$seconds s"
        }
    }
}