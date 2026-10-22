package com.tesis.albumcolaborativo.backend.repository

import com.tesis.albumcolaborativo.backend.model.FotoMetadato
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface FotoMetadatoRepository : JpaRepository<FotoMetadato, UUID> {

    fun countByAlbumId(albumId: UUID): Long

    /** Filas [albumId, cantidad de fotos]. */
    @Query("select f.albumId, count(f) from FotoMetadato f where f.albumId in :albumIds group by f.albumId")
    fun contarPorAlbum(@Param("albumIds") albumIds: Collection<UUID>): List<Array<Any>>
}
