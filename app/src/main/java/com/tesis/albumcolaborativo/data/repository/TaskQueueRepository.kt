package com.tesis.albumcolaborativo.data.repository

import com.tesis.albumcolaborativo.data.local.dao.TaskQueueDao
import com.tesis.albumcolaborativo.data.local.entities.TaskQueueEntity

class TaskQueueRepository(
    private val taskQueueDao: TaskQueueDao
) {

    suspend fun enqueueTasks(tasks: List<TaskQueueEntity>) {
        taskQueueDao.insertAll(tasks)
    }

    fun getTasksByAlbum(albumId: String) =
        taskQueueDao.getTasksByAlbum(albumId)

    suspend fun getPendingTasks(albumId: String): List<TaskQueueEntity> {
        return taskQueueDao.getTasksByEstado(
            albumId = albumId,
            estado = "PENDIENTE"
        )
    }

    suspend fun updateTaskStatus(
        taskId: String,
        status: String
    ) {
        taskQueueDao.updateEstado(
            taskId = taskId,
            estado = status
        )
    }

    suspend fun getAssignedTasks(albumId: String): List<TaskQueueEntity> {
        return taskQueueDao.getTasksByEstado(
            albumId = albumId,
            estado = "ASIGNADA"
        )
    }

    // CU-D-03: el bloque rechazado vuelve a la cola para la próxima distribución
    suspend fun releaseAssignedTasks(albumId: String): Int {
        return taskQueueDao.updateEstadoByAlbum(
            albumId = albumId,
            estadoActual = "ASIGNADA",
            nuevoEstado = "PENDIENTE"
        )
    }
}
