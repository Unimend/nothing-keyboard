/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.navigation

import org.fcitx.fcitx5.android.input.mode.KeyboardLayout
import org.fcitx.fcitx5.android.input.mode.PickerKind

sealed interface InputNavigationCommand {
    enum class CandidateLayout { Grid, Flexbox }

    data class ShowKeyboard(val layout: KeyboardLayout? = null) : InputNavigationCommand
    data object ShowClipboard : InputNavigationCommand
    /** Return from the current transient surface without letting UI choose a destination. */
    data object Return : InputNavigationCommand
    data object ShowEditing : InputNavigationCommand
    data class ShowPicker(val kind: PickerKind? = null) : InputNavigationCommand
    data object ShowStatus : InputNavigationCommand
    data class ShowExpandedCandidates(val layout: CandidateLayout) : InputNavigationCommand
}
