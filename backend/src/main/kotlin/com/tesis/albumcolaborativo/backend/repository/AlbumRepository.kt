package com.tesis.albumcolaborativo.backend.repository

import com.tesis.albumcolaborativo.backend.model.Album
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import java.util.UUID

interface AlbumRepository : JpaRepository<Album, UUID> {

    fun existsByTokenInvitacion(tokenInvitacion: String): Boolean

    /** Cada fila es [Album, rol del usuario en ese álbum]. Solo membresías ACTIVO, más recientes primero. */
    @Query(
        "select a, m.rol from Album a, MiembroAlbum m " +
            "where m.id.albumId = a.id and m.id.usuarioId = :usuarioId and m.estado = 'ACTIVO' " +
            "order by a.fechaCreacion desc"
    )
    fun findDeUsuario(@Param("usuarioId") usuarioId: UUID): List<Array<Any>>
    fun findByTokenInvitacion(tokenInvitacion: String): Optional<Album>
}
