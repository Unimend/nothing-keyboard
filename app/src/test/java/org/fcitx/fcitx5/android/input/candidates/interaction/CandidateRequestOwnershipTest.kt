package org.fcitx.fcitx5.android.input.candidates.interaction

import kotlinx.coroutines.Job
import org.fcitx.fcitx5.android.input.engine.EngineCommandPort
import org.junit.Assert.*
import org.junit.Test

class CandidateRequestOwnershipTest {
    private fun controller() = CandidateInteractionController(
        EngineCommandPort { Job().apply { complete() } })

    @Test fun `stale failure cannot cancel retry at the same offset`() {
        val controller = controller()
        controller.replaceCandidates(16, 100)
        val first = controller.beginLoadMore()!!
        controller.failLoadMore(first)
        val retry = controller.beginLoadMore()!!
        assertNotEquals(first.requestId, retry.requestId)
        controller.failLoadMore(first)
        assertTrue(controller.state.loadMoreInFlight)
        assertFalse(controller.completeLoadMore(first, 32, 32))
        assertTrue(controller.completeLoadMore(retry, 32, 32))
        assertEquals(48, controller.state.loadedCount)
    }

    @Test fun `replacement invalidates an outstanding page`() {
        val controller = controller()
        controller.replaceCandidates(16, 100)
        val old = controller.beginLoadMore()!!
        controller.replaceCandidates(2, 2)
        assertFalse(controller.completeLoadMore(old, 32, 32))
        controller.failLoadMore(old)
        assertEquals(2, controller.state.loadedCount)
        assertFalse(controller.state.canExpand)
        assertEquals(controller.state, controller.snapshots.value)
    }

    @Test fun `empty state cannot expand and unknown nonempty total can`() {
        val controller = controller()
        assertFalse(controller.state.canExpand)
        controller.replaceCandidates(16, -1)
        assertTrue(controller.state.canExpand)
        val page = controller.beginLoadMore()!!
        controller.completeLoadMore(page, 0, 32)
        assertFalse(controller.state.canExpand)
    }

    @Test fun `successive full pages advance offsets until a short page closes the stream`() {
        val controller = controller()
        controller.replaceCandidates(8, -1)

        val first = controller.beginLoadMore()!!
        assertEquals(8, first.offset)
        assertTrue(controller.completeLoadMore(first, 32, 32))
        assertEquals(40, controller.state.loadedCount)
        assertFalse(controller.state.reachedEnd)

        val second = controller.beginLoadMore()!!
        assertEquals(40, second.offset)
        assertTrue(controller.completeLoadMore(second, 7, 32))
        assertEquals(47, controller.state.loadedCount)
        assertTrue(controller.state.reachedEnd)
        assertNull(controller.beginLoadMore())

        assertFalse(controller.completeLoadMore(first, 32, 32))
        assertEquals(47, controller.state.loadedCount)
    }
}
