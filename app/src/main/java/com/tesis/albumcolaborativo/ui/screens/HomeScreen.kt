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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesis.albumcolaborativo.ui.components.BottomNavigationBar

data class Album(
    val title: String,
    val participants: Int,
    val date: String,
    val photoCount: Int,
    val progress: Float
)

@Composable
fun HomeScreen(
    onNavigateToCreateAlbum: () -> Unit = {},
    onNavigateToP2P: () -> Unit = {},
    onNavigateToAlbumDetail: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val albums = listOf(
        Album("Vacaciones Cartagena", 4, "Hace 2 días", 142, 1.0f),
        Album("Cumpleaños Ana", 6, "Procesando...", 87, 0.4f)
    )

    Scaffold(
        bottomBar = { 
            BottomNavigationBar(
                currentScreen = "home",
                onNavigateToP2P = onNavigateToP2P,
                onNavigateToProfile = onNavigateToProfile
            ) 
        },
        floatingActionButton = {
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
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            HeaderSection(onProfileClick = onNavigateToProfile)
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Álbumes recientes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(albums) { album ->
                    AlbumCard(album, onClick = onNavigateToAlbumDetail)
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // Espacio para el FAB
                }
            }
        }
    }
}

@Composable
fun HeaderSection(onProfileClick: () -> Unit) {
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
                    text = "Juan García",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "2 álbumes activos",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = Color.Yellow)
            }
        }
    }
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
                Text("🏖️", fontSize = 24.sp)
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
                    text = "${album.participants} participantes - ${album.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { album.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = if (album.progress >= 1.0f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text("fotos", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
