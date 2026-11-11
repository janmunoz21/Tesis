package com.tesis.albumcolaborativo.data.network.p2p

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.tesis.albumcolaborativo.data.distribution.grpc.ImageTask
import com.tesis.albumcolaborativo.data.distribution.grpc.TaskBlockRequest
import com.tesis.albumcolaborativo.data.distribution.grpc.TaskBlockResponse
import com.tesis.albumcolaborativo.data.local.entities.TaskQueueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class GrpcP2PClient {

    companion object {
        private const val TAG = "GrpcP2PClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = com.google.gson.GsonBuilder().setLenient().create()

    private fun buildImageTaskWithBytes(task: TaskQueueEntity): ImageTask {
        return try {
            val payloadObj = JSONObject(task.payloadJson)
            val path = payloadObj.optString("imagePath", payloadObj.optString("localPath", ""))
            if (path.isNotBlank()) {
                val file = File(path)
                if (file.exists()) {
                    val bytes = file.readBytes()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    payloadObj.put("imageBytesBase64", base64)
                }
            }
            ImageTask(
                taskId = task.taskId,
                tipoTarea = task.tipoTarea,
                payloadJson = payloadObj.toString()
            )
        } catch (e: Exception) {
            ImageTask(
                taskId = task.taskId,
                tipoTarea = task.tipoTarea,
                payloadJson = task.payloadJson
            )
        }
    }

    /**
     * Realiza un ping rápido de salud HTTP (1 segundo timeout) hacia un nodo P2P.
     */
    suspend fun pingNode(targetAddress: String, targetPort: Int = 50051): Boolean = withContext(Dispatchers.IO) {
        try {
            val pingClient = client.newBuilder().connectTimeout(2, TimeUnit.SECONDS).readTimeout(2, TimeUnit.SECONDS).build()
            val request = Request.Builder().url("http://$targetAddress:$targetPort/p2p/ping").get().build()
            pingClient.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.d(TAG, "Ping falló hacia $targetAddress:$targetPort (${e.message})")
            false
        }
    }

    /**
     * Envía fotos tomadas por un invitado para encolarlas en la lista compartida del creador con reintentos automáticos.
     */
    suspend fun sendTaskBlockToEnqueue(
        targetAddress: String,
        targetPort: Int,
        albumId: String,
        tasks: List<TaskQueueEntity>
    ): Boolean = withContext(Dispatchers.IO) {
        var attempts = 0
        while (attempts < 3) {
            attempts++
            try {
                Log.i(TAG, "Enviando ${tasks.size} fotos para ENQUEUE_ONLY a http://$targetAddress:$targetPort/p2p/sendTaskBlock (intento $attempts)...")

                val imageTasks = tasks.map { buildImageTaskWithBytes(it) }
                val requestDto = TaskBlockRequest(
                    albumId = albumId,
                    tasks = imageTasks,
                    action = "ENQUEUE_ONLY"
                )

                val jsonBody = gson.toJson(requestDto)
                val url = "http://$targetAddress:$targetPort/p2p/sendTaskBlock"

                val httpRequest = Request.Builder()
                    .url(url)
                    .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val success = client.newCall(httpRequest).execute().use { response ->
                    response.isSuccessful
                }
                if (success) {
                    Log.i(TAG, "Fotos enviadas exitosamente para encolar a $targetAddress:$targetPort en intento $attempts")
                    return@withContext true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando fotos para encolar a $targetAddress (intento $attempts): ${e.message}")
                if (attempts < 3) delay(1000)
            }
        }
        false
    }

    /**
     * Envía un bloque de tareas de fotos a un nodo colaborador vía canal P2P/HTTP (puerto 50051).
     */
    suspend fun sendTaskBlock(
        targetAddress: String,
        targetPort: Int,
        albumId: String,
        tasks: List<TaskQueueEntity>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Enviando bloque P2P a http://$targetAddress:$targetPort/p2p/sendTaskBlock con ${tasks.size} tareas...")

            val imageTasks = tasks.map { buildImageTaskWithBytes(it) }

            val requestDto = TaskBlockRequest(
                albumId = albumId,
                tasks = imageTasks
            )

            val jsonBody = gson.toJson(requestDto)
            val url = "http://$targetAddress:$targetPort/p2p/sendTaskBlock"

            val httpRequest = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(httpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val taskResponse = gson.fromJson(responseBody, TaskBlockResponse::class.java)
                    Log.i(TAG, "Respuesta P2P recibida de $targetAddress: accepted=${taskResponse?.accepted}")
                    taskResponse?.accepted ?: true
                } else {
                    Log.e(TAG, "Error en respuesta P2P HTTP ${response.code} de $targetAddress")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallo de conexión P2P hacia $targetAddress:$targetPort -> ${e.message}")
            false
        }
    }

    /**
     * Envía un bloque P2P y retorna la lista de resultados clasificados por el nodo remoto.
     */
    suspend fun sendTaskBlockWithResults(
        targetAddress: String,
        targetPort: Int,
        albumId: String,
        tasks: List<TaskQueueEntity>
    ): TaskBlockResponse? = withContext(Dispatchers.IO) {
        var attempts = 0
        while (attempts < 3) {
            attempts++
            try {
                Log.i(TAG, "Enviando bloque P2P con imagen a $targetAddress:$targetPort/p2p/sendTaskBlock con ${tasks.size} tareas (intento $attempts)...")

                val imageTasks = tasks.map { buildImageTaskWithBytes(it) }

                val requestDto = TaskBlockRequest(albumId = albumId, tasks = imageTasks)
                val jsonBody = gson.toJson(requestDto)
                val url = "http://$targetAddress:$targetPort/p2p/sendTaskBlock"

                val httpRequest = Request.Builder()
                    .url(url)
                    .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val res = client.newCall(httpRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        gson.fromJson(responseBody, TaskBlockResponse::class.java)
                    } else null
                }
                if (res != null && res.accepted) {
                    return@withContext res
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en comunicación P2P con $targetAddress (intento $attempts): ${e.message}")
                if (attempts < 3) delay(1000)
            }
        }
        null
    }

    /**
     * Envía una notificación instantánea de invitación P2P a otro dispositivo en la red Wi-Fi local.
     */
    suspend fun sendP2PInvite(
        targetAddress: String,
        targetPort: Int,
        albumId: String,
        albumName: String,
        inviterName: String,
        senderAddress: String = "",
        senderPort: Int = 50051
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Enviando invitación P2P a $targetAddress:$targetPort para el álbum '$albumName' (ID: $albumId)...")
            val payload = mapOf(
                "action" to "INVITE",
                "albumId" to albumId,
                "albumName" to albumName,
                "inviterName" to inviterName,
                "inviterAddress" to senderAddress,
                "inviterPort" to senderPort
            )
            val jsonBody = gson.toJson(payload)
            val url = "http://$targetAddress:$targetPort/p2p/invite"

            val httpRequest = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(httpRequest).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando invitación P2P a $targetAddress: ${e.message}")
            false
        }
    }

    /**
     * Envía la señal P2P de inicio de procesamiento sincronizado a un nodo colaborador.
     */
    suspend fun sendStartProcessingSignal(
        targetAddress: String,
        targetPort: Int,
        albumId: String,
        albumName: String,
        totalTasks: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Enviando señal START_PROCESSING a $targetAddress:$targetPort para álbum $albumId...")
            val payload = mapOf(
                "action" to "START_PROCESSING",
                "albumId" to albumId,
                "albumName" to albumName,
                "totalTasks" to totalTasks
            )
            val jsonBody = gson.toJson(payload)
            val url = "http://$targetAddress:$targetPort/p2p/startProcessing"

            val httpRequest = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(httpRequest).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando señal START_PROCESSING a $targetAddress: ${e.message}")
            false
        }
    }

    /**
     * Transmite los resultados consolidados de clasificación P2P a los nodos colaboradores.
     */
    suspend fun broadcastCompletedResults(
        targetAddress: String,
        targetPort: Int,
        albumId: String,
        results: List<com.tesis.albumcolaborativo.data.local.entities.ClassificationResultEntity>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Transmitiendo ${results.size} resultados P2P consolidados a http://$targetAddress:$targetPort/p2p/syncResults...")
            val dtos = results.map { res ->
                val imgBytesBase64 = try {
                    val f = File(res.imagePath)
                    if (f.exists()) {
                        val b = f.readBytes()
                        Base64.encodeToString(b, Base64.NO_WRAP)
                    } else null
                } catch (_: Exception) { null }

                com.tesis.albumcolaborativo.data.distribution.grpc.RemoteTaskResultDto(
                    taskId = res.taskId,
                    topPredictionsJson = res.topPredictionsJson,
                    predictedClassIndex = res.predictedClassIndex,
                    predictedScore = res.predictedScore,
                    inferenceTimeMs = res.inferenceTimeMs,
                    imageBytesBase64 = imgBytesBase64
                )
            }
            val payload = mapOf(
                "action" to "SYNC_RESULTS",
                "albumId" to albumId,
                "results" to dtos
            )
            val jsonBody = gson.toJson(payload)
            val url = "http://$targetAddress:$targetPort/p2p/syncResults"

            val httpRequest = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(httpRequest).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Error transmitiendo resultados consolidados P2P a $targetAddress: ${e.message}")
            false
        }
    }
}
