/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.candidates.expanded

import android.content.Context
import android.util.LruCache
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.candidates.CandidateViewHolder
import org.fcitx.fcitx5.android.input.design.NothingDesignTokens
import org.fcitx.fcitx5.android.input.design.NothingTextMeasurer
import org.fcitx.fcitx5.android.input.design.NothingTextStyle
import org.fcitx.fcitx5.android.input.candidates.model.textForDisplay
import splitties.dimensions.dp
import splitties.views.dsl.core.matchParent

abstract class GridPagingCandidateViewAdapter(context: Context, theme: Theme) : PagingCandidateViewAdapter(theme) {
    private val measurer = NothingTextMeasurer(context, theme, NothingTextStyle.Candidate)

    // cache measureWidth
    private val measuredWidths = object : LruCache<String, Float>(200) {
        override fun create(key: String): Float = measurer.widthInEm(key)
    }

    fun measureWidth(position: Int): Float {
        val candidate = getItem(position)?.word ?: return 0f
        return measuredWidths[candidate.textForDisplay()]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateViewHolder {
        return super.onCreateViewHolder(parent, viewType).apply {
            itemView.apply {
                layoutParams = GridLayoutManager.LayoutParams(
                    matchParent,
                    dp(NothingDesignTokens.Candidate.ExpandedRowHeightDp)
                )
            }
        }
    }
}
