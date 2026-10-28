/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.transition.Slide
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.session.EngineInputMethod
import org.fcitx.fcitx5.android.input.session.EditorInputKind
import org.fcitx.fcitx5.android.input.session.EditorSessionSnapshot
import org.fcitx.fcitx5.android.input.session.EditorSessionChange
import org.fcitx.fcitx5.android.input.bar.KawaiiBarComponent
import org.fcitx.fcitx5.android.input.design.ReturnKeyGlyphPolicy
import org.fcitx.fcitx5.android.input.design.isNothingStyle
import org.fcitx.fcitx5.android.input.session.InputPresentationReducer
import org.fcitx.fcitx5.android.input.runtime.InputSessionAccess
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.picker.PickerWindow
import org.fcitx.fcitx5.android.input.navigation.InputNavigationCommand
import org.fcitx.fcitx5.android.input.navigation.InputNavigator
import org.fcitx.fcitx5.android.input.mode.KeyboardLayout
import org.fcitx.fcitx5.android.input.mode.InputMode
import org.fcitx.fcitx5.android.input.mode.InputModeStore
import org.fcitx.fcitx5.android.input.popup.PopupActionListener
import org.fcitx.fcitx5.android.input.popup.PopupComponent
import org.fcitx.fcitx5.android.input.session.InputSurface
import org.fcitx.fcitx5.android.input.wm.EssentialWindow
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.mechdancer.dependency.manager.must
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent

class KeyboardWindow : InputWindow.SimpleInputWindow<KeyboardWindow>(), EssentialWindow {

    override val surface = InputSurface.Keyboard

    private val inputSession: InputSessionAccess by manager.must()
    private val theme by manager.theme()
    private val commonKeyActionListener: CommonKeyActionListener by manager.must()
    private val windowManager: InputWindowManager by manager.must()
    private val navigator: InputNavigator by manager.must()
    private val modeStore: InputModeStore by manager.must()
    private val popup: PopupComponent by manager.must()
    private val bar: KawaiiBarComponent by manager.must()

    companion object : EssentialWindow.Key

    override val key: EssentialWindow.Key
        get() = KeyboardWindow

    override fun enterAnimation(lastWindow: InputWindow) = Slide().apply {
        slideEdge = Gravity.BOTTOM
    }.takeIf {
        // disable animation switching between picker
        lastWindow !is PickerWindow
    }

    override fun exitAnimation(nextWindow: InputWindow) =
        super.exitAnimation(nextWindow).takeIf {
            // disable animation switching between picker
            nextWindow !is PickerWindow
        }

    private lateinit var keyboardView: FrameLayout

    private val keyboards: Map<KeyboardLayout, BaseKeyboard> by lazy {
        hashMapOf(
            KeyboardLayout.Text to TextKeyboard(context, theme),
            KeyboardLayout.Number to NumberKeyboard(context, theme)
        )
    }
    private val currentLayout get() = modeStore.state.currentKeyboardLayout
    private val currentKeyboard: BaseKeyboard get() = keyboards.getValue(currentLayout)

    private val keyActionListener = KeyActionListener { it, source ->
        if (it is KeyAction.LayoutSwitchAction) {
            when (val target = it.target) {
                null -> switchLayout(null)
                is InputMode.Keyboard ->
                    navigator.navigate(InputNavigationCommand.ShowKeyboard(target.layout))
                is InputMode.Picker -> {
                    modeStore.activatePicker(target.kind, rememberAsSymbolMode = true)
                    navigator.navigate(InputNavigationCommand.ShowPicker(target.kind))
                }
            }
        } else {
            commonKeyActionListener.listener.onKeyAction(it, source)
        }
    }

    private val popupActionListener: PopupActionListener by lazy {
        popup.listener
    }

    // This will be called EXACTLY ONCE
    override fun onCreateView(): View {
        keyboardView = context.frameLayout(R.id.keyboard_view)
        attachLayout(KeyboardLayout.Text)
        return keyboardView
    }

    private fun detachCurrentLayout() {
        currentKeyboard.also {
            it.onDetach()
            keyboardView.removeView(it)
            it.keyActionListener = null
            it.popupActionListener = null
        }
    }

    private fun attachLayout(target: KeyboardLayout) {
        check(currentLayout == target)
        currentKeyboard.let {
            it.keyActionListener = keyActionListener
            it.popupActionListener = popupActionListener
            keyboardView.apply { add(it, lParams(matchParent, matchParent)) }
            it.onAttach()
            val snapshot = inputSession.reader.current
            it.onReturnDrawableUpdate(ReturnKeyGlyphPolicy.resolve(
                snapshot.editor,
                InputPresentationReducer.reduce(snapshot.engine).preeditEmpty,
                theme.isNothingStyle
            ))
            inputSession.reader.current.engine.inputMethod?.let { inputMethod ->
                it.onInputMethodUpdate(inputMethod)
            }
        }
    }

    fun switchLayout(to: KeyboardLayout?, remember: Boolean = true) {
        ContextCompat.getMainExecutor(context).execute {
            if (windowManager.isDisposed) return@execute
            val targetMode = to?.let(InputMode::Keyboard) ?: modeStore.state.rememberedSymbolMode
            if (targetMode is InputMode.Keyboard) {
                val target = targetMode.layout
                val unchanged = target == currentLayout
                if (!unchanged) {
                    detachCurrentLayout()
                }
                modeStore.activateKeyboard(
                    target,
                    rememberAsSymbolMode = remember && target != KeyboardLayout.Text
                )
                if (unchanged) return@execute
                attachLayout(target)
                if (windowManager.isAttached(this)) {
                    notifyBarLayoutChanged()
                }
            } else {
                check(targetMode is InputMode.Picker)
                modeStore.activatePicker(
                    targetMode.kind,
                    rememberAsSymbolMode = remember
                )
                navigator.navigate(InputNavigationCommand.ShowPicker(targetMode.kind))
            }
        }
    }

    fun onEditorSessionUpdate(session: EditorSessionSnapshot, change: EditorSessionChange) {
        if (change != EditorSessionChange.Started) return
        val targetLayout = when (session.editor.inputKind) {
            EditorInputKind.Number, EditorInputKind.Phone -> KeyboardLayout.Number
            else -> KeyboardLayout.Text
        }
        switchLayout(targetLayout, remember = false)
    }

    fun onImeUpdate(ime: EngineInputMethod) {
        currentKeyboard.onInputMethodUpdate(ime)
    }

    fun onPunctuationUpdate(mapping: Map<String, String>) {
        currentKeyboard.onPunctuationUpdate(mapping)
    }

    fun onReturnKeyDrawableUpdate(resourceId: Int) {
        currentKeyboard.onReturnDrawableUpdate(resourceId)
    }

    override fun onAttached() {
        currentKeyboard.let {
            it.keyActionListener = keyActionListener
            it.popupActionListener = popupActionListener
            it.onAttach()
        }
        notifyBarLayoutChanged()
    }

    override fun onDetached() {
        currentKeyboard.let {
            it.onDetach()
            it.keyActionListener = null
            it.popupActionListener = null
        }
        popup.dismissAll()
    }

    // Call this when
    // 1) the keyboard window was newly attached
    // 2) currently keyboard window is attached and switchLayout was used
    private fun notifyBarLayoutChanged() {
        bar.onKeyboardLayoutSwitched(currentLayout == KeyboardLayout.Number)
    }
}
