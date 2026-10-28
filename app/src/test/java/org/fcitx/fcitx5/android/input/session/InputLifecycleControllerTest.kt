/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.session

import kotlinx.coroutines.Job
import org.fcitx.fcitx5.android.input.editing.EditingCommand
import org.fcitx.fcitx5.android.input.editing.EditingCommandPort
import org.fcitx.fcitx5.android.input.engine.EngineCommand
import org.fcitx.fcitx5.android.input.engine.EngineCommandPort
import org.junit.Assert.assertEquals
import org.junit.Test

class InputLifecycleControllerTest {
    private val calls = mutableListOf<String>()
    private val editor = object : InputLifecycleEditor {
        override fun startInput(start: Int, end: Int) { calls += "editor:start:$start:$end" }
        override fun selectionChanged(start: Int, end: Int, composingStart: Int, composingEnd: Int) {
            calls += "editor:selection:$start:$end:$composingStart:$composingEnd"
        }
        override fun finishInputView() { calls += "editor:finishView" }
        override fun unbindInput() { calls += "editor:unbind" }
    }
    private val editing = EditingCommandPort { calls += "editing:$it" }
    private val engine = EngineCommandPort {
        calls += "engine:$it"
        Job().apply { complete() }
    }
    private val session = object : InputLifecycleSessionSink {
        override fun inputStarted(editor: EditorSnapshot, capabilities: Set<EditorCapability>, restarting: Boolean) {
            calls += "session:start:$restarting"
        }
        override fun inputViewStarted(editor: EditorSnapshot, restarting: Boolean) { calls += "session:view:$restarting" }
        override fun selectionChanged(start: Int, end: Int) { calls += "session:selection:$start:$end" }
        override fun inputViewFinished() { calls += "session:finishView" }
        override fun inputFinished() { calls += "session:finish" }
    }
    private val controller = InputLifecycleController(editor, editing, engine, session)
    private val snapshot = EditorSnapshot(
        packageName = "test",
        initialSelection = SessionRange(1, 2)
    )

    @Test
    fun `restart orders gesture reset editor session and engine refocus`() {
        controller.startInput(snapshot, emptySet(), 1, 2, restarting = true, acceptsEngineFocus = true)
        assertEquals(
            listOf(
                "editing:${EditingCommand.ResetDeletionGesture}",
                "editor:start:1:2",
                "session:start:true",
                "engine:${EngineCommand.Focus(false)}",
                "engine:${EngineCommand.SetCapabilities(emptySet())}",
                "engine:${EngineCommand.Focus(true)}"
            ),
            calls
        )
    }

    @Test
    fun `raw editor receives capabilities without engine focus`() {
        controller.startInput(snapshot, emptySet(), 0, 0, restarting = false, acceptsEngineFocus = false)
        assertEquals(1, calls.count { it.startsWith("engine:") })
        assertEquals("engine:${EngineCommand.SetCapabilities(emptySet())}", calls.last())
    }

    @Test
    fun `selection publishes session state before editor reconciliation`() {
        controller.selectionChanged(4, 7, 2, 6)
        assertEquals(
            listOf("session:selection:4:7", "editor:selection:4:7:2:6"),
            calls
        )
    }

    @Test
    fun `view finish and input finish have stable order`() {
        controller.startInputView(snapshot, restarting = false)
        controller.finishInputView()
        controller.finishInput()
        controller.unbindInput()
        assertEquals(
            listOf(
                "session:view:false",
                "engine:${EngineCommand.Focus(true)}",
                "session:finishView",
                "editor:finishView",
                "engine:${EngineCommand.FocusOutIn}",
                "session:finish",
                "engine:${EngineCommand.Focus(false)}",
                "editor:unbind"
            ),
            calls
        )
    }

    @Test
    fun `configuration change resets engine through lifecycle owner`() {
        controller.configurationChanged()
        assertEquals(listOf("engine:${EngineCommand.Reset}"), calls)
    }
}
