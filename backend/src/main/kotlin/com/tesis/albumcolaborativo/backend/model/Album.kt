package com.tesis.albumcolaborativo.backend.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "albumes")
class Album(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    val id: UUID? = null,

    @Column(name = "nombre", nullable = false, length = 150)
    var nombre: String,

    @Column(name = "descripcion", length = 500)
    var descripcion: String? = null,

    @Column(name = "creador_id", nullable = false)
    val creadorId: UUID,

    @Column(name = "fecha_creacion", nullable = false)
    val fechaCreacion: Instant = Instant.now(),

    @Column(name = "token_invitacion", nullable = false, unique = true, length = 64)
    val tokenInvitacion: String
)
