package com.tesis.albumcolaborativo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tesis.albumcolaborativo.data.local.entities.ExperimentMetricsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExperimentMetricsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(experiment: ExperimentMetricsEntity)

    @Query("""
        SELECT * FROM experiment_metrics
        ORDER BY startedAt DESC
    """)
    fun getAll(): Flow<List<ExperimentMetricsEntity>>

    @Query("""
        SELECT * FROM experiment_metrics
        WHERE experimentId = :experimentId
        LIMIT 1
    """)
    suspend fun getById(
        experimentId: String
    ): ExperimentMetricsEntity?

    @Query("""
        UPDATE experiment_metrics
        SET completedAt = :completedAt,
            totalDurationMs = :durationMs,
            successfulTasks = :successfulTasks,
            failedTasks = :failedTasks
        WHERE experimentId = :experimentId
    """)
    suspend fun finishExperiment(
        experimentId: String,
        completedAt: Long,
        durationMs: Long,
        successfulTasks: Int,
        failedTasks: Int
    ): Int
}