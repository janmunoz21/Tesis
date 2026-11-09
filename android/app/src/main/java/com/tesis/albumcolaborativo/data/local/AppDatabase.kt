package com.tesis.albumcolaborativo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tesis.albumcolaborativo.data.local.dao.TaskQueueDao
import com.tesis.albumcolaborativo.data.local.entities.TaskQueueEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tesis.albumcolaborativo.data.local.dao.ClassificationResultDao
import com.tesis.albumcolaborativo.data.local.dao.ExperimentMetricsDao
import com.tesis.albumcolaborativo.data.local.entities.ClassificationResultEntity
import com.tesis.albumcolaborativo.data.local.entities.ExperimentMetricsEntity

@Database(
    entities = [
        TaskQueueEntity::class,
        ClassificationResultEntity::class,
        ExperimentMetricsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskQueueDao(): TaskQueueDao
    abstract fun classificationResultDao(): ClassificationResultDao
    abstract fun experimentMetricsDao(): ExperimentMetricsDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS classification_results (
                taskId TEXT NOT NULL PRIMARY KEY,
                experimentId TEXT NOT NULL,
                albumId TEXT NOT NULL,
                imagePath TEXT NOT NULL,
                predictedClassIndex INTEGER NOT NULL,
                predictedScore REAL NOT NULL,
                topPredictionsJson TEXT NOT NULL,
                preprocessingTimeMs INTEGER NOT NULL,
                tensorPreparationTimeMs INTEGER NOT NULL,
                inferenceTimeMs INTEGER NOT NULL,
                totalProcessingTimeMs INTEGER NOT NULL,
                deviceId TEXT NOT NULL,
                completedAt INTEGER NOT NULL
            )
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS experiment_metrics (
                experimentId TEXT NOT NULL PRIMARY KEY,
                executionMode TEXT NOT NULL,
                deviceCount INTEGER NOT NULL,
                totalImages INTEGER NOT NULL,
                startedAt INTEGER NOT NULL,
                completedAt INTEGER,
                totalDurationMs INTEGER,
                successfulTasks INTEGER NOT NULL,
                failedTasks INTEGER NOT NULL
            )
            """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "album_colaborativo.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}