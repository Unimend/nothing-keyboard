package org.fcitx.fcitx5.android.input.editing

import android.view.inputmethod.InputConnection
import android.view.inputmethod.ExtractedText
import java.lang.reflect.Proxy
import org.fcitx.fcitx5.android.input.cursor.CursorRange
import org.junit.Assert.*
import org.junit.Test

class DestructiveEditingControllerTest {
    /** A synchronous editor with deliberately separate predicted and actual selection. */
    private class Editor : DestructiveEditingHost {
        var text = "abc"
        var start = 0
        var end = 3
        var sentBackspace = 0
        var simulateBackspace = true
        var finishedComposition = 0
        override val editingSelection = CursorRange(0, 3)
        override val editingComposition = CursorRange()
        override val hasActiveComposition: Boolean
            get() = editingComposition.isNotEmpty()
        override val shouldSimulateBackspaceKey: Boolean
            get() = simulateBackspace
        override val editingConnection = Proxy.newProxyInstance(
            InputConnection::class.java.classLoader,
            arrayOf(InputConnection::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getSelectedText" -> text.substring(start, end)
                "getTextBeforeCursor" -> text.substring(0, start)
                "getExtractedText" -> ExtractedText().apply { this.text = this@Editor.text }
                "performContextMenuAction" -> {
                    if (args!![0] == android.R.id.selectAll) {
                        start = 0
                        end = text.length
                        true
                    } else {
                        false
                    }
                }
                "commitText" -> {
                    val replacement = args!![0].toString()
                    text = text.replaceRange(start, end, replacement)
                    start += replacement.length
                    end = start
                    true
                }
                "setSelection" -> { start = args!![0] as Int; end = args[1] as Int; true }
                "deleteSurroundingTextInCodePoints", "deleteSurroundingText" -> {
                    val before = args!![0] as Int
                    val after = args[1] as Int
                    val deleteStart = text.offsetByCodePoints(start, -before.coerceAtMost(text.codePointCount(0, start)))
                    val availableAfter = text.codePointCount(end, text.length)
                    val deleteEnd = text.offsetByCodePoints(end, after.coerceAtMost(availableAfter))
                    text = text.removeRange(deleteStart, deleteEnd)
                    start = deleteStart
                    end = start
                    true
                }
                "finishComposingText" -> { finishedComposition++; true }
                "beginBatchEdit", "endBatchEdit" -> true
                else -> error("Unexpected editor call: ${method.name}")
            }
        } as InputConnection
        override fun predictEditingSelection(start: Int, end: Int) = editingSelection.update(start, end)
        override fun resetEditingSelection(start: Int, end: Int) = editingSelection.update(start, end)
        override fun resetEditingComposition() = editingComposition.clear()
        override fun sendBackspaceKey() { sentBackspace++ }
        override fun resetInputEngine() = Unit
    }

    @Test
    fun `selected backspace never races a synthetic key with selection collapse`() {
        val editor = Editor()
        val controller = DestructiveEditingController(editor)
        controller.deleteBackward()
        assertEquals("", editor.text)
        assertEquals(0, editor.sentBackspace)
        assertEquals(0, editor.start)
        assertEquals(0, editor.end)
        controller.undo()
        assertEquals("abc", editor.text)
        assertEquals(3, editor.start)
        assertEquals(3, editor.end)
    }

    @Test
    fun `selected delete undo can repeat without duplicating or restoring selection`() {
        val editor = Editor()
        val controller = DestructiveEditingController(editor)
        repeat(3) {
            editor.start = 0
            editor.end = editor.text.length
            editor.editingSelection.update(editor.start, editor.end)
            controller.deleteSelection()
            assertEquals("", editor.text)
            controller.undo()
            assertEquals("abc", editor.text)
            assertEquals(editor.start, editor.end)
        }
    }

    @Test
    fun `clear during pinyin composition restores only committed text at its original cursor`() {
        val editor = Editor().apply {
            text = "abcni"
            start = 5
            end = 5
            editingSelection.update(5, 5)
            editingComposition.update(3, 5)
        }
        val controller = DestructiveEditingController(editor)

        controller.clearAll()
        assertEquals("", editor.text)
        assertEquals(1, editor.finishedComposition)
        assertEquals(0, editor.start)
        assertEquals(0, editor.end)

        controller.undo()
        assertEquals("abc", editor.text)
        assertEquals(2, editor.finishedComposition)
        assertEquals(3, editor.start)
        assertEquals(3, editor.end)
    }

    @Test
    fun `clear undo replaces delayed stale composition instead of duplicating document`() {
        val editor = Editor().apply {
            text = "abcni"
            start = 5
            end = 5
            editingSelection.update(5, 5)
            editingComposition.update(3, 5)
        }
        val controller = DestructiveEditingController(editor)

        controller.clearAll()
        // Simulate a late client-preedit callback arriving after the Android clear transaction.
        editor.text = "ni"
        editor.start = 2
        editor.end = 2
        editor.editingSelection.update(2, 2)

        controller.undo()
        assertEquals("abc", editor.text)
        assertEquals(3, editor.start)
        assertEquals(3, editor.end)
        controller.undo()
        assertEquals("abc", editor.text)
    }

    @Test
    fun `plain clear undo restores the document exactly once`() {
        val editor = Editor().apply {
            text = "abc中文"
            start = text.length
            end = start
            editingSelection.update(start, end)
        }
        val controller = DestructiveEditingController(editor)

        controller.clearAll()
        controller.undo()
        controller.undo()

        assertEquals("abc中文", editor.text)
        assertEquals(editor.text.length, editor.start)
        assertEquals(editor.start, editor.end)
    }

    @Test
    fun `composition in the middle is excluded and cursor returns to the committed boundary`() {
        val editor = Editor().apply {
            text = "前ni后"
            start = 3
            end = 3
            editingSelection.update(3, 3)
            editingComposition.update(1, 3)
        }
        val controller = DestructiveEditingController(editor)

        controller.clearAll()
        controller.undo()

        assertEquals("前后", editor.text)
        assertEquals(1, editor.start)
        assertEquals(editor.start, editor.end)
    }

    @Test
    fun `ordinary delete after range delete replaces history with the latest mutation`() {
        val editor = Editor().apply {
            text = "abcXYZ"
            start = 3
            end = 6
            editingSelection.update(start, end)
            simulateBackspace = false
        }
        val controller = DestructiveEditingController(editor)

        controller.deleteSelection()
        controller.deleteBackward()
        assertEquals("ab", editor.text)

        controller.undo()
        assertEquals("abc", editor.text)
        controller.undo()
        assertEquals("abc", editor.text)
    }

    @Test
    fun `supplementary unicode backspace and undo preserve one code point`() {
        val editor = Editor().apply {
            text = "甲😀"
            start = text.length
            end = start
            editingSelection.update(start, end)
            simulateBackspace = false
        }
        val controller = DestructiveEditingController(editor)

        controller.deleteBackward()
        assertEquals("甲", editor.text)
        controller.undo()

        assertEquals("甲😀", editor.text)
        assertEquals(editor.text.length, editor.start)
        assertEquals(editor.start, editor.end)
    }
}
