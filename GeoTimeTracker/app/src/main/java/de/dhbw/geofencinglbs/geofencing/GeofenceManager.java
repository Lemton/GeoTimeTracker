package de.dhbw.geofencinglbs.geofencing;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

import de.dhbw.geofencinglbs.model.GeofenceModel;

/**
 * Manager-Klasse für die Verwaltung von Geofences mit dem Fused Location Provider.
 * Implementiert das Singleton-Muster.
 */
public class GeofenceManager {
    private static final String TAG = "GeofenceManager";
    private static GeofenceManager instance;

    private final Context context;
    private final GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;

    private GeofenceManager(Context context) {
        this.context = context.getApplicationContext();
        this.geofencingClient = LocationServices.getGeofencingClient(context);
    }

    public static synchronized GeofenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new GeofenceManager(context);
        }
        return instance;
    }

    /**
     * Wandelt ein GeofenceModel in ein Geofence-Objekt um.
     */
    private Geofence createGeofence(GeofenceModel model) {
        return new Geofence.Builder()
                // Verwende die ID als String für die Google-API
                .setRequestId(String.valueOf(model.getId()))
                // Setze Koordinaten und Radius
                .setCircularRegion(model.getLatitude(), model.getLongitude(), model.getRadius())
                // Der Geofence soll unbegrenzt gültig sein (oder kann auch begrenzt werden)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                // Transitionen, die überwacht werden sollen
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT |
                        Geofence.GEOFENCE_TRANSITION_DWELL)
                // Verweildauer für DWELL-Events festlegen (in Millisekunden)
                .setLoiteringDelay(60000) // 1 Minute Verweildauer
                .build();
    }

    /**
     * Erstellt eine GeofencingRequest mit den übergebenen Geofence-Modellen.
     */
    private GeofencingRequest createGeofencingRequest(List<GeofenceModel> geofenceModels) {
        List<Geofence> geofenceList = new ArrayList<>();

        for (GeofenceModel model : geofenceModels) {
            if (model.isActive()) {
                geofenceList.add(createGeofence(model));
            }
        }

        Log.d(TAG, "Creating geofencing request with " + geofenceList.size() + " geofences");

        return new GeofencingRequest.Builder()
                // Auslösen beim Hinzufügen, wenn sich das Gerät bereits in der Zone befindet
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofenceList)
                .build();
    }

    /**
     * Registriert die übergebenen Geofences beim Fused Location Provider.
     */
    public void registerGeofences(List<GeofenceModel> geofenceModels, GeofenceCallback callback) {
        if (geofenceModels.isEmpty()) {
            if (callback != null) {
                callback.onError("Keine aktiven Geofences zum Registrieren");
            }
            return;
        }

        try {
            // Geofences beim Client registrieren
            geofencingClient.addGeofences(
                    createGeofencingRequest(geofenceModels),
                    getGeofencePendingIntent()
            ).addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Geofences successfully registered");
                if (callback != null) {
                    callback.onSuccess();
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to register geofences: " + e.getMessage());
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
            if (callback != null) {
                callback.onError("Fehlende Standortberechtigungen: " + e.getMessage());
            }
        }
    }

    /**
     * Entfernt alle registrierten Geofences.
     */
    public void removeGeofences(GeofenceCallback callback) {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Geofences successfully removed");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove geofences: " + e.getMessage());
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    /**
     * Erstellt das PendingIntent für Geofence-Transitionen.
     */
    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        // Android 12 und höher benötigt FLAG_MUTABLE oder FLAG_IMMUTABLE
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }

        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
        return geofencePendingIntent;
    }

    /**
     * Aktualisiert die registrierten Geofences nach Änderungen.
     */
    public void updateGeofences(List<GeofenceModel> geofenceModels, GeofenceCallback callback) {
        // Zuerst alle Geofences entfernen und dann neu registrieren
        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnCompleteListener(task -> {
                    // Unabhängig vom Ergebnis versuchen wir, die neuen Geofences zu registrieren
                    registerGeofences(geofenceModels, callback);
                });
    }

    /**
     * Aktualisiert einen einzelnen Geofence, indem alle Geofences aktualisiert werden.
     */
    public void updateSingleGeofence(GeofenceModel updatedGeofence, List<GeofenceModel> allGeofences, GeofenceCallback callback) {
        // Finde den alten Geofence und ersetze ihn durch den aktualisierten
        for (int i = 0; i < allGeofences.size(); i++) {
            if (allGeofences.get(i).getId() == updatedGeofence.getId()) {
                allGeofences.set(i, updatedGeofence);
                break;
            }
        }

        // Aktualisiere alle Geofences
        updateGeofences(allGeofences, callback);
    }

    /**
     * Callback-Interface für Geofence-Operationen.
     */
    public interface GeofenceCallback {
        void onSuccess();
        void onError(String errorMessage);
    }
}