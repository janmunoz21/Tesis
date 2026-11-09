package com.tesis.albumcolaborativo.data.distribution

import com.tesis.albumcolaborativo.data.local.entities.TaskQueueEntity

class TaskDistributor {

    fun distribute(
        tasks: List<TaskQueueEntity>,
        nodes: List<ProcessingNode>
    ): List<AssignedBlock> {

        if (tasks.isEmpty()) {
            return emptyList()
        }

        val availableNodes = nodes.filter { it.available }

        // Si no existen nodos disponibles,
        // todo queda asignado al procesamiento local.
        if (availableNodes.isEmpty()) {

            val localNode = ProcessingNode(
                nodeId = "LOCAL",
                address = "LOCAL",
                port = 0,
                available = true
            )

            return listOf(
                AssignedBlock(
                    node = localNode,
                    tasks = tasks
                )
            )
        }

        val numberOfNodes = availableNodes.size
        val totalTasks = tasks.size

        val baseTasksPerNode = totalTasks / numberOfNodes
        val remainingTasks = totalTasks % numberOfNodes

        val blocks = mutableListOf<AssignedBlock>()

        var currentIndex = 0

        availableNodes.forEachIndexed { index, node ->

            // Los primeros nodos reciben una tarea adicional
            // cuando la división no es exacta.
            val tasksForNode =
                baseTasksPerNode + if (index < remainingTasks) 1 else 0

            val endIndex = currentIndex + tasksForNode

            val nodeTasks = tasks.subList(
                currentIndex,
                endIndex
            )

            blocks.add(
                AssignedBlock(
                    node = node,
                    tasks = nodeTasks
                )
            )

            currentIndex = endIndex
        }

        return blocks
    }
}