package com.example.lbsapp.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GeofenceDao {
    @Insert
    suspend fun insert(geofence: GeofenceEntity): Long

    @Query("SELECT * FROM geofences")
    fun getAllGeofences(): LiveData<List<GeofenceEntity>>

    @Query("SELECT * FROM geofences WHERE id = :id")
    suspend fun getGeofenceById(id: Long): GeofenceEntity?

    @Delete
    suspend fun delete(geofence: GeofenceEntity)
}