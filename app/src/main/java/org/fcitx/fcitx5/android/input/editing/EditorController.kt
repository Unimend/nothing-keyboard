/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.editing

import android.os.Build
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import org.fcitx.fcitx5.android.input.cursor.CursorRange
import org.fcitx.fcitx5.android.input.design.renderEngineText
import org.fcitx.fcitx5.android.input.engine.EngineCommand
import org.fcitx.fcitx5.android.input.engine.EngineEffect
import org.fcitx.fcitx5.android.input.engine.EngineVirtualKey
import org.fcitx.fcitx5.android.input.session.EngineText
import org.fcitx.fcitx5.android.input.session.InputLifecycleEditor
import org.fcitx.fcitx5.android.utils.withBatchEdit
import splitties.bitflags.hasFlag
import timber.log.Timber

import android.view.inputmethod.InputConnection
import org.fcitx.fcitx5.android.input.cursor.CursorTracker

/** Android editor operations share this tracker with composition and destructive editing. */
class EditorController(
    private val connection: () -> InputConnection?,
    private val editorInfo: () -> EditorInfo,
    private val sendKey: (Int) -> Unit,
    private val submitEngineCommand: (EngineCommand, () -> Boolean) -> Unit,
    private val ignoreSystemCursor: () -> Boolean
) : InputLifecycleEditor {
    private fun submitEngineCommand(command: EngineCommand) = submitEngineCommand(command) { true }

    private val selection = CursorTracker()
    /** Returns false for engine effects owned by another subsystem. */
    fun handleEngineEffect(effect: EngineEffect): Boolean {
        when (effect) {
            is EngineEffect.CommitText -> commitText(effect.text, effect.cursor)
            is EngineEffect.UpdateClientPreedit -> updateComposingText(effect.text)
            is EngineEffect.DeleteSurrounding -> handleDeleteSurrounding(effect.before, effect.after)
            is EngineEffect.VirtualKey -> when (effect.key) {
                EngineVirtualKey.Backspace -> deleteBackward()
                EngineVirtualKey.Return -> handleReturnKey()
                EngineVirtualKey.ArrowLeft -> handleArrowKey(KeyEvent.KEYCODE_DPAD_LEFT)
                EngineVirtualKey.ArrowRight -> handleArrowKey(KeyEvent.KEYCODE_DPAD_RIGHT)
                EngineVirtualKey.Unicode -> commitText(Character.toString(effect.unicode))
                EngineVirtualKey.Unhandled -> Timber.w("Unhandled virtual engine key: ${effect.rawSymbol}")
            }
            else -> return false
        }
        return true
    }

    internal fun contextAction(id: Int) {
        connection()?.performContextMenuAction(id)
    }
    val currentSelection: CursorRange
        get() = CursorRange(selection.latest.start, selection.latest.end)

    override fun startInput(start: Int, end: Int) {
        cursorUpdateIndex += 1
        selection.resetTo(start, end)
        resetComposingState()
    }

    override fun selectionChanged(start: Int, end: Int, composingStart: Int, composingEnd: Int) {
        cursorUpdateIndex += 1
        handleCursorUpdate(start, end, composingStart, composingEnd, cursorUpdateIndex)
    }

    override fun unbindInput() {
        cursorUpdateIndex += 1
    }

    override fun finishInputView() {
        cursorUpdateIndex += 1
        connection()?.finishComposingText()
        resetComposingState()
    }

    fun cancelSelection() {
        val target = connection() ?: return
        val previous = selection.latest
        if (previous.isEmpty()) return
        val end = previous.end
        selection.predict(end)
        target.setSelection(end, end)
    }
    private val composing = CursorRange()
    private var composingText = EngineText()

    private val destructiveEditing = DestructiveEditingController(
        object : DestructiveEditingHost {
            override val editingConnection: InputConnection?
                get() = connection()
            override val editingSelection: CursorRange
                get() = selection.latest
            override val editingComposition: CursorRange
                get() = CursorRange(composing.start, composing.end)
            override val hasActiveComposition: Boolean
                get() = composing.isNotEmpty()
            override val shouldSimulateBackspaceKey: Boolean
                get() = editorInfo().privateImeOptions != "org.fcitx.fcitx5.android.DELETE_SURROUNDING" ||
                    editorInfo().inputType and InputType.TYPE_MASK_CLASS ==
                    InputType.TYPE_NULL

            override fun predictEditingSelection(start: Int, end: Int) {
                selection.predict(start, end)
            }

            override fun resetEditingSelection(start: Int, end: Int) {
                selection.resetTo(start, end)
            }

            override fun resetEditingComposition() = resetComposingState()

            override fun sendBackspaceKey() = sendKey(KeyEvent.KEYCODE_DEL)

            override fun resetInputEngine() {
                submitEngineCommand(EngineCommand.Reset)
            }
        }
    )

    private fun resetComposingState() {
        composing.clear()
        composingText = EngineText()
    }

    private var cursorUpdateIndex: Int = 0

    var highlightColor: Int = 0x008577

    fun handleDeleteSurrounding(before: Int, after: Int) {
        val ic = connection() ?: return
        destructiveEditing.invalidateUndo()
        if (before > 0) {
            selection.predictOffset(-before)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ic.deleteSurroundingTextInCodePoints(before, after)
        } else {
            ic.deleteSurroundingText(before, after)
        }
    }

    fun deleteBackward() {
        destructiveEditing.deleteBackward()
    }

    fun handleReturnKey() {
        editorInfo().run {
            if (inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_NULL ||
                imeOptions.hasFlag(EditorInfo.IME_FLAG_NO_ENTER_ACTION)
            ) {
                sendKey(KeyEvent.KEYCODE_ENTER)
                return
            }
            if (actionLabel?.isNotEmpty() == true && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                connection()?.performEditorAction(actionId)
                return
            }
            when (val action = imeOptions and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_UNSPECIFIED,
                EditorInfo.IME_ACTION_NONE -> sendKey(KeyEvent.KEYCODE_ENTER)
                else -> connection()?.performEditorAction(action)
            }
        }
    }

    fun handleArrowKey(keyCode: Int) {
        val type = editorInfo().inputType and InputType.TYPE_MASK_CLASS
        val variation = editorInfo().inputType and InputType.TYPE_MASK_VARIATION
        if (type == InputType.TYPE_NULL ||
            // confirm URL suggestion in browser location bar, see also https://bugzilla.mozilla.org/show_bug.cgi?id=1999915
            type == InputType.TYPE_CLASS_TEXT && variation == InputType.TYPE_TEXT_VARIATION_URI
        ) {
            sendKey(keyCode)
            return
        }
        val (start, end) = selection.latest
        val offset = if (start == end) 1 else 0
        val target = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> start - offset
            KeyEvent.KEYCODE_DPAD_RIGHT -> end + offset
            else -> return
        }
        connection()?.setSelection(target, target)
    }

    fun commitText(text: String, cursor: Int = -1) {
        val ic = connection() ?: return
        destructiveEditing.invalidateUndo()
        // when composing text equals commit content, finish composing text as-is
        if (composing.isNotEmpty() && composingText.plainText == text) {
            val c = if (cursor == -1) text.length else cursor
            val target = composing.start + c
            resetComposingState()
            ic.withBatchEdit {
                if (selection.current.start != target) {
                    selection.predict(target)
                    ic.setSelection(target, target)
                }
                ic.finishComposingText()
            }
            return
        }
        // committed text should replace composing (if any), replace selected range (if any),
        // or simply prepend before cursor
        val start = if (composing.isEmpty()) selection.latest.start else composing.start
        resetComposingState()
        if (cursor == -1) {
            selection.predict(start + text.length)
            ic.commitText(text, 1)
        } else {
            val target = start + cursor
            selection.predict(target)
            ic.withBatchEdit {
                commitText(text, 1)
                setSelection(target, target)
            }
        }
    }

    fun clearAllText() {
        destructiveEditing.clearAll()
    }

    fun undoLastEdit() {
        destructiveEditing.undo()
    }

    fun deleteSelection() {
        destructiveEditing.deleteSelection()
    }

    fun invalidateDestructiveUndo() {
        destructiveEditing.invalidateUndo()
    }

    private fun handleCursorUpdate(
        newSelStart: Int,
        newSelEnd: Int,
        newComposingStart: Int,
        newComposingEnd: Int,
        updateIndex: Int
    ) {
        if (selection.consume(newSelStart, newSelEnd)) {
            // try restore composing range in case it was dropped by InputFilter
            // but only when prediction matches, since InputFilter can also change editor content
            // ref:
            // https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r36/core/java/android/widget/Editor.java#2083
            // https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r36/core/java/android/widget/TextView.java#7351
            if (newComposingStart == -1 && newComposingEnd == -1 && composing.isNotEmpty()) {
                connection()?.setComposingRegion(composing.start, composing.end)
            }
            return // do nothing if prediction matches
        } else {
            // cursor update can't match any prediction: it's treated as a user input
            selection.resetTo(newSelStart, newSelEnd)
        }
        // skip selection range update, we only care about selection cursor (zero width) here
        if (newSelStart != newSelEnd) return
        // do reset if composing is empty && input panel is not empty
        if (composing.isEmpty()) {
            Timber.d("handleCursorUpdate: reset if engine is not empty")
            submitEngineCommand(EngineCommand.ResetIfNotEmpty)
            return
        }
        // check if cursor inside composing text
        if (composing.contains(newSelStart)) {
            if (ignoreSystemCursor()) return
            // fcitx cursor position is relative to client preedit (composing text)
            val position = newSelStart - composing.start
            // move fcitx cursor when cursor position changed
            if (position != composingText.cursor) {
                // cursor in InvokeActionEvent counts by "UTF-8 characters"
                val codePointPosition = composingText.plainText.codePointCount(0, position)
                submitEngineCommand(
                    EngineCommand.MoveCursor(codePointPosition),
                    { updateIndex == cursorUpdateIndex }
                )
            }
        } else {
            Timber.d("handleCursorUpdate: focus out/in")
            resetComposingState()
            // cursor outside composing range, finish composing as-is
            connection()?.finishComposingText()
            // `fcitx.reset()` here would commit preedit after new cursor position
            // since we have `ClientUnfocusCommit`, focus out and in would do the trick
            submitEngineCommand(EngineCommand.FocusOutIn)
        }
    }

    // because setComposingText(text, cursor) can only put cursor at end of composing,
    // sometimes onUpdateSelection would receive event with wrong cursor position.
    // those events need to be filtered.
    // because of https://android.googlesource.com/platform/frameworks/base.git/+/refs/tags/android-11.0.0_r45/core/java/android/view/inputmethod/BaseInputConnection.java#851
    // it's not possible to set cursor inside composing text
    fun updateComposingText(text: EngineText) {
        val ic = connection() ?: return
        val lastSelection = selection.latest
        ic.withBatchEdit {
            if ((composingText.segments == text.segments)) {
                // composing text content is up-to-date
                // update cursor only when it's not empty AND cursor position is valid
                if (text.plainText.length > 0 && text.cursor >= 0) {
                    val p = text.cursor + composing.start
                    if (p != lastSelection.start) {
                        Timber.d("updateComposingText: set Android selection ($p, $p)")
                        ic.setSelection(p, p)
                        selection.predict(p)
                    }
                }
            } else {
                // composing text content changed
                Timber.d("updateComposingText: '$text' lastSelection=$lastSelection")
                if (text.isEmpty) {
                    if (composing.isEmpty()) {
                        // do not reset saved selection range when incoming composing
                        // and saved composing range are both empty:
                        // composing.start is invalid when it's empty.
                        selection.predict(lastSelection.start)
                    } else {
                        // clear composing text, put cursor at start of original composing
                        selection.predict(composing.start)
                        composing.clear()
                    }
                    ic.setComposingText("", 1)
                } else {
                    val start = if (composing.isEmpty()) lastSelection.start else composing.start
                    composing.update(start, start + text.plainText.length)
                    // skip cursor reposition when:
                    // - preedit cursor is at the end
                    // - cursor position is invalid
                    if (text.cursor == text.plainText.length || text.cursor < 0) {
                        selection.predict(composing.end)
                        ic.setComposingText(renderEngineText(text, highlightColor), 1)
                    } else {
                        val p = text.cursor + composing.start
                        selection.predict(p)
                        ic.setComposingText(renderEngineText(text, highlightColor), 1)
                        ic.setSelection(p, p)
                    }
                }
                Timber.d("updateComposingText: composing=$composing")
            }
            composingText = text
        }
    }

    /**
     * Finish composing text and leave cursor position as-is.
     * Also clears this controller's composition state.
     */
    fun finishComposing() {
        val ic = connection() ?: return
        if (composing.isEmpty()) return
        composing.clear()
        composingText = EngineText()
        ic.finishComposingText()
    }

}
