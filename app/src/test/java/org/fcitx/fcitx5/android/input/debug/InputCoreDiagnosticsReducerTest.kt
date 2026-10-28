/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.debug

import org.junit.Assert.assertEquals
import org.junit.Test

class InputCoreDiagnosticsReducerTest {
    @Test
    fun `events update only their own counter and last value`() {
        val start = InputCoreDiagnosticsSnapshot(engineEventCount = 4, navigationCount = 2)
        val result = reduceInputDiagnostics(start, InputDiagnosticEvent.EngineEvent("Ready"))
        assertEquals(5, result.engineEventCount)
        assertEquals("Ready", result.lastEngineEvent)
        assertEquals(2, result.navigationCount)
    }

    @Test
    fun `sequential events preserve every count`() {
        val result = List(100) { InputDiagnosticEvent.EngineCommand("Command-$it") }
            .fold(InputCoreDiagnosticsSnapshot()) { state, event ->
                reduceInputDiagnostics(state, event)
            }
        assertEquals(100, result.engineCommandCount)
        assertEquals("Command-99", result.lastEngineCommand)
    }
}
