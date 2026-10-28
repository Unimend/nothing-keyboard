/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.autofill

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineSuggestionPolicyTest {
    @Test
    fun `inline suggestions require both preference and virtual keyboard`() {
        assertFalse(InlineSuggestionPolicy.isAvailable(false, false))
        assertFalse(InlineSuggestionPolicy.isAvailable(false, true))
        assertFalse(InlineSuggestionPolicy.isAvailable(true, false))
        assertTrue(InlineSuggestionPolicy.isAvailable(true, true))
    }
}
