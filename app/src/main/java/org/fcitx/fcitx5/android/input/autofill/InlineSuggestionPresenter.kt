/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.autofill

import android.content.Context
import android.os.Build
import android.util.Size
import android.view.ViewGroup
import android.view.inputmethod.InlineSuggestion
import android.view.inputmethod.InlineSuggestionsResponse
import android.widget.inline.InlineContentView
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlinx.coroutines.*

data class InlineSuggestionContent(
    val pinned: InlineContentView?,
    val scrollable: List<InlineContentView?>
)

/** Owns platform suggestion inflation and latest-request lifetime; never accesses toolbar internals. */
@RequiresApi(Build.VERSION_CODES.R)
class InlineSuggestionPresenter(
    private val context: Context,
    private val scope: CoroutineScope,
    heightPx: Int,
    private val render: (InlineSuggestionContent?) -> Unit
) {
    private val size = Size(ViewGroup.LayoutParams.WRAP_CONTENT, heightPx)
    private val executor = Executor { it.run() }
    private var request: Job? = null
    private var disposed = false

    fun show(response: InlineSuggestionsResponse): Boolean {
        if (disposed) return false
        clear()
        val order = orderInlineSuggestions(response.inlineSuggestions) { it.info.isPinned }
        if (order.pinned == null && order.scrollable.isEmpty()) return true
        request = scope.launch {
            val content = coroutineScope {
                val pinned = async { order.pinned?.let { inflate(it) } }
                val scrollable = order.scrollable.map { item -> async { inflate(item) } }
                InlineSuggestionContent(pinned.await(), scrollable.awaitAll())
            }
            ensureActive()
            if (!disposed) render(content)
        }
        return true
    }

    fun clear() {
        request?.cancel()
        request = null
        render(null)
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        clear()
    }

    private suspend fun inflate(suggestion: InlineSuggestion): InlineContentView? =
        suspendCancellableCoroutine { continuation ->
            suggestion.inflate(context, size, executor) { view -> continuation.resume(view) }
        }
}
