/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.clipboard

import org.fcitx.fcitx5.android.data.clipboard.ClipboardSystem

/** Adapter to existing persistent history; no presentation or undo policy belongs here. */
object StoredClipboardMutations : ClipboardMutationPort {
    override suspend fun pin(id: Int) = ClipboardSystem.pin(id)
    override suspend fun unpin(id: Int) = ClipboardSystem.unpin(id)
    override suspend fun markDeleted(id: Int) = ClipboardSystem.delete(id)
    override suspend fun markAllDeleted(skipPinned: Boolean) =
        ClipboardSystem.deleteAll(skipPinned).toList()
    override suspend fun restore(ids: List<Int>) = ClipboardSystem.undoDelete(*ids.toIntArray())
    override suspend fun finalizeDeleted(ids: List<Int>) =
        ClipboardSystem.finalizeDeleted(ids.toIntArray())
    override suspend fun haveUnpinned() = ClipboardSystem.haveUnpinned()
}
