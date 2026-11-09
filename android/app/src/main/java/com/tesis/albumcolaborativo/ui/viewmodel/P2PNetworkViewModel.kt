package com.tesis.albumcolaborativo.ui.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.albumcolaborativo.data.distribution.ProcessingNode
import com.tesis.albumcolaborativo.data.local.AppDatabase
import com.tesis.albumcolaborativo.data.network.p2p.P2PManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class P2PNetworkUiState(
    val nodes: List<ProcessingNode> = emptyList(),
    val isNodeActive: Boolean = true,
    val isDiscovering: Boolean = false,
    val processedCount: Int = 0,
    val totalTaskCount: Int = 0,
    val localDeviceName: String = Build.MODEL,
    val avgInferenceTimeMs: Long = 48L,
    val speedupFactor: Float = 1.0f,
    val p2pNodesCount: Int = 0
)

class P2PNetworkViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)

    private val _processedCount = MutableStateFlow(0)
    private val _avgInferenceTimeMs = MutableStateFlow(48L)

    val uiState: StateFlow<P2PNetworkUiState> = combine(
        P2PManager.getDiscoveredNodes(),
        P2PManager.isNodeActive,
        P2PManager.isDiscovering(),
        _processedCount,
        _avgInferenceTimeMs
    ) { nodes, isActive, isDiscovering, processed, avgInference ->
        val activeNodesCount = nodes.count { it.available } + 1
        val estimatedSpeedup = if (activeNodesCount > 1) (activeNodesCount * 0.85f) else 1.0f

        P2PNetworkUiState(
            nodes = nodes,
            isNodeActive = isActive,
            isDiscovering = isDiscovering,
            processedCount = processed,
            totalTaskCount = maxOf(processed, 20),
            localDeviceName = Build.MODEL,
            avgInferenceTimeMs = avgInference,
            speedupFactor = estimatedSpeedup,
            p2pNodesCount = nodes.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = P2PNetworkUiState()
    )

    init {
        P2PManager.init(application)
        loadContributionMetrics()
    }

    fun toggleNodeAvailability(active: Boolean) {
        P2PManager.toggleAvailability(active)
    }

    fun addManualNode(ipAddress: String, label: String = "Nodo Manual") {
        P2PManager.addManualNode(ipAddress.trim(), 50051, label.trim())
    }

    private fun loadContributionMetrics() {
        viewModelScope.launch {
            try {
                val resultsCount = database.classificationResultDao().countByExperiment("LOCAL")
                if (resultsCount > 0) {
                    _processedCount.value = resultsCount
                }
            } catch (_: Exception) {
                // Silencioso si no hay metricas registradas aún
            }
        }
    }
}
