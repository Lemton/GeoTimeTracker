package com.example.lbsapp.database

import com.example.lbsapp.database.GeofenceDao
import com.example.lbsapp.database.VisitDao
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [GeofenceEntity::class, VisitEntity::class],
    version = 1,
    exportSchema = false // Verhindert die Schema-Export-Warnung
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun geofenceDao(): GeofenceDao
    abstract fun visitDao(): VisitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lbs_app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}