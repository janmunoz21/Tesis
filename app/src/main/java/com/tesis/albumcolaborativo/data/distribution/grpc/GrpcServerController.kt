package com.tesis.albumcolaborativo.data.distribution.grpc

import android.content.Context
import android.net.wifi.WifiManager

class GrpcServerController(
    private val context: Context
) {

    private val serverManager = GrpcServerManager()

    fun start(port: Int = 50051) {
        serverManager.start(port)
    }

    fun stop() {
        serverManager.stop()
    }

    fun isRunning(): Boolean {
        return serverManager.isRunning()
    }

    fun getLocalIpAddress(): String {

        val wifiManager =
            context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager

        val ipAddress = wifiManager.connectionInfo.ipAddress

        return android.text.format.Formatter
            .formatIpAddress(ipAddress)
    }
}