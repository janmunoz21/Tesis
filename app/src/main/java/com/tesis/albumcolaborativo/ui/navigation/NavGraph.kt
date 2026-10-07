package com.tesis.albumcolaborativo.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tesis.albumcolaborativo.ui.screens.*
import java.util.UUID

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
    object AlbumDetail : Screen("album_detail")
    object PhotoDetail : Screen("photo_detail")
    object Profile : Screen("profile")
    object DownloadAlbum : Screen("download_album")
}

// Datos del álbum que viajan por el flujo Subir → Bloque asignado → Procesamiento → Álbum listo
data class AlbumArgs(val id: String, val name: String, val photoCount: Int = 0)

sealed class AlbumFlowScreen(private val base: String) :
    Screen("$base/{albumId}?albumName={albumName}&photoCount={photoCount}") {
    fun createRoute(album: AlbumArgs) =
        "$base/${album.id}?albumName=${Uri.encode(album.name)}&photoCount=${album.photoCount}"
}

private val albumArguments = listOf(
    navArgument("albumId") { type = NavType.StringType },
    navArgument("albumName") { type = NavType.StringType; defaultValue = "" },
    navArgument("photoCount") { type = NavType.IntType; defaultValue = 0 }
)

private fun NavBackStackEntry.albumArgs(): AlbumArgs {
    val args = requireNotNull(arguments)
    return AlbumArgs(
        id = requireNotNull(args.getString("albumId")),
        name = args.getString("albumName").orEmpty(),
        photoCount = args.getInt("photoCount")
    )
}

@Composable
fun AppNavGraph(navController: NavHostController) {
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
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = { 
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
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
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCreateAlbum = { navController.navigate(Screen.CreateAlbum.route) },
                onNavigateToP2P = { navController.navigate(Screen.GroupP2P.route) },
                onNavigateToAlbumDetail = { navController.navigate(Screen.AlbumDetail.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }
        composable(Screen.CreateAlbum.route) {
            CreateAlbumScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUpload = { albumName ->
                    val album = AlbumArgs(UUID.randomUUID().toString(), albumName)
                    navController.navigate(Screen.UploadPhotos.createRoute(album))
                }
            )
        }
        composable(Screen.UploadPhotos.route, arguments = albumArguments) { entry ->
            val album = entry.albumArgs()
            UploadPhotosScreen(
                albumId = album.id,
                onNavigateBack = { navController.popBackStack() },
                onBlockAssigned = { photoCount ->
                    navController.navigate(Screen.AssignedBlock.createRoute(album.copy(photoCount = photoCount)))
                }
            )
        }
        composable(Screen.AssignedBlock.route, arguments = albumArguments) { entry ->
            val album = entry.albumArgs()
            AssignedBlockScreen(
                albumName = album.name,
                photoCount = album.photoCount,
                // ~0.3 s por foto en MobileNetV3, mínimo 1 s
                estimatedTimeSec = maxOf(1, (album.photoCount * 3 + 9) / 10),
                onAcceptBlock = {
                    navController.navigate(Screen.ProcessingPhotos.createRoute(album)) {
                        popUpTo(Screen.AssignedBlock.route) { inclusive = true }
                    }
                },
                onRejectBlock = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }
        composable(Screen.ProcessingPhotos.route, arguments = albumArguments) { entry ->
            val album = entry.albumArgs()
            ProcessingPhotosScreen(
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
                    navController.navigate(Screen.AlbumDetail.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }
        composable(Screen.GroupP2P.route) {
            GroupP2PNetworkScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AlbumDetail.route) {
            AlbumCategoriesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPhotoDetail = { navController.navigate(Screen.PhotoDetail.route) },
                onNavigateToDownload = { navController.navigate(Screen.DownloadAlbum.route) }
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
                onLogout = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
