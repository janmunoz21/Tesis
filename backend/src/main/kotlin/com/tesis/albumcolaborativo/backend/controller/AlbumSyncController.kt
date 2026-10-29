package com.tesis.albumcolaborativo.backend.controller

import com.tesis.albumcolaborativo.backend.dto.SyncAlbumResultsDto
import com.tesis.albumcolaborativo.backend.service.AlbumSyncService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/albums")
class AlbumSyncController(
    private val albumSyncService: AlbumSyncService
) {

    @PostMapping("/{albumId}/sincronizar-resultados")
    fun sincronizarResultados(
        @PathVariable albumId: UUID,
        @RequestBody request: SyncAlbumResultsDto
    ): ResponseEntity<Void> {
        val dtoConAlbumId = request.copy(albumId = albumId)
        albumSyncService.consolidarResultadosAlbum(dtoConAlbumId)
        return ResponseEntity.ok().build()
    }
}