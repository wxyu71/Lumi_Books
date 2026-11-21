# Android CI — 可执行契约

**唯一事实来源**是 `.github/workflows/android-beta.yml` 及其脚本/测试。此页描述任务结构和排障入口，**不记录各轮 CI 编号、失败流水账和特定提交的临时状态**。单次证据应附到该次 [Actions 运行](https://github.com/wxyu71/Lumi_Books/actions/workflows/android-beta.yml) 或 PR 讨论中。

## 触发与权限

- 面向 `main` 的 `pull_request`、`main` 的 `push`、`workflow_dispatch`。
- 功能分支没有重复的 `push` 触发；过期 PR 运行由 concurrency 取消。工作流 `contents: read`、`persist-credentials: false`，不用发布密钥和 `pull_request_target`。
- **必须保持完整 Required Gate**，不能为减少运行使用 `[skip ci]` 或路径过滤来跳过必需检查。

## 必需任务

| 任务 | 职责 | 主要入口 |
| --- | --- | --- |
| `policy` | Fork 禁止项、翻译、测试分片和 Python 回归 | `tools/check_community_policy.py`、`tools/ci_test_inventory.py`、`tools/tests/` |
| `verify` | OSS Debug APK、包名/版本/Debug 签名与 SHA-256、reader/ui/core JVM | `tools/ci_run_jvm_shards.sh` |
| `quality` | `lintDebug` 零 Error/Fatal、Release/Diagnostic 编译 | `tools/ci_require_clean_lint.py` |
| `device-smoke` | API 29 与 35 模拟器集成、UI/截图和 JUnit 结果 | `tools/ci_run_device_smoke.sh` |
| `ci-gate` | 汇总所有上游任务，形成 `Required CI summary` | workflow 末尾的必需结果检查 |

扩展设备矩阵在 `android-emulator-extended.yml`，额外质量任务在 `android-quality.yml`；不要把它们与 PR Required Gate 混淆。PR 产生的 Debug APK 是测试 Artifact，不是对外分发的个人 Beta。

## 出错时怎么做

1. 找到当前 PR **精确 head SHA** 的 Actions 运行，先查 `Required CI summary`，再查失败 job。
2. 汇总同一轮失败，而不是每个报错修一行立即推送。区分源码失败、测试断言、依赖下载故障和 runner 中断。
3. 本地尽可能运行受影响的脚本、JVM 或 Android Gradle 任务；仅有 GitHub 插件时审查完整 diff，批量提交一次后检查新 head。
4. 只有明确的瞬态基础设施故障才考虑重跑失败 Job；没有新源码不要用空提交触发一整轮新构建。

## 发布与普通 CI 的边界

`android-release.yml` 在成功的 **main/push** CI 完成后单独运行，复用其验证结果，不再把全套模拟器任务运行第二次。它只在尚未发布的 Beta 版本上创建 Release；发布方式见 [发布指南](release.md)。
