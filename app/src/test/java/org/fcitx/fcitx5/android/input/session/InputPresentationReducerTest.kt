/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.session

import org.junit.Assert.assertEquals
import org.junit.Test

class InputPresentationReducerTest {
    @Test
    fun emptyEngineUsesIdlePresentation() {
        assertEquals(InputPresentationState(), InputPresentationReducer.reduce(EngineState()))
    }

    @Test
    fun clientAndPanelPreeditShareOneEmptyState() {
        val client = EngineState(clientPreedit = EngineText(listOf(EngineTextSegment("ni", 0))))
        val panel = EngineState(
            inputPanel = EngineInputPanel(preedit = EngineText(listOf(EngineTextSegment("hao", 0))))
        )

        assertEquals(false, InputPresentationReducer.reduce(client).preeditEmpty)
        assertEquals(false, InputPresentationReducer.reduce(panel).preeditEmpty)
    }

    @Test
    fun candidateVisibilityIsDerivedOnce() {
        val state = EngineState(
            candidates = EngineCandidateList(items = listOf(EngineCandidate.Empty))
        )

        assertEquals(
            InputPresentationState(
                preeditEmpty = true,
                candidatesEmpty = false,
                showCandidates = true
            ),
            InputPresentationReducer.reduce(state)
        )
    }
}
