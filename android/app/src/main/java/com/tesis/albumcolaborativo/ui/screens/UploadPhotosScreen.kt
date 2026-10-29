package com.tesis.albumcolaborativo.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
    onBlockAssigned: (photoCount: Int) -> Unit = {},
    viewModel: UploadPhotosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Launcher para seleccionar fotos de la galería
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addPhotos(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subir Fotografías", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { photoPickerLauncher.launch("image/*") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar fotos")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Info Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFE3F2FD).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(40.dp)
                                .background(Color(0xFF2196F3), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Selecciona las fotos del evento. Las imágenes se ajustarán automáticamente a 224x224 px para la clasificación con MobileNetV3.",
                            fontSize = 13.sp,
                            color = Color(0xFF1976D2),
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Área para seleccionar de galería
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8F9FA),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.5f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Toca aquí para seleccionar de la galería",
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Seleccionadas (${uiState.selectedUris.size}/200)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Grid de fotos seleccionadas
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

                // Error
                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Botón Encolar / Procesar
                Button(
                    onClick = {
                        viewModel.processAndEnqueueBatch(albumId) {
                            // Se ejecuta cuando termina el lote de fotos local
                            val count = viewModel.uiState.value.processedCount
                            onBlockAssigned(if (count > 0) count else viewModel.uiState.value.selectedUris.size)
                        }
                    },
                    enabled = uiState.selectedUris.isNotEmpty() && !uiState.isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = if (uiState.isProcessing)
                            "Procesando (${uiState.processedCount}/${uiState.selectedUris.size})..."
                        else
                            "Procesar y Clasificar (${uiState.selectedUris.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
fun SelectedPhotoCard(uri: Uri, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEAEEF2))
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
                .size(28.dp)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(14.dp))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
