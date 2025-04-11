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
        statusMessage.postValue("Bereit");
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
        currentLocation.postValue(location);
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
        DeviceStatus status = deviceStatus.getValue();
        if (status == null) {
            status = new DeviceStatus();
        }
        status.setBatteryLevel(batteryLevel);
        status.setCharging(isCharging);
        status.setNetworkType(networkType);
        deviceStatus.postValue(status);
    }

    /**
     * Erweiterte Version: Aktualisiert den Gerätestatus mit Provider-Details.
     */
    public void updateDeviceStatus(float batteryLevel, boolean isCharging, String networkType, String providerDetails) {
        DeviceStatus status = deviceStatus.getValue();
        if (status == null) {
            status = new DeviceStatus();
        }
        status.setBatteryLevel(batteryLevel);
        status.setCharging(isCharging);
        status.setNetworkType(networkType);
        status.setProviderDetails(providerDetails);
        deviceStatus.postValue(status);
    }

    /**
     * Aktualisiert die Geofence- und Ereignis-Daten.
     */
    public void refreshGeofenceData() {
        isLoading.postValue(true);
        repository.refreshGeofences();
        repository.refreshEvents();
        isLoading.postValue(false);
    }

    /**
     * Fügt einen neuen Geofence hinzu.
     */
    public void addGeofence(String name, double latitude, double longitude, float radius) {
        isLoading.postValue(true);
        statusMessage.postValue("Füge Geofence hinzu...");

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
        isLoading.postValue(true);
        statusMessage.postValue("Aktualisiere Geofence...");

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
        isLoading.postValue(true);
        statusMessage.postValue("Lösche Geofence...");

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
            isLoading.postValue(false);
            statusMessage.postValue("Keine aktiven Geofences");
            return;
        }

        geofenceManager.updateGeofences(geofences, new GeofenceManager.GeofenceCallback() {
            @Override
            public void onSuccess() {
                isLoading.postValue(false);
                statusMessage.postValue("Geofences erfolgreich aktualisiert");
                Log.d(TAG, "Geofences successfully registered: " + geofences.size());
            }

            @Override
            public void onError(String errorMessage) {
                isLoading.postValue(false);
                statusMessage.postValue("Fehler: " + errorMessage);
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
        private String providerDetails;
        private int locationMode = 1; // Standard: Ausgewogen

        public DeviceStatus() {
            this.batteryLevel = 0;
            this.isCharging = false;
            this.networkType = "UNKNOWN";
            this.providerDetails = "Initializing...";
        }

        public DeviceStatus(float batteryLevel, boolean isCharging, String networkType) {
            this.batteryLevel = batteryLevel;
            this.isCharging = isCharging;
            this.networkType = networkType;
            this.providerDetails = "";
        }

        public float getBatteryLevel() {
            return batteryLevel;
        }

        public void setBatteryLevel(float batteryLevel) {
            this.batteryLevel = batteryLevel;
        }

        public boolean isCharging() {
            return isCharging;
        }

        public void setCharging(boolean charging) {
            isCharging = charging;
        }

        public String getNetworkType() {
            return networkType;
        }

        public void setNetworkType(String networkType) {
            this.networkType = networkType;
        }

        public String getProviderDetails() {
            return providerDetails;
        }

        public void setProviderDetails(String providerDetails) {
            this.providerDetails = providerDetails;
        }

        public int getLocationMode() {
            return locationMode;
        }

        public void setLocationMode(int locationMode) {
            this.locationMode = locationMode;
        }
    }
}