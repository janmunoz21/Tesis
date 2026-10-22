package com.tesis.albumcolaborativo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(
    currentScreen: String = "home",
    onNavigateToHome: () -> Unit = {},
    onNavigateToAlbums: () -> Unit = {},
    onNavigateToP2P: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Inicio") },
            selected = currentScreen == "home",
            onClick = onNavigateToHome
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.PhotoAlbum, contentDescription = null) },
            label = { Text("Álbumes") },
            selected = currentScreen == "albums",
            onClick = onNavigateToAlbums
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Public, contentDescription = null) },
            label = { Text("Red") },
            selected = currentScreen == "p2p",
            onClick = onNavigateToP2P
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text("Perfil") },
            selected = currentScreen == "profile",
            onClick = onNavigateToProfile
        )
    }
}
