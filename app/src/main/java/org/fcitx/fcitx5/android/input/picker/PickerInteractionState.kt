/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.picker

import org.fcitx.fcitx5.android.input.mode.PickerKind

data class PickerPosition(
    val categoryIndex: Int = DefaultCategoryIndex,
    val pageInCategory: Int = 0
) {
    companion object {
        const val DefaultCategoryIndex = 1
    }
}

data class PickerInteractionState(
    val positions: Map<PickerKind, PickerPosition> = emptyMap()
) {
    fun position(kind: PickerKind): PickerPosition = positions[kind] ?: PickerPosition()
}

sealed interface PickerInteractionEvent {
    data class PageSelected(
        val kind: PickerKind,
        val categoryIndex: Int,
        val pageInCategory: Int
    ) : PickerInteractionEvent
}

object PickerInteractionReducer {
    fun reduce(
        state: PickerInteractionState,
        event: PickerInteractionEvent
    ): PickerInteractionState = when (event) {
        is PickerInteractionEvent.PageSelected -> state.copy(
            positions = state.positions + (
                event.kind to PickerPosition(
                    categoryIndex = event.categoryIndex.coerceAtLeast(0),
                    pageInCategory = event.pageInCategory.coerceAtLeast(0)
                )
            )
        )
    }
}

internal fun resolvePickerPage(
    position: PickerPosition,
    categoryRanges: List<IntRange>
): Int {
    if (categoryRanges.isEmpty()) return 0
    val fallbackCategory = PickerPosition.DefaultCategoryIndex
        .coerceAtMost(categoryRanges.lastIndex)
    val categoryValid = position.categoryIndex in categoryRanges.indices
    val category = if (categoryValid) position.categoryIndex else fallbackCategory
    val range = categoryRanges[category]
    if (range.isEmpty()) return categoryRanges[fallbackCategory].first.coerceAtLeast(0)
    val pageInCategory = if (categoryValid) position.pageInCategory else 0
    return range.first + pageInCategory.coerceIn(0, range.last - range.first)
}
