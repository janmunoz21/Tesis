package com.tesis.albumcolaborativo.backend.security

import org.springframework.security.core.Authentication
import java.util.UUID

/** Id del usuario autenticado (el filtro JWT guarda el UUID como principal). */
fun Authentication.usuarioId(): UUID = this.principal as UUID
