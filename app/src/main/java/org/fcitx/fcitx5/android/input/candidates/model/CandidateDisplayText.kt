package org.fcitx.fcitx5.android.input.candidates.model

import org.fcitx.fcitx5.android.input.session.EngineCandidate

/** Identical comment spacing for display and width measurement. */
fun EngineCandidate.commentForDisplay(): String = when {
    comment.isBlank() -> ""
    spaceBeforeComment -> " $comment"
    else -> comment
}

fun EngineCandidate.textForDisplay(): String = text + commentForDisplay()
