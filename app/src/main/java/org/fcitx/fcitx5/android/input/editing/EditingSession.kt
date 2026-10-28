/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.editing

/** Owns editing-panel selection state without depending on Android Views or InputConnection. */
class EditingSession {
    var hasSelection: Boolean = false
        private set
    var userSelection: Boolean = false
        private set

    val selectionActive: Boolean
        get() = hasSelection || userSelection

    fun updateSelection(start: Int, end: Int) {
        hasSelection = start != end
    }

    fun move(keyCode: Int) = EditingCommand.MoveCursor(keyCode, selectionActive)

    fun toggleSelection(): EditingCommand? {
        return if (hasSelection) {
            userSelection = false
            EditingCommand.CancelSelection
        } else {
            userSelection = !userSelection
            null
        }
    }

    fun selectAll(): EditingCommand {
        userSelection = true
        return EditingCommand.SelectAll
    }

    fun cut(): EditingCommand = leaveSelectionMode(EditingCommand.Cut)
    fun copy(): EditingCommand = leaveSelectionMode(EditingCommand.Copy)
    fun paste(): EditingCommand = leaveSelectionMode(EditingCommand.Paste)
    fun backspace(): EditingCommand = leaveSelectionMode(EditingCommand.Backspace)
    fun clearAll(): EditingCommand {
        hasSelection = false
        return leaveSelectionMode(EditingCommand.ClearAll)
    }

    fun finishDeletionGesture() {
        hasSelection = false
        userSelection = false
    }

    private fun leaveSelectionMode(command: EditingCommand): EditingCommand {
        userSelection = false
        return command
    }
}
