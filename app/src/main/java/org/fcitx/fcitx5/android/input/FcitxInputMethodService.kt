/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
import android.view.inputmethod.InputMethodSubtype
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import org.fcitx.fcitx5.android.input.engine.FcitxCommandDispatcher
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.session.AndroidEditorCapabilities
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.daemon.FcitxDaemon
import org.fcitx.fcitx5.android.data.InputFeedbacks
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.editing.EditorController
import org.fcitx.fcitx5.android.input.debug.InputCoreDiagnostics
import org.fcitx.fcitx5.android.input.engine.FcitxEngineBridge
import org.fcitx.fcitx5.android.input.engine.AndroidKeyEventController
import org.fcitx.fcitx5.android.input.engine.EngineCommand
import org.fcitx.fcitx5.android.input.engine.EngineEffect
import org.fcitx.fcitx5.android.input.autofill.InlineSuggestionRequestFactory
import org.fcitx.fcitx5.android.input.wm.AndroidImeWindowLayout
import org.fcitx.fcitx5.android.input.wm.AndroidCandidateAnchorController
import org.fcitx.fcitx5.android.input.wm.AndroidInputSurfaceRebuildSignals
import org.fcitx.fcitx5.android.input.wm.InputSurfaceRebuildController
import org.fcitx.fcitx5.android.input.wm.InputSurfaceRebuildHost
import org.fcitx.fcitx5.android.input.platform.ImeDialogController
import org.fcitx.fcitx5.android.input.platform.InputDevicePresentationController
import org.fcitx.fcitx5.android.input.platform.InputDevicePresentationHost
import org.fcitx.fcitx5.android.input.dialog.InputMethodPickerController
import org.fcitx.fcitx5.android.input.engine.EngineCommandPort
import org.fcitx.fcitx5.android.input.engine.EngineSwitchReason
import org.fcitx.fcitx5.android.input.engine.FcitxEngineQueries
import org.fcitx.fcitx5.android.input.engine.InputEngineQueries
import org.fcitx.fcitx5.android.input.engine.InputEngineEffectHost
import org.fcitx.fcitx5.android.input.engine.InputEngineEffectRouter
import org.fcitx.fcitx5.android.input.session.InputSessionEvent
import org.fcitx.fcitx5.android.input.session.InputLifecycleController
import org.fcitx.fcitx5.android.input.session.InputLifecycleSessionSink
import org.fcitx.fcitx5.android.input.session.InputSessionStore
import org.fcitx.fcitx5.android.input.session.InputSessionReader
import org.fcitx.fcitx5.android.input.session.EditorCapability
import org.fcitx.fcitx5.android.input.session.EditorSnapshot
import org.fcitx.fcitx5.android.input.engine.AndroidSubtypeController
import org.fcitx.fcitx5.android.input.engine.PlatformInputMethodPort
import org.fcitx.fcitx5.android.input.session.toEditorSnapshot
import org.fcitx.fcitx5.android.input.session.editorSessionOrNull
import org.fcitx.fcitx5.android.utils.InputMethodUtil
import org.fcitx.fcitx5.android.utils.alpha
import org.fcitx.fcitx5.android.utils.forceShowSelf
import org.fcitx.fcitx5.android.utils.inputMethodManager
import org.fcitx.fcitx5.android.utils.isTypeNull
import org.fcitx.fcitx5.android.utils.monitorCursorAnchor
import org.fcitx.fcitx5.android.utils.styledColorOrDefault
import org.fcitx.fcitx5.android.utils.styledFloat
import splitties.dimensions.dp
import splitties.resources.styledColor
import timber.log.Timber

class FcitxInputMethodService : LifecycleInputMethodService() {

    private val sessionStore = InputSessionStore()
    val inputSession: InputSessionReader = sessionStore.reader

    private lateinit var fcitx: FcitxConnection
    private lateinit var engineBridge: FcitxEngineBridge
    lateinit var engineQueries: InputEngineQueries
        private set

    private lateinit var subtypeController: AndroidSubtypeController
    val platformInputMethods: PlatformInputMethodPort get() = subtypeController

    private lateinit var engineDispatcher: FcitxCommandDispatcher
    private val editingAwareEngineCommands: EngineCommandPort by lazy {
        org.fcitx.fcitx5.android.input.editing.EditingAwareEngineCommandPort(
            engineDispatcher,
            editor::invalidateDestructiveUndo
        )
    }
    val engineCommands: EngineCommandPort get() = editingAwareEngineCommands

    private lateinit var pkgNameCache: PackageNameCache

    private lateinit var decorView: View
    private lateinit var contentView: FrameLayout
    private var inputView: InputView? = null
    private var candidatesView: CandidatesView? = null
    val dialogs = ImeDialogController(
        windowToken = { decorView.windowToken },
        dimAmount = { styledFloat(android.R.attr.backgroundDimAmount) }
    )
    val inputMethodPicker = InputMethodPickerController(
        queries = { engineQueries },
        currentInputMethod = { inputSession.current.engine.inputMethod?.uniqueName },
        activateEngineInputMethod = {
            engineCommands.submit(EngineCommand.ActivateInputMethod(it))
        },
        activatePlatformInputMethod = { switchInputMethod(it) }
    )
    private val candidateAnchor = AndroidCandidateAnchorController(
        contentView = { contentView },
        decorView = { decorView },
        candidatesView = { candidatesView }
    )

    private val keyEvents: AndroidKeyEventController = AndroidKeyEventController(
        connection = { currentInputConnection },
        submitHardwareKey = ::submitHardwareKey,
        commitText = { editor.commitText(it) },
        invalidateDestructiveHistory = { editor.invalidateDestructiveUndo() }
    )

    private fun currentStatusIcon() = inputSession.current.engine.inputMethod?.let(
        StatusIconMapping::fromEntry
    ) ?: R.drawable.ic_baseline_keyboard_24

    private val navbarMgr = NavigationBarManager()
    private val devicePresentation = InputDevicePresentationController(
        object : InputDevicePresentationHost {
            override fun setCandidatePagingMode(mode: Int) {
                submitEngineCommand(EngineCommand.SetCandidatePagingMode(mode))
            }

            override fun monitorCursorAnchor(enabled: Boolean) {
                currentInputConnection?.monitorCursorAnchor(enabled)
            }

            override fun hideStatusIcon() = this@FcitxInputMethodService.hideStatusIcon()

            override fun showCurrentStatusIcon() = showStatusIcon(currentStatusIcon())

            override fun updateNavigationBar(isVirtualKeyboard: Boolean) {
                window.window?.let { navbarMgr.evaluate(it, isVirtualKeyboard) }
            }
        }
    )
    private val inputDeviceMgr = InputDeviceManager(
        ::systemWantsInputView,
        devicePresentation::applyMode
    )

    private val editor: EditorController = EditorController(
        connection = { currentInputConnection },
        editorInfo = { currentInputEditorInfo },
        sendKey = { keyEvents.sendDownUp(it) },
        submitEngineCommand = { command, guard -> submitEngineCommand(command, guard) },
        ignoreSystemCursor = { ignoreSystemCursor }
    )

    val editingCommands: org.fcitx.fcitx5.android.input.editing.EditingCommandPort by lazy {
        org.fcitx.fcitx5.android.input.editing.AndroidEditingCommandExecutor(
            editor, inputSession, { submitEngineCommand(EngineCommand.Reset) }
        ) { code, shift, ctrl ->
            keyEvents.sendCombination(code, shift = shift, ctrl = ctrl)
        }
    }

    private val inputLifecycle by lazy {
        InputLifecycleController(
            editor = editor,
            editingCommands = editingCommands,
            engineCommands = engineCommands,
            session = object : InputLifecycleSessionSink {
                override fun inputStarted(
                    editor: EditorSnapshot,
                    capabilities: Set<EditorCapability>,
                    restarting: Boolean
                ) = sessionStore.dispatch(InputSessionEvent.InputStarted(editor, capabilities, restarting))

                override fun inputViewStarted(
                    editor: EditorSnapshot,
                    restarting: Boolean
                ) = sessionStore.dispatch(InputSessionEvent.InputViewStarted(editor, restarting))

                override fun inputViewFinished() =
                    sessionStore.dispatch(InputSessionEvent.InputViewFinished)

                override fun inputFinished() = sessionStore.dispatch(InputSessionEvent.InputFinished)
            }
        )
    }

    private val engineEffectRouter by lazy {
        InputEngineEffectRouter(
            handleEditorEffect = editor::handleEngineEffect,
            host = object : InputEngineEffectHost {
                override fun forwardKey(effect: EngineEffect.ForwardKey) {
                    keyEvents.handleForwarded(effect)
                }

                override fun inputMethodChanged(effect: EngineEffect.InputMethodChanged) {
                    subtypeController.onEngineInputMethodChanged(effect.inputMethod.uniqueName)
                    if (inputDeviceMgr.evaluateOnInputMethodActivate()) {
                        showStatusIcon(StatusIconMapping.fromEntry(effect.inputMethod))
                    }
                }

                override fun inputMethodSwitched(effect: EngineEffect.InputMethodSwitched) {
                    if (effect.reason != EngineSwitchReason.CapabilityChanged &&
                        effect.reason != EngineSwitchReason.Other &&
                        inputDeviceMgr.evaluateOnInputMethodSwitch()
                    ) {
                        forceShowSelf()
                    }
                }
            }
        )
    }

    private val prefs = AppPrefs.getInstance()
    private val inlineSuggestions by prefs.keyboard.inlineSuggestions
    private val ignoreSystemCursor by prefs.advanced.ignoreSystemCursor

    private val recreateInputViewPrefs: Array<ManagedPreference<*>> = arrayOf(
        prefs.keyboard.expandKeypressArea,
        prefs.advanced.disableAnimation,
        prefs.advanced.ignoreSystemWindowInsets,
    )

    private fun replaceInputView(theme: Theme): InputView {
        val newInputView = InputView(this, theme,
            surfaceSink = { sessionStore.dispatch(InputSessionEvent.SurfaceChanged(it)) },
            punctuationSink = { sessionStore.dispatch(InputSessionEvent.PunctuationChanged(it.toMap())) })
        setInputView(newInputView)
        inputDeviceMgr.setInputView(newInputView)
        inputView = newInputView
        return newInputView
    }

    private fun replaceCandidateView(theme: Theme): CandidatesView {
        val newCandidatesView = CandidatesView(this, theme)
        // replace CandidatesView manually
        contentView.removeView(candidatesView)
        // put CandidatesView directly under content view
        contentView.addView(newCandidatesView)
        inputDeviceMgr.setCandidatesView(newCandidatesView)
        candidatesView = newCandidatesView
        return newCandidatesView
    }

    private fun replaceInputViews(theme: Theme) {
        navbarMgr.evaluate(window.window!!, inputDeviceMgr.isVirtualKeyboard)
        replaceInputView(theme)
        replaceCandidateView(theme)
    }

    private val surfaceRebuilds by lazy {
        InputSurfaceRebuildController(
            signals = AndroidInputSurfaceRebuildSignals(
                keyboardPreferences = recreateInputViewPrefs,
                candidatePreferences = prefs.candidates
            ),
            activeTheme = { ThemeManager.activeTheme },
            host = object : InputSurfaceRebuildHost {
                override fun replaceKeyboard(theme: Theme) { replaceInputView(theme) }
                override fun replaceCandidates(theme: Theme) { replaceCandidateView(theme) }
                override fun replaceAll(theme: Theme) { replaceInputViews(theme) }
            }
        )
    }

    private fun submitHardwareKey(event: KeyEvent, eventId: Int): Boolean =
        engineDispatcher.submitHardwareKey(event, eventId)

    private fun submitEngineCommand(command: EngineCommand): Job = engineCommands.submit(command)

    private fun submitEngineCommand(command: EngineCommand, guard: () -> Boolean): Job =
        engineDispatcher.submit(command, guard)

    override fun onCreate() {
        fcitx = FcitxDaemon.connect(javaClass.name)
        engineQueries = FcitxEngineQueries(fcitx)
        engineDispatcher = FcitxCommandDispatcher(fcitx, lifecycleScope)
        subtypeController = AndroidSubtypeController(engineQueries, engineCommands,
            currentSubtype = { inputMethodManager.currentInputMethodSubtype },
            switchSubtype = { switchInputMethod(InputMethodUtil.componentName, it) })
        engineBridge = FcitxEngineBridge(fcitx, inputSession,
            publish = { sessionStore.dispatch(InputSessionEvent.EngineChanged(it)) },
            effectSink = engineEffectRouter::handle)
        engineBridge.startIn(lifecycleScope)
        lifecycleScope.launch {
            inputSession.state.collect(InputCoreDiagnostics::publishSession)
        }
        pkgNameCache = PackageNameCache(this)
        surfaceRebuilds.start()
        lifecycleScope.launch { platformInputMethods.synchronize() }
        super.onCreate()
        window.window!!.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes = attributes.apply { format = PixelFormat.TRANSLUCENT }
            decorView.setBackgroundColor(Color.TRANSPARENT)
        }
        decorView = window.window!!.decorView
        contentView = decorView.findViewById(android.R.id.content)
        contentView.setBackgroundColor(Color.TRANSPARENT)
        lastKnownConfig = resources.configuration
    }

    private lateinit var lastKnownConfig: Configuration

    override fun onConfigurationChanged(newConfig: Configuration) {
        submitEngineCommand(EngineCommand.Reset)
        /**
         * skip keyboard|keyboardHidden changes, because we have [inputDeviceMgr]
         * skip uiMode (system light/dark mode) changes, because [surfaceRebuilds]
         * replaces the input surfaces when the active theme changes
         * [android.inputmethodservice.InputMethodService.onConfigurationChanged] would call
         * resetStateForNewConfiguration() which calls initViews() causes InputView(s) to be replaced again
         * https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r36/core/java/android/inputmethodservice/InputMethodService.java#1984
         */
        val f = ActivityInfo.CONFIG_KEYBOARD or
                ActivityInfo.CONFIG_KEYBOARD_HIDDEN or
                ActivityInfo.CONFIG_UI_MODE
        val diff = lastKnownConfig.diff(newConfig)
        Timber.d("onConfigurationChanged diff=$diff")
        /**
         * perform `super.onConfigurationChanged` only when `newConfig` diff fall outside "skipped" flags
         * we have to calculate the mask ourselves because nobody knows how `handledConfigChanges` works
         * https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r36/core/java/android/inputmethodservice/InputMethodService.java#1876
         */
        if (diff and f != diff) {
            super.onConfigurationChanged(newConfig)
        }
        lastKnownConfig = newConfig
    }

    override fun onWindowShown() {
        super.onWindowShown()
        editor.highlightColor =
            styledColorOrDefault(android.R.attr.colorAccent, DefaultHighlightColor).alpha(0.4f)
        InputFeedbacks.syncSystemPrefs()
    }

    override fun onCreateInputView(): View? {
        surfaceRebuilds.createInitialSurfaces()
        // We will call `setInputView` by ourselves. This is fine.
        return null
    }

    private val windowLayout = AndroidImeWindowLayout()

    override fun setInputView(view: View) {
        super.setInputView(view)
        windowLayout.expandInputArea(contentView, view)
    }

    override fun onConfigureWindow(win: Window, isFullscreen: Boolean, isCandidatesOnly: Boolean) {
        windowLayout.configure(win)
    }

    override fun onComputeInsets(outInsets: Insets) {
        windowLayout.computeInsets(outInsets, inputDeviceMgr.isVirtualKeyboard,
            inputView?.keyboardView, decorView)
    }

    // always show InputView since we delegate CandidatesView's visibility to it
    @SuppressLint("MissingSuperCall")
    override fun onEvaluateInputViewShown() = true

    private fun systemWantsInputView() = super.onEvaluateInputViewShown()

    override fun onEvaluateFullscreenMode() = false

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // request to show floating CandidatesView when pressing physical keyboard
        if (inputDeviceMgr.evaluateOnKeyDown(event)) {
            submitEngineCommand(EngineCommand.Focus(true))
            forceShowSelf()
        }
        return keyEvents.forwardToEngine(event) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return keyEvents.forwardToEngine(event) || super.onKeyUp(keyCode, event)
    }

    // Added in API level 14, deprecated in 29
    // it's needed because editors still use it even on API 36
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onViewClicked(focusChanged: Boolean) {
        super.onViewClicked(focusChanged)
        inputDeviceMgr.evaluateOnViewClicked()
    }

    @RequiresApi(34)
    override fun onUpdateEditorToolType(toolType: Int) {
        super.onUpdateEditorToolType(toolType)
        inputDeviceMgr.evaluateOnUpdateEditorToolType(toolType)
    }

    override fun onBindInput() {
        val uid = currentInputBinding.uid
        val pkgName = pkgNameCache.forUid(uid)
        Timber.d("onBindInput: uid=$uid pkg=$pkgName")
        submitEngineCommand(EngineCommand.ActivateContext(uid, pkgName))
        subtypeController.onBindInput()
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        subtypeController.onPlatformSubtypeChanged(newSubtype)
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        val capabilities = AndroidEditorCapabilities.fromAndroid(attribute.inputType, attribute.imeOptions)
        // EditorInfo may change between onStartInput and onStartInputView
        inputDeviceMgr.notifyOnStartInput(attribute)
        inputLifecycle.startInput(
            editorSnapshot = attribute.toEditorSnapshot(),
            capabilities = capabilities,
            initialSelectionStart = attribute.initialSelStart,
            initialSelectionEnd = attribute.initialSelEnd,
            restarting = restarting,
            acceptsEngineFocus = !attribute.isTypeNull()
        )
        Timber.d("onStartInput: initialSel=${editor.currentSelection}, restarting=$restarting")
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        Timber.d("onStartInputView: restarting=$restarting")
        inputLifecycle.startInputView(info.toEditorSnapshot(), restarting)
        if (inputDeviceMgr.evaluateOnStartInputView(info)) {
            // because onStartInputView will always be called after onStartInput,
            // editorInfo and capFlags should be up-to-date
            inputSession.current.editorSessionOrNull()?.let { inputView?.startInput(it) }
        } else {
            if (currentInputConnection?.monitorCursorAnchor() != true) {
                // anchor CandidatesView to bottom-left corner in case InputConnection does not
                // support monitoring CursorAnchorInfo
                candidateAnchor.showFallback()
            }
            showStatusIcon(currentStatusIcon())
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        // onUpdateSelection can left behind when user types quickly enough, eg. long press backspace
        sessionStore.dispatch(InputSessionEvent.SelectionChanged(newSelStart, newSelEnd))
        Timber.d("onUpdateSelection: old=[$oldSelStart,$oldSelEnd] new=[$newSelStart,$newSelEnd] cand=[$candidatesStart,$candidatesEnd]")
        editor.selectionChanged(
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd
        )
    }

    override fun onUpdateCursorAnchorInfo(info: CursorAnchorInfo) {
        candidateAnchor.update(info)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest? {
        if (!inlineSuggestions || !inputDeviceMgr.isVirtualKeyboard) return null
        return InlineSuggestionRequestFactory.create(this, ThemeManager.activeTheme)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
        if (!inlineSuggestions || !inputDeviceMgr.isVirtualKeyboard) return false
        return inputView?.handleInlineSuggestions(response) == true
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        Timber.d("onFinishInputView: finishingInput=$finishingInput")
        candidateAnchor.invalidateWindowGeometry()
        inputDeviceMgr.onFinishInputView()
        inputLifecycle.finishInputView()
        currentInputConnection?.monitorCursorAnchor(false)
        hideStatusIcon()
        dialogs.dismiss()
    }

    override fun onFinishInput() {
        Timber.d("onFinishInput")
        inputLifecycle.finishInput()
    }

    override fun onUnbindInput() {
        keyEvents.reset()
        inputLifecycle.unbindInput()
        // currentInputBinding can be null on some devices under some special Multi-screen mode
        val uid = currentInputBinding?.uid ?: return
        Timber.d("onUnbindInput: uid=$uid")
        submitEngineCommand(EngineCommand.DeactivateContext(uid))
    }

    override fun onDestroy() {
        surfaceRebuilds.stop()
        dialogs.dismiss()
        super.onDestroy()
        // Fcitx might be used in super.onDestroy()
        FcitxDaemon.disconnect(javaClass.name)
    }

    @Suppress("ConstPropertyName")
    companion object {
        const val DefaultHighlightColor = 0x008577  // material_deep_teal_500
        const val DeleteSurroundingFlag = "org.fcitx.fcitx5.android.DELETE_SURROUNDING"
    }
}
