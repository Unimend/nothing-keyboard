package org.fcitx.fcitx5.android.input.candidates.interaction

import kotlinx.coroutines.Job
import org.fcitx.fcitx5.android.input.engine.EngineCommand
import org.fcitx.fcitx5.android.input.engine.EngineCommandPort
import org.junit.Assert.assertEquals
import org.junit.Test

class CandidateCommandPortTest {
    @Test
    fun `candidate controller works with a replaceable command port without Android service`() {
        val sent = mutableListOf<EngineCommand>()
        val port = EngineCommandPort { sent += it; Job().apply { complete() } }
        val controller = CandidateInteractionController(port)
        controller.select(-1)
        controller.select(8)
        controller.triggerTabAction(3)
        port.triggerCandidateAction(-1, 9)
        port.triggerCandidateAction(8, 9)
        port.offsetCandidatePage(0)
        port.offsetCandidatePage(-1)
        assertEquals(listOf(EngineCommand.SelectCandidate(8),
            EngineCommand.TriggerCandidateTabAction(3),
            EngineCommand.TriggerCandidateAction(8, 9),
            EngineCommand.OffsetCandidatePage(-1)), sent)
    }
}
