/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Size
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
import android.view.inputmethod.InputMethodSubtype
import android.widget.FrameLayout
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.common.ImageViewStyle
import androidx.autofill.inline.common.TextViewStyle
import androidx.autofill.inline.common.ViewStyle
import androidx.autofill.inline.v1.InlineSuggestionUi
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import org.fcitx.fcitx5.android.input.engine.FcitxCommandDispatcher
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.CapabilityFlags
import org.fcitx.fcitx5.android.core.CapabilityFlag
import org.fcitx.fcitx5.android.core.SubtypeManager
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.daemon.FcitxDaemon
import org.fcitx.fcitx5.android.data.InputFeedbacks
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceProvider
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.editing.EditorController
import org.fcitx.fcitx5.android.input.debug.InputCoreDiagnostics
import org.fcitx.fcitx5.android.input.engine.FcitxEngineBridge
import org.fcitx.fcitx5.android.input.engine.AndroidKeyEventController
import org.fcitx.fcitx5.android.input.engine.EngineCommand
import org.fcitx.fcitx5.android.input.engine.EngineEffect
import org.fcitx.fcitx5.android.input.engine.EngineCommandPort
import org.fcitx.fcitx5.android.input.engine.EngineSwitchReason
import org.fcitx.fcitx5.android.input.engine.FcitxEngineQueries
import org.fcitx.fcitx5.android.input.engine.InputEngineQueries
import org.fcitx.fcitx5.android.input.session.InputSessionEvent
import org.fcitx.fcitx5.android.input.session.InputSessionStore
import org.fcitx.fcitx5.android.input.engine.FcitxPlatformAdapter
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

class FcitxInputMethodService : LifecycleInputMethodService(), EngineCommandPort {

    val inputSession = InputSessionStore()

    private lateinit var fcitx: FcitxConnection
    private lateinit var engineBridge: FcitxEngineBridge
    lateinit var engineQueries: InputEngineQueries
        private set

    private lateinit var engineCommands: FcitxCommandDispatcher

    private lateinit var pkgNameCache: PackageNameCache

    private lateinit var decorView: View
    private lateinit var contentView: FrameLayout
    private var inputView: InputView? = null
    private var candidatesView: CandidatesView? = null

    private val keyEvents: AndroidKeyEventController = AndroidKeyEventController(
        connection = { currentInputConnection },
        submitHardwareKey = ::submitHardwareKey,
        commitText = { editor.commitText(it) }
    )

    private fun currentStatusIcon() = inputSession.current.engine.inputMethod?.let(
        StatusIconMapping::fromEntry
    ) ?: R.drawable.ic_baseline_keyboard_24

    private val navbarMgr = NavigationBarManager()
    private val inputDeviceMgr = InputDeviceManager { isVirtualKeyboard ->
        submitEngineCommand(
            EngineCommand.SetCandidatePagingMode(if (isVirtualKeyboard) 0 else 1)
        )
        currentInputConnection?.monitorCursorAnchor(!isVirtualKeyboard)
        if (isVirtualKeyboard) {
            hideStatusIcon()
        } else {
            showStatusIcon(currentStatusIcon())
        }
        window.window?.let {
            navbarMgr.evaluate(it, isVirtualKeyboard)
        }
    }

    private val editor: EditorController = EditorController(
        connection = { currentInputConnection },
        editorInfo = { currentInputEditorInfo },
        sendKey = { keyEvents.sendDownUp(it) },
        submitEngineCommand = { command, guard -> submitEngineCommand(command, guard) },
        ignoreSystemCursor = { ignoreSystemCursor }
    )

    val editingCommands: org.fcitx.fcitx5.android.input.editing.EditingCommandPort by lazy {
        org.fcitx.fcitx5.android.input.editing.AndroidEditingCommandExecutor(editor) { code, shift, ctrl ->
            keyEvents.sendCombination(code, shift = shift, ctrl = ctrl)
        }
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
        val newInputView = InputView(this, theme)
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

    @Keep
    private val recreateInputViewListener = ManagedPreference.OnChangeListener<Any> { _, _ ->
        replaceInputView(ThemeManager.activeTheme)
    }

    @Keep
    private val recreateCandidatesViewListener = ManagedPreferenceProvider.OnChangeListener {
        replaceCandidateView(ThemeManager.activeTheme)
    }

    @Keep
    private val onThemeChangeListener = ThemeManager.OnThemeChangeListener {
        replaceInputViews(it)
    }

    private fun submitHardwareKey(event: KeyEvent, eventId: Int): Boolean =
        engineCommands.submitHardwareKey(event, eventId)

    override fun submit(command: EngineCommand): Job = engineCommands.submit(command)

    fun submitEngineCommand(command: EngineCommand): Job = submit(command)

    suspend fun syncPlatformInputMethodSubtypes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            SubtypeManager.syncWith(
                engineQueries.enabledInputMethods().map(FcitxPlatformAdapter::inputMethod)
                    .toTypedArray()
            )
        }
    }

    private fun submitEngineCommand(command: EngineCommand, guard: () -> Boolean): Job =
        engineCommands.submit(command, guard)

    override fun onCreate() {
        fcitx = FcitxDaemon.connect(javaClass.name)
        engineQueries = FcitxEngineQueries(fcitx)
        engineCommands = FcitxCommandDispatcher(fcitx, lifecycleScope)
        engineBridge = FcitxEngineBridge(fcitx, inputSession, ::handleEngineEffect)
        engineBridge.startIn(lifecycleScope)
        lifecycleScope.launch {
            inputSession.state.collect(InputCoreDiagnostics::publishSession)
        }
        pkgNameCache = PackageNameCache(this)
        recreateInputViewPrefs.forEach {
            it.registerOnChangeListener(recreateInputViewListener)
        }
        prefs.candidates.registerOnChangeListener(recreateCandidatesViewListener)
        ThemeManager.addOnChangedListener(onThemeChangeListener)
        lifecycleScope.launch { syncPlatformInputMethodSubtypes() }
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

    private fun handleEngineEffect(effect: EngineEffect) {
        if (editor.handleEngineEffect(effect)) return
        when (effect) {
            is EngineEffect.ForwardKey -> handleForwardedEngineKey(effect)
            is EngineEffect.InputMethodChanged -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val im = effect.inputMethod.uniqueName
                    val subtype = SubtypeManager.subtypeOf(im) ?: return
                    skipNextSubtypeChange = im
                    // [^1]: notify system that input method subtype has changed
                    switchInputMethod(InputMethodUtil.componentName, subtype)
                }
                if (inputDeviceMgr.evaluateOnInputMethodActivate()) {
                    showStatusIcon(StatusIconMapping.fromEntry(effect.inputMethod))
                }
            }
            is EngineEffect.InputMethodSwitched -> {
                if (effect.reason != EngineSwitchReason.CapabilityChanged &&
                    effect.reason != EngineSwitchReason.Other
                ) {
                    if (inputDeviceMgr.evaluateOnInputMethodSwitch()) {
                        // show inputView for [CandidatesView] when input method switched by user
                        forceShowSelf()
                    }
                }
            }
            else -> Unit // Editor effects were consumed above.
        }
    }

    private fun handleForwardedEngineKey(effect: EngineEffect.ForwardKey) =
        keyEvents.handleForwarded(effect)


    private lateinit var lastKnownConfig: Configuration

    override fun onConfigurationChanged(newConfig: Configuration) {
        submitEngineCommand(EngineCommand.Reset)
        /**
         * skip keyboard|keyboardHidden changes, because we have [inputDeviceMgr]
         * skip uiMode (system light/dark mode) changes, because we have [onThemeChangeListener]
         * to replace InputView(s) when needed
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
        replaceInputViews(ThemeManager.activeTheme)
        // We will call `setInputView` by ourselves. This is fine.
        return null
    }

    override fun setInputView(view: View) {
        super.setInputView(view)
        // input method layout has not changed in 11 years:
        // https://android.googlesource.com/platform/frameworks/base/+/ae3349e1c34f7aceddc526cd11d9ac44951e97b6/core/res/res/layout/input_method.xml
        // expand inputArea to fullscreen
        contentView.findViewById<FrameLayout>(android.R.id.inputArea)
            .updateLayoutParams<ViewGroup.LayoutParams> {
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        /**
         * expand InputView to fullscreen, since [android.inputmethodservice.InputMethodService.setInputView]
         * would set InputView's height to [ViewGroup.LayoutParams.WRAP_CONTENT]
         */
        view.updateLayoutParams<ViewGroup.LayoutParams> {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    override fun onConfigureWindow(win: Window, isFullscreen: Boolean, isCandidatesOnly: Boolean) {
        win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private var inputViewLocation = intArrayOf(0, 0)

    override fun onComputeInsets(outInsets: Insets) {
        if (inputDeviceMgr.isVirtualKeyboard) {
            inputView?.keyboardView?.getLocationInWindow(inputViewLocation)
            outInsets.apply {
                contentTopInsets = inputViewLocation[1]
                visibleTopInsets = inputViewLocation[1]
                touchableInsets = Insets.TOUCHABLE_INSETS_VISIBLE
            }
        } else {
            val n = decorView.findViewById<View>(android.R.id.navigationBarBackground)?.height ?: 0
            val h = decorView.height - n
            outInsets.apply {
                contentTopInsets = h
                visibleTopInsets = h
                touchableInsets = Insets.TOUCHABLE_INSETS_VISIBLE
            }
        }
    }

    // always show InputView since we delegate CandidatesView's visibility to it
    @SuppressLint("MissingSuperCall")
    override fun onEvaluateInputViewShown() = true

    fun superEvaluateInputViewShown() = super.onEvaluateInputViewShown()

    override fun onEvaluateFullscreenMode() = false

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // request to show floating CandidatesView when pressing physical keyboard
        if (inputDeviceMgr.evaluateOnKeyDown(event, this)) {
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
        inputDeviceMgr.evaluateOnViewClicked(this)
    }

    @RequiresApi(34)
    override fun onUpdateEditorToolType(toolType: Int) {
        super.onUpdateEditorToolType(toolType)
        inputDeviceMgr.evaluateOnUpdateEditorToolType(toolType, this)
    }

    private var firstBindInput = true

    override fun onBindInput() {
        val uid = currentInputBinding.uid
        val pkgName = pkgNameCache.forUid(uid)
        Timber.d("onBindInput: uid=$uid pkg=$pkgName")
        // ensure InputContext has been created before focusing it
        submitEngineCommand(EngineCommand.ActivateContext(uid, pkgName))
        if (firstBindInput) {
            firstBindInput = false
            // only use input method from subtype for the first `onBindInput`, because
            // 1. fcitx has `ShareInputState` option, thus reading input method from subtype
            //    everytime would ruin `ShareInputState=Program`
            // 2. im from subtype should be read once, when user changes input method from other
            //    app to a subtype of ours via system input method picker (on 34+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val subtype = inputMethodManager.currentInputMethodSubtype ?: return
                val im = SubtypeManager.inputMethodOf(subtype)
                submitEngineCommand(EngineCommand.ActivateInputMethod(im))
            }
        }
    }

    /**
     * When input method changes internally (eg. via language switch key or keyboard shortcut),
     * we want to notify system that subtype has changed (see [^1]), then ignore the incoming
     * [onCurrentInputMethodSubtypeChanged] callback.
     * Input method should only be changed when user changes subtype in system input method picker
     * manually.
     */
    private var skipNextSubtypeChange: String? = null

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val im = SubtypeManager.inputMethodOf(newSubtype)
            Timber.d("onCurrentInputMethodSubtypeChanged: im=$im")
            // don't change input method if this "subtype change" was our notify to system
            // see [^1]
            if (skipNextSubtypeChange == im) {
                skipNextSubtypeChange = null
                return
            }
            submitEngineCommand(EngineCommand.ActivateInputMethod(im))
        }
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        // update selection as soon as possible
        // sometimes when restarting input, onUpdateSelection happens before onStartInput, and
        // initialSel{Start,End} is outdated. but it's the client app's responsibility to send
        // right cursor position, try to workaround this would simply introduce more bugs.
        editor.startInput(attribute.initialSelStart, attribute.initialSelEnd)
        val flags = CapabilityFlags.fromEditorInfo(attribute)
        inputSession.dispatch(
            InputSessionEvent.InputStarted(
                attribute.toEditorSnapshot(), flags.flags,
                flags.has(CapabilityFlag.Password), restarting
            )
        )
        // EditorInfo may change between onStartInput and onStartInputView
        inputDeviceMgr.notifyOnStartInput(attribute)
        Timber.d("onStartInput: initialSel=${editor.currentSelection}, restarting=$restarting")
        val isNullType = attribute.isTypeNull()
        // Commands share one serial queue, preserving focus -> capability -> focus ordering.
        if (restarting) submitEngineCommand(EngineCommand.Focus(false))
        submitEngineCommand(EngineCommand.SetCapabilities(flags.flags))
        if (!isNullType) submitEngineCommand(EngineCommand.Focus(true))
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        Timber.d("onStartInputView: restarting=$restarting")
        inputSession.dispatch(
            InputSessionEvent.InputViewStarted(info.toEditorSnapshot(), restarting)
        )
        submitEngineCommand(EngineCommand.Focus(true))
        if (inputDeviceMgr.evaluateOnStartInputView(info, this)) {
            // because onStartInputView will always be called after onStartInput,
            // editorInfo and capFlags should be up-to-date
            inputSession.current.editorSessionOrNull()?.let { inputView?.startInput(it) }
        } else {
            if (currentInputConnection?.monitorCursorAnchor() != true) {
                if (!decorLocationUpdated) {
                    updateDecorLocation()
                }
                // anchor CandidatesView to bottom-left corner in case InputConnection does not
                // support monitoring CursorAnchorInfo
                candidatesView?.updateCursorAnchor(contentSize)
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
        inputSession.dispatch(InputSessionEvent.SelectionChanged(newSelStart, newSelEnd))
        Timber.d("onUpdateSelection: old=[$oldSelStart,$oldSelEnd] new=[$newSelStart,$newSelEnd] cand=[$candidatesStart,$candidatesEnd]")
        editor.selectionChanged(
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd
        )
    }

    private val contentSize = floatArrayOf(0f, 0f)
    private val decorLocation = floatArrayOf(0f, 0f)
    private val decorLocationInt = intArrayOf(0, 0)
    private var decorLocationUpdated = false

    private fun updateDecorLocation() {
        contentSize[0] = contentView.width.toFloat()
        contentSize[1] = contentView.height.toFloat()
        decorView.getLocationOnScreen(decorLocationInt)
        decorLocation[0] = decorLocationInt[0].toFloat()
        decorLocation[1] = decorLocationInt[1].toFloat()
        // contentSize and decorLocation can be completely wrong,
        // when measuring right after the very first onStartInputView() of an IMS' lifecycle
        if (contentSize[0] > 0 && contentSize[1] > 0) {
            decorLocationUpdated = true
        }
    }

    private val anchorPosition = floatArrayOf(0f, 0f, 0f, 0f)

    override fun onUpdateCursorAnchorInfo(info: CursorAnchorInfo) {
        val bounds = info.getCharacterBounds(0)
        if (bounds != null) {
            // anchor to start of composing span instead of insertion mark if available
            val horizontal =
                if (candidatesView?.layoutDirection == View.LAYOUT_DIRECTION_RTL) bounds.right else bounds.left
            anchorPosition[0] = horizontal
            anchorPosition[1] = bounds.bottom
            anchorPosition[2] = horizontal
            anchorPosition[3] = bounds.top
        } else {
            anchorPosition[0] = info.insertionMarkerHorizontal
            anchorPosition[1] = info.insertionMarkerBottom
            anchorPosition[2] = info.insertionMarkerHorizontal
            anchorPosition[3] = info.insertionMarkerTop
        }
        // avoid calling `decorView.getLocationOnScreen` repeatedly
        if (!decorLocationUpdated) {
            updateDecorLocation()
        }
        if (anchorPosition.any(Float::isNaN)) {
            // anchor candidates view to bottom-left corner in case CursorAnchorInfo is invalid
            candidatesView?.updateCursorAnchor(contentSize)
            return
        }
        // params of `Matrix.mapPoints` must be [x0, y0, x1, y1]
        info.matrix.mapPoints(anchorPosition)
        val (xOffset, yOffset) = decorLocation
        anchorPosition[0] -= xOffset
        anchorPosition[1] -= yOffset
        anchorPosition[2] -= xOffset
        anchorPosition[3] -= yOffset
        candidatesView?.updateCursorAnchor(anchorPosition, contentSize)
    }

    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest? {
        // ignore inline suggestion when disabled by user || using physical keyboard with floating candidates view
        if (!inlineSuggestions || !inputDeviceMgr.isVirtualKeyboard) return null
        val theme = ThemeManager.activeTheme
        val chipDrawable =
            if (theme.isDark) R.drawable.bkg_inline_suggestion_dark else R.drawable.bkg_inline_suggestion_light
        val chipBg = Icon.createWithResource(this, chipDrawable).setTint(theme.keyTextColor)
        val style = InlineSuggestionUi.newStyleBuilder()
            .setSingleIconChipStyle(
                ViewStyle.Builder()
                    .setBackgroundColor(Color.TRANSPARENT)
                    .setPadding(0, 0, 0, 0)
                    .build()
            )
            .setChipStyle(
                ViewStyle.Builder()
                    .setBackground(chipBg)
                    .setPadding(dp(10), 0, dp(10), 0)
                    .build()
            )
            .setTitleStyle(
                TextViewStyle.Builder()
                    .setLayoutMargin(dp(4), 0, dp(4), 0)
                    .setTextColor(theme.keyTextColor)
                    .setTextSize(14f)
                    .build()
            )
            .setSubtitleStyle(
                TextViewStyle.Builder()
                    .setTextColor(theme.altKeyTextColor)
                    .setTextSize(12f)
                    .build()
            )
            .setStartIconStyle(
                ImageViewStyle.Builder()
                    .setTintList(ColorStateList.valueOf(theme.altKeyTextColor))
                    .build()
            )
            .setEndIconStyle(
                ImageViewStyle.Builder()
                    .setTintList(ColorStateList.valueOf(theme.altKeyTextColor))
                    .build()
            )
            .build()
        val styleBundle = UiVersions.newStylesBuilder()
            .addStyle(style)
            .build()
        val spec = InlinePresentationSpec
            .Builder(Size(0, 0), Size(Int.MAX_VALUE, Int.MAX_VALUE))
            .setStyle(styleBundle)
            .build()
        return InlineSuggestionsRequest.Builder(listOf(spec))
            .setMaxSuggestionCount(InlineSuggestionsRequest.SUGGESTION_COUNT_UNLIMITED)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
        if (!inlineSuggestions || !inputDeviceMgr.isVirtualKeyboard) return false
        return inputView?.handleInlineSuggestions(response) == true
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        Timber.d("onFinishInputView: finishingInput=$finishingInput")
        inputSession.dispatch(InputSessionEvent.InputViewFinished)
        decorLocationUpdated = false
        inputDeviceMgr.onFinishInputView()
        editor.finishInputView()
        currentInputConnection?.monitorCursorAnchor(false)
        submitEngineCommand(EngineCommand.FocusOutIn)
        hideStatusIcon()
        showingDialog?.dismiss()
    }

    override fun onFinishInput() {
        Timber.d("onFinishInput")
        inputSession.dispatch(InputSessionEvent.InputFinished)
        submitEngineCommand(EngineCommand.Focus(false))
    }

    override fun onUnbindInput() {
        keyEvents.reset()
        editor.unbindInput()
        // currentInputBinding can be null on some devices under some special Multi-screen mode
        val uid = currentInputBinding?.uid ?: return
        Timber.d("onUnbindInput: uid=$uid")
        submitEngineCommand(EngineCommand.DeactivateContext(uid))
    }

    override fun onDestroy() {
        recreateInputViewPrefs.forEach {
            it.unregisterOnChangeListener(recreateInputViewListener)
        }
        prefs.candidates.unregisterOnChangeListener(recreateCandidatesViewListener)
        ThemeManager.removeOnChangedListener(onThemeChangeListener)
        super.onDestroy()
        // Fcitx might be used in super.onDestroy()
        FcitxDaemon.disconnect(javaClass.name)
    }

    private var showingDialog: Dialog? = null

    fun showDialog(dialog: Dialog) {
        showingDialog?.dismiss()
        dialog.window?.also {
            it.attributes.apply {
                token = decorView.windowToken
                type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
            }
            it.addFlags(
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            )
            it.setDimAmount(styledFloat(android.R.attr.backgroundDimAmount))
        }
        dialog.setOnDismissListener {
            showingDialog = null
        }
        dialog.show()
        showingDialog = dialog
    }

    @Suppress("ConstPropertyName")
    companion object {
        const val DefaultHighlightColor = 0x008577  // material_deep_teal_500
        const val DeleteSurroundingFlag = "org.fcitx.fcitx5.android.DELETE_SURROUNDING"
    }
}
