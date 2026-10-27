/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.design

import org.junit.Assert.assertEquals
import org.junit.Test

class NothingTypographyPolicyTest {
    @Test
    fun latinAndNumbersUseDotoCategory() {
        assertEquals(
            KeyboardTextKind.LatinOrNumber,
            NothingTypographyPolicy.classify("Pinyin 123")
        )
    }

    @Test
    fun cjkTakesPriorityInMixedCandidateText() {
        assertEquals(
            KeyboardTextKind.Cjk,
            NothingTypographyPolicy.classify("杭州 Hangzhou")
        )
    }

    @Test
    fun punctuationStaysInSystemSymbolCategory() {
        assertEquals(
            KeyboardTextKind.Symbol,
            NothingTypographyPolicy.classify("？！@#")
        )
    }
}
