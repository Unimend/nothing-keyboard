/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class InputDevicePresentationControllerTest {
    private class Host : InputDevicePresentationHost {
        val calls = mutableListOf<String>()
        override fun setCandidatePagingMode(mode: Int) { calls += "paging:$mode" }
        override fun monitorCursorAnchor(enabled: Boolean) { calls += "cursor:$enabled" }
        override fun hideStatusIcon() { calls += "status:hide" }
        override fun showCurrentStatusIcon() { calls += "status:show" }
        override fun updateNavigationBar(isVirtualKeyboard: Boolean) { calls += "navbar:$isVirtualKeyboard" }
        override fun focusEngine() { calls += "engine:focus" }
        override fun forceShowInput() { calls += "input:show" }
    }

    @Test
    fun `virtual mode applies one complete presentation transaction`() {
        val host = Host()
        InputDevicePresentationController(host).applyMode(true)
        assertEquals(
            listOf("paging:0", "cursor:false", "status:hide", "navbar:true"),
            host.calls
        )
    }

    @Test
    fun `hardware mode applies one complete presentation transaction`() {
        val host = Host()
        InputDevicePresentationController(host).applyMode(false)
        assertEquals(
            listOf("paging:1", "cursor:true", "status:show", "navbar:false"),
            host.calls
        )
    }

    @Test
    fun `hardware request focuses engine before showing input`() {
        val host = Host()
        InputDevicePresentationController(host).showForHardwareInput()
        assertEquals(listOf("engine:focus", "input:show"), host.calls)
    }
}
