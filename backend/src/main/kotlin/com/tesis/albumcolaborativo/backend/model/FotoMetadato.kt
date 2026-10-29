package com.tesis.albumcolaborativo.backend.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "fotos_metadatos")
class FotoMetadato(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "album_id", nullable = false)
    val albumId: UUID,

    @Column(name = "propietario_id", nullable = false)
    val propietarioId: UUID,

    @Column(name = "etiqueta_consolidada")
    var etiquetaConsolidada: String? = null,

    @Column(name = "porcentaje_confianza")
    var porcentajeConfianza: Float? = null
)