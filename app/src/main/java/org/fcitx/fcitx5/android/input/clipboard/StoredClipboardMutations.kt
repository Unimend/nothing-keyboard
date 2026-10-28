/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.clipboard

import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager

/** Adapter to existing persistent history; no presentation or undo policy belongs here. */
object StoredClipboardMutations : ClipboardMutationPort {
    override suspend fun pin(id: Int) = ClipboardManager.pin(id)
    override suspend fun unpin(id: Int) = ClipboardManager.unpin(id)
    override suspend fun markDeleted(id: Int) = ClipboardManager.delete(id)
    override suspend fun markAllDeleted(skipPinned: Boolean) =
        ClipboardManager.deleteAll(skipPinned).toList()
    override suspend fun restore(ids: List<Int>) = ClipboardManager.undoDelete(*ids.toIntArray())
    override suspend fun finalizeDeleted(ids: List<Int>) =
        ClipboardManager.finalizeDeleted(ids.toIntArray())
    override suspend fun haveUnpinned() = ClipboardManager.haveUnpinned()
}
