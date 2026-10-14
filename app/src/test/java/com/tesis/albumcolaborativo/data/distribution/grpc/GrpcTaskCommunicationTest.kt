package com.tesis.albumcolaborativo.data.distribution.grpc

import com.tesis.albumcolaborativo.data.distribution.AssignedBlock
import com.tesis.albumcolaborativo.data.distribution.ProcessingNode
import com.tesis.albumcolaborativo.data.local.entities.TaskQueueEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrpcTaskCommunicationTest {

    @Test
    fun `un bloque se envia correctamente mediante gRPC`() {

        val serverManager = GrpcServerManager()

        serverManager.start(50051)

        try {

            val tasks = listOf(
                TaskQueueEntity(
                    taskId = "task-1",
                    albumId = "album-1",
                    tipoTarea = "PROCESAMIENTO_IMAGEN",
                    payloadJson = """{"imagePath":"image-1.jpg"}""",
                    estadoSincronizacion = "PENDIENTE"
                ),
                TaskQueueEntity(
                    taskId = "task-2",
                    albumId = "album-1",
                    tipoTarea = "PROCESAMIENTO_IMAGEN",
                    payloadJson = """{"imagePath":"image-2.jpg"}""",
                    estadoSincronizacion = "PENDIENTE"
                )
            )

            val node = ProcessingNode(
                nodeId = "node-test",
                address = "127.0.0.1",
                port = 50051
            )

            val block = AssignedBlock(
                node = node,
                tasks = tasks
            )

            val sender = GrpcTaskSender()

            val response = kotlinx.coroutines.runBlocking {

                sender.sendBlock(block)
            }

            assertTrue(response.accepted)
            assertEquals(
                "Bloque recibido correctamente",
                response.message
            )

        } finally {

            serverManager.stop()
        }
    }
}