package com.tesis.albumcolaborativo.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.albumcolaborativo.data.local.AppDatabase
import com.tesis.albumcolaborativo.data.local.entities.ClassificationResultEntity
import com.tesis.albumcolaborativo.data.network.NetworkResult
import com.tesis.albumcolaborativo.data.repository.AlbumRepository
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
    private val repository = AlbumRepository(application)

    private val _uiState = MutableStateFlow(AlbumCategoriesUiState())
    val uiState: StateFlow<AlbumCategoriesUiState> = _uiState.asStateFlow()

    fun cargarFotosYCategorias(albumId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            withContext(Dispatchers.IO) {
                val cleanAlbumId = albumId.replace("\"", "").trim()

                Log.d("ALBUM_CATEGORIES", "Buscando fotos en Room para albumId: '$cleanAlbumId'")

                val localResults = resultDao.getResultsForAlbum(cleanAlbumId).toMutableList()

                // Consultar siempre al backend PostgreSQL para consolidar fotos procesadas por otros nodos
                try {
                    when (val backendRes = repository.obtenerMetadatosAlbum(cleanAlbumId)) {
                        is NetworkResult.Success -> {
                            val backendMetadatos = backendRes.data
                            if (backendMetadatos.isNotEmpty()) {
                                val existingTaskIds = localResults.map { it.taskId }.toSet()
                                val remoteResults = backendMetadatos.filter { it.id !in existingTaskIds }.map { meta ->
                                    val fakeJson = JSONArray().apply {
                                        put(org.json.JSONObject().apply {
                                            put("label", meta.etiquetaConsolidada)
                                            put("score", meta.porcentajeConfianza)
                                        })
                                    }.toString()

                                    ClassificationResultEntity(
                                        taskId = meta.id,
                                        experimentId = "REST_BACKEND",
                                        albumId = cleanAlbumId,
                                        imagePath = meta.rutaArchivo,
                                        predictedClassIndex = 0,
                                        predictedScore = meta.porcentajeConfianza.toFloat(),
                                        topPredictionsJson = fakeJson,
                                        preprocessingTimeMs = 0L,
                                        tensorPreparationTimeMs = 0L,
                                        inferenceTimeMs = 0L,
                                        totalProcessingTimeMs = 0L,
                                        deviceId = "BACKEND"
                                    )
                                }
                                remoteResults.forEach { resultDao.insert(it) }
                                localResults.addAll(remoteResults)
                            }
                        }
                        else -> {
                            Log.e("ALBUM_CATEGORIES", "Error consultando metadatos al backend para albumId '$cleanAlbumId'")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ALBUM_CATEGORIES", "Error consultando backend: ${e.message}")
                }

                val finalResults = localResults.distinctBy { it.taskId }

                Log.d("ALBUM_CATEGORIES", "Total fotos recuperadas para '$cleanAlbumId': ${finalResults.size}")

                if (finalResults.isNotEmpty()) {
                    val groupedMap = finalResults.groupBy { result ->
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
                        totalPhotos = finalResults.size,
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
