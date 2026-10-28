/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.candidates.model

import org.fcitx.fcitx5.android.input.session.EngineCandidate

/**
 * Stable candidate identity shared by every candidate surface.
 *
 * [engineIndex] belongs to the Fcitx candidate generation and must never be reconstructed from a
 * RecyclerView/Paging position after this entry has been created.
 */
data class CandidateEntry(
    val engineIndex: Int,
    val word: EngineCandidate
)

fun List<EngineCandidate>.withEngineIndices(offset: Int = 0): List<CandidateEntry> =
    mapIndexed { localIndex, word -> CandidateEntry(offset + localIndex, word) }
