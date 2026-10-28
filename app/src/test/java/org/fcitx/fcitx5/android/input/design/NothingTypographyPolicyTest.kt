/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun mixedContentOnlyStylesLatinAndDigitsWithDoto() {
        assertTrue(NothingTypographyPolicy.usesDoto('A'))
        assertTrue(NothingTypographyPolicy.usesDoto('7'))
        assertFalse(NothingTypographyPolicy.usesDoto('你'))
        assertFalse(NothingTypographyPolicy.usesDoto('@'))
        assertFalse(NothingTypographyPolicy.usesDoto('！'))
        val mixedLabel = "?123"
        assertFalse(NothingTypographyPolicy.usesDoto(mixedLabel[0]))
        assertTrue(mixedLabel.drop(1).all(NothingTypographyPolicy::usesDoto))
    }

    @Test
    fun pureLatinAndNumbersUseWholeDotoTypeface() {
        assertTrue(NothingTypographyPolicy.canUseWholeDotoTypeface("Q"))
        assertTrue(NothingTypographyPolicy.canUseWholeDotoTypeface("PINYIN 123"))
        assertFalse(NothingTypographyPolicy.canUseWholeDotoTypeface("1?#"))
        assertFalse(NothingTypographyPolicy.canUseWholeDotoTypeface("杭州"))
    }

    @Test
    fun semanticStylesKeepContentAndControlsSeparated() {
        assertEquals(NothingTextRole.AdaptiveContent, NothingTextStyle.KeyPrimary.role)
        assertEquals(NothingTextRole.AdaptiveContent, NothingTextStyle.Candidate.role)
        assertEquals(NothingTextRole.ControlLabel, NothingTextStyle.ControlLabel.role)
        assertEquals(NothingTextRole.SystemContent, NothingTextStyle.ClipboardEntry.role)
    }
}
