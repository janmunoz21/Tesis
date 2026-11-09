package com.tesis.albumcolaborativo.data.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class ImagePreprocessor(private val context: Context) {

    companion object {
        private const val TAG = "ImagePreprocessor"
    }

    /**
     * Redimensiona la imagen a 224x224 px en segundo plano
     * requerida para la capa de entrada de MobileNetV3.
     */
    suspend fun resizeImageTo224x224(uri: Uri): File? =
        withContext(Dispatchers.IO) {
            try {
                // Si la URI apunta a un archivo local en disco
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: "")
                    if (file.exists()) {
                        return@withContext processFile(file)
                    }
                }

                // 1. Obtener dimensiones sin cargar en memoria
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                
                openInputStreamSafe(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, boundsOptions)
                }

                var sampleSize = 1
                while (boundsOptions.outWidth / sampleSize > 512 || boundsOptions.outHeight / sampleSize > 512) {
                    sampleSize *= 2
                }

                // 2. Decodificar la imagen optimizada
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }

                val originalBitmap = openInputStreamSafe(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                } ?: run {
                    Log.e(TAG, "No se pudo abrir InputStream para la URI: $uri")
                    return@withContext null
                }

                // 3. Redimensionar exactamente a 224x224 px
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
                Log.e(TAG, "Error procesando imagen $uri: ${e.message}", e)
                null
            }
        }

    private fun processFile(file: File): File? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

            var sampleSize = 1
            while (boundsOptions.outWidth / sampleSize > 512 || boundsOptions.outHeight / sampleSize > 512) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val originalBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 224, 224, true)
            if (resizedBitmap != originalBitmap) originalBitmap.recycle()

            val photoId = UUID.randomUUID().toString()
            val outputFile = File(context.filesDir, "prepared_$photoId.jpg")

            FileOutputStream(outputFile).use { outputStream ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
            resizedBitmap.recycle()
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando archivo ${file.absolutePath}: ${e.message}")
            null
        }
    }

    private fun openInputStreamSafe(uri: Uri): InputStream? {
        return try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al abrir Stream para $uri: ${e.message}")
            null
        }
    }
}
