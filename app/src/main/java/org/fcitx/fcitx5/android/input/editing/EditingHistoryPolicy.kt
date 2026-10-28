/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.editing

enum class EditingHistoryEffect {
    Preserve,
    ManageDestructiveEdit,
    ConsumeDestructiveEdit,
    InvalidateDestructiveEdit
}

/** Pure command policy; surfaces and Android adapters never decide history ownership. */
object EditingHistoryPolicy {
    fun effect(command: EditingCommand): EditingHistoryEffect = when (command) {
        is EditingCommand.CommitText,
        EditingCommand.ApplicationUndo,
        EditingCommand.ApplicationRedo,
        EditingCommand.Cut,
        EditingCommand.Paste -> EditingHistoryEffect.InvalidateDestructiveEdit

        EditingCommand.Backspace,
        EditingCommand.ClearAll,
        is EditingCommand.DeletionGestureRelease -> EditingHistoryEffect.ManageDestructiveEdit

        EditingCommand.Undo -> EditingHistoryEffect.ConsumeDestructiveEdit
        else -> EditingHistoryEffect.Preserve
    }
}
