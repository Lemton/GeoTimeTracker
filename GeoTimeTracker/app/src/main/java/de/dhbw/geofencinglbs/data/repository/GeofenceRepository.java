package de.dhbw.geofencinglbs.data.repository;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.dhbw.geofencinglbs.data.local.GeofenceDao;
import de.dhbw.geofencinglbs.data.local.GeofenceEventDao;
import de.dhbw.geofencinglbs.data.local.AppDatabase;
import de.dhbw.geofencinglbs.model.GeofenceEvent;
import de.dhbw.geofencinglbs.model.GeofenceModel;

/**
 * Repository-Klasse als Single Source of Truth für Geofence-Daten.
 * Abstrahiert den Datenzugriff und stellt eine saubere API für den Zugriff auf Daten aus verschiedenen Quellen bereit.
 */
public class GeofenceRepository {
    private final GeofenceDao geofenceDao;
    private final GeofenceEventDao eventDao;
    private final ExecutorService executorService;

    /**
     * Konstruktor für das Repository.
     */
    public GeofenceRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        geofenceDao = database.geofenceDao();
        eventDao = database.geofenceEventDao();
        executorService = Executors.newFixedThreadPool(4);
    }

    /**
     * Gibt alle Geofences zurück.
     */
    public LiveData<List<GeofenceModel>> getAllGeofences() {
        return geofenceDao.getAllGeofences();
    }

    /**
     * Gibt nur aktive Geofences zurück (für die Registrierung beim Geofencing-Service).
     */
    public void getActiveGeofences(GeofenceDataCallback callback) {
        executorService.execute(() -> {
            List<GeofenceModel> activeGeofences = geofenceDao.getActiveGeofencesSync();
            callback.onGeofencesLoaded(activeGeofences);
        });
    }

    /**
     * Gibt die letzten N Ereignisse zurück.
     */
    public LiveData<List<GeofenceEvent>> getRecentEvents(int limit) {
        return eventDao.getRecentEvents(limit);
    }

    /**
     * Fügt einen neuen Geofence hinzu.
     */
    public void insert(GeofenceModel geofence) {
        executorService.execute(() -> {
            geofenceDao.insert(geofence);
        });
    }

    /**
     * Aktualisiert einen vorhandenen Geofence.
     */
    public void update(GeofenceModel geofence) {
        executorService.execute(() -> {
            geofenceDao.update(geofence);
        });
    }

    /**
     * Löscht einen Geofence.
     */
    public void delete(GeofenceModel geofence) {
        executorService.execute(() -> {
            geofenceDao.delete(geofence);
        });
    }

    /**
     * Fügt ein neues Ereignis hinzu.
     */
    public void insertEvent(GeofenceEvent event) {
        executorService.execute(() -> {
            eventDao.insert(event);
        });
    }

    /**
     * Aktualisiert die Eintrittszeit eines Geofences.
     */
    public void updateGeofenceEntryTime(long geofenceId, long entryTime) {
        executorService.execute(() -> {
            GeofenceModel geofence = geofenceDao.getGeofenceByIdSync(geofenceId);
            if (geofence != null) {
                geofence.setLastEntryTime(entryTime);
                geofenceDao.update(geofence);
            }
        });
    }

    /**
     * Aktualisiert die Austrittszeit eines Geofences.
     */
    public void updateGeofenceExitTime(long geofenceId, long exitTime) {
        executorService.execute(() -> {
            GeofenceModel geofence = geofenceDao.getGeofenceByIdSync(geofenceId);
            if (geofence != null) {
                geofence.setLastExitTime(exitTime);
                geofenceDao.update(geofence);
            }
        });
    }

    /**
     * Aktualisiert explizit die Daten aus der Datenbank
     */
    public void refreshGeofences() {
        // Diese Methode ist Teil der LiveData-Implementierung und funktioniert ohne explizite Aktualisierung
        // Wir können hier jedoch zusätzliche Logik für zukünftige Erweiterungen hinzufügen
    }

    /**
     * Aktualisiert explizit die Ereignisdaten aus der Datenbank
     */
    public void refreshEvents() {
        // Diese Methode ist Teil der LiveData-Implementierung und funktioniert ohne explizite Aktualisierung
        // Wir können hier jedoch zusätzliche Logik für zukünftige Erweiterungen hinzufügen
    }

    /**
     * Callback-Interface für die asynchrone Datenabfrage.
     */
    public interface GeofenceDataCallback {
        void onGeofencesLoaded(List<GeofenceModel> geofences);
    }
}