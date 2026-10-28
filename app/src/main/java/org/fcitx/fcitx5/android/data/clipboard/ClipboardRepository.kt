/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.data.clipboard

import androidx.paging.PagingSource
import androidx.room.withTransaction
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardDao
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardDatabase
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardEntry
import kotlin.coroutines.CoroutineContext

/** Owns persistence and transactions; it has no Android clipboard or preference knowledge. */
internal class ClipboardRepository(
    private val database: ClipboardDatabase,
    private val dao: ClipboardDao,
    private val ioContext: CoroutineContext
) {
    suspend fun itemCount() = dao.itemCount()
    suspend fun get(id: Int) = dao.get(id)
    suspend fun haveUnpinned() = dao.haveUnpinned()
    fun allEntries(): PagingSource<Int, ClipboardEntry> = dao.allEntries()
    suspend fun pin(id: Int, pinned: Boolean) = dao.updatePinStatus(id, pinned)
    suspend fun updateText(id: Int, text: String) = dao.updateText(id, text)

    suspend fun markDeleted(id: Int) = database.withTransaction {
        dao.markAsDeleted(id)
        dao.itemCount()
    }

    suspend fun markAllDeleted(skipPinned: Boolean): Pair<IntArray, Int> = database.withTransaction {
        val ids = if (skipPinned) dao.findUnpinnedIds() else dao.findAllIds()
        dao.markAsDeleted(*ids)
        ids to dao.itemCount()
    }

    suspend fun restore(ids: IntArray) = database.withTransaction {
        dao.undoDelete(*ids)
        dao.itemCount()
    }

    suspend fun finalizeDeleted(ids: IntArray) {
        if (ids.isNotEmpty()) dao.finalizeDeleted(ids)
    }

    suspend fun clear(): Int = withContext(ioContext) {
        database.clearAllTables()
        dao.itemCount()
    }

    suspend fun trimTo(limit: Int): Int = database.withTransaction {
        removeOutdated(limit)
        dao.itemCount()
    }

    suspend fun store(entry: ClipboardEntry, limit: Int): ClipboardEntry {
        dao.find(entry.text, entry.sensitive)?.let {
            dao.updateTime(it.id, entry.timestamp)
            return it.copy(timestamp = entry.timestamp)
        }
        return database.withTransaction {
            val rowId = dao.insert(entry)
            removeOutdated(limit)
            dao.get(rowId) ?: entry
        }
    }

    private suspend fun removeOutdated(limit: Int) {
        val expired = ClipboardRetentionPolicy.expiredIds(
            dao.getAllUnpinned().map { ClipboardRecency(it.id, it.timestamp) }, limit
        )
        if (expired.isNotEmpty()) dao.deleteExpiredUnpinned(expired.toIntArray())
    }
}
