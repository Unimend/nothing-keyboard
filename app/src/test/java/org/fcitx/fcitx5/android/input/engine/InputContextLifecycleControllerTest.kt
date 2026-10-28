/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.engine

import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Test

class InputContextLifecycleControllerTest {
    private val calls = mutableListOf<String>()
    private val commands = EngineCommandPort {
        calls += "engine:$it"
        Job().apply { complete() }
    }
    private val controller = InputContextLifecycleController(commands) { calls += "platform:bind" }

    @Test
    fun `bind activates context before synchronizing platform input method`() {
        controller.bind(42, "example.editor")
        assertEquals(
            listOf("engine:${EngineCommand.ActivateContext(42, "example.editor")}", "platform:bind"),
            calls
        )
    }

    @Test
    fun `unbind deactivates the exact bound uid`() {
        controller.unbind(42)
        assertEquals(listOf("engine:${EngineCommand.DeactivateContext(42)}"), calls)
    }

    @Test
    fun `missing Android binding does not invent a context`() {
        controller.unbind(null)
        assertEquals(emptyList<String>(), calls)
    }
}
