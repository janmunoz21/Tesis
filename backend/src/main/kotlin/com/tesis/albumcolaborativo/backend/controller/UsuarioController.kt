package com.tesis.albumcolaborativo.backend.controller

import com.tesis.albumcolaborativo.backend.dto.CambiarContrasenaRequest
import com.tesis.albumcolaborativo.backend.dto.EditarPerfilRequest
import com.tesis.albumcolaborativo.backend.dto.UsuarioResponse
import com.tesis.albumcolaborativo.backend.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/usuarios") // 👈 Ruta protegida independiente de /api/auth
class UsuarioController(private val authService: AuthService) {

    @PutMapping("/perfil")
    fun editarPerfil(
        @Valid @RequestBody request: EditarPerfilRequest,
        authentication: Authentication
    ): ResponseEntity<UsuarioResponse> {
        val usuarioId = authentication.name
        val respuesta = authService.editarPerfil(usuarioId, request)
        return ResponseEntity.ok(respuesta)
    }

    @PutMapping("/cambiar-contrasena")
    fun cambiarContrasena(
        @Valid @RequestBody request: CambiarContrasenaRequest,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val usuarioId = authentication.name
        val respuesta = authService.cambiarContrasena(usuarioId, request)
        return ResponseEntity.ok(respuesta)
    }
}
