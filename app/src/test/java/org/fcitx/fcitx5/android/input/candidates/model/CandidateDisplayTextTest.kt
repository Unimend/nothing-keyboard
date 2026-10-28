package org.fcitx.fcitx5.android.input.candidates.model

import org.fcitx.fcitx5.android.input.session.EngineCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class CandidateDisplayTextTest {
    @Test
    fun `display and measurement share comment spacing`() {
        val candidate = EngineCandidate("1", "你好abc", "ni3 hao3", true)
        assertEquals("你好abc ni3 hao3", candidate.textForDisplay())
        assertEquals("你好abcni3 hao3", candidate.copy(spaceBeforeComment = false).textForDisplay())
    }

    @Test
    fun `blank comments add no invisible width`() {
        val candidate = EngineCandidate("1", "123", "  ", true)
        assertEquals("123", candidate.textForDisplay())
        assertEquals("", candidate.commentForDisplay())
    }
}
