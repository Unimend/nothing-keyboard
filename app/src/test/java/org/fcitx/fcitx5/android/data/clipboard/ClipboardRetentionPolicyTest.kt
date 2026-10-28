package org.fcitx.fcitx5.android.data.clipboard

import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardRetentionPolicyTest {
    @Test
    fun `recently reused older entry is kept by timestamp not insertion id`() {
        assertEquals(listOf(2), ClipboardRetentionPolicy.expiredIds(
            listOf(ClipboardRecency(1, 300), ClipboardRecency(2, 100), ClipboardRecency(3, 200)), 2))
    }
    @Test
    fun `equal timestamps retain exact limit deterministically`() {
        assertEquals(listOf(2, 1), ClipboardRetentionPolicy.expiredIds(
            listOf(ClipboardRecency(1, 100), ClipboardRecency(3, 100), ClipboardRecency(2, 100)), 1))
    }
    @Test
    fun `zero and negative limits expire every supplied active entry`() {
        val items = listOf(ClipboardRecency(1, 100))
        assertEquals(listOf(1), ClipboardRetentionPolicy.expiredIds(items, 0))
        assertEquals(listOf(1), ClipboardRetentionPolicy.expiredIds(items, -1))
    }
    @Test
    fun `larger limit expires nothing`() {
        assertEquals(emptyList<Int>(), ClipboardRetentionPolicy.expiredIds(
            listOf(ClipboardRecency(1, 100)), 3))
    }
}
