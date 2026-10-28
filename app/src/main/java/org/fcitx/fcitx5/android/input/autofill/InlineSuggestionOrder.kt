/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.autofill

data class InlineSuggestionOrder<T>(val pinned: T?, val scrollable: List<T>)

/** Keep the first pinned item fixed; extra pinned items lead the scrollable list, stably. */
fun <T> orderInlineSuggestions(items: List<T>, isPinned: (T) -> Boolean): InlineSuggestionOrder<T> {
    val pinned = items.filter(isPinned)
    return InlineSuggestionOrder(pinned.firstOrNull(), pinned.drop(1) + items.filterNot(isPinned))
}
