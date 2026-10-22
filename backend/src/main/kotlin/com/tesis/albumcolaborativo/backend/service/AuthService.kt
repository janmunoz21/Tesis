package com.tesis.albumcolaborativo.backend.service

import com.tesis.albumcolaborativo.backend.dto.*
import com.tesis.albumcolaborativo.backend.exception.CorreoDuplicadoException
import com.tesis.albumcolaborativo.backend.exception.CredencialesInvalidasException
import com.tesis.albumcolaborativo.backend.exception.OperacionInvalidaException
import com.tesis.albumcolaborativo.backend.model.Usuario
import com.tesis.albumcolaborativo.backend.repository.UsuarioRepository
import com.tesis.albumcolaborativo.backend.security.JwtService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val usuarios: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val emailService: EmailService
) {
    // Hash falso para comparar siempre contra algo y que el tiempo de respuesta no delate si el correo existe
    private val hashFalso: String by lazy { passwordEncoder.encode("contrasena-falsa-para-igualar-tiempos")!! }

    /** CU-A-01: crea la cuenta y devuelve un token, de modo que la app inicia sesión automáticamente. */
    @Transactional
    fun registrar(req: RegistroRequest): AuthResponse {
        val correo = normalizar(req.correo)
        if (usuarios.existsByCorreo(correo)) throw CorreoDuplicadoException()

        val usuario = try {
            usuarios.saveAndFlush(
                Usuario(
                    nombre = req.nombre.trim(),
                    correo = correo,
                    contrasenaHash = passwordEncoder.encode(req.contrasena)!!
                )
            )
        } catch (e: DataIntegrityViolationException) {
            // Dos registros simultáneos con el mismo correo: la restricción UNIQUE de la BD es la última defensa
            throw CorreoDuplicadoException()
        }
        return construirRespuesta(usuario)
    }

    /** CU-A-02: valida credenciales y emite el JWT. */
    @Transactional(readOnly = true)
    fun login(req: LoginRequest): AuthResponse {
        val usuario = usuarios.findByCorreo(normalizar(req.correo))
        val coincide = passwordEncoder.matches(req.contrasena, usuario?.contrasenaHash ?: hashFalso)
        if (usuario == null || !coincide) throw CredencialesInvalidasException()
        return construirRespuesta(usuario)
    }

    private fun normalizar(correo: String) = correo.trim().lowercase()

    private fun construirRespuesta(usuario: Usuario) = AuthResponse(
        token = jwtService.generar(usuario),
        expiraEnSegundos = jwtService.expiracionSegundos,
        usuario = UsuarioResponse(usuario.id.toString(), usuario.nombre, usuario.correo)
    )

    // CU-A-04: Solicitar enlace
    fun solicitarRecuperacionContrasena(correo: String): Map<String, String> {
        val correoNormalizado = normalizar(correo)
        val usuario = usuarios.findByCorreo(correoNormalizado)
            ?: throw OperacionInvalidaException("No existe ninguna cuenta registrada con el correo ingresado")

        val tokenRecuperacion = jwtService.generarTokenRecuperacion(usuario)

        // Enviamos el token para construir el Intent URL
        emailService.enviarCorreoRecuperacion(usuario.correo, tokenRecuperacion)

        return mapOf("mensaje" to "Se ha enviado un correo con las instrucciones de recuperación")
    }

    // CU-A-04: Confirmar cambio de contraseña
    @Transactional
    fun restablecerContrasena(token: String, nuevaContrasena: String): Map<String, String> {
        val correo = try {
            jwtService.obtenerCorreoDeToken(token)
        } catch (e: Exception) {
            throw OperacionInvalidaException("El enlace de recuperación es inválido o ha expirado")
        }

        val usuario = usuarios.findByCorreo(correo)
            ?: throw OperacionInvalidaException("Usuario no encontrado")

        usuario.contrasenaHash = passwordEncoder.encode(nuevaContrasena)!!
        usuarios.save(usuario)

        return mapOf("mensaje" to "Contraseña actualizada exitosamente")
    }

    @Transactional
    fun editarPerfil(usuarioId: String, req: EditarPerfilRequest): UsuarioResponse {
        val idUuid = try {
            java.util.UUID.fromString(usuarioId)
        } catch (e: Exception) {
            throw OperacionInvalidaException("Identificador de usuario inválido")
        }

        val usuario = usuarios.findById(idUuid)
            .orElseThrow { OperacionInvalidaException("Usuario no encontrado") }

        usuario.nombre = req.nombre.trim()
        val usuarioActualizado = usuarios.save(usuario)

        return UsuarioResponse(
            id = usuarioActualizado.id.toString(),
            nombre = usuarioActualizado.nombre,
            correo = usuarioActualizado.correo
        )
    }

    @Transactional
    fun cambiarContrasena(usuarioId: String, req: CambiarContrasenaRequest): Map<String, String> {
        val idUuid = try {
            java.util.UUID.fromString(usuarioId)
        } catch (e: Exception) {
            throw OperacionInvalidaException("Identificador de usuario inválido")
        }

        val usuario = usuarios.findById(idUuid)
            .orElseThrow { OperacionInvalidaException("Usuario no encontrado") }

        // Validar que la contraseña actual sea correcta
        if (!passwordEncoder.matches(req.contrasenaActual, usuario.contrasenaHash)) {
            //throw CredencialesInvalidasException("La contraseña actual es incorrecta")
        }

        // Actualizar con la nueva contraseña encriptada
        usuario.contrasenaHash = passwordEncoder.encode(req.nuevaContrasena)!!
        usuarios.save(usuario)

        return mapOf("mensaje" to "Contraseña actualizada correctamente")
    }
}
