/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.navigation

import org.fcitx.fcitx5.android.input.picker.PickerWindow

sealed interface InputNavigationCommand {
    data class ShowKeyboard(val layout: String? = null) : InputNavigationCommand
    data object ShowClipboard : InputNavigationCommand
    data object ShowEditing : InputNavigationCommand
    data class ShowPicker(val key: PickerWindow.Key? = null) : InputNavigationCommand
}
