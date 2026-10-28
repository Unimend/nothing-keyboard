/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.candidates.horizontal

import android.content.res.Configuration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import org.fcitx.fcitx5.android.input.candidates.interaction.CandidateLoadRequest
import org.fcitx.fcitx5.android.input.runtime.InputEngineAccess
import timber.log.Timber
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.candidates.CandidateViewHolder
import org.fcitx.fcitx5.android.input.candidates.horizontal.HorizontalCandidateMode.AutoFillWidth
import org.fcitx.fcitx5.android.input.candidates.horizontal.HorizontalCandidateMode.NeverFillWidth
import org.fcitx.fcitx5.android.input.candidates.interaction.CandidateInteractionController
import org.fcitx.fcitx5.android.input.dependency.UniqueViewComponent
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputView
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.session.EngineCandidateList
import org.mechdancer.dependency.manager.must
import splitties.dimensions.dp

class HorizontalCandidateComponent :
    UniqueViewComponent<HorizontalCandidateComponent, RecyclerView>() {

    private val context by manager.context()
    private val theme by manager.theme()
    private val inputView by manager.inputView()
    private val candidateInteraction: CandidateInteractionController by manager.must()
    private val engine: InputEngineAccess by manager.must()

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
    private var disposed = false
    private var loadMoreJob: Job? = null
    private var pendingDelivery: Runnable? = null
    private var pendingRequest: CandidateLoadRequest? = null

    private fun loadMoreCandidates() {
        if (disposed) return
        val request = candidateInteraction.beginLoadMore() ?: return
        pendingRequest = request
        val pageSize = 32
        loadMoreJob = inputView.surfaceScope.launch {
            try {
                val next = engine.queries.candidates(request.offset, pageSize)
                ensureActive()
                val delivery = Runnable {
                    pendingDelivery = null
                    if (disposed) return@Runnable
                    if (pendingRequest == request) pendingRequest = null
                    if (candidateInteraction.completeLoadMore(request, next.size, pageSize)) {
                        adapter.appendCandidates(next, candidateInteraction.state.total)
                        refreshExpanded()
                    }
                }
                pendingDelivery = delivery
                view.post(delivery)
            } catch (cancelled: CancellationException) {
                candidateInteraction.failLoadMore(request)
                throw cancelled
            } catch (failure: Exception) {
                candidateInteraction.failLoadMore(request)
                if (pendingRequest == request) pendingRequest = null
                Timber.w(failure, "Candidate page query failed")
            }
        }
    }

    private fun cancelPendingLoad() {
        loadMoreJob?.cancel()
        loadMoreJob = null
        pendingDelivery?.let(view::removeCallbacks)
        pendingDelivery = null
        pendingRequest?.let(candidateInteraction::failLoadMore)
        pendingRequest = null
    }

    fun dispose() {
        disposed = true
        cancelPendingLoad()
    }

    // Since expanded candidate window is created once the expand button was clicked,
    // we need to replay the last offset
    private fun refreshExpanded() {
        candidateInteraction.publishExpandedOffset()
    }

    val adapter: HorizontalCandidateViewAdapter by lazy {
        object : HorizontalCandidateViewAdapter(theme) {
            override fun onBindViewHolder(holder: CandidateViewHolder, position: Int) {
                super.onBindViewHolder(holder, position)
                holder.itemView.minimumWidth = layoutMinWidth
                holder.itemView.setOnClickListener {
                    candidateInteraction.select(holder.idx)
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
                refreshExpanded()
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

    fun onCandidateUpdate(data: EngineCandidateList) {
        if (disposed) return
        cancelPendingLoad()
        // A new composition starts a new search, so its candidate rail must start at the primary
        // candidate. Subsequent user drags remain untouched until the candidate data changes.
        val previousCandidates = adapter.candidates
        val candidates = data.items
        val total = data.total
        layoutMinWidth = if (fillStyle == NeverFillWidth) 0
            else view.width / maxSpanCountPref.getValue()
        adapter.updateCandidates(candidates, total)
        candidateInteraction.replaceCandidates(candidates.size, total)
        if (previousCandidates.toList() != candidates) {
            view.scrollToPosition(0)
        }
        // An empty adapter may not trigger LayoutManager.onLayoutCompleted().
        if (candidates.isEmpty()) {
            refreshExpanded()
        }
    }
}
