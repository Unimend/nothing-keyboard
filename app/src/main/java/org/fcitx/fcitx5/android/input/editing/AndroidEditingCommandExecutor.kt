/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.editing

import android.view.KeyEvent
import kotlin.math.absoluteValue
import org.fcitx.fcitx5.android.input.session.InputSessionReader

/** The only editing-system class that translates commands into Android editor operations. */
class AndroidEditingCommandExecutor(
    private val editor: EditorController,
    private val session: InputSessionReader,
    private val resetEngine: () -> Unit,
    private val sendCombination: (Int, Boolean, Boolean) -> Unit
) : EditingCommandPort {
    private val deletionGesture = DeletionGestureSession()

    override fun execute(command: EditingCommand) {
        val gestureEffect = when (command) {
            is EditingCommand.DeletionGestureMove -> deletionGesture.move(command.steps, session.current.engine)
            is EditingCommand.DeletionGestureRelease -> deletionGesture.release(command.totalSteps)
            EditingCommand.DeletionGestureCancel -> deletionGesture.cancel()
            else -> { deletionGesture.reset(); null }
        }
        when (gestureEffect) {
            is DeletionGestureEffect.Extend -> extendSelection(gestureEffect.steps)
            DeletionGestureEffect.Delete -> editor.deleteSelection()
            DeletionGestureEffect.ResetComposition -> resetEngine()
            DeletionGestureEffect.CollapseSelection -> editor.cancelSelection()
            null -> Unit
        }
        when (command) {
            is EditingCommand.DeletionGestureMove,
            is EditingCommand.DeletionGestureRelease,
            EditingCommand.DeletionGestureCancel,
            EditingCommand.ResetDeletionGesture -> Unit
            is EditingCommand.CommitText -> editor.commitText(command.text, command.cursor)
            EditingCommand.FinishComposition -> editor.finishComposing()
            EditingCommand.ApplicationUndo -> sendCombination(KeyEvent.KEYCODE_Z, false, true)
            EditingCommand.ApplicationRedo -> sendCombination(KeyEvent.KEYCODE_Z, true, true)
            is EditingCommand.MoveCursor ->
                sendCombination(command.keyCode, command.extendSelection, false)
            EditingCommand.CancelSelection -> editor.cancelSelection()
            EditingCommand.SelectAll -> contextAction(android.R.id.selectAll)
            EditingCommand.Cut -> contextAction(android.R.id.cut, invalidatesUndo = true)
            EditingCommand.Copy -> contextAction(android.R.id.copy)
            EditingCommand.Paste -> contextAction(android.R.id.paste, invalidatesUndo = true)
            EditingCommand.Backspace -> editor.deleteBackward()
            EditingCommand.ClearAll -> editor.clearAllText()
            EditingCommand.Undo -> editor.undoLastEdit()
        }
    }

    private fun extendSelection(steps: Int) {
        val keyCode = if (steps > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        repeat(steps.absoluteValue) { sendCombination(keyCode, true, false) }
    }

    private fun contextAction(id: Int, invalidatesUndo: Boolean = false) {
        editor.contextAction(id, invalidatesUndo)
    }
}
