package com.tesis.albumcolaborativo.data.network.p2p

import android.content.Context
import com.tesis.albumcolaborativo.data.distribution.ProcessingNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class P2PInviteEvent(
    val albumName: String,
    val inviterName: String
)

object P2PManager {

    private var nsdManager: NsdDiscoveryManager? = null
    private var grpcService: GrpcP2PService? = null
    private var isInitialized = false

    private val _isNodeActive = MutableStateFlow(true)
    val isNodeActive: StateFlow<Boolean> = _isNodeActive.asStateFlow()

    private val _pendingInvitation = MutableStateFlow<P2PInviteEvent?>(null)
    val pendingInvitation: StateFlow<P2PInviteEvent?> = _pendingInvitation.asStateFlow()

    fun init(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        nsdManager = NsdDiscoveryManager(appContext)
        grpcService = GrpcP2PService(appContext, 50051)
        isInitialized = true

        startServices()
    }

    fun startServices() {
        nsdManager?.registerService(port = 50051)
        grpcService?.startServer()
        nsdManager?.startDiscovery()
        _isNodeActive.value = true
    }

    fun stopServices() {
        nsdManager?.unregisterService()
        nsdManager?.stopDiscovery()
        grpcService?.stopServer()
        _isNodeActive.value = false
    }

    fun toggleAvailability(active: Boolean) {
        if (active) startServices() else stopServices()
    }

    fun onInviteReceived(albumName: String, inviterName: String) {
        _pendingInvitation.value = P2PInviteEvent(albumName, inviterName)
    }

    fun clearPendingInvitation() {
        _pendingInvitation.value = null
    }

    fun getDiscoveredNodes(): StateFlow<List<ProcessingNode>> {
        return nsdManager?.discoveredNodes ?: MutableStateFlow(emptyList())
    }

    fun isDiscovering(): StateFlow<Boolean> {
        return nsdManager?.isDiscovering ?: MutableStateFlow(false)
    }
}
