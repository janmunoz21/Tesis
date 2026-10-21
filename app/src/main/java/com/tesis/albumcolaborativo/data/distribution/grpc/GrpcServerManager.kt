package com.tesis.albumcolaborativo.data.distribution.grpc

import io.grpc.InsecureServerCredentials
import io.grpc.Server
import io.grpc.okhttp.OkHttpServerBuilder
import java.util.concurrent.TimeUnit

class GrpcServerManager {

    private var server: Server? = null

    val receiver = GrpcTaskReceiver()

    fun start(port: Int = 50051) {

        if (server != null) {
            return
        }

        // OkHttp en lugar de Netty: Netty no tiene soporte oficial en Android
        server = OkHttpServerBuilder
            .forPort(port, InsecureServerCredentials.create())
            .addService(receiver)
            .build()
            .start()

        println("Servidor gRPC iniciado en puerto $port")
    }

    fun stop() {

        server?.shutdown()
        // Espera breve para liberar el puerto antes de un posible reinicio
        server?.awaitTermination(500, TimeUnit.MILLISECONDS)
        server = null

        println("Servidor gRPC detenido")
    }

    fun isRunning(): Boolean {
        return server?.isShutdown == false
    }
}