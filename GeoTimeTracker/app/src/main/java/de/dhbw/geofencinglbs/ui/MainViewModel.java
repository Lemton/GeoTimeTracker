package de.dhbw.geofencinglbs.ui;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import de.dhbw.geofencinglbs.data.repository.GeofenceRepository;
import de.dhbw.geofencinglbs.geofencing.GeofenceManager;
import de.dhbw.geofencinglbs.model.GeofenceEvent;
import de.dhbw.geofencinglbs.model.GeofenceModel;

/**
 * ViewModel für die Hauptaktivität.
 * Verwaltet die Geofences und stellt LiveData für die UI bereit.
 */
public class MainViewModel extends AndroidViewModel {
    private static final String TAG = "MainViewModel";

    private final GeofenceRepository repository;
    private final GeofenceManager geofenceManager;
    private final LiveData<List<GeofenceModel>> allGeofences;
    private final LiveData<List<GeofenceEvent>> recentEvents;

    // LiveData für den aktuellen Standort
    private final MutableLiveData<Location> currentLocation = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<DeviceStatus> deviceStatus = new MutableLiveData<>(new DeviceStatus());

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new GeofenceRepository(application);
        geofenceManager = GeofenceManager.getInstance(application);
        allGeofences = repository.getAllGeofences();
        recentEvents = repository.getRecentEvents(50);

        // Standard-Statusmeldung
        statusMessage.setValue("Bereit");
    }

    /**
     * Gibt alle gespeicherten Geofences zurück.
     */
    public LiveData<List<GeofenceModel>> getAllGeofences() {
        return allGeofences;
    }

    /**
     * Gibt die neuesten Geofence-Ereignisse zurück.
     */
    public LiveData<List<GeofenceEvent>> getRecentEvents() {
        return recentEvents;
    }

    /**
     * Gibt den aktuellen Standort zurück.
     */
    public LiveData<Location> getCurrentLocation() {
        return currentLocation;
    }

    /**
     * Setzt den aktuellen Standort.
     */
    public void setCurrentLocation(Location location) {
        currentLocation.setValue(location);
    }

    /**
     * Gibt die aktuelle Statusmeldung zurück.
     */
    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    /**
     * Gibt den Lade-Status zurück.
     */
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    /**
     * Gibt den Gerätestatus zurück.
     */
    public LiveData<DeviceStatus> getDeviceStatus() {
        return deviceStatus;
    }

    /**
     * Aktualisiert den Gerätestatus.
     */
    public void updateDeviceStatus(float batteryLevel, boolean isCharging, String networkType) {
        DeviceStatus status = new DeviceStatus(batteryLevel, isCharging, networkType);
        deviceStatus.setValue(status);
    }

    /**
     * Fügt einen neuen Geofence hinzu.
     */
    public void addGeofence(String name, double latitude, double longitude, float radius) {
        isLoading.setValue(true);
        statusMessage.setValue("Füge Geofence hinzu...");

        GeofenceModel geofence = new GeofenceModel(name, latitude, longitude, radius);
        repository.insert(geofence);

        // Wir müssen warten, bis der Geofence in der Datenbank ist und eine ID hat,
        // bevor wir ihn beim GeofenceManager registrieren
        repository.getActiveGeofences(geofences -> {
            registerGeofences(geofences);
        });
    }

    /**
     * Aktualisiert einen vorhandenen Geofence.
     */
    public void updateGeofence(GeofenceModel geofence) {
        isLoading.setValue(true);
        statusMessage.setValue("Aktualisiere Geofence...");

        repository.update(geofence);

        // Alle aktiven Geofences neu laden und registrieren
        repository.getActiveGeofences(geofences -> {
            registerGeofences(geofences);
        });
    }

    /**
     * Löscht einen Geofence.
     */
    public void deleteGeofence(GeofenceModel geofence) {
        isLoading.setValue(true);
        statusMessage.setValue("Lösche Geofence...");

        repository.delete(geofence);

        // Alle aktiven Geofences neu laden und registrieren
        repository.getActiveGeofences(geofences -> {
            registerGeofences(geofences);
        });
    }

    /**
     * Aktiviert oder deaktiviert einen Geofence.
     */
    public void toggleGeofenceActive(GeofenceModel geofence, boolean isActive) {
        geofence.setActive(isActive);
        updateGeofence(geofence);
    }

    /**
     * Registriert alle aktiven Geofences beim GeofenceManager.
     */
    private void registerGeofences(List<GeofenceModel> geofences) {
        if (geofences.isEmpty()) {
            // Es gibt keine Geofences zu registrieren
            isLoading.setValue(false);
            statusMessage.setValue("Keine aktiven Geofences");
            return;
        }

        geofenceManager.updateGeofences(geofences, new GeofenceManager.GeofenceCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                statusMessage.setValue("Geofences erfolgreich aktualisiert");
                Log.d(TAG, "Geofences successfully registered: " + geofences.size());
            }

            @Override
            public void onError(String errorMessage) {
                isLoading.setValue(false);
                statusMessage.setValue("Fehler: " + errorMessage);
                Log.e(TAG, "Error registering geofences: " + errorMessage);
            }
        });
    }

    /**
     * Datenklasse für den Gerätestatus.
     */
    public static class DeviceStatus {
        private float batteryLevel;
        private boolean isCharging;
        private String networkType;

        public DeviceStatus() {
            this.batteryLevel = 0;
            this.isCharging = false;
            this.networkType = "UNKNOWN";
        }

        public DeviceStatus(float batteryLevel, boolean isCharging, String networkType) {
            this.batteryLevel = batteryLevel;
            this.isCharging = isCharging;
            this.networkType = networkType;
        }

        public float getBatteryLevel() {
            return batteryLevel;
        }

        public boolean isCharging() {
            return isCharging;
        }

        public String getNetworkType() {
            return networkType;
        }
    }
}