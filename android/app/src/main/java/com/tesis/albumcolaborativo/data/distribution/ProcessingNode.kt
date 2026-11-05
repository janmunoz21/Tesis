package com.tesis.albumcolaborativo.data.distribution

data class ProcessingNode(
    val nodeId: String,
    val address: String,
    val port: Int,
    val available: Boolean = true,
    val deviceName: String = "",
    val isLocal: Boolean = false,
    val isProcessing: Boolean = false
)