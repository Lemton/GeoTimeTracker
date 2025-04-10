package de.dhbw.geofencinglbs.geofencing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.BatteryManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

import de.dhbw.geofencinglbs.data.repository.GeofenceRepository;
import de.dhbw.geofencinglbs.model.GeofenceEvent;
import de.dhbw.geofencinglbs.util.DeviceInfoUtil;
import de.dhbw.geofencinglbs.util.NotificationHelper;

/**
 * BroadcastReceiver für Geofence-Ereignisse.
 * Wird aufgerufen, wenn ein Geofence-Übergang (ENTER, EXIT, DWELL) erkannt wird.
 */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Geofence broadcast received");

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent fromIntent returned null");
            return;
        }

        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, "Geofencing error: " + errorMessage);
            return;
        }

        // Übergangstyp bestimmen
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Prüfen, ob es sich um ein bekanntes Übergangs-Event handelt
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {

            // Betroffene Geofences abrufen
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Bei leerer Liste gibt es nichts zu tun
            if (triggeringGeofences == null || triggeringGeofences.isEmpty()) {
                Log.w(TAG, "No triggering geofences found");
                return;
            }

            // Für jeden ausgelösten Geofence ein Event erstellen
            for (Geofence geofence : triggeringGeofences) {
                processGeofenceTransition(context, geofence, geofenceTransition, geofencingEvent.getTriggeringLocation());
            }
        } else {
            Log.e(TAG, "Unknown geofence transition type: " + geofenceTransition);
        }
    }

    /**
     * Verarbeitet einen einzelnen Geofence-Übergang.
     */
    private void processGeofenceTransition(Context context, Geofence geofence, int transitionType, Location location) {
        // Geofence-ID (String) in eine Long-ID für die Datenbank umwandeln
        long geofenceId;
        try {
            geofenceId = Long.parseLong(geofence.getRequestId());
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing geofence ID: " + e.getMessage());
            return;
        }

        // Event-Typ für unsere Datenbank bestimmen
        int eventType;
        String transitionName;

        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                eventType = GeofenceEvent.TYPE_ENTER;
                transitionName = "Betreten";
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                eventType = GeofenceEvent.TYPE_EXIT;
                transitionName = "Verlassen";
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                eventType = GeofenceEvent.TYPE_DWELL;
                transitionName = "Verweilen";
                break;
            default:
                Log.e(TAG, "Unknown transition type: " + transitionType);
                return;
        }

        // Batteriestatus abrufen
        float batteryLevel = DeviceInfoUtil.getBatteryLevel(context);
        boolean isCharging = DeviceInfoUtil.isDeviceCharging(context);
        String networkType = DeviceInfoUtil.getNetworkConnectionType(context);

        // Geofence-Event erstellen
        GeofenceEvent event = new GeofenceEvent(
                geofenceId,
                eventType,
                System.currentTimeMillis(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getProvider(),
                batteryLevel,
                isCharging,
                networkType
        );

        // Event in der Datenbank speichern
        GeofenceRepository repository = new GeofenceRepository(
                (android.app.Application) context.getApplicationContext());
        repository.insertEvent(event);

        // Debugging-Informationen
        Log.d(TAG, String.format("Geofence ID %d: %s (Accuracy: %.2fm, Provider: %s, Battery: %.1f%%)",
                geofenceId, transitionName, location.getAccuracy(), location.getProvider(), batteryLevel));

        // Benachrichtigung erstellen
        NotificationHelper.showGeofenceNotification(context, geofenceId, transitionType, location);
    }
}