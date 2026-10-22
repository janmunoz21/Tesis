package com.tesis.albumcolaborativo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tesis.albumcolaborativo.data.local.TokenManager
import com.tesis.albumcolaborativo.data.repository.AuthRepository
import com.tesis.albumcolaborativo.ui.components.BottomNavigationBar
import com.tesis.albumcolaborativo.ui.viewmodel.AlbumUiState
import com.tesis.albumcolaborativo.ui.viewmodel.AlbumViewModel

@Composable
fun ProfileScreen(
    albumViewModel: AlbumViewModel = viewModel(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToP2P: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {}, // 👈 Lambda hacia la nueva pantalla
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val authRepository = remember { AuthRepository(context) }

    val albumUiState by albumViewModel.uiState.collectAsState()

    val albumList = when (val state = albumUiState) {
        is AlbumUiState.Success -> state.albumes
        else -> emptyList()
    }

    val totalAlbumes = albumList.size
    var totalFotos = 0
    for (a in albumList) {
        totalFotos += a.photoCount
    }

    val userEmail = tokenManager.obtenerEmailUsuario()
    var userName by remember { mutableStateOf(tokenManager.obtenerNombreUsuario()) }

    var autoNodeEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        userName = tokenManager.obtenerNombreUsuario()
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = "profile",
                onNavigateToP2P = onNavigateToP2P,
                onNavigateToProfile = {}
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2196F3))
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.3f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Foto de perfil",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Álbumes",
                    value = "$totalAlbumes",
                    icon = "📁",
                    iconBg = Color(0xFFE3F2FD)
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Fotos",
                    value = "$totalFotos",
                    icon = "🖼️",
                    iconBg = Color(0xFFE8F5E9)
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Sesiones",
                    value = "1",
                    icon = "⚡",
                    iconBg = Color(0xFFFFF3E0)
                )
            }

            // Sección de Configuración
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Configuración",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                ) {
                    Column {
                        // 👇 Redirige a la pantalla dedicada
                        SettingItem(
                            icon = Icons.Default.Edit,
                            iconTint = Color(0xFF2196F3),
                            title = "Editar datos de perfil",
                            onClick = onNavigateToEditProfile
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))

                        SettingItem(
                            icon = Icons.Default.Notifications,
                            iconTint = Color(0xFFFFB300),
                            title = "Notificaciones",
                            onClick = {}
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Modo nodo automático", fontWeight = FontWeight.Medium)
                            }
                            Switch(
                                checked = autoNodeEnabled,
                                onCheckedChange = { autoNodeEnabled = it }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))

                        SettingItem(
                            icon = Icons.Default.Lock,
                            iconTint = Color(0xFFFFB300),
                            title = "Privacidad",
                            onClick = {}
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))

                        // Opción de Cerrar Sesión
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .background(Color.Transparent),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    authRepository.cerrarSesion()
                                    onLogout()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Cerrar sesión",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: String,
    iconBg: Color
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconBg,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(icon, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // 👈 Asegúrate de que el Modifier tenga .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Medium)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
    }
}