/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.candidates.interaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidateInteractionReducerTest {
    @Test
    fun `replacement starts a new generation and resets loading`() {
        val loading = CandidateInteractionState(
            generation = 4,
            loadedCount = 8,
            total = 20,
            loadMoreInFlight = true
        )

        val result = CandidateInteractionReducer.reduce(
            loading,
            CandidateInteractionEvent.CandidatesReplaced(3, 9)
        )

        assertEquals(5, result.generation)
        assertEquals(3, result.loadedCount)
        assertEquals(9, result.total)
        assertFalse(result.loadMoreInFlight)
        assertFalse(result.reachedEnd)
    }

    @Test
    fun `stale page cannot mutate a newer candidate generation`() {
        val current = CandidateInteractionState(
            generation = 3,
            loadedCount = 6,
            total = 18,
            loadMoreInFlight = true
        )

        val result = CandidateInteractionReducer.reduce(
            current,
            CandidateInteractionEvent.LoadMoreCompleted(2, 6, 6, 6)
        )

        assertEquals(current, result)
    }

    @Test
    fun `short page marks candidate stream as exhausted`() {
        val loading = CandidateInteractionState(
            generation = 2,
            loadedCount = 8,
            total = -1,
            loadMoreInFlight = true
        )

        val result = CandidateInteractionReducer.reduce(
            loading,
            CandidateInteractionEvent.LoadMoreCompleted(2, 8, 3, 8)
        )

        assertEquals(11, result.loadedCount)
        assertFalse(result.loadMoreInFlight)
        assertTrue(result.reachedEnd)
    }

    @Test
    fun `duplicate load request is ignored while one is active`() {
        val loading = CandidateInteractionState(
            generation = 1,
            loadedCount = 4,
            total = 20,
            loadMoreInFlight = true
        )

        val result = CandidateInteractionReducer.reduce(
            loading,
            CandidateInteractionEvent.LoadMoreStarted(1, 4)
        )

        assertEquals(loading, result)
    }

    @Test
    fun `loaded candidates remain expandable after the horizontal stream reaches its end`() {
        val state = CandidateInteractionState(loadedCount = 37, total = 37, reachedEnd = true)

        assertTrue(state.canExpand)
    }
}
