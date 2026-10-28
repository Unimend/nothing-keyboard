/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.core.KeyStates
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.core.ScancodeMapping
import org.fcitx.fcitx5.android.input.mode.InputMode
import org.fcitx.fcitx5.android.input.mode.PickerKind

sealed class KeyAction {

    data class FcitxKeyAction(
        val act: String,
        val code: Int = ScancodeMapping.charToScancode(act[0]),
        val states: KeyStates = KeyStates.Virtual
    ) : KeyAction()

    data class SymAction(val sym: KeySym, val states: KeyStates = KeyStates.Virtual) : KeyAction()

    data class CommitAction(val text: String) : KeyAction()

    data class CapsAction(val lock: Boolean) : KeyAction()

    data object QuickPhraseAction : KeyAction()

    data object UnicodeAction : KeyAction()

    data object LangSwitchAction : KeyAction()

    data object ShowInputMethodPickerAction : KeyAction()

    data class LayoutSwitchAction(val target: InputMode? = null) : KeyAction()

    data class CandidatePageAction(val direction: Direction) : KeyAction() {
        enum class Direction { Previous, Next }
    }

    data class MoveSelectionAction(val steps: Int) : KeyAction()

    data class DeleteSelectionAction(val totalCnt: Int = 0) : KeyAction()

    data object ClearAllTextAction : KeyAction()

    data object UndoAction : KeyAction()

    data object ShowTextEditingAction : KeyAction()

    data class PickerSwitchAction(val kind: PickerKind? = null) : KeyAction()

    data object SpaceLongPressAction : KeyAction()
}
