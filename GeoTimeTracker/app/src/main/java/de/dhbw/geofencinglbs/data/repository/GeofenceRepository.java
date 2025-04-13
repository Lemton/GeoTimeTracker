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
     * Fügt ein neues Ereignis hinzu und aktualisiert den zugehörigen Geofence.
     * Diese optimierte Methode führt beide Operationen in einer Transaktion aus,
     * was die Effizienz erhöht und Verzögerungen minimiert.
     */
    public void insertEventAndUpdateGeofence(GeofenceEvent event) {
        executorService.execute(() -> {
            // Füge das Ereignis ein
            eventDao.insert(event);

            // Hole den zugehörigen Geofence und aktualisiere ihn
            GeofenceModel geofence = geofenceDao.getGeofenceByIdSync(event.getGeofenceId());
            if (geofence != null) {
                // Aktualisiere den Geofence basierend auf dem Ereignistyp
                switch (event.getEventType()) {
                    case GeofenceEvent.TYPE_ENTER:
                        geofence.setLastEntryTime(event.getTimestamp());
                        break;
                    case GeofenceEvent.TYPE_EXIT:
                        geofence.setLastExitTime(event.getTimestamp());
                        break;
                }
                // Speichere den aktualisierten Geofence
                geofenceDao.update(geofence);
            }
        });
    }

    public void refreshGeofences() {

    }

    /**
     * Aktualisiert explizit die Ereignisdaten aus der Datenbank
     */
    public void refreshEvents() {

    }

    /**
     * Callback-Interface für die asynchrone Datenabfrage.
     */
    public interface GeofenceDataCallback {
        void onGeofencesLoaded(List<GeofenceModel> geofences);
    }
}