package com.tesis.albumcolaborativo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_queue")
data class TaskQueueEntity(

    @PrimaryKey
    val taskId: String,

    val albumId: String,

    val tipoTarea: String,

    val payloadJson: String,

    val estadoSincronizacion: String,

    val fechaCreacion: Long = System.currentTimeMillis()
)