package com.tesis.albumcolaborativo.backend.service

import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(private val mailSender: JavaMailSender) {

    fun enviarCorreoRecuperacion(destino: String, tokenRecuperacion: String) {
        val mensaje = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(mensaje, true, "UTF-8")

        helper.setTo(destino)
        helper.setSubject("Restablecer Contraseña - Álbum Colaborativo")

        // Formato de enlace Intent aceptado por Gmail y Chrome en Android
        val enlaceIntent = "intent://reset-password?token=$tokenRecuperacion#Intent;scheme=albumcolaborativo;package=com.tesis.albumcolaborativo;end"

        val contenidoHtml = """
        <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
            <h2>Restablecimiento de Contraseña</h2>
            <p>Has solicitado restablecer tu contraseña para la aplicación <b>Álbum Colaborativo</b>.</p>
            <p>Haz clic en el siguiente botón desde tu teléfono Android para ingresar tu nueva contraseña:</p>
            <br>
            <a href="$enlaceIntent" style="background-color: #1E7898; color: white; padding: 14px 24px; text-decoration: none; border-radius: 8px; display: inline-block; font-weight: bold;">Restablecer Contraseña</a>
            <br><br>
            <p><small>Este enlace vencerá en 15 minutos.</small></p>
        </div>
    """.trimIndent()

        helper.setText(contenidoHtml, true)
        mailSender.send(mensaje)
    }
}