package com.tesis.albumcolaborativo.data.distribution.grpc

import com.tesis.albumcolaborativo.data.distribution.AssignedBlock
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GrpcTaskSender {

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

            stub.sendTaskBlock(request)

        } finally {

            channel.shutdown()
        }
    }
}