# Lumi Community Beta

> 此仓库是 [huangder/Lumi_Books](https://github.com/huangder/Lumi_Books) 的非官方社区 Fork，不代表原作者。请保留源项目的 `LICENSE`、`NOTICE` 和相关第三方版权信息。

## 目标与当前范围

- **目标版本**：`2.3.12-beta.1`（公开上游源码为 `2.3.11`），Android `versionCode=19`。
- **独立包名**：`io.github.wxyu71.lumibooks`，独立于上游 `com.huangder.lumibooks` 安装；两个应用的数据目录也不同，不能自动共享书籍和阅读记录。
- **构建**：始终选择 `app/src/oss/`。公开版的 `OssAccess.isUnlocked()` 本来就返回 true、远程书源无限制；本 Fork 不包含官方私有会员许可校验和服务器。
- **社区功能边界**：不复制私有 Premium UI；公开版的标签数据库、Room 迁移、日志事件记录、摘录与签到海报、笔记导出基础模块可复用。社区独立构建并完善标签管理、可选择开启的阅读日志、基础海报与本地 TXT/Markdown 导出；**不提供年度/周月阅读报告和高级付费摘录模板**。

## CI 流程

[Android Community Beta 工作流](https://github.com/wxyu71/Lumi_Books/actions/workflows/android-beta.yml)

向 `main` 发起 PR 或推送 `main` 时自动运行；也可从默认分支的 Actions 页面手动触发。PR 检查不再与功能分支 Push 重复运行。

1. 在 `ubuntu-24.04` runner 上安装 Android API 37、Build Tools 37.0.0、NDK 28.0.13004108 和 CMake 3.22.1；APK、JVM 与所有必需任务均使用 JDK 21。
2. 使用校验过的 Gradle Wrapper 与官方 Gradle Actions 缓存；所有第三方 Action 固定完整 commit SHA。
3. 只读 `GITHUB_TOKEN`，无仓库私钥，无 `pull_request_target`，且检查仓库内没有私有 Premium 源集。
4. 在 GitHub Actions 构建 `:app:assembleDebug`，验证每个 APK 的包名、版本、Debug 签名及 `SHA256SUMS.txt`，上传可审计的 APK、签名和校验和。
5. JVM 检查在单个 Runner 上依次执行 reader/ui/core 三个逻辑分片，共用 Kotlin 编译结果，各自验证 JUnit XML、上传日志；即使 APK 构建失败仍尝试保留 JVM 诊断。只有明确依赖解析故障才有限重试，编译/断言失败不重试。
6. Android Lint Debug 以仓库中的固定 `app/lint-baseline.xml`（历史 556 项 Error）为基线，任何新增 Error 都阻塞；Diagnostic/Release 构建必须成功。
7. GitHub-hosted API 29/35 模拟器执行数据库迁移、UI 交互、像素回归并验证 JUnit 报告及导出的 PNG。Policy、APK/JVM、Lint/变体和模拟器全部通过，Required Gate 才会成功；扩展 API 26/33/36 和性能测试也仅在 GitHub Actions 的定时/手动工作流执行。
8. **不使用真机或任何第三方云设备农场**，不预留相关任务、密钥或离线验收步骤。

在工作流页面打开成功的运行记录，下载 `Lumi-Community-Beta-<run_number>` Artifact，解压后选择 `*universal*apk`（通用版）或适配设备架构的文件，安装到 Android 8.0+ 设备。可在解压目录执行 `sha256sum -c SHA256SUMS.txt` 验证文件一致性。

**签名限制**：CI 当前提供 Android 默认 Debug 签名的测试 APK，适合安装和评估。不同 CI 运行的 Debug 签名通常无法保证一致，后续版本可能无法直接覆盖安装；正式分发前应创建安全保管的长期签名密钥并另设仅在受保护 Tag 上运行的签名发布工作流。绝对不要将私钥写入仓库或在不可信 PR 中注入私钥。

## 本地构建

可选的开发者本地编译需要 JDK 21、Android SDK API 37、NDK 28.0.13004108、CMake 3.22.1（不属于 CI 或发布验收步骤）：

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

APK 位于 `app/build/oss/outputs/apk/debug/`。如更新版本，修改根目录 `gradle.properties` 中的 `lumi.communityVersionName`、`lumi.communityVersionCode`，并确认 `versionCode` 严格递增。官方上游版本若变化，应先对比上游源代码、保留本 Fork 的 `applicationId`、source-set 和 CI 配置，再决定新的 Beta 版本号。

## 社区功能现状与后续验收

本 PR 的验收完全以 GitHub Actions 的最新 Required Gate 为准：

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
