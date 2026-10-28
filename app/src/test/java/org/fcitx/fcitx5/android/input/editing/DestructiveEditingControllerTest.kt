package org.fcitx.fcitx5.android.input.editing

import android.view.inputmethod.InputConnection
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
        override val editingSelection = CursorRange(0, 3)
        override val hasActiveComposition = false
        override val shouldSimulateBackspaceKey = true
        override val editingConnection = Proxy.newProxyInstance(
            InputConnection::class.java.classLoader,
            arrayOf(InputConnection::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getSelectedText" -> text.substring(start, end)
                "commitText" -> {
                    val replacement = args!![0].toString()
                    text = text.replaceRange(start, end, replacement)
                    start += replacement.length
                    end = start
                    true
                }
                "setSelection" -> { start = args!![0] as Int; end = args[1] as Int; true }
                "beginBatchEdit", "endBatchEdit" -> true
                else -> error("Unexpected editor call: ${method.name}")
            }
        } as InputConnection
        override fun predictEditingSelection(start: Int, end: Int) = editingSelection.update(start, end)
        override fun resetEditingSelection(start: Int, end: Int) = editingSelection.update(start, end)
        override fun resetEditingComposition() = Unit
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
}
