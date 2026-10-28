/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.clipboard

import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardInteractionReducerTest {
    @Test
    fun `disabled listening always shows enable instructions`() {
        val state = ClipboardInteractionState(listeningEnabled = false, contentEmpty = false)
        assertEquals(ClipboardViewMode.EnableListening, state.viewMode)
    }

    @Test
    fun `enabled empty clipboard shows add-more instructions`() {
        val state = ClipboardInteractionState(listeningEnabled = true, contentEmpty = true)
        assertEquals(ClipboardViewMode.AddMore, state.viewMode)
    }

    @Test
    fun `enabled clipboard with content shows entries`() {
        val state = ClipboardInteractionState(listeningEnabled = true, contentEmpty = false)
        assertEquals(ClipboardViewMode.Normal, state.viewMode)
    }

    @Test
    fun `staged deletes accumulate without duplicate ids`() {
        val first = ClipboardInteractionReducer.reduce(
            ClipboardInteractionState(),
            ClipboardInteractionEvent.DeletesStaged(listOf(3, 5))
        )
        val second = ClipboardInteractionReducer.reduce(
            first,
            ClipboardInteractionEvent.DeletesStaged(listOf(5, 8))
        )
        assertEquals(listOf(3, 5, 8), second.pendingDeleteIds)
    }

    @Test
    fun `completed transaction clears pending deletes`() {
        val state = ClipboardInteractionState(pendingDeleteIds = listOf(3, 5))
        val result = ClipboardInteractionReducer.reduce(
            state,
            ClipboardInteractionEvent.PendingDeletesCleared
        )
        assertEquals(emptyList<Int>(), result.pendingDeleteIds)
    }
}
