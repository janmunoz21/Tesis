package com.tesis.albumcolaborativo.ui.viewmodels

import android.app.Application
import android.net.Uri
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
import com.tesis.albumcolaborativo.data.ml.MobileNetInferenceTest
import com.tesis.albumcolaborativo.data.local.entities.ClassificationResultEntity
import com.tesis.albumcolaborativo.data.local.entities.ExperimentMetricsEntity
import com.tesis.albumcolaborativo.data.ml.MobileNetClassifier
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import android.util.Log

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
        _uiState.update { state ->
            // SRS CU-D-01: máximo 200 fotos por lote
            val updatedList = (state.selectedUris + uris)
                .distinct()
                .take(200)

            state.copy(
                selectedUris = updatedList,
                errorMessage = null
            )
        }
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

            val experimentId = UUID.randomUUID().toString()
            val experimentStartMs = System.currentTimeMillis()
            val experimentStartNanos = System.nanoTime()

            val tasks = mutableListOf<TaskQueueEntity>()
            var successfulCount = 0
            var failedCount = 0
            var experimentSaved = false

            try {
                withContext(Dispatchers.IO) {
                    val resultDao = database.classificationResultDao()
                    val metricsDao = database.experimentMetricsDao()

                    metricsDao.insert(
                        ExperimentMetricsEntity(
                            experimentId = experimentId,
                            executionMode = "LOCAL",
                            deviceCount = 1,
                            totalImages = currentUris.size,
                            startedAt = experimentStartMs
                        )
                    )

                    experimentSaved = true

                    MobileNetClassifier(
                        getApplication<Application>()
                    ).use { classifier ->

                        currentUris.forEachIndexed { index, uri ->
                            try {
                                // 1. Medir el preprocesamiento de la imagen.
                                val preprocessingStart = System.nanoTime()

                                val processedFile =
                                    imagePreprocessor.resizeImageTo224x224(uri)

                                val preprocessingTimeMs =
                                    (System.nanoTime() - preprocessingStart) /
                                            1_000_000L

                                if (processedFile == null) {
                                    throw IllegalStateException(
                                        "No se pudo preprocesar la imagen."
                                    )
                                }

                                // 2. Clasificar y medir la inferencia.
                                val metrics =
                                    classifier.classifyWithMetrics(processedFile)

                                val predictions = metrics.topPredictions

                                if (predictions.isEmpty()) {
                                    throw IllegalStateException(
                                        "El modelo no devolvió predicciones."
                                    )
                                }

                                val taskId = UUID.randomUUID().toString()

                                // 3. Serializar las cinco predicciones.
                                val predictionsJson = JSONArray().apply {
                                    predictions.forEach { (classIndex, score) ->
                                        put(
                                            org.json.JSONObject().apply {
                                                put("classIndex", classIndex)
                                                put("label", classifier.getLabel(classIndex))
                                                put("score", score.toDouble())
                                            }
                                        )
                                    }
                                }.toString()

                                // 4. Guardar el resultado en Room.
                                resultDao.insert(
                                    ClassificationResultEntity(
                                        taskId = taskId,
                                        experimentId = experimentId,
                                        albumId = albumId,
                                        imagePath = processedFile.absolutePath,
                                        predictedClassIndex = predictions.first().first,
                                        predictedScore = predictions.first().second,
                                        topPredictionsJson = predictionsJson,
                                        preprocessingTimeMs = preprocessingTimeMs,
                                        tensorPreparationTimeMs =
                                            metrics.tensorPreparationTimeMs,
                                        inferenceTimeMs = metrics.inferenceTimeMs,
                                        totalProcessingTimeMs =
                                            preprocessingTimeMs +
                                                    metrics.totalClassificationTimeMs,
                                        deviceId = "LOCAL"
                                    )
                                )

                                // 5. Preparar la tarea para la cola.
                                val task = TaskQueueEntity(
                                    taskId = taskId,
                                    albumId = albumId,
                                    tipoTarea = "PROCESAMIENTO_IMAGEN",
                                    payloadJson = org.json.JSONObject().apply {
                                        put("imagePath", processedFile.absolutePath)
                                        put("albumId", albumId)
                                        put("experimentId", experimentId)
                                    }.toString(),
                                    estadoSincronizacion = "PENDIENTE"
                                )

                                tasks.add(task)
                                successfulCount++

                                Log.i(
                                    "CLASSIFICATION",
                                    "Imagen ${index + 1}: " +
                                            "clase=${predictions.first().first}, " +
                                            "preprocesamiento=${preprocessingTimeMs}ms, " +
                                            "inferencia=${metrics.inferenceTimeMs}ms, " +
                                            "total=${preprocessingTimeMs + metrics.totalClassificationTimeMs}ms"
                                )
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                failedCount++

                                Log.e(
                                    "CLASSIFICATION",
                                    "Error procesando imagen ${index + 1}",
                                    e
                                )
                            }

                            _uiState.update {
                                it.copy(processedCount = index + 1)
                            }
                        }
                    }

                    // 6. Guardar las tareas que sí se procesaron correctamente.
                    if (tasks.isNotEmpty()) {
                        taskQueueRepository.enqueueTasks(tasks)
                    }

                    // 7. Finalizar el experimento.
                    metricsDao.finishExperiment(
                        experimentId = experimentId,
                        completedAt = System.currentTimeMillis(),
                        durationMs =
                            (System.nanoTime() - experimentStartNanos) /
                                    1_000_000L,
                        successfulTasks = successfulCount,
                        failedTasks = failedCount
                    )
                }

                if (tasks.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage =
                                "No se pudo clasificar ninguna imagen. Revisa Logcat."
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        isBatchEnqueued = true,
                        selectedUris = emptyList(),
                        errorMessage = if (failedCount > 0) {
                            "Se clasificaron $successfulCount imágenes; " +
                                    "$failedCount fallaron."
                        } else {
                            null
                        }
                    )
                }

                onSuccess()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CLASSIFICATION", "Error en el experimento", e)

                // Intentar cerrar el registro del experimento si hubo un error.
                if (experimentSaved) {
                    try {
                        withContext(Dispatchers.IO) {
                            database.experimentMetricsDao().finishExperiment(
                                experimentId = experimentId,
                                completedAt = System.currentTimeMillis(),
                                durationMs =
                                    (System.nanoTime() - experimentStartNanos) /
                                            1_000_000L,
                                successfulTasks = successfulCount,
                                failedTasks = currentUris.size - successfulCount
                            )
                        }
                    } catch (updateError: Exception) {
                        Log.e(
                            "CLASSIFICATION",
                            "No se pudo finalizar el registro experimental",
                            updateError
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage =
                            "Error durante el procesamiento: ${e.message}"
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