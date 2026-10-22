package com.tesis.albumcolaborativo.backend.controller

import com.tesis.albumcolaborativo.backend.dto.*
import com.tesis.albumcolaborativo.backend.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    fun registro(@Valid @RequestBody req: RegistroRequest): AuthResponse = authService.registrar(req)

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): AuthResponse = authService.login(req)

    @PostMapping("/recuperar-contrasena")
    fun recuperarContrasena(@Valid @RequestBody request: RecuperarContrasenaRequest): ResponseEntity<Map<String, String>> {
        val respuesta = authService.solicitarRecuperacionContrasena(request.correo)
        return ResponseEntity.ok(respuesta)
    }

    @PostMapping("/restablecer-contrasena")
    fun restablecerContrasena(@Valid @RequestBody request: RestablecerContrasenaRequest): ResponseEntity<Map<String, String>> {
        val respuesta = authService.restablecerContrasena(request.token, request.nuevaContrasena)
        return ResponseEntity.ok(respuesta)
    }

    @PutMapping("/perfil")
    fun editarPerfil(
        @Valid @RequestBody request: EditarPerfilRequest,
        authentication: org.springframework.security.core.Authentication
    ): ResponseEntity<UsuarioResponse> {
        val usuarioId = authentication.name // Extraído automáticamente del Token JWT
        val respuesta = authService.editarPerfil(usuarioId, request)
        return ResponseEntity.ok(respuesta)
    }

    @PutMapping("/cambiar-contrasena")
    fun cambiarContrasena(
        @Valid @RequestBody request: CambiarContrasenaRequest,
        authentication: org.springframework.security.core.Authentication
    ): ResponseEntity<Map<String, String>> {
        val usuarioId = authentication.name // ID del usuario desde el JWT
        val respuesta = authService.cambiarContrasena(usuarioId, request)
        return ResponseEntity.ok(respuesta)
    }
}
