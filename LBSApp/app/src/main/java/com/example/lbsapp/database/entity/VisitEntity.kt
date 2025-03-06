package com.example.lbsapp.database

import androidx.room.*

@Entity(
    tableName = "visits",
    foreignKeys = [ForeignKey(
        entity = GeofenceEntity::class,
        parentColumns = ["id"],
        childColumns = ["geofenceId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("geofenceId")] // Füge einen Index für die Fremdschlüssel-Spalte hinzu (behebt die Warnung)
)
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val geofenceId: Long,
    val enterTime: Long,
    val exitTime: Long? = null, // Null bedeutet, dass der Besuch noch läuft
    val totalDuration: Long? = null // Wird beim Verlassen berechnet
)