/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.candidates.expanded.window

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.View
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.input.candidates.CandidateViewHolder
import org.fcitx.fcitx5.android.input.candidates.expanded.CandidateTabActionsAdapter
import org.fcitx.fcitx5.android.input.candidates.expanded.CandidatesPagingSource
import org.fcitx.fcitx5.android.input.candidates.expanded.ExpandedCandidateLayout
import org.fcitx.fcitx5.android.input.candidates.expanded.PagingCandidateViewAdapter
import org.fcitx.fcitx5.android.input.candidates.interaction.CandidateInteractionController
import org.fcitx.fcitx5.android.input.dependency.inputView
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.KeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyActionListener
import org.fcitx.fcitx5.android.input.navigation.InputNavigationCommand
import org.fcitx.fcitx5.android.input.navigation.InputNavigator
import org.fcitx.fcitx5.android.input.session.InputSurface
import org.fcitx.fcitx5.android.input.design.toInputColorScheme
import org.fcitx.fcitx5.android.input.feedback.InputMotionSystem
import org.fcitx.fcitx5.android.input.session.EngineCandidateAction
import org.fcitx.fcitx5.android.input.session.EngineInputPanel
import org.fcitx.fcitx5.android.input.session.InputPresentationState
import org.fcitx.fcitx5.android.input.session.InputPresentationReducer
import org.fcitx.fcitx5.android.input.design.ReturnKeyGlyphPolicy
import org.fcitx.fcitx5.android.input.design.isNothingStyle
import org.fcitx.fcitx5.android.input.runtime.InputSessionAccess
import org.fcitx.fcitx5.android.input.runtime.InputEngineAccess
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.mechdancer.dependency.manager.must
import splitties.dimensions.dp
import splitties.views.recyclerview.verticalLayoutManager
import kotlin.math.max

abstract class BaseExpandedCandidateWindow<T : BaseExpandedCandidateWindow<T>> :
    InputWindow.SimpleInputWindow<T>() {

    override val surface = InputSurface.ExpandedCandidates

    protected val theme by manager.theme()
    protected val inputView by manager.inputView()
    private val inputSession: InputSessionAccess by manager.must()
    private val engine: InputEngineAccess by manager.must()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()
    private val candidateInteraction: CandidateInteractionController by manager.must()
    private val navigator: InputNavigator by manager.must()

    protected val motionEnabled get() = InputMotionSystem.enabled

    private lateinit var candidateLayout: ExpandedCandidateLayout

    protected val dividerDrawable by lazy {
        ShapeDrawable(RectShape()).apply {
            val intrinsicSize = max(1, context.dp(1))
            intrinsicWidth = intrinsicSize
            intrinsicHeight = intrinsicSize
            paint.color = theme.toInputColorScheme().divider
        }
    }

    abstract fun onCreateCandidateLayout(): ExpandedCandidateLayout

    final override fun onCreateView(): View {
        candidateLayout = onCreateCandidateLayout().apply {
            scrollableTabs.apply {
                adapter = tabsAdapter
                layoutManager = verticalLayoutManager()
            }
            pinnedTabs.apply {
                adapter = pinnedTabsAdapter
                layoutManager = verticalLayoutManager()
            }
        }
        return candidateLayout
    }

    private val keyActionListener = KeyActionListener { it, source ->
        if (it is KeyAction.CandidatePageAction) {
            when (it.direction) {
                KeyAction.CandidatePageAction.Direction.Previous -> prevPage()
                KeyAction.CandidatePageAction.Direction.Next -> nextPage()
            }
        } else {
            commonKeyActionListener.listener.onKeyAction(it, source)
        }
    }

    abstract val adapter: PagingCandidateViewAdapter
    abstract val layoutManager: RecyclerView.LayoutManager

    val tabsAdapter by lazy {
        object : CandidateTabActionsAdapter(theme, false) {
            override fun onTriggerTabAction(id: Int) {
                candidateInteraction.triggerTabAction(id)
            }
        }
    }

    val pinnedTabsAdapter by lazy {
        object : CandidateTabActionsAdapter(theme, true) {
            override fun onTriggerTabAction(id: Int) {
                candidateInteraction.triggerTabAction(id)
            }
        }
    }

    private fun updateTabs(newTabs: List<EngineCandidateAction>) {
        val tabs = newTabs.takeWhile { !it.separator }
        val pinnedTabs = newTabs.drop(tabs.size + 1).filter { !it.separator }
        if (tabs.isEmpty() && pinnedTabs.isEmpty()) {
            candidateLayout.tabsContainer.visibility = View.GONE
        } else {
            candidateLayout.tabsContainer.visibility = View.VISIBLE
        }
        tabsAdapter.updateTabs(tabs)
        pinnedTabsAdapter.updateTabs(pinnedTabs)
    }

    private var offsetJob: Job? = null

    private val candidatesPager by lazy {
        Pager(
            config = PagingConfig(
                pageSize = 48,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                CandidatesPagingSource(
                    engine.queries,
                    total = candidateInteraction.state.total,
                    offset = adapter.offset
                )
            }
        )
    }
    private var candidatesSubmitJob: Job? = null
    private var sessionJob: Job? = null

    abstract fun prevPage()

    abstract fun nextPage()

    override fun onAttached() {
        candidateLayout.embeddedKeyboard.also {
            val snapshot = inputSession.reader.current
            it.onReturnDrawableUpdate(ReturnKeyGlyphPolicy.resolve(
                snapshot.editor,
                InputPresentationReducer.reduce(snapshot.engine).preeditEmpty,
                theme.isNothingStyle
            ))
            it.keyActionListener = keyActionListener
        }
        updateTabs(inputSession.reader.current.engine.inputPanel.tabs)
        offsetJob = inputView.surfaceScope.launch {
            candidateInteraction.expandedOffset.collect {
                if (it <= 0) {
                    navigator.navigate(InputNavigationCommand.ShowKeyboard())
                } else {
                    candidateLayout.resetPosition()
                    adapter.refreshWithOffset(it)
                }
            }
        }
        candidatesSubmitJob = inputView.surfaceScope.launch {
            candidatesPager.flow.collectLatest {
                adapter.submitData(it)
            }
        }
        sessionJob = inputView.surfaceScope.launch {
            inputSession.reader.state.drop(1).collect { snapshot ->
                val presentation = InputPresentationReducer.reduce(snapshot.engine)
                if (presentation.preeditEmpty) {
                    navigator.navigate(InputNavigationCommand.ShowKeyboard())
                } else {
                    updateTabs(snapshot.engine.inputPanel.tabs)
                    candidateLayout.embeddedKeyboard.onReturnDrawableUpdate(
                        ReturnKeyGlyphPolicy.resolve(
                            snapshot.editor, presentation.preeditEmpty, theme.isNothingStyle
                        )
                    )
                }
            }
        }
    }

    fun bindCandidateUiViewHolder(holder: CandidateViewHolder) {
        holder.itemView.setOnClickListener {
            // Snapshot the immutable engine index at touch completion. Never infer it from the
            // RecyclerView position: paging may refresh between layout and click dispatch.
            candidateInteraction.select(holder.idx)
        }
        holder.itemView.setOnLongClickListener {
            inputView.showCandidateActionMenu(holder.idx, holder.candidate.text, holder.ui.root)
            true
        }
    }

    fun recycleCandidateViewHolder(holder: CandidateViewHolder) {
        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnLongClickListener(null)
    }

    override fun onDetached() {
        candidatesSubmitJob?.cancel()
        offsetJob?.cancel()
        sessionJob?.cancel()
        sessionJob = null
        candidateLayout.embeddedKeyboard.keyActionListener = null
    }

}
