/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.bar

import org.junit.Assert.assertEquals
import org.junit.Test

class ToolbarReducerTest {
    @Test
    fun priorityIsStableAcrossCompetingContent() {
        var state = ToolbarState(passwordEditor = true)
        state = ToolbarReducer.reduce(state, ToolbarEvent.InlineSuggestionChanged(true))
        assertEquals(ToolbarContent.InlineSuggestion, state.content)

        state = ToolbarReducer.reduce(state, ToolbarEvent.ClipboardChanged(true))
        assertEquals(ToolbarContent.Clipboard, state.content)

        state = ToolbarReducer.reduce(state, ToolbarEvent.NumberRowRequested)
        assertEquals(ToolbarContent.NumberRow, state.content)
    }

    @Test
    fun sessionResetCollapsesToolbarAndClearsTransientInputState() {
        val state = ToolbarReducer.reduce(
            ToolbarState(
                content = ToolbarContent.Toolbar,
                toolbarExpanded = true,
                inlineSuggestionPresent = true,
                numberRowMode = NumberRowMode.ForceShow
            ),
            ToolbarEvent.SessionStarted(passwordEditor = false)
        )

        assertEquals(ToolbarContent.Empty, state.content)
        assertEquals(false, state.toolbarExpanded)
        assertEquals(false, state.inlineSuggestionPresent)
        assertEquals(NumberRowMode.Auto, state.numberRowMode)
    }

    @Test
    fun menuCanTemporarilyReplaceSuggestedContent() {
        val clipboard = ToolbarReducer.reduce(
            ToolbarState(),
            ToolbarEvent.ClipboardChanged(true)
        )
        val toolbar = ToolbarReducer.reduce(clipboard, ToolbarEvent.MenuPressed)

        assertEquals(ToolbarContent.Toolbar, toolbar.content)
        assertEquals(true, toolbar.clipboardFresh)
        assertEquals(
            ToolbarContent.Clipboard,
            ToolbarReducer.reduce(toolbar, ToolbarEvent.MenuPressed).content
        )
        assertEquals(
            ToolbarContent.Clipboard,
            ToolbarReducer.reduce(toolbar, ToolbarEvent.KeyboardLayoutChanged(false)).content
        )
    }

    @Test
    fun collapsedPasswordNumberRowStaysHidden() {
        var state = ToolbarReducer.reduce(
            ToolbarState(),
            ToolbarEvent.SessionStarted(passwordEditor = true)
        )
        assertEquals(ToolbarContent.NumberRow, state.content)

        state = ToolbarReducer.reduce(state, ToolbarEvent.NumberRowCollapsed)
        assertEquals(ToolbarContent.Empty, state.content)
    }
}
