package de.dhbw.geofencinglbs.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.Geofence;

import de.dhbw.geofencinglbs.R;
import de.dhbw.geofencinglbs.ui.MainActivity;

/**
 * Hilfsklasse für die Erstellung und Verwaltung von Benachrichtigungen.
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "geofence_channel";
    private static final String CHANNEL_NAME = "Geofence Notifications";
    private static final String CHANNEL_DESCRIPTION = "Zeigt Benachrichtigungen für Geofence-Ereignisse";

    /**
     * Erstellt einen Benachrichtigungskanal für Android 8.0+
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Zeigt eine Benachrichtigung für ein Geofence-Ereignis an.
     */
    public static void showGeofenceNotification(Context context, long geofenceId, int transitionType, Location location) {
        // Intent für den Klick auf die Benachrichtigung
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("geofence_id", geofenceId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, flags);

        // Titel und Text der Benachrichtigung
        String title;
        String text;

        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                title = "Geofence betreten";
                text = "Du hast Zone " + geofenceId + " betreten";
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                title = "Geofence verlassen";
                text = "Du hast Zone " + geofenceId + " verlassen";
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                title = "Im Geofence verweilt";
                text = "Du verweilst in Zone " + geofenceId;
                break;
            default:
                title = "Geofence-Ereignis";
                text = "Unbekannter Übergang für Zone " + geofenceId;
        }

        // Zusätzliche Informationen zur Genauigkeit
        String accuracy = String.format("Genauigkeit: %.1f m", location.getAccuracy());

        // Benachrichtigung erstellen
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Diese Ressource muss erstellt werden
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(text + "\n" + accuracy))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Benachrichtigung anzeigen
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            // Eindeutige ID für jede Benachrichtigung
            int notificationId = (int) (geofenceId * 10 + transitionType); // Eindeutige ID aus Geofence-ID und Übergangstyp
            notificationManager.notify(notificationId, builder.build());
        }
    }
}