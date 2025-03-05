package com.karte.lbsapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lbsapp.R
import com.example.lbsapp.databinding.ActivityMapsBinding
import com.example.lbsapp.tracking.TrackingManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var trackingManager: TrackingManager
    private lateinit var trackingModeInfoTextView: TextView

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val locationList = mutableListOf<LatLng>()
    private var mapReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityMapsBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Logging für Debugging
            logMessage("MapsActivity onCreate gestartet")

            // Initialisiere den TrackingManager
            trackingManager = TrackingManager.getInstance(applicationContext)

            // Finde das TextView für Tracking-Modus-Info
            trackingModeInfoTextView = binding.trackingModeInfo

            // Aktualisiere die Tracking-Info
            updateTrackingInfo()

            // Beobachte Änderungen am Tracking-Status
            setupObservers()

            // Zurück-Button
            binding.backButton.setOnClickListener {
                logMessage("Zurück-Button geklickt")
                finish()
            }

            // Lade die Karte
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as? SupportMapFragment

            if (mapFragment != null) {
                logMessage("MapFragment gefunden, rufe getMapAsync auf")
                mapFragment.getMapAsync(this)
                Toast.makeText(this, "Karte wird geladen...", Toast.LENGTH_SHORT).show()
            } else {
                logMessage("FEHLER: MapFragment ist null")
                Toast.makeText(this, "Fehler beim Laden der Karte", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            logMessage("FEHLER in onCreate: ${e.message}")
            Toast.makeText(this, "Fehler beim Initialisieren: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupObservers() {
        try {
            // Beobachte Standortänderungen
            trackingManager.locationData.observe(this) { location ->
                if (mapReady && trackingManager.isTracking.value == true) {
                    logMessage("Neuer Standort erhalten: ${location.latitude}, ${location.longitude}")
                    updateMap(location.latitude, location.longitude)
                }
            }

            // Beobachte Tracking-Status
            trackingManager.isTracking.observe(this) { isTracking ->
                updateTrackingInfo()
            }

            // Beobachte Tracking-Modus
            trackingManager.currentMode.observe(this) { mode ->
                updateTrackingInfo()
            }

            // Beobachte Fehler
            trackingManager.error.observe(this) { error ->
                logMessage("Tracking Fehler: $error")
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            logMessage("FEHLER in setupObservers: ${e.message}")
        }
    }

    private fun updateTrackingInfo() {
        try {
            val isTracking = trackingManager.isTracking.value ?: false
            val mode = trackingManager.currentMode.value?.displayName ?: "Unbekannt"

            val trackingStatus = if (isTracking) "aktiv" else "inaktiv"
            trackingModeInfoTextView.text = "$mode: $trackingStatus"

            logMessage("Tracking-Info aktualisiert: $mode ist $trackingStatus")
        } catch (e: Exception) {
            logMessage("FEHLER in updateTrackingInfo: ${e.message}")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        try {
            logMessage("onMapReady aufgerufen")
            mMap = googleMap
            mapReady = true

            // Standardansicht auf Deutschland setzen
            val germany = LatLng(51.1657, 10.4515)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(germany, 5.5f))

            // Aktiviere My Location Button nur, wenn wir Berechtigungen haben
            if (checkLocationPermission()) {
                enableMyLocation()

                // Wenn Tracking aktiv ist, zeige aktuellen Standort
                if (trackingManager.isTracking.value == true) {
                    trackingManager.getLastLocation()?.let { location ->
                        logMessage("Letzter bekannter Standort: ${location.latitude}, ${location.longitude}")
                        updateMap(location.latitude, location.longitude)
                    }
                }
            } else {
                logMessage("Keine Standortberechtigungen vorhanden")
                requestLocationPermission()
            }
        } catch (e: Exception) {
            logMessage("FEHLER in onMapReady: ${e.message}")
            Toast.makeText(this, "Fehler beim Initialisieren der Karte: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateMap(latitude: Double, longitude: Double) {
        if (!mapReady) return

        try {
            val currentPosition = LatLng(latitude, longitude)

            // Füge Punkt zur Liste hinzu
            locationList.add(currentPosition)

            // Zeichne den Track, falls mehr als ein Punkt vorhanden ist
            if (locationList.size > 1) {
                mMap.addPolyline(
                    PolylineOptions()
                        .add(locationList[locationList.size - 2], locationList[locationList.size - 1])
                        .width(5f)
                        .color(Color.BLUE)
                )
            }

            mMap.clear()
            mMap.addMarker(MarkerOptions().position(currentPosition).title("AlgoLocation"))

            // Nur beim ersten Punkt die Kamera bewegen (oder nach Bedarf)
            if (locationList.size == 1) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15f))
            }

            // Zeichne die gesamte Route neu, da wir die Karte gecleared haben
            if (locationList.size > 1) {
                val polylineOptions = PolylineOptions()
                    .width(5f)
                    .color(Color.BLUE)

                for (point in locationList) {
                    polylineOptions.add(point)
                }

                mMap.addPolyline(polylineOptions)
            }
        } catch (e: Exception) {
            logMessage("FEHLER in updateMap: ${e.message}")
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun enableMyLocation() {
        try {
            // Aktiviere den "Mein Standort"-Button auf der Karte
            mMap.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            logMessage("FEHLER in enableMyLocation: ${e.message}")
            Toast.makeText(this, "Standortberechtigung erforderlich", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                logMessage("Standortberechtigungen erteilt")
                enableMyLocation()
            } else {
                logMessage("Standortberechtigungen verweigert")
                Toast.makeText(this, "Ohne Standortberechtigungen kann der aktuelle Standort nicht angezeigt werden", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun logMessage(message: String) {
        // In echter App durch echtes Logging ersetzen
        println("LBSApp - MapsActivity: $message")
    }

    override fun onResume() {
        super.onResume()
        updateTrackingInfo()
    }
}