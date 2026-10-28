# 输入内核评估

## 我们真正需要的边界

Nothing Keyboard 的 Android IME 生命周期、编辑事务、手势、导航、候选界面、
字体、图形、动画和设置都应由自己的系统负责。输入内核只提供以下能力：

1. 接收按键或文本事件；
2. 返回预编辑文本和候选列表；
3. 选择候选、翻页、重置会话；
4. 管理系统词典和用户学习数据；
5. 不直接持有 Android View、InputConnection 或界面导航。

所有内核必须通过 `input/engine` 的端口接入，禁止界面直接依赖具体内核类型。

## 候选方案

### 1. libime（当前首选）

- 仓库：https://github.com/fcitx/libime
- 定位：通用输入法实现库，不是完整 Android 键盘。
- 优点：拼音、双拼、表格输入与词典格式成熟；边界比完整 Fcitx 小；当前项目已经
  能稳定构建它，迁移可以渐进完成。
- 代价：C++ API 仍需要我们编写 JNI 和会话适配层；部分中文行为目前经由
  `fcitx5-chinese-addons` 组织，直接使用前要确认哪些能力应下沉到适配器。
- 结论：最符合“自己的输入法壳 + 独立中文内核”。

### 2. librime

- 仓库：https://github.com/rime/librime
- Android 参考：https://github.com/osfans/trime
- 定位：跨平台、可配置、支持大量输入方案的完整输入引擎。
- 优点：方案和词库生态强，拼音、五笔及自定义方案扩展能力出色；Trime 提供了成熟
  的 Android/JNI 集成参考。
- 代价：部署、YAML 配置、用户数据同步和维护线程形成另一套较重的运行体系；若只要
  简体拼音，会引入我们暂时不需要的复杂度。
- 结论：适合作为未来可选后端，不建议现在替换为唯一内核。

### 3. AOSP PinyinIME

- 仓库：https://android.googlesource.com/platform/packages/inputmethods/PinyinIME
- 优点：代码规模相对有限，Android 集成直接。
- 代价：主要实现和提交停留在早期 Android 时代，词频、学习能力和现代维护均不足。
- 结论：只适合阅读算法与接口，不适合成为新产品内核。

### 4. OpenWnn

- 仓库：https://android.googlesource.com/platform/packages/inputmethods/OpenWnn/
- 定位偏日文输入，主线同样长期缺乏现代演进。
- 结论：不符合本项目的简体中文优先目标。

### 5. HeliBoard / OpenBoard

- 仓库：https://github.com/HeliBorg/HeliBoard
- 它们是值得参考的 Android 键盘壳、英文词典和手势实现，但不是成熟的中文拼音内核。
- 结论：可用于研究性能与 Android 兼容策略，不作为中文内核替换项。

## 决策

短期继续把 Fcitx 限制在唯一引擎边界内，完成旧壳清理；中期建立直接调用 libime 的
实验适配器，并让它与现有 Fcitx 适配器通过同一组契约测试。只有当候选、用户学习、
模糊音、撤回组合和词典迁移测试全部通过后，才移除完整 Fcitx 运行时。

这条路线不要求再次重写界面和手势系统，也不会把 librime 或未来自研内核排除在外。
