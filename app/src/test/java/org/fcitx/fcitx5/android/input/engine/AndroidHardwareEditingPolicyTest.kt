/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.engine

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidHardwareEditingPolicyTest {
    @Test
    fun `physical text key down invalidates custom undo`() {
        assertTrue(invalidates(KeyEvent.KEYCODE_A, 'a'.code))
        assertTrue(invalidates(KeyEvent.KEYCODE_ENTER))
        assertTrue(invalidates(KeyEvent.KEYCODE_SPACE, ' '.code))
        assertTrue(invalidates(KeyEvent.KEYCODE_DEL))
        assertTrue(invalidates(KeyEvent.KEYCODE_FORWARD_DEL))
    }

    @Test
    fun `key up modifiers and navigation preserve custom undo`() {
        assertFalse(invalidates(KeyEvent.KEYCODE_A, 'a'.code, KeyEvent.ACTION_UP))
        assertFalse(invalidates(KeyEvent.KEYCODE_SHIFT_LEFT))
        assertFalse(invalidates(KeyEvent.KEYCODE_DPAD_LEFT))
    }

    private fun invalidates(keyCode: Int, unicode: Int = 0, action: Int = KeyEvent.ACTION_DOWN) =
        AndroidHardwareEditingPolicy.invalidatesDestructiveHistory(action, keyCode, unicode)
}
