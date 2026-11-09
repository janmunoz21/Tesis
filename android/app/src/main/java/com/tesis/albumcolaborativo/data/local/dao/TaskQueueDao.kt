package com.tesis.albumcolaborativo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tesis.albumcolaborativo.data.local.entities.TaskQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskQueueDao {

    @Insert
    suspend fun insert(task: TaskQueueEntity): Long

    @Insert
    suspend fun insertAll(tasks: List<TaskQueueEntity>): List<Long>

    @Query("SELECT * FROM task_queue WHERE albumId = :albumId")
    fun getTasksByAlbum(albumId: String): Flow<List<TaskQueueEntity>>

    @Query("""
        SELECT * FROM task_queue
        WHERE albumId = :albumId
        AND estadoSincronizacion = :estado
    """)
    suspend fun getTasksByEstado(
        albumId: String,
        estado: String
    ): List<TaskQueueEntity>

    @Query("""
        UPDATE task_queue
        SET estadoSincronizacion = :estado
        WHERE taskId = :taskId
    """)
    suspend fun updateEstado(
        taskId: String,
        estado: String
    ): Int
}