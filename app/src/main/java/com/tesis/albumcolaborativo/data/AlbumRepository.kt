package com.tesis.albumcolaborativo.data

import com.tesis.albumcolaborativo.data.model.Album
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Repositorio en memoria. Se reemplazará por Room sin cambiar la interfaz pública.
 */
class AlbumRepository {

    private val _albums = MutableStateFlow(sampleAlbums())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    fun createAlbum(title: String, description: String): Album {
        val album = Album(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            description = description.trim(),
            inviteCode = generateInviteCode(),
            participants = 1,
            createdAt = System.currentTimeMillis()
        )
        _albums.update { listOf(album) + it }
        return album
    }

    private fun generateInviteCode(): String {
        // Sin 0/O ni 1/I para evitar confusiones al dictar el código
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val existing = _albums.value.map { it.inviteCode }.toSet()
        var code: String
        do {
            code = (1..INVITE_CODE_LENGTH).map { chars.random() }.joinToString("")
        } while (code in existing)
        return code
    }

    private fun sampleAlbums(): List<Album> {
        val now = System.currentTimeMillis()
        return listOf(
            Album("sample-1", "Vacaciones Cartagena", "", "CTG826", 4, now - TimeUnit.DAYS.toMillis(2), 142, 1.0f),
            Album("sample-2", "Cumpleaños Ana", "", "ANA457", 6, now - TimeUnit.HOURS.toMillis(3), 87, 0.4f)
        )
    }

    companion object {
        const val INVITE_CODE_LENGTH = 6
    }
}
