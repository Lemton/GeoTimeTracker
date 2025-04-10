package com.example.lbsapp.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface VisitDao {
    @Insert
    suspend fun insert(visit: VisitEntity): Long

    @Update
    suspend fun update(visit: VisitEntity)

    @Query("SELECT * FROM visits WHERE geofenceId = :geofenceId ORDER BY enterTime DESC")
    fun getVisitsForGeofence(geofenceId: Long): LiveData<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE exitTime IS NULL")
    suspend fun getActiveVisits(): List<VisitEntity>

    @Query("SELECT * FROM visits WHERE geofenceId = :geofenceId AND exitTime IS NULL ORDER BY enterTime DESC")
    suspend fun getActiveVisitsForGeofence(geofenceId: Long): List<VisitEntity>

    @Query("SELECT SUM(totalDuration) FROM visits WHERE geofenceId = :geofenceId AND totalDuration IS NOT NULL")
    suspend fun getTotalDurationForGeofence(geofenceId: Long): Long?
}