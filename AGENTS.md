# Agent instructions — Lumi Community

适用范围：整个仓库。此文件只规定 **Agent 如何在本仓库安全开发和交付变更**，不复制产品、UI 或 CI 设计文档。

## 开始工作前

- 这是 `wxyu71/Lumi_Books` 的非官方开源社区 Fork。先核对目标仓库、当前分支、PR 的 base/head 及最新 head SHA，**不要默认修改 `main`**。社区版必须保持 OSS 源集、独立包名、上游署名和许可证；范围见 `COMMUNITY.md`。
- 以当前分支的真实源码、`.github/workflows/android-beta.yml` 为准；CI 设计和门禁依据见 `devdocs/community-ci-test-design.md`，UI 任务参考 `devdocs/community-ui-ux-implementation.md` 和 `devdocs/lumi-ui-motion-design-system.md`。
- 历史 handoff、计划和设计资料只供背景参考；不能覆盖当前源码、构建配置、CI 与 `COMMUNITY.md` 的文档导航。

## Commit 与 Push 是两件事

- **一个 commit = 一个完整、可审查的逻辑改动单元**，含必要的相关测试；可同时涉及多个文件。不按文件数、行数硬切提交，也不把互不相关的修改塞进同一提交。
- **一个 push / 远端 branch ref 更新 = 一批已经准备、检查过的提交**。可以是一个完整 commit，也可以是多个有意义的 commit；不是每编辑一个文件就推送一次。紧急修复可单独交付。
- 先汇总任务范围、改动文件、关联测试和已有 CI 失败；尽可能在推送前处理整轮问题。不要为了修一个错误、改一段文案或补一条测试而连续更新远端 PR。
- 有本地 Git 环境时，先在本地组织提交并完成能运行的检查，最后一次 `git push` 更新 PR 分支。**减少 push 次数不等于减少合理的本地 commit 数量**。

## 仅有 GitHub 插件 / API 时的批量写入协议

GitHub Contents API 的 `create_file`、`update_file`、`delete_file` **每次都会创建提交并更新远端分支**。同一任务涉及多个文件时，不要逐文件调用它们来“边修边推”；每次远端 head 变化都可能触发新的 PR CI，并取消上一轮运行。

按以下顺序执行（GitHub 插件提供对应能力）：

1. 读取 PR/head SHA、目标文件和该 head 对应的 Git tree SHA；在内存中准备并复核**全部**相关文件内容、删除项及测试改动。
2. 使用 `create_blob`（按需）和 `create_tree(base_tree_sha=...)` 暂存一组文件改动，再用 `create_commit(tree_sha=..., parent_sha=...)` 创建逻辑提交。需要多个逻辑提交时，依次使用上一个提交和 tree 构建下一个提交。**此阶段不要调用 `update_ref`**。
3. 所有提交准备好后，只调用**一次** `update_ref(branch_name=..., sha=<最终提交>, expected_sha=<起始 head>, force=false)` 将目标 PR 分支前移。不要直接推送到 `main`。
4. 如果预期 head 不匹配或 ref 更新被拒绝，说明远端可能被其他开发者更新：重新读取 PR/head 与 diff，整理、重建提交后再更新；**不得强推覆盖他人改动**。

创建 blob、tree、commit 对象本身不会移动分支；**只有最终更新 ref 才会发布这批改动**。单文件紧急变更可以直接用 Contents API，但不能把它当作多文件任务的默认工作方式。

## 检查与 CI 反馈

- 有可用本地环境时，至少执行与改动有关的检查，例如 `git diff --check`、`python3 -m unittest discover -s tools/tests -p 'test_*.py'`、`python3 tools/check_community_policy.py`、`python3 tools/ci_test_inventory.py`；涉及 Android 源码/资源时按需执行 `./gradlew :app:lintDebug` 和相关编译、测试。执行范围以实际任务为准，不宣称没跑过的检查已通过。
- 只有 GitHub 插件、没有本地构建环境时，先审查完整 diff 和已有 CI 日志，**一次批量更新后**等待该 head 对应的 GitHub Actions 完整验证；说明哪些检查尚未执行/完成。不能用多个试探性 push 代替读日志。
- CI 失败时先收集同一轮所有相关 job/测试失败、区分代码问题与临时依赖或网络故障，再集中修复。确属偶发基础设施故障且源码未变时，可按需重跑失败的 job；不要用空提交、改文档或反复推送来触发重试。
- 保持现有完整 PR Required Gate（含零 Error/Fatal Android Lint、APK/JVM、模拟器及最终 `Required CI summary`）。不要为省 CI 使用 `[skip ci]`、路径过滤或绕过/放宽必需检查；没有明确任务理由不要调整 CI YAML。
- **验证状态始终绑定精确 head SHA**：旧提交的绿色 CI 不能证明新提交通过。保持当前 Draft PR 的审查/合并条件；不得擅自合并或声称仓库保护规则已经启用。

## Beta 发布边界

- 版本、签名及 Tag 发布步骤见 `COMMUNITY.md`；不得在未合并的 PR 上创建正式 Beta Tag，不能以 Debug APK、历史绿色 CI 或缺失签名证书的产物冒充 Pre-release。
- Tag 发布流水线需使用独立的受保护 Environment、固定签名身份与完整的 Required Gate。只有用户明确要求并且完成发布前检查后，Agent 才可启动公开发布；不要求 Agent 能读取或生成签名密钥。

## 文档放置原则

- 只把跨任务、跨目录的 Agent 行为规则留在本文件；CI 机制的技术说明更新到已有 `devdocs/community-ci-test-design.md`，社区产品边界更新到 `COMMUNITY.md`，UI 决策更新到相应现有设计文档。
- 优先更新已有的权威文档；不要每次修 Bug 都新增独立的说明、交接或“最终总结” Markdown，也不要把同一规范复制到 README、PR 描述和多个 `devdocs/` 文件。
- 除非任务确实修改了文档所描述的行为，不要为了记录每一次 CI 尝试而改文件、单独推送或制造一次新的 Required Gate 运行；临时进度和单次运行证据优先写在 PR 讨论中。
