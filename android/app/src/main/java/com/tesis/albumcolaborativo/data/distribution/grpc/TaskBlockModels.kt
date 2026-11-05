package com.tesis.albumcolaborativo.data.distribution.grpc

data class ImageTask(
    val taskId: String,
    val tipoTarea: String,
    val payloadJson: String
)

data class TaskBlockRequest(
    val albumId: String,
    val tasks: List<ImageTask>
)

data class TaskBlockResponse(
    val accepted: Boolean,
    val message: String
)
