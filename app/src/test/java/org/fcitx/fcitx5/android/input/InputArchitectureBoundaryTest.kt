/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android.input

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prevents legacy UI code from regaining direct access to centralized input systems. */
class InputArchitectureBoundaryTest {
    @Test
    fun `service cannot implement native engine command execution`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        listOf("FcitxEngineCommandAdapter", "postFcitxJob", "Channel<Job>", "CoroutineStart.LAZY")
            .forEach { assertFalse("Engine queue escaped its boundary: $it", it in source) }
    }

    @Test
    fun `surfaces cannot access concrete editor implementations`() {
        val forbidden = Regex("""\b(EditorController|AndroidEditingCommandExecutor|DestructiveEditingController)\b""")
        val violations = inputSources().filter {
            val path = it.invariantSeparatorsPath
            "/editing/" !in path && it.name != "FcitxInputMethodService.kt" &&
                forbidden.containsMatchIn(it.readText())
        }
        assertTrue("Concrete editor dependency: $violations", violations.isEmpty())
    }

    @Test
    fun `service cannot own composition or destructive editing implementations`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        listOf("FormattedText", "CursorTracker()", "DestructiveEditingController(",
            "setComposingText(", "finishComposingText(", "commitText(text, 1)").forEach {
            assertFalse("Editor implementation leaked into service: $it", it in source)
        }
    }

    @Test
    fun `semantic text spans have one renderer`() {
        val violations = inputSources().filter {
            it.name != "EngineTextRenderer.kt" &&
                "EngineTextStyle" in it.readText() &&
                "setSpan(" in it.readText()
        }
        assertTrue("Duplicated semantic text rendering: $violations", violations.isEmpty())
    }

    private val sourceRoot: File by lazy {
        sequenceOf(File("app/src/main/java"), File("../app/src/main/java"))
            .first { it.isDirectory }
    }

    private fun inputSources(): List<File> = sourceRoot
        .resolve("org/fcitx/fcitx5/android/input")
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .toList()

    @Test
    fun `input surfaces cannot choose typefaces outside typography system`() {
        val allowed = setOf("design/NothingTypography.kt", "AutoScaleTextView.kt")
        val violations = inputSources().filter { file ->
            val relative = file.relativeTo(sourceRoot).invariantSeparatorsPath
            allowed.none(relative::endsWith) &&
                Regex("""\bsetTypeface\s*\(|\.typeface\s*=|\btypeface\s*=""")
                    .containsMatchIn(file.readText())
        }
        assertTrue("Direct typeface access: ${violations.joinToString()}", violations.isEmpty())
    }

    @Test
    fun `views cannot mutate Android editor outside editing boundaries`() {
        val allowed = setOf(
            "FcitxInputMethodService.kt",
            "editing/AndroidEditingCommandExecutor.kt",
            "editing/EditorController.kt",
            "editing/DestructiveEditingController.kt",
            "engine/AndroidKeyEventController.kt"
        )
        val mutation = Regex(
            """currentInputConnection|import android\.view\.inputmethod\.InputConnection"""
        )
        val violations = inputSources().filter { file ->
            val relative = file.relativeTo(sourceRoot).invariantSeparatorsPath
            allowed.none(relative::endsWith) && mutation.containsMatchIn(file.readText())
        }
        assertTrue("Direct editor mutation: ${violations.joinToString()}", violations.isEmpty())
    }

    @Test
    fun `only navigator and bootstrap may attach input windows`() {
        val allowed = setOf(
            "InputView.kt",
            "navigation/InputNavigator.kt",
            "wm/InputWindowManager.kt"
        )
        val violations = inputSources().filter { file ->
            val relative = file.relativeTo(sourceRoot).invariantSeparatorsPath
            allowed.none(relative::endsWith) && ".attachWindow(" in file.readText()
        }
        assertTrue("Direct window navigation: ${violations.joinToString()}", violations.isEmpty())
    }

    @Test
    fun `picker category model has no drawable bypass`() {
        val data = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/picker/PickerData.kt"
        ).readText()
        val tabs = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/picker/PickerTabsUi.kt"
        ).readText()
        assertFalse("Picker categories must remain semantic text", "val icon:" in data)
        assertFalse("Picker tabs must not branch around typography", "category.icon" in tabs)
        assertTrue("Picker labels must use the shared typography API", "setNothingText(" in tabs)
    }

    @Test
    fun `legacy input broadcast API cannot return`() {
        val violations = inputSources().filter {
            val source = it.readText()
            "InputBroadcaster" in source || "InputBroadcastReceiver" in source ||
                "input.broadcast" in source
        }
        assertTrue("Legacy input broadcast API: ${violations.joinToString()}", violations.isEmpty())
    }

    @Test
    fun `Fcitx protocol types stay inside engine platform boundary`() {
        val allowed = setOf(
            "FcitxInputMethodService.kt",
            "engine/"
        )
        val violations = inputSources().filter { file ->
            val relative = file.relativeTo(sourceRoot).invariantSeparatorsPath
            val permitted = allowed.any { token ->
                if (token.endsWith("/")) "/$token" in relative else relative.endsWith(token)
            }
            !permitted && "org.fcitx.fcitx5.android.core" in file.readText()
        }
        assertTrue("Fcitx type leaked downstream: ${violations.joinToString()}", violations.isEmpty())
    }

    @Test
    fun `legacy engine vocabulary cannot shape keyboard actions`() {
        val keyboardRoot = sourceRoot.resolve("org/fcitx/fcitx5/android/input/keyboard")
        val forbidden = Regex("""\b(FcitxKeyAction|FcitxKeyMapping|KeySym|KeyStates|ScancodeMapping)\b""")
        val violations = keyboardRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && forbidden.containsMatchIn(it.readText()) }
            .toList()
        assertTrue("Legacy engine vocabulary in keyboard model: ${violations.joinToString()}", violations.isEmpty())
    }

    @Test
    fun `session mutations have explicit owners`() {
        val allowed = setOf(
            "FcitxInputMethodService.kt",
            "engine/FcitxEngineBridge.kt",
            "engine/PunctuationController.kt",
            "wm/InputWindowManager.kt",
            "session/InputSessionStore.kt"
        )
        val violations = inputSources().filter { file ->
            val relative = file.relativeTo(sourceRoot).invariantSeparatorsPath
            allowed.none(relative::endsWith) &&
                Regex("""inputSession\.dispatch|session\.dispatch\(InputSessionEvent""")
                    .containsMatchIn(file.readText())
        }
        assertTrue("Unowned session mutation: ${violations.joinToString()}", violations.isEmpty())
    }

    @Test
    fun `hardware key correlation has one owner`() {
        val allowed = setOf(
            "engine/AndroidKeyEventController.kt",
            "engine/FcitxEngineCommandAdapter.kt"
        )
        val hardwareState = Regex("""LruCache<Int, KeyEvent>|ScancodeMapping|clearMetaKeyStates""")
        val violations = inputSources().filter { file ->
            val relative = file.relativeTo(sourceRoot).invariantSeparatorsPath
            allowed.none(relative::endsWith) && hardwareState.containsMatchIn(file.readText())
        }
        assertTrue("Hardware key state escaped its controller: ${violations.joinToString()}", violations.isEmpty())
    }
}
