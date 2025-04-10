package de.dhbw.geofencinglbs.data.repository;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.dhbw.geofencinglbs.data.local.AppDatabase;
import de.dhbw.geofencinglbs.data.local.GeofenceDao;
import de.dhbw.geofencinglbs.data.local.GeofenceEventDao;
import de.dhbw.geofencinglbs.model.GeofenceEvent;
import de.dhbw.geofencinglbs.model.GeofenceModel;

/**
 * Repository für den Zugriff auf Geofence-Daten.
 * Stellt eine saubere API für den Datenzugriff bereit und
 * abstrahiert die Datenquelle (lokale Datenbank).
 */
public class GeofenceRepository {

    private final GeofenceDao geofenceDao;
    private final GeofenceEventDao geofenceEventDao;
    private final LiveData<List<GeofenceModel>> allGeofences;
    private final ExecutorService executor;

    public GeofenceRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        geofenceDao = database.geofenceDao();
        geofenceEventDao = database.geofenceEventDao();
        allGeofences = geofenceDao.getAllGeofences();
        executor = Executors.newSingleThreadExecutor();
    }

    // Methoden für Geofence-Operationen
    public LiveData<List<GeofenceModel>> getAllGeofences() {
        return allGeofences;
    }

    public void insert(GeofenceModel geofence) {
        executor.execute(() -> {
            geofenceDao.insert(geofence);
        });
    }

    public void update(GeofenceModel geofence) {
        executor.execute(() -> {
            geofenceDao.update(geofence);
        });
    }

    public void delete(GeofenceModel geofence) {
        executor.execute(() -> {
            geofenceDao.delete(geofence);
        });
    }

    public LiveData<GeofenceModel> getGeofenceById(long geofenceId) {
        return geofenceDao.getGeofenceByIdLive(geofenceId);
    }

    public void getActiveGeofences(GeofenceCallback callback) {
        executor.execute(() -> {
            List<GeofenceModel> geofences = geofenceDao.getActiveGeofences();
            callback.onGeofencesLoaded(geofences);
        });
    }

    // Methoden für Geofence-Event-Operationen
    public void insertEvent(GeofenceEvent event) {
        executor.execute(() -> {
            long eventId = geofenceEventDao.insert(event);
            GeofenceModel geofence = geofenceDao.getGeofenceById(event.getGeofenceId());

            // Aktualisiere den Geofence mit dem letzten Ereignis
            switch (event.getEventType()) {
                case GeofenceEvent.TYPE_ENTER:
                    geofence.setLastEntryTime(event.getTimestamp());
                    break;
                case GeofenceEvent.TYPE_EXIT:
                    geofence.setLastExitTime(event.getTimestamp());
                    break;
            }

            geofenceDao.update(geofence);
        });
    }

    public LiveData<List<GeofenceEvent>> getEventsForGeofence(long geofenceId) {
        return geofenceEventDao.getEventsForGeofence(geofenceId);
    }

    public LiveData<List<GeofenceEvent>> getRecentEvents(int limit) {
        return geofenceEventDao.getRecentEvents(limit);
    }

    public void calculateDwellTime(long geofenceId, long startTime, long endTime, DwellTimeCallback callback) {
        executor.execute(() -> {
            long dwellTime = geofenceEventDao.calculateTotalDwellTime(geofenceId, startTime, endTime);
            callback.onDwellTimeCalculated(dwellTime);
        });
    }

    // Callback-Interfaces
    public interface GeofenceCallback {
        void onGeofencesLoaded(List<GeofenceModel> geofences);
    }

    public interface DwellTimeCallback {
        void onDwellTimeCalculated(long dwellTimeMillis);
    }
}