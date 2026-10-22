package com.tesis.albumcolaborativo.backend.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// CU-A-01
data class RegistroRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    @field:Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    val nombre: String,

    @field:NotBlank(message = "El correo es obligatorio")
    @field:Email(message = "El formato del correo no es válido")
    @field:Size(max = 255, message = "El correo no puede superar 255 caracteres")
    val correo: String,

    @field:NotBlank(message = "La contraseña es obligatoria")
    @field:Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
    val contrasena: String
)

// CU-A-02
data class LoginRequest(
    @field:NotBlank(message = "El correo es obligatorio")
    val correo: String,

    @field:NotBlank(message = "La contrasena es obligatoria")
    val contrasena: String
)

data class RecuperarContrasenaRequest(
    @field:NotBlank(message = "El correo es obligatorio")
    @field:Email(message = "El formato del correo no es válido")
    val correo: String
)

data class RestablecerContrasenaRequest(
    @field:NotBlank(message = "El token es requerido")
    val token: String,

    @field:NotBlank(message = "La nueva contraseña es obligatoria")
    @field:Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
    val nuevaContrasena: String
)

data class EditarPerfilRequest(
    @field:NotBlank(message = "El nombre no puede estar vacío")
    val nombre: String
)

data class CambiarContrasenaRequest(
    @field:NotBlank(message = "La contraseña actual es requerida")
    val contrasenaActual: String,

    @field:NotBlank(message = "La nueva contraseña es requerida")
    @field:Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
    val nuevaContrasena: String
)

data class UsuarioResponse(
    val id: String,
    val nombre: String,
    val correo: String
)

data class AuthResponse(
    val token: String,
    val tipo: String = "Bearer",
    val expiraEnSegundos: Long,
    val usuario: UsuarioResponse
)

data class ErrorResponse(
    val mensaje: String,
    val campos: Map<String, String>? = null
)
