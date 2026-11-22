# Android CI

CI 的事实来源是 [Android Community Beta](../../.github/workflows/android-beta.yml)、[扩展兼容性工作流](../../.github/workflows/android-emulator-extended.yml) 和 [发布工作流](../../.github/workflows/android-release.yml)。本文只解释稳定的职责，不记录某次运行状态。

## PR / main 分层

| 阶段 | 真实执行内容 |
| --- | --- |
| Repository static analysis | actionlint + ShellCheck、zizmor、markdownlint-cli2、Lychee、本 Fork 极少量发行边界检查 |
| JVM compile + Robolectric | Kotlin/测试源码真实编译、Robolectric JAR 由 Gradle 预取、API 33 smoke、标准 `:app:testDebugUnitTest` |
| Android Lint | 标准 `:app:lintDebug`，Error/Fatal 直接失败 |
| P3 packages | Debug APK、Diagnostic、unsigned Release 分别独立构建和报告 |
| Emulator | API 33/35 的真实 `connectedDebugAndroidTest`、UI/Room/截图测试 |
| Required CI summary | 汇总上述 workflow job 结果；不承担仓库权限/分支规则职责 |

APK、Diagnostic、Release 都必须等 JVM 与 Lint 成功后才启动；emulator 再等待全部 P3 构建成功。main 的 P3 Release job 还会验证并上传唯一的 unsigned universal APK；完整 gate 通过后，发布 workflow 直接签名这一同-run不可变 artifact，不再二次构建。

## 社区工具而非自建解析器

通用检查交给维护成熟的工具：

- **actionlint**：GitHub Actions YAML、表达式、`needs`、action input 等工作流语义，并调用 ShellCheck 检查内嵌 shell。
- **zizmor**：GitHub Actions 安全静态分析。
- **markdownlint-cli2**：Markdown 结构和格式。
- **Lychee**：本地文档链接有效性。
- **Gradle / AGP / Android Lint**：依赖解析、编译、资源与 Android 静态分析。
- **JUnit / Robolectric / AndroidJUnitRunner**：真实测试发现、执行和失败状态。
- **action-junit-report**：读取 Gradle 已生成的标准 JUnit 报告并在 Actions 中展示，不重新定义“哪些测试算通过”。

仓库不维护工作流 YAML 文本捕捉测试、测试源文件库存脚本、自定义 JUnit XML 判定器或 Maven HTTP 探针。

## Robolectric 范围

PR lane 设置 `robolectric.enabledSdks=33,35,36` 并预取相应 Android JAR，减少普通 PR 的下载成本。显式要求旧 SDK 的测试不会被宣称为 PR 已覆盖。

每周 legacy lane 设置 `LUMI_ROBOLECTRIC_LEGACY=1`，额外准备 API 26/28/29/30/31/32/34，随后运行同一个标准 `:app:testDebugUnitTest`。因此两条 lane 的区别只有 SDK 解析范围，没有自定义 test shard 或另一套测试发现逻辑。

## Emulator

PR 阻塞矩阵使用 API 33/35。脚本只负责选择明确的 instrumentation smoke class、保存 logcat 和把截图从 emulator 拉回 artifact；测试成败直接采用 `:app:connectedDebugAndroidTest` / AndroidJUnitRunner 退出码，不再由 Python 二次解释 XML。

API 26/33/36 的扩展 emulator、完整历史 Robolectric 和可选 Macrobenchmark 位于定期/手动 workflow。

## 失败排查

1. 绑定当前 head SHA，先看最早失败 job。
2. 静态分析失败就修 workflow/docs，不启动 Android 重任务。
3. 编译失败先修编译；Robolectric/JUnit 失败读取真实测试名、stack trace 和原生 XML/HTML 报告。
4. 网络/CDN 失败与业务断言分开处理；不要重试断言来制造绿色结果。
5. 只有确认是 GitHub/网络瞬时故障时，才重跑失败 job。

发布权限和签名边界见 [发布指南](release.md)。
