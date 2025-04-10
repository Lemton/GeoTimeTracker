package de.dhbw.geofencinglbs.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import de.dhbw.geofencinglbs.model.GeofenceModel;

/**
 * Data Access Object für die Geofence-Tabelle.
 * Definiert Methoden für CRUD-Operationen.
 */
@Dao
public interface GeofenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(GeofenceModel geofence);

    @Update
    void update(GeofenceModel geofence);

    @Delete
    void delete(GeofenceModel geofence);

    @Query("SELECT * FROM geofences ORDER BY name ASC")
    LiveData<List<GeofenceModel>> getAllGeofences();

    @Query("SELECT * FROM geofences WHERE id = :geofenceId")
    GeofenceModel getGeofenceById(long geofenceId);

    @Query("SELECT * FROM geofences WHERE id = :geofenceId")
    LiveData<GeofenceModel> getGeofenceByIdLive(long geofenceId);

    @Query("SELECT * FROM geofences WHERE isActive = 1")
    List<GeofenceModel> getActiveGeofences();
}