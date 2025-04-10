package com.example.lbsapp.geofence

import com.example.lbsapp.database.VisitEntity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lbsapp.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VisitAdapter : ListAdapter<VisitEntity, VisitAdapter.VisitViewHolder>(VisitDiffCallback()) {

    private val dateFormatter = SimpleDateFormat("dd. MMMM yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_visit, parent, false)
        return VisitViewHolder(view)
    }

    override fun onBindViewHolder(holder: VisitViewHolder, position: Int) {
        val visit = getItem(position)
        holder.bind(visit)
    }

    inner class VisitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val timeRangeTextView: TextView = itemView.findViewById(R.id.timeRangeTextView)
        private val durationTextView: TextView = itemView.findViewById(R.id.durationTextView)

        fun bind(visit: VisitEntity) {
            val enterDate = Date(visit.enterTime)

            dateTextView.text = dateFormatter.format(enterDate)

            val timeRangeText = if (visit.exitTime != null) {
                val exitDate = Date(visit.exitTime)
                "${timeFormatter.format(enterDate)} - ${timeFormatter.format(exitDate)}"
            } else {
                "${timeFormatter.format(enterDate)} - noch anwesend"
            }
            timeRangeTextView.text = timeRangeText

            val durationText = if (visit.totalDuration != null) {
                "Dauer: ${formatDuration(visit.totalDuration)}"
            } else {
                "Dauer: fortlaufend"
            }
            durationTextView.text = durationText
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

    class VisitDiffCallback : DiffUtil.ItemCallback<VisitEntity>() {
        override fun areItemsTheSame(oldItem: VisitEntity, newItem: VisitEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: VisitEntity, newItem: VisitEntity): Boolean {
            return oldItem == newItem
        }
    }
}