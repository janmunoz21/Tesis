package com.tesis.albumcolaborativo.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tesis.albumcolaborativo.ui.viewmodels.ProcessingPhotosViewModel

private val Blue = Color(0xFF2196F3)
private val Green = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingPhotosScreen(
    albumId: String,
    onNavigateBack: () -> Unit = {},
    onProcessingComplete: () -> Unit = {},
    viewModel: ProcessingPhotosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(albumId) {
        viewModel.observeAlbum(albumId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Procesamiento de Fotos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = when {
                    uiState.totalTasks == 0 -> "No hay tareas en este álbum"
                    uiState.isComplete -> "Procesamiento completado"
                    uiState.completedTasks > 0 -> "Procesamiento en curso..."
                    else -> "Tareas preparadas para procesar"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Álbum ID: $albumId",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(32.dp))

            // Indicador Central Animado / Visual
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.LightGray,
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(10f, 10f),
                                0f
                            )
                        )
                    )
                }

                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        Blue
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("📸", fontSize = 32.sp)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Barra de Progreso
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Progreso del álbum",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${uiState.progressPercent}%",
                    color = Blue,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape),
                color = Green,
                trackColor = Color(0xFFE8F5E9)
            )

            Spacer(Modifier.height(24.dp))

            // Tarjeta con métricas de la base de datos
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF8F9FA)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Estado de las imágenes",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    StatusRow(
                        label = "Total de imágenes",
                        count = uiState.totalTasks,
                        color = Color.Black
                    )

                    StatusRow(
                        label = "Pendientes",
                        count = uiState.pendingTasks,
                        color = Color(0xFFED9D25)
                    )

                    StatusRow(
                        label = "Asignadas",
                        count = uiState.assignedTasks,
                        color = Blue
                    )

                    StatusRow(
                        label = "Completadas",
                        count = uiState.completedTasks,
                        color = Green
                    )

                    if (uiState.otherTasks > 0) {
                        StatusRow(
                            label = "Otros estados",
                            count = uiState.otherTasks,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = when {
                    uiState.totalTasks == 0 ->
                        "No se encontraron imágenes guardadas para este álbum."

                    uiState.isComplete ->
                        "Todas las fotos del álbum han sido clasificadas con éxito."

                    else ->
                        "El sistema está clasificando cada foto mediante la red neuronal MobileNetV3."
                },
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onProcessingComplete,
                enabled = uiState.isComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Blue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Ver Álbum Clasificado",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (label == "Completadas" && count > 0) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
        } else if (label == "Pendientes" && count > 0) {
            Icon(
                Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
        }

        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = Color.Black
        )

        Text(
            text = count.toString(),
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
