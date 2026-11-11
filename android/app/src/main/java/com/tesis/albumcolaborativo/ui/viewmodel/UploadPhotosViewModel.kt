package com.tesis.albumcolaborativo.ui.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.albumcolaborativo.data.distribution.AssignedBlock
import com.tesis.albumcolaborativo.data.distribution.ProcessingNode
import com.tesis.albumcolaborativo.data.distribution.TaskDistributor
import com.tesis.albumcolaborativo.data.local.AppDatabase
import com.tesis.albumcolaborativo.data.local.entities.ClassificationResultEntity
import com.tesis.albumcolaborativo.data.local.entities.ExperimentMetricsEntity
import com.tesis.albumcolaborativo.data.local.entities.TaskQueueEntity
import com.tesis.albumcolaborativo.data.ml.MobileNetClassifier
import com.tesis.albumcolaborativo.data.network.NetworkResult
import com.tesis.albumcolaborativo.data.network.p2p.GrpcP2PClient
import com.tesis.albumcolaborativo.data.network.p2p.P2PManager
import com.tesis.albumcolaborativo.data.repository.AlbumRepository
import com.tesis.albumcolaborativo.data.repository.TaskQueueRepository
import com.tesis.albumcolaborativo.data.utils.ImagePreprocessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.util.UUID

data class UploadPhotosUiState(
    val selectedUris: List<Uri> = emptyList(),
    val isProcessing: Boolean = false,
    val processedCount: Int = 0,
    val pendingPhotosCount: Int = 0,
    val isBatchEnqueued: Boolean = false,
    val errorMessage: String? = null
)

class UploadPhotosViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UploadPhotosUiState())

    private val taskDistributor = TaskDistributor()
    val uiState: StateFlow<UploadPhotosUiState> = _uiState.asStateFlow()

    private val imagePreprocessor = ImagePreprocessor(application)
    private val database = AppDatabase.getDatabase(application)
    private val taskQueueRepository = TaskQueueRepository(database.taskQueueDao())
    private val albumRepository = AlbumRepository(application)
    private val p2pClient = GrpcP2PClient()

    fun addPhotos(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val cachedUris = uris.mapNotNull { uri ->
                try {
                    if (uri.scheme == "file") {
                        uri
                    } else {
                        val inputStream = app.contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            val cacheFile = File(app.cacheDir, "picked_${UUID.randomUUID()}.jpg")
                            cacheFile.outputStream().use { output ->
                                inputStream.copyTo(output)
                            }
                            Uri.fromFile(cacheFile)
                        } else null
                    }
                } catch (e: Exception) {
                    Log.e("UploadPhotosViewModel", "Error copiando URI $uri: ${e.message}")
                    null
                }
            }

            _uiState.update { state ->
                val updatedList = (state.selectedUris + cachedUris)
                    .distinct()
                    .take(200)

                state.copy(
                    selectedUris = updatedList,
                    errorMessage = null
                )
            }
        }
    }

    fun removePhoto(uri: Uri) {
        _uiState.update { state ->
            val updatedList = state.selectedUris.filter { it != uri }
            state.copy(selectedUris = updatedList)
        }
    }

    /**
     * PASO 1: Guarda las fotos seleccionadas en la cola del álbum compartido y sincroniza con el backend.
     */
    fun guardarFotosEnAlbum(albumId: String, isCreator: Boolean = false, onExito: () -> Unit) {
        viewModelScope.launch {
            val cleanAlbumId = albumId.replace("\"", "").trim()
            val currentUris = _uiState.value.selectedUris

            if (currentUris.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "Selecciona al menos una foto.") }
                return@launch
            }

            _uiState.update { it.copy(isProcessing = true) }

            withContext(Dispatchers.IO) {
                try {
                    val tasksToEnqueue = mutableListOf<TaskQueueEntity>()
                    currentUris.forEach { uri ->
                        val preparedFile = imagePreprocessor.resizeImageTo224x224(uri)
                        if (preparedFile != null) {
                            val taskId = UUID.randomUUID().toString()
                            val task = TaskQueueEntity(
                                taskId = taskId,
                                albumId = cleanAlbumId,
                                tipoTarea = "PROCESAMIENTO_IMAGEN",
                                payloadJson = org.json.JSONObject().apply {
                                    put("localPath", preparedFile.absolutePath)
                                    put("imagePath", preparedFile.absolutePath)
                                    put("albumId", cleanAlbumId)
                                    put("PENDIENTE", true)
                                }.toString(),
                                estadoSincronizacion = "PENDIENTE"
                            )
                            tasksToEnqueue.add(task)
                        }
                    }

                    if (tasksToEnqueue.isNotEmpty()) {
                        taskQueueRepository.enqueueTasks(tasksToEnqueue)

                        // Registrar metadatos de fotos subidas en el backend PostgreSQL
                        try {
                            val fotosDto = tasksToEnqueue.map { task ->
                                com.tesis.albumcolaborativo.data.model.FotoResultDto(
                                    taskId = task.taskId,
                                    propietarioId = "00000000-0000-0000-0000-000000000000",
                                    etiquetaConsolidada = "PENDIENTE",
                                    porcentajeConfianza = 0.5f
                                )
                            }
                            val request = com.tesis.albumcolaborativo.data.model.SyncAlbumResultsDto(
                                albumId = cleanAlbumId,
                                fotos = fotosDto
                            )
                            albumRepository.sincronizarResultados(cleanAlbumId, request)
                        } catch (e: Exception) {
                            Log.d("UploadPhotosViewModel", "Error sincronizando con backend REST: ${e.message}")
                        }

                        // Si somos el INVITADO (!isCreator), enviar las fotos al CREADOR/Coordinador vía P2P
                        if (!isCreator) {
                            val creatorIp = P2PManager.getCreatorAddress()
                            val creatorPort = P2PManager.getCreatorPort()
                            if (!creatorIp.isNullOrBlank()) {
                                Log.i("UploadPhotosViewModel", "Invitado enviando ${tasksToEnqueue.size} fotos directamente al Creador $creatorIp:$creatorPort")
                                p2pClient.sendTaskBlockToEnqueue(creatorIp, creatorPort, cleanAlbumId, tasksToEnqueue)
                            } else {
                                val activeNodes = P2PManager.getDiscoveredNodes().value.filter { node ->
                                    !node.isLocal && node.available && p2pClient.pingNode(node.address, node.port)
                                }
                                activeNodes.forEach { node ->
                                    Log.i("UploadPhotosViewModel", "Invitado enviando ${tasksToEnqueue.size} fotos al nodo remoto ${node.address}:${node.port}")
                                    p2pClient.sendTaskBlockToEnqueue(node.address, node.port, cleanAlbumId, tasksToEnqueue)
                                }
                            }
                        }
                    }

                    val allPending = taskQueueRepository.getPendingTasks(cleanAlbumId).size

                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            selectedUris = emptyList(),
                            pendingPhotosCount = allPending,
                            errorMessage = null
                        )
                    }

                    withContext(Dispatchers.Main) { onExito() }
                } catch (e: Exception) {
                    Log.e("UploadPhotosViewModel", "Error guardando fotos: ${e.message}")
                    _uiState.update { it.copy(isProcessing = false, errorMessage = "Error guardando fotos: ${e.message}") }
                }
            }
        }
    }

    /**
     * PASO 2: Recopila TODAS las fotos acumuladas en el álbum (de creador e invitados), las divide con TaskDistributor y las procesa en paralelo.
     */
    fun processAndEnqueueBatch(
        albumId: String,
        albumName: String = "",
        onBlockAssigned: (localBlockCount: Int) -> Unit
    ) {
        viewModelScope.launch {
            val cleanAlbumId = albumId.replace("\"", "").trim()

            _uiState.update { it.copy(isProcessing = true, processedCount = 0, errorMessage = null) }

            withContext(Dispatchers.IO) {
                try {
                    // 1. Descargar metadatos de fotos registradas en el backend PostgreSQL por el Creador o Invitados
                    try {
                        val backendRes = albumRepository.obtenerMetadatosAlbum(cleanAlbumId)
                        if (backendRes is NetworkResult.Success) {
                            val backendMetadatos = backendRes.data
                            if (backendMetadatos.isNotEmpty()) {
                                val existingPending = taskQueueRepository.getPendingTasks(cleanAlbumId)
                                val existingTaskIds = existingPending.map { it.taskId }.toSet()

                                val newBackendTasks = backendMetadatos.filter { it.id !in existingTaskIds }.map { meta ->
                                    TaskQueueEntity(
                                        taskId = meta.id,
                                        albumId = cleanAlbumId,
                                        tipoTarea = "PROCESAMIENTO_IMAGEN",
                                        payloadJson = org.json.JSONObject().apply {
                                            put("imagePath", meta.rutaArchivo)
                                            put("albumId", cleanAlbumId)
                                            put("PENDIENTE", true)
                                        }.toString(),
                                        estadoSincronizacion = "PENDIENTE"
                                    )
                                }
                                if (newBackendTasks.isNotEmpty()) {
                                    taskQueueRepository.enqueueTasks(newBackendTasks)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("UploadPhotosViewModel", "No se pudieron obtener metadatos backend: ${e.message}")
                    }

                    // 2. Obtener TODAS las fotos pendientes acumuladas en este álbum
                    var allPendingTasks = taskQueueRepository.getPendingTasks(cleanAlbumId)

                    // Si no hay registradas en la cola pero hay seleccionadas en pantalla
                    if (allPendingTasks.isEmpty() && _uiState.value.selectedUris.isNotEmpty()) {
                        val localUris = _uiState.value.selectedUris
                        localUris.forEach { uri ->
                            val preparedFile = imagePreprocessor.resizeImageTo224x224(uri)
                            if (preparedFile != null) {
                                val taskId = UUID.randomUUID().toString()
                                taskQueueRepository.enqueueTasks(listOf(
                                    TaskQueueEntity(
                                        taskId = taskId,
                                        albumId = cleanAlbumId,
                                        tipoTarea = "PROCESAMIENTO_IMAGEN",
                                        payloadJson = org.json.JSONObject().apply {
                                            put("localPath", preparedFile.absolutePath)
                                            put("imagePath", preparedFile.absolutePath)
                                            put("albumId", cleanAlbumId)
                                            put("PENDIENTE", true)
                                        }.toString(),
                                        estadoSincronizacion = "PENDIENTE"
                                    )
                                ))
                            }
                        }
                        allPendingTasks = taskQueueRepository.getPendingTasks(cleanAlbumId)
                    }

                    if (allPendingTasks.isEmpty()) {
                        _uiState.update { it.copy(isProcessing = false, errorMessage = "No hay fotos en el álbum para procesar.") }
                        return@withContext
                    }

                    val experimentId = UUID.randomUUID().toString()
                    val experimentStartMs = System.currentTimeMillis()
                    val experimentStartNanos = System.nanoTime()

                    val resultDao = database.classificationResultDao()
                    val metricsDao = database.experimentMetricsDao()

                    // Obtener todos los nodos P2P descubiertos o agregados manualmente (excluyendo la IP local propia)
                    val discovered = P2PManager.getDiscoveredNodes().value.filter { !it.isLocal }
                    val creatorIp = P2PManager.getCreatorAddress()
                    val creatorPort = P2PManager.getCreatorPort()

                    val candidateNodes = if (!creatorIp.isNullOrBlank() && discovered.none { it.address == creatorIp }) {
                        discovered + ProcessingNode(
                            nodeId = "Creator_$creatorIp",
                            address = creatorIp,
                            port = creatorPort,
                            available = true,
                            deviceName = "Creador $creatorIp",
                            isLocal = false
                        )
                    } else {
                        discovered
                    }

                    // Probar ping de salud, asegurando incluir SOLAMENTE nodos remotos que respondan activamente por HTTP
                    val activeP2PNodes = candidateNodes.filter { node ->
                        node.available && p2pClient.pingNode(node.address, node.port)
                    }.distinctBy { it.address }
                    val localNode = ProcessingNode(nodeId = "LOCAL", address = "127.0.0.1", port = 0, available = true)

                    metricsDao.insert(
                        ExperimentMetricsEntity(
                            experimentId = cleanAlbumId,
                            executionMode = if (activeP2PNodes.isEmpty()) "LOCAL" else "P2P_DISTRIBUTED",
                            deviceCount = activeP2PNodes.size + 1,
                            totalImages = allPendingTasks.size,
                            startedAt = experimentStartMs
                        )
                    )

                    // Notificar inicio P2P a todos los nodos remotos activos
                    val finalAlbumName = albumName.ifBlank { "Álbum Colaborativo" }
                    activeP2PNodes.forEach { node ->
                        p2pClient.sendStartProcessingSignal(
                            targetAddress = node.address,
                            targetPort = node.port,
                            albumId = cleanAlbumId,
                            albumName = finalAlbumName,
                            totalTasks = allPendingTasks.size
                        )
                    }

                    // Fraccionar las tareas entre todos los nodos Wi-Fi activos
                    val blocks = taskDistributor.distribute(allPendingTasks, activeP2PNodes + localNode)

                    var localAssignedCount = allPendingTasks.size

                    blocks.forEach { block ->
                        if (block.node.nodeId == "LOCAL") {
                            localAssignedCount = block.tasks.size
                            P2PManager.setAssignedBlockCount(cleanAlbumId, localAssignedCount)
                            // Procesar bloque local
                            MobileNetClassifier(getApplication()).use { classifier ->
                                block.tasks.forEachIndexed { index, task ->
                                    try {
                                        val payload = org.json.JSONObject(task.payloadJson)
                                        val filePath = payload.getString("imagePath")
                                        val file = File(filePath)

                                        if (file.exists()) {
                                            val metrics = classifier.classifyWithMetrics(file)
                                            val predictionsJson = JSONArray().apply {
                                                metrics.topPredictions.forEach { (classIdx, score) ->
                                                    put(org.json.JSONObject().apply {
                                                        put("classIndex", classIdx)
                                                        put("label", classifier.getLabel(classIdx))
                                                        put("score", score.toDouble())
                                                    })
                                                }
                                            }.toString()

                                            resultDao.insert(
                                                ClassificationResultEntity(
                                                    taskId = task.taskId,
                                                    experimentId = experimentId,
                                                    albumId = cleanAlbumId,
                                                    imagePath = file.absolutePath,
                                                    predictedClassIndex = metrics.topPredictions.first().first,
                                                    predictedScore = metrics.topPredictions.first().second,
                                                    topPredictionsJson = predictionsJson,
                                                    preprocessingTimeMs = 0L,
                                                    tensorPreparationTimeMs = metrics.tensorPreparationTimeMs,
                                                    inferenceTimeMs = metrics.inferenceTimeMs,
                                                    totalProcessingTimeMs = metrics.totalClassificationTimeMs,
                                                    deviceId = "LOCAL"
                                                )
                                            )
                                        }
                                        taskQueueRepository.updateTaskStatus(task.taskId, "PROCESADA")
                                    } catch (e: Exception) {
                                        Log.e("CLASSIFICATION", "Error en clasificacion local: ${e.message}")
                                    }
                                    _uiState.update { it.copy(processedCount = index + 1) }
                                }
                            }
                        } else {
                            // Enviar bloque remoto a nodo P2P
                            Log.i("P2P_DISTRIBUTION", "Enviando bloque de ${block.tasks.size} fotos a ${block.node.address}:${block.node.port}")
                            val p2pRes = p2pClient.sendTaskBlockWithResults(
                                targetAddress = block.node.address,
                                targetPort = block.node.port,
                                albumId = cleanAlbumId,
                                tasks = block.tasks
                            )
                            if (p2pRes != null && p2pRes.accepted) {
                                p2pRes.results.forEach { remoteRes ->
                                    val matchingTask = block.tasks.find { it.taskId == remoteRes.taskId }
                                    var realPath = if (matchingTask != null) {
                                        val payloadObj = org.json.JSONObject(matchingTask.payloadJson)
                                        payloadObj.optString("imagePath", payloadObj.optString("localPath", ""))
                                    } else ""

                                    if (realPath.isBlank() || !File(realPath).exists()) {
                                        val cacheFile = File(getApplication<Application>().cacheDir, "p2p_res_${remoteRes.taskId}.jpg")
                                        if (!remoteRes.imageBytesBase64.isNullOrEmpty()) {
                                            try {
                                                val b = android.util.Base64.decode(remoteRes.imageBytesBase64, android.util.Base64.DEFAULT)
                                                cacheFile.writeBytes(b)
                                                realPath = cacheFile.absolutePath
                                            } catch (_: Exception) {}
                                        }
                                        if (realPath.isBlank()) realPath = cacheFile.absolutePath
                                    }

                                    resultDao.insert(
                                        ClassificationResultEntity(
                                            taskId = remoteRes.taskId,
                                            experimentId = cleanAlbumId,
                                            albumId = cleanAlbumId,
                                            imagePath = realPath,
                                            predictedClassIndex = remoteRes.predictedClassIndex,
                                            predictedScore = remoteRes.predictedScore,
                                            topPredictionsJson = remoteRes.topPredictionsJson,
                                            preprocessingTimeMs = 0L,
                                            tensorPreparationTimeMs = 0L,
                                            inferenceTimeMs = remoteRes.inferenceTimeMs,
                                            totalProcessingTimeMs = remoteRes.inferenceTimeMs,
                                            deviceId = block.node.deviceName
                                        )
                                    )
                                }
                                block.tasks.forEach { taskQueueRepository.updateTaskStatus(it.taskId, "PROCESADA") }
                            } else {
                                // FALLBACK: El nodo remoto no respondió. Procesar estas tareas en LOCAL para no perderlas
                                Log.w("P2P_DISTRIBUTION", "Nodo remoto ${block.node.address} no respondió. Ejecutando ${block.tasks.size} tareas en LOCAL como respaldo.")
                                MobileNetClassifier(getApplication()).use { classifier ->
                                    block.tasks.forEachIndexed { _, task ->
                                        try {
                                            val payload = org.json.JSONObject(task.payloadJson)
                                            val filePath = payload.getString("imagePath")
                                            val file = File(filePath)

                                            if (file.exists()) {
                                                val metrics = classifier.classifyWithMetrics(file)
                                                val predictionsJson = JSONArray().apply {
                                                    metrics.topPredictions.forEach { (classIdx, score) ->
                                                        put(org.json.JSONObject().apply {
                                                            put("classIndex", classIdx)
                                                            put("label", classifier.getLabel(classIdx))
                                                            put("score", score.toDouble())
                                                        })
                                                    }
                                                }.toString()

                                                resultDao.insert(
                                                    ClassificationResultEntity(
                                                        taskId = task.taskId,
                                                        experimentId = cleanAlbumId,
                                                        albumId = cleanAlbumId,
                                                        imagePath = file.absolutePath,
                                                        predictedClassIndex = metrics.topPredictions.first().first,
                                                        predictedScore = metrics.topPredictions.first().second,
                                                        topPredictionsJson = predictionsJson,
                                                        preprocessingTimeMs = 0L,
                                                        tensorPreparationTimeMs = metrics.tensorPreparationTimeMs,
                                                        inferenceTimeMs = metrics.inferenceTimeMs,
                                                        totalProcessingTimeMs = metrics.totalClassificationTimeMs,
                                                        deviceId = "LOCAL_RESPALDO"
                                                    )
                                                )
                                            }
                                            taskQueueRepository.updateTaskStatus(task.taskId, "PROCESADA")
                                        } catch (e: Exception) {
                                            Log.e("CLASSIFICATION", "Error en clasificacion local de respaldo: ${e.message}")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sincronizar resultados consolidados con backend PostgreSQL
                    sincronizarLoteConBackend(albumId, allPendingTasks)

                    // Transmitir todos los resultados clasificados consolidados a todos los invitados vía P2P
                    val resultadosConsolidados = resultDao.getResultsForAlbum(cleanAlbumId)
                    activeP2PNodes.forEach { node ->
                        try {
                            p2pClient.broadcastCompletedResults(
                                targetAddress = node.address,
                                targetPort = node.port,
                                albumId = cleanAlbumId,
                                results = resultadosConsolidados
                            )
                        } catch (e: Exception) {
                            Log.e("P2P_DISTRIBUTION", "Error transmitiendo resultados P2P a ${node.address}: ${e.message}")
                        }
                    }

                    metricsDao.finishExperiment(
                        experimentId = cleanAlbumId,
                        completedAt = System.currentTimeMillis(),
                        durationMs = (System.nanoTime() - experimentStartNanos) / 1_000_000L,
                        successfulTasks = allPendingTasks.size,
                        failedTasks = 0
                    )

                    val finalAssignedCount = if (localAssignedCount > 0) localAssignedCount else allPendingTasks.size

                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            selectedUris = emptyList(),
                            errorMessage = null
                        )
                    }

                    withContext(Dispatchers.Main) { onBlockAssigned(finalAssignedCount) }

                } catch (e: Exception) {
                    Log.e("CLASSIFICATION", "Error durante el procesamiento distribuido: ${e.message}")
                    _uiState.update { it.copy(isProcessing = false, errorMessage = "Error: ${e.message}") }
                }
            }
        }
    }

    private suspend fun sincronizarLoteConBackend(albumId: String, tasks: List<TaskQueueEntity>) {
        withContext(Dispatchers.IO) {
            try {
                val resultDao = database.classificationResultDao()
                val cleanAlbumId = albumId.replace("\"", "").trim()

                val resultadosLocales = resultDao.getResultsForAlbum(cleanAlbumId)
                if (resultadosLocales.isEmpty()) return@withContext

                val fotosDto = resultadosLocales.map { result ->
                    com.tesis.albumcolaborativo.data.model.FotoResultDto(
                        taskId = result.taskId,
                        propietarioId = "00000000-0000-0000-0000-000000000000",
                        etiquetaConsolidada = extractLabelFromJson(result.topPredictionsJson),
                        porcentajeConfianza = result.predictedScore
                    )
                }

                val request = com.tesis.albumcolaborativo.data.model.SyncAlbumResultsDto(
                    albumId = cleanAlbumId,
                    fotos = fotosDto
                )

                albumRepository.sincronizarResultados(cleanAlbumId, request)
            } catch (e: Exception) {
                Log.e("SYNC_BACKEND", "Error sincronizando con backend: ${e.message}")
            }
        }
    }

    private fun extractLabelFromJson(jsonStr: String): String {
        return try {
            val jsonArray = JSONArray(jsonStr)
            if (jsonArray.length() > 0) {
                jsonArray.getJSONObject(0).optString("label", "Sin categoría")
            } else "Sin categoría"
        } catch (_: Exception) {
            "Sin categoría"
        }
    }
}
