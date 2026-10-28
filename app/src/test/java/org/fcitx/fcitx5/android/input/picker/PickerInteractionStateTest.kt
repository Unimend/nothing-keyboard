/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.picker

import org.fcitx.fcitx5.android.input.mode.PickerKind
import org.junit.Assert.assertEquals
import org.junit.Test

class PickerInteractionStateTest {
    @Test
    fun `each picker keeps an independent position`() {
        var state = PickerInteractionState()
        state = PickerInteractionReducer.reduce(
            state,
            PickerInteractionEvent.PageSelected(PickerKind.Symbol, 3, 2)
        )
        state = PickerInteractionReducer.reduce(
            state,
            PickerInteractionEvent.PageSelected(PickerKind.Emoji, 5, 1)
        )

        assertEquals(PickerPosition(3, 2), state.position(PickerKind.Symbol))
        assertEquals(PickerPosition(5, 1), state.position(PickerKind.Emoji))
        assertEquals(PickerPosition(), state.position(PickerKind.Emoticon))
    }

    @Test
    fun `negative page input is normalized`() {
        val state = PickerInteractionReducer.reduce(
            PickerInteractionState(),
            PickerInteractionEvent.PageSelected(PickerKind.Symbol, -2, -4)
        )

        assertEquals(PickerPosition(0, 0), state.position(PickerKind.Symbol))
    }

    @Test
    fun `saved page resolves within its category`() {
        val ranges = listOf(0..0, 1..3, 4..8)

        assertEquals(7, resolvePickerPage(PickerPosition(2, 3), ranges))
    }

    @Test
    fun `saved page clamps after data shrinks`() {
        val ranges = listOf(0..0, 1..2)

        assertEquals(2, resolvePickerPage(PickerPosition(1, 20), ranges))
    }

    @Test
    fun `missing category falls back to first content category`() {
        val ranges = listOf(0..0, 1..4)

        assertEquals(1, resolvePickerPage(PickerPosition(99, 2), ranges))
    }
}
