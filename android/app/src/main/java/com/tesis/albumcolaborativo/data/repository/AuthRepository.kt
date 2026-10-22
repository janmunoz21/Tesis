package com.tesis.albumcolaborativo.data.repository

import android.content.Context
import com.tesis.albumcolaborativo.data.local.TokenManager
import com.tesis.albumcolaborativo.data.mapper.toUI
import com.tesis.albumcolaborativo.data.model.CambiarContrasenaRequest
import com.tesis.albumcolaborativo.data.model.EditarPerfilRequest
import com.tesis.albumcolaborativo.data.model.LoginRequest
import com.tesis.albumcolaborativo.data.model.RecuperarContrasenaRequest
import com.tesis.albumcolaborativo.data.model.RegistroRequest
import com.tesis.albumcolaborativo.data.model.User
import com.tesis.albumcolaborativo.data.network.NetworkResult
import com.tesis.albumcolaborativo.data.network.RetrofitClient

class AuthRepository(context: Context) {

    private val api = RetrofitClient.getApiService(context)
    private val tokenManager = TokenManager(context)

    suspend fun login(email: String, clave: String): NetworkResult<String> {
        return try {
            val response = api.login(LoginRequest(email, clave))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val token = body.token
                tokenManager.guardarToken(token)

                // Guardar nombre real devuelto por el servidor si viene disponible
                val nombre = body.usuario?.nombre ?: email.substringBefore("@")
                val correoReal = body.usuario?.correo ?: email
                tokenManager.guardarUsuario(nombre, correoReal)

                NetworkResult.Success(token)
            } else {
                NetworkResult.Error("Credenciales incorrectas")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error al conectar")
        }
    }

    suspend fun registrar(nombre: String, correo: String, contrasena: String): NetworkResult<User> {
        return try {
            val response = api.registrar(RegistroRequest(nombre, correo, contrasena))
            if (response.isSuccessful && response.body() != null) {
                val authData = response.body()!!
                tokenManager.guardarToken(authData.token)
                tokenManager.guardarUsuario(nombre, correo)

                val user = authData.usuario?.toUI() ?: User(
                    name = nombre,
                    email = correo
                )
                NetworkResult.Success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error de validación o servidor"
                NetworkResult.Error("Error (${response.code()}): $errorMsg")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red inesperado")
        }
    }

    fun estaAutenticado(): Boolean {
        val token = tokenManager.obtenerToken()
        return !token.isNullOrEmpty()
    }

    fun cerrarSesion() {
        tokenManager.eliminarToken()
    }

    suspend fun recuperarContrasena(correo: String): NetworkResult<String> {
        return try {
            val response = api.recuperarContrasena(RecuperarContrasenaRequest(correo))
            if (response.isSuccessful) {
                val mensaje = response.body()?.get("mensaje") ?: "Enlace enviado al correo"
                NetworkResult.Success(mensaje)
            } else {
                val errorBodyStr = response.errorBody()?.string()
                val mensajeError = try {
                    if (!errorBodyStr.isNullOrEmpty()) {
                        org.json.JSONObject(errorBodyStr).optString("mensaje", "Error al procesar la solicitud")
                    } else {
                        "Error del servidor (${response.code()})"
                    }
                } catch (e: Exception) {
                    "No existe una cuenta registrada con ese correo"
                }
                NetworkResult.Error(mensajeError)
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red inesperado")
        }
    }

    suspend fun restablecerContrasena(token: String, nuevaContrasena: String): NetworkResult<String> {
        return try {
            val response = api.restablecerContrasena(com.tesis.albumcolaborativo.data.model.RestablecerContrasenaRequest(token, nuevaContrasena))
            if (response.isSuccessful) {
                val mensaje = response.body()?.get("mensaje") ?: "Contraseña actualizada exitosamente"
                NetworkResult.Success(mensaje)
            } else {
                val errorBodyStr = response.errorBody()?.string()
                val mensajeError = try {
                    if (!errorBodyStr.isNullOrEmpty()) {
                        org.json.JSONObject(errorBodyStr).optString("mensaje", "Error al restablecer la contraseña")
                    } else {
                        "Error del servidor (${response.code()})"
                    }
                } catch (e: Exception) {
                    "El enlace de recuperación es inválido o ha expirado"
                }
                NetworkResult.Error(mensajeError)
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red inesperado")
        }
    }

    suspend fun editarPerfil(nuevoNombre: String): NetworkResult<User> {
        return try {
            val response = api.editarPerfil(EditarPerfilRequest(nuevoNombre))
            if (response.isSuccessful && response.body() != null) {
                val userDto = response.body()!!
                tokenManager.guardarUsuario(userDto.nombre, userDto.correo)

                val user = userDto.toUI()
                NetworkResult.Success(user)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error al actualizar perfil"
                NetworkResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red inesperado")
        }
    }

    suspend fun cambiarContrasena(actual: String, nueva: String): NetworkResult<String> {
        return try {
            val response = api.cambiarContrasena(CambiarContrasenaRequest(actual, nueva))
            if (response.isSuccessful) {
                val mensaje = response.body()?.get("mensaje") ?: "Contraseña actualizada"
                NetworkResult.Success(mensaje)
            } else {
                val errorBodyStr = response.errorBody()?.string()
                val mensajeError = try {
                    if (!errorBodyStr.isNullOrEmpty()) {
                        org.json.JSONObject(errorBodyStr).optString("mensaje", "La contraseña actual es incorrecta")
                    } else {
                        "Error al cambiar contraseña (${response.code()})"
                    }
                } catch (e: Exception) {
                    "La contraseña actual es incorrecta"
                }
                NetworkResult.Error(mensajeError)
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error de red inesperado")
        }
    }
}