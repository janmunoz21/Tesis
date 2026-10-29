package com.tesis.albumcolaborativo.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.albumcolaborativo.data.local.AppDatabase
import com.tesis.albumcolaborativo.data.local.entities.ClassificationResultEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class CategoryGroup(
    val categoryName: String,
    val photos: List<ClassificationResultEntity>
)

data class AlbumCategoriesUiState(
    val categories: List<CategoryGroup> = emptyList(),
    val totalPhotos: Int = 0,
    val isLoading: Boolean = true
)

class AlbumCategoriesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val resultDao = database.classificationResultDao()

    private val _uiState = MutableStateFlow(AlbumCategoriesUiState())
    val uiState: StateFlow<AlbumCategoriesUiState> = _uiState.asStateFlow()

    fun cargarFotosYCategorias(albumId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            withContext(Dispatchers.IO) {
                // 1. Sanitizar ID
                val cleanAlbumId = albumId.replace("\"", "").trim()

                Log.d("ALBUM_CATEGORIES", "Buscando fotos en Room para albumId: '$cleanAlbumId'")

                // 2. Probar primero con el ID limpio
                var results = resultDao.getResultsForAlbum(cleanAlbumId)

                // 3. Fallback: Si no encuentra resultados, probar con el ID tal como llegó de la navegación
                if (results.isEmpty() && cleanAlbumId != albumId) {
                    Log.w("ALBUM_CATEGORIES", "Sin resultados para id limpio. Intentando con id original: '$albumId'")
                    results = resultDao.getResultsForAlbum(albumId)
                }

                Log.d("ALBUM_CATEGORIES", "Total fotos recuperadas de Room: ${results.size}")

                if (results.isNotEmpty()) {
                    val groupedMap = results.groupBy { result ->
                        extraerEtiquetaPrincipal(result.topPredictionsJson)
                    }

                    val categoryGroups = groupedMap.map { (category, photos) ->
                        CategoryGroup(
                            categoryName = category,
                            photos = photos
                        )
                    }.sortedByDescending { it.photos.size }

                    _uiState.value = AlbumCategoriesUiState(
                        categories = categoryGroups,
                        totalPhotos = results.size,
                        isLoading = false
                    )
                } else {
                    _uiState.value = AlbumCategoriesUiState(
                        categories = emptyList(),
                        totalPhotos = 0,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun extraerEtiquetaPrincipal(topPredictionsJson: String): String {
        return try {
            val jsonArray = JSONArray(topPredictionsJson)
            if (jsonArray.length() > 0) {
                val firstObj = jsonArray.getJSONObject(0)
                val label = firstObj.optString("label", "Sin categoría")
                label.ifBlank { "Sin categoría" }
            } else {
                "Sin categoría"
            }
        } catch (e: Exception) {
            "Gente / General"
        }
    }
}