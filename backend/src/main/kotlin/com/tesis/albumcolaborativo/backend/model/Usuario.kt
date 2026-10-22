package com.tesis.albumcolaborativo.backend.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "usuarios")
class Usuario(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    val id: UUID? = null,

    @Column(name = "nombre", nullable = false, length = 100)
    var nombre: String,

    @Column(name = "correo", nullable = false, unique = true, length = 255)
    var correo: String,

    @Column(name = "contrasena_hash", nullable = false, length = 255)
    var contrasenaHash: String
)
