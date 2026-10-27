/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2024 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.candidates.expanded

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.fcitx.fcitx5.android.core.CandidateWord
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import timber.log.Timber

data class IndexedCandidateWord(val index: Int, val candidate: CandidateWord)

class CandidatesPagingSource(val fcitx: FcitxConnection, val total: Int, val offset: Int) :
    PagingSource<Int, IndexedCandidateWord>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, IndexedCandidateWord> {
        // use candidate index for key, null means load from beginning (with offset)
        val startIndex = params.key ?: offset
        val pageSize = params.loadSize
        Timber.d("getCandidates(offset=$startIndex, limit=$pageSize)")
        val candidates = fcitx.runOnReady {
            getCandidates(startIndex, pageSize)
        }
        val prevKey = if (startIndex >= pageSize) startIndex - pageSize else null
        val nextKey = if (total > 0) {
            if (startIndex + pageSize + 1 >= total) null else startIndex + pageSize
        } else {
            if (candidates.size < pageSize) null else startIndex + pageSize
        }
        // Keep the engine index attached to the word. The adapter's position can move while
        // Paging refreshes or prepends data, so deriving the selection index from that position
        // can commit a neighbouring candidate instead of the one the user actually touched.
        return LoadResult.Page(
            candidates.mapIndexed { localIndex, candidate ->
                IndexedCandidateWord(startIndex + localIndex, candidate)
            },
            prevKey,
            nextKey
        )
    }

    // always reload from beginning
    override fun getRefreshKey(state: PagingState<Int, IndexedCandidateWord>) = null

}
