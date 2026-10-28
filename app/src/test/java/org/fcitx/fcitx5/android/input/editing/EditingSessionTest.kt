/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.editing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditingSessionTest {
    @Test
    fun `cancel clears transient selection without deletion`() {
        val session = EditingSession()
        session.toggleSelection()
        session.updateSelection(1, 5)
        session.finishDeletionGesture()
        assertFalse(session.selectionActive)
        assertFalse(session.hasSelection)
    }

    @Test
    fun `selection mode extends cursor movement`() {
        val session = EditingSession()
        assertNull(session.toggleSelection())

        assertEquals(EditingCommand.MoveCursor(21, true), session.move(21))
        assertTrue(session.selectionActive)
    }

    @Test
    fun `existing selection toggles to cancel command`() {
        val session = EditingSession()
        session.updateSelection(2, 5)

        assertEquals(EditingCommand.CancelSelection, session.toggleSelection())
        assertFalse(session.userSelection)
    }

    @Test
    fun `clipboard command leaves manual selection mode`() {
        val session = EditingSession()
        session.toggleSelection()

        assertEquals(EditingCommand.Copy, session.copy())
        assertFalse(session.userSelection)
    }

    @Test
    fun `select all activates selection mode before editor callback`() {
        val session = EditingSession()

        assertEquals(EditingCommand.SelectAll, session.selectAll())
        assertTrue(session.userSelection)
    }

    @Test
    fun `clear all immediately collapses local selection state`() {
        val session = EditingSession()
        session.updateSelection(1, 5)
        session.toggleSelection()

        assertEquals(EditingCommand.ClearAll, session.clearAll())
        assertFalse(session.hasSelection)
        assertFalse(session.userSelection)
        assertFalse(session.selectionActive)
    }

    @Test
    fun `delete selection immediately collapses local selection state`() {
        val session = EditingSession()
        session.updateSelection(1, 5)
        session.toggleSelection()

        session.finishDeletionGesture()
        assertFalse(session.hasSelection)
        assertFalse(session.userSelection)
        assertFalse(session.selectionActive)
    }

}
