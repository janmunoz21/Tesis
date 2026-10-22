package com.tesis.albumcolaborativo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.albumcolaborativo.data.model.Album
import com.tesis.albumcolaborativo.data.network.NetworkResult
import com.tesis.albumcolaborativo.data.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AlbumUiState {
    object Idle : AlbumUiState()
    object Loading : AlbumUiState()
    data class Success(val albumes: List<Album>) : AlbumUiState()
    data class Error(val message: String) : AlbumUiState()
}

class AlbumViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AlbumRepository(application)

    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)

    private val _albumDetalle = MutableStateFlow<com.tesis.albumcolaborativo.data.model.AlbumDetalleDto?>(null)

    val albumDetalle: StateFlow<com.tesis.albumcolaborativo.data.model.AlbumDetalleDto?> = _albumDetalle.asStateFlow()
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    init {
        cargarMisAlbumes()
    }

    fun cargarMisAlbumes() {
        viewModelScope.launch {
            _uiState.value = AlbumUiState.Loading
            when (val result = repository.obtenerMisAlbumes()) {
                is NetworkResult.Success -> {
                    _uiState.value = AlbumUiState.Success(result.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = AlbumUiState.Error(result.message)
                }
            }
        }
    }

    fun crearAlbum(nombre: String, descripcion: String?, onExito: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AlbumUiState.Loading
            when (val result = repository.crearAlbum(nombre, descripcion)) {
                is NetworkResult.Success -> {
                    cargarMisAlbumes()
                    onExito()
                }
                is NetworkResult.Error -> {
                    _uiState.value = AlbumUiState.Error(result.message)
                }
            }
        }
    }

    fun unirseAAlbum(token: String, onExito: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AlbumUiState.Loading
            when (val result = repository.unirsePorToken(token)) {
                is NetworkResult.Success -> {
                    cargarMisAlbumes()
                    onExito()
                }
                is NetworkResult.Error -> {
                    _uiState.value = AlbumUiState.Error(result.message)
                }
            }
        }
    }

    fun editarAlbum(albumId: String, nuevoNombre: String, nuevaDescripcion: String, onExito: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AlbumUiState.Loading
            when (val result = repository.editarAlbum(albumId, nuevoNombre, nuevaDescripcion)) {
                is NetworkResult.Success -> {
                    cargarMisAlbumes()
                    onExito()
                }
                is NetworkResult.Error -> {
                    _uiState.value = AlbumUiState.Error(result.message)
                }
            }
        }
    }

    fun eliminarAlbum(albumId: String, onExito: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AlbumUiState.Loading
            when (val result = repository.eliminarAlbum(albumId)) {
                is NetworkResult.Success -> {
                    cargarMisAlbumes()
                    onExito()
                }
                is NetworkResult.Error -> {
                    _uiState.value = AlbumUiState.Error(result.message)
                }
            }
        }
    }

    fun salirDeAlbum(albumId: String, onExito: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AlbumUiState.Loading
            when (val result = repository.salirDeAlbum(albumId)) {
                is NetworkResult.Success -> {
                    cargarMisAlbumes() // Actualiza la lista en PostgreSQL
                    onExito()
                }
                is NetworkResult.Error -> {
                    _uiState.value = AlbumUiState.Error(result.message)
                }
            }
        }
    }

    fun invitarParticipante(albumId: String, correo: String, onResultado: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.invitarParticipante(albumId, correo)) {
                is NetworkResult.Success -> {
                    onResultado(true, result.data)
                }
                is NetworkResult.Error -> {
                    onResultado(false, result.message)
                }
            }
        }
    }

    fun removerParticipante(albumId: String, usuarioId: String, onExito: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = AlbumUiState.Loading
            when (val result = repository.removerParticipante(albumId, usuarioId)) {
                is NetworkResult.Success -> {
                    cargarMisAlbumes()
                    onExito(result.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = AlbumUiState.Error(result.message)
                }
            }
        }
    }

    fun aceptarInvitacion(albumId: String, onExito: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AlbumUiState.Loading
            when (val result = repository.aceptarInvitacion(albumId)) {
                is NetworkResult.Success -> {
                    cargarMisAlbumes() // Recarga los álbumes para incluir el nuevo en la lista
                    onExito()
                }
                is NetworkResult.Error -> {
                    _uiState.value = AlbumUiState.Error(result.message)
                }
            }
        }
    }

    fun rechazarInvitacion(albumId: String, onExito: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AlbumUiState.Loading
            when (val result = repository.rechazarInvitacion(albumId)) {
                is NetworkResult.Success -> {
                    cargarMisAlbumes()
                    onExito()
                }
                is NetworkResult.Error -> {
                    _uiState.value = AlbumUiState.Error(result.message)
                }
            }
        }
    }

    private val _invitaciones = MutableStateFlow<List<Album>>(emptyList())
    val invitaciones: StateFlow<List<Album>> = _invitaciones.asStateFlow()

    fun cargarInvitacionesPendientes() {
        viewModelScope.launch {
            when (val result = repository.obtenerInvitacionesPendientes()) {
                is NetworkResult.Success -> _invitaciones.value = result.data
                is NetworkResult.Error -> _invitaciones.value = emptyList()
            }
        }
    }

    fun cargarDetalleAlbum(albumId: String) {
        viewModelScope.launch {
            when (val result = repository.obtenerDetalleAlbum(albumId)) {
                is NetworkResult.Success -> {
                    _albumDetalle.value = result.data
                }
                is NetworkResult.Error -> {
                    _albumDetalle.value = null
                }
            }
        }
    }
}
