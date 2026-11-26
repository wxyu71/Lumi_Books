# AGENTS.md — Lumi Community

本仓库是 `wxyu71/Lumi_Books` 的单人维护、个人使用的非官方 OSS Fork。开始工作前先确认 PR、base/head 和分支 SHA。未经维护者明确要求，不修改 `main`、合并 PR 或公开发布；不可删除合法的上游版权和许可证。

项目边界见 [COMMUNITY.md](COMMUNITY.md)，架构见 [docs/architecture/overview.md](docs/architecture/overview.md)，开发入口见 [docs/README.md](docs/README.md)。

## 代码与测试

- 把代码放到已有 feature/layer。Community-only UI 仍应进入 `ui/<feature>/`；`edition/` 只做薄适配，不承载大型页面、Repository 或数据模型。
- 优先复用现有 Room、Repository、domain model、导航和 UI 组件，不为 Community 功能建立平行存储或第二套框架。
- 行为变化使用真实 Kotlin/JUnit/Robolectric/instrumentation 测试。禁止用读取 Kotlin/YAML/Markdown 后 `assertIn` 某段文本的“contract test”替代行为验证。
- 通用仓库检查交给现有轮子：actionlint/ShellCheck/zizmor、Android Lint、markdownlint-cli2/Lychee、Gradle/JUnit/AndroidJUnitRunner。项目自有脚本只保留这些工具无法表达的发行边界或证据收集。
- 不通过测试重试、排除或放宽断言把真实失败变绿。先定位根因，再修产品或测试同步。

## GitHub 与 CI

- 先收集一轮失败日志再修改；每次结论绑定精确 head SHA。
- 仅有 GitHub 插件时，优先用 Git tree/commit 一次提交同一逻辑变更，并用 `update_ref(expected_sha=...)` 防止覆盖并发更新；不要强推。
- CI 顺序为静态分析 → 编译/Robolectric/JVM → Android Lint → APK/Diagnostic/Release → emulator → workflow summary。分支保护不是本仓库 CI 重构的验收目标。
- GitHub Actions 是无本地 SDK 时的正式远程 Android 执行环境：提交后读取精确 SHA 的编译、JUnit、emulator 结果和运行证据；未结束的运行不得称为通过。
- UI、导航、闪退修复优先加入真实 Activity 用户入口的 instrumentation 回归；尽可能在修复前复现失败、修复后运行同一测试。无法复现时明确记录覆盖范围与缺口，不用静态推断替代根因证据。

## 发布

- 只有 `main` 的新版在正常 CI 成功后才能自动发布 Pre-release；见 [docs/development/release.md](docs/development/release.md)。
- Release build 与 privileged signing/publish 分离：前者无 Secrets/写权限，后者只签名前者 artifact 并发布，不重新运行 Gradle。
- 固定签名只使用 `LUMI_BETA_KEYSTORE_BASE64` 和 `LUMI_BETA_KEYSTORE_PASSWORD`；不能在 CI 中生成新密钥。
- 正式合并或发布需用户明确要求；不得伪称未发生的 Release。

## 文档

- 用户说明：`README*.md`
- Fork 边界：`COMMUNITY.md`
- 人类贡献：`CONTRIBUTING.md`
- 安全：`SECURITY.md`
- 架构：`docs/architecture/`
- 操作手册：`docs/development/`
- 长期实现参考：`docs/reference/`

不要创建 handoff、某次 CI 状态、过时计划或重复的权威文档；这些历史由 Git、PR/Issue 和 Actions 保存。
