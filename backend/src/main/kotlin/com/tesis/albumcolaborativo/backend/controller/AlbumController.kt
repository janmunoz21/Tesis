package com.tesis.albumcolaborativo.backend.controller

import com.tesis.albumcolaborativo.backend.dto.*
import com.tesis.albumcolaborativo.backend.security.usuarioId
import com.tesis.albumcolaborativo.backend.service.AlbumService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("/api/albumes")
class AlbumController(private val albumService: AlbumService) {

    // CU-B-01
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun crear(auth: Authentication, @Valid @RequestBody req: CrearAlbumRequest): AlbumDetalleResponse =
        albumService.crear(auth.usuarioId(), req)

    /**
     * CU-B-02: Invitar participante por correo
     * POST /api/albumes/{id}/invitar
     */
    @PostMapping("/{id}/invitar")
    fun invitarParticipante(
        @PathVariable id: UUID,
        @Valid @RequestBody request: InvitarUsuarioRequest,
        auth: Authentication
    ): Map<String, String> =
        albumService.invitarParticipante(id, auth.usuarioId(), request)

    /**
     * CU-B-03 (Opción A): Unirse mediante token de invitación
     * POST /api/albumes/unirse
     */
    @PostMapping("/unirse")
    fun unirsePorToken(
        @Valid @RequestBody request: UnirseAlbumRequest,
        auth: Authentication
    ): AlbumResumenResponse =
        albumService.unirsePorToken(auth.usuarioId(), request)

    /**
     * CU-B-03 (Opción B): Aceptar invitación pendiente
     * POST /api/albumes/{id}/aceptar-invitacion
     */
    @PostMapping("/{id}/aceptar-invitacion")
    fun aceptarInvitacion(
        @PathVariable id: UUID,
        auth: Authentication
    ): Map<String, String> =
        albumService.aceptarInvitacion(id, auth.usuarioId())

    @GetMapping("/invitaciones")
    fun misInvitacionesPendientes(auth: Authentication): List<AlbumResumenResponse> =
        albumService.listarInvitacionesPendientes(auth.usuarioId())

    /**
     * CU-B-04: Rechazar invitación pendiente
     * POST /api/albumes/{id}/rechazar-invitacion
     */
    @PostMapping("/{id}/rechazar-invitacion")
    fun rechazarInvitacion(
        @PathVariable id: UUID,
        auth: Authentication
    ): Map<String, String> =
        albumService.rechazarInvitacion(id, auth.usuarioId())

    // CU-B-05
    @GetMapping
    fun misAlbumes(auth: Authentication): List<AlbumResumenResponse> =
        albumService.listar(auth.usuarioId())

    // CU-B-06
    @GetMapping("/{id}")
    fun detalle(auth: Authentication, @PathVariable id: UUID): AlbumDetalleResponse =
        albumService.obtenerDetalle(auth.usuarioId(), id)

    /**
     * CU-B-07: Editar información del álbum
     * PUT /api/albumes/{id}
     */
    @PutMapping("/{id}")
    fun editarAlbum(
        @PathVariable id: UUID,
        @Valid @RequestBody request: EditarAlbumRequest,
        auth: Authentication
    ): AlbumResumenResponse =
        albumService.editarAlbum(id, auth.usuarioId(), request)

    /**
     * CU-B-08: Eliminar álbum
     * DELETE /api/albumes/{id}
     */
    @DeleteMapping("/{id}")
    fun eliminarAlbum(
        @PathVariable id: UUID,
        auth: Authentication
    ): Map<String, String> {
        albumService.eliminarAlbum(id, auth.usuarioId())
        return mapOf("mensaje" to "Album eliminado correctamente")
    }

    /**
     * CU-B-09 (Parte A): Salir voluntariamente de un álbum
     * POST /api/albumes/{id}/salir
     */
    @PostMapping("/{id}/salir")
    fun salirDeAlbum(
        @PathVariable id: UUID,
        auth: Authentication
    ): Map<String, String> {
        albumService.salirDeAlbum(id, auth.usuarioId())
        return mapOf("mensaje" to "Has salido del album exitosamente")
    }

    /**
     * CU-B-09 (Parte B): Remover participante (solo el creador)
     * POST /api/albumes/{id}/remover-participante
     */
    @PostMapping("/{id}/remover-participante")
    fun removerParticipante(
        @PathVariable id: UUID,
        @RequestBody request: RemoverParticipanteRequest,
        auth: Authentication
    ): Map<String, String> {
        albumService.removerParticipante(id, auth.usuarioId(), request.usuarioId)
        return mapOf("mensaje" to "Participante removido del album")
    }
}
