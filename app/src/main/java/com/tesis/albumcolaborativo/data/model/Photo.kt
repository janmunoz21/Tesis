package com.tesis.albumcolaborativo.data.model

data class Photo(
    val uri: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String?
)
