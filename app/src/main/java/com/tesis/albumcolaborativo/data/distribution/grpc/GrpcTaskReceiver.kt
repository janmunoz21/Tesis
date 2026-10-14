package com.tesis.albumcolaborativo.data.distribution.grpc

import io.grpc.stub.StreamObserver

class GrpcTaskReceiver :
    TaskTransferServiceGrpc.TaskTransferServiceImplBase() {

    var lastAlbumId: String? = null
        private set

    var lastTaskCount: Int = 0
        private set

    override fun sendTaskBlock(
        request: TaskBlockRequest,
        responseObserver: StreamObserver<TaskBlockResponse>
    ) {

        lastAlbumId = request.albumId
        lastTaskCount = request.tasksCount

        println("Bloque recibido por gRPC")
        println("Album: ${request.albumId}")
        println("Cantidad de tareas: ${request.tasksCount}")

        val response = TaskBlockResponse.newBuilder()
            .setAccepted(true)
            .setMessage("Bloque recibido correctamente")
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}