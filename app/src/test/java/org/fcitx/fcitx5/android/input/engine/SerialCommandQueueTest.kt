package org.fcitx.fcitx5.android.input.engine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class SerialCommandQueueTest {
    @Test
    fun `joining a later command cannot overtake a suspended command`() = runBlocking {
        val owner = SupervisorJob(coroutineContext[Job])
        try {
            val queue = SerialCommandQueue(CoroutineScope(coroutineContext + owner))
            val gate = CompletableDeferred<Unit>()
            val started = CompletableDeferred<Unit>()
            val events = mutableListOf<Int>()
            queue.submit { events += 1; started.complete(Unit); gate.await(); events += 2 }
            val second = queue.submit { events += 3 }
            val waiter = launch { second.join() }
            withTimeout(2000) { started.await() }
            yield()
            assertEquals(listOf(1), events)
            gate.complete(Unit)
            withTimeout(2000) { waiter.join() }
            assertEquals(listOf(1, 2, 3), events)
        } finally { owner.cancelAndJoin() }
    }

    @Test
    fun `cancelled queued command is skipped and following work continues`() = runBlocking {
        val owner = SupervisorJob(coroutineContext[Job])
        try {
            val queue = SerialCommandQueue(CoroutineScope(coroutineContext + owner))
            val gate = CompletableDeferred<Unit>()
            queue.submit { gate.await() }
            var cancelledRan = false
            val cancelled = queue.submit { cancelledRan = true }
            cancelled.cancel()
            var lastRan = false
            val last = queue.submit { lastRan = true }
            gate.complete(Unit)
            withTimeout(2000) { last.join() }
            assertFalse(cancelledRan)
            assertTrue(lastRan)
        } finally { owner.cancelAndJoin() }
    }

    @Test
    fun `failure completes its handle without blocking the queue`() = runBlocking {
        val owner = SupervisorJob(coroutineContext[Job])
        try {
            val queue = SerialCommandQueue(CoroutineScope(coroutineContext + owner))
            val failed = queue.submit { error("test failure") }
            var continued = false
            val next = queue.submit { continued = true }
            withTimeout(2000) { next.join() }
            assertTrue(failed.isCancelled)
            assertTrue(continued)
        } finally { owner.cancelAndJoin() }
    }

    @Test
    fun `ending owner cancels active pending and future handles`() = runBlocking {
        val owner = SupervisorJob(coroutineContext[Job])
        val queue = SerialCommandQueue(CoroutineScope(coroutineContext + owner))
        val gate = CompletableDeferred<Unit>()
        val active = queue.submit { gate.await() }
        val pending = queue.submit { fail("Pending command ran after shutdown") }
        yield()
        owner.cancelAndJoin()
        val late = queue.submit { fail("Late command ran after shutdown") }
        withTimeout(2000) { active.join(); pending.join(); late.join() }
        assertTrue(active.isCancelled)
        assertTrue(pending.isCancelled)
        assertTrue(late.isCancelled)
    }
}
