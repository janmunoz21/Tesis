package com.tesis.albumcolaborativo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.tesis.albumcolaborativo.ui.viewmodel.AlbumViewModel
import com.tesis.albumcolaborativo.ui.viewmodels.AlbumCategoriesViewModel
import com.tesis.albumcolaborativo.ui.viewmodels.CategoryGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumCategoriesScreen(
    albumId: String,
    albumViewModel: AlbumViewModel = viewModel(),
    categoriesViewModel: AlbumCategoriesViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPhotoDetail: () -> Unit = {},
    onNavigateToDownload: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onNavigateToP2P: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }

    var nuevoNombre by remember { mutableStateOf("") }
    var nuevaDescripcion by remember { mutableStateOf("") }
    var correoInvitado by remember { mutableStateOf("") }

    val albumDetalle by albumViewModel.albumDetalle.collectAsState()
    val categoriesState by categoriesViewModel.uiState.collectAsState()

    // Categoria seleccionada en los Chips ("TODOS" por defecto)
    var selectedCategoryFilter by remember { mutableStateOf("TODOS") }

    LaunchedEffect(albumId) {
        albumViewModel.cargarDetalleAlbum(albumId)
        categoriesViewModel.cargarFotosYCategorias(albumId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏖️ ", fontSize = 18.sp)
                            Text(
                                text = albumDetalle?.nombre ?: "Álbum Colaborativo",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        }
                        val countParticipantes = albumDetalle?.participantes?.size ?: 1
                        Text(
                            text = "${categoriesState.totalPhotos} fotos · $countParticipantes participantes",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Descargar", tint = Color.White)
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Invitar participante") },
                            onClick = { showMenu = false; showInviteDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Editar álbum") },
                            onClick = { showMenu = false; showEditDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Gestionar participantes") },
                            onClick = { showMenu = false; showMembersDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Salir del álbum", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; showLeaveDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar álbum", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; showDeleteDialog = true }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0288D1))
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            if (categoriesState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF0288D1)
                )
            } else if (categoriesState.categories.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📷", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Aún no hay fotos clasificadas.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sube fotos para agruparlas automáticamente por IA.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {

                    // ==========================================
                    // BARRA HORIZONTAL DE CHIPS DE FILTRO
                    // ==========================================
                    val allCategories = listOf("TODOS") + categoriesState.categories.map { it.categoryName }

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allCategories) { categoryName ->
                            val isSelected = selectedCategoryFilter == categoryName
                            val emoji = getEmojiForCategory(categoryName)

                            Surface(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color.Transparent else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedCategoryFilter = categoryName },
                                color = if (isSelected) Color(0xFFE3F2FD) else Color.White,
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (emoji.isNotEmpty()) {
                                        Text(text = "$emoji ", fontSize = 14.sp)
                                    }
                                    Text(
                                        text = if (categoryName == "TODOS") "Todos" else categoryName,
                                        color = if (isSelected) Color(0xFF1976D2) else Color.DarkGray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // ==========================================
                    // SECCIONES VERTICALES DE FOTOS
                    // ==========================================
                    val filteredGroups = if (selectedCategoryFilter == "TODOS") {
                        categoriesState.categories
                    } else {
                        categoriesState.categories.filter { it.categoryName == selectedCategoryFilter }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(filteredGroups) { group ->
                            CategorySection(group = group)
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS DE CONTROL (EDITAR, INVITAR, ETC) ---
    if (showInviteDialog) {
        var isSending by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSending) showInviteDialog = false },
            title = { Text("Invitar participante") },
            text = {
                Column {
                    Text("Ingresa el correo electrónico del participante:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = correoInvitado,
                        onValueChange = { correoInvitado = it; errorMessage = null },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSending = true
                        albumViewModel.invitarParticipante(albumId, correoInvitado.trim()) { exito, msg ->
                            isSending = false
                            if (exito) { showInviteDialog = false; correoInvitado = "" } else { errorMessage = msg }
                        }
                    }
                ) { Text("Enviar") }
            },
            dismissButton = { TextButton(onClick = { showInviteDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showMembersDialog) {
        AlertDialog(
            onDismissRequest = { showMembersDialog = false },
            title = { Text("Participantes") },
            text = {
                val participantes = albumDetalle?.participantes ?: emptyList()
                if (participantes.isEmpty()) {
                    Text("No hay otros participantes.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                        items(participantes) { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(p.nombre, fontWeight = FontWeight.Bold)
                                    Text(if (p.rol == "CREADOR") "Creador" else "Participante", fontSize = 12.sp, color = Color.Gray)
                                }
                                if (p.rol != "CREADOR") {
                                    IconButton(onClick = {
                                        albumViewModel.removerParticipante(albumId, p.usuarioId) {
                                            albumViewModel.cargarDetalleAlbum(albumId)
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showMembersDialog = false }) { Text("Cerrar") } }
        )
    }

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
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nuevaDescripcion,
                        onValueChange = { nuevaDescripcion = it },
                        label = { Text("Nueva descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    albumViewModel.editarAlbum(albumId, nuevoNombre, nuevaDescripcion) {
                        showEditDialog = false
                        onNavigateHome()
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("¿Salir de este álbum?") },
            text = { Text("Perderás acceso a las fotos compartidas.") },
            confirmButton = {
                Button(
                    onClick = {
                        albumViewModel.salirDeAlbum(albumId) { showLeaveDialog = false; onNavigateHome() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Salir", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar este álbum?") },
            text = { Text("Esta acción eliminará el álbum del servidor central.") },
            confirmButton = {
                Button(
                    onClick = {
                        albumViewModel.eliminarAlbum(albumId) { showDeleteDialog = false; onNavigateHome() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }
}

// ==========================================
// COMPOSABLE: SECCIÓN POR CATEGORÍA
// ==========================================
@Composable
fun CategorySection(group: CategoryGroup) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = getEmojiForCategory(group.categoryName) + " ",
                    fontSize = 18.sp
                )
                Text(
                    text = group.categoryName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF212121)
                )
            }
            Text(
                text = "${group.photos.size} fotos",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }

        // Grilla de 3 columnas para cada sección
        val rowCount = (group.photos.size + 2) / 3
        val gridHeight = (rowCount * 110).dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false // El scroll principal lo hace LazyColumn
        ) {
            items(group.photos) { photo ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF0F4F8))
                ) {
                    AsyncImage(
                        model = photo.imagePath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "${(photo.predictedScore * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// Helper para asignar Emojis dinámicos según el nombre de la categoría
fun getEmojiForCategory(categoryName: String): String {
    val lower = categoryName.lowercase()
    return when {
        lower.contains("playa") || lower.contains("beach") -> "🏖️"
        lower.contains("comida") || lower.contains("food") || lower.contains("fountain") -> "🍽️"
        lower.contains("atardecer") || lower.contains("sunset") || lower.contains("vault") -> "🌅"
        lower.contains("perro") || lower.contains("dog") -> "🐶"
        lower.contains("carro") || lower.contains("car") -> "🚗"
        lower == "todos" -> ""
        else -> "🖼️"
    }
}