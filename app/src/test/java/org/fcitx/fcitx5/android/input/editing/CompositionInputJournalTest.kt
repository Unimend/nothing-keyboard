package org.fcitx.fcitx5.android.input.editing

import org.fcitx.fcitx5.android.input.engine.EngineCommand
import org.fcitx.fcitx5.android.input.engine.EngineKey
import org.junit.Assert.assertEquals
import org.junit.Test

class CompositionInputJournalTest {
    @Test
    fun `records raw pinyin independently of rendered preedit`() {
        val journal = CompositionInputJournal()
        "nihao".forEach { journal.observe(EngineCommand.SendKeyText(it.toString())) }
        assertEquals("nihao", journal.rawInput)
    }

    @Test
    fun `tracks edits within source input and resets at engine boundary`() {
        val journal = CompositionInputJournal()
        journal.observe(EngineCommand.SendKeyText("nihao"))
        journal.observe(EngineCommand.SendKey(EngineKey.ArrowLeft))
        journal.observe(EngineCommand.SendKey(EngineKey.Backspace))
        journal.observe(EngineCommand.SendKeyText("a"))
        assertEquals("nihao", journal.rawInput)

        journal.observe(EngineCommand.Reset)
        assertEquals("", journal.rawInput)
    }
}
