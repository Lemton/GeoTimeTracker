package de.dhbw.geofencinglbs.location;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import de.dhbw.geofencinglbs.R;
import de.dhbw.geofencinglbs.ui.MainActivity;
import de.dhbw.geofencinglbs.util.DeviceInfoUtil;

/**
 * Hintergrunddienst für kontinuierliche Standortbestimmung.
 * Implementiert verschiedene Energiesparmodi und passt die Abtastraten entsprechend an.
 */
public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "location_channel";
    private static final int NOTIFICATION_ID = 1001;

    // Verschiedene Prioritätsstufen für unterschiedliche Energiemodi
    public static final int MODE_HIGH_ACCURACY = 0;
    public static final int MODE_BALANCED = 1;
    public static final int MODE_LOW_POWER = 2;

    // Intervalldauern in Millisekunden für verschiedene Modi
    private static final long UPDATE_INTERVAL_HIGH_ACCURACY = 5000; // 5 Sekunden
    private static final long UPDATE_INTERVAL_BALANCED = 15000; // 15 Sekunden
    private static final long UPDATE_INTERVAL_LOW_POWER = 60000; // 1 Minute

    private final IBinder binder = new LocalBinder();
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private int currentMode = MODE_BALANCED; // Standard-Modus
    private LocationListener locationListener;

    /**
     * Binder-Klasse für die Service-Verbindung.
     */
    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationCallback();
        createLocationRequest();
        createNotificationChannel();
    }

    /**
     * Erstellt den LocationCallback, der Standortaktualisierungen empfängt.
     */
    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    // Aktualisiere den letzten bekannten Standort
                    lastLocation = location;

                    // Erfasse Metadaten für die Analyse
                    float batteryLevel = DeviceInfoUtil.getBatteryLevel(LocationService.this);
                    boolean isCharging = DeviceInfoUtil.isDeviceCharging(LocationService.this);
                    String networkType = DeviceInfoUtil.getNetworkConnectionType(LocationService.this);

                    // Logge Standortinformationen
                    Log.d(TAG, String.format("Location update: %.6f, %.6f (Accuracy: %.2fm, Provider: %s, Battery: %.1f%%)",
                            location.getLatitude(), location.getLongitude(),
                            location.getAccuracy(), location.getProvider(), batteryLevel));

                    // Benachrichtige Listener (falls vorhanden)
                    if (locationListener != null) {
                        locationListener.onLocationChanged(location, batteryLevel, isCharging, networkType);
                    }

                    // Adaptive Anpassung des Standortmodus basierend auf Batteriestatus
                    adaptLocationUpdateRate(batteryLevel, isCharging);
                }
            }
        };
    }

    /**
     * Erstellt die LocationRequest-Konfiguration basierend auf dem aktuellen Modus.
     */
    private void createLocationRequest() {
        locationRequest = new LocationRequest.Builder(getPriorityForMode(currentMode), getIntervalForMode(currentMode))
                .setMinUpdateIntervalMillis(getIntervalForMode(currentMode) / 2)
                .setMaxUpdateDelayMillis(getIntervalForMode(currentMode) * 2)
                .build();
    }

    /**
     * Startet die Standortaktualisierungen.
     */
    @SuppressWarnings("MissingPermission") // Die Berechtigungen werden in der Activity überprüft
    private void startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
            Log.d(TAG, "Started location updates with interval: " + getIntervalForMode(currentMode) + "ms");
        } catch (SecurityException e) {
            Log.e(TAG, "Lost location permission: " + e.getMessage());
        }
    }

    /**
     * Stoppt die Standortaktualisierungen.
     */
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Log.d(TAG, "Stopped location updates");
    }

    /**
     * Setzt den Standortmodus und aktualisiert die LocationRequest-Konfiguration.
     */
    public void setLocationMode(int mode) {
        this.currentMode = mode;
        createLocationRequest();

        // Wenn bereits aktiv, aktualisieren
        if (locationCallback != null) {
            stopLocationUpdates();
            startLocationUpdates();
        }

        Log.d(TAG, "Location mode changed to: " + getModeString());
    }

    /**
     * Passt die Aktualisierungsrate basierend auf Batteriestatus an.
     */
    private void adaptLocationUpdateRate(float batteryLevel, boolean isCharging) {
        // Wenn das Gerät geladen wird, verwenden wir immer den hochpräzisen Modus
        if (isCharging) {
            if (currentMode != MODE_HIGH_ACCURACY) {
                setLocationMode(MODE_HIGH_ACCURACY);
            }
            return;
        }

        // Batterie-basierte Anpassung
        if (batteryLevel < 15 && currentMode != MODE_LOW_POWER) {
            // Bei kritischem Batteriestand in den Energiesparmodus wechseln
            setLocationMode(MODE_LOW_POWER);
        } else if (batteryLevel < 30 && currentMode == MODE_HIGH_ACCURACY) {
            // Bei niedrigem Batteriestand in den ausgewogenen Modus wechseln
            setLocationMode(MODE_BALANCED);
        } else if (batteryLevel > 50 && currentMode == MODE_LOW_POWER) {
            // Bei ausreichendem Batteriestand in den ausgewogenen Modus wechseln
            setLocationMode(MODE_BALANCED);
        }
    }

    /**
     * Gibt die Priorität basierend auf dem aktuellen Modus zurück.
     */
    private int getPriorityForMode(int mode) {
        switch (mode) {
            case MODE_HIGH_ACCURACY:
                return Priority.PRIORITY_HIGH_ACCURACY;
            case MODE_BALANCED:
                return Priority.PRIORITY_BALANCED_POWER_ACCURACY;
            case MODE_LOW_POWER:
                return Priority.PRIORITY_LOW_POWER;
            default:
                return Priority.PRIORITY_BALANCED_POWER_ACCURACY;
        }
    }

    /**
     * Gibt das Aktualisierungsintervall basierend auf dem aktuellen Modus zurück.
     */
    private long getIntervalForMode(int mode) {
        switch (mode) {
            case MODE_HIGH_ACCURACY:
                return UPDATE_INTERVAL_HIGH_ACCURACY;
            case MODE_BALANCED:
                return UPDATE_INTERVAL_BALANCED;
            case MODE_LOW_POWER:
                return UPDATE_INTERVAL_LOW_POWER;
            default:
                return UPDATE_INTERVAL_BALANCED;
        }
    }

    /**
     * Gibt die Bezeichnung des aktuellen Modus zurück.
     */
    private String getModeString() {
        switch (currentMode) {
            case MODE_HIGH_ACCURACY:
                return "Hohe Genauigkeit";
            case MODE_BALANCED:
                return "Ausgewogen";
            case MODE_LOW_POWER:
                return "Energiesparmodus";
            default:
                return "Unbekannt";
        }
    }

    /**
     * Erstellt den Benachrichtigungskanal für Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Standort-Updates",
                    NotificationManager.IMPORTANCE_LOW); // Niedrige Priorität für Dauerbetrieb
            channel.setDescription("Ermöglicht die kontinuierliche Standortbestimmung im Hintergrund");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Erstellt die Vordergrund-Benachrichtigung für den Service.
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Standorterfassung aktiv")
                .setContentText("Modus: " + getModeString())
                .setSmallIcon(R.drawable.ic_notification) // Diese Ressource muss erstellt werden
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");

        // Als Vordergrunddienst starten (erforderlich für Android 8.0+)
        startForeground(NOTIFICATION_ID, createNotification());

        // Standortaktualisierungen starten
        startLocationUpdates();

        // Service neu starten, wenn er vom System beendet wird
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        stopLocationUpdates();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Setzt einen Listener für Standortaktualisierungen.
     */
    public void setLocationListener(LocationListener listener) {
        this.locationListener = listener;
    }

    /**
     * Gibt den letzten bekannten Standort zurück.
     */
    public Location getLastLocation() {
        return lastLocation;
    }

    /**
     * Interface für Standortaktualisierungen.
     */
    public interface LocationListener {
        void onLocationChanged(Location location, float batteryLevel, boolean isCharging, String networkType);
    }
}