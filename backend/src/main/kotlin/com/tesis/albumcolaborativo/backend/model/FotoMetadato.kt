package com.tesis.albumcolaborativo.backend.model

import jakarta.persistence.*
import java.util.UUID

/** Mapeo mínimo de fotos_metadatos: por ahora solo se necesita contar las fotos de un álbum. */
@Entity
@Table(name = "fotos_metadatos")
class FotoMetadato(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "album_id", nullable = false)
    val albumId: UUID
)
