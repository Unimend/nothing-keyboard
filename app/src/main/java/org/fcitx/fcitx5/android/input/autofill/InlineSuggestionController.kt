/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.autofill

import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse

object InlineSuggestionPolicy {
    fun isAvailable(enabled: Boolean, isVirtualKeyboard: Boolean): Boolean =
        enabled && isVirtualKeyboard
}

/** Owns feature gating for both sides of the Android inline-suggestion exchange. */
class InlineSuggestionController(
    private val enabled: () -> Boolean,
    private val isVirtualKeyboard: () -> Boolean,
    private val createRequest: () -> InlineSuggestionsRequest,
    private val showResponse: (InlineSuggestionsResponse) -> Boolean
) {
    fun createRequest(): InlineSuggestionsRequest? {
        if (!InlineSuggestionPolicy.isAvailable(enabled(), isVirtualKeyboard())) return null
        return createRequest.invoke()
    }

    fun handleResponse(response: InlineSuggestionsResponse): Boolean {
        if (!InlineSuggestionPolicy.isAvailable(enabled(), isVirtualKeyboard())) return false
        return showResponse(response)
    }
}
