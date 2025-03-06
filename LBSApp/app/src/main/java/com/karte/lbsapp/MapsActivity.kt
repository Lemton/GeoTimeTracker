package com.karte.lbsapp

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.lbsapp.R
import com.example.lbsapp.databinding.ActivityMapsBinding
import com.example.lbsapp.repository.GeofenceRepository
import com.example.lbsapp.tracking.TrackingManager
import com.example.lbsapp.tracking.receivers.GeofenceBroadcastReceiver
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var trackingManager: TrackingManager
    private var selectedLocation: LatLng? = null
    private var tempMarker: Marker? = null
    private var tempCircle: Circle? = null
    private var currentRadius: Float = 100f // Standardradius
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val TAG = "MapsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialisiere den FusedLocationProvider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialisiere den GeofencingClient
        geofencingClient = LocationServices.getGeofencingClient(this)

        // Initialisiere den TrackingManager
        trackingManager = TrackingManager.getInstance(this)

        // Zurück-Button
        binding.backButton.setOnClickListener {
            finish()
        }

        // Geofence-Erstellungs-Button (zunächst unsichtbar)
        binding.createGeofenceButton.visibility = View.GONE
        binding.createGeofenceButton.setOnClickListener {
            showGeofenceDialog()
        }

        // SeekBar für den Radius
        binding.radiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Skalieren auf sinnvollen Bereich (20m bis 500m)
                currentRadius = 20f + progress * 4.8f
                binding.radiusTextView.text = "${currentRadius.toInt()}m"

                // Aktualisiere den temporären Kreis
                updateTempCircle()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Radius-Controls zunächst verstecken
        binding.radiusControls.visibility = View.GONE

        // Initialisiere die Karte
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Karten-Einstellungen
        if (checkLocationPermission()) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true

            // Zeige aktuelle Position
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentPosition = LatLng(it.latitude, it.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15f))
                }
            }
        } else {
            requestLocationPermission()
        }

        // Zeige bestehende Geofences an
        showExistingGeofences()

        // Klick-Listener für neue Geofences
        mMap.setOnMapClickListener { latLng ->
            selectedLocation = latLng

            // Entferne alten temporären Marker und Kreis
            tempMarker?.remove()
            tempCircle?.remove()

            // Füge neuen temporären Marker hinzu
            tempMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Neuer Geofence")
                    .draggable(true)
            )

            // Zeige den Radius-Kreis
            tempCircle = mMap.addCircle(
                CircleOptions()
                    .center(latLng)
                    .radius(currentRadius.toDouble())
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(70, 255, 0, 0))
            )

            // Zeige Radius-Controls und Erstellen-Button
            binding.radiusControls.visibility = View.VISIBLE
            binding.createGeofenceButton.visibility = View.VISIBLE
        }

        // Drag-Listener für Marker
        mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {}

            override fun onMarkerDrag(marker: Marker) {
                // Aktualisiere die Position des Kreises während des Ziehens
                selectedLocation = marker.position
                updateTempCircle()
            }

            override fun onMarkerDragEnd(marker: Marker) {
                selectedLocation = marker.position
                updateTempCircle()
            }
        })
    }

    private fun updateTempCircle() {
        selectedLocation?.let { location ->
            tempCircle?.remove()
            tempCircle = mMap.addCircle(
                CircleOptions()
                    .center(location)
                    .radius(currentRadius.toDouble())
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(70, 255, 0, 0))
            )
        }
    }

    private fun showExistingGeofences() {
        val geofenceRepository = GeofenceRepository(application)
        geofenceRepository.allGeofences.observe(this) { geofences ->
            // Karte leeren (nur Geofences, nicht den temporären Marker)
            mMap.clear()
            tempMarker = null
            tempCircle = null

            // Bestehende Geofences anzeigen
            for (geofence in geofences) {
                val position = LatLng(geofence.latitude, geofence.longitude)

                // Marker für den Geofence
                mMap.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(geofence.name)
                )

                // Kreis für den Radius
                mMap.addCircle(
                    CircleOptions()
                        .center(position)
                        .radius(geofence.radius.toDouble())
                        .strokeColor(Color.BLUE)
                        .fillColor(Color.argb(70, 0, 0, 255))
                )
            }

            // Temporären Marker und Kreis neu hinzufügen, falls vorhanden
            selectedLocation?.let { location ->
                tempMarker = mMap.addMarker(
                    MarkerOptions()
                        .position(location)
                        .title("Neuer Geofence")
                        .draggable(true)
                )

                tempCircle = mMap.addCircle(
                    CircleOptions()
                        .center(location)
                        .radius(currentRadius.toDouble())
                        .strokeColor(Color.RED)
                        .fillColor(Color.argb(70, 255, 0, 0))
                )
            }
        }
    }

    private fun showGeofenceDialog() {
        selectedLocation?.let { location ->
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_name_geofence, null)
            val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)

            AlertDialog.Builder(this)
                .setTitle("Geofence benennen")
                .setView(dialogView)
                .setPositiveButton("Erstellen") { _, _ ->
                    val name = nameEditText.text.toString()

                    if (name.isNotBlank()) {
                        // Geofence erstellen
                        val geofenceRepository = GeofenceRepository(application)
                        lifecycleScope.launch {
                            val geofenceId = geofenceRepository.addGeofence(
                                name,
                                location.latitude,
                                location.longitude,
                                currentRadius
                            )

                            // Geofence beim GeofencingClient registrieren
                            addGeofenceToMonitoring(
                                geofenceId,
                                location.latitude,
                                location.longitude,
                                currentRadius
                            )

                            // UI zurücksetzen
                            runOnUiThread {
                                selectedLocation = null
                                tempMarker = null
                                tempCircle = null
                                binding.radiusControls.visibility = View.GONE
                                binding.createGeofenceButton.visibility = View.GONE

                                // Aktualisiere die Anzeige
                                showExistingGeofences()

                                Toast.makeText(this@MapsActivity, "Geofence '$name' erstellt", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Bitte gib einen Namen ein", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Abbrechen") { _, _ ->
                    // Abbrechen - nichts tun
                }
                .show()
        }
    }

    private fun addGeofenceToMonitoring(
        geofenceId: Long,
        latitude: Double,
        longitude: Double,
        radius: Float
    ) {
        Log.d(TAG, "Füge Geofence $geofenceId zum Monitoring hinzu (lat: $latitude, lng: $longitude, radius: $radius)")

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Keine Berechtigung für Geofencing")
            return
        }

        try {
            // Alle vorhandenen Geofences entfernen
            geofencingClient.removeGeofences(getPendingIntent())
                .addOnSuccessListener {
                    Log.d(TAG, "Alte Geofences entfernt")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Fehler beim Entfernen alter Geofences: ${e.message}")
                }

            // Erstelle Geofence mit eindeutiger ID
            val geofence = Geofence.Builder()
                .setRequestId(geofenceId.toString())
                .setCircularRegion(latitude, longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(30000) // Optional: 30 Sekunden für DWELL-Ereignisse
                .build()

            // Erstelle GeofencingRequest
            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            // Erstelle eindeutigen PendingIntent für dieses Geofence
            val pendingIntent = getPendingIntent()

            // Füge Geofence hinzu
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence $geofenceId erfolgreich zum Monitoring hinzugefügt")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Fehler beim Hinzufügen des Geofence $geofenceId: ${e.message}")
                    Log.e(TAG, "Fehlerdetails: ${e.stackTraceToString()}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Unerwarteter Fehler im Geofencing: ${e.message}")
            Log.e(TAG, "Fehlerdetails: ${e.stackTraceToString()}")
        }
    }

    private fun getPendingIntent(): PendingIntent {
        Log.d(TAG, "Erstelle PendingIntent für Geofences")
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = "com.example.lbsapp.ACTION_GEOFENCE_EVENT"

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getBroadcast(this, 0, intent, flags)
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
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
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
                try {
                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true

                    // Zeige aktuelle Position
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val currentPosition = LatLng(it.latitude, it.longitude)
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15f))
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Berechtigung fehlt trotz Überprüfung: ${e.message}")
                }
            } else {
                Log.d(TAG, "Standortberechtigungen verweigert")
                Toast.makeText(this, "Standortberechtigungen verweigert", Toast.LENGTH_SHORT).show()
            }
        }
    }
}