package com.tesis.albumcolaborativo.data.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object CameraImageFiles {

    /**
     * Crea un archivo temporal en caché y devuelve su Uri del FileProvider
     * para que la app de cámara guarde ahí la foto (CU-D-01).
     */
    fun createImageUri(context: Context): Uri {

        val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }

        val imageFile = File(cameraDir, "camera_${UUID.randomUUID()}.jpg")

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }
}
