package com.tesis.albumcolaborativo.backend.service

import com.tesis.albumcolaborativo.backend.dto.*
import com.tesis.albumcolaborativo.backend.exception.AlbumNoEncontradoException
import com.tesis.albumcolaborativo.backend.exception.OperacionInvalidaException
import com.tesis.albumcolaborativo.backend.exception.PermisoDenegadoException
import com.tesis.albumcolaborativo.backend.model.Album
import com.tesis.albumcolaborativo.backend.model.EstadoMiembro
import com.tesis.albumcolaborativo.backend.model.MiembroAlbum
import com.tesis.albumcolaborativo.backend.model.MiembroAlbumId
import com.tesis.albumcolaborativo.backend.model.RolAlbum
import com.tesis.albumcolaborativo.backend.repository.AlbumRepository
import com.tesis.albumcolaborativo.backend.repository.FotoMetadatoRepository
import com.tesis.albumcolaborativo.backend.repository.MiembroAlbumRepository
import com.tesis.albumcolaborativo.backend.repository.UsuarioRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.util.UUID

@Service
class AlbumService(
    private val albumes: AlbumRepository,
    private val miembros: MiembroAlbumRepository,
    private val fotos: FotoMetadatoRepository,
    private val usuarios: UsuarioRepository
) {
    private val random = SecureRandom()

    /**
     * CU-B-01: crea el álbum y registra al creador como miembro en UNA sola transacción (SRS 3.6.2).
     */
    @Transactional
    fun crear(usuarioId: UUID, req: CrearAlbumRequest): AlbumDetalleResponse {
        val album = albumes.save(
            Album(
                nombre = req.nombre.trim(),
                descripcion = req.descripcion?.trim()?.takeIf { it.isNotEmpty() },
                creadorId = usuarioId,
                tokenInvitacion = generarTokenUnico()
            )
        )
        val albumId = album.id!!
        miembros.save(
            MiembroAlbum(
                id = MiembroAlbumId(albumId, usuarioId),
                rol = RolAlbum.CREADOR,
                estado = EstadoMiembro.ACTIVO
            )
        )
        return detalle(usuarioId, albumId)
    }

    /**
     * CU-B-02: Invitar a un participante por su correo electrónico.
     */
    @Transactional
    fun invitarParticipante(albumId: UUID, creadorId: UUID, req: InvitarUsuarioRequest): Map<String, String> {
        val album = albumes.findById(albumId)
            .orElseThrow { AlbumNoEncontradoException() }

        if (album.creadorId != creadorId) {
            throw PermisoDenegadoException("Solo el creador puede invitar miembros al álbum")
        }

        val correoLimpio = req.correo.trim().lowercase()

        // Manejo compatible tanto para Usuario? como para Optional<Usuario>
        val usuarioAInvitar = usuarios.findByCorreo(correoLimpio)?.takeIf { true }
            ?: throw OperacionInvalidaException("No existe ningún usuario registrado con el correo ingresado")

        val miembroId = MiembroAlbumId(albumId, usuarioAInvitar.id!!)
        val membresiaExistente = miembros.findById(miembroId)

        if (membresiaExistente.isPresent) {
            val estado = membresiaExistente.get().estado
            if (estado == EstadoMiembro.ACTIVO) {
                throw OperacionInvalidaException("El usuario ya es miembro activo de este álbum")
            } else {
                throw OperacionInvalidaException("Ya existe una invitación pendiente para este usuario")
            }
        }

        miembros.save(
            MiembroAlbum(
                id = miembroId,
                rol = RolAlbum.PARTICIPANTE,
                estado = EstadoMiembro.PENDIENTE
            )
        )

        return mapOf("mensaje" to "Invitación enviada exitosamente a ${usuarioAInvitar.correo}")
    }

    /**
     * CU-B-03 (Opción A): Unirse usando token de invitación.
     */
    @Transactional
    fun unirsePorToken(usuarioId: UUID, req: UnirseAlbumRequest): AlbumResumenResponse {
        val codigoLimpio = req.tokenInvitacion.trim().lowercase()

        // Busca por coincidencia exacta de token O por los últimos 5 caracteres del token / ID
        val album = albumes.findAll().firstOrNull { album ->
            val token = album.tokenInvitacion?.lowercase() ?: ""
            val idStr = album.id.toString().lowercase()

            token == codigoLimpio || token.endsWith(codigoLimpio) || idStr.endsWith(codigoLimpio)
        } ?: throw OperacionInvalidaException("El código de invitación es inválido o no existe")

        val albumId = album.id!!
        val miembroId = MiembroAlbumId(albumId, usuarioId)
        val membresia = miembros.findById(miembroId).orElse(null)

        if (membresia != null) {
            if (membresia.estado == EstadoMiembro.ACTIVO) {
                throw OperacionInvalidaException("Ya eres miembro de este álbum")
            } else {
                membresia.estado = EstadoMiembro.ACTIVO
                miembros.save(membresia)
            }
        } else {
            miembros.save(
                MiembroAlbum(
                    id = miembroId,
                    rol = RolAlbum.PARTICIPANTE,
                    estado = EstadoMiembro.ACTIVO
                )
            )
        }
        val totalFotos = fotos.countByAlbumId(albumId)
        val totalMiembros = miembros.countByIdAlbumIdAndEstado(albumId, EstadoMiembro.ACTIVO)

        return AlbumResumenResponse(
            id = albumId,
            nombre = album.nombre,
            descripcion = album.descripcion,
            fechaCreacion = album.fechaCreacion,
            miRol = RolAlbum.PARTICIPANTE,
            totalFotos = totalFotos,
            totalMiembros = totalMiembros
        )
    }


    /**
     * CU-B-03 (Opción B): Aceptar una invitación previa.
     */
    @Transactional
    fun aceptarInvitacion(albumId: UUID, usuarioId: UUID): Map<String, String> {
        val miembroId = MiembroAlbumId(albumId, usuarioId)
        val miembro = miembros.findById(miembroId)
            .orElseThrow { OperacionInvalidaException("No tienes invitaciones pendientes para este álbum") }

        if (miembro.estado == EstadoMiembro.ACTIVO) {
            throw OperacionInvalidaException("Ya eres un participante activo en este álbum")
        }

        miembro.estado = EstadoMiembro.ACTIVO
        miembros.save(miembro)

        return mapOf("mensaje" to "Invitación aceptada correctamente")
    }

    /**
     * CU-B-04: Rechazar una invitación.
     */
    @Transactional
    fun rechazarInvitacion(albumId: UUID, usuarioId: UUID): Map<String, String> {
        val miembroId = MiembroAlbumId(albumId, usuarioId)
        val miembro = miembros.findById(miembroId)
            .orElseThrow { OperacionInvalidaException("No tienes invitaciones pendientes para este álbum") }

        if (miembro.estado == EstadoMiembro.ACTIVO) {
            throw OperacionInvalidaException("No puedes rechazar un álbum al que ya te has unido. Usa la opción de salir del álbum.")
        }

        miembros.delete(miembro)

        return mapOf("mensaje" to "Invitación rechazada")
    }

    /** CU-B-05: álbumes donde el usuario es miembro activo. */
    @Transactional(readOnly = true)
    fun listar(usuarioId: UUID): List<AlbumResumenResponse> {
        val filas = albumes.findDeUsuario(usuarioId)
        if (filas.isEmpty()) return emptyList()

        val ids = filas.map { (it[0] as Album).id!! }
        val miembrosPorAlbum = miembros.contarPorAlbum(ids).associate { (it[0] as UUID) to (it[1] as Long) }
        val fotosPorAlbum = fotos.contarPorAlbum(ids).associate { (it[0] as UUID) to (it[1] as Long) }

        return filas.map { fila ->
            val album = fila[0] as Album
            val id = album.id!!
            AlbumResumenResponse(
                id = id,
                nombre = album.nombre,
                descripcion = album.descripcion,
                fechaCreacion = album.fechaCreacion,
                miRol = fila[1] as String,
                totalMiembros = miembrosPorAlbum[id] ?: 0L,
                totalFotos = fotosPorAlbum[id] ?: 0L
            )
        }
    }

    @Transactional(readOnly = true)
    fun listarInvitacionesPendientes(usuarioId: UUID): List<AlbumResumenResponse> {
        val albumesPendientes = miembros.findInvitacionesPendientes(usuarioId)
        if (albumesPendientes.isEmpty()) return emptyList()

        return albumesPendientes.map { album ->
            AlbumResumenResponse(
                id = album.id!!,
                nombre = album.nombre,
                descripcion = album.descripcion,
                fechaCreacion = album.fechaCreacion,
                miRol = RolAlbum.PARTICIPANTE,
                totalMiembros = miembros.countByIdAlbumIdAndEstado(album.id!!, EstadoMiembro.ACTIVO),
                totalFotos = fotos.countByAlbumId(album.id!!)
            )
        }
    }

    /** CU-B-06: detalle del álbum con participantes. Solo para miembros activos. */
    @Transactional(readOnly = true)
    fun obtenerDetalle(usuarioId: UUID, albumId: UUID): AlbumDetalleResponse = detalle(usuarioId, albumId)

    private fun detalle(usuarioId: UUID, albumId: UUID): AlbumDetalleResponse {
        // Si no existe o no soy miembro respondemos lo mismo (404): así no se revela que el álbum existe
        val membresia = miembros.findById(MiembroAlbumId(albumId, usuarioId)).orElse(null)
        if (membresia == null || membresia.estado != EstadoMiembro.ACTIVO) throw AlbumNoEncontradoException()

        val album = albumes.findById(albumId).orElseThrow { AlbumNoEncontradoException() }
        val participantes = miembros.participantesDe(albumId).map {
            ParticipanteResponse(usuarioId = it[0] as UUID, nombre = it[1] as String, rol = it[2] as String)
        }

        return AlbumDetalleResponse(
            id = albumId,
            nombre = album.nombre,
            descripcion = album.descripcion,
            fechaCreacion = album.fechaCreacion,
            tokenInvitacion = album.tokenInvitacion,
            creadorId = album.creadorId,
            miRol = membresia.rol,
            totalFotos = fotos.countByAlbumId(albumId),
            participantes = participantes
        )
    }

    /**
     * CU-B-07: Editar información del álbum (nombre/descripción)
     * Solo el CREADOR puede editar el álbum.
     */
    @Transactional
    fun editarAlbum(albumId: UUID, usuarioId: UUID, request: EditarAlbumRequest): AlbumResumenResponse {
        val album = albumes.findById(albumId)
            .orElseThrow { AlbumNoEncontradoException() }

        // Validar que el usuario sea el CREADOR
        if (album.creadorId != usuarioId) {
            throw PermisoDenegadoException("Solo el creador puede editar la información del álbum")
        }

        album.nombre = request.nombre.trim()
        album.descripcion = request.descripcion?.trim()?.takeIf { it.isNotEmpty() }

        val albumActualizado = albumes.save(album)

        val totalFotos = fotos.countByAlbumId(albumId)
        val totalMiembros = miembros.countByIdAlbumIdAndEstado(albumId, EstadoMiembro.ACTIVO)

        return AlbumResumenResponse(
            id = albumActualizado.id!!,
            nombre = albumActualizado.nombre,
            descripcion = albumActualizado.descripcion,
            fechaCreacion = albumActualizado.fechaCreacion,
            miRol = RolAlbum.CREADOR,
            totalFotos = totalFotos,
            totalMiembros = totalMiembros
        )
    }

    /**
     * CU-B-08: Eliminar álbum
     * Solo el CREADOR puede eliminar el álbum.
     */
    @Transactional
    fun eliminarAlbum(albumId: UUID, usuarioId: UUID) {
        val album = albumes.findById(albumId)
            .orElseThrow { AlbumNoEncontradoException() }

        if (album.creadorId != usuarioId) {
            throw PermisoDenegadoException("Solo el creador tiene permisos para eliminar este álbum")
        }

        albumes.delete(album)
    }

    /**
     * CU-B-09 (Parte A): Salir voluntariamente de un álbum
     */
    @Transactional
    fun salirDeAlbum(albumId: UUID, usuarioId: UUID) {
        val album = albumes.findById(albumId)
            .orElseThrow { AlbumNoEncontradoException() }

        if (album.creadorId == usuarioId) {
            throw OperacionInvalidaException("El creador no puede salir del álbum. Si deseas cerrarlo, debes eliminarlo.")
        }

        val miembroId = MiembroAlbumId(albumId, usuarioId)
        val miembro = miembros.findById(miembroId)
            .orElseThrow { PermisoDenegadoException("No perteneces a este álbum") }

        miembros.delete(miembro)
    }

    /**
     * CU-B-09 (Parte B): Remover a un participante (solo el creador)
     */
    @Transactional
    fun removerParticipante(albumId: UUID, creadorId: UUID, usuarioARemoverId: UUID) {
        val album = albumes.findById(albumId)
            .orElseThrow { AlbumNoEncontradoException() }

        if (album.creadorId != creadorId) {
            throw PermisoDenegadoException("Solo el creador puede remover participantes del álbum")
        }

        if (creadorId == usuarioARemoverId) {
            throw OperacionInvalidaException("No puedes removerte a ti mismo siendo el creador")
        }

        val miembroId = MiembroAlbumId(albumId, usuarioARemoverId)
        val miembroARemover = miembros.findById(miembroId)
            .orElseThrow { OperacionInvalidaException("El usuario a remover no pertenece al álbum") }

        miembros.delete(miembroARemover)
    }

    // Código de 8 caracteres sin letras/números ambiguos (sin 0, O, 1, I) para que sea fácil de dictar o escribir
    private fun generarTokenUnico(): String {
        repeat(5) {
            val token = (1..8).map { ALFABETO[random.nextInt(ALFABETO.length)] }.joinToString("")
            if (!albumes.existsByTokenInvitacion(token)) return token
        }
        throw IllegalStateException("No se pudo generar un código de invitación único")
    }

    private companion object {
        const val ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
