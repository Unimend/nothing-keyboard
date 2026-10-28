/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.editing

import android.os.Build
import android.view.inputmethod.InputConnection
import android.view.inputmethod.ExtractedTextRequest
import org.fcitx.fcitx5.android.input.cursor.CursorRange
import org.fcitx.fcitx5.android.utils.withBatchEdit

/** Bridge kept deliberately small so destructive editing does not depend on the IME service. */
interface DestructiveEditingHost {
    val editingConnection: InputConnection?
    val editingSelection: CursorRange
    val hasActiveComposition: Boolean
    val shouldSimulateBackspaceKey: Boolean

    fun predictEditingSelection(start: Int, end: Int = start)
    fun resetEditingSelection(start: Int, end: Int = start)
    fun resetEditingComposition()
    fun sendBackspaceKey()
    fun resetInputEngine()
}

/**
 * Owns destructive edit capture, execution and one-level restoration.
 *
 * History intentionally survives input-field changes. A real mutation such as typing or paste
 * must call [invalidateUndo]; merely changing focus must not.
 */
class DestructiveEditingController(
    private val host: DestructiveEditingHost,
    private val history: DestructiveEditHistory = DestructiveEditHistory()
) {
    fun deleteBackward() {
        val connection = host.editingConnection ?: return
        val selection = host.editingSelection
        val start = selection.start
        val end = selection.end

        if (start != end) {
            // A queued DEL followed by setSelection can collapse the range before DEL arrives.
            // All selected-range deletions must use the same ordered editor transaction.
            deleteSelection()
            return
        } else if (start > 0) {
            val deletedLength = recordCharacterBeforeCursor(connection, start)
            host.predictEditingSelection(start - deletedLength)
        }

        if (host.shouldSimulateBackspaceKey) {
            host.sendBackspaceKey()
            return
        }
        if (start <= 0) {
            host.sendBackspaceKey()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connection.deleteSurroundingTextInCodePoints(1, 0)
        } else {
            connection.deleteSurroundingText(1, 0)
        }
    }

    fun deleteSelection() {
        val connection = host.editingConnection ?: return
        val selection = host.editingSelection
        if (selection.isEmpty()) return
        recordSelectedText(connection, selection.start, selection.end)
        host.predictEditingSelection(selection.start)
        connection.withBatchEdit {
            commitText("", 1)
            setSelection(selection.start, selection.start)
        }
        host.resetEditingSelection(selection.start)
    }

    fun clearAll() {
        val connection = host.editingConnection ?: return
        val before = connection.getExtractedText(ExtractedTextRequest(), 0)
            ?.text
            ?.toString()
            .orEmpty()
        val selection = host.editingSelection
        history.record(DeletedTextEdit(0, before, selection.end))
        host.resetEditingComposition()
        host.predictEditingSelection(0)
        connection.withBatchEdit {
            performContextMenuAction(android.R.id.selectAll)
            commitText("", 1)
            setSelection(0, 0)
        }
        host.resetEditingSelection(0)
        host.resetInputEngine()
    }

    fun undo() {
        val connection = host.editingConnection ?: return
        val edit = history.consume() ?: return
        host.resetEditingComposition()
        connection.withBatchEdit {
            setSelection(edit.start, edit.start)
            commitText(edit.deletedText, 1)
            setSelection(edit.cursorAfterRestore, edit.cursorAfterRestore)
        }
        host.resetEditingSelection(edit.cursorAfterRestore)
        host.resetInputEngine()
    }

    fun invalidateUndo() = history.clear()

    private fun recordSelectedText(connection: InputConnection, start: Int, end: Int) {
        history.record(
            DeletedTextEdit(
                start = start,
                deletedText = connection.getSelectedText(0)?.toString().orEmpty(),
                cursorAfterRestore = end
            )
        )
    }

    private fun recordCharacterBeforeCursor(connection: InputConnection, cursor: Int): Int {
        // Fcitx owns deletion inside composition; never restore raw composing pinyin as text.
        if (host.hasActiveComposition) return 1
        val textBeforeCursor = connection.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        if (textBeforeCursor.isEmpty()) return 1
        val deletedText = Character.toString(
            textBeforeCursor.codePointBefore(textBeforeCursor.length)
        )
        history.record(
            DeletedTextEdit(
                start = cursor - deletedText.length,
                deletedText = deletedText,
                cursorAfterRestore = cursor
            )
        )
        return deletedText.length
    }
}
