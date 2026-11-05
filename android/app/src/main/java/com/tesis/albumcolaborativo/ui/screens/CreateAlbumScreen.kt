package com.tesis.albumcolaborativo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import com.tesis.albumcolaborativo.ui.viewmodel.AlbumViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAlbumScreen(
    viewModel: AlbumViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToUpload: (albumId: String, albumName: String) -> Unit = { _, _ -> }
) {
    var albumName by remember { mutableStateOf("Vacaciones Cartagena 2026") }
    var description by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    // Estados para la gestión dinámica de invitaciones
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var nuevoCorreo by remember { mutableStateOf("") }
    var errorCorreo by remember { mutableStateOf<String?>(null) }

    // Lista local de correos agregados antes de persistir el álbum
    val invitadosLocales = remember { mutableStateListOf<String>() }

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
                enabled = !isCreating,
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
                enabled = !isCreating,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text("Invitar participantes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Participantes renderizados dinámicamente según la lista local
            if (invitadosLocales.isEmpty()) {
                Text(
                    text = "Aún no has agregado invitaciones.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                invitadosLocales.forEach { correo ->
                    ParticipantItem(
                        name = correo.substringBefore("@").replaceFirstChar { it.uppercase() },
                        email = correo,
                        status = "Pendiente",
                        statusColor = Color(0xFFFFF3E0),
                        textColor = Color(0xFFEF6C00),
                        onRemove = { invitadosLocales.remove(correo) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón con acción para desplegar el diálogo
            TextButton(
                onClick = { showAddMemberDialog = true },
                enabled = !isCreating
            ) {
                Text("+ Agregar más personas", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isNameValid && !isCreating) {
                        isCreating = true
                        viewModel.crearAlbum(albumName.trim(), description.trim()) { nuevoAlbum ->
                            // Enviar las invitaciones acumuladas al albumId real de PostgreSQL
                            if (invitadosLocales.isNotEmpty()) {
                                invitadosLocales.forEach { correo ->
                                    viewModel.invitarParticipante(nuevoAlbum.id, correo) { _, _ -> }
                                }
                            }
                            isCreating = false
                            onNavigateToUpload(nuevoAlbum.id, nuevoAlbum.nombre)
                        }
                    }
                },
                enabled = isNameValid && !isCreating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Crear álbum", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // DIÁLOGO FLOTANTE PARA AGREGAR NUEVO CORREO
    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Invitar participante", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Ingresa el correo electrónico del participante:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nuevoCorreo,
                        onValueChange = {
                            nuevoCorreo = it
                            errorCorreo = null
                        },
                        label = { Text("Correo electrónico") },
                        placeholder = { Text("usuario@correo.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorCorreo != null) {
                        Text(
                            text = errorCorreo!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanCorreo = nuevoCorreo.trim()
                        if (cleanCorreo.isBlank() || !cleanCorreo.contains("@")) {
                            errorCorreo = "Ingresa un correo electrónico válido."
                        } else if (invitadosLocales.contains(cleanCorreo)) {
                            errorCorreo = "Este correo ya está en la lista de invitaciones."
                        } else {
                            invitadosLocales.add(cleanCorreo)
                            nuevoCorreo = ""
                            errorCorreo = null
                            showAddMemberDialog = false
                        }
                    }
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddMemberDialog = false
                        nuevoCorreo = ""
                        errorCorreo = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ParticipantItem(
    name: String,
    email: String,
    status: String,
    statusColor: Color,
    textColor: Color,
    onRemove: (() -> Unit)? = null
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
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(email, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
            Surface(
                color = statusColor,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remover",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
