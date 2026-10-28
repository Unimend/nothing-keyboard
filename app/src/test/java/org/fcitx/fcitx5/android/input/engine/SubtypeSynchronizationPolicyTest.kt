package org.fcitx.fcitx5.android.input.engine

import org.junit.Assert.*
import org.junit.Test

class SubtypeSynchronizationPolicyTest {
    @Test
    fun `platform selection is imported only at the first binding`() {
        val policy = SubtypeSynchronizationPolicy()
        assertTrue(policy.consumeInitialBinding())
        assertFalse(policy.consumeInitialBinding())
        assertFalse(policy.consumeInitialBinding())
    }

    @Test
    fun `own subtype echo is consumed exactly once`() {
        val policy = SubtypeSynchronizationPolicy()
        policy.expectingEcho("pinyin")
        assertFalse(policy.shouldActivate("pinyin"))
        assertTrue(policy.shouldActivate("pinyin"))
    }

    @Test
    fun `different user selection discards stale echo expectation`() {
        val policy = SubtypeSynchronizationPolicy()
        policy.expectingEcho("pinyin")
        assertTrue(policy.shouldActivate("keyboard-us"))
        assertTrue(policy.shouldActivate("pinyin"))
    }
}
