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
| 1. Android lifecycle | IME lifecycle/presentation/window controllers; Service composition root | Device and multi-window smoke matrix |
| 2. Session state | InputSessionStore → InputSessionReader; narrow sinks | Reader-port device smoke passed; broader editor matrix remains |
| 3. Engine integration | EngineCommandPort / InputEngineQueries / EngineEffect | Replacement-engine integration is not yet tested |
| 4. Editor capabilities | EditorCapability + AndroidEditorPolicy + native adapter | Vendor editor smoke matrix |
| 5. Composition and cursor | EditorController | Complex external editor/filter cases |
| 6. Key commands and hardware | Semantic keys + AndroidKeyEventController | Physical keyboard and stale native replies |
| 7. Input modes | InputModeStore | Cross-session remembered-mode matrix |
| 8. Candidate data and selection | Candidate models/reducers/interaction controller | Large-list and mixed-font profiling |
| 9. Destructive editing | EditingCommandPort + DestructiveEditingController | Custom editors rejecting edits |
| 10. Clipboard | ClipboardSystem + platform source + repository + interaction ports | Device matrix for sensitive and vendor ClipData |
| 11. Navigation and windows | InputNavigator + InputWindowManager | Disposal now explicit; stress rebuilds |
| 12. Gestures | KeyboardGesturePolicy + shared bindings | Cancel verified on device; multi-touch matrix remains |
| 13. Typography | NothingTypography + shared measurer/renderer | Font scale and fallback matrix |
| 14. Functional glyphs | NothingGlyphRegistry | Missing custom assets remain explicit fallbacks |
| 15. Layout/theme | NothingDesignTokens + semantic surface/color styles | Device/font-scale visual matrix |
| 16. Motion/feedback | InputMotionSystem + InputMotionPolicy + KeyboardFeedback | Device refresh-rate feel tuning |
| 17. Popups | Popup contract + PopupComponent + PopupGeometry | Multi-window and unusual inset device matrix |
| 18. Settings, persistence and diagnostics | InputSettings boundary + repositories + atomic diagnostic reducer | Migration/device restore smoke matrix |

### 当前完整架构树与完成状态

标记含义：`✅ 已完成`表示该责任已有唯一所有者，普通生产路径已统一使用新接口；
`🟡 进行中`表示边界已经建立，但仍有迁移、平台兼容或实机矩阵需要收尾；
`⬜ 未开始`表示尚未进入专项重构。这里的“完成”指架构责任完成，不代表所有 Android
版本、第三方输入框和极端设备条件均已验证。

```text
Nothing Keyboard（Fcitx 只保留最底层输入引擎边界）
│
├── 平台与引擎层
│   │
│   ├── 01. ✅ Android 生命周期系统
│   │   ├── FcitxInputMethodService：只保留 Android 回调与组合根
│   │   ├── ImeInputLifecycleCoordinator：输入开始、视图、选区与结束顺序
│   │   ├── InputViewPresentationController：可见界面开始/清理事务
│   │   ├── InputDevicePresentationController：软/硬键盘模式切换
│   │   ├── AndroidImeWindowLayout / CandidateAnchor / DialogController
│   │   └── Service 基类、Window、引擎与界面信号有固定创建/销毁顺序
│   │
│   ├── 02. ✅ 输入会话与状态系统
│   │   ├── InputSessionStore：唯一可变状态所有者
│   │   ├── InputSessionReader：界面只读入口
│   │   ├── Engine / Surface / Punctuation 独立窄写入端口
│   │   └── 不再允许旧广播或普通界面直接修改会话
│   │
│   ├── 03. ✅ Fcitx 引擎集成系统
│   │   ├── EngineCommandPort：语义命令边界
│   │   ├── InputEngineQueries：只读查询边界
│   │   ├── EngineEffect：引擎输出模型
│   │   ├── FcitxEngineCommandAdapter：唯一协议适配器
│   │   └── SerialCommandQueue：保证命令顺序
│   │
│   └── 04. ✅ 编辑器能力与平台兼容系统
│       ├── EditorCapability：输入框能力模型
│       ├── AndroidEditorPolicy：平台特殊行为唯一判断入口
│       ├── AndroidEditorCapabilities：InputType → 语义能力
│       ├── AndroidInputSessionAdapter：EditorInfo → 会话快照
│       ├── 密码、单行、动作键、选区能力判断
│       └── 密码输入统一启用 Password / Private / NoSuggestions
│
├── 输入核心层
│   │
│   ├── 05. ✅ 组合文本、光标与编辑器状态系统
│   │   ├── EditorController：唯一状态协调者
│   │   ├── 拼音预编辑、候选提交与原始文本提交
│   │   ├── 光标、选区与 composing range 对齐
│   │   └── 引擎效果与 Android InputConnection 的隔离
│   │
│   ├── 06. ✅ 按键命令与硬件键系统
│   │   ├── EngineKey / EngineKeyModifier：语义按键
│   │   ├── 软件键统一动作入口
│   │   ├── AndroidKeyEventController：硬件键入口
│   │   └── Return、删除、符号和语言键不再直接调用旧 API
│   │
│   ├── 07. ✅ 输入模式系统
│   │   ├── InputModeStore：唯一模式状态所有者
│   │   ├── 拼音 / 英文 / 符号 / Picker 模式
│   │   └── 跨界面模式切换通过统一命令完成
│   │
│   └── 08. ✅ 候选数据与选择系统
│       ├── 候选快照与稳定索引模型
│       ├── 横向连续滚动与增量加载
│       ├── 展开候选、浮动候选与候选操作
│       ├── CandidateInteractionController：唯一交互入口
│       └── 候选测量与渲染共享相同文本模型
│
├── 编辑与交互层
│   │
│   ├── 09. ✅ 删除、选区与撤回系统
│   │   ├── EditingCommandPort：所有界面统一命令入口
│   │   ├── DestructiveEditingController：唯一执行者
│   │   ├── 普通删除、选区删除、范围滑动删除、清空全部
│   │   ├── DestructiveEditHistory：键盘自己的撤回历史
│   │   └── 恢复后强制折叠光标，不恢复旧选区
│   │
│   ├── 10. ✅ 剪贴板系统
│   │   ├── ClipboardInteractionController：界面交互与撤回策略
│   │   ├── ClipboardHistory / ClipboardMutations：输入界面独立端口
│   │   ├── PlatformClipboardSource：Android 监听与重复事件过滤
│   │   ├── ClipboardRepository：Room 查询、事务与保留策略执行
│   │   ├── ClipboardSystem：设置监听、采集编排和事件发布
│   │   └── 平台、设置、存储三类依赖由架构测试禁止交叉泄漏
│   │
│   ├── 11. ✅ 界面导航与窗口系统
│   │   ├── InputNavigator：用户导航命令唯一入口
│   │   ├── InputWindowManager：窗口创建、切换与销毁
│   │   ├── 键盘 / 编辑 / 剪贴板 / Picker / 展开候选
│   │   └── 界面不能再直接构造并挂载其他界面
│   │
│   └── 12. ✅ 手势系统
│       ├── KeyboardGesturePolicy：阈值与动作判定
│       ├── BackspaceGestureController：全界面共享删除手势
│       ├── 上滑清空、下滑撤回、横滑范围删除
│       ├── 空格滑动与快捷工具手势
│       └── 手势识别、语义动作和视觉反馈彼此分离
│
├── 视觉系统层
│   │
│   ├── 13. ✅ 字体系统
│   │   ├── NothingTextRole / NothingTextStyle
│   │   ├── Doto：英文、数字和控制标签
│   │   ├── 系统字体：中文、通用符号与用户内容
│   │   ├── 同一文本中的混合字体 Span
│   │   └── 测量和绘制保持完全相同的字体规则
│   │
│   ├── 14. ✅ 功能图标系统
│   │   ├── NothingGlyphRegistry：唯一图标映射表
│   │   ├── 删除、回车、发送、Shift、语言和工具图标
│   │   ├── ReturnKeyGlyphPolicy：按编辑器语义选择图标
│   │   └── 未定制图标明确回退原资源，不在界面私自替换
│   │
│   ├── 15. ✅ 布局、主题与颜色系统
│   │   ├── NothingDesignTokens：基础设计令牌
│   │   ├── InputSurfaceStyle：各类界面的完整语义几何
│   │   ├── InputColorScheme：输入界面专用颜色角色
│   │   ├── Theme → InputColorScheme：旧主题唯一兼容入口
│   │   ├── 主键盘、候选、编辑、剪贴板、Picker、弹窗已接入
│   │   ├── 弹窗尺寸同样由 PopupSurfaceStyle 统一提供
│   │   └── 架构测试禁止普通渲染器重新读取旧 Theme 颜色字段
│   │
│   ├── 16. ✅ 动画与反馈系统
│   │   ├── InputMotionRole：语义动效类型
│   │   ├── InputMotionPolicy：统一时长与禁用策略
│   │   ├── InputMotionSystem：动画设置唯一运行时入口
│   │   ├── KeyboardFeedback：点阵反馈与触觉反馈入口
│   │   └── 所有界面动画、滚动和延时反馈均读取同一套开关与时长
│   │
│   └── 17. ✅ 弹窗系统
│       ├── PopupAction / PopupResult：弹窗语义模型
│       ├── PopupComponent：弹窗生命周期所有者
│       ├── PopupGeometry：纯函数计算并限制四个屏幕边缘
│       ├── 长按字符、按键菜单和候选菜单统一使用同一放置规则
│       └── 尺寸变化时销毁旧弹窗，禁止继续使用失效坐标
│
└── 应用支持层
    │
    └── 18. ✅ 设置、持久化与诊断系统
        ├── InputSettings：输入系统读取持久设置的唯一入口
        ├── AppPrefs：限制在设置适配边界内
        ├── Repository / RetentionPolicy：存储事务与保留策略
        ├── InputDiagnosticEvent：明确的诊断事件模型
        ├── reduceInputDiagnostics：纯 reducer 与原子 StateFlow 更新
        └── InputCoreDiagnosticsActivity + 架构/策略回归测试
```

当前共计：`18 个已完成`、`0 个进行中`、`0 个未开始`。这里的完成表示每项责任已有
唯一所有者、明确模型或事件接口，并由架构测试阻止旧路径重新出现；不同厂商设备、
第三方编辑器、横竖屏和多窗口仍属于持续实机验证矩阵，不与架构完成状态混为一谈。

### Visual-system upgrade

The visual upgrade starts from semantic resolution, not from changing individual pixels. A surface
declares that it is a keyboard, candidate row or clipboard entry and consumes the corresponding
`InputSurfaceStyle`. Only `InputSurfaceStylePolicy` may assemble theme identity, design tokens and
legacy-theme fallbacks into complete component geometry. Renderers must not repeat that selection
with local `isNothingStyle` branches for margins, radii, sizing or marker presence.

Migration proceeds from the highest-frequency shared foundations outward:

1. keyboard, candidate and clipboard geometry;
2. toolbar, editing and picker surfaces;
3. semantic color roles and state layers;
4. motion choreography after static geometry is stable.

The first slice covers keyboard height/radius/space treatment, candidate marker geometry and
clipboard grid/entry geometry. The second adds toolbar controls, editing-button construction and
picker geometry. Typography and glyph ownership remain separate systems and are not folded into
this policy.

Motion is selected by semantic `InputMotionRole` through `InputMotionPolicy`. A renderer may choose
the Android animation primitive and targets, but it must not choose an unrelated raw duration.
This preserves one timing vocabulary across candidates, toolbar transitions, caps feedback and
window changes while keeping the global disable-animation preference authoritative.

Input surfaces consume `InputColorScheme` roles rather than the persisted `Theme` schema directly.
`Theme.toInputColorScheme()` is the compatibility boundary for old and custom themes. This keeps
container/content, alternate, accent, pressed, candidate and active-state meanings stable even if
theme storage or the Nothing palette changes later.

The palette migration now covers the core key renderer, candidate rows and tabs, clipboard and
editing controls, picker navigation, preedit, popup key previews and status entries. Direct Theme
color access remains acceptable only at platform-owned surfaces (inline autofill templates,
navigation-bar coloring and Android dialogs) or inside the Theme-to-palette adapter itself.

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
