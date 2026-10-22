package com.tesis.albumcolaborativo.data.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

// ==========================================
// 1. MODELOS DE UI (USADOS POR PANTALLAS Y VIEWMODEL)
// ==========================================

data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val isAutoNode: Boolean = true,
    val credits: Double = 0.0
)

data class Album(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val photoCount: Int = 0,
    val status: String = "Listo",
    val isOwner: Boolean = true
)

data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val count: Int,
    val confidence: Float
)

data class Photo(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val category: String,
    val confidence: Float,
    val processedByNode: String = "Local"
)

data class NodeDevice(
    val id: String,
    val name: String,
    val status: String,
    val isAvailable: Boolean
)

data class TaskBlock(
    val id: String,
    val albumTitle: String,
    val photoCount: Int,
    val assignedTime: String
)

// ==========================================
// 2. DTOS DE RED (CONEXIÓN CON EL BACKEND)
// ==========================================

// --- Autenticación ---
data class RegistroRequest(
    val nombre: String,
    val correo: String,
    val contrasena: String
)

data class LoginRequest(
    val correo: String,
    val contrasena: String
)

data class AuthResponseDto(
    val token: String
)

data class AuthResponse(
    val token: String,
    val usuario: UsuarioDto
)

data class UsuarioDto(
    val id: String,
    val nombre: String,
    val correo: String,
    val fotoPerfilUrl: String? = null
)

data class RecuperarContrasenaRequest(
    val correo: String
)

data class RestablecerContrasenaRequest(
    val token: String,
    val nuevaContrasena: String
)

data class EditarPerfilRequest(
    val nombre: String
)

data class CambiarContrasenaRequest(
    val contrasenaActual: String,
    val nuevaContrasena: String
)

// --- Álbumes solicitudes ---
data class CrearAlbumRequest(
    val nombre: String,
    val descripcion: String? = null
)

data class EditarAlbumRequest(
    val nombre: String,
    val descripcion: String? = null
)

data class InvitarUsuarioRequest(
    val correo: String
)

data class UnirseAlbumRequest(
    val tokenInvitacion: String
)

data class RemoverParticipanteRequest(
    val usuarioId: String
)

// --- Álbumes respuestas ---
data class AlbumResumenDto(
    val id: String,
    val nombre: String,
    val descripcion: String? = null,
    val fechaCreacion: String? = null,
    val miRol: String,
    val totalMiembros: Long,
    val totalFotos: Long
)

data class AlbumDetalleDto(
    val id: String,
    val nombre: String,
    val descripcion: String? = null,
    val fechaCreacion: String? = null,
    val tokenInvitacion: String,
    val creadorId: String,
    val miRol: String,
    val totalFotos: Long,
    val participantes: List<ParticipanteDto>
)

data class ParticipanteDto(
    val usuarioId: String,
    val nombre: String,
    val rol: String
)