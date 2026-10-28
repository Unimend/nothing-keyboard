/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.candidates.horizontal

import android.content.res.Configuration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.daemon.launchOnReady
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.BooleanKey.ExpandedCandidatesEmpty
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.TransitionEvent.ExpandedCandidatesUpdated
import org.fcitx.fcitx5.android.input.bar.KawaiiBarComponent
import org.fcitx.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fcitx.fcitx5.android.input.candidates.CandidateViewHolder
import org.fcitx.fcitx5.android.input.candidates.horizontal.HorizontalCandidateMode.AutoFillWidth
import org.fcitx.fcitx5.android.input.candidates.horizontal.HorizontalCandidateMode.NeverFillWidth
import org.fcitx.fcitx5.android.input.dependency.UniqueViewComponent
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputView
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.engine.EngineCommand
import org.fcitx.fcitx5.android.input.session.EngineCandidateList
import org.mechdancer.dependency.manager.must
import splitties.dimensions.dp

class HorizontalCandidateComponent :
    UniqueViewComponent<HorizontalCandidateComponent, RecyclerView>(), InputBroadcastReceiver {

    private val context by manager.context()
    private val theme by manager.theme()
    private val inputView by manager.inputView()
    private val bar: KawaiiBarComponent by manager.must()

    private val fillStyle by AppPrefs.getInstance().keyboard.horizontalCandidateStyle
    private val maxSpanCountPref by lazy {
        AppPrefs.getInstance().keyboard.run {
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                expandedCandidateGridSpanCount
            else
                expandedCandidateGridSpanCountLandscape
        }
    }

    private var layoutMinWidth = 0

    private var candidateGeneration = 0
    private var loadMoreInFlight = false
    private var reachedCandidateEnd = false

    private fun loadMoreCandidates() {
        if (loadMoreInFlight || reachedCandidateEnd) return
        val offset = adapter.itemCount
        val total = adapter.total
        if (total >= 0 && offset >= total) {
            reachedCandidateEnd = true
            return
        }
        val generation = candidateGeneration
        loadMoreInFlight = true
        inputView.service.lifecycleScope.launch {
            val next = inputView.service.engineQueries.candidates(offset, 32)
            view.post {
                if (generation != candidateGeneration) return@post
                loadMoreInFlight = false
                if (next.isEmpty()) {
                    reachedCandidateEnd = true
                    return@post
                }
                adapter.appendCandidates(next, total)
                reachedCandidateEnd = next.size < 32 ||
                    (total >= 0 && adapter.itemCount >= total)
            }
        }
    }

    // Since expanded candidate window is created once the expand button was clicked,
    // we need to replay the last offset
    private val _expandedCandidateOffset = MutableSharedFlow<Int>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val expandedCandidateOffset = _expandedCandidateOffset.asSharedFlow()

    private fun refreshExpanded(childCount: Int) {
        _expandedCandidateOffset.tryEmit(childCount)
        bar.expandButtonStateMachine.push(
            ExpandedCandidatesUpdated,
            ExpandedCandidatesEmpty to (adapter.total == childCount)
        )
    }

    val adapter: HorizontalCandidateViewAdapter by lazy {
        object : HorizontalCandidateViewAdapter(theme) {
            override fun onBindViewHolder(holder: CandidateViewHolder, position: Int) {
                super.onBindViewHolder(holder, position)
                holder.itemView.minimumWidth = layoutMinWidth
                holder.itemView.setOnClickListener {
                    val candidateIndex = holder.idx
                    if (candidateIndex >= 0) {
                        inputView.service.submitEngineCommand(
                            EngineCommand.SelectCandidate(candidateIndex)
                        )
                    }
                }
                holder.itemView.setOnLongClickListener {
                    inputView.showCandidateActionMenu(holder.idx, holder.candidate.text, holder.ui.root)
                    true
                }
            }

            override fun onViewRecycled(holder: CandidateViewHolder) {
                holder.itemView.setOnClickListener(null)
                holder.itemView.setOnLongClickListener(null)
                super.onViewRecycled(holder)
            }
        }
    }

    val layoutManager: LinearLayoutManager by lazy {
        object : LinearLayoutManager(context, HORIZONTAL, false) {
            override fun onLayoutCompleted(state: RecyclerView.State) {
                super.onLayoutCompleted(state)
                refreshExpanded(childCount)
            }
        }
    }

    override val view by lazy {
        object : RecyclerView(context) {
            override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                super.onSizeChanged(w, h, oldw, oldh)
                if (fillStyle == AutoFillWidth) {
                    val maxSpanCount = maxSpanCountPref.getValue()
                    layoutMinWidth = w / maxSpanCount
                }
            }
        }.apply {
            id = R.id.candidate_view
            itemAnimator = null
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            isHorizontalScrollBarEnabled = false
            adapter = this@HorizontalCandidateComponent.adapter
            layoutManager = this@HorizontalCandidateComponent.layoutManager
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dx <= 0) return
                    val lastVisible = this@HorizontalCandidateComponent.layoutManager
                        .findLastVisibleItemPosition()
                    if (lastVisible >= this@HorizontalCandidateComponent.adapter.itemCount - 4) {
                        loadMoreCandidates()
                    }
                }
            })
        }
    }

    override fun onCandidateUpdate(data: EngineCandidateList) {
        // A new composition starts a new search, so its candidate rail must start at the primary
        // candidate. Subsequent user drags remain untouched until the candidate data changes.
        val previousCandidates = adapter.candidates
        candidateGeneration++
        loadMoreInFlight = false
        reachedCandidateEnd = false
        val candidates = data.items
        val total = data.total
        layoutMinWidth = if (fillStyle == NeverFillWidth) 0
            else view.width / maxSpanCountPref.getValue()
        adapter.updateCandidates(candidates, total)
        if (previousCandidates.toList() != candidates) {
            view.scrollToPosition(0)
        }
        // An empty adapter may not trigger LayoutManager.onLayoutCompleted().
        if (candidates.isEmpty()) {
            refreshExpanded(0)
        }
    }
}
