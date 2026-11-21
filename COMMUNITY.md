# Lumi Community Beta

> 此仓库是 [huangder/Lumi_Books](https://github.com/huangder/Lumi_Books) 的非官方社区 Fork，不代表原作者。请保留源项目的 `LICENSE`、`NOTICE` 和相关第三方版权信息。

## 目标与当前范围

- **目标版本**：`2.3.12-beta.1`（公开上游源码为 `2.3.11`），Android `versionCode=19`。
- **独立包名**：`io.github.wxyu71.lumibooks`，独立于上游 `com.huangder.lumibooks` 安装；两个应用的数据目录也不同，不能自动共享书籍和阅读记录。
- **构建**：始终选择 `app/src/oss/`。公开版的 `OssAccess.isUnlocked()` 本来就返回 true、远程书源无限制；本 Fork 不包含官方私有会员许可校验和服务器。
- **社区功能边界**：不复制私有 Premium UI；公开版的标签数据库、Room 迁移、日志事件记录、摘录与签到海报、笔记导出基础模块可复用。社区独立构建并完善标签管理、可选择开启的阅读日志、基础海报与本地 TXT/Markdown 导出；**不提供年度/周月阅读报告和高级付费摘录模板**。

## 文档职责与入口

| 资料 | 唯一用途 |
| --- | --- |
| `README.md` / `README.*.md` | 用户介绍、下载安装、版权与上游署名 |
| `README.dev.md` / 翻译 | 贡献者构建、代码架构、开发导航 |
| `COMMUNITY.md`（本文） | 社区 Fork 边界、发布流程与当前验收条件 |
| `AGENTS.md` | Agent 变更组织、GitHub 插件批量提交、验证纪律 |
| `devdocs/community-ci-test-design.md` | 详细 CI 门禁、故障调查及技术依据 |
| `devdocs/community-ui-ux-implementation.md` / `devdocs/lumi-ui-motion-design-system.md` | 当前社区 UI 及动效实现标准 |

`devdocs/` 中的其他 handoff、旧计划和上游设计资料是历史背景，不代表当前可执行规范。旧的 Agent 快速上手指南已过时；以当前源码和上述入口为准。`docs/` 含上游历史网站、资源和技术调研，**未作为此 Fork 的正式网站部署**；其中 `docs/app-config.json` 仅为手动联网时获取隐私/条款版本等静态策略数据。不要创建平行的“最终方案/日常 CI 日志”文档；单次执行证据在 PR 或 Actions 中追踪。

## CI 与正式 Beta Pre-release

- **PR 阶段**：`.github/workflows/android-beta.yml` 对面向 `main` 的 PR、`main` push 和手动调度运行。所有必需 Policy、OSS APK/JVM、零 Error/Fatal Lint、Release/Diagnostic 编译、API29/35 模拟器检查必须成功，最终以 `Required CI summary` 为准。PR APK 使用临时 Debug 签名，只用于测试，不能当作公开可升级的 Beta。
- **Tag 发布阶段**：`.github/workflows/android-release.yml` 仅由 `vX.Y.Z-beta.N` Tag 的 push 触发，重用整套 Required Gate；只有 Tag 对应版本号且其提交已包含在 `main`、全套验证通过，才进入受限的 `beta-release` Environment。该任务单独启用 `contents: write`，签名 `:app:assembleRelease` 的 OSS 通用 APK、校验签名证书指纹与版本、生成 `SHA256SUMS.txt`，最后创建 GitHub **Pre-release** 并附上 APK 和校验文件。失败时不发布，已有 Release 不覆盖。
- **应用内检查更新**：只有用户主动请求时，应用才读取本 Fork 的 GitHub Releases API，且只把带有真实社区签名 APK 的已公开 Beta 标记为更新。历史 `docs/app-config.json` 的静态版本号不再决定可用更新；下载页只通向 `https://github.com/wxyu71/Lumi_Books/releases` 下的对应 Release，不使用上游镜像、网盘或其它下载入口。
- **主分支保护**：必须由仓库管理员配置 `main` 的 PR 合并要求和 Required Checks，建议同时保护 `v*-beta.*` Tag，并设置 `beta-release` Environment 的批准人及保护规则。CI 自身成功不等于 GitHub 已经强制保护合并。

### 首次发布前必须由维护者配置（仓库中不保存密钥）

在 GitHub **Settings → Environments → beta-release** 内设置下面四个 Secrets 和一个非机密 Variable：

| 名称 | 用途 |
| --- | --- |
| `LUMI_BETA_KEYSTORE_BASE64` | 长期 Android Release keystore 的 Base64 编码 |
| `LUMI_BETA_STORE_PASSWORD` | Keystore 密码 |
| `LUMI_BETA_KEY_ALIAS` | 私钥别名 |
| `LUMI_BETA_KEY_PASSWORD` | 别名密码 |
| `LUMI_BETA_CERT_SHA256` (Variable) | 签名证书的 SHA-256 指纹；必须从本地 keystore 独立核对 |

签名密钥应由维护者在可信环境离线生成并安全备份，之后所有 Beta 更新必须保持相同签名和 `applicationId`。不要把密钥写入仓库、PR、Actions 日志或任何 Agent 对话；变量缺失、证书不匹配时流水线会失败而不是发布。Beta 目前不使用 Play Store、外部设备农场或第三方 APK 镜像。

当 PR 合并、对应 `main` 的 CI 通过、签名环境已设好，维护者才可以在 **main 的已验证提交** 上创建并推送匹配 `gradle.properties` 的版本 Tag（例如 `v2.3.12-beta.1`）。切勿在 Draft PR 分支上先打正式发布 Tag。每次新 Beta 均需递增 `lumi.communityVersionCode`，并匹配 `lumi.communityVersionName`；发布后到 [社区 GitHub Releases](https://github.com/wxyu71/Lumi_Books/releases) 核对 Pre-release、签名和 APK 校验和。首次真正发布前这里**没有可下载的正式 Beta**，不要把 GitHub Actions 的 Debug Artifact 混称为公开正式版本。

## 本地构建

可选的开发者本地编译需要 JDK 21、Android SDK API 37、NDK 28.0.13004108、CMake 3.22.1（不属于 CI 或发布验收步骤）：

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

APK 位于 `app/build/oss/outputs/apk/debug/`。如更新版本，修改根目录 `gradle.properties` 中的 `lumi.communityVersionName`、`lumi.communityVersionCode`，并确认 `versionCode` 严格递增。官方上游版本若变化，应先对比上游源代码、保留本 Fork 的 `applicationId`、source-set 和 CI 配置，再决定新的 Beta 版本号。

## 社区功能现状与后续验收

所有社区版研发 PR 以 GitHub Actions 的最新 Required Gate 为验收依据：

1. **标签管理**：书籍标签和标注标签创建、重命名、删除；分组的创建、重命名、删除；标签分组移动及批量移动/删除。复用原有 Room/DAO、备份同步协议与 `AppSheetHeader`、`GroupedCard`、`LiquidGlassAlertDialog`。删除组只解除分组；删除标签需要确认。全局标签操作不依赖会员资格。
2. **阅读日志与海报**：复用 `ReadingJournalRepository`、`ReadingJournalRecorder` 和保留期限策略；默认关闭、开启前同意、随时停止、用户明确确认后清除。本地事件可进入现有 `BookShareActivity` 签到海报生成及保存/系统分享流程，不增加社区服务。
3. **笔记导出增强**：单书 TXT/Markdown 和「全部标注」当前筛选结果的跨书批量 TXT/Markdown 导出；使用 `BookNotesExportBuilder` 与 Android Storage Access Framework，由用户明确选择保存位置，隐藏书籍与墨迹绘制不导出。
4. **保留边界**：仍然**不引入阅读报告图、年度报告、额外摘录付费模板、桌面小组件、独立社区云书库、遥测或会员购买功能**。

仍待完善：多语言完全本地化、超大书库/低内存压力验证、所有第三方网络端点与 Manifest 组件审计、完整无障碍/RTL/电子墨水/低动效回归测试、稳定签名与正式发布。阅读记忆作为独立候选功能，尚未在此 PR 默认开启或新增数据收集。详见 [社区 UI 与交互实现规范](devdocs/community-ui-ux-implementation.md)。

## 参考

- [Gradle on GitHub Actions](https://docs.gradle.org/current/userguide/github-actions.html)
- [GitHub Actions security hardening](https://docs.github.com/en/actions/reference/security/secure-use)
- [Android SDK 37](https://developer.android.com/about/versions/17/setup-sdk)
- [F-Droid nightly builds](https://f-droid.org/en/docs/Publishing_Nightly_Builds/)

## 社区版产品原则（2026-11-20）

- 移除 QQ、设置中的问题反馈与首页会员营销入口；「关于应用」保留社区 GitHub 仓库、Discussions 和可选择的原作者赞助入口。
- 首次使用无需倒计时和宣传翻页；语言设置（如有）之后，阅读本地条款与隐私政策并点击同意即可进入。
- 取消启动联网检查原作者的更新公告、捐赠公告和远程配置；用户手动检查更新时仅访问本社区 Fork 的 GitHub Raw 地址。没有新增遥测或私有远程服务。
- 保留由用户自己启用/配置的 WebDAV、OPDS、翻译、TTS 和 MinerU；不提供会员服务。
- 不暴露阅读报告、官方高级摘录模板与两个桌面小组件；现有笔记 TXT 导出保留。
- 书籍/标注标签、分组、批量操作、可选阅读日志和基础海报 UI 已在此 PR 编写；均须通过 GitHub Actions 上的 JVM、Lint、构建变体与模拟器门禁，完成后方可标记正式可用。
- 上游同步采用定时自动提出 Draft PR、人工合并的策略，防止上游恢复已经删除的会员 UI 或远程请求。

### Fork 管理提示

截至 2026-11-20，GitHub API 显示此 Fork 的 Issues 与 Discussions 均处于关闭状态。需在仓库 Settings → General → Features 中手动开启它们，社区 Discussions 链接才会有内容可访问。需要允许自动提出 PR 时，还应在 Settings → Actions → General 开启「Allow GitHub Actions to create and approve pull requests」。定时工作流只在代码合并到默认 `main` 后执行，不会在 Draft PR 中定时触发。

### 代码清理说明
独立的官方「问题反馈」Activity 与其 QR/QQ 页面已从源码移除，原本共用文件里的本地诊断界面保留（仅在用户主动调用时导出诊断信息）。启动阶段的早期捐赠者兑换公告不会出现。上游版权声明、第三方许可证和原项目贡献者署名仍应保留。

### 后续静态网站
Fork 暂停了上游 `.github/workflows/static.yml` 的 GitHub Pages 自动部署，避免直接发布包含 Premium 推广的原站点。历史 `docs/` 源码仍留作技术参考，但不作为社区版官网；若后续需要网页，应另行设计并清理所有导航和营销内容。
