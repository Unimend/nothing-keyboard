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
            "editing/DestructiveEditingController.kt"
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
}
