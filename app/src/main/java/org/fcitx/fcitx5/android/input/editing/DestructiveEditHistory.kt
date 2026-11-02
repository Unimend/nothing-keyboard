/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.editing

sealed interface DestructiveEditTransaction {
    val deletedText: String
    val cursorAfterRestore: Int

    data class RestoreRange(
        val start: Int,
        override val deletedText: String,
        override val cursorAfterRestore: Int
    ) : DestructiveEditTransaction

    data class RestoreDocument(
        override val deletedText: String,
        override val cursorAfterRestore: Int
    ) : DestructiveEditTransaction

    /** Restores only engine-owned source input; committed editor text stays untouched. */
    data class RestoreComposition(
        override val deletedText: String,
        override val cursorAfterRestore: Int
    ) : DestructiveEditTransaction

}

/** One-level undo history owned by our destructive gestures, independent of app/system Undo. */
class DestructiveEditHistory {
    private var lastEdit: DestructiveEditTransaction? = null
    private var grouping = false

    fun beginGroup() {
        grouping = true
        lastEdit = null
    }

    fun endGroup() {
        grouping = false
    }

    fun record(edit: DestructiveEditTransaction) {
        if (edit.deletedText.isEmpty()) return
        lastEdit = if (grouping) mergeBackward(lastEdit, edit) else edit
    }

    fun consume(): DestructiveEditTransaction? = lastEdit.also { lastEdit = null }

    fun clear() {
        lastEdit = null
        grouping = false
    }

    private fun mergeBackward(
        previous: DestructiveEditTransaction?,
        next: DestructiveEditTransaction
    ): DestructiveEditTransaction {
        if (previous !is DestructiveEditTransaction.RestoreRange ||
            next !is DestructiveEditTransaction.RestoreRange ||
            next.start + next.deletedText.length != previous.start
        ) {
            return next
        }
        return DestructiveEditTransaction.RestoreRange(
            start = next.start,
            deletedText = next.deletedText + previous.deletedText,
            cursorAfterRestore = previous.cursorAfterRestore
        )
    }
}
