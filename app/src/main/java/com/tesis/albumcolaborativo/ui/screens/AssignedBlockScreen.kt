package com.tesis.albumcolaborativo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tesis.albumcolaborativo.ui.viewmodels.AssignedBlockViewModel

@Composable
fun AssignedBlockScreen(
    albumId: String,
    albumName: String = "Álbum Evento Javeriana",
    photoCount: Int = 10,
    estimatedTimeSec: Int = 3,
    onAcceptBlock: () -> Unit = {},
    // Se llama cuando el bloque ya fue reasignado o devuelto a la cola
    onRejectBlock: () -> Unit = {},
    viewModel: AssignedBlockViewModel = viewModel()
) {
    val context = LocalContext.current
    val isRejecting by viewModel.isRejecting.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Solicitud de Cómputo P2P",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64FFDA),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "El álbum '$albumName' te ha asignado un bloque de procesamiento P2P.",
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Métricas del Bloque
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$photoCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB388FF))
                        Text(text = "Fotos", fontSize = 12.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "~$estimatedTimeSec seg", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB388FF))
                        Text(text = "Tiempo Est.", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Botones de Decisión (CU-D-03)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Sin descubrimiento P2P todavía: no hay otros nodos a quién reasignar
                            viewModel.rejectBlock(albumId, nodes = emptyList()) { result ->
                                val message = result.fold(
                                    onSuccess = { blocks ->
                                        if (blocks.isEmpty())
                                            "Bloque devuelto a la cola: se reasignará cuando haya otro nodo disponible"
                                        else
                                            "Bloque reasignado a ${blocks.size} nodo(s)"
                                    },
                                    onFailure = { "Error al rechazar el bloque: ${it.message}" }
                                )
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                if (result.isSuccess) onRejectBlock()
                            }
                        },
                        enabled = !isRejecting,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Text(if (isRejecting) "Reasignando..." else "Rechazar")
                    }

                    Button(
                        onClick = onAcceptBlock,
                        enabled = !isRejecting,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64FFDA))
                    ) {
                        Text("Aceptar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}