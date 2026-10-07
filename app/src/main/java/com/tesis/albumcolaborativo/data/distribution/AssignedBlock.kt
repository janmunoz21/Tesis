package com.tesis.albumcolaborativo.data.distribution

import com.tesis.albumcolaborativo.data.local.entities.TaskQueueEntity

data class AssignedBlock(
    val node: ProcessingNode,
    val tasks: List<TaskQueueEntity>
)