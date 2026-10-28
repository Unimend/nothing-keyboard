/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.mode

import org.junit.Assert.assertEquals
import org.junit.Test

class InputModeReducerTest {
    @Test
    fun editorLayoutDoesNotOverwriteRememberedSymbolMode() {
        val initial = InputModeState(
            rememberedSymbolMode = InputMode.Picker(PickerKind.Symbol)
        )
        val state = InputModeReducer.reduce(
            initial,
            InputModeEvent.KeyboardActivated(
                KeyboardLayout.Number,
                rememberAsSymbolMode = false
            )
        )

        assertEquals(KeyboardLayout.Number, state.currentKeyboardLayout)
        assertEquals(initial.rememberedSymbolMode, state.rememberedSymbolMode)
    }

    @Test
    fun explicitAlternativeLayoutsAreRemembered() {
        val number = InputModeReducer.reduce(
            InputModeState(),
            InputModeEvent.KeyboardActivated(
                KeyboardLayout.Number,
                rememberAsSymbolMode = true
            )
        )
        assertEquals(InputMode.Keyboard(KeyboardLayout.Number), number.rememberedSymbolMode)

        val symbol = InputModeReducer.reduce(
            number,
            InputModeEvent.PickerActivated(
                PickerKind.Symbol,
                rememberAsPicker = true,
                rememberAsSymbolMode = true
            )
        )
        assertEquals(InputMode.Picker(PickerKind.Symbol), symbol.rememberedSymbolMode)
        assertEquals(PickerKind.Symbol, symbol.rememberedPicker)
    }

    @Test
    fun emojiPickerDoesNotReplaceSymbolAlternative() {
        val initial = InputModeState(
            rememberedSymbolMode = InputMode.Keyboard(KeyboardLayout.Number)
        )
        val state = InputModeReducer.reduce(
            initial,
            InputModeEvent.PickerActivated(
                PickerKind.Emoji,
                rememberAsPicker = true,
                rememberAsSymbolMode = false
            )
        )

        assertEquals(initial.rememberedSymbolMode, state.rememberedSymbolMode)
        assertEquals(PickerKind.Emoji, state.rememberedPicker)
    }

    @Test
    fun invalidStoredAlternativeFallsBackToSymbolPicker() {
        assertEquals(
            InputMode.Picker(PickerKind.Symbol),
            InputModeStore.decodeSymbolMode("invalid")
        )
    }
}
