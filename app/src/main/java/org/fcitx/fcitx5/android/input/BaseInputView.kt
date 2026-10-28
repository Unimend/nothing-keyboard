/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024-2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import android.view.View
import android.view.WindowInsets
import android.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.InputFeedbacks
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.data.theme.ThemePrefs
import org.fcitx.fcitx5.android.input.candidates.interaction.triggerCandidateAction
import org.fcitx.fcitx5.android.input.session.EngineState
import org.fcitx.fcitx5.android.utils.item
import org.fcitx.fcitx5.android.utils.navbarFrameHeight
import org.fcitx.fcitx5.android.utils.styledColorOrDefault
import splitties.views.dsl.core.withTheme
import kotlin.math.max

abstract class BaseInputView(
    val service: FcitxInputMethodService,
    val theme: Theme
) : ConstraintLayout(service) {

    protected abstract fun handleEngineState(state: EngineState)

    private var eventHandlerJob: Job? = null

    private fun setupEngineStateCollector() {
        eventHandlerJob = service.lifecycleScope.launch {
            service.inputSession.state.collect { handleEngineState(it.engine) }
        }
    }

    var handleEvents = false
        set(value) {
            field = value
            if (field) {
                if (eventHandlerJob == null) {
                    setupEngineStateCollector()
                }
            } else {
                eventHandlerJob?.cancel()
                eventHandlerJob = null
            }
        }

    private fun triggerCandidateAction(idx: Int, actionIdx: Int) {
        service.triggerCandidateAction(idx, actionIdx)
    }

    private var candidateActionMenu: PopupMenu? = null

    val themedContext = context.withTheme(R.style.Theme_InputViewTheme)

    fun showCandidateActionMenu(idx: Int, text: String, view: View) {
        candidateActionMenu?.dismiss()
        candidateActionMenu = null
        service.lifecycleScope.launch {
            val actions = service.engineQueries.candidateActions(idx)
            if (actions.isEmpty()) return@launch
            InputFeedbacks.hapticFeedback(view, longPress = true)
            candidateActionMenu = PopupMenu(themedContext, view).apply {
                menu.add(buildSpannedString {
                    bold {
                        color(
                            context.styledColorOrDefault(
                                android.R.attr.colorAccent,
                                theme.genericActiveForegroundColor
                            )
                        ) {
                            append(text)
                        }
                    }
                }).apply {
                    isEnabled = false
                }
                actions.forEach { action ->
                    menu.item(action.text) {
                        triggerCandidateAction(idx, action.id)
                    }
                }
                setOnDismissListener {
                    candidateActionMenu = null
                }
                show()
            }
        }
    }

    private val navbarBackground by ThemeManager.prefs.navbarBackground

    protected fun getNavBarBottomInset(windowInsets: WindowInsets): Int {
        if (navbarBackground != ThemePrefs.NavbarBackground.Full) {
            return 0
        }
        val insets = WindowInsetsCompat.toWindowInsetsCompat(windowInsets)
        // use navigation bar insets when available
        val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        // in case navigation bar insets goes wrong (eg. on LineageOS 21+ with gesture navigation)
        // use mandatory system gesture insets
        val mandatory = insets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures())
        var insetsBottom = max(navBars.bottom, mandatory.bottom)
        if (insetsBottom <= 0) {
            // check system gesture insets and fallback to navigation_bar_frame_height just in case
            val gesturesBottom = insets.getInsets(WindowInsetsCompat.Type.systemGestures()).bottom
            if (gesturesBottom > 0) {
                insetsBottom = max(gesturesBottom, context.navbarFrameHeight())
            }
        }
        return insetsBottom
    }

    private val ignoreSystemWindowInsets by AppPrefs.getInstance().advanced.ignoreSystemWindowInsets

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (ignoreSystemWindowInsets) {
            // suppress view's own onApplyWindowInsets
            setOnApplyWindowInsetsListener { _, insets -> insets }
        } else {
            // on API 35+, we must call requestApplyInsets() manually after replacing views,
            // otherwise View#onApplyWindowInsets won't be called. ¯\_(ツ)_/¯
            requestApplyInsets()
        }
    }

    override fun onDetachedFromWindow() {
        handleEvents = false
        super.onDetachedFromWindow()
    }
}
