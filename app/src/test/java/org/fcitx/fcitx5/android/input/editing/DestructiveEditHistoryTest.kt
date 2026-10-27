/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.editing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DestructiveEditHistoryTest {
    @Test
    fun `record can be consumed only once`() {
        val history = DestructiveEditHistory()
        val edit = DeletedTextEdit(2, "中文", 4)

        history.record(edit)

        assertEquals(edit, history.consume())
        assertNull(history.consume())
    }

    @Test
    fun `new typing can invalidate destructive undo`() {
        val history = DestructiveEditHistory()
        history.record(DeletedTextEdit(0, "old", 3))

        history.clear()

        assertNull(history.consume())
    }

    @Test
    fun `empty deletion is not recorded`() {
        val history = DestructiveEditHistory()
        history.record(DeletedTextEdit(0, "", 0))
        assertNull(history.consume())
    }

    @Test
    fun `a deletion after undo creates a fresh undo cycle`() {
        val history = DestructiveEditHistory()
        val edit = DeletedTextEdit(2, "中文", 4)

        history.record(edit)
        assertEquals(edit, history.consume())

        history.record(edit)
        assertEquals(edit, history.consume())
    }
}
