/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.navigation

import org.fcitx.fcitx5.android.input.picker.PickerWindow

sealed interface InputNavigationCommand {
    enum class CandidateLayout { Grid, Flexbox }

    data class ShowKeyboard(val layout: String? = null) : InputNavigationCommand
    data object ShowClipboard : InputNavigationCommand
    data object ShowEditing : InputNavigationCommand
    data class ShowPicker(val key: PickerWindow.Key? = null) : InputNavigationCommand
    data object ShowStatus : InputNavigationCommand
    data object ShowEditorInfo : InputNavigationCommand
    data class ShowExpandedCandidates(val layout: CandidateLayout) : InputNavigationCommand
}
