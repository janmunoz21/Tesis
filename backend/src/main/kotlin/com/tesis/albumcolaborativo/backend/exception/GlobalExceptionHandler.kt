package com.tesis.albumcolaborativo.backend.exception

import com.tesis.albumcolaborativo.backend.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacion(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val campos = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Valor inválido") }
        return ResponseEntity.badRequest().body(ErrorResponse("Datos inválidos", campos))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun cuerpoInvalido(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(ErrorResponse("El cuerpo de la solicitud no es un JSON válido"))

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun parametroInvalido(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(ErrorResponse("Identificador inválido"))

    @ExceptionHandler(CorreoDuplicadoException::class)
    fun duplicado(e: CorreoDuplicadoException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(e.message ?: "Correo duplicado"))

    @ExceptionHandler(CredencialesInvalidasException::class)
    fun credenciales(e: CredencialesInvalidasException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse(e.message ?: "Credenciales inválidas"))

    @ExceptionHandler(AlbumNoEncontradoException::class)
    fun albumNoEncontrado(e: AlbumNoEncontradoException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.message ?: "Álbum no encontrado"))

    @ExceptionHandler(PermisoDenegadoException::class)
    fun handlePermisoDenegado(ex: PermisoDenegadoException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse(ex.message ?: "Acceso denegado"))
    }

    @ExceptionHandler(OperacionInvalidaException::class)
    fun handleOperacionInvalida(ex: OperacionInvalidaException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(ex.message ?: "Operación inválida"))
    }
}
