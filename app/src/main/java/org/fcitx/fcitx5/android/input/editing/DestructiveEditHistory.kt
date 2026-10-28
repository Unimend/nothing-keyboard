/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.editing

data class DeletedTextEdit(
    val start: Int,
    val deletedText: String,
    val cursorAfterRestore: Int,
    val restoration: Restoration = Restoration.Insert
)

enum class Restoration {
    Insert,
    ReplaceDocument
}

/** One-level undo history owned by our destructive gestures, independent of app/system Undo. */
class DestructiveEditHistory {
    private var lastEdit: DeletedTextEdit? = null

    fun record(edit: DeletedTextEdit) {
        lastEdit = edit.takeIf { it.deletedText.isNotEmpty() }
    }

    fun consume(): DeletedTextEdit? = lastEdit.also { lastEdit = null }

    fun clear() {
        lastEdit = null
    }
}
