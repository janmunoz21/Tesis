package com.tesis.albumcolaborativo.backend.security

import com.tesis.albumcolaborativo.backend.model.Usuario
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.expiration-minutes}") private val expiracionMinutos: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))

    val expiracionSegundos: Long get() = expiracionMinutos * 60

    fun generar(usuario: Usuario): String {
        val ahora = Instant.now()
        return Jwts.builder()
            .subject(usuario.id.toString())
            .claim("correo", usuario.correo)
            .issuedAt(Date.from(ahora))
            .expiration(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
            .signWith(key)
            .compact()
    }

    /** Genera un token temporal de 15 minutos exclusivo para la recuperación de contraseña */
    fun generarTokenRecuperacion(usuario: Usuario): String {
        val ahora = Instant.now()
        return Jwts.builder()
            .subject(usuario.id.toString())
            .claim("correo", usuario.correo)
            .claim("tipo", "RECUPERACION")
            .issuedAt(Date.from(ahora))
            .expiration(Date.from(ahora.plus(15, ChronoUnit.MINUTES)))
            .signWith(key)
            .compact()
    }

    /** Extrae el correo del token si es válido y de tipo RECUPERACION */
    fun obtenerCorreoDeToken(token: String): String {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        return claims.get("correo", String::class.java)
            ?: throw IllegalArgumentException("El token no contiene un correo válido")
    }

    /** Devuelve el id del usuario si el token es válido y no ha expirado; null en caso contrario. */
    fun validar(token: String): UUID? = try {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        UUID.fromString(claims.subject)
    } catch (e: JwtException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}
