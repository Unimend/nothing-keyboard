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
| Editing tools | `DestructiveEditingController`, transaction/history policies and `EditingSession` | `EditingCommandPort` / `AndroidEditingCommandExecutor` |
| Engine command order | `SerialCommandQueue` | `FcitxCommandDispatcher` |
| Navigation | `InputNavigator` | `InputWindowManager` |
| Visual language | design tokens, typography and glyph registry | Android Views/drawables |

## 18 responsibility areas: working audit index

This index separates responsibility from completion. Several areas have established boundaries,
but that does not mean the entire product has passed every device/lifecycle case.

| Area | Owner / contract | Remaining verification or cleanup |
|---|---|---|
| 1. Android lifecycle | Service, InputDeviceManager | Further isolate insets/device orchestration |
| 2. Session state | InputSessionStore → InputSessionReader; narrow sinks | Reader-port device smoke passed; broader editor matrix remains |
| 3. Engine integration | EngineCommandPort / InputEngineQueries / EngineEffect | Replacement-engine integration is not yet tested |
| 4. Editor capabilities | EditorCapability + Android policy + native adapter | Password editor/device matrix |
| 5. Composition and cursor | EditorController | Complex external editor/filter cases |
| 6. Key commands and hardware | Semantic keys + AndroidKeyEventController | Physical keyboard and stale native replies |
| 7. Input modes | InputModeStore | Cross-session remembered-mode matrix |
| 8. Candidate data and selection | Candidate models/reducers/interaction controller | Large-list and mixed-font profiling |
| 9. Destructive editing | EditingCommandPort + DestructiveEditingController | Custom editors rejecting edits |
| 10. Clipboard | ClipboardInteractionController + history/mutation ports + ClipboardItem | Platform collection, retention and settings audit |
| 11. Navigation and windows | InputNavigator + InputWindowManager | Disposal now explicit; stress rebuilds |
| 12. Gestures | KeyboardGesturePolicy + shared bindings | Cancel verified on device; multi-touch matrix remains |
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

`DeletionGestureSession` owns the logical range-selection/reset decision inside the editing system.
All surfaces submit move/release/cancel commands; candidate renderer state must never decide whether
a destructive action is allowed. Input-session changes reset this transient state, not undo history.

Down-swipe undo uses `DestructiveEditHistory`, which restores only the last range deletion or
clear-all performed by this keyboard. It must not invoke application/system Undo because that can
reopen a committed CJK phrase as raw composing pinyin.

Destructive edits follow the same transaction rule as UI Maker's Action/History boundary: one
continuous gesture produces one semantic edit and one history entry. The history entry describes
how to restore the document; a renderer or Android callback must not reconstruct that policy.
Range deletion restores by insertion at the deleted range. Clear-all restores by replacing the
document, so a delayed editor/composition callback cannot turn undo into a second copy of the
document. Client preedit is engine-owned transient state and is excluded from committed clear-all
history.

History ownership is explicit at both input boundaries. `EditingHistoryPolicy` classifies editing
commands; `EditingAwareEngineCommandPort` invalidates stale history when software-key text intent
enters the engine, before preedit exists; `AndroidHardwareEditingPolicy` does the same for accepted
physical text/delete keys. Engine reset, focus, navigation, copy and composition rendering preserve
history. Engine `DeleteSurrounding` effects invalidate it because they are mutations not captured by
the destructive transaction controller.

`DestructiveEditingController` owns capture, deletion, clear-all, Unicode-aware backspace history,
and restoration. `EditorController` supplies cursor, composition and engine-reset bridges through
`DestructiveEditingHost`. The service supplies platform connection providers at construction time;
surfaces submit `EditingCommand` through `EditingCommandPort`, never the concrete controller.

Every path that deletes a selected range (gesture release, editing-panel command, or a normal
backspace tap while text is selected) records the same `RestoreRange` transaction. This keeps repeated
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
- Pixel glyph selection is centralized in `NothingGlyphRegistry`.
- Candidate text never shrinks to fit; candidate containers grow and scroll instead.
- New behavior requires a pure policy/model test before device-only verification.

Editing changes require sequence tests, not only isolated-command tests. At minimum cover:

- committed Latin/CJK text + active preedit + clear-all + undo;
- a delayed preedit/editor callback between clear-all and undo;
- selection delete + ordinary delete + undo, repeated more than once;
- supplementary Unicode characters, cursor-in-middle and selection-collapse behavior;
- the same semantic control from keyboard, editing panel and picker/emoji surfaces.

## Migration order

1. Extract visual tokens and typography without changing behavior.
2. Introduce a stable candidate session and indexed candidate model.
3. Separate gesture recognition, action mapping and feedback.
4. Replace ad-hoc window state with a single keyboard-surface state.
5. Split `FcitxInputMethodService` only after the boundaries above are stable.

The first Service split keeps two lifetimes deliberately separate: `InputLifecycleController`
owns stable editor/session/engine transitions, while `FcitxInputMethodService` remains the Android
composition root for views, input-device presentation, candidate anchoring and window callbacks.
Renderers must not reconstruct lifecycle command order.

Android selection updates are lifecycle transitions too: session selection is published before
the editor reconciles cursor and composing ranges. Direct Service-to-editor selection calls are
forbidden because they can expose stale selection state to gesture and undo systems.

Input-surface rebuild reasons are also centralized. Keyboard-layout preferences rebuild only the
keyboard surface, candidate preferences rebuild only candidates, and theme changes rebuild both.
`AndroidInputSurfaceRebuildSignals` is the sole owner of the corresponding listener lifetime.

An input-device mode change is one presentation transaction. Candidate paging, cursor-anchor
monitoring, status-icon visibility and navigation-bar styling must all enter through
`InputDevicePresentationController`; no renderer may update only a subset.

Project-owned engine effects enter `InputEngineEffectRouter`. Editor effects get first refusal;
forwarded keys, input-method changes and switch notifications then route to exactly one Android
host action. The Service supplies those actions but does not reconstruct dispatch priority.

Android binding creates an engine context transaction through `InputContextLifecycleController`:
activate the exact UID/package pair, then synchronize the platform subtype. Unbind deactivates only
when Android still exposes a binding UID; the Service cannot synthesize a replacement identity.

Input-view start and finish presentation enter `InputViewPresentationController`. Hardware-mode
cursor monitoring and fallback anchoring are paired, while finish cleanup has a fixed order around
the core lifecycle transition. This prevents a hidden candidate/status/dialog surface surviving
after the editor lifecycle has ended.

Configuration and inline-autofill callbacks are thin adapters. `ImeConfigurationPolicy` decides
whether Android may rebuild IME state, while `InlineSuggestionController` applies the same enabled
and virtual-keyboard gate to both request creation and response presentation.

The Service decomposition stops at the Android composition-root boundary. Framework callbacks,
protected superclass calls, View construction and concrete platform-object providers remain there.
They are dependency wiring, not unfinished systems; new classes must not be created merely to make
the Service shorter.
