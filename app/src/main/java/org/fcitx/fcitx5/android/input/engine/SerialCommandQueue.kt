package org.fcitx.fcitx5.android.input.engine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** A completion handle cannot start queued work early by calling join(). */
internal class SerialCommandQueue(
    private val scope: CoroutineScope,
    private val onFailure: (Throwable) -> Unit = {}
) {
    private val pending = Channel<Job>(Channel.UNLIMITED, onUndeliveredElement = { it.cancel() })

    init {
        scope.launch {
            try {
                for (task in pending) task.join()
            } finally {
                pending.cancel()
            }
        }
    }

    fun submit(block: suspend () -> Unit): Job {
        val completion = CompletableDeferred<Unit>(scope.coroutineContext[Job])
        val task = scope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
                completion.complete(Unit)
            } catch (failure: Throwable) {
                completion.completeExceptionally(failure)
                if (failure !is CancellationException) onFailure(failure)
            }
        }
        completion.invokeOnCompletion { if (completion.isCancelled) task.cancel() }
        task.invokeOnCompletion { if (!completion.isCompleted) completion.cancel() }
        if (!pending.trySend(task).isSuccess) task.cancel()
        return completion
    }
}
