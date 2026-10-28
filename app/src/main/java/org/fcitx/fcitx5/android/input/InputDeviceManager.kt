/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024-2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.candidates.floating.FloatingCandidatesMode
import org.fcitx.fcitx5.android.utils.isTypeNull

class InputDeviceManager(
    private val systemWantsInputView: () -> Boolean,
    private val onChange: (Boolean) -> Unit
) {

    private var inputView: InputView? = null
    private var candidatesView: CandidatesView? = null

    private fun setupInputViewEvents(isVirtual: Boolean) {
        val iv = inputView ?: return
        iv.handleEvents = isVirtual
        iv.visibility = if (isVirtual) View.VISIBLE else View.GONE
    }

    private fun setupCandidatesViewEvents(isVirtual: Boolean) {
        val cv = candidatesView ?: return
        cv.handleEvents = !isVirtual
        // hide CandidatesView when entering virtual keyboard mode,
        // but preserve the visibility when entering physical keyboard mode (in case it's empty)
        if (isVirtual) {
            cv.visibility = View.GONE
        }
    }

    private fun setupViewEvents(isVirtual: Boolean) {
        setupInputViewEvents(isVirtual)
        setupCandidatesViewEvents(isVirtual)
    }

    var isVirtualKeyboard = true
        private set(value) {
            if (field == value) {
                return
            }
            field = value
            setupViewEvents(value)
            // fire change AFTER updating InputView(s),
            // make the view(s) ready for incoming events during `onChange`
            onChange(value)
        }

    fun setInputView(inputView: InputView) {
        this.inputView = inputView
        setupInputViewEvents(this.isVirtualKeyboard)
    }

    fun setCandidatesView(candidatesView: CandidatesView) {
        this.candidatesView = candidatesView
        setupCandidatesViewEvents(this.isVirtualKeyboard)
    }

    private var startedInputView = false
    private var isNullInputType = true

    private var candidatesViewMode by AppPrefs.getInstance().candidates.mode

    fun notifyOnStartInput(attribute: EditorInfo) {
        isNullInputType = attribute.isTypeNull()
    }

    /**
     * @return should use virtual keyboard
     */
    fun evaluateOnStartInputView(info: EditorInfo): Boolean {
        startedInputView = true
        isNullInputType = info.isTypeNull()
        isVirtualKeyboard = InputDeviceVisibilityPolicy.onStartInputView(
            candidatesViewMode, isVirtualKeyboard, systemWantsInputView()
        )
        return isVirtualKeyboard
    }

    /**
     * @return should force show input views on hardware key down
     */
    fun evaluateOnKeyDown(e: KeyEvent): Boolean {
        if (startedInputView) {
            // filter out back/home/volume buttons and combination keys
            if (e.unicodeChar != 0) {
                // evaluate virtual keyboard visibility when pressing physical keyboard while InputView visible
                evaluateOnKeyDownInner()
            }
            // no need to force show InputView since it's already visible
            return false
        } else {
            // force show InputView when focusing on text input (likely inputType is not TYPE_NULL)
            // and pressing any digit/letter/punctuation key on physical keyboard
            val showInputView = !isNullInputType && e.unicodeChar != 0
            if (showInputView) {
                evaluateOnKeyDownInner()
            }
            return showInputView
        }
    }

    private fun evaluateOnKeyDownInner() {
        isVirtualKeyboard = InputDeviceVisibilityPolicy.onHardwareKey(
            candidatesViewMode, systemWantsInputView()
        )
    }

    fun evaluateOnViewClicked() {
        if (!startedInputView) return
        isVirtualKeyboard = InputDeviceVisibilityPolicy.onEditorClicked(
            candidatesViewMode, systemWantsInputView()
        )
    }

    fun evaluateOnUpdateEditorToolType(toolType: Int) {
        if (!startedInputView) return
        isVirtualKeyboard = InputDeviceVisibilityPolicy.onEditorTool(
            candidatesViewMode,
            isVirtualKeyboard,
            systemWantsInputView(),
            isDirectTouch = toolType == MotionEvent.TOOL_TYPE_FINGER ||
                toolType == MotionEvent.TOOL_TYPE_STYLUS
        )
    }

    /**
     * Should be called when input method switched **by user**
     * @return should force show inputView for [CandidatesView] when input method switched by user
     */
    fun evaluateOnInputMethodSwitch(): Boolean {
        return !isVirtualKeyboard && !startedInputView
    }

    /**
     * Should be called whenever active input method changes,
     * eg. focus in/out, editor capability flag changes, user presses the shortcut, and so on...
     * @return should update status icon for this change
     */
    fun evaluateOnInputMethodActivate(): Boolean {
        return startedInputView && !isVirtualKeyboard
    }

    fun onFinishInputView() {
        startedInputView = false
    }
}

/** Pure visibility decisions; Android views and service callbacks stay in [InputDeviceManager]. */
object InputDeviceVisibilityPolicy {
    fun onStartInputView(
        mode: FloatingCandidatesMode,
        currentVirtual: Boolean,
        systemWantsInputView: Boolean
    ): Boolean = when (mode) {
        FloatingCandidatesMode.SystemDefault -> systemWantsInputView
        FloatingCandidatesMode.InputDevice -> currentVirtual
        FloatingCandidatesMode.Disabled -> true
    }

    fun onHardwareKey(
        mode: FloatingCandidatesMode,
        systemWantsInputView: Boolean
    ): Boolean = when (mode) {
        FloatingCandidatesMode.SystemDefault -> systemWantsInputView
        FloatingCandidatesMode.InputDevice -> false
        FloatingCandidatesMode.Disabled -> true
    }

    fun onEditorClicked(
        mode: FloatingCandidatesMode,
        systemWantsInputView: Boolean
    ): Boolean = when (mode) {
        FloatingCandidatesMode.SystemDefault -> systemWantsInputView
        FloatingCandidatesMode.InputDevice,
        FloatingCandidatesMode.Disabled -> true
    }

    fun onEditorTool(
        mode: FloatingCandidatesMode,
        currentVirtual: Boolean,
        systemWantsInputView: Boolean,
        isDirectTouch: Boolean
    ): Boolean = when (mode) {
        FloatingCandidatesMode.SystemDefault -> systemWantsInputView
        FloatingCandidatesMode.InputDevice -> if (isDirectTouch) true else currentVirtual
        FloatingCandidatesMode.Disabled -> true
    }
}
