# Android CI

CI 的事实来源是 [Android Community Beta](../../.github/workflows/android-beta.yml)、[扩展兼容性工作流](../../.github/workflows/android-emulator-extended.yml) 和 [发布工作流](../../.github/workflows/android-release.yml)。本文只解释稳定的职责，不记录某次运行状态。

## PR / main 分层

| 阶段 | 真实执行内容 |
| --- | --- |
| Repository static analysis | actionlint + ShellCheck、zizmor、markdownlint-cli2、Lychee、本 Fork 极少量发行边界检查 |
| JVM compile + Robolectric | Kotlin/JVM/instrumentation 测试源码真实编译、Robolectric JAR 由 Gradle 预取、API 33 smoke、标准 `:app:testDebugUnitTest` |
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
- **nick-fields/retry**：只包住 Gradle/Robolectric 依赖准备阶段的瞬时网络失败；真实 Kotlin 编译、Robolectric smoke、JUnit 断言和 Android instrumentation 从不重试。

仓库不维护工作流 YAML 文本捕捉测试、测试源文件库存脚本、自定义 JUnit XML 判定器或 Maven HTTP 探针。

## Robolectric 范围

PR lane 设置 `robolectric.enabledSdks=33,35,36` 并预取相应 Android JAR，减少普通 PR 的下载成本。显式要求旧 SDK 的测试不会被宣称为 PR 已覆盖。

每周 legacy lane 设置 `LUMI_ROBOLECTRIC_LEGACY=1`，额外准备 API 26/28/29/30/31/32/34，随后运行同一个标准 `:app:testDebugUnitTest`。因此两条 lane 的区别只有 SDK 解析范围，没有自定义 test shard 或另一套测试发现逻辑。

## Emulator

PR 阻塞矩阵使用 API 33/35。GitHub Actions Runner 是本地没有 Android SDK 或物理设备时的正式远程执行环境。脚本选择明确的 instrumentation smoke class，保存运行期全量 logcat、crash buffer、环境、最终窗口层级及实际交互截图；测试成败直接采用 `:app:connectedDebugAndroidTest` / AndroidJUnitRunner 退出码，不再由 Python 二次解释 XML。

标签回归覆盖真实分类 Activity → 标签管理 Activity、分类书籍页长按 → 标签选择器、保存和重新打开，以及真实 Room 持久化。测试数据只在隔离的测试安装中建立并清理。UI Automator 负责跨 Activity 的可访问性操作，Compose Test 负责语义、布局及选中状态断言；截图由实际 emulator 渲染，不是设计示意图。截图证据不等同于 golden 像素基线；现有图标像素差测试继续保留。

| 范围 | PR 必须通过 | 扩展 / 手动 |
| --- | --- | --- |
| Android API | 33、35 手机 | 26、33、36 手机与 API 36 平板 |
| 标签页面主题 | Lumi、Material 3、液态玻璃组件 | 真实页面简体中文/深色/大字号场景 |
| 用户入口 | 分类管理、长按书籍、跨目录选择、确认/取消并重新进入 | 同路径扩展环境 |
| 全量 instrumentation | 明确 smoke 类 | 手动 `run_full_instrumentation` |
| Macrobenchmark | 不运行 | 手动 `run_macrobenchmarks`，数据仅供参考 |

UI Automator 和 Compose Test 已足以覆盖当前流程。Maestro/Appium 仅在需要独立于 instrumentation 的发布 APK 黑盒验收时选用，不默认安装第二套 UI 测试框架。物理 GPU、厂商 ROM 等仍需要设备农场或实机补充。

API 26/33/36 手机与 API 36 平板的扩展 emulator、完整历史 Robolectric 和可选 Macrobenchmark 位于定期/手动 workflow。仪器测试源码在 JVM 阶段提前编译，设备任务在启动 AVD 前准备 APK；每个设备独立上传 JUnit、截图与日志，并展示标准 JUnit 摘要。完整测试模式仍使用 AndroidJUnitRunner 发现，不通过脚本解析结果替代 Gradle。

### 参考实现

- [AnkiDroid emulator matrix](https://github.com/ankidroid/Anki-Android/blob/e87e18a57a03add43520a0eca3229c7013da3293/.github/workflows/tests_emulator.yml)：PR 与扩展设备分层，平板进入定期/手动矩阵，Gradle 缓存只由主分支写入。
- [AnkiDroid TagsDialogScreenshotTest](https://github.com/ankidroid/Anki-Android/blob/e87e18a57a03add43520a0eca3229c7013da3293/AnkiDroid/src/test/java/com/ichi2/anki/dialogs/tags/TagsDialogScreenshotTest.kt)：使用明确标签夹具，分别覆盖空目录、选中状态和方向。
- [AntennaPod checks](https://github.com/AntennaPod/AntennaPod/blob/4dd48842391846fcb4b64071b11734cb70bf7943/.github/workflows/checks.yml)：先构建测试 APK，再启动 KVM emulator；测试夹具独立建立、清理，失败时保存原生报告。
- [Now in Android adaptive screenshots](https://github.com/android/nowinandroid/blob/2982e63cb691ad49cd009aab43dc206fee6699c2/app/src/testDemo/kotlin/com/google/samples/apps/nowinandroid/ui/NiaAppScreenSizesScreenshotTests.kt)：明确窗口尺寸与布局场景；其标签测试也覆盖大字体。Lumi 先用 Robolectric qualifiers 和语义断言覆盖同类风险，真实截图作为运行证据；尚无受审查的 Roborazzi golden 基线。

只迁移适合 Lumi 的测试设计与工作流思路，不复制 GPL 源码，不增加无实际断言的测试，不将重复测试用于掩盖不稳定失败。

## 回归证据（Red → Green）

先在修复前的独立验证分支上移植不依赖修复实现的回归测试（包含所需测试依赖），通过现有 workflow_dispatch 运行并保存失败 job、测试名、堆栈、截图及 SHA。再将同一测试运行在修复分支上；两次使用相同 API、主题、语言和数据。基线失败必须来自目标行为，而不是编译失败、网络或测试环境错误。若无法复现，PR 应注明未复现的条件，不得将绿色 CI 当成已证明的根因。

运行证据位于每个 API 的 Actions artifact；`final-screen.png` 和 `final-hierarchy.xml` 是测试结束时的尽力收集（可能已卸载应用），关键页面应在测试内部截图。失败日志不上传用户真实书库、密钥或私人数据。

## 失败排查

1. 绑定当前 head SHA，先看最早失败 job。
2. 静态分析失败就修 workflow/docs，不启动 Android 重任务。
3. 编译失败先修编译；Robolectric/JUnit 失败读取真实测试名、stack trace 和原生 XML/HTML 报告。
4. 网络/CDN 失败与业务断言分开处理。Gradle Wrapper 使用 60 秒网络超时；dependency/bootstrap 阶段最多由 `nick-fields/retry` 重试 3 次、间隔 60 秒。
5. Kotlin 编译、Robolectric smoke、JUnit 断言、Lint 和 instrumentation 不使用自动重试；这些失败必须按真实根因修复。

发布权限和签名边界见 [发布指南](release.md)。

## 自动上游同步

[上游同步工作流](../../.github/workflows/sync-upstream.yml) 每周一、周四自动运行，也支持维护者从 Actions 手工触发。目标是吸收 `huangder/Lumi_Books` 上游公开代码，而不是把 Community 分支重置为上游。

1. 从两仓库最近共同祖先执行标准 Git 三方合并，**从不重置或强推 `main`**。上游已在历史中时，不创建多余提交。
2. 即便没有文本冲突，只要上游和 Community 自分叉点以来改过同一个文件，或者上游修改了发行版本、OSS source set、构建、签名/CI、Community 规范、启动清单、应用字符串等受保护路径，就视为**需要产品语义审查**，不自动入 `main`。
3. 对完全无冲突且无社区文件交叉修改的合并，先创建唯一 `sync/verify-*` 候选分支，使用 `workflow_dispatch` 运行完整 Android Community Beta CI（JVM/Robolectric、Lint、Debug/Diagnostic/Release、API 33/35 emulator）。候选分支**不读取发布签名密钥，也不发布 APK**。
4. 候选 CI 通过后，重新确认 `main` 与上游都没有更新，使用普通 Git fast-forward 推送到 `main`。因为 `GITHUB_TOKEN` 发出的 push 不会触发其他工作流，必须再次显式 `workflow_dispatch` 主分支 CI；只有该次完整门禁通过、且版本未发布，才允许已有的固定签名 Beta 发布流程运行。
5. 真正的 Git 冲突创建指向上游跟踪分支的 Draft PR；无文本冲突但存在社区改动交叉、候选 CI 失败、分支并发移动或主分支拒绝更新时，创建指向合并候选的 Draft PR。**不会悄悄选择上游版本、删改社区特性或把失败当成通过。**
6. 所有 PR 必须由维护者解决冲突、复核 Community 行为并通过标准 CI 后再合并。Actions 创建的 PR 可能不会自动触发普通 `pull_request` 工作流；必要时请对已解决的分支手动运行 `Android Community Beta`。

工作流仅在本 Fork 的默认分支持有临时写权限，并对外部来源的合并候选保持只读检查；不要求配置 Personal Access Token 或额外的签名 Secret。受保护分支规则如禁止机器人直接推送，则会退化为审查 PR，而非绕过规则。

这套机制保护**已明确改动的社区文件与经过测试的行为**，但不能绝对识别未覆盖的跨文件语义变化；重要的上游架构变化仍需人工审查和回归测试。

## 已合并分支清理

[分支清理工作流](../../.github/workflows/prune-merged-branches.yml) 在正常 main push 后运行，也可手动触发。仅检查 Community 开发分支（`community/*`）和上游审查分支（`sync/upstream-*`）；只有同仓库 PR 已合并至 main、合并提交仍在 main 历史中、分支 SHA 完全匹配已合并 PR 的 head，且没有开放 PR 时，才使用 Git lease 安全删除分支。并发新增提交、未合并分支、main 和其他分支一律保留。清理不使用发布密钥，也不影响 APK 发布门禁。
