package com.karte.lbsapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lbsapp.R
import com.example.lbsapp.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

@Suppress("DEPRECATION")
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var locationUpdateState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialisiere den FusedLocationProvider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Erstelle die LocationRequest-Einstellungen
        createLocationRequest()

        // Erstelle den LocationCallback für Updates
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    // Aktualisiere die Karte mit dem neuen Standort
                    val currentPosition = LatLng(location.latitude, location.longitude)
                    mMap.clear() // Entferne alte Marker
                    mMap.addMarker(MarkerOptions().position(currentPosition).title("Mein Standort"))
                    // Nur beim ersten Update zoomen
                    if (!locationUpdateState) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15f))
                        locationUpdateState = true
                    }
                }
            }
        }

        // Finde den Zurück-Button und setze den Click-Listener
        binding.backButton.setOnClickListener {
            finish() // Schließt die aktuelle Activity und kehrt zur vorherigen zurück
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Performance-Optimierung: Die Karte wird asynchron geladen
        Toast.makeText(this, "Karte wird geladen...", Toast.LENGTH_SHORT).show()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 Sekunden zwischen Updates
            fastestInterval = 5000 // Schnellste Update-Rate
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Prüfe und fordere Berechtigungen an, falls nötig
        if (checkLocationPermission()) {
            enableMyLocation()
            startLocationUpdates()
        } else {
            requestLocationPermission()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
                startLocationUpdates()
            } else {
                // Berechtigungen wurden verweigert, zeige einen Default-Standort
                val defaultLocation = LatLng(0.0, 0.0)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 2f))
                Toast.makeText(this, "Standortberechtigungen verweigert", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enableMyLocation() {
        try {
            // Aktiviere den "Mein Standort"-Button auf der Karte
            mMap.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            // Berechtigungen wurden nicht erteilt
            Toast.makeText(this, "Standortberechtigung erforderlich", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startLocationUpdates() {
        if (checkLocationPermission()) {
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper() // Verwende den Hauptthread-Looper für Callbacks
                )
            } catch (e: SecurityException) {
                Toast.makeText(this, "Standortberechtigung nicht erteilt", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Stoppe Updates, wenn die Activity nicht im Vordergrund ist
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        // Starte Updates, wenn die Activity wieder im Vordergrund ist
        if (checkLocationPermission() && ::mMap.isInitialized) {
            startLocationUpdates()
        }
    }
}