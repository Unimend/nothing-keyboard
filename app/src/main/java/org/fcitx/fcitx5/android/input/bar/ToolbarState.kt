/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.bar

enum class ToolbarContent {
    Empty,
    Toolbar,
    Clipboard,
    NumberRow,
    InlineSuggestion
}

enum class NumberRowMode {
    Auto,
    ForceShow,
    ForceHide
}

data class ToolbarState(
    val content: ToolbarContent = ToolbarContent.Empty,
    val toolbarExpanded: Boolean = false,
    val clipboardFresh: Boolean = false,
    val inlineSuggestionPresent: Boolean = false,
    val passwordEditor: Boolean = false,
    val keyboardLayoutNumber: Boolean = false,
    val numberRowMode: NumberRowMode = NumberRowMode.Auto
)

sealed interface ToolbarEvent {
    data class SessionStarted(val passwordEditor: Boolean) : ToolbarEvent
    data class ClipboardChanged(val fresh: Boolean) : ToolbarEvent
    data class InlineSuggestionChanged(val present: Boolean) : ToolbarEvent
    data class KeyboardLayoutChanged(val number: Boolean) : ToolbarEvent
    data object MenuPressed : ToolbarEvent
    data object NumberRowRequested : ToolbarEvent
    data object NumberRowCollapsed : ToolbarEvent
}

object ToolbarReducer {
    fun reduce(state: ToolbarState, event: ToolbarEvent): ToolbarState {
        if (event == ToolbarEvent.MenuPressed) {
            val expanded = state.content != ToolbarContent.Toolbar
            val updated = state.copy(toolbarExpanded = expanded)
            return updated.copy(
                content = if (expanded) ToolbarContent.Toolbar else resolveContent(updated)
            )
        }

        val updated = when (event) {
            is ToolbarEvent.SessionStarted -> state.copy(
                toolbarExpanded = false,
                inlineSuggestionPresent = false,
                passwordEditor = event.passwordEditor,
                numberRowMode = NumberRowMode.Auto
            )
            is ToolbarEvent.ClipboardChanged -> state.copy(clipboardFresh = event.fresh)
            is ToolbarEvent.InlineSuggestionChanged ->
                state.copy(inlineSuggestionPresent = event.present)
            is ToolbarEvent.KeyboardLayoutChanged ->
                state.copy(keyboardLayoutNumber = event.number)
            ToolbarEvent.NumberRowRequested ->
                state.copy(numberRowMode = NumberRowMode.ForceShow)
            ToolbarEvent.NumberRowCollapsed ->
                state.copy(numberRowMode = NumberRowMode.ForceHide)
            ToolbarEvent.MenuPressed -> error("handled above")
        }
        return updated.copy(content = resolveContent(updated))
    }

    private fun resolveContent(state: ToolbarState): ToolbarContent = when {
        state.numberRowMode == NumberRowMode.ForceShow -> ToolbarContent.NumberRow
        state.clipboardFresh -> ToolbarContent.Clipboard
        state.inlineSuggestionPresent -> ToolbarContent.InlineSuggestion
        state.passwordEditor &&
            !state.keyboardLayoutNumber &&
            state.numberRowMode != NumberRowMode.ForceHide -> ToolbarContent.NumberRow
        state.toolbarExpanded -> ToolbarContent.Toolbar
        else -> ToolbarContent.Empty
    }
}
