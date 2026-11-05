package com.tesis.albumcolaborativo.backend.controller

import com.tesis.albumcolaborativo.backend.dto.SyncAlbumResultsDto
import com.tesis.albumcolaborativo.backend.service.AlbumSyncService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/albumes") // 👈 Unificado a /api/albumes
class AlbumSyncController(
    private val albumSyncService: AlbumSyncService
) {

    @PostMapping("/{albumId}/sincronizar-resultados")
    fun sincronizarResultados(
        @PathVariable albumId: String, // 👈 Cambiado a String para evitar fallos de conversión HTTP 401
        @RequestBody request: SyncAlbumResultsDto
    ): ResponseEntity<Void> {
        val cleanAlbumId = albumId.replace("\"", "").trim()
        val dtoConAlbumId = request.copy(albumId = UUID.fromString(cleanAlbumId))

        albumSyncService.consolidarResultadosAlbum(dtoConAlbumId)
        return ResponseEntity.ok().build()
    }
}