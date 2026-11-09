package com.tesis.albumcolaborativo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.albumcolaborativo.data.local.AppDatabase
import com.tesis.albumcolaborativo.data.local.entities.TaskQueueEntity
import com.tesis.albumcolaborativo.data.repository.TaskQueueRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProcessingPhotosUiState(
    val tasks: List<TaskQueueEntity> = emptyList(),
    val totalTasks: Int = 0,
    val pendingTasks: Int = 0,
    val assignedTasks: Int = 0,
    val completedTasks: Int = 0,
    val otherTasks: Int = 0
) {
    val progress: Float
        get() = if (totalTasks == 0) {
            1f
        } else {
            (completedTasks.toFloat() / totalTasks.toFloat()).coerceIn(0f, 1f)
        }

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val isComplete: Boolean
        get() = totalTasks == 0 || completedTasks >= totalTasks
}

class ProcessingPhotosViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = TaskQueueRepository(database.taskQueueDao())

    private val _uiState = MutableStateFlow(ProcessingPhotosUiState())
    val uiState: StateFlow<ProcessingPhotosUiState> = _uiState.asStateFlow()

    private var observationJob: Job? = null
    private var observedAlbumId: String? = null

    fun observeAlbum(albumId: String) {
        val cleanAlbumId = albumId.replace("\"", "").trim()
        if (observedAlbumId == cleanAlbumId && observationJob?.isActive == true) {
            return
        }

        observationJob?.cancel()
        observedAlbumId = cleanAlbumId

        observationJob = viewModelScope.launch {
            repository.getTasksByAlbum(cleanAlbumId).collect { tasks ->

                val completedStatuses = setOf(
                    "COMPLETADA",
                    "COMPLETADO",
                    "PROCESADA",
                    "PROCESADO"
                )

                val normalizedStatuses = tasks.map {
                    it.estadoSincronizacion.trim().uppercase()
                }

                var completed = normalizedStatuses.count {
                    it in completedStatuses
                }

                val pending = normalizedStatuses.count {
                    it == "PENDIENTE"
                }

                val assigned = normalizedStatuses.count {
                    it == "ASIGNADA"
                }

                var totalCount = tasks.size

                // Fallback: Si no hay tareas en cola o el conteo no coincide, consultar classification_results
                if (totalCount == 0 || completed == 0) {
                    withContext(Dispatchers.IO) {
                        val classificationResults = database.classificationResultDao().getResultsForAlbum(cleanAlbumId)
                        if (classificationResults.isNotEmpty()) {
                            completed = classificationResults.size
                            totalCount = maxOf(totalCount, classificationResults.size)
                        } else {
                            val allResults = database.classificationResultDao().getAllResults()
                            if (allResults.isNotEmpty()) {
                                completed = allResults.size
                                totalCount = allResults.size
                            }
                        }
                    }
                }

                _uiState.value = ProcessingPhotosUiState(
                    tasks = tasks,
                    totalTasks = totalCount,
                    pendingTasks = pending,
                    assignedTasks = assigned,
                    completedTasks = completed,
                    otherTasks = maxOf(0, totalCount - pending - assigned - completed)
                )
            }
        }
    }
}
