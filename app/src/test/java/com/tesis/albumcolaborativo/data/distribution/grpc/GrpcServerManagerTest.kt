package com.tesis.albumcolaborativo.data.distribution.grpc

import org.junit.Assert.assertTrue
import org.junit.Test

class GrpcServerManagerTest {

    @Test
    fun `servidor gRPC puede iniciar y detenerse`() {

        val serverManager = GrpcServerManager()

        serverManager.start(50051)

        assertTrue(serverManager.isRunning())

        serverManager.stop()

        assertTrue(!serverManager.isRunning())
    }
}