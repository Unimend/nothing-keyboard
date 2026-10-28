/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.feedback

import org.fcitx.fcitx5.android.input.design.NothingDesignTokens
import org.junit.Assert.assertEquals
import org.junit.Test

class InputMotionPolicyTest {
    @Test
    fun `disabled motion never delays commands`() {
        assertEquals(0L, InputMotionPolicy.duration(false, 120L))
        assertEquals(0L, InputMotionPolicy.confirmationActionDelay(false, true))
    }

    @Test
    fun `non-nothing themes do not inherit decorative confirmation delay`() {
        assertEquals(0L, InputMotionPolicy.confirmationActionDelay(true, false))
    }

    @Test
    fun `nothing confirmation uses the shared action delay`() {
        assertEquals(
            NothingDesignTokens.Motion.ConfirmationActionDelayMs,
            InputMotionPolicy.confirmationActionDelay(true, true)
        )
    }
}
