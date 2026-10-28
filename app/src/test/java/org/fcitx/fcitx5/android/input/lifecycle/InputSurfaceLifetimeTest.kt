package org.fcitx.fcitx5.android.input.lifecycle

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputSurfaceLifetimeTest {
    @Test fun `closing surface cancels every owned child`() = runBlocking {
        val lifetime = InputSurfaceLifetime(Dispatchers.Unconfined)
        val cancelled = CompletableDeferred<Unit>()
        lifetime.scope.launch {
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }

        assertFalse(lifetime.isClosed)
        lifetime.close()
        cancelled.await()
        assertTrue(lifetime.isClosed)
    }

    @Test fun `closing surface is idempotent`() {
        val lifetime = InputSurfaceLifetime(Dispatchers.Unconfined)
        lifetime.close()
        lifetime.close()
        assertTrue(lifetime.isClosed)
    }
}
