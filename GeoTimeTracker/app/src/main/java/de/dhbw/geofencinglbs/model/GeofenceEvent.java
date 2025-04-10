package de.dhbw.geofencinglbs.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Repräsentiert ein Geofence-Ereignis (Eintritt oder Austritt).
 * Wird für die Historisierung und Auswertung von Aufenthaltszeiten verwendet.
 */
@Entity(tableName = "geofence_events",
        foreignKeys = @ForeignKey(
                entity = GeofenceModel.class,
                parentColumns = "id",
                childColumns = "geofenceId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("geofenceId")})
public class GeofenceEvent {

    public static final int TYPE_ENTER = 1;
    public static final int TYPE_EXIT = 2;
    public static final int TYPE_DWELL = 3;

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long geofenceId;
    private int eventType; // 1=ENTER, 2=EXIT, 3=DWELL
    private long timestamp;
    private double latitude;
    private double longitude;
    private float accuracy; // Genauigkeit in Metern
    private String provider; // GPS, NETWORK, FUSED

    // Zusätzliche Metadaten über den Kontext
    private float batteryLevel; // 0-100%
    private boolean isCharging;
    private String networkConnectionType; // WIFI, MOBILE, NONE

    // Standardkonstruktor für Room
    public GeofenceEvent() {
        // Room benötigt einen leeren Konstruktor
    }

    // Konstruktor für einfachste Verwendung - mit @Ignore markiert
    @Ignore
    public GeofenceEvent(long geofenceId, int eventType) {
        this.geofenceId = geofenceId;
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();
    }

    // Vollständiger Konstruktor - mit @Ignore markiert
    @Ignore
    public GeofenceEvent(long geofenceId, int eventType, long timestamp,
                         double latitude, double longitude, float accuracy,
                         String provider, float batteryLevel, boolean isCharging,
                         String networkConnectionType) {
        this.geofenceId = geofenceId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.provider = provider;
        this.batteryLevel = batteryLevel;
        this.isCharging = isCharging;
        this.networkConnectionType = networkConnectionType;
    }

    // Getter und Setter
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getGeofenceId() {
        return geofenceId;
    }

    public void setGeofenceId(long geofenceId) {
        this.geofenceId = geofenceId;
    }

    public int getEventType() {
        return eventType;
    }

    public void setEventType(int eventType) {
        this.eventType = eventType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

    public String getNetworkConnectionType() {
        return networkConnectionType;
    }

    public void setNetworkConnectionType(String networkConnectionType) {
        this.networkConnectionType = networkConnectionType;
    }

    /**
     * Gibt einen Ereignistyp als lesbaren String zurück.
     */
    public String getEventTypeString() {
        switch (eventType) {
            case TYPE_ENTER:
                return "Eintritt";
            case TYPE_EXIT:
                return "Austritt";
            case TYPE_DWELL:
                return "Verweilen";
            default:
                return "Unbekannt";
        }
    }
}