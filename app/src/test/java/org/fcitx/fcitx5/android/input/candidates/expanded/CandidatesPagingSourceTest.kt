/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.candidates.expanded

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CandidatesPagingSourceTest {
    @Test
    fun `full page continues from actual loaded boundary`() {
        assertEquals(49, nextCandidatePageKey(1, 48, 48, 100))
    }

    @Test
    fun `page ending exactly at total has no next key`() {
        assertNull(nextCandidatePageKey(1, 48, 48, 49))
    }

    @Test
    fun `short page ends unknown-total stream`() {
        assertNull(nextCandidatePageKey(32, 7, 48, -1))
    }

    @Test
    fun `empty result cannot create a self-looping key`() {
        assertNull(nextCandidatePageKey(32, 0, 48, -1))
    }
}
