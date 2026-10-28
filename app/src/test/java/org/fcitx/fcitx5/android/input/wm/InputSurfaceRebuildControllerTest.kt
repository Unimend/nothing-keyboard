/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.wm

import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemePreset
import org.junit.Assert.assertEquals
import org.junit.Test

class InputSurfaceRebuildControllerTest {
    private class Signals : InputSurfaceRebuildSignals {
        var keyboard: () -> Unit = {}
        var candidates: () -> Unit = {}
        var theme: (Theme) -> Unit = {}
        var stops = 0

        override fun start(
            keyboardChanged: () -> Unit,
            candidatesChanged: () -> Unit,
            themeChanged: (Theme) -> Unit
        ) {
            keyboard = keyboardChanged
            candidates = candidatesChanged
            theme = themeChanged
        }

        override fun stop() { stops++ }
    }

    private class Host : InputSurfaceRebuildHost {
        val calls = mutableListOf<String>()
        override fun replaceKeyboard(theme: Theme) { calls += "keyboard:${theme.name}" }
        override fun replaceCandidates(theme: Theme) { calls += "candidates:${theme.name}" }
        override fun replaceAll(theme: Theme) { calls += "all:${theme.name}" }
    }

    @Test
    fun `each signal rebuilds only its owned surface scope`() {
        val signals = Signals()
        val host = Host()
        val controller = InputSurfaceRebuildController(signals, { ThemePreset.NothingDark }, host)

        controller.start()
        controller.createInitialSurfaces()
        signals.keyboard()
        signals.candidates()
        signals.theme(ThemePreset.MaterialLight)
        controller.stop()

        assertEquals(
            listOf(
                "all:${ThemePreset.NothingDark.name}",
                "keyboard:${ThemePreset.NothingDark.name}",
                "candidates:${ThemePreset.NothingDark.name}",
                "all:${ThemePreset.MaterialLight.name}"
            ),
            host.calls
        )
        assertEquals(1, signals.stops)
    }
}
