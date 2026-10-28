/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.keyboard

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.core.FcitxKeyMapping
import org.fcitx.fcitx5.android.daemon.launchOnReady
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.broadcast.PreeditEmptyStateComponent
import org.fcitx.fcitx5.android.input.candidates.horizontal.HorizontalCandidateComponent
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dialog.AddMoreInputMethodsPrompt
import org.fcitx.fcitx5.android.input.dialog.InputMethodPickerDialog
import org.fcitx.fcitx5.android.input.editing.AndroidEditingCommandExecutor
import org.fcitx.fcitx5.android.input.editing.EditingCommand
import org.fcitx.fcitx5.android.input.engine.EngineCommand
import org.fcitx.fcitx5.android.input.navigation.InputNavigationCommand
import org.fcitx.fcitx5.android.input.navigation.InputNavigator
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener.BackspaceSwipeState.Reset
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener.BackspaceSwipeState.Selection
import org.fcitx.fcitx5.android.input.keyboard.CommonKeyActionListener.BackspaceSwipeState.Stopped
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.CommitAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.ClearAllTextAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.DeleteSelectionAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.FcitxKeyAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.LangSwitchAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.MoveSelectionAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.PickerSwitchAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.QuickPhraseAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.ShowInputMethodPickerAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.ShowTextEditingAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.SpaceLongPressAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.SymAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.UnicodeAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.UndoAction
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.fcitx.fcitx5.android.utils.switchToNextIME
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must

class CommonKeyActionListener :
    UniqueComponent<CommonKeyActionListener>(), Dependent, ManagedHandler by managedHandler() {

    enum class BackspaceSwipeState {
        Stopped, Selection, Reset
    }

    private val context by manager.context()
    private val service by manager.inputMethodService()
    private val preeditState: PreeditEmptyStateComponent by manager.must()
    private val horizontalCandidate: HorizontalCandidateComponent by manager.must()
    private val windowManager: InputWindowManager by manager.must()

    private val kbdPrefs = AppPrefs.getInstance().keyboard

    private val spaceKeyLongPressBehavior by kbdPrefs.spaceKeyLongPressBehavior
    private val langSwitchKeyBehavior by kbdPrefs.langSwitchKeyBehavior

    private var backspaceSwipeState = Stopped
    private val editingCommands by lazy { AndroidEditingCommandExecutor(service) }
    private val navigator by lazy { InputNavigator(service, windowManager) }

    private fun hasPreedit(): Boolean = service.inputSession.current.engine.let {
        !it.clientPreedit.isEmpty || !it.inputPanel.preedit.isEmpty
    }

    private fun commitAndReset(): Job {
        val engine = service.inputSession.current.engine
        if (engine.inputMethod?.languageCode?.startsWith("zh") == true) {
            // Chinese: select 1st candidate, except prediction candidates
            if (hasPreedit()) {
                // preedit not empty, maybe there are candidates to select ...
                service.submitEngineCommand(EngineCommand.SelectCandidate(0))
            }
        } else {
            // Other languages: commit preedit as-is
            service.finishComposing()
        }
        return service.submitEngineCommand(EngineCommand.Reset)
    }

    private fun showInputMethodPicker() {
        service.lifecycleScope.launch {
            service.showDialog(InputMethodPickerDialog.build(service, context))
        }
    }

    val listener by lazy {
        KeyActionListener { action, _ ->
            when (action) {
                is FcitxKeyAction -> service.submitEngineCommand(
                    EngineCommand.SendKeyText(action.act, action.states.states, action.code)
                )
                is SymAction -> {
                    if (
                        action.sym.sym == FcitxKeyMapping.FcitxKey_Return &&
                        hasPreedit()
                    ) {
                        // Return completes active input instead of leaking through as an editor
                        // newline: Chinese selects candidate 0; other languages commit raw text.
                        commitAndReset()
                    } else {
                        service.submitEngineCommand(
                            EngineCommand.SendKey(action.sym.sym, action.states.states)
                        )
                    }
                }
                is CommitAction -> {
                    commitAndReset().invokeOnCompletion { failure ->
                        if (failure == null) {
                            service.lifecycleScope.launch { service.commitText(action.text) }
                        }
                    }
                }
                is QuickPhraseAction -> {
                    commitAndReset()
                    service.submitEngineCommand(EngineCommand.TriggerQuickPhrase)
                }
                is UnicodeAction -> {
                    commitAndReset()
                    service.submitEngineCommand(EngineCommand.TriggerUnicode)
                }
                is LangSwitchAction -> {
                    when (langSwitchKeyBehavior) {
                        LangSwitchBehavior.Enumerate -> {
                            service.lifecycleScope.launch {
                                if (service.engineQueries.enabledInputMethods().size < 2) {
                                    service.showDialog(AddMoreInputMethodsPrompt.build(context))
                                } else {
                                    service.submitEngineCommand(
                                        EngineCommand.EnumerateInputMethod()
                                    )
                                }
                            }
                        }
                        LangSwitchBehavior.ToggleActivate -> {
                            service.submitEngineCommand(EngineCommand.ToggleInputMethod)
                        }
                        LangSwitchBehavior.NextInputMethodApp -> {
                            service.switchToNextIME()
                        }
                    }
                }
                is ShowInputMethodPickerAction -> showInputMethodPicker()
                is MoveSelectionAction -> {
                    when (backspaceSwipeState) {
                        Stopped -> {
                            backspaceSwipeState = if (
                                preeditState.isEmpty &&
                                horizontalCandidate.adapter.total <= 0 // total is -1 on initialization
                            ) {
                                editingCommands.execute(EditingCommand.ExtendSelection(action.steps))
                                Selection
                            } else {
                                Reset
                            }
                        }
                        Selection -> {
                            editingCommands.execute(EditingCommand.ExtendSelection(action.steps))
                        }
                        Reset -> {}
                    }
                }
                is DeleteSelectionAction -> {
                    when (backspaceSwipeState) {
                        Stopped -> {}
                        Selection -> editingCommands.execute(EditingCommand.DeleteSelection)
                        Reset -> if (action.totalCnt != 0) {
                            service.submitEngineCommand(EngineCommand.Reset)
                        }
                    }
                    backspaceSwipeState = Stopped
                }
                ClearAllTextAction -> editingCommands.execute(EditingCommand.ClearAll)
                UndoAction -> editingCommands.execute(EditingCommand.Undo)
                ShowTextEditingAction -> navigator.navigate(InputNavigationCommand.ShowEditing)
                is PickerSwitchAction ->
                    navigator.navigate(InputNavigationCommand.ShowPicker(action.key))
                is SpaceLongPressAction -> {
                    when (spaceKeyLongPressBehavior) {
                        SpaceLongPressBehavior.None -> {}
                        SpaceLongPressBehavior.Enumerate -> service.submitEngineCommand(
                            EngineCommand.EnumerateInputMethod()
                        )
                        SpaceLongPressBehavior.ToggleActivate ->
                            service.submitEngineCommand(EngineCommand.ToggleInputMethod)
                        SpaceLongPressBehavior.ShowPicker -> showInputMethodPicker()
                    }
                }
                else -> {}
            }
        }
    }
}
