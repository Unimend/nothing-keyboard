/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.editing

sealed interface EditingCommand {
    data class CommitText(val text: String, val cursor: Int = -1) : EditingCommand
    data object FinishComposition : EditingCommand
    data object ApplicationUndo : EditingCommand
    data object ApplicationRedo : EditingCommand
    data class MoveCursor(val keyCode: Int, val extendSelection: Boolean) : EditingCommand
    data class DeletionGestureMove(val steps: Int) : EditingCommand
    data class DeletionGestureRelease(val totalSteps: Int) : EditingCommand
    data object DeletionGestureCancel : EditingCommand
    data object ResetDeletionGesture : EditingCommand
    data object CancelSelection : EditingCommand
    data object SelectAll : EditingCommand
    data object Cut : EditingCommand
    data object Copy : EditingCommand
    data object Paste : EditingCommand
    data object Backspace : EditingCommand
    data object ClearAll : EditingCommand
    data object Undo : EditingCommand
}
