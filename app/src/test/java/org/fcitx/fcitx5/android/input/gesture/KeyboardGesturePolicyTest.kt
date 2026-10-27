/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardGesturePolicyTest {
    @Test
    fun `space horizontal movement requests cursor steps`() {
        val result = KeyboardGesturePolicy.space(
            GestureSnapshot(GesturePhase.Move, countX = -2, totalX = -2, totalY = 0)
        )
        assertEquals(KeyboardGestureAction.MoveCursor(-2), result.action)
        assertTrue(result.consumed)
    }

    @Test
    fun `space upward release opens editing panel`() {
        val result = KeyboardGesturePolicy.space(
            GestureSnapshot(GesturePhase.Up, countX = 0, totalX = 0, totalY = -1)
        )
        assertEquals(KeyboardGestureAction.OpenEditingPanel, result.action)
        assertTrue(result.consumed)
    }

    @Test
    fun `backspace long upward swipe clears all`() {
        val result = KeyboardGesturePolicy.backspace(
            GestureSnapshot(GesturePhase.Up, countX = 0, totalX = 0, totalY = -2)
        )
        assertEquals(KeyboardGestureAction.ClearAllText, result.action)
        assertTrue(result.consumed)
    }

    @Test
    fun `backspace downward swipe undoes`() {
        val result = KeyboardGesturePolicy.backspace(
            GestureSnapshot(GesturePhase.Up, countX = 0, totalX = 0, totalY = 1)
        )
        assertEquals(KeyboardGestureAction.Undo, result.action)
    }

    @Test
    fun `plain backspace release remains available to click handler`() {
        val result = KeyboardGesturePolicy.backspace(
            GestureSnapshot(GesturePhase.Up, countX = 0, totalX = 0, totalY = 0)
        )
        assertEquals(KeyboardGestureAction.DeleteSelection(0), result.action)
        assertFalse(result.consumed)
    }

    @Test
    fun `short upward backspace gesture is consumed without destructive action`() {
        val result = KeyboardGesturePolicy.backspace(
            GestureSnapshot(GesturePhase.Up, countX = 0, totalX = 0, totalY = -1)
        )
        assertNull(result.action)
        assertTrue(result.consumed)
    }
}
