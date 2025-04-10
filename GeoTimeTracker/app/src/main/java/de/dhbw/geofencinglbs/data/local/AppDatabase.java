package de.dhbw.geofencinglbs.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import de.dhbw.geofencinglbs.model.GeofenceEvent;
import de.dhbw.geofencinglbs.model.GeofenceModel;

/**
 * Room-Datenbankklasse für die gesamte Anwendung.
 * Implementiert das Singleton-Muster.
 */
@Database(entities = {GeofenceModel.class, GeofenceEvent.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "geofencing_db";
    private static AppDatabase instance;

    public abstract GeofenceDao geofenceDao();
    public abstract GeofenceEventDao geofenceEventDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                    .fallbackToDestructiveMigration() // Bei Schema-Änderungen Datenbank neu erstellen
                    .build();
        }
        return instance;
    }
}