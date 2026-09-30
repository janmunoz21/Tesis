package com.tesis.albumcolaborativo.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tesis.albumcolaborativo.data.PhotoMetadataReader
import com.tesis.albumcolaborativo.data.model.Photo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadPhotosScreen(
    onNavigateBack: () -> Unit,
    onContinue: () -> Unit,
    albumTitle: String? = null,
    photos: List<Photo> = emptyList(),
    onPhotosPicked: (List<Photo>) -> Int = { 0 },
    onRemovePhoto: (Photo) -> Unit = {}
) {
    val context = LocalContext.current
    val metadataReader = remember(context) { PhotoMetadataReader(context.contentResolver) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Selector de fotos del sistema: no requiere permisos de almacenamiento
    val pickPhotos = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val rejected = onPhotosPicked(uris.map(metadataReader::read))
        if (rejected > 0) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (rejected == 1) "1 foto supera los 20 MB y no se agregó"
                    else "$rejected fotos superan los 20 MB y no se agregaron"
                )
            }
        }
    }
    val openPicker: () -> Unit = {
        pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Subir fotos", fontWeight = FontWeight.Bold)
                        if (albumTitle != null) {
                            Text(albumTitle, fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
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
                        text = "Selecciona las fotos que tomaste en el evento. El sistema las distribuirá entre los dispositivos del grupo para procesarlas.",
                        fontSize = 14.sp,
                        color = Color(0xFF1976D2),
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Selection Area
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clickable(onClick = openPicker),
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
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Seleccionar de galería",
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Máximo 20 MB por foto",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                if (photos.size == 1) "Seleccionada (1 foto)" else "Seleccionadas (${photos.size} fotos)",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Photo Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(photos, key = { it.uri }) { photo ->
                    PhotoThumbnail(photo, onRemove = { onRemovePhoto(photo) })
                }
                item {
                    Surface(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable(onClick = openPicker),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0F2F5)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }
            }

            Button(
                onClick = onContinue,
                enabled = photos.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("Continuar", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PhotoThumbnail(photo: Photo, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFEAEEF2)
    ) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = photo.uri,
                contentDescription = photo.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onRemove),
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Quitar ${photo.name}",
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                color = Color.Gray.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    photo.mimeType?.substringAfter('/')?.uppercase() ?: "IMG",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    fontSize = 8.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
