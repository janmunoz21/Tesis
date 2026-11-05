package com.tesis.albumcolaborativo.data.network.p2p

import android.util.Log
import com.google.gson.Gson
import com.tesis.albumcolaborativo.data.distribution.grpc.ImageTask
import com.tesis.albumcolaborativo.data.distribution.grpc.TaskBlockRequest
import com.tesis.albumcolaborativo.data.distribution.grpc.TaskBlockResponse
import com.tesis.albumcolaborativo.data.local.entities.TaskQueueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GrpcP2PClient {

    companion object {
        private const val TAG = "GrpcP2PClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

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

            val imageTasks = tasks.map { task ->
                ImageTask(
                    taskId = task.taskId,
                    tipoTarea = task.tipoTarea,
                    payloadJson = task.payloadJson
                )
            }

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
     * Envía una notificación instantánea de invitación P2P a otro dispositivo en la red Wi-Fi local.
     */
    suspend fun sendP2PInvite(
        targetAddress: String,
        targetPort: Int,
        albumName: String,
        inviterName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Enviando invitación P2P a $targetAddress:$targetPort para el álbum '$albumName'...")
            val payload = mapOf(
                "action" to "INVITE",
                "albumName" to albumName,
                "inviterName" to inviterName
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
}
