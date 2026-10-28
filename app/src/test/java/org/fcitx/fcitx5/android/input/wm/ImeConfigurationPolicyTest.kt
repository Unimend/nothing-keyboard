/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.wm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeConfigurationPolicyTest {
    private val keyboard = 1 shl 0
    private val keyboardHidden = 1 shl 1
    private val theme = 1 shl 2
    private val orientation = 1 shl 3
    private val local = keyboard or keyboardHidden or theme

    @Test
    fun `locally handled configuration changes do not trigger Android rebuild`() {
        assertFalse(ImeConfigurationPolicy.shouldDelegateToSystem(0, local))
        assertFalse(ImeConfigurationPolicy.shouldDelegateToSystem(keyboard, local))
        assertFalse(ImeConfigurationPolicy.shouldDelegateToSystem(keyboard or theme, local))
    }

    @Test
    fun `any unhandled configuration bit delegates to Android`() {
        assertTrue(ImeConfigurationPolicy.shouldDelegateToSystem(orientation, local))
        assertTrue(ImeConfigurationPolicy.shouldDelegateToSystem(keyboard or orientation, local))
    }
}
