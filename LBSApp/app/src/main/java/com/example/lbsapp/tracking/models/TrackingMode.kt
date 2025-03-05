package com.example.lbsapp.tracking.models

/**
 * Enumeration der verschiedenen Tracking-Modi
 */
enum class TrackingMode(val displayName: String) {
    GPS("GPS Tracking"),
    FUSED_LOCATION("Fused Location Provider"),
    GEOFENCING("Geofencing")
}
