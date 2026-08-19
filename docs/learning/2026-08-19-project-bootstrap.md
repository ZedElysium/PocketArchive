# PocketArchive 项目骨架

- Date: 2026-08-19
- Project: PocketArchive
- Phase: Project bootstrap
- Status: 已完成
- GitHub repository: 暂未创建
- Commit/PR: 暂未创建

## 1. 学习目标与职业关联

建立可构建的 Android 多模块项目骨架，理解 Gradle、Version Catalog、应用模块、功能模块和核心模块的职责。这是后续展示 Android 架构、测试和工程化能力的基础，本阶段不实现业务功能。

## 2. 前置知识

- Gradle Wrapper 用于固定构建工具版本。
- Android `applicationId` 标识安装包，`namespace` 决定生成代码的包空间。
- 模块依赖应保持单向，避免核心层反向依赖功能层。
- Java/Kotlin 字节码目标版本和 Android SDK 版本是不同概念。

## 3. 功能模块与职责

- `app`：APK 打包和未来的依赖装配入口。
- `feature-library`：未来资料库页面及其交互状态边界。
- `core-model`：不依赖 Android 的领域模型。

首轮只验证模块边界，不创建空的数据库、文件或搜索模块。

## 4. 数据流与关键边界

当前没有业务数据流。编译期依赖方向固定为 `app -> feature-library -> core-model`。`core-model` 使用 Kotlin/JVM 插件，避免领域模型意外依赖 Android API。

失败边界主要位于构建配置：插件版本不兼容、SDK 缺失、Java 工具链不匹配或依赖无法解析都会让验证失败。

## 5. 编码前实施步骤

### Step 1: 初始化独立仓库与 Gradle 根工程
- 计划：在指定子目录初始化 `main` 分支，创建 Wrapper、仓库配置、Version Catalog 和通用 Gradle 属性。
- 验收：Git 根目录正确，Gradle 能读取工程配置。
- 实施记录：已完成。`git init -b main pocketarchive` 创建了独立仓库；`settings.gradle.kts` 声明仓库和模块；`gradle/libs.versions.toml` 集中管理插件与依赖版本；`gradle.properties` 开启 AndroidX、并行构建和配置缓存；Wrapper 固定为 Gradle 8.9。父目录 Git 仓库没有被修改，也没有创建 commit 或 remote。

### Step 2: 建立三个最小模块
- 计划：创建 `app`、`feature-library`、`core-model`，配置 SDK、Java 17 和严格单向依赖。
- 验收：Gradle 项目列表包含且只包含这三个业务模块。
- 实施记录：已完成。`app/build.gradle.kts` 配置 `dev.elysium.pocketarchive`、SDK 35/26 和对 `feature-library` 的依赖；`feature-library/build.gradle.kts` 配置 Compose 并只依赖 `core-model`；`core-model/build.gradle.kts` 使用 Kotlin/JVM。三个模块统一生成 Java 17 字节码。`./gradlew --offline projects` 显示且只显示三个计划模块。

### Step 3: 配置下一阶段所需基础能力
- 计划：集中声明 Compose、Coroutines 和 Hilt 依赖；不提前加入 Room、DataStore 或 WorkManager。
- 验收：依赖可解析，模块能完成 Debug 编译。
- 实施记录：已完成。Compose 与 Coroutines 位于未来承载界面的 `feature-library`；Hilt 配置在 `app` 与功能模块，为下一阶段的装配入口和依赖注入做准备；`core-model` 不依赖 Android 或 Hilt。没有加入 Room、DataStore、WorkManager，也没有创建 Activity、Composable 或业务类型。首次离线构建发现部分 AndroidX 传递依赖未缓存，允许 Gradle 下载公开依赖后构建成功，随后再次离线构建也成功。

### Step 4: 验证并记录结果
- 计划：运行项目解析、Debug APK 构建和单元测试任务，检查 Git 状态与敏感信息。
- 验收：三个约定命令成功，文档记录真实结果且没有 commit。
- 实施记录：已完成。项目解析、Debug 构建和 Android 单元测试任务均返回 `BUILD SUCCESSFUL`。APK 位于 `app/build/outputs/apk/debug/app-debug.apk`，约 5.0 MB。测试任务为 `NO-SOURCE`，表示构建链路可执行但尚无测试用例，不能据此声称已有测试覆盖。敏感信息扫描通过，`git status` 显示所有项目文件尚未跟踪，仓库仍为 `No commits yet on main`。

## 6. 方案选择与备选方案

- 使用 Version Catalog 集中版本，而不是在每个模块散落版本号。
- 渐进式创建模块，而不是预先创建全部空模块，降低首轮配置复杂度。
- `core-model` 使用纯 Kotlin/JVM，而不是 Android Library，使领域边界更清晰。
- 使用本机已有的兼容依赖版本进行离线验证，避免把网络状态混入骨架验收。

## 7. 测试与验证计划

```bash
./gradlew --offline projects
./gradlew --offline assembleDebug
./gradlew --offline testDebugUnitTest
```

同时检查 `git status`、跟踪候选文件中的绝对用户路径、密钥和公司私有内容。验收要求全部 Gradle 命令返回成功。

## 8. 编码后模块详解

- 根工程：`settings.gradle.kts` 是模块清单和依赖仓库的单一入口；根 `build.gradle.kts` 只声明共享插件，不把插件强制应用到所有模块。
- `app`：负责 application ID、APK 打包、发布构建入口和未来依赖装配。Manifest 当前只有应用属性，没有 Launcher Activity，因此本里程碑产物可安装但没有可启动业务界面。
- `feature-library`：Android Library，预先启用 Compose，并提供下一阶段的 UI/状态边界；当前没有源码，避免用占位实现制造虚假进度。
- `core-model`：纯 Kotlin/JVM 模块。未来的文档、标签等领域类型放在这里后仍可脱离 Android 框架测试和复用。
- 构建数据流：Gradle 读取 Version Catalog，配置三个模块，先产出 `core-model` JAR，再构建 `feature-library` AAR，最后打包 `app` APK。当前没有运行时业务数据流。
- 依赖与隐私边界：项目只依赖公开 Maven 构件；没有本地绝对路径、签名文件、凭据或公司代码。`local.properties`、密钥文件和构建目录均被 `.gitignore` 排除。

## 9. 计划偏差与问题排查

- 直接在受限沙箱中运行缓存的 Gradle 失败，报错为无法加载 `libnative-platform.dylib`；检查确认系统、JDK 和动态库均为 arm64。允许 Gradle 在沙箱外访问用户级缓存后 Wrapper 生成成功，因此根因是沙箱权限，不是架构不匹配。
- 首次 `--offline assembleDebug` 因 Hilt/Compose 所需的 AndroidX 传递依赖未缓存而失败。没有删除基础能力或伪造本地依赖，而是下载公开依赖；下载后再次离线构建成功。
- 原计划没有要求业务源码，因此测试任务显示 `NO-SOURCE`。没有添加无意义的恒真测试来制造通过数量，真正的测试从下一里程碑出现状态和行为后开始。

## 10. 实际验证结果

2026-08-19 实际结果：

- `./gradlew --offline projects`：成功，识别 `app`、`core-model`、`feature-library`。
- `./gradlew assembleDebug`：成功，68 个任务执行，生成约 5.0 MB 的 Debug APK。
- `./gradlew --offline assembleDebug`：成功，验证下载完成后可以复用本地缓存构建。
- `./gradlew --offline testDebugUnitTest`：成功；两个 Android 模块均为 `NO-SOURCE`，没有测试覆盖率主张。
- 敏感信息扫描：未发现 `/Users/<username>` 形式的机器绝对路径、私钥头、常见明文密码或 API key 模式。
- Git：`main` 分支无提交、无远程，所有项目文件仍为未跟踪状态。

## 11. 知识总结

- `applicationId` 是安装身份，模块 `namespace` 是代码生成边界，两者不需要处处相同。
- Version Catalog 统一“依赖叫什么”和“使用哪个版本”，模块脚本只表达自身需要什么。
- 多模块不是目录数量竞赛；依赖方向和职责清晰比预先创建空模块更重要。
- `BUILD SUCCESSFUL` 只说明执行的任务成功；`NO-SOURCE` 不等于测试覆盖有效。
- 离线构建必须同时具备直接依赖与完整传递依赖缓存。

## 12. 自测问题与练习

自测问题：

1. `applicationId`、`namespace` 和 Kotlin package 各自解决什么问题？
2. 为什么 `core-model` 使用 Kotlin/JVM，而不是 Android Library？
3. 为什么依赖方向应是 `app -> feature-library -> core-model`？
4. Version Catalog 解决了什么维护问题？
5. `testDebugUnitTest` 成功但显示 `NO-SOURCE` 时，可以声称什么、不能声称什么？

建议练习：

- 运行 `./gradlew projects`，根据输出画出当前模块依赖方向，并说明输出本身为什么没有展示依赖边。
- 在不修改文件的前提下运行 `./gradlew :feature-library:dependencies --configuration debugRuntimeClasspath`，找到 Compose 和 Hilt 的传递依赖。

## 13. GitHub、README 与简历证据

- GitHub repository: 暂未创建
- Commit/PR: 暂未创建
- README: 已创建最小版本，后续补充架构图、截图、演示、测试与性能数据
- Technical article: 暂未创建
- Demo: 暂未创建
- Resume evidence: 当前骨架不足以形成简历成果

## 14. 尚不能声称的能力

尚不能声称已实现离线资料管理、持久化、可靠导入、进程恢复、测试体系或性能优化。

## 15. 下一里程碑

应用壳与资料库空状态：加入 Compose 主题、启动 Activity、最小 UDF 状态和相应测试。
