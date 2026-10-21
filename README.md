# PocketArchive

PocketArchive 是一款离线优先的 Android 资料整理应用，用于管理 PDF、图片，并为后续的本地检索能力提供基础。项目按照真实 Android 工程流程逐步开发；当前阶段不包含 AI、账号系统或云同步。

> PocketArchive is an offline-first Android archive organizer built incrementally as an Android engineering portfolio project. AI, accounts, and cloud sync are currently out of scope.

## 当前进度 / Current status

应用目前已经具备：

- 可运行的 Jetpack Compose 应用壳层及浅色、深色主题。
- 基于 State、Action 和一次性 Effect 的资料库 UDF 数据流。
- 使用 Activity Result API 打开系统文档选择器，可选择 PDF 或图片。
- 与 Android 平台无关的资料领域模型，包括资料 ID、类型和基础元数据。
- 独立的 Android 文件边界，可读取 `content://` 文档的名称、MIME 和可选大小。
- 可测试的导入事务编排，明确暂存、提交、失败清理和取消清理规则。
- 应用私有文件存储适配器，可将 ContentResolver 输入流暂存并原子提交到 `noBackupFilesDir`。
- 由应用层 Hilt 依赖图组装文件读取、私有存储、ID、时钟和导入用例，并将导入边界注入资料库 ViewModel。
- ViewModel 在用户选中文档后启动导入，并向 Compose 暴露导入中、成功和失败状态。
- 独立的 Room 数据库模块已定义资料表结构、插入与查询契约。

选择 PDF 或图片后，文件会复制到应用私有存储，并在页面显示本次结果。数据库持久化和资料列表尚未接入，因此导入结果暂时不会跨进程恢复或显示为列表项。

## 模块结构 / Modules

- `app`：Android 应用入口、依赖组装和系统能力接入。
- `core-database`：Room 资料实体、DAO 契约和数据库映射。
- `core-files`：系统文档 URI、ContentResolver 和文件所有权边界。
- `feature-library`：资料库页面、UDF 状态、用户 Action 和一次性 Effect。
- `core-model`：供其他模块共享的纯 Kotlin 领域模型。

当前主要依赖方向：`app -> feature-library -> core-files -> core-model`，`core-database -> core-model`。`app` 负责组装 Android 生产实现。

`core-model` 不依赖 Android 或数据库框架，确保领域规则可以通过纯 JVM 测试独立验证。

## 构建与验证 / Build and verification

项目使用 Gradle Wrapper。在项目根目录运行：

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew :feature-library:assembleDebugAndroidTest
./gradlew lintDebug
```

## 当前边界 / Current scope

- 离线优先，不要求登录或联网。
- 暂未实现数据库持久化、资料列表和搜索。
- 暂不加入 AI、云同步和账号体系。
