package com.tesis.albumcolaborativo.data.distribution.grpc

data class ImageTask(
    val taskId: String,
    val tipoTarea: String,
    val payloadJson: String
)

data class TaskBlockRequest(
    val albumId: String,
    val tasks: List<ImageTask>,
    val action: String = "PROCESS"
)

data class RemoteTaskResultDto(
    val taskId: String,
    val topPredictionsJson: String,
    val predictedClassIndex: Int,
    val predictedScore: Float,
    val inferenceTimeMs: Long
)

data class TaskBlockResponse(
    val accepted: Boolean,
    val message: String,
    val results: List<RemoteTaskResultDto> = emptyList()
)
