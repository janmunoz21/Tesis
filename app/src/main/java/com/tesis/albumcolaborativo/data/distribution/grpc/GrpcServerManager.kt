package com.tesis.albumcolaborativo.data.distribution.grpc

import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder

class GrpcServerManager {

    private var server: Server? = null

    val receiver = GrpcTaskReceiver()

    fun start(port: Int = 50051) {

        if (server != null) {
            return
        }

        server = NettyServerBuilder
            .forPort(port)
            .addService(receiver)
            .build()
            .start()

        println("Servidor gRPC iniciado en puerto $port")
    }

    fun stop() {

        server?.shutdown()
        server = null

        println("Servidor gRPC detenido")
    }

    fun isRunning(): Boolean {
        return server?.isShutdown == false
    }
}