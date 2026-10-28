package org.fcitx.fcitx5.android.input.editing

import org.fcitx.fcitx5.android.input.session.*
import org.junit.Assert.*
import org.junit.Test

class DeletionGestureSessionTest {
    private val idle = EngineState()
    private val composing = EngineState(clientPreedit = EngineText(listOf(EngineTextSegment("ni"))))

    @Test fun `committed text selects in both directions then deletes once`() {
        val gesture = DeletionGestureSession()
        assertEquals(DeletionGestureEffect.Extend(-1), gesture.move(-1, idle))
        assertEquals(DeletionGestureEffect.Extend(2), gesture.move(2, idle))
        assertEquals(DeletionGestureEffect.Delete, gesture.release(1))
        assertNull(gesture.release(1))
    }
    @Test fun `composition is reset without selecting application text`() {
        val gesture = DeletionGestureSession()
        assertNull(gesture.move(-1, composing))
        assertNull(gesture.move(-1, idle))
        assertEquals(DeletionGestureEffect.ResetComposition, gesture.release(-2))
    }
    @Test fun `loaded candidates block selection even with unknown total`() {
        val gesture = DeletionGestureSession()
        val candidates = EngineState(candidates = EngineCandidateList(-1, listOf(EngineCandidate.Empty)))
        assertNull(gesture.move(1, candidates))
        assertEquals(DeletionGestureEffect.ResetComposition, gesture.release(1))
    }
    @Test fun `cancel only collapses a gesture-created selection`() {
        val gesture = DeletionGestureSession()
        assertNull(gesture.cancel())
        gesture.move(-1, composing)
        assertNull(gesture.cancel())
        gesture.move(-1, idle)
        assertEquals(DeletionGestureEffect.CollapseSelection, gesture.cancel())
        assertNull(gesture.release(-1))
    }
    @Test fun `clear undo or session change can reset state before next gesture`() {
        val gesture = DeletionGestureSession()
        gesture.move(-1, idle)
        gesture.reset()
        assertNull(gesture.move(-1, composing))
        assertEquals(DeletionGestureEffect.ResetComposition, gesture.release(-1))
    }
    @Test fun `zero displacement never deletes or resets composition`() {
        val gesture = DeletionGestureSession()
        assertNull(gesture.move(0, idle))
        assertNull(gesture.release(0))
        gesture.move(-1, idle)
        assertNull(gesture.release(0))
    }
}
