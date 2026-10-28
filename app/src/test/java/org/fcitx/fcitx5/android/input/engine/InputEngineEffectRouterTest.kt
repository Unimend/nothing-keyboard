/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.engine

import org.fcitx.fcitx5.android.input.session.EngineInputMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class InputEngineEffectRouterTest {
    private class Host : InputEngineEffectHost {
        val calls = mutableListOf<String>()
        override fun forwardKey(effect: EngineEffect.ForwardKey) { calls += "forward:${effect.androidKeyCode}" }
        override fun inputMethodChanged(effect: EngineEffect.InputMethodChanged) {
            calls += "changed:${effect.inputMethod.uniqueName}"
        }
        override fun inputMethodSwitched(effect: EngineEffect.InputMethodSwitched) {
            calls += "switched:${effect.reason}"
        }
    }

    @Test
    fun `editor consumption has priority over platform routing`() {
        val host = Host()
        val router = InputEngineEffectRouter({ true }, host)
        router.handle(EngineEffect.ForwardKey(0, 67, 0, 0, false))
        assertEquals(emptyList<String>(), host.calls)
    }

    @Test
    fun `platform effects route to exactly one destination`() {
        val host = Host()
        val router = InputEngineEffectRouter({ false }, host)
        router.handle(EngineEffect.ForwardKey(0, 67, 0, 0, false))
        router.handle(EngineEffect.InputMethodChanged(EngineInputMethod(
            "keyboard-us", "US", "English", "EN", "en", "", "keyboard", false,
            "", "", ""
        )))
        router.handle(EngineEffect.InputMethodSwitched(EngineSwitchReason.Trigger))
        assertEquals(
            listOf("forward:67", "changed:keyboard-us", "switched:Trigger"),
            host.calls
        )
    }

    @Test
    fun `unconsumed editor-only effect cannot leak into platform host`() {
        val host = Host()
        InputEngineEffectRouter({ false }, host).handle(EngineEffect.CommitText("text", 1))
        assertEquals(emptyList<String>(), host.calls)
    }
}
