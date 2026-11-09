package com.tesis.albumcolaborativo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import java.util.Locale
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tesis.albumcolaborativo.data.distribution.ProcessingNode
import com.tesis.albumcolaborativo.ui.components.BottomNavigationBar
import com.tesis.albumcolaborativo.ui.viewmodel.P2PNetworkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupP2PNetworkScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: P2PNetworkViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showManualIpDialog by remember { mutableStateOf(false) }
    var manualIpInput by remember { mutableStateOf("") }
    var manualLabelInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nodos del grupo (mDNS / gRPC)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = { 
            BottomNavigationBar(
                currentScreen = "p2p",
                onNavigateToHome = onNavigateToHome,
                onNavigateToProfile = onNavigateToProfile
            ) 
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Switch para visibilidad / Modo Nodo Automático (CU-C-02)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Visibilidad P2P en Wi-Fi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (uiState.isNodeActive) 
                                "Publicando servicio mDNS en puerto 50051" 
                                else "Nodo oculto (modo manual desactivado)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isNodeActive,
                        onCheckedChange = { active ->
                            viewModel.toggleNodeAvailability(active)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Box sobre nodos descubiertos
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isNodeActive) Color(0xFF4CAF50) else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (uiState.nodes.isEmpty()) {
                            "Escaneando subred Wi-Fi con mDNS..."
                        } else {
                            "Descubiertos ${uiState.nodes.size} nodos P2P en tu red Wi-Fi."
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dispositivos descubiertos en Wi-Fi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showManualIpDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Conectar por IP", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.nodes.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Buscando otros celulares en la misma Wi-Fi...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.nodes.forEach { node ->
                        P2PNodeItem(node)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CARD DE MÉTRICAS DE RENDIMIENTO Y EVALUACIÓN (1 vs N DISPOSITIVOS)
            Text("Métricas de Rendimiento", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            PerformanceMetricsCard(
                avgInferenceMs = uiState.avgInferenceTimeMs,
                speedupFactor = uiState.speedupFactor,
                nodeCount = uiState.p2pNodesCount + 1,
                processedCount = uiState.processedCount
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showManualIpDialog) {
            AlertDialog(
                onDismissRequest = { showManualIpDialog = false },
                title = { Text("Conectar Nodo P2P por IP") },
                text = {
                    Column {
                        Text("Ingresa la dirección IP del otro celular o del emulador (ej: 192.168.80.17):")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = manualIpInput,
                            onValueChange = { manualIpInput = it },
                            label = { Text("Dirección IP") },
                            placeholder = { Text("192.168.80.17") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualLabelInput,
                            onValueChange = { manualLabelInput = it },
                            label = { Text("Nombre del Dispositivo (Opcional)") },
                            placeholder = { Text("Motorola One Hyper / Emulador") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = manualIpInput.isNotBlank(),
                        onClick = {
                            viewModel.addManualNode(manualIpInput, manualLabelInput)
                            showManualIpDialog = false
                            manualIpInput = ""
                            manualLabelInput = ""
                        }
                    ) {
                        Text("Agregar Nodo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualIpDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun P2PNodeItem(node: ProcessingNode) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.deviceName.ifBlank { node.nodeId },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "IP: ${node.address}:${node.port}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = if (node.available) Color(0xFF4CAF50) else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = when {
                        node.isProcessing -> Color(0xFFE3F2FD)
                        node.available -> Color(0xFFE8F5E9)
                        else -> Color(0xFFF5F5F5)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when {
                            node.isProcessing -> "Procesando"
                            node.available -> "Disponible"
                            else -> "Offline"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = when {
                            node.isProcessing -> Color(0xFF1976D2)
                            node.available -> Color(0xFF2E7D32)
                            else -> Color.Gray
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PerformanceMetricsCard(
    avgInferenceMs: Long,
    speedupFactor: Float,
    nodeCount: Int,
    processedCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MetricRow(
                label = "T. Inferencia MobileNetV3 (Promedio)",
                value = "$avgInferenceMs ms / foto",
                valueColor = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MetricRow(
                label = "Dispositivos Activos en Red",
                value = "$nodeCount nodos",
                valueColor = MaterialTheme.colorScheme.onSurface
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MetricRow(
                label = "Aceleración Rendimiento (Speedup)",
                value = String.format(Locale.getDefault(), "%.1fx más rápido", speedupFactor),
                valueColor = Color(0xFF4CAF50)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MetricRow(
                label = "Fotos Procesadas en Local",
                value = "$processedCount fotos",
                valueColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MetricRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
