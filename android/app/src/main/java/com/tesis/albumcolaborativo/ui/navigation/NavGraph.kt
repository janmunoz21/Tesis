package com.tesis.albumcolaborativo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.tesis.albumcolaborativo.ui.screens.*
import com.tesis.albumcolaborativo.ui.viewmodel.AuthViewModel

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
    object AlbumDetail : Screen("album_detail/{albumId}") {
        fun createRoute(albumId: String) = "album_detail/$albumId"
    }
    object PhotoDetail : Screen("photo_detail")
    object Profile : Screen("profile")
    object DownloadAlbum : Screen("download_album")
    object ResetPassword : Screen("reset_password?token={token}")
    object EditProfile : Screen("edit_profile")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
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

        // Reset Pass
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

        // 👈 Única declaración de ProfileScreen con la navegación a EditProfile
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

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCreateAlbum = { navController.navigate(Screen.CreateAlbum.route) },
                onNavigateToP2P = { navController.navigate(Screen.GroupP2P.route) },
                onNavigateToAlbumDetail = { albumId -> navController.navigate(Screen.AlbumDetail.createRoute(albumId)) },
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
        composable(
            route = Screen.AlbumDetail.route,
            arguments = listOf(navArgument("albumId") { type = NavType.StringType })
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
            AlbumCategoriesScreen(
                albumId = albumId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPhotoDetail = { navController.navigate(Screen.PhotoDetail.route) },
                onNavigateToDownload = { navController.navigate(Screen.DownloadAlbum.route) },
                onNavigateHome = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
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
    }
}
