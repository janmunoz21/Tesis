package com.tesis.albumcolaborativo.data.repository

import android.content.Context
import com.tesis.albumcolaborativo.data.mapper.toUI
import com.tesis.albumcolaborativo.data.model.Album
import com.tesis.albumcolaborativo.data.model.AlbumResumenDto
import com.tesis.albumcolaborativo.data.model.CrearAlbumRequest
import com.tesis.albumcolaborativo.data.model.UnirseAlbumRequest
import com.tesis.albumcolaborativo.data.network.NetworkResult
import com.tesis.albumcolaborativo.data.network.RetrofitClient
import java.util.ArrayList

class AlbumRepository(context: Context) {

    private val api = RetrofitClient.getApiService(context)

    suspend fun obtenerMisAlbumes(): NetworkResult<List<Album>> {
        return try {
            val response = api.misAlbumes()
            val body: List<AlbumResumenDto>? = response.body()
            if (response.isSuccessful && body != null) {
                val albumesUI = ArrayList<Album>()
                for (dto in body) {
                    albumesUI.add(dto.toUI())
                }
                NetworkResult.Success(albumesUI)
            } else {
                NetworkResult.Error("Error del servidor (${response.code()}): ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red inesperado")
        }
    }

    suspend fun crearAlbum(nombre: String, descripcion: String?): NetworkResult<Unit> {
        return try {
            val response = api.crearAlbum(CrearAlbumRequest(nombre, descripcion))
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error("No se pudo crear el album")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red inesperado")
        }
    }

    suspend fun unirsePorToken(tokenInvitacion: String): NetworkResult<Album> {
        return try {
            val response = api.unirsePorToken(UnirseAlbumRequest(tokenInvitacion))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                NetworkResult.Success(body.toUI())
            } else {
                NetworkResult.Error("Codigo de invitacion invalido")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red inesperado")
        }
    }

    suspend fun editarAlbum(albumId: String, nuevoNombre: String, nuevaDescripcion: String?): NetworkResult<Unit> {
        return try {
            val response = api.editarAlbum(albumId, com.tesis.albumcolaborativo.data.model.EditarAlbumRequest(nuevoNombre, nuevaDescripcion))
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error("No se pudo editar el álbum (${response.code()})")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red inesperado")
        }
    }

    suspend fun eliminarAlbum(albumId: String): NetworkResult<Unit> {
        return try {
            val response = api.eliminarAlbum(albumId)
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error("No se pudo eliminar el álbum (${response.code()})")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red inesperado")
        }
    }

    suspend fun salirDeAlbum(albumId: String): NetworkResult<Unit> {
        return try {
            val response = api.salirDeAlbum(albumId)
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error("No se pudo salir del álbum (${response.code()})")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red inesperado")
        }
    }

    suspend fun invitarParticipante(albumId: String, correo: String): NetworkResult<String> {
        return try {
            val response = api.invitarParticipante(albumId, com.tesis.albumcolaborativo.data.model.InvitarUsuarioRequest(correo))
            if (response.isSuccessful) {
                val mensaje = response.body()?.get("mensaje") ?: "Invitación enviada exitosamente"
                NetworkResult.Success(mensaje)
            } else {
                NetworkResult.Error("No se pudo enviar la invitación (${response.code()})")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red inesperado")
        }
    }

    suspend fun removerParticipante(albumId: String, usuarioId: String): NetworkResult<String> {
        return try {
            val response = api.removerParticipante(albumId, com.tesis.albumcolaborativo.data.model.RemoverParticipanteRequest(usuarioId))
            if (response.isSuccessful) {
                val mensaje = response.body()?.get("mensaje") ?: "Participante removido"
                NetworkResult.Success(mensaje)
            } else {
                NetworkResult.Error("No se pudo remover al participante (${response.code()})")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red inesperado")
        }
    }

    suspend fun aceptarInvitacion(albumId: String): NetworkResult<String> {
        return try {
            val response = api.aceptarInvitacion(albumId)
            if (response.isSuccessful) {
                val msj = response.body()?.get("mensaje") ?: "Invitación aceptada"
                NetworkResult.Success(msj)
            } else {
                NetworkResult.Error("Error al aceptar invitación")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red")
        }
    }

    suspend fun rechazarInvitacion(albumId: String): NetworkResult<String> {
        return try {
            val response = api.rechazarInvitacion(albumId)
            if (response.isSuccessful) {
                val msj = response.body()?.get("mensaje") ?: "Invitación rechazada"
                NetworkResult.Success(msj)
            } else {
                NetworkResult.Error("Error al rechazar invitación")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red")
        }
    }

    suspend fun obtenerInvitacionesPendientes(): NetworkResult<List<Album>> {
        return try {
            val response = api.misInvitacionesPendientes()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                NetworkResult.Success(body.map { it.toUI() })
            } else {
                NetworkResult.Error("Error al cargar invitaciones")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red")
        }
    }

    suspend fun obtenerDetalleAlbum(albumId: String): NetworkResult<com.tesis.albumcolaborativo.data.model.AlbumDetalleDto> {
        return try {
            val response = api.obtenerDetalleAlbum(albumId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                NetworkResult.Success(body)
            } else {
                NetworkResult.Error("No se pudo obtener el detalle del álbum (${response.code()})")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red inesperado")
        }
    }
}
