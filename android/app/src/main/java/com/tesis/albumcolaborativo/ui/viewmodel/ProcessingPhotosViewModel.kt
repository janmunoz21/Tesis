
package com.tesis.albumcolaborativo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.albumcolaborativo.data.local.AppDatabase
import com.tesis.albumcolaborativo.data.local.entities.TaskQueueEntity
import com.tesis.albumcolaborativo.data.repository.TaskQueueRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
            0f
        } else {
            completedTasks.toFloat() / totalTasks
        }

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val isComplete: Boolean
        get() = totalTasks > 0 && completedTasks == totalTasks
}

class ProcessingPhotosViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = TaskQueueRepository(
        AppDatabase.getDatabase(application).taskQueueDao()
    )

    private val _uiState = MutableStateFlow(ProcessingPhotosUiState())
    val uiState: StateFlow<ProcessingPhotosUiState> =
        _uiState.asStateFlow()

    private var observationJob: Job? = null
    private var observedAlbumId: String? = null

    fun observeAlbum(albumId: String) {
        if (observedAlbumId == albumId && observationJob?.isActive == true) {
            return
        }

        observationJob?.cancel()
        observedAlbumId = albumId

        observationJob = viewModelScope.launch {
            repository.getTasksByAlbum(albumId).collect { tasks ->

                val completedStatuses = setOf(
                    "COMPLETADA",
                    "COMPLETADO",
                    "PROCESADA",
                    "PROCESADO"
                )

                val normalizedStatuses = tasks.map {
                    it.estadoSincronizacion.trim().uppercase()
                }

                val completed = normalizedStatuses.count {
                    it in completedStatuses
                }

                val pending = normalizedStatuses.count {
                    it == "PENDIENTE"
                }

                val assigned = normalizedStatuses.count {
                    it == "ASIGNADA"
                }

                _uiState.value = ProcessingPhotosUiState(
                    tasks = tasks,
                    totalTasks = tasks.size,
                    pendingTasks = pending,
                    assignedTasks = assigned,
                    completedTasks = completed,
                    otherTasks = tasks.size - pending - assigned - completed
                )
            }
        }
    }
}