package com.tesis.albumcolaborativo.ui.navigation

import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.tesis.albumcolaborativo.ui.screens.*
import com.tesis.albumcolaborativo.ui.viewmodel.AuthViewModel
import java.util.UUID

// 1. Definición de Pantallas y Rutas
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object CreateAlbum : Screen("create_album")
    object UploadPhotos : AlbumFlowScreen("upload_photos")
    object AssignedBlock : AlbumFlowScreen("assigned_block")
    object ProcessingPhotos : AlbumFlowScreen("processing_photos")
    object AlbumReady : AlbumFlowScreen("album_ready")
    object GroupP2P : Screen("group_p2p")
    object AlbumDetail : Screen("album_detail/{albumId}") {
        fun createRoute(albumId: String): String {
            val cleanId = albumId.replace("\"", "").trim()
            return "album_detail/$cleanId"
        }
    }
    object PhotoDetail : Screen("photo_detail")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object DownloadAlbum : Screen("download_album")
    object ResetPassword : Screen("reset_password?token={token}")
}

// 2. Estructura de argumentos compartidos para el flujo del Álbum
data class AlbumArgs(val id: String, val name: String, val photoCount: Int = 0)

sealed class AlbumFlowScreen(private val base: String) :
    Screen("$base/{albumId}?albumName={albumName}&photoCount={photoCount}&isCreator={isCreator}") {
    fun createRoute(album: AlbumArgs, isCreator: Boolean = true): String {
        val cleanId = album.id.replace("\"", "").trim()
        return "$base/$cleanId?albumName=${Uri.encode(album.name)}&photoCount=${album.photoCount}&isCreator=$isCreator"
    }
}

private val albumArguments = listOf(
    navArgument("albumId") { type = NavType.StringType },
    navArgument("albumName") { type = NavType.StringType; defaultValue = "" },
    navArgument("photoCount") { type = NavType.IntType; defaultValue = 0 },
    navArgument("isCreator") { type = NavType.BoolType; defaultValue = true }
)

private fun NavBackStackEntry.albumArgs(): AlbumArgs {
    val args = requireNotNull(arguments)
    val rawId = requireNotNull(args.getString("albumId"))
    return AlbumArgs(
        id = rawId.replace("\"", "").trim(),
        name = args.getString("albumName").orEmpty(),
        photoCount = args.getInt("photoCount")
    )
}

// 3. Grafo Principal de Navegación
@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val pendingInvite by com.tesis.albumcolaborativo.data.network.p2p.P2PManager.pendingInvitation.collectAsState()
    LaunchedEffect(Unit) {
        com.tesis.albumcolaborativo.data.network.p2p.P2PManager.processingSignal.collect { signal ->
            if (signal != null) {
                val album = AlbumArgs(signal.albumId, signal.albumName, signal.totalTasks)
                com.tesis.albumcolaborativo.data.network.p2p.P2PManager.clearProcessingSignal()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    navController.navigate(Screen.ProcessingPhotos.createRoute(album))
                }
            }
        }
    }

    pendingInvite?.let { invite ->
        AlertDialog(
            onDismissRequest = { com.tesis.albumcolaborativo.data.network.p2p.P2PManager.clearPendingInvitation() },
            title = { Text("Invitación a Álbum Colaborativo") },
            text = { Text("¡${invite.inviterName} te ha invitado a unirte al álbum '${invite.albumName}' vía Wi-Fi P2P!") },
            confirmButton = {
                Button(
                    onClick = {
                        val album = AlbumArgs(invite.albumId, invite.albumName)
                        com.tesis.albumcolaborativo.data.network.p2p.P2PManager.clearPendingInvitation()
                        navController.navigate(Screen.UploadPhotos.createRoute(album) + "&isCreator=false")
                    }
                ) {
                    Text("Aceptar e Ir a Subir Fotos")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { com.tesis.albumcolaborativo.data.network.p2p.P2PManager.clearPendingInvitation() }
                ) {
                    Text("Rechazar")
                }
            }
        )
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(
                navArgument("token") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "albumcolaborativo://reset-password?token={token}"
                }
            )
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            ResetPasswordScreen(
                token = token,
                authViewModel = authViewModel,
                onPasswordResetSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCreateAlbum = { navController.navigate(Screen.CreateAlbum.route) },
                onNavigateToP2P = { navController.navigate(Screen.GroupP2P.route) },
                onNavigateToAlbumDetail = { albumId ->
                    val cleanId = albumId.replace("\"", "").trim()
                    navController.navigate(Screen.AlbumDetail.createRoute(cleanId))
                },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(Screen.CreateAlbum.route) {
            CreateAlbumScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUpload = { albumId, albumName ->
                    val album = AlbumArgs(albumId, albumName)
                    navController.navigate(Screen.UploadPhotos.createRoute(album))
                }
            )
        }

        composable(Screen.UploadPhotos.route, arguments = albumArguments) { entry ->
            val album = entry.albumArgs()
            val isCreator = entry.arguments?.getBoolean("isCreator") ?: true
            UploadPhotosScreen(
                albumId = album.id,
                isCreator = isCreator,
                onNavigateBack = { navController.popBackStack() },
                onBlockAssigned = { photoCount ->
                    navController.navigate(
                        Screen.AssignedBlock.createRoute(album.copy(photoCount = photoCount))
                    )
                }
            )
        }

        composable(Screen.AssignedBlock.route, arguments = albumArguments) { entry ->
            val album = entry.albumArgs()
            AssignedBlockScreen(
                albumName = album.name,
                photoCount = album.photoCount,
                estimatedTimeSec = maxOf(1, (album.photoCount * 3 + 9) / 10),
                onNavigateBack = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onAcceptAndProcess = {
                    navController.navigate(Screen.ProcessingPhotos.createRoute(album)) {
                        popUpTo(Screen.AssignedBlock.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ProcessingPhotos.route, arguments = albumArguments) { entry ->
            val album = entry.albumArgs()
            ProcessingPhotosScreen(
                albumId = album.id,
                onNavigateBack = { navController.popBackStack() },
                onProcessingComplete = {
                    navController.navigate(Screen.AlbumReady.createRoute(album)) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.AlbumReady.route, arguments = albumArguments) { entry ->
            val album = entry.albumArgs()
            AlbumReadyScreen(
                albumName = album.name,
                photoCount = album.photoCount,
                onViewAlbum = {
                    navController.navigate(Screen.AlbumDetail.createRoute(album.id)) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.GroupP2P.route) {
            GroupP2PNetworkScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Screen.AlbumDetail.route,
            arguments = listOf(navArgument("albumId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rawAlbumId = backStackEntry.arguments?.getString("albumId") ?: ""
            val cleanAlbumId = rawAlbumId.replace("\"", "").trim()

            AlbumCategoriesScreen(
                albumId = cleanAlbumId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPhotoDetail = { navController.navigate(Screen.PhotoDetail.route) },
                onNavigateToDownload = { navController.navigate(Screen.DownloadAlbum.route) },
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToP2P = { navController.navigate(Screen.GroupP2P.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(Screen.DownloadAlbum.route) {
            DownloadAlbumScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PhotoDetail.route) {
            PhotoDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToHome = { navController.navigate(Screen.Home.route) },
                onNavigateToP2P = { navController.navigate(Screen.GroupP2P.route) },
                onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onLogout = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
