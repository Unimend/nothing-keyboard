/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.mode

import org.fcitx.fcitx5.android.input.settings.InputSettings
import org.mechdancer.dependency.UniqueComponent

class InputModeStore : UniqueComponent<InputModeStore>() {
    private var lastSymbolStorage by InputSettings.internal.lastSymbolLayout
    private var lastPickerStorage by InputSettings.internal.lastPickerType

    var state: InputModeState = InputModeState(
        rememberedSymbolMode = decodeSymbolMode(lastSymbolStorage),
        rememberedPicker = PickerKind.fromStorage(lastPickerStorage) ?: PickerKind.Emoji
    )
        private set

    fun activateKeyboard(layout: KeyboardLayout, rememberAsSymbolMode: Boolean) {
        update(InputModeEvent.KeyboardActivated(layout, rememberAsSymbolMode))
    }

    fun activatePicker(
        kind: PickerKind,
        rememberAsPicker: Boolean = true,
        rememberAsSymbolMode: Boolean = false
    ) {
        update(InputModeEvent.PickerActivated(kind, rememberAsPicker, rememberAsSymbolMode))
    }

    private fun update(event: InputModeEvent) {
        val previous = state
        state = InputModeReducer.reduce(previous, event)
        if (state.rememberedSymbolMode != previous.rememberedSymbolMode) {
            lastSymbolStorage = state.rememberedSymbolMode.storageValue
        }
        if (state.rememberedPicker != previous.rememberedPicker) {
            lastPickerStorage = state.rememberedPicker.name
        }
    }

    companion object {
        fun decodeSymbolMode(value: String): InputMode =
            KeyboardLayout.fromStorage(value)?.let(InputMode::Keyboard)
                ?: PickerKind.fromStorage(value)?.let(InputMode::Picker)
                ?: InputMode.Picker(PickerKind.Symbol)

        private val InputMode.storageValue: String
            get() = when (this) {
                is InputMode.Keyboard -> layout.name
                is InputMode.Picker -> kind.name
            }
    }
}
