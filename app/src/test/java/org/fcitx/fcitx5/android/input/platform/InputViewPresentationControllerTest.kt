/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.platform

import org.fcitx.fcitx5.android.input.session.EditorSessionSnapshot
import org.fcitx.fcitx5.android.input.session.EditorSnapshot
import org.fcitx.fcitx5.android.input.session.SessionRange
import org.junit.Assert.assertEquals
import org.junit.Test

class InputViewPresentationControllerTest {
    private class Host(private val cursorMonitoring: Boolean = true) : InputViewPresentationHost {
        val calls = mutableListOf<String>()
        override fun startVirtualInput(session: EditorSessionSnapshot) { calls += "virtual" }
        override fun enableCursorMonitoring(): Boolean {
            calls += "cursor:on"
            return cursorMonitoring
        }
        override fun showCandidateFallback() { calls += "anchor:fallback" }
        override fun showCurrentStatusIcon() { calls += "status:show" }
        override fun invalidateCandidateAnchor() { calls += "anchor:invalidate" }
        override fun finishInputDeviceView() { calls += "device:finish" }
        override fun disableCursorMonitoring() { calls += "cursor:off" }
        override fun hideStatusIcon() { calls += "status:hide" }
        override fun dismissDialogs() { calls += "dialogs:dismiss" }
    }

    private val session = EditorSessionSnapshot(
        editor = EditorSnapshot(packageName = "test"),
        selection = SessionRange(0, 0),
        restarting = false,
        password = false
    )

    @Test
    fun `virtual start enters only the keyboard surface`() {
        val host = Host()
        InputViewPresentationController(host).start(true, session)
        assertEquals(listOf("virtual"), host.calls)
    }

    @Test
    fun `hardware start falls back before showing status when cursor monitoring fails`() {
        val host = Host(cursorMonitoring = false)
        InputViewPresentationController(host).start(false, session)
        assertEquals(listOf("cursor:on", "anchor:fallback", "status:show"), host.calls)
    }

    @Test
    fun `finish wraps lifecycle with complete presentation cleanup`() {
        val host = Host()
        InputViewPresentationController(host).finish { host.calls += "lifecycle:finish" }
        assertEquals(
            listOf(
                "anchor:invalidate", "device:finish", "lifecycle:finish",
                "cursor:off", "status:hide", "dialogs:dismiss"
            ),
            host.calls
        )
    }
}
