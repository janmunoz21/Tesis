package com.tesis.albumcolaborativo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesis.albumcolaborativo.data.network.NetworkResult
import com.tesis.albumcolaborativo.data.repository.AlbumRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    onNavigateBack: () -> Unit,
    albumId: String = "demo-album",
    fotoId: String = "demo-foto"
) {
    val context = LocalContext.current
    val repository = remember { AlbumRepository(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var currentCategory by remember { mutableStateOf("nematode, nematode worm, roundworm") }
    var showReclassifyDialog by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }
    var isReclassifying by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Foto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Main Image Placeholder
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp)),
                color = Color(0xFFE3F2FD)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🖼️", fontSize = 120.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Scene Title and Reclassify Action (CU-E-04)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Escena: $currentCategory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Subida por Miembro · Inferencia MobileNetV3",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                IconButton(onClick = { 
                    newCategoryText = currentCategory
                    showReclassifyDialog = true 
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Reclasificar foto manual",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Processing Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = Color.Transparent
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    InfoRow(label = "Procesada por", value = "Nodo P2P / Local")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    InfoRow(label = "Confianza modelo", value = "94.2%", valueColor = Color(0xFF4CAF50))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    InfoRow(label = "Tiempo de inferencia", value = "48 ms")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Foto guardada en la galería")
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar")
                }
                Button(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Abriendo opciones de compartir...")
                        }
                    },
                    modifier = Modifier.weight(1.4f).height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartir")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Dialog de Reclasificación Manual (CU-E-04 / CU-D-05)
        if (showReclassifyDialog) {
            AlertDialog(
                onDismissRequest = { if (!isReclassifying) showReclassifyDialog = false },
                title = { Text("Reclasificar Foto (Manual)") },
                text = {
                    Column {
                        Text("Modifica la etiqueta predicha por la IA si consideras que es incorrecta:")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newCategoryText,
                            onValueChange = { newCategoryText = it },
                            label = { Text("Nueva Etiqueta") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = newCategoryText.isNotBlank() && !isReclassifying,
                        onClick = {
                            isReclassifying = true
                            scope.launch {
                                val result = repository.reclasificarFoto(albumId, fotoId, newCategoryText.trim())
                                isReclassifying = false
                                showReclassifyDialog = false
                                when (result) {
                                    is NetworkResult.Success -> {
                                        currentCategory = newCategoryText.trim()
                                        snackbarHostState.showSnackbar("Etiqueta actualizada correctamente en el servidor")
                                    }
                                    is NetworkResult.Error -> {
                                        snackbarHostState.showSnackbar(result.message)
                                    }
                                }
                            }
                        }
                    ) {
                        if (isReclassifying) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        } else {
                            Text("Guardar Cambios")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isReclassifying,
                        onClick = { showReclassifyDialog = false }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
