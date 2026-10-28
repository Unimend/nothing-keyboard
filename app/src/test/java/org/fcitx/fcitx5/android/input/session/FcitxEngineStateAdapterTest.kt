/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.session

import org.fcitx.fcitx5.android.core.CandidateAction
import org.fcitx.fcitx5.android.core.CandidateWord
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.core.FormattedText
import org.fcitx.fcitx5.android.input.engine.FcitxEngineStateAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FcitxEngineStateAdapterTest {
    @Test
    fun inputPanelIsCopiedIntoProjectOwnedModels() {
        val event = FcitxEvent.InputPanelEvent(
            FcitxEvent.InputPanelEvent.Data(
                preedit = FormattedText(arrayOf("ni"), intArrayOf(8), 2),
                auxUp = FormattedText.Empty,
                auxDown = FormattedText.Empty,
                tabs = arrayOf(CandidateAction(3, "Words", false, "", true, true))
            )
        )

        val mapped = FcitxEngineStateAdapter.map(event) as EngineStateEvent.InputPanelChanged

        assertEquals("ni", mapped.value.preedit.plainText)
        assertEquals(
            setOf(EngineTextStyle.Underline),
            mapped.value.preedit.segments.single().styles
        )
        assertEquals(3, mapped.value.tabs.single().id)
        assertEquals(true, mapped.value.tabs.single().checked)
    }

    @Test
    fun candidateArraysBecomeImmutableLists() {
        val source = arrayOf(CandidateWord("1", "你", "person", false))
        val event = FcitxEvent.CandidateListEvent(
            FcitxEvent.CandidateListEvent.Data(1, source)
        )

        val mapped = FcitxEngineStateAdapter.map(event) as EngineStateEvent.CandidatesChanged
        source[0] = CandidateWord.Empty

        assertEquals("你", mapped.value.items.single().text)
        assertEquals(false, mapped.value.items.single().spaceBeforeComment)
    }

    @Test
    fun effectOnlyEventsDoNotEnterPersistentState() {
        assertNull(FcitxEngineStateAdapter.map(FcitxEvent.CommitStringEvent(
            FcitxEvent.CommitStringEvent.Data("text", -1)
        )))
    }
}
