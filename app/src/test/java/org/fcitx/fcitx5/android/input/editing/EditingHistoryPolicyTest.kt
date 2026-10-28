/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.editing

import org.junit.Assert.assertEquals
import org.junit.Test

class EditingHistoryPolicyTest {
    @Test
    fun `external text mutations invalidate custom destructive history`() {
        val commands = listOf(
            EditingCommand.CommitText("字"),
            EditingCommand.ApplicationUndo,
            EditingCommand.ApplicationRedo,
            EditingCommand.Cut,
            EditingCommand.Paste
        )
        commands.forEach {
            assertEquals(
                "$it must invalidate custom history",
                EditingHistoryEffect.InvalidateDestructiveEdit,
                EditingHistoryPolicy.effect(it)
            )
        }
    }

    @Test
    fun `destructive commands declare whether they record or consume history`() {
        assertEquals(
            EditingHistoryEffect.ManageDestructiveEdit,
            EditingHistoryPolicy.effect(EditingCommand.Backspace)
        )
        assertEquals(
            EditingHistoryEffect.ManageDestructiveEdit,
            EditingHistoryPolicy.effect(EditingCommand.ClearAll)
        )
        assertEquals(
            EditingHistoryEffect.ConsumeDestructiveEdit,
            EditingHistoryPolicy.effect(EditingCommand.Undo)
        )
    }

    @Test
    fun `navigation selection and copy preserve destructive history`() {
        val commands = listOf(
            EditingCommand.Copy,
            EditingCommand.SelectAll,
            EditingCommand.CancelSelection,
            EditingCommand.MoveCursor(android.view.KeyEvent.KEYCODE_DPAD_LEFT, false),
            EditingCommand.FinishComposition
        )
        commands.forEach {
            assertEquals(
                "$it must preserve custom history",
                EditingHistoryEffect.Preserve,
                EditingHistoryPolicy.effect(it)
            )
        }
    }
}
