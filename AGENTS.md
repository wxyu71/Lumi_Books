# AGENTS.md — Lumi Community

本仓库是 `wxyu71/Lumi_Books` 的单人维护、个人使用的非官方 OSS Fork。先检查当前任务的 PR、base/head、分支 SHA。未经维护者明确要求，不直接修改 `main`、合并 PR 或公开发布；不可删除上游的合法版权/许可证。项目定位见 [COMMUNITY.md](COMMUNITY.md)，开发入口见 [docs/README.md](docs/README.md)。

## 代码与 CI

- 一个 commit 对应一个完整逻辑变化，包含相关测试；不要对每个文件各 commit / push 一次。开发可积累多个本地 commit，然后一次 push。
- **仅有 GitHub 插件时**：读取 head 和 base tree → 准备全部文件 → `create_tree` → `create_commit`（可多个逻辑 commit）→ 最后 **一次** `update_ref(expected_sha=<起始 head>, force=false)`。遇到冲突重新读取并重建，不要强推。
- 推送前先收集整轮 CI 失败和必要代码变更；有本地环境则运行相关测试。GitHub 插件环境没有构建能力时必须如实说明，**不得把静态审查等同于 Android CI 成功**。
- 保留正常 PR 的 Required Gate（Policy、APK/JVM、Lint、模拟器与 `Required CI summary`）；不使用 `[skip ci]` 绕过必需检查。查看日志后批量修复，不用重复推送试错。每次验证绑定精确 head SHA。

## 发布（简化的个人 Beta）

- 只有 `main` 的 **新版**在正常 CI 成功后才能自动发布 Pre-release；操作方法见 [docs/development/release.md](docs/development/release.md)。
- 自动发布只需要预先配置的两项 Repository Secrets：`LUMI_BETA_KEYSTORE_BASE64`、`LUMI_BETA_KEYSTORE_PASSWORD`；不需要 Environment 审批、人工 Tag 或第二次完整 CI。**始终使用原固定签名密钥**，不可在 CI 中重新生成以免破坏覆盖升级；首次从 Debug/临时签名迁移须备份后重装。
- 可以通过 GitHub 插件编辑并提交版本修改，正式合并需用户明确要求；若当前连接不能直接创建 Release，以已合并版本触发的 Actions 自动发布，不伪称已经发布。

## 文档维护

- 全部开发文档从 [docs/README.md](docs/README.md) 导航。用户说明更新 README，开发操作更新 `docs/development/`，长期技术行为更新 `docs/reference/`，产品边界更新 `COMMUNITY.md`。
- 不创建 handoff、某次 CI 记录、过时计划和空目录；任务证据留在 PR/Actions。旧资料由 Git 历史保留，不作为未来 Agent 指令。
