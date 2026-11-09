package com.tesis.albumcolaborativo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.tesis.albumcolaborativo.data.network.p2p.P2PManager
import com.tesis.albumcolaborativo.ui.navigation.AppNavGraph
import com.tesis.albumcolaborativo.ui.theme.AlbumColaborativoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar servicios P2P (mDNS y gRPC) a nivel de aplicación
        P2PManager.init(applicationContext)

        setContent {
            AlbumColaborativoTheme {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(navController = navController)
                }
            }
        }
    }
}
