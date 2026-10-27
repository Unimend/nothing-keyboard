/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input.session

import org.fcitx.fcitx5.android.core.Action
import org.fcitx.fcitx5.android.core.CandidateAction
import org.fcitx.fcitx5.android.core.CandidateWord
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.core.FormattedText
import org.fcitx.fcitx5.android.core.InputMethodEntry
import org.fcitx.fcitx5.android.core.InputMethodSubMode

/** Temporary compatibility output for UI components not yet migrated to project-owned models. */
object LegacyEngineStateAdapter {
    fun text(value: EngineText) = FormattedText(
        value.segments.map { it.text }.toTypedArray(),
        value.segments.map { it.formatFlags }.toIntArray(),
        value.cursor
    )

    fun inputPanel(value: EngineInputPanel) = FcitxEvent.InputPanelEvent.Data(
        text(value.preedit),
        text(value.auxiliaryUp),
        text(value.auxiliaryDown),
        value.tabs.map {
            CandidateAction(it.id, it.text, it.separator, it.icon, it.checkable, it.checked)
        }.toTypedArray()
    )

    fun candidates(value: EngineCandidateList) = FcitxEvent.CandidateListEvent.Data(
        value.total,
        value.items.map(::candidate).toTypedArray()
    )

    fun pagedCandidates(value: EnginePagedCandidates) = FcitxEvent.PagedCandidateEvent.Data(
        value.items.map(::candidate).toTypedArray(),
        value.cursorIndex,
        when (value.layout) {
            EngineCandidateLayout.Unspecified -> FcitxEvent.PagedCandidateEvent.LayoutHint.NotSet
            EngineCandidateLayout.Vertical -> FcitxEvent.PagedCandidateEvent.LayoutHint.Vertical
            EngineCandidateLayout.Horizontal -> FcitxEvent.PagedCandidateEvent.LayoutHint.Horizontal
        },
        value.hasPrevious,
        value.hasNext
    )

    fun inputMethod(value: EngineInputMethod) = InputMethodEntry(
        value.uniqueName,
        value.name,
        value.icon,
        value.nativeName,
        value.label,
        value.languageCode,
        value.addon,
        value.configurable,
        InputMethodSubMode(value.subModeName, value.subModeLabel, value.subModeIcon)
    )

    fun statusActions(value: List<EngineStatusAction>): Array<Action> = value.map {
        Action(
            it.id, it.separator, it.checkable, it.checked, it.name, it.icon,
            it.shortText, it.longText, statusActions(it.children).takeIf { children ->
                children.isNotEmpty()
            }
        )
    }.toTypedArray()

    private fun candidate(value: EngineCandidate) = CandidateWord(
        value.label, value.text, value.comment, value.spaceBeforeComment
    )
}
