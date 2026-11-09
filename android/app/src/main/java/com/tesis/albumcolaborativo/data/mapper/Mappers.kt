package com.tesis.albumcolaborativo.data.mapper

import com.tesis.albumcolaborativo.data.model.Album
import com.tesis.albumcolaborativo.data.model.AlbumResumenDto
import com.tesis.albumcolaborativo.data.model.User
import com.tesis.albumcolaborativo.data.model.UsuarioDto

fun UsuarioDto.toUI(): User {
    return User(
        id = this.id,
        name = this.nombre,
        email = this.correo,
        isAutoNode = true,
        credits = 0.0
    )
}

fun AlbumResumenDto.toUI(): Album {
    return Album(
        id = this.id,
        title = this.nombre,
        description = this.descripcion ?: "",
        photoCount = this.totalFotos.toInt(),
        status = "Listo",
        isOwner = (this.miRol == "CREADOR")
    )
}