package com.example.lbsapp.tracking.models

enum class TrackingMode(val displayName: String) {
    GPS("GPS Tracking"),
    FUSED_LOCATION("Fused Location Provider"),
    GEOFENCING("Geofencing")
}