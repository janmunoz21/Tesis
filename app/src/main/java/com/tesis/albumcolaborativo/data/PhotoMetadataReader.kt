package com.tesis.albumcolaborativo.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.tesis.albumcolaborativo.data.model.Photo

/**
 * Lee nombre, tamaño y tipo de las fotos elegidas en el selector del sistema.
 */
class PhotoMetadataReader(private val contentResolver: ContentResolver) {

    fun read(uri: Uri): Photo {
        var name = uri.lastPathSegment ?: "foto"
        var size = 0L
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
        return Photo(
            uri = uri.toString(),
            name = name,
            sizeBytes = size,
            mimeType = contentResolver.getType(uri)
        )
    }
}
