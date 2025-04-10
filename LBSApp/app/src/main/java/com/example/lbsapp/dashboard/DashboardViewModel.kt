package com.example.lbsapp.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.lbsapp.tracking.models.TrackingMode

class DashboardViewModel : ViewModel() {

    // Tracking Status
    private val _isTracking = MutableLiveData<Boolean>(false)
    val isTracking: LiveData<Boolean> = _isTracking

    // Ausgewählter Tracking-Modus
    private val _selectedTrackingMode = MutableLiveData<TrackingMode>(TrackingMode.GPS)
    val selectedTrackingMode: LiveData<TrackingMode> = _selectedTrackingMode

    // Tracking starten
    fun startTracking() {
        // Hier sollte der jeweilige Tracker initialisiert und gestartet werden
        // basierend auf dem ausgewählten Modus
        _isTracking.value = true
    }

    // Tracking stoppen
    fun stopTracking() {
        // Hier sollte der aktive Tracker gestoppt werden
        _isTracking.value = false
    }

    // Tracking-Modus wechseln
    fun selectTrackingMode(mode: TrackingMode) {
        _selectedTrackingMode.value = mode

        // Falls Tracking aktiv ist, neu starten mit dem neuen Modus
        if (_isTracking.value == true) {
            stopTracking()
            startTracking()
        }
    }
}