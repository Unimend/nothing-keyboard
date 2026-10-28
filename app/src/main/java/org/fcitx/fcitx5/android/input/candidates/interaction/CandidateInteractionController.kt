/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.candidates.interaction

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.fcitx.fcitx5.android.input.engine.EngineCommandPort
import org.mechdancer.dependency.UniqueComponent

/** Single command and loading-state boundary shared by every candidate surface. */
class CandidateInteractionController(private val commands: EngineCommandPort) :
    UniqueComponent<CandidateInteractionController>() {

    var state = CandidateInteractionState()
        private set

    private val _expandedOffset = MutableSharedFlow<Int>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val expandedOffset = _expandedOffset.asSharedFlow()

    fun replaceCandidates(loadedCount: Int, total: Int) {
        update(CandidateInteractionEvent.CandidatesReplaced(loadedCount, total))
        publishExpandedOffset()
    }

    fun beginLoadMore(): CandidateLoadRequest? {
        val before = state
        update(CandidateInteractionEvent.LoadMoreStarted(before.generation, before.loadedCount))
        return if (!before.loadMoreInFlight && state.loadMoreInFlight) {
            CandidateLoadRequest(before.generation, before.loadedCount)
        } else null
    }

    fun completeLoadMore(request: CandidateLoadRequest, receivedCount: Int, pageSize: Int): Boolean {
        val before = state
        update(
            CandidateInteractionEvent.LoadMoreCompleted(
                request.generation,
                request.offset,
                receivedCount,
                pageSize
            )
        )
        val accepted = state !== before && state.generation == request.generation
        if (accepted) publishExpandedOffset()
        return accepted
    }

    fun failLoadMore(request: CandidateLoadRequest) {
        update(CandidateInteractionEvent.LoadMoreFailed(request.generation))
    }

    fun publishExpandedOffset() {
        _expandedOffset.tryEmit(state.expandedOffset)
    }

    fun select(engineIndex: Int) {
        commands.selectCandidate(engineIndex)
    }

    fun triggerTabAction(actionId: Int) {
        commands.triggerCandidateTabAction(actionId)
    }

    private fun update(event: CandidateInteractionEvent) {
        state = CandidateInteractionReducer.reduce(state, event)
    }
}

data class CandidateLoadRequest(val generation: Int, val offset: Int)
