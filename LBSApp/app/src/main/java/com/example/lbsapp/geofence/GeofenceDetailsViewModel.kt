package com.example.lbsapp.geofence

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.example.lbsapp.geofence.GeofenceRepository

class GeofenceDetailsViewModel(application: Application, private val geofenceId: Long) : AndroidViewModel(application) {

    private val repository = GeofenceRepository(application)

    val geofence = liveData {
        emit(repository.geofenceDao.getGeofenceById(geofenceId))
    }

    val visits = repository.getVisitsForGeofence(geofenceId)
}

class GeofenceDetailsViewModelFactory(
    private val application: Application,
    private val geofenceId: Long
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GeofenceDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GeofenceDetailsViewModel(application, geofenceId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}