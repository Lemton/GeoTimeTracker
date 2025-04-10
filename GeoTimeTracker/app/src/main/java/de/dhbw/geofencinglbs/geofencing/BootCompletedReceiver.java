package de.dhbw.geofencinglbs.geofencing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

import de.dhbw.geofencinglbs.data.repository.GeofenceRepository;
import de.dhbw.geofencinglbs.location.LocationService;
import de.dhbw.geofencinglbs.model.GeofenceModel;

/**
 * BroadcastReceiver, der nach dem Geräteneustart ausgeführt wird und
 * den Location-Service und die Geofences neu startet.
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "Boot completed, starting location service and registering geofences");

            // LocationService starten
            Intent serviceIntent = new Intent(context, LocationService.class);
            context.startService(serviceIntent);

            // Geofences registrieren
            GeofenceRepository repository = new GeofenceRepository((android.app.Application) context.getApplicationContext());
            repository.getActiveGeofences(geofences -> {
                if (geofences != null && !geofences.isEmpty()) {
                    GeofenceManager.getInstance(context).registerGeofences(geofences, new GeofenceManager.GeofenceCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Successfully re-registered " + geofences.size() + " geofences after boot");
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Failed to re-register geofences after boot: " + errorMessage);
                        }
                    });
                }
            });
        }
    }
}