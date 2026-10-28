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

/** Converts engine-owned objects at the boundary; downstream state never depends on Fcitx types. */
object FcitxEngineStateAdapter {
    fun map(event: FcitxEvent<*>): EngineStateEvent? = when (event) {
        is FcitxEvent.ReadyEvent -> EngineStateEvent.Operational
        is FcitxEvent.ClientPreeditEvent ->
            EngineStateEvent.ClientPreeditChanged(event.data.toEngineText())
        is FcitxEvent.InputPanelEvent -> EngineStateEvent.InputPanelChanged(
            EngineInputPanel(
                preedit = event.data.preedit.toEngineText(),
                auxiliaryUp = event.data.auxUp.toEngineText(),
                auxiliaryDown = event.data.auxDown.toEngineText(),
                tabs = event.data.tabs.map {
                    EngineCandidateAction(
                        it.id, it.text, it.isSeparator, it.icon, it.isCheckable, it.isChecked
                    )
                }
            )
        )
        is FcitxEvent.CandidateListEvent -> EngineStateEvent.CandidatesChanged(
            EngineCandidateList(event.data.total, event.data.candidates.map { it.toEngineCandidate() })
        )
        is FcitxEvent.PagedCandidateEvent -> EngineStateEvent.PagedCandidatesChanged(
            EnginePagedCandidates(
                items = event.data.candidates.map { it.toEngineCandidate() },
                cursorIndex = event.data.cursorIndex,
                layout = when (event.data.layoutHint) {
                    FcitxEvent.PagedCandidateEvent.LayoutHint.NotSet -> EngineCandidateLayout.Unspecified
                    FcitxEvent.PagedCandidateEvent.LayoutHint.Vertical -> EngineCandidateLayout.Vertical
                    FcitxEvent.PagedCandidateEvent.LayoutHint.Horizontal -> EngineCandidateLayout.Horizontal
                },
                hasPrevious = event.data.hasPrev,
                hasNext = event.data.hasNext
            )
        )
        is FcitxEvent.IMChangeEvent ->
            EngineStateEvent.InputMethodChanged(event.data.toEngineInputMethod())
        is FcitxEvent.StatusAreaEvent -> EngineStateEvent.StatusAreaChanged(
            event.data.actions.map { it.toEngineStatusAction() },
            event.data.im.toEngineInputMethod()
        )
        else -> null
    }

    fun text(value: FormattedText) = value.toEngineText()

    private fun FormattedText.toEngineText() = EngineText(
        segments = strings.indices.map { EngineTextSegment(strings[it], flags[it]) },
        cursor = cursor
    )

    fun CandidateWord.toEngineCandidate() =
        EngineCandidate(label, text, comment, spaceBetweenComment)

    fun candidate(value: CandidateWord) = value.toEngineCandidate()

    fun candidateAction(value: CandidateAction) = EngineCandidateAction(
        value.id,
        value.text,
        value.isSeparator,
        value.icon,
        value.isCheckable,
        value.isChecked
    )

    fun inputMethod(value: InputMethodEntry) = value.toEngineInputMethod()

    private fun InputMethodEntry.toEngineInputMethod() = EngineInputMethod(
        uniqueName, name, nativeName, label, languageCode, icon, addon, isConfigurable,
        subMode.name, subMode.label, subMode.icon
    )

    private fun Action.toEngineStatusAction(): EngineStatusAction = EngineStatusAction(
        id, isSeparator, isCheckable, isChecked, name, icon, shortText, longText,
        menu?.map { it.toEngineStatusAction() }.orEmpty()
    )
}
