# 可启动应用壳与资料库空状态

- Date: 2026-08-19
- Project: PocketArchive
- Phase: App shell and empty library
- Status: 已完成
- GitHub repository: 暂未创建
- Commit/PR: 暂未创建

## 1. 学习目标与职业关联

把仅能打包的工程骨架变成真正具有 Launcher 入口的 Compose 应用，并在不引入业务状态的前提下完成资料库空状态。这个里程碑训练 Android 启动链、应用级依赖装配、主题、Compose 模块边界和 UI 测试，是后续 UDF 与导入闭环的可运行基础。

## 2. 前置知识

- `Application` 在应用进程创建时初始化，Hilt 使用它建立应用级组件。
- `Activity` 是本阶段的 Android 启动与 Compose 承载入口。
- `setContent` 把 Activity 生命周期与 Compose 组合树连接起来。
- 深浅主题由系统配置驱动，Compose 通过 `isSystemInDarkTheme()`选择颜色方案。
- 静态展示层不需要 ViewModel；过早加入状态对象只会增加无意义抽象。

## 3. 功能模块与职责

- `app`：声明 `PocketArchiveApplication`、Launcher `MainActivity` 和全局 `PocketArchiveTheme`。
- `feature-library`：声明可复用的 `LibraryScreen` 和静态空状态，不依赖 `app`。
- `core-model`：本阶段无新职责，保持不变。

## 4. 数据流与关键边界

启动流为：Android 系统 → `PocketArchiveApplication` → `MainActivity` → `PocketArchiveTheme` → `LibraryScreen`。

本阶段没有业务状态流。`LibraryScreen` 通过参数获得 `importEnabled` 和 `onImportClick`，因此功能模块不知道上层如何处理操作。导入按钮保持禁用并明确说明下一里程碑接入，避免无响应按钮误导用户。

主题随系统配置重新组合；Activity 重建后页面由静态参数重新生成。没有持久数据、后台线程或隐私数据进入应用。

## 5. 编码前实施步骤

### Step 1: 建立应用启动链
- 计划：创建 Hilt Application、Launcher Activity，并更新 Manifest。
- 验收：合并后的 Manifest 包含 MAIN/LAUNCHER Activity，Hilt 编译成功。
- 实施记录：已完成。`PocketArchiveApplication` 使用 `@HiltAndroidApp` 建立应用级组件；`MainActivity` 使用 `@AndroidEntryPoint` 并通过 `setContent` 创建 Compose 组合树；Manifest 声明 Application、导出的 MAIN/LAUNCHER Activity、应用图标以及显式备份排除规则。APK 的 badging 验证 `dev.elysium.pocketarchive.MainActivity` 是启动入口。

### Step 2: 建立全局 Compose 主题
- 计划：提供固定的浅色与深色配色，并让系统主题决定默认方案。
- 验收：两套配色都能编译，Preview 可选择不同 `uiMode`。
- 实施记录：已完成。`PocketArchiveTheme` 定义浅色与深色 `ColorScheme`，默认读取 `isSystemInDarkTheme()`；Activity 只选择全局主题，不把应用主题实现泄漏给功能模块。XML 启动主题也分别提供普通与夜间资源，减少 Compose 首帧前的明暗不一致。

### Step 3: 实现静态资料库空状态
- 计划：在 `feature-library` 中添加标题、空状态说明、禁用的导入按钮和阶段说明。
- 验收：页面不依赖 `app`，按钮不会伪装成已实现功能，窄屏与较宽布局均有宽度约束。
- 实施记录：已完成。`LibraryScreen` 使用 Material 3 展示“资料库”、空状态说明和导入入口；内容最大宽度限制为 420dp，并处理系统栏内边距。`importEnabled` 与 `onImportClick` 由上层传入，功能模块不依赖 Activity。当前 Activity 明确传入 `false`，按钮禁用并展示下一里程碑说明。两个 Preview 覆盖浅色和深色模式。

### Step 4: 添加并验证 UI 测试
- 计划：添加 Compose UI 测试，验证核心文本显示及导入按钮禁用状态；构建应用和测试 APK。
- 验收：Debug 构建、单元测试任务、AndroidTest 编译均成功；没有设备时明确记录未执行仪器测试。
- 实施记录：已完成。`LibraryScreenTest.emptyLibrary_showsHonestImportState` 检查标题、空状态、禁用按钮和阶段说明。应用 APK 与 AndroidTest APK 均成功构建；测试随后在 Pixel 7 Pro AVD、Android 14（API 34）执行，1/1 通过。现有 JVM 单元测试任务仍为 `NO-SOURCE`。

## 6. 方案选择与备选方案

- 主题放在 `app`，因为它是全应用视觉策略；功能页面只使用 `MaterialTheme`，避免反向依赖。
- 暂不加入 Navigation，单页面应用壳不需要导航框架。
- 暂不加入 ViewModel/UDF，静态页面没有需要管理的业务状态；下一里程碑再展示状态变化的真实价值。
- 不让导入按钮执行空 lambda 的可点击行为，而是禁用并说明阶段边界。

## 7. 测试与验证计划

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew :feature-library:assembleDebugAndroidTest
```

检查合并 Manifest 中的启动入口，并确认应用 APK 与测试 APK 均生成。有设备时再运行 `connectedDebugAndroidTest`；模拟器或未执行的测试不能包装成真机证据。

## 8. 编码后模块详解

- `PocketArchiveApplication`：仅承担 Hilt 进程级入口，不放业务初始化，避免 Application 变成全局杂物箱。
- `MainActivity`：Android 与 Compose 的边界。调用 `enableEdgeToEdge()`，应用全局主题，然后装配 `LibraryScreen`；没有资料库业务判断。
- `PocketArchiveTheme`：根据系统配置选择颜色方案。它位于 `app`，因为全应用品牌主题属于应用级策略。
- `LibraryScreen`：无状态展示函数。`importEnabled` 是当前能力状态，`onImportClick` 是对上层公开的操作出口；本阶段没有 Action、ViewModel 或 StateFlow。
- 启动数据流：系统创建 Application 和 Activity，Activity 创建主题与页面。配置变化导致 Activity/Compose 重建，静态参数重新渲染；没有跨线程工作或持久状态。
- 隐私默认值：`backup_rules.xml` 与 `data_extraction_rules.xml` 排除 root、file、database、shared preferences 和 external 域，避免未来用户资料默认进入系统备份或设备迁移。
- 测试边界：Compose 测试通过语义树查找用户可见文本并验证禁用状态，而不是依赖像素位置。

## 9. 计划偏差与问题排查

- 首次构建使用 Kapt 时，Kotlin 2.0.21 被提示回退到 1.9 语言级。检查到本机已有匹配的 KSP `2.0.21-1.0.28`，因此把 Hilt 注解处理从 Kapt 迁移到 KSP；重新构建成功，回退警告消失。
- 首次离线 Lint 因 AndroidTest lint 模型缺少传递依赖而停止，这不是代码检查失败。下载公开依赖后 Lint 完成。
- Lint 最初指出缺少应用图标和 Android 12+ 数据提取规则。补充矢量图标、旧版完整备份规则和新版云备份/设备迁移排除规则后，两项警告消失。
- Lint 仍报告 17 条依赖可升级提醒。当前 Android Studio/本机缓存对应 AGP 8.7.3 工具链；升级到 AGP 9.3.1 会成为独立迁移任务，本里程碑不为追新版本扩大范围。
- 模拟器首次显示全黑时，应用进程仍存在且没有崩溃。通过 `dumpsys power/display/window policy` 确认 `Wakefulness=Asleep`、`ScreenState=OFF`；唤醒屏幕并重新启动 Activity 后页面正常。这是模拟器休眠，不是 Compose 渲染缺陷。
- 首次离线运行连接设备测试时缺少 Unified Test Platform 插件缓存，下载公开测试运行器依赖后重跑成功；第一次属于工具依赖解析失败，不计为测试用例失败。

## 10. 实际验证结果

2026-08-19 实际结果：

- `./gradlew --offline assembleDebug testDebugUnitTest :feature-library:assembleDebugAndroidTest lintDebug`：成功，135 个任务中 31 个执行、104 个已是最新状态。
- 应用 APK：`app/build/outputs/apk/debug/app-debug.apk`，10,135,398 字节。
- 测试 APK：`feature-library/build/outputs/apk/androidTest/debug/feature-library-debug-androidTest.apk`，10,525,115 字节。
- APK badging：包名 `dev.elysium.pocketarchive`，版本 `0.0.1`，Launcher 为 `MainActivity`。
- Lint：应用模块 0 errors、17 个依赖版本提醒；功能模块无问题。
- `testDebugUnitTest`：成功但仍为 `NO-SOURCE`，不能声称 JVM 测试覆盖。
- Compose UI test：`Pixel_7_Pro(AVD)`、Android 14/API 34 上执行 1 项，1/1 通过；这是模拟器功能验证，不是真机证据。
- Git：没有 commit、remote、push、tag 或 release。

## 11. 知识总结

- Launcher Activity 必须同时声明 MAIN action、LAUNCHER category，并在含 intent filter 时显式设置 `exported`。
- Activity 是 Compose 的宿主，不应持有功能页面的业务实现。
- 无状态 Composable 通过值和回调与上层协作，这为下一步 UDF 保留清晰接口。
- 深色模式包含 Compose 运行时主题和首帧前 Android XML 主题两个层面。
- KSP 与 Kapt 都可运行注解处理；使用与 Kotlin 版本匹配的 KSP 可避免旧语言级回退。
- 测试 APK 构建成功只证明测试可编译，不等于测试已经在设备执行。

## 12. 自测问题与练习

自测问题：

1. `Application`、`Activity` 和 Composable 在启动过程中分别负责什么？
2. 为什么全局主题放在 `app`，而 `LibraryScreen` 放在功能模块？
3. `LibraryScreen` 为什么同时接收 `importEnabled` 和 `onImportClick`？
4. 为什么本阶段不需要 ViewModel 或 StateFlow？
5. Preview、测试 APK 编译、模拟器仪器测试和真机验证分别能证明什么？
6. KSP 相比本次出现警告的 Kapt 路径解决了什么问题？

建议练习：

- 在 Preview 中分别传入 `importEnabled = true/false`，观察语义和视觉变化，但不要提交这个实验改动。
- 阅读合并后的 Manifest，找出哪些 Activity 来自应用，哪些由测试或依赖合并进来。

## 13. GitHub、README 与简历证据

- GitHub repository: 暂未创建
- Commit/PR: 暂未创建
- README: 已更新当前可启动应用壳、功能边界和验证命令
- Technical article: 暂未创建
- Demo: 暂未创建
- Resume evidence: 单一静态空状态不足以作为简历成果

## 14. 尚不能声称的能力

尚不能声称已实现 UDF、文件导入、状态恢复、持久化、完整自适应布局或真机 UI 测试。

## 15. 下一里程碑

为资料库引入最小 UDF：`LibraryUiState`、`LibraryAction`、`LibraryViewModel` 和真实状态转换单元测试。
