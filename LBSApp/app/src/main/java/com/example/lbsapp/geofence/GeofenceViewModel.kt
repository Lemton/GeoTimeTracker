package com.example.lbsapp.geofence

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.lbsapp.database.GeofenceEntity
import com.example.lbsapp.repository.GeofenceRepository
import kotlinx.coroutines.launch

class GeofenceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GeofenceRepository(application)

    val allGeofences: LiveData<List<GeofenceEntity>> = repository.allGeofences

    fun addGeofence(name: String, latitude: Double, longitude: Double, radius: Float) {
        viewModelScope.launch {
            repository.addGeofence(name, latitude, longitude, radius)
        }
    }

    fun deleteGeofence(geofence: GeofenceEntity) {
        viewModelScope.launch {
            repository.deleteGeofence(geofence)
        }
    }
}