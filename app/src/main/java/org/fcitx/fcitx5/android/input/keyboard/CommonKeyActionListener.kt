/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.input.keyboard

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.input.engine.EngineKey
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.inputView
import org.fcitx.fcitx5.android.input.dialog.AddMoreInputMethodsPrompt
import org.fcitx.fcitx5.android.input.dialog.InputMethodPickerController
import org.fcitx.fcitx5.android.input.editing.EditingCommand
import org.fcitx.fcitx5.android.input.engine.EngineCommand
import org.fcitx.fcitx5.android.input.candidates.interaction.selectCandidate
import org.fcitx.fcitx5.android.input.navigation.InputNavigationCommand
import org.fcitx.fcitx5.android.input.navigation.InputNavigator
import org.fcitx.fcitx5.android.input.platform.ImeDialogController
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.CommitAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.ClearAllTextAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.DeleteSelectionAction
import org.fcitx.fcitx5.android.input.keyboard.KeyAction.TextKeyAction
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
import org.fcitx.fcitx5.android.utils.switchToNextIME
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must

class CommonKeyActionListener :
    UniqueComponent<CommonKeyActionListener>(), Dependent, ManagedHandler by managedHandler() {

    private val context by manager.context()
    private val service by manager.inputMethodService()
    private val inputView by manager.inputView()

    private val kbdPrefs = AppPrefs.getInstance().keyboard

    private val spaceKeyLongPressBehavior by kbdPrefs.spaceKeyLongPressBehavior
    private val langSwitchKeyBehavior by kbdPrefs.langSwitchKeyBehavior

    private val editingCommands get() = service.editingCommands
    private val navigator: InputNavigator by manager.must()
    private val dialogs: ImeDialogController by manager.must()
    private val inputMethodPicker: InputMethodPickerController by manager.must()

    private fun hasPreedit(): Boolean = service.inputSession.current.engine.let {
        !it.clientPreedit.isEmpty || !it.inputPanel.preedit.isEmpty
    }

    private fun commitAndReset(): Job {
        val engine = service.inputSession.current.engine
        if (engine.inputMethod?.languageCode?.startsWith("zh") == true) {
            // Chinese: select 1st candidate, except prediction candidates
            if (hasPreedit()) {
                // preedit not empty, maybe there are candidates to select ...
                service.engineCommands.selectCandidate(0)
            }
        } else {
            // Other languages: commit preedit as-is
            service.editingCommands.execute(EditingCommand.FinishComposition)
        }
        return service.engineCommands.submit(EngineCommand.Reset)
    }

    private fun showInputMethodPicker() {
        inputView.surfaceScope.launch {
            dialogs.show(inputMethodPicker.build(context))
        }
    }

    val listener by lazy {
        KeyActionListener { action, _ ->
            when (action) {
                is TextKeyAction -> service.engineCommands.submit(
                    EngineCommand.SendKeyText(action.act, action.modifiers)
                )
                is SymAction -> {
                    if (
                        action.key == EngineKey.Return &&
                        hasPreedit()
                    ) {
                        // Return completes active input instead of leaking through as an editor
                        // newline: Chinese selects candidate 0; other languages commit raw text.
                        commitAndReset()
                    } else {
                        service.engineCommands.submit(
                            EngineCommand.SendKey(action.key, action.modifiers)
                        )
                    }
                }
                is CommitAction -> {
                    commitAndReset().invokeOnCompletion { failure ->
                        if (failure == null) {
                            inputView.surfaceScope.launch { service.editingCommands.execute(EditingCommand.CommitText(action.text)) }
                        }
                    }
                }
                is QuickPhraseAction -> {
                    commitAndReset()
                    service.engineCommands.submit(EngineCommand.TriggerQuickPhrase)
                }
                is UnicodeAction -> {
                    commitAndReset()
                    service.engineCommands.submit(EngineCommand.TriggerUnicode)
                }
                is LangSwitchAction -> {
                    when (langSwitchKeyBehavior) {
                        LangSwitchBehavior.Enumerate -> {
                            inputView.surfaceScope.launch {
                                if (service.engineQueries.enabledInputMethods().size < 2) {
                                    dialogs.show(AddMoreInputMethodsPrompt.build(context))
                                } else {
                                    service.engineCommands.submit(
                                        EngineCommand.EnumerateInputMethod()
                                    )
                                }
                            }
                        }
                        LangSwitchBehavior.ToggleActivate -> {
                            service.engineCommands.submit(EngineCommand.ToggleInputMethod)
                        }
                        LangSwitchBehavior.NextInputMethodApp -> {
                            service.switchToNextIME()
                        }
                    }
                }
                is ShowInputMethodPickerAction -> showInputMethodPicker()
                is MoveSelectionAction ->
                    editingCommands.execute(EditingCommand.DeletionGestureMove(action.steps))
                is DeleteSelectionAction ->
                    editingCommands.execute(EditingCommand.DeletionGestureRelease(action.totalCnt))
                ClearAllTextAction -> editingCommands.execute(EditingCommand.ClearAll)
                UndoAction -> editingCommands.execute(EditingCommand.Undo)
                KeyAction.CancelSelectionAction ->
                    editingCommands.execute(EditingCommand.DeletionGestureCancel)
                ShowTextEditingAction -> navigator.navigate(InputNavigationCommand.ShowEditing)
                is PickerSwitchAction ->
                    navigator.navigate(InputNavigationCommand.ShowPicker(action.kind))
                is SpaceLongPressAction -> {
                    when (spaceKeyLongPressBehavior) {
                        SpaceLongPressBehavior.None -> {}
                        SpaceLongPressBehavior.Enumerate -> service.engineCommands.submit(
                            EngineCommand.EnumerateInputMethod()
                        )
                        SpaceLongPressBehavior.ToggleActivate ->
                            service.engineCommands.submit(EngineCommand.ToggleInputMethod)
                        SpaceLongPressBehavior.ShowPicker -> showInputMethodPicker()
                    }
                }
                else -> {}
            }
        }
    }
}
