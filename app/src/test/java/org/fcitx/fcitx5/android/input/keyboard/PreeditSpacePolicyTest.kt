/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.input.session.EngineInputMethod
import org.fcitx.fcitx5.android.input.session.EngineInputPanel
import org.fcitx.fcitx5.android.input.session.EngineState
import org.fcitx.fcitx5.android.input.session.EngineText
import org.fcitx.fcitx5.android.input.session.EngineTextSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreeditSpacePolicyTest {
    private fun text(value: String) = EngineText(listOf(EngineTextSegment(value)))

    private fun state(language: String, raw: String, rendered: String = raw) = EngineState(
        clientPreedit = text(rendered),
        inputPanel = EngineInputPanel(preedit = text(raw)),
        inputMethod = EngineInputMethod(
            "test", "Test", "Test", "T", language, "", "", false, "", "", ""
        )
    )

    @Test
    fun `Chinese Space outputs raw pinyin instead of the rendered candidate`() {
        assertEquals("nihao", PreeditSpacePolicy.rawTextOrNull(state("zh_CN", "nihao", "你好")))
    }

    @Test
    fun `Chinese Space falls back to client preedit when panel preedit is absent`() {
        assertEquals("nihao", PreeditSpacePolicy.rawTextOrNull(state("zh_CN", "", "nihao")))
    }

    @Test
    fun `non Chinese and empty composition keep normal Space behavior`() {
        assertNull(PreeditSpacePolicy.rawTextOrNull(state("en_US", "hello")))
        assertNull(PreeditSpacePolicy.rawTextOrNull(state("zh_CN", "", "")))
    }
}
