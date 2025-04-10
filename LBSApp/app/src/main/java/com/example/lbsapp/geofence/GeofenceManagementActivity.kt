package com.example.lbsapp.geofence

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lbsapp.R
import com.example.lbsapp.database.GeofenceEntity
import com.example.lbsapp.databinding.ActivityGeofenceManagementBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.karte.lbsapp.MapsActivity

class GeofenceManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGeofenceManagementBinding
    private lateinit var viewModel: GeofenceViewModel
    private lateinit var adapter: GeofenceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGeofenceManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(GeofenceViewModel::class.java)

        // Setup RecyclerView
        adapter = GeofenceAdapter(
            onDeleteClick = { geofence -> viewModel.deleteGeofence(geofence) },
            onDetailsClick = { geofence -> showGeofenceDetails(geofence) }
        )

        binding.geofenceRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.geofenceRecyclerView.adapter = adapter

        // Observe geofences
        viewModel.allGeofences.observe(this) { geofences ->
            adapter.submitList(geofences)

            // Zeige Empty State oder RecyclerView je nach Daten
            if (geofences.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.geofenceRecyclerView.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.geofenceRecyclerView.visibility = View.VISIBLE
            }
        }

        // Zurück-Button
        binding.backButton.setOnClickListener {
            finish()
        }

        // Karte öffnen Button
        binding.openMapButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showGeofenceDetails(geofence: GeofenceEntity) {
        val intent = Intent(this, GeofenceDetailsActivity::class.java)
        intent.putExtra(GeofenceDetailsActivity.EXTRA_GEOFENCE_ID, geofence.id)
        startActivity(intent)
    }
}