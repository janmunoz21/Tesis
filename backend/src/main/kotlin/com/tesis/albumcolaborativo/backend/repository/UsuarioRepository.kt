package com.tesis.albumcolaborativo.backend.repository

import com.tesis.albumcolaborativo.backend.model.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UsuarioRepository : JpaRepository<Usuario, UUID> {
    fun findByCorreo(correo: String): Usuario?
    fun existsByCorreo(correo: String): Boolean
}
