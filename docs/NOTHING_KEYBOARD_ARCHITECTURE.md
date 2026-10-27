# Nothing Keyboard architecture

## Boundary

Fcitx remains the input engine. It owns language engines, dictionaries, composing state and the
candidate source. The Nothing layer owns Android presentation, gestures, editing tools and visual
feedback. UI code must not recreate language-engine behavior.

## Systems

1. **Engine bridge** — converts Fcitx events and commands into stable application models.
2. **Keyboard session** — current editor, composition, candidate generation and active surface.
3. **Candidate system** — indexed candidate data, incremental loading and selection.
4. **Gesture system** — recognizes gestures separately from the actions they trigger.
5. **Editing tools** — cursor, selection, undo/redo and clipboard commands.
6. **Visual system** — tokens, typography, glyphs, color roles and motion specifications.
7. **Surfaces** — keyboard, candidate rail, expanded candidates, editing panel and pickers.

## Dependency direction

```text
Fcitx engine -> engine bridge -> session/models -> controllers -> Android views
                                      ^                |
visual system ------------------------+----------------+
```

Views may render models and emit user intents. They must not infer engine indices, own paging
state, classify fonts independently or embed feature-specific gesture state.

## Shared semantic controls

Buttons with the same meaning on different surfaces must emit the same command and use the same
gesture policy, thresholds and executor. A surface may choose placement, size and visual treatment;
it must not reimplement clear, undo, copy, paste, selection or navigation behavior.

## First-stage rules

- Measurements and typography live under `input/design`.
- Latin letters and numbers use Doto; CJK and generic symbols use the system font.
- Pixel glyph selection will be centralized in a glyph registry.
- Candidate text never shrinks to fit; candidate containers grow and scroll instead.
- New behavior requires a pure policy/model test before device-only verification.

## Migration order

1. Extract visual tokens and typography without changing behavior.
2. Introduce a stable candidate session and indexed candidate model.
3. Separate gesture recognition, action mapping and feedback.
4. Replace ad-hoc window state with a single keyboard-surface state.
5. Split `FcitxInputMethodService` only after the boundaries above are stable.
