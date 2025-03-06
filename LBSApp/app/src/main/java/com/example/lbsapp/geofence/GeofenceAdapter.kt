package com.example.lbsapp.geofence

import com.example.lbsapp.database.GeofenceEntity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lbsapp.R


class GeofenceAdapter(
    private val onDeleteClick: (GeofenceEntity) -> Unit,
    private val onDetailsClick: (GeofenceEntity) -> Unit
) : ListAdapter<GeofenceEntity, GeofenceAdapter.GeofenceViewHolder>(GeofenceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeofenceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_geofence, parent, false)
        return GeofenceViewHolder(view)
    }

    override fun onBindViewHolder(holder: GeofenceViewHolder, position: Int) {
        val geofence = getItem(position)
        holder.bind(geofence)
    }

    inner class GeofenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.geofenceNameTextView)
        private val radiusTextView: TextView = itemView.findViewById(R.id.geofenceRadiusTextView)
        private val totalTimeTextView: TextView = itemView.findViewById(R.id.totalTimeTextView)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        private val viewDetailsButton: Button = itemView.findViewById(R.id.viewDetailsButton)

        fun bind(geofence: GeofenceEntity) {
            nameTextView.text = geofence.name
            radiusTextView.text = "Radius: ${geofence.radius}m"
            totalTimeTextView.text = "Koordinaten: ${geofence.latitude}, ${geofence.longitude}"

            deleteButton.setOnClickListener {
                onDeleteClick(geofence)
            }

            viewDetailsButton.setOnClickListener {
                onDetailsClick(geofence)
            }
        }
    }

    class GeofenceDiffCallback : DiffUtil.ItemCallback<GeofenceEntity>() {
        override fun areItemsTheSame(oldItem: GeofenceEntity, newItem: GeofenceEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GeofenceEntity, newItem: GeofenceEntity): Boolean {
            return oldItem == newItem
        }
    }
}