/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.clipboard

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.map
import kotlinx.coroutines.flow.map
import org.fcitx.fcitx5.android.data.clipboard.ClipboardSystem
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardEntry

internal fun ClipboardEntry.toClipboardItem() = ClipboardItem(id, text, pinned, timestamp, sensitive)

/** The only history reader that knows persistent entities and the singleton listener API. */
object StoredClipboardHistory : ClipboardHistoryPort {
    override val itemCount get() = ClipboardSystem.itemCount
    override val lastItem get() = ClipboardSystem.lastEntry?.toClipboardItem()
    override fun pages() = Pager(PagingConfig(pageSize = 16)) {
        ClipboardSystem.allEntries()
    }.flow.map { page -> page.map { it.toClipboardItem() } }

    override fun observe(listener: ClipboardUpdateListener): ClipboardObservation {
        val bridge = ClipboardSystem.UpdateListener { listener.onUpdate(it.toClipboardItem()) }
        ClipboardSystem.addUpdateListener(bridge)
        return ClipboardObservation { ClipboardSystem.removeUpdateListener(bridge) }
    }
}
