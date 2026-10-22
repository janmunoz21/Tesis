package com.tesis.albumcolaborativo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    onNavigateToUpload: () -> Unit = {}
) {
    var albumName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val nameString: String = albumName
    val isNameValid = nameString.length > 0

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

            Text("El álbum se creará y podrás ver el token de invitación en los detalles del álbum.", color = MaterialTheme.colorScheme.outline)

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (isNameValid) {
                        viewModel.crearAlbum(albumName, description) {
                            onNavigateBack()
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
                Text("Crear álbum", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ParticipantItem(name: String, email: String, status: String, statusColor: Color, textColor: Color) {
    val initialLetters = if (name.length >= 2) "${name[0]}${name[1]}" else name

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
                Text(initialLetters, color = Color.White, fontWeight = FontWeight.Bold)
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
        }
    }
}
