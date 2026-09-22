package com.tesis.albumcolaborativo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tesis.albumcolaborativo.ui.screens.*

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object CreateAlbum : Screen("create_album")
    object UploadPhotos : Screen("upload_photos")
    object AssignedBlock : Screen("assigned_block")
    object ProcessingPhotos : Screen("processing_photos")
    object GroupP2P : Screen("group_p2p")
    object AlbumDetail : Screen("album_detail")
    object PhotoDetail : Screen("photo_detail")
    object Profile : Screen("profile")
    object DownloadAlbum : Screen("download_album")
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
                onNavigateToUpload = { navController.navigate(Screen.UploadPhotos.route) }
            )
        }
        composable(Screen.UploadPhotos.route) {
            UploadPhotosScreen(
                onNavigateBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Screen.AssignedBlock.route) }
            )
        }
        composable(Screen.AssignedBlock.route) {
            AssignedBlockScreen(
                onNavigateBack = { navController.popBackStack() },
                onAcceptAndProcess = { navController.navigate(Screen.ProcessingPhotos.route) },
                onNavigateToProcessing = { navController.navigate(Screen.ProcessingPhotos.route) }
            )
        }
        composable(Screen.ProcessingPhotos.route) {
            ProcessingPhotosScreen(
                onNavigateBack = { navController.popBackStack() }
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
