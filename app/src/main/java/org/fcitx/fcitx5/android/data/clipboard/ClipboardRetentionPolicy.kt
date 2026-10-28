/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.data.clipboard

data class ClipboardRecency(val id: Int, val timestamp: Long)

/** Receives only active unpinned entries; pending undo and pinned records are never candidates. */
object ClipboardRetentionPolicy {
    fun expiredIds(activeUnpinned: List<ClipboardRecency>, limit: Int): List<Int> =
        activeUnpinned.sortedWith(compareByDescending<ClipboardRecency> { it.timestamp }
            .thenByDescending { it.id }).drop(limit.coerceAtLeast(0)).map { it.id }
}
