package com.tesis.albumcolaborativo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "experiment_metrics")
data class ExperimentMetricsEntity(

    @PrimaryKey
    val experimentId: String,

    // LOCAL o DISTRIBUTED
    val executionMode: String,

    val deviceCount: Int,
    val totalImages: Int,

    val startedAt: Long,
    val completedAt: Long? = null,

    val totalDurationMs: Long? = null,

    val successfulTasks: Int = 0,
    val failedTasks: Int = 0
)