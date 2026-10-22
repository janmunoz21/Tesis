package com.tesis.albumcolaborativo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.albumcolaborativo.data.network.NetworkResult
import com.tesis.albumcolaborativo.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun iniciarSesion(correo: String, clave: String, onExito: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when (val result = authRepository.login(correo, clave)) {
                is NetworkResult.Success -> {
                    _isLoading.value = false
                    onExito()
                }
                is NetworkResult.Error -> {
                    _isLoading.value = false
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun registrar(nombre: String, email: String, clave: String, onExito: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = authRepository.registrar(nombre, email, clave)) {
                is NetworkResult.Success -> {
                    _isLoading.value = false
                    onExito()
                }
                is NetworkResult.Error -> {
                    _isLoading.value = false
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun solicitarRecuperacionContrasena(correo: String, onResultado: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val result = authRepository.recuperarContrasena(correo)) {
                is NetworkResult.Success -> onResultado(true, result.data)
                is NetworkResult.Error -> onResultado(false, result.message)
            }
        }
    }

    fun restablecerContrasena(token: String, nuevaContrasena: String, onResultado: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val result = authRepository.restablecerContrasena(token, nuevaContrasena)) {
                is NetworkResult.Success -> onResultado(true, result.data)
                is NetworkResult.Error -> onResultado(false, result.message)
            }
        }
    }

    fun editarPerfil(nuevoNombre: String, onResultado: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = authRepository.editarPerfil(nuevoNombre)) {
                is NetworkResult.Success -> {
                    _isLoading.value = false
                    onResultado(true, null)
                }
                is NetworkResult.Error -> {
                    _isLoading.value = false
                    onResultado(false, result.message)
                }
            }
        }
    }

    fun cambiarContrasena(actual: String, nueva: String, onResultado: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val result = authRepository.cambiarContrasena(actual, nueva)) {
                is NetworkResult.Success -> onResultado(true, result.data)
                is NetworkResult.Error -> onResultado(false, result.message)
            }
        }
    }

}