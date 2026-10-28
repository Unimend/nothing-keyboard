package org.fcitx.fcitx5.android.input.autofill

import org.junit.Assert.*
import org.junit.Test

class InlineSuggestionOrderTest {
    @Test
    fun `first pinned stays fixed and additional pinned precede ordinary items`() {
        val result = orderInlineSuggestions(listOf("a", "pin1", "b", "pin2", "pin3")) {
            it.startsWith("pin")
        }
        assertEquals("pin1", result.pinned)
        assertEquals(listOf("pin2", "pin3", "a", "b"), result.scrollable)
    }
    @Test
    fun `ordinary response preserves all items in order`() {
        val result = orderInlineSuggestions(listOf(1, 2, 3)) { false }
        assertNull(result.pinned)
        assertEquals(listOf(1, 2, 3), result.scrollable)
    }
    @Test
    fun `empty response has no content`() {
        val result = orderInlineSuggestions(emptyList<Int>()) { true }
        assertNull(result.pinned)
        assertTrue(result.scrollable.isEmpty())
    }
}
