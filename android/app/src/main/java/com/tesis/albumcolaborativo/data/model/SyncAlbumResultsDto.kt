package com.tesis.albumcolaborativo.data.model

import java.util.UUID

data class SyncAlbumResultsDto(
    val albumId: String,
    val fotos: List<FotoResultDto>
)

data class FotoResultDto(
    val taskId: String,
    val propietarioId: String,
    val etiquetaConsolidada: String,
    val porcentajeConfianza: Float
)