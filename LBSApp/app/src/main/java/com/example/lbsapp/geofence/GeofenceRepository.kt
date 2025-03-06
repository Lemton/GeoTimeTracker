package com.example.lbsapp.geofence

import com.example.lbsapp.database.GeofenceEntity
import com.example.lbsapp.database.VisitEntity
import android.app.Application
import androidx.lifecycle.LiveData
import com.example.lbsapp.database.AppDatabase

class GeofenceRepository(private val application: Application) {
    private val database = AppDatabase.getDatabase(application)
    val geofenceDao = database.geofenceDao()
    private val visitDao = database.visitDao()

    val allGeofences = geofenceDao.getAllGeofences()

    suspend fun addGeofence(name: String, latitude: Double, longitude: Double, radius: Float): Long {
        val geofence = GeofenceEntity(name = name, latitude = latitude, longitude = longitude, radius = radius)
        return geofenceDao.insert(geofence)
    }

    suspend fun deleteGeofence(geofence: GeofenceEntity) {
        geofenceDao.delete(geofence)
    }

    suspend fun recordEntry(geofenceId: Long) {
        val visit = VisitEntity(geofenceId = geofenceId, enterTime = System.currentTimeMillis())
        visitDao.insert(visit)
    }

    suspend fun recordExit(geofenceId: Long) {
        val activeVisits = visitDao.getActiveVisits()
        val currentVisit = activeVisits.find { it.geofenceId == geofenceId }

        currentVisit?.let {
            val exitTime = System.currentTimeMillis()
            val duration = exitTime - it.enterTime

            val updatedVisit = it.copy(
                exitTime = exitTime,
                totalDuration = duration
            )

            visitDao.update(updatedVisit)
        }
    }

    fun getVisitsForGeofence(geofenceId: Long): LiveData<List<VisitEntity>> {
        return visitDao.getVisitsForGeofence(geofenceId)
    }

    suspend fun getTotalTimeInGeofence(geofenceId: Long): Long {
        return visitDao.getTotalDurationForGeofence(geofenceId) ?: 0
    }
}