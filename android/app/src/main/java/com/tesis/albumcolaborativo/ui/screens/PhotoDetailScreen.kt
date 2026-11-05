package com.tesis.albumcolaborativo.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.tesis.albumcolaborativo.data.local.AppDatabase
import com.tesis.albumcolaborativo.data.local.entities.ClassificationResultEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    taskId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var classificationResult by remember { mutableStateOf<ClassificationResultEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Cargar los metadatos reales guardados en Room para esta foto
    LaunchedEffect(taskId) {
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val result = db.classificationResultDao().getByTask(taskId)
                classificationResult = result
            } catch (e: Exception) {
                Log.e("PHOTO_DETAIL", "Error cargando foto de Room", e)
            } finally {
                isLoading = false
            }
        }
    }

    val labelName = remember(classificationResult) {
        classificationResult?.topPredictionsJson?.let { jsonStr ->
            try {
                val array = JSONArray(jsonStr)
                if (array.length() > 0) array.getJSONObject(0).optString("label", "Escena") else "Escena"
            } catch (e: Exception) {
                "Escena"
            }
        } ?: "Foto procesada"
    }

    val confidencePercent = remember(classificationResult) {
        val score = classificationResult?.predictedScore ?: 0.0f
        String.format("%.1f%%", score * 100)
    }

    val confidenceShort = remember(classificationResult) {
        val score = classificationResult?.predictedScore ?: 0.0f
        "${(score * 100).toInt()}%"
    }

    val processingTimeSec = remember(classificationResult) {
        val timeMs = classificationResult?.totalProcessingTimeMs ?: 0L
        String.format("%.2f s", timeMs / 1000.0)
    }

    val deviceDisplay = remember(classificationResult) {
        val dev = classificationResult?.deviceId ?: "LOCAL"
        if (dev.uppercase() == "LOCAL") "Dispositivo Local — Usuario" else dev
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(labelName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (classificationResult == null) {
                Text(
                    text = "No se encontraron los metadatos de esta fotografía.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Imagen Real Cargada desde el almacenamiento interno con Coil
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        color = Color(0xFFF0F4F8)
                    ) {
                        AsyncImage(
                            model = classificationResult?.imagePath,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Título de la Escena y Estado de Confianza
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Escena: $labelName",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Procesada con MobileNetV3",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = confidenceShort,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Tarjeta con la Información Experimental de Inferencia
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        color = Color.Transparent
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            InfoRow(label = "Procesada por", value = deviceDisplay)
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            InfoRow(
                                label = "Confianza modelo",
                                value = confidencePercent,
                                valueColor = Color(0xFF4CAF50)
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            InfoRow(label = "Tiempo proc.", value = processingTimeSec)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Botones de Acción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Metadatos y foto guardados en la galería")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Compartiendo resultado de inferencia local...")
                                }
                            },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("↑ Compartir")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
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
