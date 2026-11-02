/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.candidates.expanded.window

import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CandidatePageScrollPolicyTest {
    @Test
    fun `empty paging snapshot never produces invalid negative target`() {
        assertNull(CandidatePageScrollPolicy.previous(RecyclerView.NO_POSITION, 0))
        assertNull(CandidatePageScrollPolicy.next(RecyclerView.NO_POSITION, 0))
    }

    @Test
    fun `unlaid nonempty snapshot starts from first candidate`() {
        assertEquals(0, CandidatePageScrollPolicy.previous(RecyclerView.NO_POSITION, 12))
        assertEquals(0, CandidatePageScrollPolicy.next(RecyclerView.NO_POSITION, 12))
    }

    @Test
    fun `paging targets stay inside adapter bounds`() {
        assertEquals(0, CandidatePageScrollPolicy.previous(0, 12))
        assertEquals(4, CandidatePageScrollPolicy.previous(5, 12))
        assertEquals(6, CandidatePageScrollPolicy.next(5, 12))
        assertEquals(11, CandidatePageScrollPolicy.next(11, 12))
    }
}
