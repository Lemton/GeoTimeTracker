package com.example.lbsapp.tracking.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.lbsapp.repository.GeofenceRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "GeofenceReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "GeofenceBroadcastReceiver.onReceive wurde aufgerufen - Intent: ${intent.action}")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent konnte nicht aus Intent erstellt werden")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofencing Error: $errorMessage (Code: ${geofencingEvent.errorCode})")
            return
        }

        // Überprüfe, welcher Übergang ausgelöst wurde
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Hol die ausgelösten Geofences
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        if (triggeringGeofences == null || triggeringGeofences.isEmpty()) {
            Log.e(TAG, "Keine Geofences in diesem Event enthalten")
            return
        }

        Log.d(TAG, "Geofence-Ereignis erkannt: ${getTransitionString(geofenceTransition)}")
        Log.d(TAG, "Anzahl der auslösenden Geofences: ${triggeringGeofences.size}")

        // Repository für Datenbankoperationen
        val repository = GeofenceRepository(context.applicationContext as android.app.Application)

        // Verarbeite den Geofencing-Übergang
        for (geofence in triggeringGeofences) {
            val geofenceId = geofence.requestId
            Log.d(TAG, "Verarbeite Geofence mit ID: $geofenceId und Übergang: ${getTransitionString(geofenceTransition)}")

            when (geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    processGeofenceEnter(context, geofence, repository)
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    processGeofenceExit(context, geofence, repository)
                }
                Geofence.GEOFENCE_TRANSITION_DWELL -> {
                    Log.d(TAG, "DWELL-Ereignis für Geofence $geofenceId erkannt")
                }
                else -> {
                    Log.d(TAG, "Unbekannter Übergang: $geofenceTransition")
                }
            }
        }
    }

    private fun processGeofenceEnter(context: Context, geofence: Geofence, repository: GeofenceRepository) {
        val geofenceId = geofence.requestId.toLongOrNull()

        if (geofenceId == null) {
            Log.e(TAG, "Geofence-ID konnte nicht in Long umgewandelt werden: ${geofence.requestId}")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Versuche, Eintritt für Geofence $geofenceId zu speichern")

            try {
                // Prüfe, ob bereits ein aktiver Besuch existiert
                val activeVisits = repository.getActiveVisits(geofenceId)
                Log.d(TAG, "Aktive Besuche für Geofence $geofenceId: ${activeVisits.size}")

                if (activeVisits.isEmpty()) {
                    // Nur einen neuen Eintrag erstellen, wenn kein aktiver Besuch existiert
                    val id = repository.recordEntry(geofenceId)
                    Log.d(TAG, "Eintritt in Geofence $geofenceId erfolgreich aufgezeichnet mit ID: $id")

                    // Benachrichtigung senden
                    sendNotification(
                        context,
                        "Geofence betreten",
                        "Du hast den Bereich '${getGeofenceName(repository, geofenceId)}' betreten. Die Zeitmessung hat begonnen."
                    )
                } else {
                    Log.d(TAG, "Eintritt ignoriert, bereits aktiver Besuch für Geofence $geofenceId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Aufzeichnen des Eintritts: ${e.message}", e)
            }
        }
    }

    private fun processGeofenceExit(context: Context, geofence: Geofence, repository: GeofenceRepository) {
        val geofenceId = geofence.requestId.toLongOrNull()

        if (geofenceId == null) {
            Log.e(TAG, "Geofence-ID konnte nicht in Long umgewandelt werden: ${geofence.requestId}")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Versuche, Austritt für Geofence $geofenceId zu speichern")

            try {
                // Prüfe, ob ein aktiver Besuch existiert
                val activeVisits = repository.getActiveVisits(geofenceId)
                Log.d(TAG, "Aktive Besuche für Geofence $geofenceId: ${activeVisits.size}")

                if (activeVisits.isNotEmpty()) {
                    // Nur einen Austritt aufzeichnen, wenn ein aktiver Besuch existiert
                    val success = repository.recordExit(geofenceId)
                    Log.d(TAG, "Austritt aus Geofence $geofenceId aufgezeichnet: $success")

                    // Aufenthaltszeit abrufen
                    val totalTime = repository.getTotalTimeInGeofence(geofenceId)
                    val formattedTime = formatDuration(totalTime)

                    // Benachrichtigung senden
                    sendNotification(
                        context,
                        "Geofence verlassen",
                        "Du hast den Bereich '${getGeofenceName(repository, geofenceId)}' verlassen. Aufenthaltsdauer: $formattedTime"
                    )
                } else {
                    Log.d(TAG, "Austritt ignoriert, kein aktiver Besuch für Geofence $geofenceId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Aufzeichnen des Austritts: ${e.message}", e)
            }
        }
    }

    private suspend fun getGeofenceName(repository: GeofenceRepository, geofenceId: Long): String {
        return try {
            repository.getGeofenceName(geofenceId) ?: "Unbekannt"
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Abrufen des Geofence-Namens: ${e.message}")
            "Unbekannt"
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> "$hours h ${minutes % 60} min"
            minutes > 0 -> "$minutes min ${seconds % 60} s"
            else -> "$seconds s"
        }
    }

    private fun sendNotification(context: Context, title: String, message: String) {
        // Hier könntest du eine richtige Benachrichtigung implementieren
        // Für diesen Prototyp nur Logging
        Log.i(TAG, "BENACHRICHTIGUNG: $title - $message")
    }

    private fun getTransitionString(transition: Int): String {
        return when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "DWELL"
            else -> "UNKNOWN($transition)"
        }
    }

    // Nur für Debugging - eine Methode, die manuell aufgerufen werden kann
    fun simulateGeofenceEntry(context: Context, geofenceId: Long) {
        Log.d(TAG, "Simuliere Geofence-Eintritt für ID: $geofenceId")
        val repository = GeofenceRepository(context.applicationContext as android.app.Application)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val id = repository.recordEntry(geofenceId)
                Log.d(TAG, "Simulierter Eintritt für Geofence $geofenceId erfolgreich mit ID: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Simulieren des Eintritts: ${e.message}", e)
            }
        }
    }

    fun simulateGeofenceExit(context: Context, geofenceId: Long) {
        Log.d(TAG, "Simuliere Geofence-Austritt für ID: $geofenceId")
        val repository = GeofenceRepository(context.applicationContext as android.app.Application)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = repository.recordExit(geofenceId)
                Log.d(TAG, "Simulierter Austritt für Geofence $geofenceId: $success")
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Simulieren des Austritts: ${e.message}", e)
            }
        }
    }
}