package com.tesis.albumcolaborativo.ui.viewmodels

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.albumcolaborativo.data.local.AppDatabase
import com.tesis.albumcolaborativo.data.local.entities.TaskQueueEntity
import com.tesis.albumcolaborativo.data.repository.TaskQueueRepository
import com.tesis.albumcolaborativo.data.utils.ImagePreprocessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import com.tesis.albumcolaborativo.data.distribution.AssignedBlock
import com.tesis.albumcolaborativo.data.distribution.ProcessingNode
import com.tesis.albumcolaborativo.data.distribution.TaskDistributor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UploadPhotosUiState(
    val selectedUris: List<Uri> = emptyList(),
    val isProcessing: Boolean = false,
    val processedCount: Int = 0,
    val isBatchEnqueued: Boolean = false,
    val errorMessage: String? = null
)

class UploadPhotosViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UploadPhotosUiState())

    private val taskDistributor = TaskDistributor()
    val uiState: StateFlow<UploadPhotosUiState> = _uiState.asStateFlow()

    private val imagePreprocessor = ImagePreprocessor(application)

    // Base de datos local
    private val database = AppDatabase.getDatabase(application)

    // Repositorio de la cola de tareas
    private val taskQueueRepository =
        TaskQueueRepository(database.taskQueueDao())

    fun addPhotos(uris: List<Uri>) {
        // SRS CU-D-01: se descartan las fotos que superan el tamaño máximo permitido
        val (validUris, oversizedUris) = uris.partition { uri ->
            val size = getFileSize(uri)
            size == null || size <= MAX_PHOTO_SIZE_BYTES
        }

        _uiState.update { state ->
            // SRS CU-D-01: máximo 200 fotos por lote
            val updatedList = (state.selectedUris + validUris)
                .distinct()
                .take(200)

            state.copy(
                selectedUris = updatedList,
                errorMessage = if (oversizedUris.isEmpty()) null else oversizedMessage(oversizedUris.size)
            )
        }
    }

    private fun getFileSize(uri: Uri): Long? {
        return try {
            getApplication<Application>().contentResolver
                .query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst() && index >= 0 && !cursor.isNull(index)) {
                        cursor.getLong(index)
                    } else {
                        null
                    }
                }
        } catch (e: Exception) {
            // Si el proveedor no informa el tamaño, la foto no se descarta
            null
        }
    }

    private fun oversizedMessage(count: Int): String {
        val limitMb = MAX_PHOTO_SIZE_BYTES / (1024 * 1024)
        return if (count == 1) {
            "1 foto supera el límite de $limitMb MB y no se agregó."
        } else {
            "$count fotos superan el límite de $limitMb MB y no se agregaron."
        }
    }

    companion object {
        const val MAX_PHOTO_SIZE_BYTES = 20L * 1024 * 1024
    }

    fun removePhoto(uri: Uri) {
        _uiState.update { state ->
            val updatedList = state.selectedUris
                .filter { it != uri }

            state.copy(
                selectedUris = updatedList
            )
        }
    }

    fun processAndEnqueueBatch(
        albumId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {

            val currentUris = _uiState.value.selectedUris

            // Validar que haya fotos
            if (currentUris.isEmpty()) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Debes seleccionar al menos una foto."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isProcessing = true,
                    processedCount = 0,
                    errorMessage = null,
                    isBatchEnqueued = false
                )
            }

            val tasks = mutableListOf<TaskQueueEntity>()
            var successfulCount = 0

            currentUris.forEach { uri ->

                val processedFile =
                    imagePreprocessor.resizeImageTo224x224(uri)

                if (processedFile != null) {

                    val task = TaskQueueEntity(
                        taskId = UUID.randomUUID().toString(),
                        albumId = albumId,
                        tipoTarea = "PROCESAMIENTO_IMAGEN",
                        payloadJson = """
                            {
                                "imagePath": "${processedFile.absolutePath}",
                                "albumId": "$albumId"
                            }
                        """.trimIndent(),
                        estadoSincronizacion = "PENDIENTE"
                    )

                    tasks.add(task)

                    successfulCount++

                    _uiState.update {
                        it.copy(
                            processedCount = successfulCount
                        )
                    }
                }
            }

            // Si ninguna imagen pudo procesarse
            if (tasks.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "No se pudo procesar ninguna de las imágenes seleccionadas."
                    )
                }
                return@launch
            }

            // Guardar todas las tareas en Room
            try {

                taskQueueRepository.enqueueTasks(tasks)

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        isBatchEnqueued = true,
                        selectedUris = emptyList()
                    )
                }

                // CU-D-02 se conectará posteriormente aquí
                onSuccess()

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Error al guardar las tareas: ${e.message}"
                    )
                }
            }
        }
    }

    fun distributePendingTasks(
        albumId: String,
        nodes: List<ProcessingNode>,
        onDistributed: (List<AssignedBlock>) -> Unit
    ) {
        viewModelScope.launch {

            try {

                val blocks = withContext(Dispatchers.IO) {

                    val pendingTasks =
                        taskQueueRepository.getPendingTasks(albumId)

                    taskDistributor.distribute(
                        tasks = pendingTasks,
                        nodes = nodes
                    )
                }

                withContext(Dispatchers.IO) {

                    blocks.forEach { block ->

                        block.tasks.forEach { task ->

                            taskQueueRepository.updateTaskStatus(
                                taskId = task.taskId,
                                status = "ASIGNADA"
                            )
                        }
                    }
                }

                onDistributed(blocks)

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        errorMessage =
                            "Error al distribuir las tareas: ${e.message}"
                    )
                }
            }
        }
    }
}