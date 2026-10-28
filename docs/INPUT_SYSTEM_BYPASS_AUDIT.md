# Input system bypass audit

This audit distinguishes a centralized implementation from an enforced boundary. A migration is
not complete merely because the new controller exists: the old call path must be removed or made
unrepresentable, and a test must prevent it from returning.

## Boundary inventory

| System | Single source of truth | Forbidden surface behavior | Current enforcement |
|---|---|---|---|
| Typography | `NothingTypography` roles/styles | Selecting a typeface in a surface | Static boundary test |
| Glyphs | `NothingGlyphRegistry` | Assigning theme-dependent functional glyphs without resolution | `KeyView`, `ToolButton`, `TextEditingButton` resolve centrally |
| Picker tabs | Semantic `Category.label` | Drawable tabs that bypass mixed typography | Drawable field and rendering branch removed |
| Editing commands | `EditingCommand` + `AndroidEditingCommandExecutor` | A View mutating `InputConnection` | Static boundary test |
| Destructive editing | `DestructiveEditingController` | Per-surface delete, clear or undo implementations | Shared controller and gesture binding |
| Editing selection UI | `EditingSession` | Keeping a private selected state after deletion | Delete command collapses local and Android selection |
| Navigation | `InputNavigationCommand` + `InputNavigator` | A control attaching a window directly | Static boundary test |
| Gesture semantics | `KeyboardGesturePolicy` + `BackspaceGestureController` | Per-surface thresholds or delete decisions | Shared controller on keyboard, editing and picker surfaces |
| Candidate data | Candidate session/model reducers | Views inferring engine indices or paging boundaries | Pure model tests |
| Input session | `InputSessionStore` snapshot | UI state sourced from parallel broadcasts | Legacy broadcast API deleted; boundary test forbids its return |
| Engine key protocol | `EngineKey` + `EngineKeyModifier` | Views constructing native symbols, flags or scan codes | Translation exists only in `FcitxEngineCommandAdapter` |
| Hardware key events | `AndroidKeyEventController` | Service/UI caching native events or synthesizing modifier sequences | Static ownership test |
| Engine command scheduling | `FcitxCommandDispatcher` + `SerialCommandQueue` | Service managing native jobs; caller join starting queued work | Queue behavior tests and Service boundary test |

## Removed legacy escape hatches

- `PickerData.Category.icon` and the icon/text branch in `PickerTabsUi`. All symbol, emoji and
  emoticon tabs now enter the same adaptive typography path.
- Direct icon assignment in shared toolbar and editing controls. Theme-dependent resolution now
  passes through `NothingGlyphRegistry`.
- Selection deletion that only predicted a cursor location. It now explicitly collapses both the
  Android editor selection and the editing-panel session state.
- The complete `InputBroadcaster`/`InputBroadcastReceiver` compatibility channel and its
  presentation/return-key mutable relay components. Surfaces now consume `InputSessionStore`
  slices, while return-key presentation is a pure policy derived from the snapshot.
- The `LegacyEngineStateAdapter` name and the misplaced state adapter. Forward and reverse
  conversions now live explicitly under `input/engine` as `FcitxEngineStateAdapter` and
  `FcitxPlatformAdapter`.
- Raw `FcitxKeyMapping`, `KeySym`, `KeyStates` and `ScancodeMapping` values in keyboard models,
  picker controls and gestures. Surfaces now emit project-owned semantic keys; only the Fcitx
  adapter can encode them into native protocol values.
- The engine-branded `FcitxKeyAction` UI model. It is now `TextKeyAction`, which describes intent
  without naming or depending on the current engine implementation.
- Hardware-key correlation caches, sticky-modifier cleanup and synthetic key sequences formerly
  spread across `FcitxInputMethodService`. They now have one owner in
  `AndroidKeyEventController`.

## Deliberate platform boundaries (not legacy UI)

- `FcitxInputMethodService` may access `InputConnection` for engine effects and Android IME
  lifecycle bridging.
- Only `input/engine` may import Fcitx core protocol types. The former Service exception has been
  removed from the architecture test.
- `AndroidEditingCommandExecutor` is the only command-to-editor adapter.
- `DestructiveEditingController` owns mutation/history mechanics and therefore receives an
  `InputConnection` through its host interface.
- `InputView` may bootstrap essential windows; user-triggered navigation must use `InputNavigator`.
- `AutoScaleTextView` may override `setTypeface` internally so it can invalidate measurements, but
  surfaces may not select its typeface.
- Android-owned dialogs and the developer diagnostics inspector retain native typography.

## Remaining migration work, ordered upstream to downstream

Editor migration checkpoint: EditorController now owns the selection tracker, composition range
and text, commit/preedit updates, cursor reconciliation, Return/arrow handling, and the destructive
editing controller. Composition uses EngineText and an Android span renderer, with no Fcitx text
types. The old Service editing forwarding methods have been deleted. Keyboard, navigation, clipboard and toolbar send
EditingCommand through EditingCommandPort; the concrete executor is constructed once by the
service. Selection tracking is private, reads return a copy, and lifecycle changes enter through
startInput, selectionChanged, finishInputView and unbindInput. Cross-editor undo behavior is intentionally preserved.
Editor effects are interpreted by EditorController. EngineTextRenderer owns semantic span conversion
for both editor composition and keyboard preedit; lifecycle transitions invalidate queued cursor moves.
The service still routes engine effects and owns platform lifecycle orchestration; those are
separate remaining boundaries, not completed by the editor extraction.

1. **Service decomposition:** `FcitxInputMethodService` still contains platform lifecycle,
   Android window/insets/device lifecycle orchestration in one large class. Command scheduling and
   subtype synchronization now belong to the engine/platform boundary; editor effects belong to
   editing. Editor capabilities are semantic values, encoded into native bits only by the adapter.
   These are permitted
   platform-boundary operations, but should become separate collaborators before replacing Fcitx.
2. **Window-local state:** toolbar and window animation state is still local to its owning surface.
   Move only cross-surface state into the session reducer; ephemeral animation state should remain
   local.
3. **Candidate measurement validation:** generic `Paint` estimation was replaced with the shared
   typography measurer (real Doto/system spans, letter spacing and candidate style). Comment spacing
   now shares a pure presentation function between measurement and rendering. Device visual checks
   for mixed Latin/CJK/symbol candidates remain pending.
4. **Non-semantic icon coverage:** the registry is now the only resolution route for shared
   controls, but several navigation arrows do not yet have custom pixel assets and intentionally
   fall back to their system drawable until those assets are designed.

Every future migration must finish with three artifacts: one owner, removal of the previous entry
point, and a boundary test. Without all three, the system is still considered transitional.

## 2026-10-28 device regression finding

Fast Select All → Backspace → Undo on the editing surface duplicated the text. Selected-range
backspace previously queued an Android DEL and immediately collapsed the selection. DEL could
arrive after collapse and delete nothing, while undo still restored the captured range. Selected
backspace now delegates to the same atomic editor transaction as DeleteSelection. Unit tests cover
synthetic-key bypass and three repeated delete/restore cycles. Cross-editor undo remains unchanged.

Verification: unit tests and debug APK build passed. Device verified English composition/Return,
ordinary backspace/undo and navigation into Editing before the selected-delete fix. The fix passed
unit tests and was installed, but its device retest was interrupted when the foreground changed to
Notes; do not mark this device case passed. Automated touches were stopped to avoid further changes
to the user's note. Hardware-key acceptance and non-reused correlation IDs were subsequently fixed
and built locally, not installed after that interruption.

Queue tests cover FIFO despite joining later handles, cancellation of queued work, failure isolation,
owner shutdown, and checking validity only after engine readiness. Native translation is no longer
called by Service. The unused parallel `capabilityFlags` field was removed; the session snapshot
remains the published capability state.

## Further boundary closure on 2026-10-28

- Native capability bits were removed from session/events/commands. `EditorCapability` expresses
  requirements; `AndroidEditorCapabilities` classifies editor metadata and `FcitxCapabilityAdapter`
  alone encodes native bits. Numeric password editors are now classified as passwords too.
- `AndroidSubtypeController` owns initial binding and notification echoes. Its pure policy has
  tests; Android 14+ integration cannot be exercised on the current Android 12 device.
- `PopupResult` replaces mutable output slots on popup requests across keyboard and picker pages.
  Popup timing uses monotonic time, repeat dismissals cancel prior jobs, and InputView teardown
  explicitly clears popups.
- `InputWindowManager.dispose()` unloads the active surface before scope destruction. Deferred
  navigation/layout commands are ignored after disposal.
- Service publishes a separate `InputSessionReader` object, not its mutable store. Window and
  punctuation producers receive narrow sink interfaces at construction. Only Service and the
  reducer/store may reference the general session event/write API.
- Punctuation refresh cancels earlier requests and is cancelled on InputView disposal.

Device checks after the user handed over the phone: selected delete/undo no longer duplicates text;
symbol clear/undo restores `abc123`; emoji range deletion restores `abc123😀` with a collapsed cursor;
expanded candidate “比” maps to `比hao`, Return commits `比好`, and delete/undo restores Chinese rather
than pinyin. Rotation with Editing open returns to a ready Keyboard with no crash-buffer entries;
the original `free` rotation setting was restored. The final read-only session-port change passed
the latest APK smoke test: English input/Return, Editing select/delete/undo, Emoji horizontal
delete/undo and Pinyin composition. Candidate font measurement was visually checked in the
expanded Chinese grid; exhaustive mixed-script/density checks remain pending.

## Gesture cancellation closure

Android ACTION_CANCEL previously reused Up, allowing an interrupted gesture to commit a deletion
or popup choice. Cancel is now a separate event through the shared gesture policy. Keyboard teardown
uses the same cancellation entry point. Backspace cancellation collapses transient selection without
deleting; popup cancellation dismisses without choosing; space cancellation cannot open Editing.
Pure tests cover cancellation across all directional step combinations, with a source boundary guard.

On the Android 12 device, injected DOWN/MOVE/CANCEL sequences on the Emoji backspace preserved
`abc😀` for both upward clear and horizontal range gestures; the cursor ended at 5..5. Normal
horizontal deletion followed by down-swipe undo also restored `abc😀` at 5..5. These are separate
from long-press repeat semantics, which may already have committed before cancellation.

## Editor batch boundary

All editor batch operations now use the shared exception-safe `withBatchEdit` helper. Composition
updates no longer manually pair begin/end. Tests cover normal completion, editor-operation failure,
and editors that report batching unsupported; a source guard rejects new manual batches inside input
systems. This closes batches on failure; it does not claim to roll back text already accepted by an
external application's InputConnection.

## Clipboard mutation and surface lifetime boundary

`ClipboardInteractionController` receives `ClipboardMutationPort`; only the persistent adapter
knows the existing storage singleton. Finalization targets explicit owned IDs instead of deleting
every soft-deleted database row. Tests use fake storage and cover serialized delete/undo, idempotent
finalization and failed mutations without phantom undo entries.

Delayed delete/query results carry an attachment generation before rendering a menu/snackbar.
Detached or reattached surfaces reject old results. Replaced snackbar dismissal callbacks cannot
finalize the current batch. Database mutations still complete in the owner scope; only stale UI
delivery is suppressed. No real device clipboard entries were deleted during these tests.

The follow-up read boundary now routes paging and toolbar previews through `ClipboardHistoryPort`.
Surfaces consume `ClipboardItem`, not Room entities; listeners return an explicit close handle.
Only the two storage adapters may import clipboard persistence types inside input systems, enforced
by a source guard. Existing database content and schema remain intact. Platform clipboard collection,
retention preferences and the standalone history-edit Activity still belong to the data/settings
side and are not claimed fully refactored by this runtime-boundary milestone.

Verification: 139 unit/boundary tests passed, debug build succeeded, and the latest APK was installed.
The diagnostic host entered Clipboard and hid the input view without a crash-buffer entry. Destructive
clipboard cases were verified using fake storage only; no real history entries were removed.

## Inline suggestion request lifetime

Pinned and scrollable Android autofill suggestions now inflate as children of one cancellable job
and are delivered together. New responses, editor-session starts and InputView destruction cancel
the prior request and clear its presentation. A source guard prevents service-scoped child tasks
from escaping that owner. Build and boundary tests pass; real autofill-provider response ordering
has not been exercised on the diagnostic input field.

## Removal of service-shaped command shortcuts

Service no longer implements `EngineCommandPort` or exposes its old public `submitEngineCommand`
shortcut. It publishes a narrowly typed command port backed by the dispatcher. Candidate command
extensions target that port, and `CandidateInteractionController` receives it at construction.
`PunctuationController` receives only a reader, query port, output sink and owner scope.
Tests exercise candidate commands without Android Service and ensure late/non-cooperative punctuation
queries cannot overwrite a newer map or publish after disposal. The Service retains private lifecycle
submission helpers; these are not callable by surfaces.

## Autofill presentation extraction

`input/autofill` now owns platform request styling, stable pinned/scrollable ordering and the
cancellable suggestion presenter. Service delegates request construction; the toolbar renders
`InlineSuggestionContent` and emits lifecycle calls without owning inflation jobs. The previous
style builder and inflation implementation were removed from those classes. Ordering tests and
source boundaries pass; provider-backed autofill still needs a real compatible provider scenario.

Latest command-port device smoke: Pinyin `nihao` → Return → comma produced `你好，`; candidate
selection and punctuation queries succeeded, with no crash-buffer entry.

## Clipboard retention ownership

Removed the now-unused global soft-delete purge API. Retention selects exact active, unpinned IDs
using timestamp then ID ordering; the SQL predicate also excludes pinned and pending-undo rows.
This preserves recently reused older entries and avoids creating new unowned retention tombstones.
Existing historical tombstones are not swept by this change. Retention preference updates and
delete/restore count updates use database transactions. Text edits publish preview updates only after
the database write succeeds. Clipboard listeners are snapshotted under a lock because registration
and worker-thread publication use different threads.

Policy tests cover timestamp-vs-ID order, tied timestamps and boundary limits. An in-memory SQLite
predicate check retained pinned and pending-undo rows during retention, and finalized only the
explicit marked row. No device history was used for these checks.

## Android window sizing boundary

`AndroidImeWindowLayout` owns full-window sizing and content/touch insets. `ImeInsetsPolicy`
provides tested geometry for virtual/hardware keyboards, missing keyboard views and invalid transient
dimensions. Service delegates instead of retaining a last keyboard coordinate array. Missing geometry
falls back to the current window bottom rather than stale coordinates.

Latest APK device checks: portrait English `abc` input succeeded; hiding the keyboard expanded the
diagnostic editor from [0,1831][1440,2058] to [0,2837][1440,3064]. Landscape recreation reported
READY=true and Keyboard surface at rotation=1. Portrait and the original free-rotation setting were
restored; crash buffer remained empty. Physical keyboard/floating-candidate geometry is still untested.

## Shared gesture-view lifetime

`CustomGestureView` now cancels long-press/repeat jobs and shared feedback when detached, hidden or
disabled. Cancellation emits a semantic Cancel only for an active touch, so detaching an idle delete
button cannot collapse a user's unrelated selection. Job cleanup no longer depends on feature flags
that may have changed during the touch. Double-tap timing uses monotonic uptime.

Device: after entering `abc`, injected DOWN on the Editing backspace followed immediately by hiding
the keyboard. More than the repeat delay elapsed during the UI dump; text remained `abc`. Reopening
reported READY=true, Keyboard and collapsed selection 3..3. This verifies hidden-window cancellation;
full multi-pointer and hardware-device interruption tests remain separate.

## Deletion gesture state moved out of keyboard routing

Found and removed a remaining renderer dependency: `CommonKeyActionListener` inspected
`HorizontalCandidateComponent.adapter.total` and owned a separate swipe state machine.
`DeletionGestureSession` now owns selection-vs-composition-reset decisions from `EngineState`.
Every surface submits the same move/release/cancel commands to the editing executor; the editing
panel no longer has a parallel release path. Unused direct ExtendSelection/DeleteSelection commands
and panel helper methods were removed. Other editing commands and input-session start reset transient
gesture state, without affecting cross-editor undo history.

Tests cover both horizontal directions, composing text, unknown candidate totals with loaded entries,
cancellation, zero displacement and state reset. Device verification checked actual intermediate
deletion on Editing and Emoji, then restoration to `abc` at 3..3 and `abc😀` at 5..5. In Pinyin,
`abc😀ni` followed by horizontal deletion became `abc😀`, with empty preedit and cursor 5..5;
committed text was not selected or deleted. Main-keyboard delete/undo also restored `abc`.

## Candidate paging and toolbar ownership

Candidate paging now has one owner, `CandidateInteractionController`. Its mutable flow is private;
surfaces can only observe immutable snapshots and submit candidate intents. Every page request has a
unique identity in addition to its generation and offset, so a cancelled request, late failure or
same-offset retry cannot finish or cancel a newer request. Replacing the composition invalidates the
outstanding request before publishing the new candidate state.

`HorizontalCandidateComponent` owns its query job and posted delivery callback for exactly its own
lifetime. Candidate replacement and component disposal cancel both paths. The expanded window no
longer reads the horizontal adapter or reaches into the toolbar. `KawaiiBarComponent` independently
derives its expand-button presentation from candidate snapshots and the attached semantic surface;
candidate views cannot push toolbar state-machine events.

Tests cover stale completion/failure, same-offset retry, candidate replacement, successive full
pages followed by a terminal short page, and empty/exhausted expand state. A source boundary rejects
candidate-to-toolbar mutation and peer-adapter inspection. The Android 12 diagnostic host verified
expanded `nihao`, selecting the visible `比`, and Return committing `比好`. Continuous horizontal
multi-page dragging remains part of the final device regression matrix rather than a new ownership
exception.

## Input-device and floating-anchor platform boundary

`InputDeviceManager` no longer receives `FcitxInputMethodService`. It receives one narrow callback
for the platform's input-view preference, while `InputDeviceVisibilityPolicy` owns the pure mode
decisions for input-view start, hardware keys, editor clicks and tool-type changes. Tests cover
SystemDefault delegation, InputDevice preservation/hardware hiding/direct-touch restoration and the
Disabled mode. A source boundary prevents the concrete service dependency from returning.

Cursor-anchor geometry moved from the service into `AndroidCandidateAnchorController` under the
window system. `CandidateAnchorPolicy` chooses composing-character start before insertion marker,
uses the right edge for RTL, and rejects NaN or infinite coordinates before and after the platform
matrix transform. The controller owns measurement retries, decor offsets and bottom-left fallback.
The service now forwards cursor information and invalidates geometry only at lifecycle boundaries.
Pure geometry and architecture tests pass; physical-keyboard floating placement still requires a
connected hardware keyboard for a full visual device check.

## Input-surface coroutine lifetime

Input surfaces no longer launch collectors, delayed feedback or UI delivery in the service-wide
`lifecycleScope`. `InputSurfaceLifetime` is a single-use supervisor owned by each `BaseInputView`;
detaching the view closes it and cancels all child work. Keyboard state collection, candidate action
menus, punctuation queries, toolbar timeouts, inline suggestions, horizontal and expanded paging,
editing selection, clipboard work, status collection and popup dismissal now use that lifetime.
Window-specific jobs still stop on window detach, while the surface lifetime is the final safety net
when the whole input view is replaced.

Tests prove child cancellation and idempotent close. An architecture guard rejects any future
`service.lifecycleScope` use outside the service. On device, Keyboard → Editing → Keyboard → Emoji
worked before rotation; landscape/portrait recreation closed the old view, and refocusing created a
ready Keyboard surface. The crash buffer remained empty and the original free-rotation mode was
restored.

## Attached-dialog and input-method picker ownership

The service no longer implements attached-dialog replacement, token wiring or dim-window flags.
`ImeDialogController` owns the single visible IME dialog and closes it at input-view finish and
service destruction. The controller is injected into the input surface rather than reached through
a public service helper.

Input-method picker UI now receives resolved entries, current selection and an `onSelected` intent.
It cannot read engine state or submit commands. `InputMethodPickerController` coordinates the query,
engine activation and Android IME switching at the system boundary. Architecture tests prevent the
concrete service, session and command dispatcher from returning to the dialog renderer.

## Explicit runtime ports replace service lookup

Input surfaces now receive four distinct runtime capabilities from the composition root:
`InputSessionAccess`, `InputEngineAccess`, `InputEditingAccess` and `ImePlatformAccess`.
Editor-info inspection has its own `EditorInfoAccess`. Keyboard routing, toolbar, editing,
clipboard, status, popup, picker, navigation and candidate surfaces no longer retrieve the concrete
service and then choose arbitrary members from it. The platform voice-input switch, keyboard hide
and next-IME actions are explicit intents on the platform port.

`BaseInputView` now accepts Context plus session/engine ports instead of a service object. Only the
Android composition roots (`FcitxInputMethodService`, `InputView`, `CandidatesView` and the legacy
dependency binding helper) may mention the concrete service, enforced by a source boundary test.
This makes Fcitx and Android replacement points visible in constructors and prevents a feature
system from silently expanding its authority through a service reference.
