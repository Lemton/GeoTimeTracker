package com.example.lbsapp.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.lbsapp.R
import com.example.lbsapp.databinding.ActivityDashboardBinding
import com.example.lbsapp.tracking.TrackingManager
import com.example.lbsapp.tracking.models.TrackingMode
import com.karte.lbsapp.MapsActivity

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var trackingManager: TrackingManager

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisiere TrackingManager
        trackingManager = TrackingManager.getInstance(this)

        // Setze das Layout mit Databinding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dashboard)
        binding.lifecycleOwner = this

        // Setze die Anfangswerte
        binding.isTracking = trackingManager.isTracking.value
        binding.trackingMode = trackingManager.currentMode.value?.displayName

        // Beobachte TrackingManager-Änderungen
        setupObservers()

        // Setze Listener für UI-Elemente
        setupListeners()

        // Überprüfe Berechtigungen
        checkAndRequestPermissions()
    }

    private fun setupObservers() {
        trackingManager.isTracking.observe(this) { isTracking ->
            binding.isTracking = isTracking
        }

        trackingManager.currentMode.observe(this) { mode ->
            binding.trackingMode = mode.displayName

            // Setze die entsprechende RadioButton-Auswahl
            when (mode) {
                TrackingMode.GPS -> binding.gpsRadioButton.isChecked = true
                TrackingMode.FUSED_LOCATION -> binding.fusedLocationRadioButton.isChecked = true
                TrackingMode.GEOFENCING -> binding.geofencingRadioButton.isChecked = true
            }
        }

        trackingManager.error.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        // Start/Stop Button
        binding.startStopButton.setOnClickListener {
            if (trackingManager.isTracking.value == true) {
                trackingManager.stopTracking()
            } else {
                if (checkPermissionsForCurrentMode()) {
                    trackingManager.startTracking()
                } else {
                    requestPermissionsForCurrentMode()
                }
            }
        }

        // Tracking Switch
        binding.trackingSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != trackingManager.isTracking.value) {
                if (isChecked) {
                    if (checkPermissionsForCurrentMode()) {
                        trackingManager.startTracking()
                    } else {
                        requestPermissionsForCurrentMode()
                        binding.trackingSwitch.isChecked = false // Setze Switch zurück, da wir noch keine Berechtigungen haben
                    }
                } else {
                    trackingManager.stopTracking()
                }
            }
        }

        // RadioGroup für Tracking-Modi
        binding.trackingModeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.gpsRadioButton -> trackingManager.setTrackingMode(TrackingMode.GPS)
                R.id.fusedLocationRadioButton -> trackingManager.setTrackingMode(TrackingMode.FUSED_LOCATION)
                R.id.geofencingRadioButton -> trackingManager.setTrackingMode(TrackingMode.GEOFENCING)
            }

            // Wenn wir den Modus wechseln, überprüfen wir, ob wir neue Berechtigungen benötigen
            checkAndRequestPermissions()
        }

        // Karten-Button
        binding.openMapButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Überprüft und fordert bei Bedarf die nötigen Berechtigungen an
     */
    private fun checkAndRequestPermissions() {
        if (!checkPermissionsForCurrentMode()) {
            requestPermissionsForCurrentMode()
        }
    }

    /**
     * Prüft, ob die für den aktuellen Modus benötigten Berechtigungen vorhanden sind
     */
    private fun checkPermissionsForCurrentMode(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // Für Geofencing benötigen wir die Hintergrund-Standortberechtigung
        val backgroundPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            trackingManager.currentMode.value == TrackingMode.GEOFENCING) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Für andere Modi oder ältere Android-Versionen nicht erforderlich
        }

        return fineLocationPermission && backgroundPermission
    }

    /**
     * Fordert die für den aktuellen Modus benötigten Berechtigungen an
     */
    private fun requestPermissionsForCurrentMode() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Ab Android 10 muss für Hintergrund-Standort eine separate Anfrage gestellt werden
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            trackingManager.currentMode.value == TrackingMode.GEOFENCING) {

            // Zuerst die Vordergrund-Berechtigungen anfordern
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )

            // Die Hintergrund-Berechtigung wird in onRequestPermissionsResult angefordert,
            // nachdem die Vordergrund-Berechtigungen erteilt wurden
        } else {
            // Für andere Modi benötigen wir nur die normalen Standortberechtigungen
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    /**
     * Fordert die Hintergrund-Standortberechtigung an
     */
    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Vordergrund-Standortberechtigungen wurden erteilt

                    // Wenn wir im Geofencing-Modus sind und Android 10 oder höher,
                    // müssen wir auch die Hintergrund-Berechtigung anfordern
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        trackingManager.currentMode.value == TrackingMode.GEOFENCING &&
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED) {

                        requestBackgroundLocationPermission()
                    } else {
                        // Für andere Modi oder wenn bereits erteilt, können wir das Tracking starten
                        Toast.makeText(this, "Standortberechtigungen erteilt", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Standortberechtigungen werden für diese Funktion benötigt", Toast.LENGTH_LONG).show()
                }
            }

            BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Hintergrund-Standortberechtigung wurde erteilt
                    Toast.makeText(this, "Hintergrund-Standortberechtigung erteilt", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Hintergrund-Standortberechtigung wird für Geofencing benötigt", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Status aktualisieren
        binding.isTracking = trackingManager.isTracking.value
    }
}