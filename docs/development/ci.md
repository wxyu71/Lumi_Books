# Android CI — 快速失败、保留测试证据

唯一事实来源为 [Android Community Beta](../../.github/workflows/android-beta.yml) 和每周 [兼容性工作流](../../.github/workflows/android-emulator-extended.yml)；不在文档中维护某次 CI 的临时状态。

## PR 与 main 必需门禁

| 阶段 | 执行内容 |
| --- | --- |
| `policy` | 官方 actionlint + ShellCheck、社区 Policy、翻译、测试分片库存和项目特定 Python 回归；不启动 Android 构建 |
| `verify` | **先**编译 Kotlin 和 JVM 测试、预取 API 33/35/36 Robolectric JAR、执行最小 Robolectric 冒烟和完整 Reader/UI/Core JVM 分片；**全部通过后**才打包 Debug APK，校验包名、版本、临时 Debug 签名与 SHA-256 |
| `quality` | 原生 Android Lint 零 Error/Fatal 要求、独立 Gradle 进程分别构建 Diagnostic 和 Release，避免合并打包时内存峰值 |
| `device-smoke` | 等编译、JVM、Lint 和 APK 通过后执行 API **33/35** 模拟器集成、UI、截图及 JUnit 报告校验 |
| `ci-gate` | `Required CI summary` 要求以上阶段全部成功；跳过任一必需项即失败 |

PR 仅触发一轮 `pull_request`，功能分支不会额外触发 `push` CI。工作流使用 `contents: read` 和 `persist-credentials: false`；Fork PR 只能读 Gradle 缓存；没有发布 Secrets、`pull_request_target` 或自动合并。

## Robolectric SDK 的明确边界

- PR 的主线测试只运行 Android **13+**（API **33/35/36**）。`robolectric.enabledSdks` 由 CI 注入，旧 SDK 专属测试由 Robolectric 在主线筛选之外排除（不保证逐项生成 JUnit `<skipped>`）；它们**不会被删除或计作主线已通过**。`ci_verify_junit.py` 打印实际执行数和 JUnit 报告中的跳过数，完整历史覆盖以每周兼容性通道为准。
- 每周和手动的扩展 CI 设置 `LUMI_ROBOLECTRIC_LEGACY=1`，不限制 SDK，并额外预取 API **26/28/29/30/31/32/34**，照常跑全部 Reader/UI/Core JVM 测试。历史行为的回归仍然可见；旧版本模拟器 API 26 也保留在每周矩阵。
- 本地开发默认不强制限制 SDK，也不会额外预取十个测试系统 JAR；Robolectric 使用自己的默认依赖解析。所有 CI 环境预取完成后才允许从本地目录加载，不把 HTTP 429 延迟到数千测试执行期间。

## 不再自制基础设施

- Gradle 官方 Wrapper 校验与缓存由 `gradle/actions/setup-gradle` 处理。
- 使用官方 `actionlint` Docker 镜像检查工作流；`ShellCheck` 检查 Bash。移除自制 Gradle 错误日志正则分类器、盲目 Maven HTTP 探针与包装重试。
- 编译错误、测试失败、Android Lint 和打包失败都直接由 Gradle 退出码判定，不尝试自动重跑测试断言。JUnit XML 和日志以 Artifact 留存。
- 项目自有的少量脚本只维护社区 Policy、分片/结果完整性、模拟器截图和发布安全约束，这些是 Gradle 通用任务不能直接保证的。

## 失败排查

先核对 PR 的 head SHA，再读最早失败的 job。编译/依赖就绪检查失败时无须分析 APK；JVM 失败时检查 JUnit XML 和被明确列出的 skipped 数。网络/CDN 故障与业务断言不要混为一谈；纯瞬时 GitHub 基础设施问题优先重跑**失败的 Job**，而非推空提交。CI 改动应审查后批量推送一次。

`android-release.yml` 仅在 **main 的 push CI 完全通过**后创建预发布，使用固定 Beta 签名密钥；PR 测试 APK 的 Debug 证书不具备原位升级能力。
