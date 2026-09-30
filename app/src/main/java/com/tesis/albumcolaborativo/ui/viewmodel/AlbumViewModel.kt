package com.tesis.albumcolaborativo.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.tesis.albumcolaborativo.data.AlbumRepository
import com.tesis.albumcolaborativo.data.model.Album
import com.tesis.albumcolaborativo.data.model.Photo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AlbumViewModel(
    private val repository: AlbumRepository = AlbumRepository()
) : ViewModel() {

    val albums: StateFlow<List<Album>> = repository.albums

    /** Álbum al que se están subiendo fotos (el último creado). */
    private val _currentAlbumId = MutableStateFlow<String?>(null)
    val currentAlbumId: StateFlow<String?> = _currentAlbumId.asStateFlow()

    /** Fotos elegidas en "Subir fotos" que aún no se han confirmado. */
    private val _pendingPhotos = MutableStateFlow<List<Photo>>(emptyList())
    val pendingPhotos: StateFlow<List<Photo>> = _pendingPhotos.asStateFlow()

    fun isValidName(name: String): Boolean = name.isNotBlank()

    fun createAlbum(name: String, description: String): Album? {
        if (!isValidName(name)) return null
        val album = repository.createAlbum(name, description)
        _currentAlbumId.value = album.id
        _pendingPhotos.value = emptyList()
        return album
    }

    /**
     * Agrega fotos a la selección pendiente. Descarta duplicados y las que superan
     * [MAX_PHOTO_SIZE_BYTES]. Devuelve cuántas se rechazaron por tamaño.
     */
    fun addPendingPhotos(photos: List<Photo>): Int {
        val (valid, tooLarge) = photos.partition { it.sizeBytes <= MAX_PHOTO_SIZE_BYTES }
        _pendingPhotos.update { current ->
            val existing = current.map { it.uri }.toSet()
            current + valid.filter { it.uri !in existing }.distinctBy { it.uri }
        }
        return tooLarge.size
    }

    fun removePendingPhoto(photo: Photo) {
        _pendingPhotos.update { current -> current.filterNot { it.uri == photo.uri } }
    }

    /** Guarda las fotos pendientes en el álbum actual. */
    fun confirmPendingPhotos(): Boolean {
        val albumId = _currentAlbumId.value ?: return false
        if (_pendingPhotos.value.isEmpty()) return false
        repository.addPhotos(albumId, _pendingPhotos.value)
        _pendingPhotos.value = emptyList()
        return true
    }

    companion object {
        const val MAX_PHOTO_SIZE_BYTES = 20L * 1024 * 1024 // 20 MB
    }
}
