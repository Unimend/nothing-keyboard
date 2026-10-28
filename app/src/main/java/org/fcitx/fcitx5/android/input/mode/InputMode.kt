/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.mode

enum class KeyboardLayout {
    Text,
    Number;

    companion object {
        fun fromStorage(value: String): KeyboardLayout? = entries.find { it.name == value }
    }
}

enum class PickerKind {
    Symbol,
    Emoji,
    Emoticon;

    companion object {
        fun fromStorage(value: String): PickerKind? = entries.find { it.name == value }
    }
}

sealed interface InputMode {
    data class Keyboard(val layout: KeyboardLayout) : InputMode
    data class Picker(val kind: PickerKind) : InputMode
}

data class InputModeState(
    val currentKeyboardLayout: KeyboardLayout = KeyboardLayout.Text,
    val rememberedSymbolMode: InputMode = InputMode.Picker(PickerKind.Symbol),
    val rememberedPicker: PickerKind = PickerKind.Emoji
)

sealed interface InputModeEvent {
    data class KeyboardActivated(
        val layout: KeyboardLayout,
        val rememberAsSymbolMode: Boolean
    ) : InputModeEvent

    data class PickerActivated(
        val kind: PickerKind,
        val rememberAsPicker: Boolean,
        val rememberAsSymbolMode: Boolean
    ) : InputModeEvent
}

object InputModeReducer {
    fun reduce(state: InputModeState, event: InputModeEvent): InputModeState = when (event) {
        is InputModeEvent.KeyboardActivated -> state.copy(
            currentKeyboardLayout = event.layout,
            rememberedSymbolMode = if (event.rememberAsSymbolMode) {
                InputMode.Keyboard(event.layout)
            } else {
                state.rememberedSymbolMode
            }
        )
        is InputModeEvent.PickerActivated -> state.copy(
            rememberedPicker = if (event.rememberAsPicker) event.kind else state.rememberedPicker,
            rememberedSymbolMode = if (event.rememberAsSymbolMode) {
                InputMode.Picker(event.kind)
            } else {
                state.rememberedSymbolMode
            }
        )
    }
}
