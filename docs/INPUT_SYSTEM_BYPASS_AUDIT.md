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

## Deliberate platform boundaries (not legacy UI)

- `FcitxInputMethodService` may access `InputConnection` for engine effects and Android IME
  lifecycle bridging.
- `FcitxInputMethodService` and `input/engine` are the only locations allowed to import Fcitx core
  protocol types. This exception is guarded by a static boundary test.
- `AndroidEditingCommandExecutor` is the only command-to-editor adapter.
- `DestructiveEditingController` owns mutation/history mechanics and therefore receives an
  `InputConnection` through its host interface.
- `InputView` may bootstrap essential windows; user-triggered navigation must use `InputNavigator`.
- `AutoScaleTextView` may override `setTypeface` internally so it can invalidate measurements, but
  surfaces may not select its typeface.
- Android-owned dialogs and the developer diagnostics inspector retain native typography.

## Remaining migration work, ordered upstream to downstream

1. **Service decomposition:** `FcitxInputMethodService` still contains the Android engine bridge,
   editor-effect adapter and lifecycle orchestration in one large class. These are permitted
   platform-boundary operations, but should become separate collaborators before replacing Fcitx.
2. **Window-local state:** toolbar and window animation state is still local to its owning surface.
   Move only cross-surface state into the session reducer; ephemeral animation state should remain
   local.
3. **Candidate measurement:** expanded candidate width estimation still uses generic `Paint`
   rather than the rendered typography metrics.
4. **Non-semantic icon coverage:** the registry is now the only resolution route for shared
   controls, but several navigation arrows do not yet have custom pixel assets and intentionally
   fall back to their system drawable until those assets are designed.

Every future migration must finish with three artifacts: one owner, removal of the previous entry
point, and a boundary test. Without all three, the system is still considered transitional.
