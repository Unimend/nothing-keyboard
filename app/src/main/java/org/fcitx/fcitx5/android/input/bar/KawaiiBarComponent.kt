/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.bar

import org.fcitx.fcitx5.android.input.editing.EditingCommand

import android.graphics.Color
import android.os.Build
import android.util.Size
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InlineSuggestion
import android.view.inputmethod.InlineSuggestionsResponse
import android.view.inputmethod.InputMethodSubtype
import android.widget.FrameLayout
import android.widget.ViewAnimator
import android.widget.inline.InlineContentView
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.session.EditorSessionSnapshot
import org.fcitx.fcitx5.android.input.session.EditorSessionChange
import org.fcitx.fcitx5.android.input.session.InputPresentationState
import org.fcitx.fcitx5.android.input.clipboard.ClipboardItem
import org.fcitx.fcitx5.android.input.clipboard.ClipboardHistory
import org.fcitx.fcitx5.android.input.clipboard.ClipboardObservation
import org.fcitx.fcitx5.android.input.clipboard.ClipboardUpdateListener
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.State.ClickToAttachWindow
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.State.ClickToDetachWindow
import org.fcitx.fcitx5.android.input.bar.ExpandButtonStateMachine.State.Hidden
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.BooleanKey.ShowCandidates
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.TransitionEvent.ExtendedWindowAttached
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.TransitionEvent.PresentationUpdated
import org.fcitx.fcitx5.android.input.bar.KawaiiBarStateMachine.TransitionEvent.WindowDetached
import org.fcitx.fcitx5.android.input.bar.ui.CandidateUi
import org.fcitx.fcitx5.android.input.bar.ui.IdleUi
import org.fcitx.fcitx5.android.input.bar.ui.TitleUi
import org.fcitx.fcitx5.android.input.bar.ui.ToolButton
import org.fcitx.fcitx5.android.input.candidates.expanded.ExpandedCandidateStyle
import org.fcitx.fcitx5.android.input.candidates.horizontal.HorizontalCandidateComponent
import org.fcitx.fcitx5.android.input.dependency.UniqueViewComponent
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.design.NothingDesignTokens
import org.fcitx.fcitx5.android.input.design.isNothingStyle
import org.fcitx.fcitx5.android.input.editing.TextEditingWindow
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import org.fcitx.fcitx5.android.input.navigation.InputNavigationCommand
import org.fcitx.fcitx5.android.input.navigation.InputNavigator
import org.fcitx.fcitx5.android.input.popup.PopupComponent
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowLifecycleObserver
import org.fcitx.fcitx5.android.utils.AppUtil
import org.fcitx.fcitx5.android.utils.InputMethodUtil
import org.mechdancer.dependency.manager.must
import splitties.dimensions.dp
import splitties.views.backgroundColor
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

class KawaiiBarComponent : UniqueViewComponent<KawaiiBarComponent, FrameLayout>(),
    InputWindowLifecycleObserver {

    private val context by manager.context()
    private val theme by manager.theme()
    private val service by manager.inputMethodService()
    private val horizontalCandidate: HorizontalCandidateComponent by manager.must()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()
    private val popup: PopupComponent by manager.must()
    private val navigator: InputNavigator by manager.must()

    private val prefs = AppPrefs.getInstance()

    private val clipboardSuggestion = prefs.clipboard.clipboardSuggestion
    private val clipboardItemTimeout = prefs.clipboard.clipboardItemTimeout
    private val clipboardMaskSensitive by prefs.clipboard.clipboardMaskSensitive
    private val expandedCandidateStyle by prefs.keyboard.expandedCandidateStyle
    private val toolbarNumRowOnPassword by prefs.keyboard.toolbarNumRowOnPassword
    private val showVoiceInputButton by prefs.keyboard.showVoiceInputButton
    private val preferredVoiceInput by prefs.keyboard.preferredVoiceInput

    private var clipboardTimeoutJob: Job? = null
    private var inlineSuggestionsJob: Job? = null
    private var inputScopeReady = false
    private val clipboardHistory: ClipboardHistory by manager.must()
    private var clipboardObservation: ClipboardObservation? = null

    private var toolbarState = ToolbarState()

    @Keep
    private val onClipboardUpdateListener =
        ClipboardUpdateListener {
            if (!clipboardSuggestion.getValue()) return@ClipboardUpdateListener
            service.lifecycleScope.launch {
                if (!inputScopeReady) return@launch
                if (it.text.isEmpty()) {
                    updateToolbar(ToolbarEvent.ClipboardChanged(false))
                } else {
                    idleUi.clipboardUi.setContent(if (it.sensitive && clipboardMaskSensitive) {
                        ClipboardItem.BULLET.repeat(min(42, it.text.length))
                    } else {
                        it.text.take(42)
                    })
                    updateToolbar(ToolbarEvent.ClipboardChanged(true))
                    launchClipboardTimeoutJob()
                }
            }
        }

    @Keep
    private val onClipboardSuggestionUpdateListener =
        ManagedPreference.OnChangeListener<Boolean> { _, it ->
            if (!it) {
                updateToolbar(ToolbarEvent.ClipboardChanged(false))
                clipboardTimeoutJob?.cancel()
                clipboardTimeoutJob = null
            }
        }

    @Keep
    private val onClipboardTimeoutUpdateListener =
        ManagedPreference.OnChangeListener<Int> { _, _ ->
            when (idleUi.currentState) {
                IdleUi.State.Clipboard -> {
                    // renew timeout when clipboard suggestion is present
                    launchClipboardTimeoutJob()
                }
                else -> {}
            }
        }

    private fun launchClipboardTimeoutJob() {
        clipboardTimeoutJob?.cancel()
        val timeout = clipboardItemTimeout.getValue() * 1000L
        // never transition to ClipboardTimedOut state when timeout < 0
        if (timeout < 0L) return
        clipboardTimeoutJob = service.lifecycleScope.launch {
            delay(timeout)
            updateToolbar(ToolbarEvent.ClipboardChanged(false))
            clipboardTimeoutJob = null
        }
    }

    private fun updateToolbar(event: ToolbarEvent, fromUser: Boolean = false) {
        toolbarState = ToolbarReducer.reduce(toolbarState, event)
        val newState = when (toolbarState.content) {
            ToolbarContent.Empty -> IdleUi.State.Empty
            ToolbarContent.Toolbar -> IdleUi.State.Toolbar
            ToolbarContent.Clipboard -> IdleUi.State.Clipboard
            ToolbarContent.NumberRow -> IdleUi.State.NumberRow
            ToolbarContent.InlineSuggestion -> IdleUi.State.InlineSuggestion
        }
        if (newState == idleUi.currentState) return
        idleUi.updateState(newState, fromUser)
    }

    private val hideKeyboardCallback = View.OnClickListener {
        service.requestHideSelf(0)
    }

    private val swipeDownExpandCallback = CustomGestureView.OnGestureListener { _, e ->
        if (e.type == CustomGestureView.GestureType.Up && e.totalY > 0) {
            service.requestHideSelf(0)
            true
        } else false
    }

    // Combined gesture: determine primary direction by comparing totalX and totalY.
    // - If horizontal is dominant and left, show number row (when allowed).
    // - If vertical is dominant and down, hide keyboard.
    private val swipeHideKeyboardCallback = CustomGestureView.OnGestureListener { v, e ->
        require(v is ToolButton)
        val numberRowAvailable = toolbarState.passwordEditor && !toolbarState.keyboardLayoutNumber
        if (numberRowAvailable) {
            val dir = if (context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR) 1 else -1
            // `e.x` and `e.y` are relative to the view's top-left corner
            val centerX = e.x - v.width / 2f
            val centerY = e.y - v.height / 2f

            val distance = hypot(centerX, centerY)
            // the button is ↓, so apply -90 degrees offset
            var angle = atan2(-centerX, centerY) * (180f / PI.toFloat())

            when (e.type) {
                CustomGestureView.GestureType.Move -> {
                    angle = if (angle in -45f..45f) {
                        angle.coerceIn(-10f, 10f)
                    } else abs(angle).coerceIn(90f - 10f, 90f + 10f) * dir
                    v.iconRotation = angle
                }
                CustomGestureView.GestureType.Up -> {
                    val handled = when (angle) {
                        in -45f..45f if distance > v.swipeThresholdX -> {
                            service.requestHideSelf(0)
                            true
                        }
                        !in -45f..45f if distance > v.swipeThresholdY -> {
                            v.iconRotation = 90f * dir
                            updateToolbar(ToolbarEvent.NumberRowRequested, fromUser = true)
                            true
                        }
                        else -> false
                    }
                    v.iconRotation = 0f
                    return@OnGestureListener handled
                }
                else -> {}
            }
        }

        if (e.type == CustomGestureView.GestureType.Up && abs(e.totalY) > abs(e.totalX) && e.totalY > 0) {
            service.requestHideSelf(0)
            true
        } else false
    }

    private var voiceInputSubtype: Pair<String, InputMethodSubtype>? = null

    private val switchToVoiceInputCallback = View.OnClickListener {
        val (id, subtype) = voiceInputSubtype ?: return@OnClickListener
        InputMethodUtil.switchInputMethod(service, id, subtype)
    }

    private val idleUi: IdleUi by lazy {
        IdleUi(context, theme, popup, commonKeyActionListener).apply {
            menuButton.setOnClickListener {
                updateToolbar(ToolbarEvent.MenuPressed, fromUser = true)
                // reset timeout timer (if present) when user switch layout
                if (clipboardTimeoutJob != null) {
                    launchClipboardTimeoutJob()
                }
            }
            hideKeyboardButton.apply {
                setOnClickListener(hideKeyboardCallback)
                swipeEnabled = true
                swipeThresholdY = dp(HEIGHT.toFloat())
                swipeThresholdX = swipeThresholdY
                onGestureListener = swipeHideKeyboardCallback
            }
            buttonsUi.apply {
                undoButton.setOnClickListener {
                    service.editingCommands.execute(EditingCommand.ApplicationUndo)
                }
                redoButton.setOnClickListener {
                    service.editingCommands.execute(EditingCommand.ApplicationRedo)
                }
                cursorMoveButton.setOnClickListener {
                    navigator.navigate(InputNavigationCommand.ShowEditing)
                }
                clipboardButton.setOnClickListener {
                    navigator.navigate(InputNavigationCommand.ShowClipboard)
                }
                moreButton.setOnClickListener {
                    navigator.navigate(InputNavigationCommand.ShowStatus)
                }
            }
            clipboardUi.suggestionView.apply {
                setOnClickListener {
                    clipboardHistory.lastItem?.let {
                        service.editingCommands.execute(EditingCommand.CommitText(it.text))
                    }
                    clipboardTimeoutJob?.cancel()
                    clipboardTimeoutJob = null
                    updateToolbar(ToolbarEvent.ClipboardChanged(false))
                }
                setOnLongClickListener {
                    clipboardHistory.lastItem?.let {
                        AppUtil.launchClipboardEdit(context, it.id, true)
                    }
                    true
                }
            }
            numberRow.apply {
                onCollapseListener = {
                    updateToolbar(ToolbarEvent.NumberRowCollapsed, fromUser = true)
                }
            }
        }
    }

    private val candidateUi by lazy {
        CandidateUi(context, theme, horizontalCandidate.view).apply {
            expandButton.apply {
                swipeEnabled = true
                swipeThresholdY = dp(HEIGHT.toFloat())
                onGestureListener = swipeDownExpandCallback
            }
        }
    }

    private val titleUi by lazy {
        TitleUi(context, theme)
    }

    private val barStateMachine = KawaiiBarStateMachine.new {
        switchUiByState(it)
    }

    val expandButtonStateMachine = ExpandButtonStateMachine.new {
        when (it) {
            ClickToAttachWindow -> {
                setExpandButtonToAttach()
                setExpandButtonEnabled(true)
            }
            ClickToDetachWindow -> {
                setExpandButtonToDetach()
                setExpandButtonEnabled(true)
            }
            Hidden -> {
                setExpandButtonEnabled(false)
            }
        }
    }

    // set expand candidate button to create expand candidate
    private fun setExpandButtonToAttach() {
        candidateUi.expandButton.setOnClickListener {
            navigator.navigate(InputNavigationCommand.ShowExpandedCandidates(
                when (expandedCandidateStyle) {
                    ExpandedCandidateStyle.Grid -> InputNavigationCommand.CandidateLayout.Grid
                    ExpandedCandidateStyle.Flexbox -> InputNavigationCommand.CandidateLayout.Flexbox
                }
            ))
        }
        candidateUi.expandButton.setIcon(R.drawable.ic_baseline_expand_more_24)
        candidateUi.expandButton.contentDescription = context.getString(R.string.expand_candidates_list)
    }

    // set expand candidate button to close expand candidate
    private fun setExpandButtonToDetach() {
        candidateUi.expandButton.setOnClickListener {
            navigator.navigate(InputNavigationCommand.ShowKeyboard())
        }
        candidateUi.expandButton.setIcon(R.drawable.ic_baseline_expand_less_24)
        candidateUi.expandButton.contentDescription = context.getString(R.string.hide_candidates_list)
    }

    // should be used with setExpandButtonToAttach or setExpandButtonToDetach
    private fun setExpandButtonEnabled(enabled: Boolean) {
        candidateUi.expandButton.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
    }

    private fun switchUiByState(state: KawaiiBarStateMachine.State) {
        val index = state.ordinal
        if (view.displayedChild == index) return
        val new = view.getChildAt(index)
        if (new != titleUi.root) {
            titleUi.setReturnButtonOnClickListener { }
            titleUi.setTitle("")
            titleUi.removeExtension()
        }
        view.displayedChild = index
    }

    override val view by lazy {
        ViewAnimator(context).apply {
            background = if (theme.isNothingStyle) {
                NothingDitherBarDrawable(resources.displayMetrics.density)
            } else null
            if (!theme.isNothingStyle) {
                backgroundColor =
                    if (ThemeManager.prefs.keyBorder.getValue()) Color.TRANSPARENT
                    else theme.barColor
            }
            add(idleUi.root, lParams(matchParent, matchParent))
            add(candidateUi.root, lParams(matchParent, matchParent))
            add(titleUi.root, lParams(matchParent, matchParent))
        }
    }

    fun onInputScopeReady() {
        if (inputScopeReady) return
        inputScopeReady = true
        clipboardHistory.lastItem?.let {
            val now = System.currentTimeMillis()
            val clipboardTimeout = clipboardItemTimeout.getValue() * 1000L
            if (now - it.timestamp < clipboardTimeout) {
                onClipboardUpdateListener.onUpdate(it)
            }
        }
        clipboardObservation = clipboardHistory.observe(onClipboardUpdateListener)
        clipboardSuggestion.registerOnChangeListener(onClipboardSuggestionUpdateListener)
        clipboardItemTimeout.registerOnChangeListener(onClipboardTimeoutUpdateListener)
    }

    fun onInputScopeDestroyed() {
        if (!inputScopeReady) return
        inputScopeReady = false
        cancelInlineSuggestions()
        clipboardTimeoutJob?.cancel()
        clipboardTimeoutJob = null
        clipboardObservation?.close()
        clipboardObservation = null
        clipboardSuggestion.unregisterOnChangeListener(onClipboardSuggestionUpdateListener)
        clipboardItemTimeout.unregisterOnChangeListener(onClipboardTimeoutUpdateListener)
    }

    fun onEditorSessionUpdate(session: EditorSessionSnapshot, change: EditorSessionChange) {
        if (change != EditorSessionChange.Started) return
        cancelInlineSuggestions()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            idleUi.privateMode(session.editor.privateMode)
        }
        updateToolbar(ToolbarEvent.SessionStarted(toolbarNumRowOnPassword && session.password))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            idleUi.inlineSuggestionsBar.clear()
        }
        voiceInputSubtype = InputMethodUtil.findVoiceSubtype(preferredVoiceInput)
        val shouldShowVoiceInput =
            showVoiceInputButton && voiceInputSubtype != null && !session.password
        idleUi.setHideKeyboardIsVoiceInput(
            shouldShowVoiceInput,
            if (shouldShowVoiceInput) switchToVoiceInputCallback else hideKeyboardCallback
        )
    }

    fun onInputPresentationUpdate(state: InputPresentationState) {
        barStateMachine.push(PresentationUpdated, ShowCandidates to state.showCandidates)
    }

    override fun onWindowAttached(window: InputWindow) {
        if (window is TextEditingWindow) {
            titleUi.showBlank()
            barStateMachine.push(ExtendedWindowAttached)
            return
        }
        when (window) {
            is InputWindow.ExtendedInputWindow<*> -> {
                titleUi.setTitle(window.title)
                window.onCreateBarExtension()?.let { titleUi.addExtension(it, window.showTitle) }
                titleUi.setReturnButtonOnClickListener {
                    navigator.navigate(InputNavigationCommand.ShowKeyboard())
                }
                barStateMachine.push(ExtendedWindowAttached)
            }
            else -> {}
        }
    }

    override fun onWindowDetached(window: InputWindow) {
        barStateMachine.push(WindowDetached)
    }

    private val suggestionSize by lazy {
        Size(ViewGroup.LayoutParams.WRAP_CONTENT, context.dp(HEIGHT))
    }

    private val directExecutor by lazy {
        Executor { it.run() }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun handleInlineSuggestions(response: InlineSuggestionsResponse): Boolean {
        if (!inputScopeReady) return false
        cancelInlineSuggestions()
        val suggestions = response.inlineSuggestions
        if (suggestions.isEmpty()) {
            updateToolbar(ToolbarEvent.InlineSuggestionChanged(false))
            idleUi.inlineSuggestionsBar.clear()
            return true
        }
        var pinned: InlineSuggestion? = null
        val scrollable = mutableListOf<InlineSuggestion>()
        var extraPinnedCount = 0
        suggestions.forEach {
            if (it.info.isPinned) {
                if (pinned == null) {
                    pinned = it
                } else {
                    scrollable.add(extraPinnedCount++, it)
                }
            } else {
                scrollable.add(it)
            }
        }
        inlineSuggestionsJob = service.lifecycleScope.launch {
            val content = coroutineScope {
                val pinnedView = async { pinned?.let { inflateInlineContentView(it) } }
                val views = scrollable.map { s -> async { inflateInlineContentView(s) } }
                pinnedView.await() to views.awaitAll()
            }
            ensureActive()
            if (!inputScopeReady) return@launch
            idleUi.inlineSuggestionsBar.setPinnedView(content.first)
            idleUi.inlineSuggestionsBar.setScrollableViews(content.second)
            updateToolbar(ToolbarEvent.InlineSuggestionChanged(true))
        }
        return true
    }

    private fun cancelInlineSuggestions() {
        inlineSuggestionsJob?.cancel()
        inlineSuggestionsJob = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            idleUi.inlineSuggestionsBar.clear()
        }
        updateToolbar(ToolbarEvent.InlineSuggestionChanged(false))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun inflateInlineContentView(suggestion: InlineSuggestion): InlineContentView? {
        return suspendCancellableCoroutine { c ->
            // callback view might be null
            suggestion.inflate(context, suggestionSize, directExecutor) { v ->
                c.resume(v)
            }
        }
    }

    companion object {
        const val HEIGHT = NothingDesignTokens.Bar.HeightDp
    }

    fun onKeyboardLayoutSwitched(isNumber: Boolean) {
        updateToolbar(ToolbarEvent.KeyboardLayoutChanged(isNumber))
    }

}
