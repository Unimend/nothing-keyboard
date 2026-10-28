# Nothing Keyboard architecture

## Boundary

Fcitx remains the input engine. It owns language engines, dictionaries, composing state and the
candidate source. The Nothing layer owns Android presentation, gestures, editing tools and visual
feedback. UI code must not recreate language-engine behavior.

The highest layer owned by this project is the engine/service bridge, not the view session. It
serializes Fcitx commands, translates Android IME lifecycle callbacks, and produces the immutable
`InputSessionSnapshot` consumed downstream. Fcitx itself remains outside the refactor boundary.

## Systems

1. **Engine bridge** — converts Fcitx events and commands into stable application models.
2. **Keyboard session** — current editor, composition, candidate generation and active surface.
3. **Candidate system** — indexed candidate data, incremental loading and selection.
4. **Gesture system** — recognizes gestures separately from the actions they trigger.
5. **Editing tools** — cursor, selection, undo/redo and clipboard commands.
6. **Visual system** — tokens, typography, glyphs, color roles and motion specifications.
7. **Surfaces** — keyboard, candidate rail, expanded candidates, editing panel and pickers.

Having several files inside one system is intentional when they represent different layers
(model, policy, controller, Android renderer). A feature is considered fragmented only when more
than one file owns mutable state, applies the same rule, or talks to a lower platform API. Each
system must therefore have exactly one state owner and one platform adapter; renderers may remain
small, separate files.

Current ownership:

| System | State/rule owner | Platform boundary |
|---|---|---|
| Engine session | `InputSessionStore` | `FcitxEngineBridge` / Fcitx adapters |
| Semantic keys | `EngineKey` and keyboard actions | `FcitxEngineCommandAdapter` |
| Hardware keys | `AndroidKeyEventController` | Android `KeyEvent` + Fcitx adapter |
| Editor state and composition | `EditorController` | Android `InputConnection`; semantic `EngineEffect` input |
| Editing tools | `DestructiveEditingController` and `EditingSession` | `EditingCommandPort` / `AndroidEditingCommandExecutor` |
| Engine command order | `SerialCommandQueue` | `FcitxCommandDispatcher` |
| Navigation | `InputNavigator` | `InputWindowManager` |
| Visual language | design tokens, typography and glyph registry | Android Views/drawables |

## 18 responsibility areas: working audit index

This index separates responsibility from completion. Several areas have established boundaries,
but that does not mean the entire product has passed every device/lifecycle case.

| Area | Owner / contract | Remaining verification or cleanup |
|---|---|---|
| 1. Android lifecycle | Service, InputDeviceManager | Further isolate insets/device orchestration |
| 2. Session state | InputSessionStore → InputSessionReader; narrow sinks | Reader-port device regression |
| 3. Engine integration | EngineCommandPort / InputEngineQueries / EngineEffect | Replacement-engine integration is not yet tested |
| 4. Editor capabilities | EditorCapability + Android policy + native adapter | Password editor/device matrix |
| 5. Composition and cursor | EditorController | Complex external editor/filter cases |
| 6. Key commands and hardware | Semantic keys + AndroidKeyEventController | Physical keyboard and stale native replies |
| 7. Input modes | InputModeStore | Cross-session remembered-mode matrix |
| 8. Candidate data and selection | Candidate models/reducers/interaction controller | Large-list and mixed-font profiling |
| 9. Destructive editing | EditingCommandPort + DestructiveEditingController | Custom editors rejecting edits |
| 10. Clipboard | ClipboardInteractionController | Persistence/privacy lifecycle audit |
| 11. Navigation and windows | InputNavigator + InputWindowManager | Disposal now explicit; stress rebuilds |
| 12. Gestures | KeyboardGesturePolicy + shared bindings | Cancellation and multi-touch matrix |
| 13. Typography | NothingTypography + shared measurer/renderer | Font scale and fallback matrix |
| 14. Functional glyphs | NothingGlyphRegistry | Missing custom assets remain explicit fallbacks |
| 15. Layout/theme | NothingDesignTokens + semantic styles | Scattered dimensions/preferences audit |
| 16. Motion/feedback | InputMotionPolicy + KeyboardFeedback | Remaining surface-local animator cleanup |
| 17. Popups | PopupAction / PopupResult + PopupComponent | Geometry ownership and configuration changes |
| 18. Settings, persistence and diagnostics | AppPrefs/data layer + InputCoreDiagnostics/tests | Settings/data ownership is not yet fully audited |

The session's general write API is private to the Service composition root and store. Ordinary
surfaces receive a reader, not a mutable store. Each producer receives only its own sink: engine
events, surface changes, or punctuation maps. A new producer must not regain general dispatch.

## Dependency direction

```text
Fcitx engine -> engine bridge -> session/models -> controllers -> Android views
                                      ^                |
visual system ------------------------+----------------+
```

Views may render models and emit user intents. They must not infer engine indices, own paging
state, classify fonts independently or embed feature-specific gesture state.

Keyboard definitions and gestures express keys through `EngineKey` and `EngineKeyModifier`.
Fcitx symbols, modifier bitfields and scan-code conversion are private to `input/engine`; adding
one of those protocol types to a surface is an architecture violation, not a convenience API.

## Lifecycle ownership

Every listener, collector, delayed job, animator, and adapter callback must be owned by the
shortest-lived surface that uses it. Registration and cancellation are symmetric:

- service listeners live from `onCreate` to `onDestroy`;
- input-view listeners live from scope setup/attachment to `onDetachedFromWindow`;
- keyboard-layout preferences live only while that layout is attached;
- window jobs and adapter listeners live from `onAttached` to `onDetached`.

`DynamicScope.clear()` releases dependency references but is not a substitute for unregistering
callbacks held by singleton managers or preferences. A component with external registrations must
expose an idempotent destruction hook and the owning view must call it before clearing the scope.

## Shared semantic controls

Buttons with the same meaning on different surfaces must emit the same command and use the same
gesture policy, thresholds and executor. A surface may choose placement, size and visual treatment;
it must not reimplement clear, undo, copy, paste, selection or navigation behavior.

The main keyboard and editing panel delete controls share `KeyboardGesturePolicy`: horizontal drag
extends a deletion range and deletes it on release; upward drag clears all; downward drag undoes.
During the additive stabilization phase, every surface exposes the complete shared behavior set.
Feature reduction must happen later through shared capability configuration, never by deleting or
forking an implementation in one surface.

Gesture feedback is owned by `KeyboardFeedback`. Surfaces request semantic confirmation or step
feedback; they do not construct dot animations or invoke haptics independently.

Every visual backspace key is bound through `BackspaceGestureController`. Main keyboards, the
editing panel, and picker/emoji pages provide only semantic callbacks. The controller owns gesture
thresholds, policy dispatch, feedback, and parent-interception locking for ViewPager surfaces.

Down-swipe undo uses `DestructiveEditHistory`, which restores only the last range deletion or
clear-all performed by this keyboard. It must not invoke application/system Undo because that can
reopen a committed CJK phrase as raw composing pinyin.

`DestructiveEditingController` owns capture, deletion, clear-all, Unicode-aware backspace history,
and restoration. `EditorController` supplies cursor, composition and engine-reset bridges through
`DestructiveEditingHost`. The service supplies platform connection providers at construction time;
surfaces submit `EditingCommand` through `EditingCommandPort`, never the concrete controller.

Every path that deletes a selected range (gesture release, editing-panel command, or a normal
backspace tap while text is selected) records the same `DeletedTextEdit`. This keeps repeated
delete → undo → delete → undo cycles reversible instead of making only the first cycle work.
Ordinary one-code-point backspace uses the same history as well, replacing any older selection
deletion so down-swipe always restores the most recent destructive action.
Restoration always collapses the cursor after the restored content; it never recreates the old
selection highlight, preventing the next backspace from deleting the restored range again.

Undo history intentionally survives changing applications or input fields. This is a product
feature: focus changes do not invalidate it, while real text mutations such as typing, cut, and
paste do.

Return-key completion is resolved in `CommonKeyActionListener`, before the key reaches the Android
editor. With active preedit, Return selects the first Chinese candidate or commits raw text for
other languages; only an empty preedit may reach the editor as newline/send.

User-facing surface changes are commands handled by `InputNavigator`. Keyboard controls, the
editing panel, clipboard, pickers, and expanded candidates emit `ShowKeyboard`, `ShowClipboard`,
`ShowEditing`, or `ShowPicker`; they do not construct another surface or attach it directly.
The navigator owns picker-memory restoration, composition finalization before editing, and the
deferred layout-switch ordering needed when returning from a picker. Input-view bootstrap/reset
and specialized child overlays remain window-manager responsibilities rather than user navigation.

Editor lifecycle, editor identity, capability flags, selection, and active surface are represented
by the service-lifetime `InputSessionStore`. Windows declare their semantic surface and the window
manager publishes transitions into the same snapshot. Surfaces consume typed slices of this store;
the former parallel input broadcast API has been removed and is forbidden by an architecture test.

## Custom rendering audit

- A custom view must preserve semantic spans in both measurement and drawing. Flattening a
  `CharSequence` with `toString()` before drawing is forbidden.
- `AutoScaleTextView` measures and draws styled runs, preserving mixed typefaces and candidate
  colors.
- Candidate grid width estimation uses `NothingTextMeasurer`, sharing semantic spans, letter
  spacing and displayed candidate/comment text with the rendered candidate.
- Pure geometric drawables and RecyclerView decorations draw no semantic text and are unaffected.
- Touch, window, and engine-event components still hold local state; migrate those systems only
  after their state is represented in the session/command layers.

## First-stage rules

- Measurements and typography live under `input/design`.
- Every input-surface text view declares a `NothingTextRole`; surfaces never select a typeface.
- `AdaptiveContent` uses Doto spans only for Latin letters and digits, while CJK and generic
  symbols retain the system font even inside the same string. `ControlLabel` uses Doto and
  `SystemContent` keeps application/user prose in the system font.
- `AutoScaleTextView` must measure and draw styled runs rather than flattening text with
  `toString()`, otherwise mixed-font and candidate-color spans silently disappear.
- Dynamic views must apply their role after replacing text, so recycled keys, candidates,
  composition, clipboard entries, popup legends and picker pages cannot retain a stale typeface.
- Android-owned dialogs and the developer-only editor-info inspector intentionally retain native
  system typography; they are outside the themed input-surface hierarchy.
- Pixel glyph selection will be centralized in a glyph registry.
- Candidate text never shrinks to fit; candidate containers grow and scroll instead.
- New behavior requires a pure policy/model test before device-only verification.

## Migration order

1. Extract visual tokens and typography without changing behavior.
2. Introduce a stable candidate session and indexed candidate model.
3. Separate gesture recognition, action mapping and feedback.
4. Replace ad-hoc window state with a single keyboard-surface state.
5. Split `FcitxInputMethodService` only after the boundaries above are stable.
