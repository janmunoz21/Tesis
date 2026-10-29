package com.tesis.albumcolaborativo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignedBlockScreen(
    albumName: String = "Vacaciones Cartagena",
    photoCount: Int = 30,
    estimatedTimeSec: Int = 45,
    onNavigateBack: () -> Unit,
    onAcceptAndProcess: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bloque Asignado", fontWeight = FontWeight.Bold) },
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
            Spacer(modifier = Modifier.height(16.dp))

            // Box Informativa
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFE3F2FD).copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(60.dp)
                            .background(Color(0xFF2196F3), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Se ha asignado a tu dispositivo un bloque de $photoCount fotos para clasificar localmente del álbum '$albumName'.",
                        fontSize = 14.sp,
                        color = Color(0xFF1976D2),
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tarjeta de Detalles del Bloque
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    StatRow("Álbum", albumName, isTitle = true)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                    StatRow("Fotos en tu bloque", "$photoCount fotos", valueColor = Color(0xFF2196F3))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                    StatRow("Tiempo estimado", "~$estimatedTimeSec seg")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Muestra del bloque",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cuadrícula de vista previa
            val sampleEmojis = listOf("🏖️", "🌅", "👨‍👩‍👧", "🍹", "🌊")
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sampleEmojis.size) { index ->
                    SamplePhotoItem(sampleEmojis[index])
                }
                item {
                    Surface(
                        modifier = Modifier.aspectRatio(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE3F2FD)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "+${if (photoCount > 5) photoCount - 5 else 0}",
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            // Acciones Inferiores
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEA4335)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEA4335))
                ) {
                    Text("Rechazar", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onAcceptAndProcess,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("Aceptar y procesar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, isTitle: Boolean = false, valueColor: Color = Color.Black) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = valueColor
        )
    }
}

@Composable
fun SamplePhotoItem(emoji: String) {
    Surface(
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFEAEEF2)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 24.sp)
        }
    }
}
