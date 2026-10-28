/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.candidates.interaction

data class CandidateInteractionState(
    val generation: Int = 0,
    val loadedCount: Int = 0,
    val total: Int = -1,
    val loadMoreInFlight: Boolean = false,
    val reachedEnd: Boolean = false
) {
    val expandedOffset: Int
        get() = loadedCount
}

sealed interface CandidateInteractionEvent {
    data class CandidatesReplaced(val loadedCount: Int, val total: Int) : CandidateInteractionEvent
    data class LoadMoreStarted(val generation: Int, val offset: Int) : CandidateInteractionEvent
    data class LoadMoreCompleted(
        val generation: Int,
        val requestedOffset: Int,
        val receivedCount: Int,
        val pageSize: Int
    ) : CandidateInteractionEvent
    data class LoadMoreFailed(val generation: Int) : CandidateInteractionEvent
}

object CandidateInteractionReducer {
    fun reduce(
        state: CandidateInteractionState,
        event: CandidateInteractionEvent
    ): CandidateInteractionState = when (event) {
        is CandidateInteractionEvent.CandidatesReplaced -> {
            val loaded = event.loadedCount.coerceAtLeast(0)
            state.copy(
                generation = state.generation + 1,
                loadedCount = loaded,
                total = event.total,
                loadMoreInFlight = false,
                reachedEnd = event.total >= 0 && loaded >= event.total
            )
        }
        is CandidateInteractionEvent.LoadMoreStarted -> {
            if (
                event.generation != state.generation ||
                event.offset != state.loadedCount ||
                state.loadMoreInFlight ||
                state.reachedEnd
            ) state else state.copy(loadMoreInFlight = true)
        }
        is CandidateInteractionEvent.LoadMoreCompleted -> {
            if (
                event.generation != state.generation ||
                event.requestedOffset != state.loadedCount ||
                !state.loadMoreInFlight
            ) state else {
                val loaded = state.loadedCount + event.receivedCount.coerceAtLeast(0)
                state.copy(
                    loadedCount = loaded,
                    loadMoreInFlight = false,
                    reachedEnd = event.receivedCount < event.pageSize ||
                        (state.total >= 0 && loaded >= state.total)
                )
            }
        }
        is CandidateInteractionEvent.LoadMoreFailed -> {
            if (event.generation != state.generation) state
            else state.copy(loadMoreInFlight = false)
        }
    }
}
