/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.navigation

import org.fcitx.fcitx5.android.input.editing.EditingCommand

import androidx.core.content.ContextCompat
import org.fcitx.fcitx5.android.input.clipboard.ClipboardWindow
import org.fcitx.fcitx5.android.input.candidates.expanded.window.FlexboxExpandedCandidateWindow
import org.fcitx.fcitx5.android.input.candidates.expanded.window.GridExpandedCandidateWindow
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.editing.TextEditingWindow
import org.fcitx.fcitx5.android.input.debug.InputCoreDiagnostics
import org.fcitx.fcitx5.android.input.keyboard.KeyboardWindow
import org.fcitx.fcitx5.android.input.mode.KeyboardLayout
import org.fcitx.fcitx5.android.input.mode.InputModeStore
import org.fcitx.fcitx5.android.input.picker.PickerWindow
import org.fcitx.fcitx5.android.input.status.StatusAreaWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.fcitx.fcitx5.android.input.runtime.InputEditingAccess
import org.fcitx.fcitx5.android.input.session.InputSurface
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must

/** The only user-navigation layer allowed to know concrete input-window classes. */
class InputNavigator : UniqueComponent<InputNavigator>(), Dependent,
    ManagedHandler by managedHandler() {
    private val context by manager.context()
    private val editing: InputEditingAccess by manager.must()
    private val windowManager: InputWindowManager by manager.must()
    private val modeStore: InputModeStore by manager.must()
    private var clipboardReturnTarget: InputNavigationCommand = InputNavigationCommand.ShowKeyboard()

    fun navigate(command: InputNavigationCommand) {
        if (windowManager.isDisposed) return
        InputCoreDiagnostics.recordNavigation(command.diagnosticName)
        when (command) {
            is InputNavigationCommand.ShowKeyboard -> showKeyboard(command.layout)
            InputNavigationCommand.ShowClipboard -> {
                clipboardReturnTarget = currentReturnTarget()
                windowManager.attachWindow(ClipboardWindow())
            }
            InputNavigationCommand.Return -> navigate(returnTarget())
            InputNavigationCommand.ShowEditing -> {
                editing.commands.execute(EditingCommand.FinishComposition)
                windowManager.attachWindow(TextEditingWindow())
            }
            is InputNavigationCommand.ShowPicker -> {
                val kind = command.kind ?: modeStore.state.rememberedPicker
                modeStore.activatePicker(kind)
                windowManager.attachWindow(PickerWindow.Key.fromKind(kind))
            }
            InputNavigationCommand.ShowStatus ->
                windowManager.attachWindow(StatusAreaWindow())
            is InputNavigationCommand.ShowExpandedCandidates ->
                windowManager.attachWindow(
                    when (command.layout) {
                        InputNavigationCommand.CandidateLayout.Grid ->
                            GridExpandedCandidateWindow()
                        InputNavigationCommand.CandidateLayout.Flexbox ->
                            FlexboxExpandedCandidateWindow()
                    }
                )
        }
    }

    private fun showKeyboard(layout: KeyboardLayout?) {
        if (layout == null) {
            windowManager.attachWindow(KeyboardWindow)
            return
        }
        val keyboard = windowManager.getEssentialWindow(KeyboardWindow) as KeyboardWindow
        keyboard.switchLayout(layout)
        // switchLayout posts its layout mutation to the main executor. Queue attachment behind it.
        ContextCompat.getMainExecutor(context).execute {
            if (windowManager.isDisposed) return@execute
            if (!windowManager.isAttached(keyboard)) {
                windowManager.attachWindow(KeyboardWindow)
            }
        }
    }

    private val InputNavigationCommand.diagnosticName: String
        get() = when (this) {
            is InputNavigationCommand.ShowKeyboard ->
                layout?.let { "Keyboard($it)" } ?: "Keyboard"
            InputNavigationCommand.ShowClipboard -> "Clipboard"
            InputNavigationCommand.Return -> "Return"
            InputNavigationCommand.ShowEditing -> "Editing"
            is InputNavigationCommand.ShowPicker -> "Picker(${kind?.name ?: "remembered"})"
            InputNavigationCommand.ShowStatus -> "Status"
            is InputNavigationCommand.ShowExpandedCandidates ->
                "ExpandedCandidates(${layout.name})"
        }

    private fun currentReturnTarget(): InputNavigationCommand = ClipboardReturnPolicy.target(
        windowManager.currentSurface,
        modeStore.state.currentKeyboardLayout
    )

    private fun returnTarget(): InputNavigationCommand = when (windowManager.currentSurface) {
        InputSurface.Clipboard -> clipboardReturnTarget
        else -> InputNavigationCommand.ShowKeyboard(modeStore.state.currentKeyboardLayout)
    }
}

internal object ClipboardReturnPolicy {
    fun target(surface: InputSurface?, keyboardLayout: KeyboardLayout): InputNavigationCommand =
        when (surface) {
            InputSurface.Keyboard -> InputNavigationCommand.ShowKeyboard(keyboardLayout)
            InputSurface.Editing -> InputNavigationCommand.ShowEditing
            is InputSurface.Picker -> InputNavigationCommand.ShowPicker(
                org.fcitx.fcitx5.android.input.mode.PickerKind.fromStorage(surface.kind)
            )
            InputSurface.Status -> InputNavigationCommand.ShowStatus
            else -> InputNavigationCommand.ShowKeyboard(keyboardLayout)
        }
}
