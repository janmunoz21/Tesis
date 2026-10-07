package com.tesis.albumcolaborativo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tesis.albumcolaborativo.data.local.dao.TaskQueueDao
import com.tesis.albumcolaborativo.data.local.entities.TaskQueueEntity

@Database(
    entities = [
        TaskQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskQueueDao(): TaskQueueDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "album_colaborativo.db"
                ).build()

                INSTANCE = instance

                instance
            }
        }
    }
}