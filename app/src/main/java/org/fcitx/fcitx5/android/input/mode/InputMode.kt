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
