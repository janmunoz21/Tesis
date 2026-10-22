package com.tesis.albumcolaborativo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.tesis.albumcolaborativo.data.model.Album
import com.tesis.albumcolaborativo.ui.components.BottomNavigationBar
import com.tesis.albumcolaborativo.ui.viewmodel.AlbumUiState
import com.tesis.albumcolaborativo.ui.viewmodel.AlbumViewModel

@Composable
fun HomeScreen(
    viewModel: AlbumViewModel = viewModel(),
    onNavigateToCreateAlbum: () -> Unit = {},
    onNavigateToP2P: () -> Unit = {},
    onNavigateToAlbumDetail: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        viewModel.cargarMisAlbumes()
        viewModel.cargarInvitacionesPendientes() // Carga las invitaciones desde PostgreSQL
    }

    val uiState by viewModel.uiState.collectAsState()
    val invitaciones by viewModel.invitaciones.collectAsState() // Observa la lista de invitaciones

    var showJoinDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    if (showJoinDialog) {
        JoinAlbumDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { token ->
                viewModel.unirseAAlbum(token) {
                    showJoinDialog = false
                }
            }
        )
    }

    // CU-B-09: Diálogo de Invitaciones Pendientes con el parámetro 'invitaciones'
    if (showNotificationsDialog) {
        PendingInvitationsDialog(
            invitaciones = invitaciones,
            onDismiss = { showNotificationsDialog = false },
            onAccept = { albumId ->
                viewModel.aceptarInvitacion(albumId) {
                    showNotificationsDialog = false
                    viewModel.cargarMisAlbumes() // Refresca los álbumes activos tras aceptar
                }
            },
            onReject = { albumId ->
                viewModel.rechazarInvitacion(albumId) {
                    showNotificationsDialog = false
                    viewModel.cargarInvitacionesPendientes()
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = "home",
                onNavigateToP2P = onNavigateToP2P,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { showJoinDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "Unirse a álbum")
                }
                ExtendedFloatingActionButton(
                    onClick = onNavigateToCreateAlbum,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear nuevo álbum", fontWeight = FontWeight.Bold)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is AlbumUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AlbumUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        HeaderSection(
                            onProfileClick = onNavigateToProfile,
                            albumCount = state.albumes.size,
                            onNotificationClick = {
                                viewModel.cargarInvitacionesPendientes()
                                showNotificationsDialog = true
                            }
                        )

                        if (state.albumes.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No tienes álbumes. ¡Crea o únete a uno!", color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "Tus álbumes",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                items(state.albumes) { album ->
                                    AlbumCard(album, onClick = { onNavigateToAlbumDetail(album.id) })
                                }

                                item {
                                    Spacer(modifier = Modifier.height(140.dp))
                                }
                            }
                        }
                    }
                }
                is AlbumUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: ${state.message}", color = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.cargarMisAlbumes() }) {
                            Text("Reintentar")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun JoinAlbumDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var codigoSufijo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unirse a un álbum") },
        text = {
            Column {
                Text("Ingresa los últimos 5 caracteres del código o ID del álbum:")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = codigoSufijo,
                    onValueChange = { input ->
                        if (input.length <= 5) {
                            codigoSufijo = input
                        }
                    },
                    label = { Text("Código (5 caracteres)") },
                    placeholder = { Text("ej. 1a699") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(codigoSufijo.trim()) },
                enabled = codigoSufijo.trim().length == 5
            ) {
                Text("Unirse")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun HeaderSection(
    onProfileClick: () -> Unit,
    albumCount: Int,
    onNotificationClick: () -> Unit // 👈 Nuevo parámetro
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onProfileClick)
            ) {
                Text(
                    text = "Usuario",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$albumCount álbumes activos",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            IconButton(onClick = onNotificationClick) { // 👈 Acción al tocar la campana
                Icon(Icons.Filled.Notifications, contentDescription = "Invitaciones", tint = Color.Yellow)
            }
        }
    }
}

@Composable
fun PendingInvitationsDialog(
    invitaciones: List<Album>,
    onDismiss: () -> Unit,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invitaciones Pendientes", fontWeight = FontWeight.Bold) },
        text = {
            if (invitaciones.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tienes invitaciones pendientes", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(invitaciones) { album ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(album.title, fontWeight = FontWeight.Bold)
                                Text(album.description.ifEmpty { "Sin descripción" }, fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { onReject(album.id) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Rechazar")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = { onAccept(album.id) }) {
                                        Text("Aceptar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (album.isOwner) "👑" else "👥", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = album.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = album.photoCount.toString(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = if (album.description.length > 0) album.description else "Sin descripción",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text("fotos", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}