package com.tesis.albumcolaborativo.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.tesis.albumcolaborativo.ui.viewmodels.UploadPhotosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadPhotosScreen(
    albumId: String,
    onNavigateBack: () -> Unit = {},
    // CU-D-02 → CU-D-03: recibe la cantidad de fotos del bloque asignado a este dispositivo
    onBlockAssigned: (photoCount: Int) -> Unit = {},
    viewModel: UploadPhotosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Photo Picker nativo con límite de 200 fotos (SRS CU-D-01)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val limitedUris = uris.take(200)
            viewModel.addPhotos(limitedUris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subir Fotografías", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    photoPickerLauncher.launch(
                        arrayOf("image/*")
                    )
                },
                containerColor = Color(0xFF64FFDA)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar fotos", tint = Color.Black)
            }
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Lote de Fotografías (${uiState.selectedUris.size}/200)",
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = "Las imágenes se adaptarón automáticamente a 224x224 px para MobileNetV3",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.selectedUris) { uri ->
                        SelectedPhotoCard(uri = uri, onRemove = { viewModel.removePhoto(uri) })
                    }
                }

                Button(
                    onClick = {
                        viewModel.processAndEnqueueBatch(albumId) {
                            // Sin nodos P2P todavía: el TaskDistributor asigna todo al nodo LOCAL
                            viewModel.distributePendingTasks(albumId, nodes = emptyList()) { blocks ->
                                onBlockAssigned(blocks.sumOf { it.tasks.size })
                            }
                        }
                    },
                    enabled = uiState.selectedUris.isNotEmpty() && !uiState.isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB388FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (uiState.isProcessing)
                            "Preprocesando (${uiState.processedCount}/${uiState.selectedUris.size})..."
                        else
                            "Encolar y Distribuir (${uiState.selectedUris.size})",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }

            if (uiState.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF64FFDA)
                )
            }
        }
    }
}

@Composable
fun SelectedPhotoCard(uri: Uri, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1B2E))
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.White)
        }
    }
}