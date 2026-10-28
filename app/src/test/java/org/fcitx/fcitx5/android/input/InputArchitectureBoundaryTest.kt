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
    fun `candidate surfaces cannot manipulate toolbar private state or other adapters`() {
        val paths = listOf("candidates/horizontal/HorizontalCandidateComponent.kt",
            "candidates/expanded/window/BaseExpandedCandidateWindow.kt")
        paths.forEach { name ->
            val source = sourceRoot.resolve("org/fcitx/fcitx5/android/input/$name").readText()
            assertFalse(name, "expandButtonStateMachine" in source)
            assertFalse(name, "horizontalCandidate.adapter" in source)
        }
        val bar = sourceRoot.resolve("org/fcitx/fcitx5/android/input/bar/KawaiiBarComponent.kt").readText()
        assertTrue("private val expandButtonStateMachine" in bar)
        assertTrue("candidateInteraction.snapshots.collect" in bar)
    }

    @Test
    fun `key routing cannot inspect candidate renderer or own deletion gesture state`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/keyboard/CommonKeyActionListener.kt"
        ).readText()
        assertFalse("HorizontalCandidateComponent" in source)
        assertFalse("BackspaceSwipeState" in source)
        assertTrue("EditingCommand.DeletionGestureMove" in source)
        val editing = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/editing/TextEditingWindow.kt"
        ).readText()
        assertTrue("EditingCommand.DeletionGestureMove" in editing)
        assertTrue("EditingCommand.DeletionGestureRelease" in editing)
    }

    @Test
    fun `every gesture view cancels delayed actions on detach and hide`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/keyboard/CustomGestureView.kt"
        ).readText()
        val detached = source.substringAfter("override fun onDetachedFromWindow()")
            .substringBefore("override fun onWindowVisibilityChanged")
        assertTrue("cancelGestures()" in detached)
        assertTrue("KeyboardFeedback.cancel(this)" in detached)
        val hidden = source.substringAfter("override fun onWindowVisibilityChanged")
            .substringBefore("@SuppressLint")
        assertTrue("cancelGestures()" in hidden)
        val cancel = source.substringAfter("fun cancelGestures()").substringBefore("isPressed = false")
        assertTrue("if (touchActive)" in cancel)
        assertFalse("System.currentTimeMillis()" in source)
    }

    @Test
    fun `service delegates content and touch insets to window adapter`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        assertFalse("inputViewLocation" in source)
        assertFalse("touchableInsets =" in source)
        assertTrue("windowLayout.computeInsets" in source)
    }

    @Test
    fun `service delegates floating candidate geometry to window system`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        assertTrue("candidateAnchor.update(info)" in source)
        assertFalse("anchorPosition" in source)
        assertFalse("decorLocationUpdated" in source)
        assertFalse("info.matrix.mapPoints" in source)
    }

    @Test
    fun `service delegates attached dialog ownership to platform controller`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        assertTrue("ImeDialogController(" in source)
        assertFalse("showingDialog" in source)
        assertFalse("fun showDialog(" in source)
        assertFalse("TYPE_APPLICATION_ATTACHED_DIALOG" in source)
    }

    @Test
    fun `input method dialog cannot depend on concrete service`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/dialog/InputMethodPickerDialog.kt"
        ).readText()
        assertFalse("FcitxInputMethodService" in source)
        assertFalse("engineCommands" in source)
        assertFalse("inputSession" in source)
        assertTrue("onSelected: (InputMethodData) -> Unit" in source)
    }

    @Test
    fun `concrete input service is visible only at Android composition roots`() {
        val allowed = setOf(
            "FcitxInputMethodService.kt",
            "InputView.kt",
            "CandidatesView.kt"
        )
        val root = sourceRoot.resolve("org/fcitx/fcitx5/android/input")
        val violations = inputSources().filter { file ->
            val relative = file.relativeTo(root).invariantSeparatorsPath
            relative !in allowed && (
                "FcitxInputMethodService" in file.readText() ||
                    "inputMethodService()" in file.readText()
                )
        }
        assertTrue("Concrete service escaped composition root: $violations", violations.isEmpty())
    }

    @Test
    fun `input device policy cannot depend on concrete input method service`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/InputDeviceManager.kt"
        ).readText()
        assertFalse("FcitxInputMethodService" in source)
        assertFalse("superEvaluateInputViewShown" in source)
        assertTrue("systemWantsInputView: () -> Boolean" in source)
    }

    @Test
    fun `candidate and punctuation controllers require narrow ports not Android service`() {
        listOf("candidates/interaction/CandidateCommands.kt",
            "candidates/interaction/CandidateInteractionController.kt",
            "engine/PunctuationController.kt").forEach { name ->
            val source = sourceRoot.resolve("org/fcitx/fcitx5/android/input/$name").readText()
            assertFalse(name, "FcitxInputMethodService" in source)
            assertFalse(name, "inputMethodService" in source)
        }
    }

    @Test
    fun `inline suggestions use one cancellable request with structured children`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/bar/KawaiiBarComponent.kt"
        ).readText()
        val request = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/autofill/InlineSuggestionPresenter.kt"
        ).readText()
        assertTrue("request = scope.launch" in request)
        assertTrue("ensureActive()" in request)
        assertFalse("service.lifecycleScope.async" in source)
        assertFalse("inflateInlineContentView" in source)
        assertTrue("inlinePresenter.dispose()" in source.substringAfter("fun onInputScopeDestroyed()")
            .substringBefore("fun onEditorSessionUpdate"))
        assertTrue("cancelInlineSuggestions()" in source.substringAfter("fun onEditorSessionUpdate")
            .substringBefore("fun onInputPresentationUpdate"))
    }

    @Test
    fun `service delegates autofill styling to platform presentation adapter`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        assertFalse("InlineSuggestionUi.newStyleBuilder" in source)
        assertTrue("InlineSuggestionRequestFactory.create" in source)
    }

    @Test
    fun `clipboard persistence types stay inside storage adapters`() {
        val allowed = setOf("StoredClipboardHistory.kt", "StoredClipboardMutations.kt")
        val violations = inputSources().filter {
            it.name !in allowed && "import org.fcitx.fcitx5.android.data.clipboard." in it.readText()
        }
        assertTrue("Clipboard storage bypass: $violations", violations.isEmpty())
    }

    @Test
    fun `clipboard interaction owns policy without importing persistent singleton`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/clipboard/ClipboardInteractionController.kt"
        ).readText()
        assertFalse("ClipboardManager" in source)
        assertTrue("ClipboardMutationPort" in source)
        assertFalse("realDelete()" in source)
    }

    @Test
    fun `clipboard platform preferences and persistence have separate owners`() {
        val root = sourceRoot.resolve("org/fcitx/fcitx5/android/data/clipboard")
        val files = root.walkTopDown().maxDepth(1)
            .filter { it.isFile && it.extension == "kt" }.toList()
        val platformUsers = files.filter { "android.content.ClipboardManager" in it.readText() }
        val preferenceUsers = files.filter { "AppPrefs" in it.readText() }
        val databaseUsers = files.filter {
            val source = it.readText()
            "ClipboardDao" in source || "ClipboardDatabase" in source
        }
        assertTrue("Android clipboard boundary: $platformUsers",
            platformUsers.map(File::getName) == listOf("PlatformClipboardSource.kt"))
        assertTrue("Clipboard preference boundary: $preferenceUsers",
            preferenceUsers.map(File::getName) == listOf("ClipboardSystem.kt"))
        assertTrue("Clipboard database boundary: $databaseUsers",
            databaseUsers.map(File::getName).toSet() ==
                setOf("ClipboardRepository.kt", "ClipboardSystem.kt"))
        assertFalse(root.resolve("ClipboardManager.kt").exists())
    }

    @Test
    fun `editor operations use the exception-safe batch boundary`() {
        val violations = inputSources().filter {
            Regex("""\b(beginBatchEdit|endBatchEdit)\s*\(""").containsMatchIn(it.readText())
        }
        assertTrue("Unmanaged editor batch: $violations", violations.isEmpty())
    }

    @Test
    fun `touch cancellation has a non-committing shared cleanup path`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/keyboard/CustomGestureView.kt"
        ).readText()
        val cancel = source.substringAfter("MotionEvent.ACTION_CANCEL -> {").substringBefore("return true")
        assertTrue("cancelGestures()" in cancel)
        assertFalse("GestureType.Up" in cancel)
        val cleanup = source.substringAfter("fun cancelGestures() {").substringBefore("isPressed = false")
        assertTrue("GestureType.Cancel" in cleanup)
    }

    @Test
    fun `surface teardown precedes dependency scope destruction`() {
        val source = sourceRoot.resolve("org/fcitx/fcitx5/android/input/InputView.kt").readText()
            .substringAfter("override fun onDetachedFromWindow()")
        assertTrue(source.indexOf("windowManager.dispose()") in 0 until source.indexOf("scope.clear()"))
        assertTrue(source.indexOf("popup.dismissAll()") in 0 until source.indexOf("scope.clear()"))
    }

    @Test
    fun `input surfaces cannot launch work in service lifetime`() {
        val violations = inputSources().filter {
            it.name != "FcitxInputMethodService.kt" && "service.lifecycleScope" in it.readText()
        }
        assertTrue("Surface work escaped into service lifetime: $violations", violations.isEmpty())
        val base = sourceRoot.resolve("org/fcitx/fcitx5/android/input/BaseInputView.kt").readText()
        assertTrue("InputSurfaceLifetime()" in base)
        assertTrue("surfaceLifetime.close()" in base)
    }

    @Test
    fun `popup requests cannot contain mutable output slots`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/popup/PopupAction.kt"
        ).readText()
        assertFalse("Mutable result slot in popup command", "var out" in source)
    }

    @Test
    fun `session and commands cannot expose native capability bits`() {
        listOf("session/InputSessionStore.kt", "engine/EngineCommand.kt").forEach { name ->
            val source = sourceRoot.resolve("org/fcitx/fcitx5/android/input/$name").readText()
            assertFalse("Native bit layout escaped into $name", "ULong" in source)
            assertFalse("Legacy capability field returned in $name", "capabilityFlags" in source)
        }
    }

    @Test
    fun `candidate grid measurement uses shared semantic typography`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/candidates/expanded/GridPagingCandidateViewAdapter.kt"
        ).readText()
        assertFalse("Generic paint bypasses mixed typography", "android.graphics.Paint" in source)
        assertTrue("Measure using actual typography", "NothingTextMeasurer" in source)
        assertTrue("Measure exactly the displayed content", "candidate.textForDisplay()" in source)
    }

    @Test
    fun `input renderers cannot bypass semantic color roles`() {
        val allowed = setOf(
            "design/InputColorScheme.kt",
            // Android navigation-bar compatibility depends on persisted built-in theme identity.
            "NavigationBarManager.kt"
        )
        val rawColor = Regex(
            """theme\.(backgroundColor|keyboardColor|barColor|keyBackgroundColor|keyTextColor|""" +
                """altKeyBackgroundColor|altKeyTextColor|accentKeyBackgroundColor|""" +
                """accentKeyTextColor|spaceBarColor|keyPressHighlightColor|keyShadowColor|""" +
                """dividerColor|candidateTextColor|candidateLabelColor|candidateCommentColor|""" +
                """popupBackgroundColor|popupTextColor|clipboardEntryColor|""" +
                """genericActiveBackgroundColor|genericActiveForegroundColor)\b"""
        )
        val root = sourceRoot.resolve("org/fcitx/fcitx5/android/input")
        val violations = inputSources().filter { file ->
            val relative = file.relativeTo(root).invariantSeparatorsPath
            relative !in allowed && rawColor.containsMatchIn(file.readText())
        }
        assertTrue("Raw theme colors escaped semantic adapter: $violations", violations.isEmpty())
    }

    @Test
    fun `popup geometry and dimensions have centralized owners`() {
        val component = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/popup/PopupComponent.kt"
        ).readText()
        val container = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/popup/PopupContainerUi.kt"
        ).readText()
        assertFalse("Popup component embeds visual dp literals", Regex("""context\.dp\(-?\d+""")
            .containsMatchIn(component))
        assertTrue("Popup containers must use edge-safe geometry", "PopupGeometry.place(" in container)
        assertTrue("Configuration changes must clear stale popup coordinates",
            "v.post(::dismissAll)" in component)
    }

    @Test
    fun `motion preference and timing have one runtime gateway`() {
        val allowed = setOf(
            "FcitxInputMethodService.kt", // observes preference only to rebuild the input surface
            "feedback/InputMotionPolicy.kt"
        )
        val root = sourceRoot.resolve("org/fcitx/fcitx5/android/input")
        val violations = inputSources().filter { file ->
            val relative = file.relativeTo(root).invariantSeparatorsPath
            relative !in allowed && "disableAnimation" in file.readText()
        }
        assertTrue("Animation preference escaped motion gateway: $violations", violations.isEmpty())
        val directTiming = inputSources().filter { file ->
            file.name != "InputMotionPolicy.kt" &&
                "NothingDesignTokens.Motion" in file.readText()
        }
        assertTrue("Motion timing escaped semantic gateway: $directTiming", directTiming.isEmpty())
    }

    @Test
    fun `persisted app settings enter input code through one boundary`() {
        val root = sourceRoot.resolve("org/fcitx/fcitx5/android/input")
        val violations = inputSources().filter { file ->
            val relative = file.relativeTo(root).invariantSeparatorsPath
            relative != "settings/InputSettings.kt" && "AppPrefs" in file.readText()
        }
        assertTrue("Persisted settings bypassed InputSettings: $violations", violations.isEmpty())
    }

    @Test
    fun `Android editor compatibility decisions have one policy owner`() {
        val editor = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/editing/EditorController.kt"
        ).readText()
        val service = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        assertFalse("Editor controller reintroduced Android input-type branches", "InputType." in editor)
        assertFalse("Service reintroduced capability parsing", "AndroidEditorCapabilities.fromAndroid" in service)
        assertTrue("Editor operations must delegate platform quirks", "AndroidEditorPolicy." in editor)
    }

    @Test
    fun `service input callbacks delegate to one lifecycle coordinator`() {
        val service = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        listOf("imeLifecycle.bind(", "imeLifecycle.startInput(", "imeLifecycle.startInputView(",
            "imeLifecycle.selectionChanged(", "imeLifecycle.finishInputView()",
            "imeLifecycle.finishInput()", "imeLifecycle.unbind(")
            .forEach { assertTrue("Missing IME lifecycle delegation: $it", it in service) }
        assertFalse("Service directly parses editor capabilities", "fromAndroid(attribute" in service)
    }

    @Test
    fun `service creates Android base and window before dependent systems`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText().substringAfter("override fun onCreate()")
        val base = source.indexOf("super.onCreate()")
        val content = source.indexOf("contentView =")
        val daemon = source.indexOf("FcitxDaemon.connect")
        val signals = source.indexOf("surfaceRebuilds.start()")
        assertTrue("Android base must exist before service dependencies", base in 0 until daemon)
        assertTrue("Window content must exist before surface signals", content in 0 until signals)
    }

    @Test
    fun `diagnostics updates are atomic event reductions`() {
        val source = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/debug/InputCoreDiagnostics.kt"
        ).readText()
        assertTrue("Diagnostics must use atomic StateFlow update", "mutableState.update" in source)
        assertTrue("Diagnostics must expose explicit events", "sealed interface InputDiagnosticEvent" in source)
        assertFalse("Diagnostics must not perform read then write increments",
            "mutableState.value.engine" in source)
    }

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
    fun `UI engine commands pass through editing aware history boundary`() {
        val service = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        assertTrue("UI command port must be editing-aware", "EditingAwareEngineCommandPort(" in service)
        assertTrue(
            "Raw dispatcher must not be exposed as the UI command port",
            "val engineCommands: EngineCommandPort get() = engineDispatcher" !in service
        )
    }

    @Test
    fun `Android input callbacks delegate stable lifecycle transitions`() {
        val service = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        val lifecycle = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/session/InputLifecycleController.kt"
        ).readText()

        listOf("imeLifecycle.startInput(", "imeLifecycle.startInputView(",
            "imeLifecycle.finishInputView()", "imeLifecycle.finishInput()", "imeLifecycle.unbind(")
            .forEach { assertTrue("Missing lifecycle delegation: $it", it in service) }
        listOf(
            "EditingCommand.ResetDeletionGesture",
            "EngineCommand.SetCapabilities",
            "EngineCommand.Focus(false)",
            "EngineCommand.FocusOutIn"
        ).forEach { assertTrue("Lifecycle owner is incomplete: $it", it in lifecycle) }
    }

    @Test
    fun `service delegates rebuild signal lifetime to surface controller`() {
        val service = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        assertTrue("Surface rebuild controller must be installed", "InputSurfaceRebuildController(" in service)
        listOf(
            "registerOnChangeListener(recreateInputViewListener)",
            "registerOnChangeListener(recreateCandidatesViewListener)",
            "ThemeManager.addOnChangedListener(onThemeChangeListener)"
        ).forEach { assertFalse("Legacy rebuild listener escaped into Service: $it", it in service) }
        assertTrue("Listener lifetime must start once", "surfaceRebuilds.start()" in service)
        assertTrue("Listener lifetime must stop symmetrically", "surfaceRebuilds.stop()" in service)
    }

    @Test
    fun `selection callbacks enter the lifecycle ordering boundary`() {
        val service = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        assertTrue(
            "Android selection callback must delegate reconciliation order",
            "imeLifecycle.selectionChanged(" in service
        )
        assertFalse(
            "Service must not call the concrete editor selection path",
            "editor.selectionChanged(" in service
        )
    }

    @Test
    fun `input device transitions use one presentation controller`() {
        val service = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        assertTrue(
            "Device changes must enter the presentation transaction",
            "devicePresentation::applyMode" in service
        )
        assertFalse(
            "InputDeviceManager callback must not regain an inline side-effect block",
            Regex("InputDeviceManager\\([^)]*\\)\\s*\\{").containsMatchIn(service)
        )
    }

    @Test
    fun `service delegates engine effect dispatch to router`() {
        val service = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        assertTrue("Engine bridge must target the router", "effectSink = engineEffectRouter::handle" in service)
        assertFalse("Legacy Service router must stay deleted", "private fun handleEngineEffect" in service)
        assertFalse("Service must not reconstruct top-level effect dispatch", "when (effect)" in service)
    }

    @Test
    fun `service delegates engine context binding lifecycle`() {
        val service = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        assertTrue("Bind callback must enter lifecycle coordinator", "imeLifecycle.bind(" in service)
        assertTrue("Unbind callback must enter lifecycle coordinator", "imeLifecycle.unbind(" in service)
        assertFalse("Service must not activate engine context directly", "submitEngineCommand(EngineCommand.ActivateContext" in service)
        assertFalse("Service must not deactivate engine context directly", "submitEngineCommand(EngineCommand.DeactivateContext" in service)
    }

    @Test
    fun `input view presentation cleanup is one ordered transaction`() {
        val service = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        assertTrue("Start presentation must use lifecycle coordinator", "imeLifecycle.startInputView(" in service)
        assertTrue(
            "Finish presentation must wrap lifecycle cleanup",
            "imeLifecycle.finishInputView()" in service
        )
        assertFalse(
            "Service must not keep the old partial anchor cleanup",
            "candidateAnchor.invalidateWindowGeometry()\n        inputDeviceMgr.onFinishInputView()" in service
        )
    }

    @Test
    fun `service delegates configuration and inline suggestion policies`() {
        val service = sourceRoot.resolve(
            "org/fcitx/fcitx5/android/input/FcitxInputMethodService.kt"
        ).readText()
        assertTrue("Configuration diff must use its policy", "ImeConfigurationPolicy.shouldDelegateToSystem" in service)
        assertTrue("Inline requests must use one controller", "inlineSuggestionController.createRequest()" in service)
        assertTrue("Inline responses must use one controller", "inlineSuggestionController.handleResponse(response)" in service)
        assertFalse("Legacy duplicated inline gate must stay deleted", "!inlineSuggestions || !inputDeviceMgr.isVirtualKeyboard" in service)
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
            "session/InputSessionStore.kt"
        )
        val violations = inputSources().filter { file ->
            val relative = file.relativeTo(sourceRoot).invariantSeparatorsPath
            allowed.none(relative::endsWith) &&
                Regex("""\b(InputSessionStore|InputSessionEvent)\b""")
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
