package org.fcitx.fcitx5.android.input.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.fcitx.fcitx5.android.input.session.*
import org.junit.Assert.*
import org.junit.Test

class PunctuationControllerTest {
    private class Reader : InputSessionReader {
        override val state = MutableStateFlow(InputSessionSnapshot(engine = EngineState(
            statusActions = listOf(EngineStatusAction(1, false, true, true, "punctuation",
                "fcitx-punc-active", "", "", emptyList()))
        )))
        override val current get() = state.value
    }

    private class Queries : InputEngineQueries {
        var count = 0
        val oldResponse = CompletableDeferred<Unit>()
        override suspend fun punctuationMapping(languageCode: String): Map<String, String> {
            count++
            if (count == 1) {
                // Simulate an external API that cannot cancel an already submitted query.
                withContext(NonCancellable) { oldResponse.await() }
                return mapOf("." to "old")
            }
            return mapOf("." to "new")
        }
        override suspend fun candidates(offset: Int, limit: Int) = emptyList<EngineCandidate>()
        override suspend fun candidateActions(index: Int) = emptyList<EngineCandidateAction>()
        override suspend fun enabledInputMethods() = emptyList<EngineInputMethod>()
    }

    @Test
    fun `late old query cannot overwrite newer punctuation map`() = runBlocking {
        val queries = Queries()
        val emitted = mutableListOf<Map<String, String>>()
        val controller = PunctuationController(PunctuationStateSink { emitted += it },
            Reader(), queries, this)
        controller.refresh()
        yield()
        controller.refresh()
        yield()
        queries.oldResponse.complete(Unit)
        yield()
        assertEquals(listOf(mapOf("." to "new")), emitted)
        controller.dispose()
    }

    @Test
    fun `disposed owner rejects pending and future refreshes`() = runBlocking {
        val queries = Queries()
        val emitted = mutableListOf<Map<String, String>>()
        val controller = PunctuationController(PunctuationStateSink { emitted += it },
            Reader(), queries, this)
        controller.refresh()
        yield()
        controller.dispose()
        controller.refresh()
        queries.oldResponse.complete(Unit)
        yield()
        assertTrue(emitted.isEmpty())
        assertEquals(1, queries.count)
    }
}
