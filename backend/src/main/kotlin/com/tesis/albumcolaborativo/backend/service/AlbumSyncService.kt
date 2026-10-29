package com.tesis.albumcolaborativo.backend.service

import com.tesis.albumcolaborativo.backend.dto.SyncAlbumResultsDto
import com.tesis.albumcolaborativo.backend.model.FotoMetadato
import com.tesis.albumcolaborativo.backend.repository.AlbumRepository
import com.tesis.albumcolaborativo.backend.repository.FotoMetadatoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlbumSyncService(
    private val fotoMetadatoRepository: FotoMetadatoRepository,
    private val albumRepository: AlbumRepository
) {

    @Transactional
    fun consolidarResultadosAlbum(dto: SyncAlbumResultsDto) {
        // 1. Verificar que el álbum existe en el backend central
        check(albumRepository.existsById(dto.albumId)) {
            "El álbum con ID ${dto.albumId} no existe en el sistema."
        }

        // 2. Mapear los resultados del DTO a Entidades JPA (fotos_metadatos)
        val entidades = dto.fotos.map { fotoDto ->
            FotoMetadato(
                albumId = dto.albumId,
                propietarioId = fotoDto.propietarioId,
                etiquetaConsolidada = fotoDto.etiquetaConsolidada,
                porcentajeConfianza = fotoDto.porcentajeConfianza
            )
        }

        // 3. Persistencia en lote (batch) en PostgreSQL
        fotoMetadatoRepository.saveAll(entidades)
    }
}