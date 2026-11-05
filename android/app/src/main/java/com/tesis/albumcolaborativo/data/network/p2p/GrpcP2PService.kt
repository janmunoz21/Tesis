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
import com.tesis.albumcolaborativo.data.ml.MobileNetClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class GrpcP2PService(private val context: Context, private val port: Int = 50051) {

    companion object {
        private const val TAG = "GrpcP2PService"
    }

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val gson = Gson()
    private val database = AppDatabase.getDatabase(context)

    fun startServer() {
        if (isRunning) return
        isRunning = true

        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.i(TAG, "Servidor P2P iniciado y escuchando en el puerto $port")

                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    handleClient(clientSocket)
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(TAG, "Error en el servidor P2P: ${e.message}")
                }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                socket.use { client ->
                    val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                    val writer = PrintWriter(client.getOutputStream(), true)

                    var line: String?
                    var contentLength = 0

                    while (reader.readLine().also { line = it } != null) {
                        if (line.isNull_or_blank()) break
                        if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                            contentLength = line!!.substringAfter(":").trim().toIntOrDefault(0)
                        }
                    }

                    if (contentLength > 0) {
                        val bodyChars = CharArray(contentLength)
                        reader.read(bodyChars, 0, contentLength)
                        val bodyJson = String(bodyChars)

                        if (bodyJson.contains("\"INVITE\"")) {
                            val map = gson.fromJson(bodyJson, Map::class.java)
                            val albumName = map["albumName"] as? String ?: "Nuevo Álbum"
                            val inviterName = map["inviterName"] as? String ?: "Un usuario"

                            Log.i(TAG, "Invitación P2P recibida de $inviterName para '$albumName'")

                            P2PManager.onInviteReceived(albumName, inviterName)

                            val jsonResponse = gson.toJson(mapOf("status" to "OK", "message" to "Invitación P2P recibida"))
                            writer.println("HTTP/1.1 200 OK")
                            writer.println("Content-Type: application/json")
                            writer.println("Content-Length: ${jsonResponse.length}")
                            writer.println()
                            writer.print(jsonResponse)
                            writer.flush()
                        } else {
                            val blockRequest = gson.fromJson(bodyJson, TaskBlockRequest::class.java)
                            Log.i(TAG, "Bloque P2P recibido para álbum ${blockRequest.albumId} con ${blockRequest.tasks.size} fotos")

                            // Procesar tareas recibidas con MobileNetV3
                            processReceivedTasks(blockRequest)

                            val responseDto = TaskBlockResponse(
                                accepted = true,
                                message = "Bloque de ${blockRequest.tasks.size} tareas aceptado y procesado"
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
            } catch (e: Exception) {
                Log.e(TAG, "Error manejando cliente P2P: ${e.message}")
            }
        }
    }

    private suspend fun processReceivedTasks(request: TaskBlockRequest) {
        try {
            val classifier = MobileNetClassifier(context)
            val resultDao = database.classificationResultDao()

            for (task in request.tasks) {
                try {
                    // Extraer ruta del archivo
                    val payload = gson.fromJson(task.payloadJson, Map::class.java)
                    val localPath = payload["localPath"] as? String ?: continue
                    val imageFile = File(localPath)

                    if (imageFile.exists()) {
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
                        Log.i(TAG, "Foto P2P procesada con éxito en ${duration}ms: ${task.taskId}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error clasificando foto P2P ${task.taskId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando clasificador P2P: ${e.message}")
        }
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
