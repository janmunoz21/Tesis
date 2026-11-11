package com.tesis.albumcolaborativo.data.network.p2p

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.tesis.albumcolaborativo.data.distribution.grpc.TaskBlockRequest
import com.tesis.albumcolaborativo.data.distribution.grpc.TaskBlockResponse
import com.tesis.albumcolaborativo.data.local.AppDatabase
import com.tesis.albumcolaborativo.data.local.entities.ClassificationResultEntity
import com.tesis.albumcolaborativo.data.local.entities.TaskQueueEntity
import com.tesis.albumcolaborativo.data.ml.MobileNetClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

class GrpcP2PService(private val context: Context, private val port: Int = 50051) {

    companion object {
        private const val TAG = "GrpcP2PService"
    }

    @Volatile
    private var serverSocket: ServerSocket? = null
    @Volatile
    private var isRunning = false
    @Volatile
    private var activePort = port
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val gson = com.google.gson.GsonBuilder().setLenient().create()
    private val database = AppDatabase.getDatabase(context)

    fun getActivePort(): Int = activePort

    fun startServer() {
        synchronized(this) {
            val currentSocket = serverSocket
            if (currentSocket != null && currentSocket.isBound && !currentSocket.isClosed) {
                return
            }
            if (isRunning) return

            var ss: ServerSocket? = null
            var boundPort = port

            for (p in port..port + 5) {
                try {
                    val candidate = ServerSocket().apply {
                        reuseAddress = true
                        bind(InetSocketAddress(p))
                    }
                    ss = candidate
                    boundPort = p
                    this@GrpcP2PService.activePort = p
                    break
                } catch (_: Exception) {}
            }

            if (ss == null) {
                isRunning = false
                Log.e(TAG, "No se pudo enlazar servidor P2P en puertos $port..${port+5}")
                return
            }

            serverSocket = ss
            isRunning = true
            Log.i(TAG, "Servidor P2P iniciado exitosamente y escuchando en puerto $boundPort")
        }

        scope.launch {
            try {
                val ss = serverSocket ?: return@launch
                while (isRunning) {
                    val clientSocket = ss.accept() ?: break
                    handleClient(clientSocket)
                }
            } catch (e: Exception) {
                synchronized(this@GrpcP2PService) {
                    isRunning = false
                }
                Log.e(TAG, "Servidor P2P error: ${e.message}")
            }
        }
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                socket.use { client ->
                    val input = client.getInputStream()
                    val headerBytes = ByteArray(4096)
                    var headerLen = 0

                    // Leer bytes del encabezado HTTP hasta encontrar \r\n\r\n
                    while (headerLen < 4096) {
                        val b = input.read()
                        if (b == -1) break
                        headerBytes[headerLen++] = b.toByte()
                        if (headerLen >= 4 &&
                            headerBytes[headerLen - 4] == 13.toByte() &&
                            headerBytes[headerLen - 3] == 10.toByte() &&
                            headerBytes[headerLen - 2] == 13.toByte() &&
                            headerBytes[headerLen - 1] == 10.toByte()) {
                            break
                        }
                    }

                    val headerText = String(headerBytes, 0, headerLen, Charsets.UTF_8)

                    if (headerText.startsWith("GET /p2p/ping", ignoreCase = true)) {
                        val jsonResponse = gson.toJson(mapOf("status" to "OK", "pong" to true))
                        writeJsonResponse(client, jsonResponse)
                        return@use
                    }

                    // Extraer Content-Length de los encabezados HTTP
                    val contentLengthLine = headerText.lines().find { it.startsWith("Content-Length:", ignoreCase = true) }
                    val contentLength = contentLengthLine?.substringAfter(":")?.trim()?.toIntOrNull() ?: 0

                    var bodyJson = ""
                    if (contentLength > 0) {
                        val bodyBytes = ByteArray(contentLength)
                        var totalRead = 0
                        while (totalRead < contentLength) {
                            val read = input.read(bodyBytes, totalRead, contentLength - totalRead)
                            if (read <= 0) break
                            totalRead += read
                        }
                        bodyJson = String(bodyBytes, 0, totalRead, Charsets.UTF_8)
                    }

                    if (bodyJson.contains("\"INVITE\"")) {
                        val map = gson.fromJson(bodyJson, Map::class.java)
                        val albumId = map["albumId"] as? String ?: UUID.randomUUID().toString()
                        val albumName = map["albumName"] as? String ?: "Nuevo Álbum"
                        val inviterName = map["inviterName"] as? String ?: "Un usuario"
                        val inviterIpInBody = map["inviterAddress"] as? String ?: ""
                        val inviterPortInBody = (map["inviterPort"] as? Number)?.toInt() ?: 50051
                        val socketIp = client.inetAddress?.hostAddress ?: ""
                        val finalCreatorIp = if (inviterIpInBody.isNotBlank()) inviterIpInBody else socketIp

                        Log.i(TAG, "Invitación P2P recibida de $inviterName ($finalCreatorIp:$inviterPortInBody) para '$albumName' (ID: $albumId)")

                        P2PManager.onInviteReceived(albumId, albumName, inviterName, finalCreatorIp, inviterPortInBody)

                        val jsonResponse = gson.toJson(mapOf("status" to "OK", "message" to "Invitación P2P recibida"))
                        writeJsonResponse(client, jsonResponse)
                    } else if (bodyJson.contains("\"START_PROCESSING\"")) {
                        val map = gson.fromJson(bodyJson, Map::class.java)
                        val albumId = map["albumId"] as? String ?: ""
                        val albumName = map["albumName"] as? String ?: "Álbum Colaborativo"
                        val totalTasks = (map["totalTasks"] as? Number)?.toInt() ?: 1

                        Log.i(TAG, "Señal START_PROCESSING recibida para álbum $albumId con $totalTasks tareas")
                        P2PManager.onProcessingSignalReceived(albumId, albumName, totalTasks)

                        val jsonResponse = gson.toJson(mapOf("status" to "OK", "message" to "Procesamiento iniciado"))
                        writeJsonResponse(client, jsonResponse)
                    } else if (bodyJson.contains("\"SYNC_RESULTS\"")) {
                        try {
                            val map = gson.fromJson(bodyJson, Map::class.java)
                            val albumId = map["albumId"] as? String ?: ""
                            val resultsArray = map["results"] as? List<*> ?: emptyList<Any>()

                            Log.i(TAG, "Recibidos ${resultsArray.size} resultados consolidados P2P para álbum $albumId")

                            val resultDao = database.classificationResultDao()
                            val taskDao = database.taskQueueDao()

                            for (item in resultsArray) {
                                val itemJson = gson.toJson(item)
                                val remoteRes = gson.fromJson(itemJson, com.tesis.albumcolaborativo.data.distribution.grpc.RemoteTaskResultDto::class.java)

                                var realPath = ""
                                if (!remoteRes.imageBytesBase64.isNullOrEmpty()) {
                                    try {
                                        val cacheFile = File(context.cacheDir, "p2p_synced_${remoteRes.taskId}.jpg")
                                        val b = android.util.Base64.decode(remoteRes.imageBytesBase64, android.util.Base64.DEFAULT)
                                        cacheFile.writeBytes(b)
                                        realPath = cacheFile.absolutePath
                                    } catch (_: Exception) {}
                                }

                                val entity = ClassificationResultEntity(
                                    taskId = remoteRes.taskId,
                                    experimentId = "P2P_SYNCED",
                                    albumId = albumId,
                                    imagePath = realPath,
                                    predictedClassIndex = remoteRes.predictedClassIndex,
                                    predictedScore = remoteRes.predictedScore,
                                    topPredictionsJson = remoteRes.topPredictionsJson,
                                    preprocessingTimeMs = 0L,
                                    tensorPreparationTimeMs = 0L,
                                    inferenceTimeMs = remoteRes.inferenceTimeMs,
                                    totalProcessingTimeMs = remoteRes.inferenceTimeMs,
                                    deviceId = "CREATOR_P2P"
                                )
                                resultDao.insert(entity)

                                taskDao.insert(
                                    TaskQueueEntity(
                                        taskId = remoteRes.taskId,
                                        albumId = albumId,
                                        tipoTarea = "PROCESAMIENTO_IMAGEN",
                                        payloadJson = "{}",
                                        estadoSincronizacion = "PROCESADA"
                                    )
                                )
                            }
                            val jsonResponse = gson.toJson(mapOf("status" to "OK", "message" to "Resultados consolidados guardados"))
                            writeJsonResponse(client, jsonResponse)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error guardando resultados consolidados P2P: ${e.message}")
                        }
                    } else if (bodyJson.isNotBlank()) {
                        val blockRequest = gson.fromJson(bodyJson, TaskBlockRequest::class.java)
                        val isEnqueueOnly = blockRequest.action == "ENQUEUE_ONLY"

                        if (isEnqueueOnly) {
                            Log.i(TAG, "Acumulando ${blockRequest.tasks.size} fotos recibidas del invitado para el álbum ${blockRequest.albumId}")
                            enqueueReceivedTasks(blockRequest)

                            val responseDto = TaskBlockResponse(
                                accepted = true,
                                message = "Fotos recibidas y guardadas en el pool del álbum"
                            )
                            val jsonResponse = gson.toJson(responseDto)
                            writeJsonResponse(client, jsonResponse)
                        } else {
                            Log.i(TAG, "Procesando bloque P2P asignado para álbum ${blockRequest.albumId} con ${blockRequest.tasks.size} fotos")
                            P2PManager.setAssignedBlockCount(blockRequest.albumId, blockRequest.tasks.size)
                            val remoteResults = processReceivedTasks(blockRequest)

                            val responseDto = TaskBlockResponse(
                                accepted = true,
                                message = "Bloque de ${blockRequest.tasks.size} tareas procesado con éxito",
                                results = remoteResults
                            )
                            val jsonResponse = gson.toJson(responseDto)
                            writeJsonResponse(client, jsonResponse)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error manejando cliente P2P: ${e.message}")
            }
        }
    }

    private fun writeJsonResponse(client: Socket, jsonStr: String) {
        try {
            val bytes = jsonStr.toByteArray(Charsets.UTF_8)
            val os = client.getOutputStream()
            val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json; charset=utf-8\r\n" +
                    "Content-Length: ${bytes.size}\r\n" +
                    "Connection: close\r\n\r\n"
            os.write(header.toByteArray(Charsets.UTF_8))
            os.write(bytes)
            os.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error escribiendo respuesta HTTP P2P: ${e.message}")
        }
    }

    private suspend fun enqueueReceivedTasks(request: TaskBlockRequest) {
        try {
            for (task in request.tasks) {
                val payload = gson.fromJson(task.payloadJson, Map::class.java)
                val base64Str = payload["imageBytesBase64"] as? String
                var savedPath = payload["localPath"] as? String ?: payload["imagePath"] as? String ?: ""

                if (!base64Str.isNullOrEmpty()) {
                    val bytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
                    val cacheFile = File(context.cacheDir, "p2p_pool_${task.taskId}.jpg")
                    cacheFile.writeBytes(bytes)
                    savedPath = cacheFile.absolutePath
                }

                val newPayloadJson = org.json.JSONObject().apply {
                    put("localPath", savedPath)
                    put("imagePath", savedPath)
                    put("albumId", request.albumId)
                    put("PENDIENTE", true)
                }.toString()

                val taskEntity = TaskQueueEntity(
                    taskId = task.taskId,
                    albumId = request.albumId,
                    tipoTarea = task.tipoTarea,
                    payloadJson = newPayloadJson,
                    estadoSincronizacion = "PENDIENTE"
                )

                database.taskQueueDao().insert(taskEntity)
            }
            Log.i(TAG, "Acumuladas ${request.tasks.size} tareas en Room DB para álbum ${request.albumId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error acumulando tareas P2P: ${e.message}")
        }
    }

    private suspend fun processReceivedTasks(request: TaskBlockRequest): List<com.tesis.albumcolaborativo.data.distribution.grpc.RemoteTaskResultDto> {
        val completedResults = mutableListOf<com.tesis.albumcolaborativo.data.distribution.grpc.RemoteTaskResultDto>()
        try {
            val classifier = MobileNetClassifier(context)
            val resultDao = database.classificationResultDao()

            for (task in request.tasks) {
                try {
                    val payload = gson.fromJson(task.payloadJson, Map::class.java)
                    val base64Str = payload["imageBytesBase64"] as? String
                    var imageFile: File? = null

                    if (!base64Str.isNullOrEmpty()) {
                        val bytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
                        val cacheFile = File(context.cacheDir, "p2p_received_${task.taskId}.jpg")
                        cacheFile.writeBytes(bytes)
                        imageFile = cacheFile
                    } else {
                        val localPath = payload["localPath"] as? String ?: payload["imagePath"] as? String ?: ""
                        imageFile = File(localPath)
                    }

                    if (imageFile != null && imageFile.exists()) {
                        val startTime = System.currentTimeMillis()
                        val top5 = classifier.classify(imageFile)
                        val duration = System.currentTimeMillis() - startTime

                        val (bestIndex, bestScore) = top5.firstOrNull() ?: Pair(0, 0f)

                        val jsonPredictions = gson.toJson(top5.map { (index, prob) ->
                            mapOf("label" to classifier.getLabel(index), "probability" to prob)
                        })

                        val resultEntity = ClassificationResultEntity(
                            taskId = task.taskId,
                            experimentId = "P2P_REMOTE",
                            albumId = request.albumId,
                            imagePath = imageFile.absolutePath,
                            predictedClassIndex = bestIndex,
                            predictedScore = bestScore,
                            topPredictionsJson = jsonPredictions,
                            preprocessingTimeMs = 0L,
                            tensorPreparationTimeMs = 0L,
                            inferenceTimeMs = duration,
                            totalProcessingTimeMs = duration,
                            deviceId = "P2P_NODE",
                            completedAt = System.currentTimeMillis()
                        )

                        resultDao.insert(resultEntity)

                        val taskQueueEntity = TaskQueueEntity(
                            taskId = task.taskId,
                            albumId = request.albumId,
                            tipoTarea = task.tipoTarea,
                            payloadJson = task.payloadJson,
                            estadoSincronizacion = "PROCESADA"
                        )
                        database.taskQueueDao().insert(taskQueueEntity)

                        val imgBytesBase64 = try {
                            if (imageFile != null && imageFile.exists()) {
                                val b = imageFile.readBytes()
                                android.util.Base64.encodeToString(b, android.util.Base64.NO_WRAP)
                            } else null
                        } catch (_: Exception) { null }

                        completedResults.add(
                            com.tesis.albumcolaborativo.data.distribution.grpc.RemoteTaskResultDto(
                                taskId = task.taskId,
                                topPredictionsJson = jsonPredictions,
                                predictedClassIndex = bestIndex,
                                predictedScore = bestScore,
                                inferenceTimeMs = duration,
                                imageBytesBase64 = imgBytesBase64
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error clasificando foto P2P ${task.taskId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando clasificador P2P: ${e.message}")
        }
        return completedResults
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }

    private fun String.toIntOrDefault(default: Int): Int {
        return this.toIntOrNull() ?: default
    }

    fun stopServer() {
        synchronized(this) {
            isRunning = false
            try {
                serverSocket?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error cerrando servidor P2P: ${e.message}")
            }
            serverSocket = null
        }
    }
}
