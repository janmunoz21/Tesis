package com.tesis.albumcolaborativo.backend.dto

import java.util.UUID

data class SyncAlbumResultsDto(
    val albumId: UUID,
    val fotos: List<FotoResultDto>
)

data class FotoResultDto(
    val taskId: String,
    val propietarioId: UUID,
    val etiquetaConsolidada: String,
    val porcentajeConfianza: Float
)