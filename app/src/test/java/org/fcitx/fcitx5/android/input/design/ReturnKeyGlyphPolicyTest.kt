/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.design

import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.session.EditorAction
import org.fcitx.fcitx5.android.input.session.EditorSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class ReturnKeyGlyphPolicyTest {
    @Test
    fun `active preedit always shows return glyph`() {
        val editor = EditorSnapshot(action = EditorAction.Send)
        assertEquals(
            R.drawable.ic_pixel_return,
            ReturnKeyGlyphPolicy.resolve(editor, preeditEmpty = false, nothingStyle = true)
        )
    }

    @Test
    fun `empty preedit follows editor action and theme`() {
        val editor = EditorSnapshot(action = EditorAction.Send)
        assertEquals(
            R.drawable.ic_pixel_send,
            ReturnKeyGlyphPolicy.resolve(editor, preeditEmpty = true, nothingStyle = true)
        )
        assertEquals(
            R.drawable.ic_baseline_send_24,
            ReturnKeyGlyphPolicy.resolve(editor, preeditEmpty = true, nothingStyle = false)
        )
    }
}
