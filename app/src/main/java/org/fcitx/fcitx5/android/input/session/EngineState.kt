/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.session

data class EngineTextSegment(val text: String, val formatFlags: Int)

data class EngineText(
    val segments: List<EngineTextSegment> = emptyList(),
    val cursor: Int = -1
) {
    val plainText: String get() = segments.joinToString(separator = "") { it.text }
    val isEmpty: Boolean get() = segments.all { it.text.isEmpty() }
}

data class EngineCandidate(
    val label: String,
    val text: String,
    val comment: String,
    val spaceBeforeComment: Boolean
) {
    companion object {
        val Empty = EngineCandidate("", "", "", false)
    }
}

data class EngineCandidateAction(
    val id: Int,
    val text: String,
    val separator: Boolean,
    val icon: String,
    val checkable: Boolean,
    val checked: Boolean
)

data class EngineInputPanel(
    val preedit: EngineText = EngineText(),
    val auxiliaryUp: EngineText = EngineText(),
    val auxiliaryDown: EngineText = EngineText(),
    val tabs: List<EngineCandidateAction> = emptyList()
)

data class EngineCandidateList(
    val total: Int = -1,
    val items: List<EngineCandidate> = emptyList()
)

data class EngineInputMethod(
    val uniqueName: String,
    val name: String,
    val nativeName: String,
    val label: String,
    val languageCode: String,
    val icon: String,
    val addon: String,
    val configurable: Boolean,
    val subModeName: String,
    val subModeLabel: String,
    val subModeIcon: String
)

data class EngineStatusAction(
    val id: Int,
    val separator: Boolean,
    val checkable: Boolean,
    val checked: Boolean,
    val name: String,
    val icon: String,
    val shortText: String,
    val longText: String,
    val children: List<EngineStatusAction>
)

enum class EngineCandidateLayout { Unspecified, Vertical, Horizontal }

data class EnginePagedCandidates(
    val items: List<EngineCandidate> = emptyList(),
    val cursorIndex: Int = -1,
    val layout: EngineCandidateLayout = EngineCandidateLayout.Unspecified,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false
)

data class EngineState(
    val ready: Boolean = false,
    val clientPreedit: EngineText = EngineText(),
    val inputPanel: EngineInputPanel = EngineInputPanel(),
    val candidates: EngineCandidateList = EngineCandidateList(),
    val pagedCandidates: EnginePagedCandidates = EnginePagedCandidates(),
    val inputMethod: EngineInputMethod? = null,
    val statusActions: List<EngineStatusAction> = emptyList()
)

sealed interface EngineStateEvent {
    /** The bridge observed a live event, stronger evidence than a missed one-shot Ready event. */
    data object Operational : EngineStateEvent
    data class ClientPreeditChanged(val value: EngineText) : EngineStateEvent
    data class InputPanelChanged(val value: EngineInputPanel) : EngineStateEvent
    data class CandidatesChanged(val value: EngineCandidateList) : EngineStateEvent
    data class PagedCandidatesChanged(val value: EnginePagedCandidates) : EngineStateEvent
    data class InputMethodChanged(val value: EngineInputMethod) : EngineStateEvent
    data class StatusAreaChanged(
        val actions: List<EngineStatusAction>,
        val inputMethod: EngineInputMethod
    ) : EngineStateEvent
    data object Reset : EngineStateEvent
}

object EngineStateReducer {
    fun reduce(state: EngineState, event: EngineStateEvent): EngineState = when (event) {
        EngineStateEvent.Operational -> state.copy(ready = true)
        is EngineStateEvent.ClientPreeditChanged -> state.copy(clientPreedit = event.value)
        is EngineStateEvent.InputPanelChanged -> state.copy(inputPanel = event.value)
        is EngineStateEvent.CandidatesChanged -> state.copy(candidates = event.value)
        is EngineStateEvent.PagedCandidatesChanged -> state.copy(pagedCandidates = event.value)
        is EngineStateEvent.InputMethodChanged -> state.copy(inputMethod = event.value)
        is EngineStateEvent.StatusAreaChanged -> state.copy(
            statusActions = event.actions,
            inputMethod = event.inputMethod
        )
        EngineStateEvent.Reset -> EngineState()
    }
}
