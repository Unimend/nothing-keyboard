/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.clipboard

/** Storage boundary. Controllers own undo state; storage never chooses which batch to finalize. */
interface ClipboardMutationPort {
    suspend fun pin(id: Int)
    suspend fun unpin(id: Int)
    suspend fun markDeleted(id: Int)
    suspend fun markAllDeleted(skipPinned: Boolean): List<Int>
    suspend fun restore(ids: List<Int>)
    suspend fun finalizeDeleted(ids: List<Int>)
    suspend fun haveUnpinned(): Boolean
}
