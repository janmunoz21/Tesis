package com.tesis.albumcolaborativo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tesis.albumcolaborativo.data.distribution.ProcessingNode
import com.tesis.albumcolaborativo.ui.viewmodel.AlbumViewModel
import com.tesis.albumcolaborativo.ui.viewmodel.P2PNetworkViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAlbumScreen(
    viewModel: AlbumViewModel = viewModel(),
    p2pViewModel: P2PNetworkViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToUpload: (albumId: String, albumName: String) -> Unit = { _, _ -> }
) {
    var albumName by remember { mutableStateOf("Vacaciones Cartagena 2026") }
    var description by remember { mutableStateOf("") }

    val p2pState by p2pViewModel.uiState.collectAsState()
    val invitedNodes = remember { mutableStateListOf<String>() }

    val isNameValid = albumName.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo álbum", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Nombre del álbum", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            TextField(
                value = albumName,
                onValueChange = { albumName = it },
                placeholder = { Text("Ej: Vacaciones Cartagena 2026") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Descripción (opcional)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            TextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Ej: Viaje con los amigos del trabajo") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // SECCIÓN: DISPOSITIVOS CERCANOS EN WI-FI (mDNS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dispositivos cercanos en tu Wi-Fi",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "mDNS Activo",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (p2pState.nodes.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Buscando otros celulares en tu misma red Wi-Fi...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    p2pState.nodes.forEach { node ->
                        val isInvited = invitedNodes.contains(node.nodeId)
                        NearbyDeviceItem(
                            node = node,
                            isInvited = isInvited,
                            onToggleInvite = {
                                if (isInvited) {
                                    invitedNodes.remove(node.nodeId)
                                } else {
                                    invitedNodes.add(node.nodeId)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val scope = rememberCoroutineScope()

            Button(
                onClick = {
                    if (isNameValid) {
                        viewModel.crearAlbum(albumName.trim(), description.trim()) { newAlbumId ->
                            // Enviar invitación P2P instantánea con el albumId REAL
                            val invitedDevices = p2pState.nodes.filter { invitedNodes.contains(it.nodeId) }
                            scope.launch {
                                val p2pClient = com.tesis.albumcolaborativo.data.network.p2p.GrpcP2PClient()
                                invitedDevices.forEach { device ->
                                    p2pClient.sendP2PInvite(
                                        targetAddress = device.address,
                                        targetPort = device.port,
                                        albumId = newAlbumId,
                                        albumName = albumName.trim(),
                                        inviterName = p2pState.localDeviceName,
                                        senderPort = com.tesis.albumcolaborativo.data.network.p2p.P2PManager.getServicePort()
                                    )
                                }
                                onNavigateToUpload(newAlbumId, albumName.trim())
                            }
                        }
                    }
                },
                enabled = isNameValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Crear álbum e invitar seleccionados", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun NearbyDeviceItem(
    node: ProcessingNode,
    isInvited: Boolean,
    onToggleInvite: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.deviceName.ifBlank { node.nodeId },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "IP: ${node.address} · Red Local Wi-Fi",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Button(
                onClick = onToggleInvite,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isInvited) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primary,
                    contentColor = if (isInvited) Color(0xFF2E7D32) else Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (isInvited) Icons.Default.Check else Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isInvited) "Invitado" else "+ Invitar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
