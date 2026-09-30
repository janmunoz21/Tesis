package com.tesis.albumcolaborativo.data.model

data class Album(
    val id: String,
    val title: String,
    val description: String,
    val inviteCode: String,
    val participants: Int,
    val createdAt: Long,
    val photoCount: Int = 0,
    val progress: Float = 0f,
    val photos: List<Photo> = emptyList()
)
