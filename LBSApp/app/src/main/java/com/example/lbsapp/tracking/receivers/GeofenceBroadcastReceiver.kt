package com.example.lbsapp.tracking.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

/**
 * BroadcastReceiver zum Empfangen von Geofencing-Events
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "GeofenceReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent != null) {
            if (geofencingEvent.hasError()) {
                val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
                Log.e(TAG, "Geofencing Error: $errorMessage")
                return
            }

            // Überprüfe, welcher Übergang ausgelöst wurde
            val geofenceTransition = geofencingEvent.geofenceTransition

            // Hol die ausgelösten Geofences
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            // Erstelle eine Log-Nachricht
            val geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition, triggeringGeofences)

            // Logge das Event
            Log.i(TAG, geofenceTransitionDetails)

            // Hier könnte eine Benachrichtigung gesendet werden oder ein Service gestartet werden
            sendNotification(context, geofenceTransitionDetails)
        }
    }

    private fun getGeofenceTransitionDetails(geofenceTransition: Int, triggeringGeofences: List<Geofence>?): String {
        val transitionString = when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "Benutzer hat Geofence betreten"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "Benutzer hat Geofence verlassen"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "Benutzer verweilt im Geofence"
            else -> "Unbekannter Übergang"
        }

        val triggeringGeofencesIdsList = triggeringGeofences?.map { it.requestId } ?: emptyList()

        return "$transitionString: ${triggeringGeofencesIdsList.joinToString(", ")}"
    }

    private fun sendNotification(context: Context, geofenceTransitionDetails: String) {
        // Hier könnte eine Benachrichtigung erstellt und angezeigt werden
        // Zum Beispiel über den NotificationManager

        // Für diese Implementierung werden wir nur einen Log-Eintrag erstellen
        Log.i(TAG, "Würde Benachrichtigung senden: $geofenceTransitionDetails")

        // In einer realen App würde hier ein NotificationCompat.Builder verwendet werden
        // und der NotificationManager würde die Benachrichtigung anzeigen
    }
}
