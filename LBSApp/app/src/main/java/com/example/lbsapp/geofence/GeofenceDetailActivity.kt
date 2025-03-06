package com.example.lbsapp.geofence

import com.example.lbsapp.geofence.GeofenceDetailsViewModelFactory
import com.example.lbsapp.geofence.GeofenceDetailsViewModel
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lbsapp.R
import com.example.lbsapp.database.VisitEntity
import com.example.lbsapp.databinding.ActivityGeofenceDetailsBinding

class GeofenceDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GEOFENCE_ID = "extra_geofence_id"
    }

    private lateinit var binding: ActivityGeofenceDetailsBinding
    private lateinit var viewModel: GeofenceDetailsViewModel
    private lateinit var adapter: VisitAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGeofenceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val geofenceId = intent.getLongExtra(EXTRA_GEOFENCE_ID, -1)
        if (geofenceId == -1L) {
            finish()
            return
        }

        // Initialize ViewModel with the geofence ID
        val factory = GeofenceDetailsViewModelFactory(application, geofenceId)
        viewModel = ViewModelProvider(this, factory).get(GeofenceDetailsViewModel::class.java)

        // Setup UI
        viewModel.geofence.observe(this) { geofence ->
            binding.toolbarTitle.text = geofence?.name ?: "Geofence Details"
        }

        // Setup RecyclerView
        adapter = VisitAdapter()
        binding.visitsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.visitsRecyclerView.adapter = adapter

        // Observe visits
        viewModel.visits.observe(this) { visits ->
            adapter.submitList(visits)

            // Update summary
            updateSummary(visits)
        }

        // Back button
        binding.backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun updateSummary(visits: List<VisitEntity>) {
        var totalTime = 0L
        var visitsCount = 0

        for (visit in visits) {
            if (visit.totalDuration != null) {
                totalTime += visit.totalDuration
                visitsCount++
            }
        }

        val averageTime = if (visitsCount > 0) totalTime / visitsCount else 0

        binding.totalVisitsTextView.text = "Gesamtbesuche: $visitsCount"
        binding.totalTimeTextView.text = "Gesamtzeit: ${formatDuration(totalTime)}"
        binding.averageTimeTextView.text = "Durchschnittliche Zeit: ${formatDuration(averageTime)}"
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> "$hours h ${minutes % 60} min"
            minutes > 0 -> "$minutes min ${seconds % 60} s"
            else -> "$seconds s"
        }
    }
}