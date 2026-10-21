package com.tesis.albumcolaborativo.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.tesis.albumcolaborativo.data.AlbumRepository
import com.tesis.albumcolaborativo.data.model.Album
import kotlinx.coroutines.flow.StateFlow

class AlbumViewModel(
    private val repository: AlbumRepository = AlbumRepository()
) : ViewModel() {

    val albums: StateFlow<List<Album>> = repository.albums

    fun isValidName(name: String): Boolean = name.isNotBlank()

    fun createAlbum(name: String, description: String): Album? {
        if (!isValidName(name)) return null
        return repository.createAlbum(name, description)
    }
}
