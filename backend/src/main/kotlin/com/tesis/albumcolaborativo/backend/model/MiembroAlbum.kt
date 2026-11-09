package com.tesis.albumcolaborativo.backend.model

import jakarta.persistence.*
import java.io.Serializable
import java.util.Objects
import java.util.UUID

/** Clave primaria compuesta (album_id, usuario_id) de miembros_album. */
@Embeddable
class MiembroAlbumId(
    @Column(name = "album_id", nullable = false)
    var albumId: UUID = UUID(0, 0),

    @Column(name = "usuario_id", nullable = false)
    var usuarioId: UUID = UUID(0, 0)
) : Serializable {
    override fun equals(other: Any?): Boolean =
        other is MiembroAlbumId && albumId == other.albumId && usuarioId == other.usuarioId

    override fun hashCode(): Int = Objects.hash(albumId, usuarioId)
}

@Entity
@Table(name = "miembros_album")
class MiembroAlbum(
    @EmbeddedId
    val id: MiembroAlbumId,

    @Column(name = "rol", nullable = false, length = 20)
    var rol: String,

    @Column(name = "estado", nullable = false, length = 20)
    var estado: String = "ACTIVO"
)

object RolAlbum {
    const val CREADOR = "CREADOR"
    const val PARTICIPANTE = "PARTICIPANTE"
}

object EstadoMiembro {
    const val ACTIVO = "ACTIVO"
    const val PENDIENTE = "PENDIENTE"
}
