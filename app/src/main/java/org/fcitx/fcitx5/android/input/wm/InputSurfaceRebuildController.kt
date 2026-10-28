/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.input.wm

import androidx.annotation.Keep
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.data.prefs.ManagedPreferenceProvider
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.data.theme.ThemeManager

interface InputSurfaceRebuildHost {
    fun replaceKeyboard(theme: Theme)
    fun replaceCandidates(theme: Theme)
    fun replaceAll(theme: Theme)
}

interface InputSurfaceRebuildSignals {
    fun start(
        keyboardChanged: () -> Unit,
        candidatesChanged: () -> Unit,
        themeChanged: (Theme) -> Unit
    )

    fun stop()
}

/** Maps rebuild reasons to the smallest surface replacement with one symmetric lifetime. */
class InputSurfaceRebuildController(
    private val signals: InputSurfaceRebuildSignals,
    private val activeTheme: () -> Theme,
    private val host: InputSurfaceRebuildHost
) {
    fun start() = signals.start(
        keyboardChanged = { host.replaceKeyboard(activeTheme()) },
        candidatesChanged = { host.replaceCandidates(activeTheme()) },
        themeChanged = host::replaceAll
    )

    fun createInitialSurfaces() = host.replaceAll(activeTheme())

    fun stop() = signals.stop()
}

/** Android preference/theme listener adapter. It owns registration and unregistration together. */
class AndroidInputSurfaceRebuildSignals(
    private val keyboardPreferences: Array<ManagedPreference<*>>,
    private val candidatePreferences: ManagedPreferenceProvider
) : InputSurfaceRebuildSignals {
    private var started = false
    private var onKeyboardChanged: () -> Unit = {}
    private var onCandidatesChanged: () -> Unit = {}
    private var onThemeChanged: (Theme) -> Unit = {}

    @Keep
    private val keyboardListener = ManagedPreference.OnChangeListener<Any> { _, _ ->
        onKeyboardChanged()
    }

    @Keep
    private val candidatesListener = ManagedPreferenceProvider.OnChangeListener {
        onCandidatesChanged()
    }

    @Keep
    private val themeListener = ThemeManager.OnThemeChangeListener {
        onThemeChanged(it)
    }

    override fun start(
        keyboardChanged: () -> Unit,
        candidatesChanged: () -> Unit,
        themeChanged: (Theme) -> Unit
    ) {
        if (started) return
        started = true
        onKeyboardChanged = keyboardChanged
        onCandidatesChanged = candidatesChanged
        onThemeChanged = themeChanged
        keyboardPreferences.forEach { it.registerOnChangeListener(keyboardListener) }
        candidatePreferences.registerOnChangeListener(candidatesListener)
        ThemeManager.addOnChangedListener(themeListener)
    }

    override fun stop() {
        if (!started) return
        keyboardPreferences.forEach { it.unregisterOnChangeListener(keyboardListener) }
        candidatePreferences.unregisterOnChangeListener(candidatesListener)
        ThemeManager.removeOnChangedListener(themeListener)
        onKeyboardChanged = {}
        onCandidatesChanged = {}
        onThemeChanged = {}
        started = false
    }
}
