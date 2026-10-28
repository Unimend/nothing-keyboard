/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineStateReducerTest {
    @Test
    fun engineStateStartsEmptyAndNotReady() {
        val state = EngineState()

        assertFalse(state.ready)
        assertTrue(state.clientPreedit.isEmpty)
        assertTrue(state.inputPanel.preedit.isEmpty)
        assertTrue(state.candidates.items.isEmpty())
        assertEquals(null, state.inputMethod)
    }

    @Test
    fun eachEngineEventOnlyChangesItsOwnedState() {
        val preedit = EngineText(listOf(EngineTextSegment("ni", 0)), 2)
        val candidates = EngineCandidateList(
            total = 1,
            items = listOf(EngineCandidate("1", "你", "", true))
        )

        val withPreedit = EngineStateReducer.reduce(
            EngineState(), EngineStateEvent.ClientPreeditChanged(preedit)
        )
        val withCandidates = EngineStateReducer.reduce(
            withPreedit, EngineStateEvent.CandidatesChanged(candidates)
        )

        assertEquals(preedit, withCandidates.clientPreedit)
        assertEquals(candidates, withCandidates.candidates)
        assertFalse(withCandidates.ready)
    }

    @Test
    fun observedEngineActivityMarksStateOperational() {
        val state = EngineStateReducer.reduce(EngineState(), EngineStateEvent.Operational)

        assertTrue(state.ready)
    }

    @Test
    fun statusAreaUpdatesActionsAndInputMethodAtomically() {
        val inputMethod = EngineInputMethod(
            "pinyin", "Pinyin", "拼音", "拼", "zh_CN", "", "pinyin", true, "", "", ""
        )
        val action = EngineStatusAction(1, false, true, true, "punctuation", "", "", "", emptyList())

        val state = EngineStateReducer.reduce(
            EngineState(), EngineStateEvent.StatusAreaChanged(listOf(action), inputMethod)
        )

        assertEquals(inputMethod, state.inputMethod)
        assertEquals(listOf(action), state.statusActions)
    }

    @Test
    fun resetDropsEveryEngineOwnedValue() {
        val dirty = EngineState(
            ready = true,
            clientPreedit = EngineText(listOf(EngineTextSegment("abc", 0)), 3),
            candidates = EngineCandidateList(2, listOf(EngineCandidate("1", "a", "", true)))
        )

        assertEquals(EngineState(), EngineStateReducer.reduce(dirty, EngineStateEvent.Reset))
    }
}
