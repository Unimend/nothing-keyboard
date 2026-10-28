/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import org.fcitx.fcitx5.android.input.engine.EngineKey
import org.fcitx.fcitx5.android.input.engine.EngineKeyModifier
import org.fcitx.fcitx5.android.input.engine.VirtualKeyModifiers
import org.fcitx.fcitx5.android.input.mode.InputMode
import org.fcitx.fcitx5.android.input.mode.PickerKind

sealed class KeyAction {

    data class TextKeyAction(
        val act: String,
        val modifiers: Set<EngineKeyModifier> = VirtualKeyModifiers
    ) : KeyAction()

    data class SymAction(
        val key: EngineKey,
        val modifiers: Set<EngineKeyModifier> = VirtualKeyModifiers
    ) : KeyAction()

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
