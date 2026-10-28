/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.clipboard

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.mechdancer.dependency.UniqueComponent

/** Presentation data, independent from Room entities and Android ClipData. */
data class ClipboardItem(
    val id: Int,
    val text: String,
    val pinned: Boolean,
    val timestamp: Long,
    val sensitive: Boolean
) {
    companion object { const val BULLET = "•" }
}

fun interface ClipboardUpdateListener { fun onUpdate(item: ClipboardItem) }
fun interface ClipboardObservation { fun close() }

interface ClipboardHistoryPort {
    val itemCount: Int
    val lastItem: ClipboardItem?
    fun pages(): Flow<PagingData<ClipboardItem>>
    fun observe(listener: ClipboardUpdateListener): ClipboardObservation
}

class ClipboardHistory(private val storage: ClipboardHistoryPort) :
    UniqueComponent<ClipboardHistory>(), ClipboardHistoryPort by storage
