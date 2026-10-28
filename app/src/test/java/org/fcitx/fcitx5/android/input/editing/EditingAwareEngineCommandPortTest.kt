/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.editing

import kotlinx.coroutines.Job
import org.fcitx.fcitx5.android.input.engine.EngineCommand
import org.fcitx.fcitx5.android.input.engine.EngineCommandPort
import org.fcitx.fcitx5.android.input.engine.EngineKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditingAwareEngineCommandPortTest {
    @Test
    fun `text and candidate intents invalidate before reaching engine`() {
        val order = mutableListOf<String>()
        val delegate = EngineCommandPort {
            order += "engine"
            Job().apply { complete() }
        }
        val port = EditingAwareEngineCommandPort(delegate) { order += "invalidate" }

        port.submit(EngineCommand.SendKeyText("n"))
        assertEquals(listOf("invalidate", "engine"), order)

        order.clear()
        port.submit(EngineCommand.SelectCandidate(0))
        assertEquals(listOf("invalidate", "engine"), order)
    }

    @Test
    fun `navigation reset and backspace preserve controller owned history`() {
        val preserved = listOf(
            EngineCommand.SendKey(EngineKey.ArrowLeft),
            EngineCommand.SendKey(EngineKey.ArrowRight),
            EngineCommand.SendKey(EngineKey.Backspace),
            EngineCommand.Reset,
            EngineCommand.FocusOutIn
        )
        preserved.forEach {
            assertFalse("$it must preserve destructive history", EngineCommandHistoryPolicy.invalidatesDestructiveHistory(it))
        }
    }

    @Test
    fun `all software text key forms invalidate history`() {
        val mutations = listOf(
            EngineCommand.SendKeyText("拼"),
            EngineCommand.SendKey(EngineKey.Space),
            EngineCommand.SendKey(EngineKey.Return),
            EngineCommand.SendKey(EngineKey.Unicode('字'.code)),
            EngineCommand.SendKey(EngineKey.KeypadDigit(2)),
            EngineCommand.SendKey(EngineKey.KeypadOperator(EngineKey.KeypadOperator.Operator.Add))
        )
        mutations.forEach {
            assertTrue("$it must invalidate destructive history", EngineCommandHistoryPolicy.invalidatesDestructiveHistory(it))
        }
    }
}
