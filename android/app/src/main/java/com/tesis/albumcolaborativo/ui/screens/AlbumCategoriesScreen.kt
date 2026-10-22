package com.tesis.albumcolaborativo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tesis.albumcolaborativo.ui.viewmodel.AlbumViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumCategoriesScreen(
    albumId: String,
    albumViewModel: AlbumViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPhotoDetail: () -> Unit,
    onNavigateToDownload: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToP2P: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }

    // Estados para los diálogos
    var nuevoNombre by remember { mutableStateOf("") }
    var nuevaDescripcion by remember { mutableStateOf("") }
    var correoInvitado by remember { mutableStateOf("") }

    var showMembersDialog by remember { mutableStateOf(false) }
    val albumDetalle by albumViewModel.albumDetalle.collectAsState()

    LaunchedEffect(albumId) {
        albumViewModel.cargarDetalleAlbum(albumId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Álbum", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Invitar participante") },
                            onClick = {
                                showMenu = false
                                showInviteDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Editar álbum") },
                            onClick = {
                                showMenu = false
                                showEditDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Salir del álbum", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showLeaveDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar álbum", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Gestionar participantes") },
                            onClick = {
                                showMenu = false
                                showMembersDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0091EA)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("Categorías de fotos para el álbum ID: $albumId")
        }
    }

    // ==========================================
    // CU-B-07: INVITAR PARTICIPANTE
    // ==========================================
    if (showInviteDialog) {
        var isSending by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                if (!isSending) showInviteDialog = false
            },
            title = { Text("Invitar por correo") },
            text = {
                Column {
                    Text("Ingresa el correo electrónico del usuario registrado que deseas invitar:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = correoInvitado,
                        onValueChange = {
                            correoInvitado = it
                            errorMessage = null
                        },
                        label = { Text("Correo electrónico") },
                        placeholder = { Text("usuario@correo.com") },
                        singleLine = true,
                        enabled = !isSending,
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = correoInvitado.isNotBlank() && !isSending,
                    onClick = {
                        isSending = true
                        errorMessage = null
                        albumViewModel.invitarParticipante(albumId, correoInvitado.trim()) { exito, mensaje ->
                            isSending = false
                            if (exito) {
                                showInviteDialog = false
                                correoInvitado = ""
                            } else {
                                errorMessage = mensaje
                            }
                        }
                    }
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Enviar Invitación")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSending,
                    onClick = { showInviteDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showMembersDialog) {
        AlertDialog(
            onDismissRequest = { showMembersDialog = false },
            title = { Text("Participantes del álbum") },
            text = {
                val participantes = albumDetalle?.participantes ?: emptyList()

                if (participantes.isEmpty()) {
                    Text("No hay otros participantes registrados.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(participantes) { participante ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(participante.nombre, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (participante.rol == "CREADOR") "Creador" else "Participante",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }

                                // Botón para eliminar sólo si no es el creador
                                if (participante.rol != "CREADOR") {
                                    IconButton(
                                        onClick = {
                                            albumViewModel.removerParticipante(albumId, participante.usuarioId) { _ ->
                                                albumViewModel.cargarDetalleAlbum(albumId) // Refresca lista
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remover",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMembersDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // ==========================================
    // DIÁLOGOS EXISTENTES (EDITAR, SALIR, ELIMINAR)
    // ==========================================
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar Álbum") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nuevoNombre,
                        onValueChange = { nuevoNombre = it },
                        label = { Text("Nuevo nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nuevaDescripcion,
                        onValueChange = { nuevaDescripcion = it },
                        label = { Text("Nueva descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nuevoNombre.isNotBlank()) {
                            albumViewModel.editarAlbum(albumId, nuevoNombre, nuevaDescripcion) {
                                showEditDialog = false
                                onNavigateHome()
                            }
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("¿Salir de este álbum?") },
            text = { Text("Ya no tendrás acceso a las fotos compartidas en este álbum.") },
            confirmButton = {
                Button(
                    onClick = {
                        albumViewModel.salirDeAlbum(albumId) {
                            showLeaveDialog = false
                            onNavigateHome()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Salir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar este álbum?") },
            text = { Text("Esta acción eliminará el álbum de la base de datos PostgreSQL. No se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        albumViewModel.eliminarAlbum(albumId) {
                            showDeleteDialog = false
                            onNavigateHome()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}