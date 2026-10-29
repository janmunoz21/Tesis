package com.tesis.albumcolaborativo.data.network

import com.tesis.albumcolaborativo.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- MÓDULO A: AUTENTICACIÓN ---
    @POST("api/auth/registro")
    suspend fun registrar(@Body request: RegistroRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/recuperar-contrasena")
    suspend fun recuperarContrasena(
        @Body request: RecuperarContrasenaRequest
    ): Response<Map<String, String>>

    @POST("api/auth/restablecer-contrasena")
    suspend fun restablecerContrasena(
        @Body request: RestablecerContrasenaRequest
    ): Response<Map<String, String>>

    // En ApiService.kt (Android)

    @PUT("api/usuarios/perfil")
    suspend fun editarPerfil(
        @Body request: EditarPerfilRequest
    ): Response<UsuarioDto> // O tu DTO de Usuario equivalente

    @PUT("api/usuarios/cambiar-contrasena")
    suspend fun cambiarContrasena(
        @Body request: CambiarContrasenaRequest
    ): Response<Map<String, String>>

    // --- MÓDULO B: ÁLBUMES ---
    @POST("api/albumes")
    suspend fun crearAlbum(@Body request: CrearAlbumRequest): Response<AlbumDetalleDto>

    @GET("api/albumes")
    suspend fun misAlbumes(): Response<List<AlbumResumenDto>>

    @GET("api/albumes/{id}")
    suspend fun obtenerDetalleAlbum(@Path("id") id: String): Response<AlbumDetalleDto>

    @PUT("api/albumes/{id}")
    suspend fun editarAlbum(
        @Path("id") id: String,
        @Body request: EditarAlbumRequest
    ): Response<AlbumResumenDto>

    @DELETE("api/albumes/{id}")
    suspend fun eliminarAlbum(@Path("id") id: String): Response<Map<String, String>>

    @POST("api/albumes/{id}/invitar")
    suspend fun invitarParticipante(
        @Path("id") id: String,
        @Body request: InvitarUsuarioRequest
    ): Response<Map<String, String>>

    @GET("api/albumes/invitaciones")
    suspend fun misInvitacionesPendientes(): Response<List<AlbumResumenDto>>

    @POST("api/albumes/unirse")
    suspend fun unirsePorToken(@Body request: UnirseAlbumRequest): Response<AlbumResumenDto>

    @POST("api/albumes/{id}/aceptar-invitacion")
    suspend fun aceptarInvitacion(@Path("id") id: String): Response<Map<String, String>>

    @POST("api/albumes/{id}/rechazar-invitacion")
    suspend fun rechazarInvitacion(@Path("id") id: String): Response<Map<String, String>>

    @POST("api/albumes/{id}/salir")
    suspend fun salirDeAlbum(@Path("id") id: String): Response<Map<String, String>>

    @POST("api/albumes/{id}/remover-participante")
    suspend fun removerParticipante(
        @Path("id") id: String,
        @Body request: RemoverParticipanteRequest
    ): Response<Map<String, String>>

    // --- MÓDULO D: SINCRONIZACIÓN DE PROCESAMIENTO E IA ---
    @POST("api/albums/{id}/sincronizar-resultados")
    suspend fun sincronizarResultados(
        @Path("id") albumId: String,
        @Body request: SyncAlbumResultsDto
    ): Response<Map<String, String>>

}