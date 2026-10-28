/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.broadcast

import android.view.inputmethod.EditorInfo
import androidx.annotation.DrawableRes
import org.fcitx.fcitx5.android.core.CapabilityFlags
import org.fcitx.fcitx5.android.input.session.EngineCandidateList
import org.fcitx.fcitx5.android.input.session.EngineInputMethod
import org.fcitx.fcitx5.android.input.session.EngineInputPanel
import org.fcitx.fcitx5.android.input.session.EngineStatusAction
import org.fcitx.fcitx5.android.input.session.EngineText

interface InputBroadcastReceiver {

    fun onStartInput(info: EditorInfo, capFlags: CapabilityFlags) {}

    fun onClientPreeditUpdate(data: EngineText) {}

    fun onInputPanelUpdate(data: EngineInputPanel) {}

    fun onImeUpdate(ime: EngineInputMethod) {}

    fun onCandidateUpdate(data: EngineCandidateList) {}

    fun onStatusAreaUpdate(actions: List<EngineStatusAction>) {}

    fun onSelectionUpdate(start: Int, end: Int) {}

    fun onPunctuationUpdate(mapping: Map<String, String>) {}

    fun onPreeditEmptyStateUpdate(empty: Boolean) {}

    fun onReturnKeyDrawableUpdate(@DrawableRes resourceId: Int) {}

}
