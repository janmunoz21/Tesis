package com.tesis.albumcolaborativo.data.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ImagePreprocessor(private val context: Context) {

    /**
     * Redimensiona la imagen a 224x224 px en segundo plano
     * requerida para la capa de entrada de MobileNetV3.
     */
    suspend fun resizeImageTo224x224(uri: Uri): File? =
        withContext(Dispatchers.IO) {

            try {

                val originalBitmap =
                    context.contentResolver
                        .openInputStream(uri)
                        ?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        }
                        ?: return@withContext null

                val resizedBitmap = Bitmap.createScaledBitmap(
                    originalBitmap,
                    224,
                    224,
                    true
                )

                if (resizedBitmap != originalBitmap) {
                    originalBitmap.recycle()
                }

                val photoId = UUID.randomUUID().toString()

                val outputFile = File(
                    context.filesDir,
                    "prepared_$photoId.jpg"
                )

                FileOutputStream(outputFile).use { outputStream ->

                    resizedBitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        90,
                        outputStream
                    )
                }

                resizedBitmap.recycle()

                outputFile

            } catch (e: Exception) {

                e.printStackTrace()

                null
            }
        }
}