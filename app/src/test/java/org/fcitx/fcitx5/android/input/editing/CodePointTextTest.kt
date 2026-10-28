/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.editing

import org.junit.Assert.assertEquals
import org.junit.Test

class CodePointTextTest {
    @Test
    fun `iterates bmp and supplementary characters without splitting surrogate pairs`() {
        val actual = mutableListOf<Int>()

        "你A🙂".forEachCodePointCompat(actual::add)

        assertEquals(listOf('你'.code, 'A'.code, 0x1F642), actual)
    }

    @Test
    fun `empty text emits no code points`() {
        var calls = 0

        "".forEachCodePointCompat { calls++ }

        assertEquals(0, calls)
    }
}
