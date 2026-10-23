package com.tesis.albumcolaborativo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classification_results")
data class ClassificationResultEntity(

    @PrimaryKey
    val taskId: String,

    val experimentId: String,
    val albumId: String,
    val imagePath: String,

    val predictedClassIndex: Int,
    val predictedScore: Float,

    // Las cinco predicciones se guardarán como JSON.
    val topPredictionsJson: String,

    // Tiempos medidos en milisegundos.
    val preprocessingTimeMs: Long,
    val tensorPreparationTimeMs: Long,
    val inferenceTimeMs: Long,
    val totalProcessingTimeMs: Long,

    val deviceId: String,
    val completedAt: Long = System.currentTimeMillis()
)