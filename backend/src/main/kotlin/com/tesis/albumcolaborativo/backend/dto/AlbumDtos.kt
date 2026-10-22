package com.tesis.albumcolaborativo.backend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

// CU-B-01
data class CrearAlbumRequest(
    @field:NotBlank(message = "El nombre del álbum es obligatorio")
    @field:Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    val nombre: String,

    @field:Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    val descripcion: String? = null
)

// CU-B-02
data class InvitarUsuarioRequest(
    @field:NotBlank(message = "El correo no puede estar vacío")
    @field:Email(message = "Debe proporcionar un correo válido")
    val correo: String
)

// CU-B-03
data class UnirseAlbumRequest(
    @field:NotBlank(message = "El token de invitación es requerido")
    val tokenInvitacion: String
)

// CU-B-05
data class AlbumResumenResponse(
    val id: UUID,
    val nombre: String,
    val descripcion: String?,
    val fechaCreacion: Instant,
    val miRol: String,
    val totalMiembros: Long,
    val totalFotos: Long
)

data class ParticipanteResponse(
    val usuarioId: UUID,
    val nombre: String,
    val rol: String
)

// CU-B-01 y CU-B-06
data class AlbumDetalleResponse(
    val id: UUID,
    val nombre: String,
    val descripcion: String?,
    val fechaCreacion: Instant,
    val tokenInvitacion: String,
    val creadorId: UUID,
    val miRol: String,
    val totalFotos: Long,
    val participantes: List<ParticipanteResponse>
)

// Para CU-B-07
data class EditarAlbumRequest(
    val nombre: String,
    val descripcion: String? = null
)

// Para CU-B-09
data class RemoverParticipanteRequest(
    val usuarioId: UUID
)
