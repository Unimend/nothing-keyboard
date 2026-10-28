package org.fcitx.fcitx5.android.input.clipboard

import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardItemMappingTest {
    @Test
    fun `storage adapter preserves display identity sensitivity and ordering data`() {
        val stored = ClipboardEntry(id = 7, text = "hello 123 中文", pinned = true,
            timestamp = 42L, sensitive = true, type = "text/plain", deleted = false)
        assertEquals(ClipboardItem(7, "hello 123 中文", true, 42L, true), stored.toClipboardItem())
    }
}
