package org.fcitx.fcitx5.android.input.clipboard

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ClipboardInteractionControllerTest {
    private class Storage : ClipboardMutationPort {
        val finalized = mutableListOf<List<Int>>()
        val restored = mutableListOf<List<Int>>()
        var failDelete = false
        var deleteGate: CompletableDeferred<Unit>? = null
        val entered = CompletableDeferred<Unit>()
        override suspend fun pin(id: Int) {}
        override suspend fun unpin(id: Int) {}
        override suspend fun markDeleted(id: Int) {
            entered.complete(Unit)
            deleteGate?.await()
            check(!failDelete) { "storage failure" }
        }
        override suspend fun markAllDeleted(skipPinned: Boolean) = listOf(2, 3)
        override suspend fun restore(ids: List<Int>) { restored += ids.toList() }
        override suspend fun finalizeDeleted(ids: List<Int>) { finalized += ids.toList() }
        override suspend fun haveUnpinned() = true
    }

    @Test
    fun `finalization targets only owned IDs and is idempotent`() = runBlocking {
        val storage = Storage()
        val controller = ClipboardInteractionController(storage)
        controller.stageDelete(1)
        controller.stageDeleteAll(true)
        controller.finalizePendingDeletes()
        controller.finalizePendingDeletes()
        assertEquals(listOf(listOf(1, 2, 3)), storage.finalized)
        assertTrue(controller.state.pendingDeleteIds.isEmpty())
    }

    @Test
    fun `undo serializes behind pending storage mutation`() = runBlocking {
        val storage = Storage().apply { deleteGate = CompletableDeferred() }
        val controller = ClipboardInteractionController(storage)
        val deleting = launch { controller.stageDelete(7) }
        storage.entered.await()
        val restoring = launch { controller.undoPendingDeletes() }
        storage.deleteGate!!.complete(Unit)
        deleting.join()
        restoring.join()
        assertEquals(listOf(listOf(7)), storage.restored)
        controller.finalizePendingDeletes()
        assertTrue(storage.finalized.isEmpty())
    }

    @Test
    fun `failed storage mutation does not advertise undo`() = runBlocking {
        val storage = Storage().apply { failDelete = true }
        val controller = ClipboardInteractionController(storage)
        try {
            controller.stageDelete(9)
            fail("Expected failure")
        } catch (_: IllegalStateException) {}
        assertTrue(controller.state.pendingDeleteIds.isEmpty())
    }
}
