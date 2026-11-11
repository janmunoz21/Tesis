package com.tesis.albumcolaborativo.data.network.p2p

import android.content.Context
import com.tesis.albumcolaborativo.data.distribution.ProcessingNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class P2PInviteEvent(
    val albumId: String,
    val albumName: String,
    val inviterName: String,
    val inviterAddress: String = "",
    val inviterPort: Int = 50051
)

data class P2PProcessingSignalEvent(
    val albumId: String,
    val albumName: String,
    val totalTasks: Int
)

object P2PManager {

    private var nsdManager: NsdDiscoveryManager? = null
    private var grpcService: GrpcP2PService? = null
    private var isInitialized = false

    private val assignedBlockCounts = java.util.concurrent.ConcurrentHashMap<String, Int>()

    fun setAssignedBlockCount(albumId: String, count: Int) {
        val cleanId = albumId.replace("\"", "").trim()
        if (cleanId.isNotBlank() && count > 0) {
            assignedBlockCounts[cleanId] = count
        }
    }

    fun getAssignedBlockCount(albumId: String): Int {
        val cleanId = albumId.replace("\"", "").trim()
        return assignedBlockCounts[cleanId] ?: 0
    }

    @Volatile
    private var activeCreatorAddress: String? = null
    @Volatile
    private var activeCreatorPort: Int = 50051

    private val _isNodeActive = MutableStateFlow(true)
    val isNodeActive: StateFlow<Boolean> = _isNodeActive.asStateFlow()

    private val _pendingInvitation = MutableStateFlow<P2PInviteEvent?>(null)
    val pendingInvitation: StateFlow<P2PInviteEvent?> = _pendingInvitation.asStateFlow()

    private val _processingSignal = MutableStateFlow<P2PProcessingSignalEvent?>(null)
    val processingSignal: StateFlow<P2PProcessingSignalEvent?> = _processingSignal.asStateFlow()

    fun init(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        nsdManager = NsdDiscoveryManager(appContext)
        grpcService = GrpcP2PService(appContext, 50051)
        isInitialized = true

        startServices()
    }

    fun startServices() {
        grpcService?.startServer()
        val port = grpcService?.getActivePort() ?: 50051
        nsdManager?.registerService(port = port)
        nsdManager?.startDiscovery()
        _isNodeActive.value = true
    }

    fun stopServices() {
        nsdManager?.unregisterService()
        nsdManager?.stopDiscovery()
        grpcService?.stopServer()
        _isNodeActive.value = false
    }

    fun onInviteReceived(albumId: String, albumName: String, inviterName: String, inviterAddress: String = "", inviterPort: Int = 50051) {
        if (inviterAddress.isNotBlank()) {
            activeCreatorAddress = inviterAddress
            activeCreatorPort = if (inviterPort > 0) inviterPort else 50051
            nsdManager?.addManualNode(inviterAddress, activeCreatorPort, inviterName)
        }
        _pendingInvitation.value = P2PInviteEvent(albumId, albumName, inviterName, inviterAddress, activeCreatorPort)
    }

    fun getCreatorAddress(): String? = activeCreatorAddress

    fun getCreatorPort(): Int = activeCreatorPort

    fun getServicePort(): Int = grpcService?.getActivePort() ?: 50051

    fun clearPendingInvitation() {
        _pendingInvitation.value = null
    }

    fun onProcessingSignalReceived(albumId: String, albumName: String, totalTasks: Int) {
        _processingSignal.value = P2PProcessingSignalEvent(albumId, albumName, totalTasks)
    }

    fun clearProcessingSignal() {
        _processingSignal.value = null
    }

    fun getDiscoveredNodes(): StateFlow<List<ProcessingNode>> {
        grpcService?.startServer()
        return nsdManager?.discoveredNodes ?: MutableStateFlow(emptyList())
    }

    fun isDiscovering(): StateFlow<Boolean> {
        return nsdManager?.isDiscovering ?: MutableStateFlow(false)
    }

    fun toggleAvailability(active: Boolean) {
        _isNodeActive.value = active
        if (active) {
            startServices()
        } else {
            stopServices()
        }
    }

    fun addManualNode(ipAddress: String, port: Int = 50051, label: String = "Nodo Manual") {
        activeCreatorAddress = ipAddress
        activeCreatorPort = port
        nsdManager?.addManualNode(ipAddress, port, label)
    }
}
