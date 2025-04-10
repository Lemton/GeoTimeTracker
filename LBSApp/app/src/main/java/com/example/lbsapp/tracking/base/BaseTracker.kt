package com.example.lbsapp.tracking.base

import android.content.Context
import android.location.Location
import android.widget.MultiAutoCompleteTextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/*
Basis Interface für alle Tracking Implementierungen.
Definiert die grundlegenden Methoden, die jeder Tracker implementiert!
 */


abstract class BaseTracker(protected val context: Context) {
    //LiveData für StandortUpdates
    protected val _locationData = MutableLiveData<Location>()
    val locationData: LiveData<Location> = _locationData

    //LiveData für den Tracking Status
    protected val _isTracking = MutableLiveData<Boolean>(false)
    val isTracking: LiveData<Boolean> get() = _isTracking

    //LiveData für Fehler
    protected val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

abstract fun startTracking()

abstract fun stopTracking()

abstract fun getLastLocation(): Location?

abstract fun hasPermissions(): Boolean

abstract fun getModeName(): String
}