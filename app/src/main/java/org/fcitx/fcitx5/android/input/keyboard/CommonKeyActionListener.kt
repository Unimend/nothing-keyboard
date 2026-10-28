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
import org.fcitx.fcitx5.android.input.dependency.inputView
import org.fcitx.fcitx5.android.input.dialog.AddMoreInputMethodsPrompt
import org.fcitx.fcitx5.android.input.dialog.InputMethodPickerController
import org.fcitx.fcitx5.android.input.editing.EditingCommand
import org.fcitx.fcitx5.android.input.engine.EngineCommand
import org.fcitx.fcitx5.android.input.candidates.interaction.selectCandidate
import org.fcitx.fcitx5.android.input.navigation.InputNavigationCommand
import org.fcitx.fcitx5.android.input.navigation.InputNavigator
import org.fcitx.fcitx5.android.input.platform.ImeDialogController
import org.fcitx.fcitx5.android.input.runtime.InputSessionAccess
import org.fcitx.fcitx5.android.input.runtime.InputEngineAccess
import org.fcitx.fcitx5.android.input.runtime.InputEditingAccess
import org.fcitx.fcitx5.android.input.runtime.ImePlatformAccess
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
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import org.mechdancer.dependency.manager.must

class CommonKeyActionListener :
    UniqueComponent<CommonKeyActionListener>(), Dependent, ManagedHandler by managedHandler() {

    private val context by manager.context()
    private val inputView by manager.inputView()
    private val session: InputSessionAccess by manager.must()
    private val engine: InputEngineAccess by manager.must()
    private val editing: InputEditingAccess by manager.must()
    private val platform: ImePlatformAccess by manager.must()

    private val kbdPrefs = AppPrefs.getInstance().keyboard

    private val spaceKeyLongPressBehavior by kbdPrefs.spaceKeyLongPressBehavior
    private val langSwitchKeyBehavior by kbdPrefs.langSwitchKeyBehavior

    private val editingCommands get() = editing.commands
    private val navigator: InputNavigator by manager.must()
    private val dialogs: ImeDialogController by manager.must()
    private val inputMethodPicker: InputMethodPickerController by manager.must()

    private fun hasPreedit(): Boolean = session.reader.current.engine.let {
        !it.clientPreedit.isEmpty || !it.inputPanel.preedit.isEmpty
    }

    private fun commitAndReset(): Job {
        val engineState = session.reader.current.engine
        if (engineState.inputMethod?.languageCode?.startsWith("zh") == true) {
            // Chinese: select 1st candidate, except prediction candidates
            if (hasPreedit()) {
                // preedit not empty, maybe there are candidates to select ...
                engine.commands.selectCandidate(0)
            }
        } else {
            // Other languages: commit preedit as-is
            editing.commands.execute(EditingCommand.FinishComposition)
        }
        return engine.commands.submit(EngineCommand.Reset)
    }

    private fun showInputMethodPicker() {
        inputView.surfaceScope.launch {
            dialogs.show(inputMethodPicker.build(context))
        }
    }

    val listener by lazy {
        KeyActionListener { action, _ ->
            when (action) {
                is TextKeyAction -> engine.commands.submit(
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
                        engine.commands.submit(
                            EngineCommand.SendKey(action.key, action.modifiers)
                        )
                    }
                }
                is CommitAction -> {
                    commitAndReset().invokeOnCompletion { failure ->
                        if (failure == null) {
                            inputView.surfaceScope.launch { editing.commands.execute(EditingCommand.CommitText(action.text)) }
                        }
                    }
                }
                is QuickPhraseAction -> {
                    commitAndReset()
                    engine.commands.submit(EngineCommand.TriggerQuickPhrase)
                }
                is UnicodeAction -> {
                    commitAndReset()
                    engine.commands.submit(EngineCommand.TriggerUnicode)
                }
                is LangSwitchAction -> {
                    when (langSwitchKeyBehavior) {
                        LangSwitchBehavior.Enumerate -> {
                            inputView.surfaceScope.launch {
                                if (engine.queries.enabledInputMethods().size < 2) {
                                    dialogs.show(AddMoreInputMethodsPrompt.build(context))
                                } else {
                                    engine.commands.submit(
                                        EngineCommand.EnumerateInputMethod()
                                    )
                                }
                            }
                        }
                        LangSwitchBehavior.ToggleActivate -> {
                            engine.commands.submit(EngineCommand.ToggleInputMethod)
                        }
                        LangSwitchBehavior.NextInputMethodApp -> {
                            platform.switchToNextInputMethod()
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
                        SpaceLongPressBehavior.Enumerate -> engine.commands.submit(
                            EngineCommand.EnumerateInputMethod()
                        )
                        SpaceLongPressBehavior.ToggleActivate ->
                            engine.commands.submit(EngineCommand.ToggleInputMethod)
                        SpaceLongPressBehavior.ShowPicker -> showInputMethodPicker()
                    }
                }
                else -> {}
            }
        }
    }
}
