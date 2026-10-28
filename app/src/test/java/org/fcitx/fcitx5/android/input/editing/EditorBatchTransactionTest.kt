package org.fcitx.fcitx5.android.input.editing

import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy
import org.fcitx.fcitx5.android.utils.withBatchEdit
import org.junit.Assert.*
import org.junit.Test

class EditorBatchTransactionTest {
    private fun connection(calls: MutableList<String>, supported: Boolean = true) =
        Proxy.newProxyInstance(InputConnection::class.java.classLoader,
            arrayOf(InputConnection::class.java)) { _, method, _ ->
            calls += method.name
            supported
        } as InputConnection

    @Test
    fun `successful edits close exactly one batch`() {
        val calls = mutableListOf<String>()
        connection(calls).withBatchEdit { commitText("abc", 1) }
        assertEquals(listOf("beginBatchEdit", "commitText", "endBatchEdit"), calls)
    }

    @Test
    fun `failed edit closes batch and propagates failure`() {
        val calls = mutableListOf<String>()
        val failure = IllegalStateException("editor disconnected")
        try {
            connection(calls).withBatchEdit { throw failure }
            fail("Failure was swallowed")
        } catch (actual: IllegalStateException) {
            assertSame(failure, actual)
        }
        assertEquals(listOf("beginBatchEdit", "endBatchEdit"), calls)
    }

    @Test
    fun `edit still runs for editors without batching support`() {
        val calls = mutableListOf<String>()
        connection(calls, supported = false).withBatchEdit { commitText("abc", 1) }
        assertEquals(listOf("beginBatchEdit", "commitText", "endBatchEdit"), calls)
    }
}
