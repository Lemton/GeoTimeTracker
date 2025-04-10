package de.dhbw.geofencinglbs.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Repräsentiert einen Geofence, der in der App überwacht wird.
 * Dieses Modell wird sowohl für die UI als auch für die Datenbank verwendet.
 */
@Entity(tableName = "geofences")
public class GeofenceModel {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private double latitude;
    private double longitude;
    private float radius; // in Metern
    private long createdAt;
    private boolean isActive;

    // Timestamp der letzten Eintrittsereignisse
    private long lastEntryTime;
    private long lastExitTime;

    // Akkumulierte Aufenthaltszeit in Millisekunden
    private long totalDwellTime;

    // Konstruktor
    public GeofenceModel(String name, double latitude, double longitude, float radius) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
        this.lastEntryTime = 0;
        this.lastExitTime = 0;
        this.totalDwellTime = 0;
    }

    // Getter und Setter
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public long getLastEntryTime() {
        return lastEntryTime;
    }

    public void setLastEntryTime(long lastEntryTime) {
        this.lastEntryTime = lastEntryTime;
    }

    public long getLastExitTime() {
        return lastExitTime;
    }

    public void setLastExitTime(long lastExitTime) {
        this.lastExitTime = lastExitTime;

        // Wenn wir den Geofence verlassen und ein Eintritt registriert wurde,
        // aktualisieren wir die Aufenthaltszeit
        if (this.lastEntryTime > 0) {
            this.totalDwellTime += (lastExitTime - this.lastEntryTime);
        }
    }

    public long getTotalDwellTime() {
        return totalDwellTime;
    }

    public void setTotalDwellTime(long totalDwellTime) {
        this.totalDwellTime = totalDwellTime;
    }

    /**
     * Berechnet die aktuelle Aufenthaltszeit, falls wir uns noch im Geofence befinden
     * @return Aktuelle Aufenthaltszeit in Millisekunden
     */
    public long getCurrentDwellTime() {
        if (lastEntryTime > 0 && (lastExitTime == 0 || lastEntryTime > lastExitTime)) {
            // Wir sind noch im Geofence, berechne die aktuelle Zeit + bereits akkumulierte Zeit
            return totalDwellTime + (System.currentTimeMillis() - lastEntryTime);
        }
        return totalDwellTime;
    }

    /**
     * Formatiert die Aufenthaltszeit als String im Format "HH:MM:SS"
     */
    public String getFormattedDwellTime() {
        long millis = getCurrentDwellTime();
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        return String.format("%02d:%02d:%02d",
                hours,
                minutes % 60,
                seconds % 60);
    }
}