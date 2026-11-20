# Lumi Community Beta

> 此仓库是 [huangder/Lumi_Books](https://github.com/huangder/Lumi_Books) 的非官方社区 Fork，不代表原作者。请保留源项目的 `LICENSE`、`NOTICE` 和相关第三方版权信息。

## 目标与当前范围

- **目标版本**：`2.3.12-beta.1`（公开上游源码为 `2.3.11`），Android `versionCode=19`。
- **独立包名**：`io.github.wxyu71.lumibooks`，独立于上游 `com.huangder.lumibooks` 安装；两个应用的数据目录也不同，不能自动共享书籍和阅读记录。
- **构建**：始终选择 `app/src/oss/`。公开版的 `OssAccess.isUnlocked()` 本来就返回 true、远程书源无限制；本 Fork 不包含官方私有会员许可校验和服务器。
- **重要限制**：公开仓库不存在完整的官方阅读报告、高级摘录样式、标签管理等 Premium UI 实现。此 Beta **不宣称已经实现这些功能**。未来功能应基于公开代码自主开发或依许可证整合第三方代码。

## CI 流程

[Android Community Beta 工作流](https://github.com/wxyu71/Lumi_Books/actions/workflows/android-beta.yml)

在 `community/**` 或 `main` 推送，以及向 `main` 发起 PR 时自动运行；也可从 `main` 的 Actions 页面手动触发。

1. 在 `ubuntu-24.04` runner 上使用 JDK 17，安装 Android API 37、Build Tools 37.0.0、NDK 28.0.13004108 和 CMake 3.22.1。
2. 使用校验过的 Gradle Wrapper 与官方 Gradle Actions 缓存；所有第三方 Action 固定完整 commit SHA。
3. 只读 `GITHUB_TOKEN`，无仓库私钥，无 `pull_request_target`，且检查仓库内没有私有 Premium 源集。
4. 运行 `./gradlew :app:assembleDebug`，验证每个 APK 的 package/versionCode/versionName，上传 APK 与 `SHA256SUMS.txt`。
5. 运行 `./gradlew :app:testDebugUnitTest` 并上传测试报告。测试失败时，工作流标记失败；即使构建产物存在，也应视为待排查版本。

在工作流页面打开成功的运行记录，下载 `Lumi-Community-Beta-<run_number>` Artifact，解压后选择 `*universal*apk`（通用版）或适配设备架构的文件，安装到 Android 8.0+ 设备。可在解压目录执行 `sha256sum -c SHA256SUMS.txt` 验证文件一致性。

**签名限制**：CI 当前提供 Android 默认 Debug 签名的测试 APK，适合安装和评估。不同 CI 运行的 Debug 签名通常无法保证一致，后续版本可能无法直接覆盖安装；正式分发前应创建安全保管的长期签名密钥并另设仅在受保护 Tag 上运行的签名发布工作流。绝对不要将私钥写入仓库或在不可信 PR 中注入私钥。

## 本地构建

需要 JDK 17、Android SDK API 37、NDK 28.0.13004108、CMake 3.22.1：

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

APK 位于 `app/build/oss/outputs/apk/debug/`。如更新版本，修改根目录 `gradle.properties` 中的 `lumi.communityVersionName`、`lumi.communityVersionCode`，并确认 `versionCode` 严格递增。官方上游版本若变化，应先对比上游源代码、保留本 Fork 的 `applicationId`、source-set 和 CI 配置，再决定新的 Beta 版本号。

## 后续功能路线

1. 自主实现标签管理 UI：复用公开版 `TagManagementRepository` 的数据层，添加 Compose 管理界面与测试。
2. 自主实现阅读报告：基于公开版 `ReadingInsights`/时间范围模型生成周/月/年度报告和可导出图片。
3. 自主设计新的摘录模板及导出流程，保留现有公开的六种基础样式。
4. 加入稳定签名、版本化 Release、校验和、发布变更日志；签名密钥仅在受保护发布事件中可用。
5. 添加版本检查与自动化同步策略，避免误将官方版本更新提示视为本 Fork 的更新。

## 参考

- [Gradle on GitHub Actions](https://docs.gradle.org/current/userguide/github-actions.html)
- [GitHub Actions security hardening](https://docs.github.com/en/actions/reference/security/secure-use)
- [Android SDK 37](https://developer.android.com/about/versions/17/setup-sdk)
- [F-Droid nightly builds](https://f-droid.org/en/docs/Publishing_Nightly_Builds/)
