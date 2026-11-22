# 架构总览

本页描述 Lumi Community 长期维护的代码边界。具体实现细节仍以代码和测试为准；一次 PR 的调查结论、CI 日志和临时方案不写入这里。

## 模块与 source set

仓库保留上游的 Android 模块结构，应用主体位于 `app`，基准测试位于 `benchmark`，第三方改编代码位于 `third_party`。

应用构建固定使用公开 OSS 代码：

- `app/src/main/`：共享生产代码、资源和业务能力。
- `app/src/oss/`：Community Fork 专属的能力适配和实现，不依赖私有 Premium 源码。
- `app/src/debug/`：仅开发/调试能力。
- `app/src/nonDebug/`：Release、Diagnostic、Benchmark 共用的非调试实现。
- `app/src/test/`：JVM/JUnit/Robolectric 测试。
- `app/src/androidTest/`：需要真实 Android runtime 的 instrumentation/UI/Room 集成测试。

`edition/` 是 OSS 与上游版本差异的**薄适配层**。它可以决定某项能力是否存在、把共享 UI 接到 OSS 实现，但不应成为第二个 UI、Repository 或数据模型根目录。

## 代码分层

代码继续遵循现有 package-by-feature 与分层组合：

- `ui/<feature>/`：Compose/Android UI、屏幕状态和用户交互。
- `data/local`、`data/remote`：Room、网络与持久化细节。
- `data/repository`：跨数据源协调和事务边界。
- `domain`：跨 UI/数据层稳定使用的模型与接口。
- `reading/journal` 等业务包：可独立测试的领域逻辑。
- `edition`：仅做 Community/edition wiring。

社区功能必须优先复用已有数据库、Repository、导航和 UI 组件。例如标签管理继续使用现有 tag/annotation 数据模型与 Room；阅读日志在已有 AppDatabase 中持久化；标注导出复用现有导出 builder，而不是建立平行存储。

## Community UI 放置

即使实现文件只在 `src/oss` 中编译，也应放回对应 feature package：

- 标签管理：`ui/tags/`
- 阅读日志界面：`ui/statistics/journal/`
- 全部标注导出：`ui/bookshelf/`

这样 source set 表达“哪个版本拥有实现”，package 表达“代码属于哪个功能”。

## 测试策略

测试验证行为，而不是验证源码中是否出现某段字符串。

- 纯业务逻辑：JUnit。
- Android framework / Compose host 行为：Robolectric。
- Room/Repository：尽量使用真实 in-memory Room 和真实 Repository。
- 真实 Android runtime、系统 UI、截图和 migration 集成：instrumentation/emulator。
- 工作流语义：actionlint、ShellCheck、zizmor。
- Android 资源/API/错误：Android Lint、AAPT/AGP。
- Markdown 与链接：markdownlint-cli2、Lychee。

PR JVM lane 面向 API 33/35/36；历史显式 SDK 场景由每周 legacy lane 执行。两条 lane 都运行标准 `:app:testDebugUnitTest`，不维护自定义测试发现器或 JUnit XML 解析器。

## CI 与发布边界

PR/main CI 按成本由低到高执行：仓库静态分析 → 编译/Robolectric/JVM → Android Lint → APK/Diagnostic/Release 构建 → API 33/35 emulator。

发布由成功的 `main` CI 触发，并分成两个权限域：

1. **build**：只读权限，无签名 Secret，构建并验证 unsigned Release APK，上传短期 artifact；
2. **publish**：下载该 artifact，才获得签名 Secret 和 `contents: write`，完成 zipalign、签名、校验和 GitHub Pre-release。

发布 job 不重新运行 Gradle，从而避免“测试过的源码”和“最终签名的二次构建”发生漂移。

## 文档职责

- `README*.md`：面向用户的功能、下载和隐私。
- `COMMUNITY.md`：Fork 身份、功能/发行边界。
- `CONTRIBUTING.md`：贡献流程。
- `SECURITY.md`：安全报告和密钥边界。
- `AGENTS.md`：自动化 Agent 的仓库操作约束。
- `docs/architecture/`：长期架构与边界。
- `docs/development/`：构建、CI、发布等操作手册。
- `docs/reference/`：稳定的实现参考。

历史方案、handoff、某次 CI 状态和临时研究留在 Git 历史、PR、Issue 或 Actions，不另建平行“权威文档”。
