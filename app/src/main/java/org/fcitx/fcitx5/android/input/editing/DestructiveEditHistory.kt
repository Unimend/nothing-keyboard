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
}

/** One-level undo history owned by our destructive gestures, independent of app/system Undo. */
class DestructiveEditHistory {
    private var lastEdit: DestructiveEditTransaction? = null

    fun record(edit: DestructiveEditTransaction) {
        lastEdit = edit.takeIf { it.deletedText.isNotEmpty() }
    }

    fun consume(): DestructiveEditTransaction? = lastEdit.also { lastEdit = null }

    fun clear() {
        lastEdit = null
    }
}
