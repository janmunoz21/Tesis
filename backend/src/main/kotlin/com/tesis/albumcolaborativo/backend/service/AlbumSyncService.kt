package com.tesis.albumcolaborativo.backend.service

import com.tesis.albumcolaborativo.backend.dto.FotoMetadatoResponse
import com.tesis.albumcolaborativo.backend.dto.SyncAlbumResultsDto
import com.tesis.albumcolaborativo.backend.model.FotoMetadato
import com.tesis.albumcolaborativo.backend.repository.AlbumRepository
import com.tesis.albumcolaborativo.backend.repository.FotoMetadatoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

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

    @Transactional(readOnly = true)
    fun obtenerMetadatosAlbum(albumId: UUID): List<FotoMetadatoResponse> {
        check(albumRepository.existsById(albumId)) {
            "El álbum con ID $albumId no existe en el sistema."
        }
        return fotoMetadatoRepository.findByAlbumId(albumId).map { f ->
            FotoMetadatoResponse(
                id = f.id!!,
                albumId = f.albumId,
                propietarioId = f.propietarioId,
                etiquetaConsolidada = f.etiquetaConsolidada,
                porcentajeConfianza = f.porcentajeConfianza,
                reclasificadaManual = f.reclasificadaManual
            )
        }
    }

    @Transactional
    fun reclasificarFoto(
        albumId: UUID,
        fotoId: UUID,
        nuevaEtiqueta: String
    ): FotoMetadatoResponse {
        val foto = fotoMetadatoRepository.findByIdAndAlbumId(fotoId, albumId)
            ?: throw IllegalArgumentException("Foto no encontrada para el álbum $albumId")

        foto.etiquetaConsolidada = nuevaEtiqueta.trim()
        foto.reclasificadaManual = true
        val guardada = fotoMetadatoRepository.save(foto)

        return FotoMetadatoResponse(
            id = guardada.id!!,
            albumId = guardada.albumId,
            propietarioId = guardada.propietarioId,
            etiquetaConsolidada = guardada.etiquetaConsolidada,
            porcentajeConfianza = guardada.porcentajeConfianza,
            reclasificadaManual = guardada.reclasificadaManual
        )
    }
}