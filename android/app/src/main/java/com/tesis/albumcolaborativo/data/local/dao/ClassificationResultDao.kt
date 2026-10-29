package com.tesis.albumcolaborativo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tesis.albumcolaborativo.data.local.entities.ClassificationResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassificationResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: ClassificationResultEntity)

    @Query("""
        SELECT * FROM classification_results
        WHERE experimentId = :experimentId
        ORDER BY completedAt ASC
    """)
    fun getByExperiment(
        experimentId: String
    ): Flow<List<ClassificationResultEntity>>

    // NUEVA
    @Query("""
        SELECT * FROM classification_results
        WHERE albumId = :albumId
        ORDER BY completedAt DESC
    """)
    suspend fun getResultsForAlbum(albumId: String): List<ClassificationResultEntity>

    @Query("""
        SELECT * FROM classification_results
        WHERE taskId = :taskId
        LIMIT 1
    """)
    suspend fun getByTask(taskId: String): ClassificationResultEntity?

    @Query("""
        SELECT COUNT(*) FROM classification_results
        WHERE experimentId = :experimentId
    """)
    suspend fun countByExperiment(experimentId: String): Int


}

