/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.editing

import android.view.KeyEvent
import kotlin.math.absoluteValue

/** The only editing-system class that translates commands into Android editor operations. */
class AndroidEditingCommandExecutor(
    private val editor: EditorController,
    private val sendCombination: (Int, Boolean, Boolean) -> Unit
) : EditingCommandPort {
    override fun execute(command: EditingCommand) {
        when (command) {
            is EditingCommand.CommitText -> editor.commitText(command.text, command.cursor)
            EditingCommand.FinishComposition -> editor.finishComposing()
            EditingCommand.ApplicationUndo -> sendCombination(KeyEvent.KEYCODE_Z, false, true)
            EditingCommand.ApplicationRedo -> sendCombination(KeyEvent.KEYCODE_Z, true, true)
            is EditingCommand.MoveCursor ->
                sendCombination(command.keyCode, command.extendSelection, false)
            is EditingCommand.ExtendSelection -> {
                val keyCode = if (command.steps > 0) {
                    KeyEvent.KEYCODE_DPAD_RIGHT
                } else {
                    KeyEvent.KEYCODE_DPAD_LEFT
                }
                repeat(command.steps.absoluteValue) {
                    sendCombination(keyCode, true, false)
                }
            }
            EditingCommand.CancelSelection -> editor.cancelSelection()
            EditingCommand.SelectAll -> contextAction(android.R.id.selectAll)
            EditingCommand.Cut -> contextAction(android.R.id.cut, invalidatesUndo = true)
            EditingCommand.Copy -> contextAction(android.R.id.copy)
            EditingCommand.Paste -> contextAction(android.R.id.paste, invalidatesUndo = true)
            EditingCommand.Backspace -> editor.deleteBackward()
            EditingCommand.DeleteSelection -> editor.deleteSelection()
            EditingCommand.ClearAll -> editor.clearAllText()
            EditingCommand.Undo -> editor.undoLastEdit()
        }
    }

    private fun contextAction(id: Int, invalidatesUndo: Boolean = false) {
        editor.contextAction(id, invalidatesUndo)
    }
}
