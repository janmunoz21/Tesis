package com.tesis.albumcolaborativo.backend.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

class CorreoDuplicadoException : RuntimeException("Ya existe una cuenta con ese correo")

// Mensaje genérico a propósito: no revela si el correo existe o si falló la contraseña
class CredencialesInvalidasException : RuntimeException("Correo o contraseña incorrectos")

// Se usa tanto si el álbum no existe como si el usuario no es miembro: no se revela cuál de los dos es
class AlbumNoEncontradoException : RuntimeException("Álbum no encontrado")

@ResponseStatus(HttpStatus.FORBIDDEN)
class PermisoDenegadoException(mensaje: String = "No tienes permisos para realizar esta acción") : RuntimeException(mensaje)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class OperacionInvalidaException(mensaje: String) : RuntimeException(mensaje)
