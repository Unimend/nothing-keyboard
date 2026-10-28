package org.fcitx.fcitx5.android.input.engine

import java.lang.reflect.Proxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.junit.Assert.assertEquals
import org.junit.Test

class FcitxCommandDispatcherTest {
    @Test
    fun `guard is checked after engine readiness not when command is submitted`() = runBlocking {
        val owner = SupervisorJob(coroutineContext[Job])
        try {
            val scope = CoroutineScope(coroutineContext + owner)
            val ready = CompletableDeferred<Unit>()
            var calls = 0
            val api = Proxy.newProxyInstance(FcitxAPI::class.java.classLoader,
                arrayOf(FcitxAPI::class.java)) { _, _, _ -> calls++; Unit } as FcitxAPI
            val connection = object : FcitxConnection {
                override val lifecycleScope = scope
                override suspend fun <T> runOnReady(block: suspend FcitxAPI.() -> T): T {
                    ready.await()
                    return api.block()
                }
                override fun <T> runImmediately(block: suspend FcitxAPI.() -> T): T =
                    error("Unexpected immediate operation")
                override fun runIfReady(block: suspend FcitxAPI.() -> Unit) =
                    error("Unexpected unqueued operation")
            }
            val dispatcher = FcitxCommandDispatcher(connection, scope)
            var valid = true
            val stale = dispatcher.submit(EngineCommand.Reset) { valid }
            yield()
            valid = false
            ready.complete(Unit)
            withTimeout(2000) { stale.join() }
            assertEquals(0, calls)
            withTimeout(2000) { dispatcher.submit(EngineCommand.Reset).join() }
            assertEquals(1, calls)
        } finally { owner.cancelAndJoin() }
    }
}
