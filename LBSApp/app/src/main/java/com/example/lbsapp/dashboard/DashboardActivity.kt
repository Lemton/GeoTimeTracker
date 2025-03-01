package com.example.lbsapp.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.lbsapp.R
import com.example.lbsapp.databinding.ActivityDashboardBinding
import com.example.lbsapp.tracking.models.TrackingMode
import com.karte.lbsapp.MapsActivity

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var viewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisiere ViewModel
        viewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

        // Setze das Layout mit Databinding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dashboard)
        binding.lifecycleOwner = this

        // Setze die Anfangswerte
        binding.isTracking = viewModel.isTracking.value
        binding.trackingMode = viewModel.selectedTrackingMode.value?.displayName

        // Beobachte ViewModel-Änderungen
        setupObservers()

        // Setze Listener für UI-Elemente
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.isTracking.observe(this) { isTracking ->
            binding.isTracking = isTracking
        }

        viewModel.selectedTrackingMode.observe(this) { mode ->
            binding.trackingMode = mode.displayName

            // Setze die entsprechende RadioButton-Auswahl
            when (mode) {
                TrackingMode.GPS -> binding.gpsRadioButton.isChecked = true
                TrackingMode.FUSED_LOCATION -> binding.fusedLocationRadioButton.isChecked = true
                TrackingMode.GEOFENCING -> binding.geofencingRadioButton.isChecked = true
            }
        }
    }

    private fun setupListeners() {
        // Start/Stop Button
        binding.startStopButton.setOnClickListener {
            if (viewModel.isTracking.value == true) {
                viewModel.stopTracking()
            } else {
                viewModel.startTracking()
            }
        }

        // Tracking Switch
        binding.trackingSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != viewModel.isTracking.value) {
                if (isChecked) {
                    viewModel.startTracking()
                } else {
                    viewModel.stopTracking()
                }
            }
        }

        // RadioGroup für Tracking-Modi
        binding.trackingModeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.gpsRadioButton -> viewModel.selectTrackingMode(TrackingMode.GPS)
                R.id.fusedLocationRadioButton -> viewModel.selectTrackingMode(TrackingMode.FUSED_LOCATION)
                R.id.geofencingRadioButton -> viewModel.selectTrackingMode(TrackingMode.GEOFENCING)
            }
        }

        // Karten-Button
        binding.openMapButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
    }
}