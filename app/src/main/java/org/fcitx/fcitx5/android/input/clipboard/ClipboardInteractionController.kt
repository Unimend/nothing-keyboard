/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.clipboard

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.mechdancer.dependency.UniqueComponent

/** Owns clipboard presentation state and serializes every destructive database transaction. */
class ClipboardInteractionController : UniqueComponent<ClipboardInteractionController>() {
    private val mutationMutex = Mutex()

    var state = ClipboardInteractionState()
        private set

    fun setListeningEnabled(enabled: Boolean) {
        update(ClipboardInteractionEvent.ListeningChanged(enabled))
    }

    fun setContentEmpty(empty: Boolean) {
        update(ClipboardInteractionEvent.ContentEmptyChanged(empty))
    }

    suspend fun pin(id: Int) = mutationMutex.withLock { ClipboardManager.pin(id) }

    suspend fun unpin(id: Int) = mutationMutex.withLock { ClipboardManager.unpin(id) }

    suspend fun stageDelete(id: Int): Int = mutationMutex.withLock {
        ClipboardManager.delete(id)
        update(ClipboardInteractionEvent.DeletesStaged(listOf(id)))
        state.pendingDeleteIds.size
    }

    suspend fun stageDeleteAll(skipPinned: Boolean): Int = mutationMutex.withLock {
        val ids = ClipboardManager.deleteAll(skipPinned).toList()
        update(ClipboardInteractionEvent.DeletesStaged(ids))
        state.pendingDeleteIds.size
    }

    suspend fun undoPendingDeletes() = mutationMutex.withLock {
        val ids = state.pendingDeleteIds
        if (ids.isNotEmpty()) ClipboardManager.undoDelete(*ids.toIntArray())
        update(ClipboardInteractionEvent.PendingDeletesCleared)
    }

    suspend fun finalizePendingDeletes() = mutationMutex.withLock {
        if (state.pendingDeleteIds.isNotEmpty()) ClipboardManager.realDelete()
        update(ClipboardInteractionEvent.PendingDeletesCleared)
    }

    suspend fun haveUnpinned(): Boolean = mutationMutex.withLock {
        ClipboardManager.haveUnpinned()
    }

    private fun update(event: ClipboardInteractionEvent) {
        state = ClipboardInteractionReducer.reduce(state, event)
    }
}
