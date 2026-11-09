package com.tesis.albumcolaborativo.data.network.p2p

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
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
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

class GrpcP2PService(private val context: Context, private val port: Int = 50051) {

    companion object {
        private const val TAG = "GrpcP2PService"
    }

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val gson = com.google.gson.GsonBuilder().setLenient().create()
    private val database = AppDatabase.getDatabase(context)

    fun startServer() {
        if (isRunning) return
        isRunning = true

        scope.launch {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                Log.i(TAG, "Servidor P2P iniciado exitosamente y escuchando en puerto $port")

                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    handleClient(clientSocket)
                }
            } catch (e: Exception) {
                isRunning = false
                Log.e(TAG, "Error iniciando servidor P2P en puerto $port: ${e.message}", e)
            }
        }
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                socket.use { client ->
                    val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                    val writer = PrintWriter(client.getOutputStream(), true)

                    var firstLine: String? = null
                    var line: String?
                    var contentLength = 0

                    firstLine = reader.readLine()
                    if (firstLine == null) return@use

                    if (firstLine.startsWith("GET /p2p/ping", ignoreCase = true)) {
                        val jsonResponse = gson.toJson(mapOf("status" to "OK", "pong" to true))
                        writer.println("HTTP/1.1 200 OK")
                        writer.println("Content-Type: application/json")
                        writer.println("Content-Length: ${jsonResponse.length}")
                        writer.println()
                        writer.print(jsonResponse)
                        writer.flush()
                        return@use
                    }

                    while (reader.readLine().also { line = it } != null) {
                        if (line.isNull_or_blank()) break
                        if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                            contentLength = line!!.substringAfter(":").trim().toIntOrDefault(0)
                        }
                    }

                    if (contentLength > 0) {
                        val bodyChars = CharArray(contentLength)
                        var totalRead = 0
                        while (totalRead < contentLength) {
                            val read = reader.read(bodyChars, totalRead, contentLength - totalRead)
                            if (read <= 0) break
                            totalRead += read
                        }
                        val bodyJson = String(bodyChars, 0, totalRead)

                        if (bodyJson.contains("\"INVITE\"")) {
                            val map = gson.fromJson(bodyJson, Map::class.java)
                            val albumId = map["albumId"] as? String ?: UUID.randomUUID().toString()
                            val albumName = map["albumName"] as? String ?: "Nuevo Álbum"
                            val inviterName = map["inviterName"] as? String ?: "Un usuario"

                            Log.i(TAG, "Invitación P2P recibida de $inviterName para '$albumName' (ID: $albumId)")

                            P2PManager.onInviteReceived(albumId, albumName, inviterName)

                            val jsonResponse = gson.toJson(mapOf("status" to "OK", "message" to "Invitación P2P recibida"))
                            writer.println("HTTP/1.1 200 OK")
                            writer.println("Content-Type: application/json")
                            writer.println("Content-Length: ${jsonResponse.length}")
                            writer.println()
                            writer.print(jsonResponse)
                            writer.flush()
                        } else if (bodyJson.contains("\"START_PROCESSING\"")) {
                            val map = gson.fromJson(bodyJson, Map::class.java)
                            val albumId = map["albumId"] as? String ?: ""
                            val albumName = map["albumName"] as? String ?: "Álbum Colaborativo"
                            val totalTasks = (map["totalTasks"] as? Number)?.toInt() ?: 1

                            Log.i(TAG, "Señal START_PROCESSING recibida para álbum $albumId con $totalTasks tareas")
                            P2PManager.onProcessingSignalReceived(albumId, albumName, totalTasks)

                            val jsonResponse = gson.toJson(mapOf("status" to "OK", "message" to "Procesamiento iniciado"))
                            writer.println("HTTP/1.1 200 OK")
                            writer.println("Content-Type: application/json")
                            writer.println("Content-Length: ${jsonResponse.length}")
                            writer.println()
                            writer.print(jsonResponse)
                            writer.flush()
                        } else {
                            val blockRequest = gson.fromJson(bodyJson, TaskBlockRequest::class.java)
                            val isEnqueueOnly = blockRequest.action == "ENQUEUE_ONLY" || bodyJson.contains("ENQUEUE_ONLY") || bodyJson.contains("PENDIENTE")

                            if (isEnqueueOnly) {
                                Log.i(TAG, "Acumulando ${blockRequest.tasks.size} fotos recibidas del invitado para el álbum ${blockRequest.albumId}")
                                enqueueReceivedTasks(blockRequest)

                                val responseDto = TaskBlockResponse(
                                    accepted = true,
                                    message = "Fotos recibidas y guardadas en el pool del álbum"
                                )
                                val jsonResponse = gson.toJson(responseDto)
                                writer.println("HTTP/1.1 200 OK")
                                writer.println("Content-Type: application/json")
                                writer.println("Content-Length: ${jsonResponse.length}")
                                writer.println()
                                writer.print(jsonResponse)
                                writer.flush()
                            } else {
                                Log.i(TAG, "Procesando bloque P2P asignado para álbum ${blockRequest.albumId} con ${blockRequest.tasks.size} fotos")
                                val remoteResults = processReceivedTasks(blockRequest)

                                val responseDto = TaskBlockResponse(
                                    accepted = true,
                                    message = "Bloque de ${blockRequest.tasks.size} tareas procesado con éxito",
                                    results = remoteResults
                                )
                                val jsonResponse = gson.toJson(responseDto)

                                writer.println("HTTP/1.1 200 OK")
                                writer.println("Content-Type: application/json")
                                writer.println("Content-Length: ${jsonResponse.length}")
                                writer.println()
                                writer.print(jsonResponse)
                                writer.flush()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error manejando cliente P2P: ${e.message}")
            }
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

                        completedResults.add(
                            com.tesis.albumcolaborativo.data.distribution.grpc.RemoteTaskResultDto(
                                taskId = task.taskId,
                                topPredictionsJson = jsonPredictions,
                                predictedClassIndex = bestIndex,
                                predictedScore = bestScore,
                                inferenceTimeMs = duration
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
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando servidor P2P: ${e.message}")
        }
    }
}
