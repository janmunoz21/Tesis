package com.tesis.albumcolaborativo.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProcessingPhotosScreen(onNavigateBack: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "Clasificando tu bloque...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "3 dispositivos procesando en paralelo",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Circular Visualization
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Dashed Circle Background
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.LightGray,
                        style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    )
                }

                // Central Node
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2196F3))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("📸", fontSize = 32.sp)
                    }
                }

                // Surrounding Nodes (Placeholders)
                val nodes = listOf("✓", "JM", "AC", "DV", "MR")

            }

            Spacer(modifier = Modifier.height(64.dp))

            // Progress Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Progreso total del álbum", fontWeight = FontWeight.Bold)
                Text("72%", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { 0.72f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFFE8F5E9)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Participants List
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF8F9FA)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ParticipantProgress("Juan M. (tú)", "30/30 fotos", true, Color(0xFF4CAF50))
                    ParticipantProgress("Andrés C.", "18/30 fotos", false, Color(0xFF2196F3))
                    ParticipantProgress("Diego V.", "26/30 fotos", false, Color(0xFF2196F3))
                    ParticipantProgress("Marco R.", "0/30 fotos", false, Color.Gray, isWaiting = true)
                }
            }
        }
    }
}

@Composable
fun ParticipantProgress(name: String, status: String, isDone: Boolean, color: Color, isWaiting: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDone) {
            Icon(Icons.Default.Check, contentDescription = null, tint = color, modifier = Modifier.size(20.dp).background(color.copy(alpha = 0.2f), CircleShape).padding(2.dp))
        } else if (isWaiting) {
            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        } else {
            Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(name, modifier = Modifier.weight(1f), color = if (isWaiting) Color.Gray else Color.Black)
        
        Text(status, fontWeight = FontWeight.Bold, color = color)
    }
}
