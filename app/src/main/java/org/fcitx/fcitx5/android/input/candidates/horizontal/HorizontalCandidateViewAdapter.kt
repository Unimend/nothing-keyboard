/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.candidates.horizontal

import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.core.CandidateWord
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.candidates.CandidateItemUi
import org.fcitx.fcitx5.android.input.candidates.CandidateViewHolder
import org.fcitx.fcitx5.android.input.candidates.model.CandidateEntry
import org.fcitx.fcitx5.android.input.candidates.model.withEngineIndices
import org.fcitx.fcitx5.android.input.design.NothingDesignTokens
import splitties.dimensions.dp
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent
import splitties.views.setPaddingDp

open class HorizontalCandidateViewAdapter(val theme: Theme) :
    RecyclerView.Adapter<CandidateViewHolder>() {

    init {
        setHasStableIds(true)
    }

    var entries: List<CandidateEntry> = emptyList()
        private set

    val candidates: Array<CandidateWord>
        get() = entries.map { it.word }.toTypedArray()

    var total = -1
        private set

    fun updateCandidates(data: Array<CandidateWord>, total: Int) {
        val old = entries
        val newEntries = data.withEngineIndices()
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size
            override fun getNewListSize() = newEntries.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return old[oldItemPosition].engineIndex == newEntries[newItemPosition].engineIndex
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return old[oldItemPosition] == newEntries[newItemPosition]
            }
        })
        entries = newEntries
        this.total = total
        diff.dispatchUpdatesTo(this)
    }

    fun appendCandidates(data: Array<CandidateWord>, total: Int) {
        if (data.isEmpty()) return
        val start = entries.size
        entries = entries + data.withEngineIndices(start)
        this.total = total
        notifyItemRangeInserted(start, data.size)
    }

    override fun getItemCount() = entries.size

    override fun getItemId(position: Int) = entries.getOrNull(position).hashCode().toLong()

    @CallSuper
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateViewHolder {
        val ui = CandidateItemUi(parent.context, theme)
        ui.root.apply {
            minimumWidth = dp(NothingDesignTokens.Candidate.MinimumItemWidthDp)
            setPaddingDp(
                NothingDesignTokens.Candidate.HorizontalPaddingDp,
                0,
                NothingDesignTokens.Candidate.HorizontalPaddingDp,
                0
            )
            layoutParams = RecyclerView.LayoutParams(wrapContent, matchParent)
        }
        return CandidateViewHolder(ui)
    }

    @CallSuper
    override fun onBindViewHolder(holder: CandidateViewHolder, position: Int) {
        val entry = entries[position]
        holder.update(entry.engineIndex, entry.word)
    }

    @CallSuper
    override fun onViewRecycled(holder: CandidateViewHolder) {
        holder.clear()
    }

}
