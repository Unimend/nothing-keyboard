package org.fcitx.fcitx5.android.input.wm

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidateAnchorPolicyTest {
    private val insertion = CandidateInsertionMarker(horizontal = 9f, top = 4f, bottom = 8f)
    private val character = CandidateCharacterBounds(left = 2f, top = 3f, right = 7f, bottom = 6f)

    @Test fun `character start anchors candidates ahead of insertion marker`() {
        assertArrayEquals(
            floatArrayOf(2f, 6f, 2f, 3f),
            CandidateAnchorPolicy.rawAnchor(character, insertion, isRtl = false),
            0f
        )
    }

    @Test fun `rtl character start uses right edge`() {
        assertArrayEquals(
            floatArrayOf(7f, 6f, 7f, 3f),
            CandidateAnchorPolicy.rawAnchor(character, insertion, isRtl = true),
            0f
        )
    }

    @Test fun `missing character bounds falls back to insertion marker`() {
        assertArrayEquals(
            floatArrayOf(9f, 8f, 9f, 4f),
            CandidateAnchorPolicy.rawAnchor(null, insertion, isRtl = false),
            0f
        )
    }

    @Test fun `non finite coordinates are rejected`() {
        assertTrue(CandidateAnchorPolicy.isValid(floatArrayOf(1f, 2f, 3f, 4f)))
        assertFalse(CandidateAnchorPolicy.isValid(floatArrayOf(Float.NaN, 2f, 3f, 4f)))
        assertFalse(CandidateAnchorPolicy.isValid(floatArrayOf(1f, Float.POSITIVE_INFINITY, 3f, 4f)))
        assertFalse(CandidateAnchorPolicy.isValid(floatArrayOf(1f, 2f)))
    }
}
