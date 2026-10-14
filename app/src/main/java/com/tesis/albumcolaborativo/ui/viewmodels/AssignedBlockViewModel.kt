package com.tesis.albumcolaborativo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.albumcolaborativo.data.distribution.AssignedBlock
import com.tesis.albumcolaborativo.data.distribution.ProcessingNode
import com.tesis.albumcolaborativo.data.distribution.TaskDistributor
import com.tesis.albumcolaborativo.data.local.AppDatabase
import com.tesis.albumcolaborativo.data.repository.TaskQueueRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssignedBlockViewModel(application: Application) : AndroidViewModel(application) {

    private val taskDistributor = TaskDistributor()

    private val taskQueueRepository =
        TaskQueueRepository(AppDatabase.getDatabase(application).taskQueueDao())

    private val _isRejecting = MutableStateFlow(false)
    val isRejecting: StateFlow<Boolean> = _isRejecting.asStateFlow()

    /**
     * CU-D-03: el colaborador rechaza su bloque y se intenta reasignar
     * a otro nodo disponible. Si no hay ninguno, las tareas vuelven a
     * PENDIENTE para que entren en la próxima distribución.
     *
     * onResult recibe los bloques reasignados (vacío si quedaron en cola).
     */
    fun rejectBlock(
        albumId: String,
        nodes: List<ProcessingNode>,
        onResult: (Result<List<AssignedBlock>>) -> Unit
    ) {
        if (_isRejecting.value) return
        _isRejecting.value = true

        viewModelScope.launch {

            val result = runCatching {
                withContext(Dispatchers.IO) {

                    val rejectedBlock = AssignedBlock(
                        node = ProcessingNode(
                            nodeId = TaskDistributor.LOCAL_NODE_ID,
                            address = TaskDistributor.LOCAL_NODE_ID,
                            port = 0
                        ),
                        tasks = taskQueueRepository.getAssignedTasks(albumId)
                    )

                    val reassignedBlocks =
                        taskDistributor.reassign(rejectedBlock, nodes)

                    // Las tareas reasignadas siguen ASIGNADA (ahora a otro nodo);
                    // sin nodos alternativos, el bloque completo vuelve a la cola.
                    if (reassignedBlocks.isEmpty()) {
                        taskQueueRepository.releaseAssignedTasks(albumId)
                    }

                    reassignedBlocks
                }
            }

            _isRejecting.value = false

            // Si se cerró la pantalla, no notificar
            (result.exceptionOrNull() as? CancellationException)?.let { throw it }

            onResult(result)
        }
    }
}
