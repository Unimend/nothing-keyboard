/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2024 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.candidates.expanded

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.fcitx.fcitx5.android.input.engine.InputEngineQueries
import org.fcitx.fcitx5.android.input.candidates.model.CandidateEntry
import org.fcitx.fcitx5.android.input.candidates.model.withEngineIndices
import timber.log.Timber

class CandidatesPagingSource(val queries: InputEngineQueries, val total: Int, val offset: Int) :
    PagingSource<Int, CandidateEntry>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CandidateEntry> {
        // use candidate index for key, null means load from beginning (with offset)
        val startIndex = params.key ?: offset
        val pageSize = params.loadSize
        Timber.d("getCandidates(offset=$startIndex, limit=$pageSize)")
        val candidates = queries.candidates(startIndex, pageSize)
        // Expanded candidates begin after the horizontal rail. Never prepend into that rail.
        val prevKey = null
        val nextKey = nextCandidatePageKey(
            startIndex = startIndex,
            loadedCount = candidates.size,
            requestedCount = pageSize,
            total = total
        )
        // Keep the engine index attached to the word. The adapter's position can move while
        // Paging refreshes or prepends data, so deriving the selection index from that position
        // can commit a neighbouring candidate instead of the one the user actually touched.
        return LoadResult.Page(
            candidates.withEngineIndices(startIndex),
            prevKey,
            nextKey
        )
    }

    // always reload from beginning
    override fun getRefreshKey(state: PagingState<Int, CandidateEntry>) = null

}

internal fun nextCandidatePageKey(
    startIndex: Int,
    loadedCount: Int,
    requestedCount: Int,
    total: Int
): Int? {
    if (loadedCount <= 0 || loadedCount < requestedCount) return null
    val next = startIndex + loadedCount
    return if (total >= 0 && next >= total) null else next
}
