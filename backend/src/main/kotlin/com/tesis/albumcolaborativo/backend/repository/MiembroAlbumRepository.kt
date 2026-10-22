package com.tesis.albumcolaborativo.backend.repository

import com.tesis.albumcolaborativo.backend.model.Album
import com.tesis.albumcolaborativo.backend.model.EstadoMiembro
import com.tesis.albumcolaborativo.backend.model.MiembroAlbum
import com.tesis.albumcolaborativo.backend.model.MiembroAlbumId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface MiembroAlbumRepository : JpaRepository<MiembroAlbum, MiembroAlbumId> {

    @Query("SELECT m.id.albumId, COUNT(m) FROM MiembroAlbum m WHERE m.id.albumId IN :albumIds AND m.estado = 'ACTIVO' GROUP BY m.id.albumId")
    fun contarPorAlbum(albumIds: List<UUID>): List<Array<Any>>

    @Query("SELECT u.id, u.nombre, m.rol FROM MiembroAlbum m JOIN Usuario u ON m.id.usuarioId = u.id WHERE m.id.albumId = :albumId AND m.estado = 'ACTIVO'")
    fun participantesDe(albumId: UUID): List<Array<Any>>

    // Conteo por un solo álbum y estado
    fun countByIdAlbumIdAndEstado(albumId: UUID, estado: String): Long

    // Búsqueda de IDs por estado
    fun findByIdUsuarioIdAndEstado(usuarioId: UUID, estado: EstadoMiembro): List<MiembroAlbum>

    @Query("""
        SELECT a 
        FROM Album a, MiembroAlbum m 
        WHERE m.id.albumId = a.id 
          AND m.id.usuarioId = :usuarioId 
          AND m.estado = com.tesis.albumcolaborativo.backend.model.EstadoMiembro.PENDIENTE
    """)
    fun findInvitacionesPendientes(@Param("usuarioId") usuarioId: UUID): List<Album>
}
