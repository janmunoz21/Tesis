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
import com.tesis.albumcolaborativo.data.distribution.grpc.GrpcServerController
import com.tesis.albumcolaborativo.ui.navigation.AppNavGraph
import com.tesis.albumcolaborativo.ui.theme.AlbumColaborativoTheme

class MainActivity : ComponentActivity() {

    private lateinit var grpcServerController: GrpcServerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        grpcServerController = GrpcServerController(this)

        grpcServerController.start()

        println(
            "Servidor gRPC activo en " +
                    "${grpcServerController.getLocalIpAddress()}:50051"
        )

        enableEdgeToEdge()

        setContent {
            AlbumColaborativoTheme {

                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        navController = navController
                    )
                }
            }
        }
    }

    override fun onDestroy() {

        grpcServerController.stop()

        super.onDestroy()
    }
}