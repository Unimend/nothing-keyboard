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
import splitties.dimensions.dp
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent
import splitties.views.setPaddingDp

open class HorizontalCandidateViewAdapter(val theme: Theme) :
    RecyclerView.Adapter<CandidateViewHolder>() {

    init {
        setHasStableIds(true)
    }

    var candidates: Array<CandidateWord> = arrayOf()
        private set

    var total = -1
        private set

    fun updateCandidates(data: Array<CandidateWord>, total: Int) {
        val old = candidates
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size
            override fun getNewListSize() = data.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return old[oldItemPosition].text == data[newItemPosition].text
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return old[oldItemPosition] == data[newItemPosition]
            }
        })
        this.candidates = data
        this.total = total
        diff.dispatchUpdatesTo(this)
    }

    fun appendCandidates(data: Array<CandidateWord>, total: Int) {
        if (data.isEmpty()) return
        val start = candidates.size
        candidates += data
        this.total = total
        notifyItemRangeInserted(start, data.size)
    }

    override fun getItemCount() = candidates.size

    override fun getItemId(position: Int) = candidates.getOrNull(position).hashCode().toLong()

    @CallSuper
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateViewHolder {
        val ui = CandidateItemUi(parent.context, theme)
        ui.root.apply {
            minimumWidth = dp(48)
            setPaddingDp(14, 0, 14, 0)
            layoutParams = RecyclerView.LayoutParams(wrapContent, matchParent)
        }
        return CandidateViewHolder(ui)
    }

    @CallSuper
    override fun onBindViewHolder(holder: CandidateViewHolder, position: Int) {
        holder.update(position, candidates[position])
    }

    @CallSuper
    override fun onViewRecycled(holder: CandidateViewHolder) {
        holder.clear()
    }

}
