package com.tesis.albumcolaborativo.data.distribution.grpc

import com.tesis.albumcolaborativo.data.distribution.AssignedBlock
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class GrpcTaskSender {

    companion object {
        // Si el nodo no responde en este tiempo, la llamada falla con DEADLINE_EXCEEDED
        const val SEND_TIMEOUT_SECONDS = 10L
    }

    suspend fun sendBlock(
        block: AssignedBlock
    ): TaskBlockResponse = withContext(Dispatchers.IO) {

        val channel: ManagedChannel =
            OkHttpChannelBuilder
                .forAddress(
                    block.node.address,
                    block.node.port
                )
                .usePlaintext()
                .build()

        try {

            val tasks = block.tasks.map { task ->

                ImageTask.newBuilder()
                    .setTaskId(task.taskId)
                    .setTipoTarea(task.tipoTarea)
                    .setPayloadJson(task.payloadJson)
                    .build()
            }

            val request =
                TaskBlockRequest.newBuilder()
                    .setAlbumId(
                        block.tasks.firstOrNull()?.albumId ?: ""
                    )
                    .addAllTasks(tasks)
                    .build()

            val stub =
                TaskTransferServiceGrpc
                    .newBlockingStub(channel)
                    .withDeadlineAfter(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            stub.sendTaskBlock(request)

        } finally {

            channel.shutdown()
        }
    }
}