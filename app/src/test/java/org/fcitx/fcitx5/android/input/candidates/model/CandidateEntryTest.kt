/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.candidates.model

import org.fcitx.fcitx5.android.input.session.EngineCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class CandidateEntryTest {
    @Test
    fun `engine indices start at requested offset`() {
        val words = listOf(
            EngineCandidate("", "one", "", false),
            EngineCandidate("", "two", "", false)
        )

        val entries = words.withEngineIndices(offset = 32)

        assertEquals(listOf(32, 33), entries.map { it.engineIndex })
        assertEquals(words, entries.map { it.word })
    }
}
