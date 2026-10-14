package com.tesis.albumcolaborativo.data.distribution

import com.tesis.albumcolaborativo.data.local.entities.TaskQueueEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskDistributorTest {

    private val distributor = TaskDistributor()

    private fun createTasks(count: Int): List<TaskQueueEntity> {
        return (1..count).map { index ->
            TaskQueueEntity(
                taskId = "task-$index",
                albumId = "album-1",
                tipoTarea = "PROCESAMIENTO_IMAGEN",
                payloadJson = """{"imagePath":"image-$index.jpg"}""",
                estadoSincronizacion = "PENDIENTE"
            )
        }
    }

    @Test
    fun `10 tareas se distribuyen 4 3 3 entre 3 nodos`() {

        val tasks = createTasks(10)

        val nodes = listOf(
            ProcessingNode("node-1", "192.168.1.10", 50051),
            ProcessingNode("node-2", "192.168.1.11", 50052),
            ProcessingNode("node-3", "192.168.1.12", 50053)
        )

        val blocks = distributor.distribute(tasks, nodes)

        assertEquals(3, blocks.size)

        assertEquals(4, blocks[0].tasks.size)
        assertEquals(3, blocks[1].tasks.size)
        assertEquals(3, blocks[2].tasks.size)
    }

    @Test
    fun `30 tareas se distribuyen 10 10 10 entre 3 nodos`() {

        val tasks = createTasks(30)

        val nodes = listOf(
            ProcessingNode("node-1", "192.168.1.10", 50051),
            ProcessingNode("node-2", "192.168.1.11", 50052),
            ProcessingNode("node-3", "192.168.1.12", 50053)
        )

        val blocks = distributor.distribute(tasks, nodes)

        assertEquals(3, blocks.size)

        assertEquals(10, blocks[0].tasks.size)
        assertEquals(10, blocks[1].tasks.size)
        assertEquals(10, blocks[2].tasks.size)
    }

    @Test
    fun `1 tarea se asigna a un solo nodo`() {

        val tasks = createTasks(1)

        val nodes = listOf(
            ProcessingNode("node-1", "192.168.1.10", 50051),
            ProcessingNode("node-2", "192.168.1.11", 50052),
            ProcessingNode("node-3", "192.168.1.12", 50053)
        )

        val blocks = distributor.distribute(tasks, nodes)

        assertEquals(3, blocks.size)

        assertEquals(1, blocks[0].tasks.size)
        assertEquals(0, blocks[1].tasks.size)
        assertEquals(0, blocks[2].tasks.size)
    }

    @Test
    fun `sin nodos se usa procesamiento local`() {

        val tasks = createTasks(10)

        val blocks = distributor.distribute(
            tasks = tasks,
            nodes = emptyList()
        )

        assertEquals(1, blocks.size)

        assertEquals("LOCAL", blocks[0].node.nodeId)

        assertEquals(10, blocks[0].tasks.size)
    }

    @Test
    fun `nodos no disponibles son ignorados`() {

        val tasks = createTasks(10)

        val nodes = listOf(
            ProcessingNode(
                nodeId = "node-1",
                address = "192.168.1.10",
                port = 50051,
                available = true
            ),
            ProcessingNode(
                nodeId = "node-2",
                address = "192.168.1.11",
                port = 50052,
                available = false
            ),
            ProcessingNode(
                nodeId = "node-3",
                address = "192.168.1.12",
                port = 50053,
                available = true
            )
        )

        val blocks = distributor.distribute(tasks, nodes)

        assertEquals(2, blocks.size)

        assertTrue(
            blocks.none {
                it.node.nodeId == "node-2"
            }
        )

        assertEquals(5, blocks[0].tasks.size)
        assertEquals(5, blocks[1].tasks.size)
    }

    @Test
    fun `bloque rechazado se reasigna sin incluir al nodo que lo rechazo`() {

        val nodes = listOf(
            ProcessingNode("node-1", "192.168.1.10", 50051),
            ProcessingNode("node-2", "192.168.1.11", 50052),
            ProcessingNode("node-3", "192.168.1.12", 50053)
        )

        val rejectedBlock = AssignedBlock(
            node = nodes[0],
            tasks = createTasks(5)
        )

        val blocks = distributor.reassign(rejectedBlock, nodes)

        assertEquals(2, blocks.size)

        assertTrue(
            blocks.none {
                it.node.nodeId == "node-1"
            }
        )

        assertEquals(5, blocks.sumOf { it.tasks.size })
    }

    @Test
    fun `bloque rechazado sin otros nodos no se reasigna`() {

        val localNode = ProcessingNode(
            nodeId = TaskDistributor.LOCAL_NODE_ID,
            address = TaskDistributor.LOCAL_NODE_ID,
            port = 0
        )

        val rejectedBlock = AssignedBlock(
            node = localNode,
            tasks = createTasks(5)
        )

        assertTrue(distributor.reassign(rejectedBlock, emptyList()).isEmpty())

        assertTrue(distributor.reassign(rejectedBlock, listOf(localNode)).isEmpty())
    }
}
