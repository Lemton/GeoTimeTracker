package de.dhbw.geofencinglbs.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import de.dhbw.geofencinglbs.model.GeofenceEvent;

/**
 * Data Access Object für die Geofence-Ereignis-Tabelle.
 */
@Dao
public interface GeofenceEventDao {

    @Insert
    long insert(GeofenceEvent event);

    @Query("SELECT * FROM geofence_events WHERE geofenceId = :geofenceId ORDER BY timestamp DESC")
    LiveData<List<GeofenceEvent>> getEventsForGeofence(long geofenceId);

    @Query("SELECT * FROM geofence_events ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<GeofenceEvent>> getRecentEvents(int limit);

    @Query("SELECT * FROM geofence_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    List<GeofenceEvent> getEventsBetweenTimestamps(long startTime, long endTime);

    @Query("SELECT * FROM geofence_events WHERE geofenceId = :geofenceId AND eventType = :eventType ORDER BY timestamp DESC LIMIT 1")
    GeofenceEvent getLatestEventByType(long geofenceId, int eventType);

    /**
     * Berechnet die Gesamtaufenthaltszeit für einen bestimmten Geofence
     * Dies wird durch die Paarung von Eintritts- und Austrittsereignissen erreicht
     */
    @Query("SELECT SUM(exit.timestamp - enter.timestamp) " +
            "FROM geofence_events enter JOIN geofence_events exit " +
            "ON enter.geofenceId = exit.geofenceId " +
            "WHERE enter.geofenceId = :geofenceId " +
            "AND enter.eventType = 1 AND exit.eventType = 2 " +
            "AND enter.timestamp < exit.timestamp " +
            "AND enter.timestamp BETWEEN :startTime AND :endTime " +
            "AND exit.timestamp BETWEEN :startTime AND :endTime")
    long calculateTotalDwellTime(long geofenceId, long startTime, long endTime);
}