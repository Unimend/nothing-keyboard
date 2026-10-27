/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.editing

import android.view.KeyEvent
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import kotlin.math.absoluteValue

/** The only editing-system class that translates commands into Android editor operations. */
class AndroidEditingCommandExecutor(
    private val service: FcitxInputMethodService
) {
    fun execute(command: EditingCommand) {
        when (command) {
            is EditingCommand.MoveCursor ->
                service.sendCombinationKeyEvents(command.keyCode, shift = command.extendSelection)
            is EditingCommand.ExtendSelection -> {
                val keyCode = if (command.steps > 0) {
                    KeyEvent.KEYCODE_DPAD_RIGHT
                } else {
                    KeyEvent.KEYCODE_DPAD_LEFT
                }
                repeat(command.steps.absoluteValue) {
                    service.sendCombinationKeyEvents(keyCode, shift = true)
                }
            }
            EditingCommand.CancelSelection -> service.cancelSelection()
            EditingCommand.SelectAll -> contextAction(android.R.id.selectAll)
            EditingCommand.Cut -> contextAction(android.R.id.cut)
            EditingCommand.Copy -> contextAction(android.R.id.copy)
            EditingCommand.Paste -> contextAction(android.R.id.paste)
            EditingCommand.Backspace -> service.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            EditingCommand.ClearAll -> service.clearAllText()
            EditingCommand.Undo -> service.undoLastEdit()
        }
    }

    private fun contextAction(id: Int) {
        service.currentInputConnection?.performContextMenuAction(id)
    }
}
