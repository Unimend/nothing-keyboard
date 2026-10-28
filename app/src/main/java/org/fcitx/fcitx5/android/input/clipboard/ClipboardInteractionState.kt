/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.clipboard

enum class ClipboardViewMode {
    Normal, AddMore, EnableListening
}

data class ClipboardInteractionState(
    val listeningEnabled: Boolean = false,
    val contentEmpty: Boolean = true,
    val pendingDeleteIds: List<Int> = emptyList()
) {
    val viewMode: ClipboardViewMode
        get() = when {
            !listeningEnabled -> ClipboardViewMode.EnableListening
            contentEmpty -> ClipboardViewMode.AddMore
            else -> ClipboardViewMode.Normal
        }
}

sealed interface ClipboardInteractionEvent {
    data class ListeningChanged(val enabled: Boolean) : ClipboardInteractionEvent
    data class ContentEmptyChanged(val empty: Boolean) : ClipboardInteractionEvent
    data class DeletesStaged(val ids: List<Int>) : ClipboardInteractionEvent
    data object PendingDeletesCleared : ClipboardInteractionEvent
}

object ClipboardInteractionReducer {
    fun reduce(
        state: ClipboardInteractionState,
        event: ClipboardInteractionEvent
    ): ClipboardInteractionState = when (event) {
        is ClipboardInteractionEvent.ListeningChanged -> state.copy(
            listeningEnabled = event.enabled
        )
        is ClipboardInteractionEvent.ContentEmptyChanged -> state.copy(
            contentEmpty = event.empty
        )
        is ClipboardInteractionEvent.DeletesStaged -> state.copy(
            pendingDeleteIds = (state.pendingDeleteIds + event.ids).distinct()
        )
        ClipboardInteractionEvent.PendingDeletesCleared -> state.copy(
            pendingDeleteIds = emptyList()
        )
    }
}
